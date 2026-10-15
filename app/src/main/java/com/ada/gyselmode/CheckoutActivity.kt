
package com.ada.gyselmode

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ada.gyselmode.data.carrito.CarritoRepository
import com.ada.gyselmode.model.pedido.CrearPedidoRequest
import com.ada.gyselmode.model.pedido.ProductoPedidoRequest
import com.ada.gyselmode.ui.pedidos.viewmodel.PedidoViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Locale
import android.content.Intent
import com.ada.gyselmode.ui.pago.PagoSimuladoActivity

import androidx.activity.result.contract.ActivityResultContracts
import com.ada.gyselmode.security.BaseSecureActivity
import com.ada.gyselmode.ui.ubicacion.SeleccionarUbicacionActivity


class CheckoutActivity : BaseSecureActivity() {

    private lateinit var viewModel: PedidoViewModel
    private lateinit var edtDireccion: EditText
    private lateinit var edtReferencia: EditText
    private lateinit var btnConfirmar: Button
    private lateinit var progress: ProgressBar

    private val costoEnvio = 0.0


    private var latitudEntrega: Double? = null
    private var longitudEntrega: Double? = null

    private val seleccionarMapa = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->

        if (resultado.resultCode == RESULT_OK) {

            val datos = resultado.data ?: return@registerForActivityResult

            if (!datos.hasExtra("LATITUD_ENTREGA") ||
                !datos.hasExtra("LONGITUD_ENTREGA")) {
                return@registerForActivityResult
            }

            latitudEntrega = datos.getDoubleExtra(
                "LATITUD_ENTREGA", 0.0
            )

            longitudEntrega = datos.getDoubleExtra(
                "LONGITUD_ENTREGA", 0.0
            )

            findViewById<TextView>(R.id.txtUbicacionMapa).text =
                "Ubicación seleccionada correctamente ✓"
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        viewModel = ViewModelProvider(this)[PedidoViewModel::class.java]

        edtDireccion = findViewById(R.id.edtDireccionEntrega)
        edtReferencia = findViewById(R.id.edtReferenciaEntrega)
        btnConfirmar = findViewById(R.id.btnConfirmarPedido)
        progress = findViewById(R.id.progressCheckout)


        findViewById<Button>(R.id.btnSeleccionarMapa)
            .setOnClickListener {

                seleccionarMapa.launch(
                    Intent(
                        this,
                        SeleccionarUbicacionActivity::class.java
                    )
                )
            }


        val items = CarritoRepository.items.value
        val subtotal = items.sumOf { it.subtotal }

        findViewById<TextView>(R.id.txtCheckoutResumen).text =
            "Productos seleccionados: ${items.sumOf { it.cantidad }}"

        findViewById<TextView>(R.id.txtCheckoutTotal).text =
            "Total estimado: ${dinero(subtotal + costoEnvio)}"

        btnConfirmar.setOnClickListener {
            confirmarPedido()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.cargando.collect { cargando ->
                        progress.visibility =
                            if (cargando) View.VISIBLE else View.GONE
                        btnConfirmar.isEnabled = !cargando
                    }
                }

                launch {
                    viewModel.mensaje.collect { mensaje ->
                        if (mensaje != null) {
                            Toast.makeText(
                                this@CheckoutActivity,
                                mensaje,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                launch {
                    viewModel.resultado.collect { pedido ->

                        if (pedido != null) {


                            val pedidoId = pedido.id

                            // Limpiar el carrito
                            CarritoRepository.vaciar()

                            viewModel.limpiarResultado()

                            val intent = Intent(
                                this@CheckoutActivity,
                                PagoSimuladoActivity::class.java
                            )

                            intent.putExtra("PEDIDO_ID", pedidoId.toLong())
                            startActivity(intent)
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun confirmarPedido() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (latitudEntrega == null || longitudEntrega == null) {
            Toast.makeText(
                this,
                "Selecciona tu ubicación de entrega en el mapa",
                Toast.LENGTH_LONG
            ).show()
            return
        }


        val latitud = latitudEntrega
        val longitud = longitudEntrega

        if (latitud == null || longitud == null) {
            Toast.makeText(
                this,
                "Selecciona la ubicación de entrega en el mapa",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (uid.isNullOrBlank()) {
            Toast.makeText(
                this,
                "Inicia sesión antes de realizar un pedido",
                Toast.LENGTH_LONG
            ).show()
            return


        }

        val direccion = edtDireccion.text.toString().trim()
        val referencia = edtReferencia.text.toString().trim()
        val items = CarritoRepository.items.value

        if (direccion.isBlank()) {
            edtDireccion.error = "Ingresa tu dirección"
            edtDireccion.requestFocus()
            return
        }

        if (items.isEmpty()) {
            Toast.makeText(
                this,
                "Tu carrito está vacío",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        val productos = items.map {
            ProductoPedidoRequest(
                productoId = it.productoId,
                tallaId = it.tallaId,
                cantidad = it.cantidad
            )
        }


        val request = CrearPedidoRequest(
            firebaseUid = uid,
            direccionEntrega = direccion,
            referencia = referencia,
            latitudEntrega = latitud,
            longitudEntrega = longitud,
            costoEnvio = costoEnvio,
            productos = productos
        )


        viewModel.crearPedido(request)
    }

    private fun dinero(valor: Double): String =
        "S/ ${String.format(Locale.US, "%.2f", valor)}"
}