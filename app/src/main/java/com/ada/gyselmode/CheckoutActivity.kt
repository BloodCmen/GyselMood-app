
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

class CheckoutActivity : AppCompatActivity() {

    private lateinit var viewModel: PedidoViewModel
    private lateinit var edtDireccion: EditText
    private lateinit var edtReferencia: EditText
    private lateinit var btnConfirmar: Button
    private lateinit var progress: ProgressBar

    private val costoEnvio = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        viewModel = ViewModelProvider(this)[PedidoViewModel::class.java]

        edtDireccion = findViewById(R.id.edtDireccionEntrega)
        edtReferencia = findViewById(R.id.edtReferenciaEntrega)
        btnConfirmar = findViewById(R.id.btnConfirmarPedido)
        progress = findViewById(R.id.progressCheckout)

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
                            CarritoRepository.vaciar()
                            viewModel.limpiarResultado()
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun confirmarPedido() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

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
            costoEnvio = costoEnvio,
            productos = productos
        )

        viewModel.crearPedido(request)
    }

    private fun dinero(valor: Double): String =
        "S/ ${String.format(Locale.US, "%.2f", valor)}"
}