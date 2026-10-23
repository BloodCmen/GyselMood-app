package com.ada.gyselmode.ui.user.pedidos

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ada.gyselmode.R
import com.ada.gyselmode.data.carrito.CarritoRepository
import com.ada.gyselmode.model.pedido.CrearPedidoRequest
import com.ada.gyselmode.model.pedido.ProductoPedidoRequest
import com.ada.gyselmode.security.BaseSecureActivity
import com.ada.gyselmode.ui.user.pago.PagoSimuladoActivity
import com.ada.gyselmode.ui.user.pedidos.viewmodel.PedidoViewModel
import com.ada.gyselmode.ui.user.ubicacion.SeleccionarUbicacionActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Locale

class CheckoutActivity : BaseSecureActivity() {

    private lateinit var viewModel: PedidoViewModel

    private lateinit var edtDireccion: EditText
    private lateinit var edtReferencia: EditText
    private lateinit var btnConfirmar: Button
    private lateinit var progress: ProgressBar

    private lateinit var txtUbicacion: TextView
    private lateinit var txtResumen: TextView
    private lateinit var txtSubtotal: TextView
    private lateinit var txtEnvio: TextView
    private lateinit var txtTotal: TextView

    // Por ahora el envío sigue siendo S/ 0.00,
    // igual que en tu implementación original.
    private val costoEnvio = 0.0

    private var latitudEntrega: Double? = null
    private var longitudEntrega: Double? = null

    private var procesandoPedido = false
    private var resultadoProcesado = false

    companion object {
        private const val KEY_LATITUD = "checkout_latitud"
        private const val KEY_LONGITUD = "checkout_longitud"
        private const val KEY_RESULTADO_PROCESADO =
            "checkout_resultado_procesado"
    }

    private val seleccionarMapa = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->

        if (resultado.resultCode != RESULT_OK) {
            return@registerForActivityResult
        }

        val datos = resultado.data
            ?: return@registerForActivityResult

        if (!datos.hasExtra("LATITUD_ENTREGA") ||
            !datos.hasExtra("LONGITUD_ENTREGA")
        ) {
            return@registerForActivityResult
        }

        val latitud = datos.getDoubleExtra(
            "LATITUD_ENTREGA",
            Double.NaN
        )

        val longitud = datos.getDoubleExtra(
            "LONGITUD_ENTREGA",
            Double.NaN
        )

        if (!coordenadasValidas(latitud, longitud)) {
            Toast.makeText(
                this,
                "La ubicación seleccionada no es válida",
                Toast.LENGTH_SHORT
            ).show()
            return@registerForActivityResult
        }

        latitudEntrega = latitud
        longitudEntrega = longitud

        actualizarEstadoUbicacion()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_checkout)

        viewModel = ViewModelProvider(this)[
            PedidoViewModel::class.java
        ]

        inicializarVistas()

        // Restaurar el punto del mapa si Android
        // tuvo que recrear esta pantalla.
        if (savedInstanceState != null) {

            if (savedInstanceState.containsKey(KEY_LATITUD) &&
                savedInstanceState.containsKey(KEY_LONGITUD)
            ) {
                latitudEntrega = savedInstanceState.getDouble(
                    KEY_LATITUD
                )

                longitudEntrega = savedInstanceState.getDouble(
                    KEY_LONGITUD
                )
            }

            resultadoProcesado = savedInstanceState.getBoolean(
                KEY_RESULTADO_PROCESADO,
                false
            )
        }

        actualizarEstadoUbicacion()
        actualizarResumen()

        findViewById<Button>(R.id.btnSeleccionarMapa)
            .setOnClickListener {

                seleccionarMapa.launch(
                    Intent(
                        this,
                        SeleccionarUbicacionActivity::class.java
                    )
                )
            }

        btnConfirmar.setOnClickListener {
            confirmarPedido()
        }

        observarEstadoPedido()
        observarCarrito()
    }

    private fun inicializarVistas() {

        edtDireccion = findViewById(
            R.id.edtDireccionEntrega
        )

        edtReferencia = findViewById(
            R.id.edtReferenciaEntrega
        )

        btnConfirmar = findViewById(
            R.id.btnConfirmarPedido
        )

        progress = findViewById(
            R.id.progressCheckout
        )

        txtUbicacion = findViewById(
            R.id.txtUbicacionMapa
        )

        txtResumen = findViewById(
            R.id.txtCheckoutResumen
        )

        txtSubtotal = findViewById(
            R.id.txtSubtotalCheckout
        )

        txtEnvio = findViewById(
            R.id.txtEnvioCheckout
        )

        txtTotal = findViewById(
            R.id.txtCheckoutTotal
        )
    }

    private fun actualizarEstadoUbicacion() {

        val latitud = latitudEntrega
        val longitud = longitudEntrega

        if (latitud != null &&
            longitud != null &&
            coordenadasValidas(latitud, longitud)
        ) {

            txtUbicacion.text =
                "Ubicación seleccionada correctamente ✓\n" +
                        "Puedes cambiarla si lo necesitas."

            txtUbicacion.setTextColor(
                Color.parseColor("#9BD8B4")
            )

            findViewById<Button>(R.id.btnSeleccionarMapa)
                .text = "CAMBIAR UBICACIÓN  →"

        } else {

            txtUbicacion.text =
                "Aún no seleccionaste una ubicación"

            txtUbicacion.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.gysel_light_gray
                )
            )

            findViewById<Button>(R.id.btnSeleccionarMapa)
                .text = "SELECCIONAR EN EL MAPA  →"
        }
    }

    private fun coordenadasValidas(
        latitud: Double,
        longitud: Double
    ): Boolean {
        return latitud.isFinite() &&
                longitud.isFinite() &&
                latitud in -90.0..90.0 &&
                longitud in -180.0..180.0
    }

    private fun actualizarResumen() {

        val items = CarritoRepository.items.value

        val unidades = items.sumOf {
            it.cantidad
        }

        val subtotal = items.sumOf {
            it.subtotal
        }

        txtResumen.text = when {

            items.isEmpty() ->
                "Tu carrito está vacío"

            items.size == 1 ->
                "1 producto · $unidades " +
                        if (unidades == 1) "unidad"
                        else "unidades"

            else ->
                "${items.size} productos · $unidades unidades"
        }

        txtSubtotal.text = dinero(subtotal)

        txtEnvio.text = dinero(costoEnvio)

        txtTotal.text = dinero(
            subtotal + costoEnvio
        )

        btnConfirmar.isEnabled =
            items.isNotEmpty() && !procesandoPedido
    }

    private fun observarCarrito() {

        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.STARTED) {

                CarritoRepository.items.collect {
                    actualizarResumen()
                }
            }
        }
    }

    private fun observarEstadoPedido() {

        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {

                    viewModel.cargando.collect { cargando ->

                        procesandoPedido = cargando

                        progress.visibility =
                            if (cargando) View.VISIBLE
                            else View.GONE

                        actualizarResumen()

                        findViewById<Button>(
                            R.id.btnSeleccionarMapa
                        ).isEnabled = !cargando
                    }
                }

                launch {

                    viewModel.mensaje.collect { mensaje ->

                        if (!mensaje.isNullOrBlank()) {

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

                        if (pedido != null &&
                            !resultadoProcesado
                        ) {

                            resultadoProcesado = true

                            val pedidoId = pedido.id.toLong()

                            // Mantenemos el comportamiento original.
                            // El pedido ya fue creado correctamente.
                            CarritoRepository.vaciar()

                            viewModel.limpiarResultado()

                            val intentPago = Intent(
                                this@CheckoutActivity,
                                PagoSimuladoActivity::class.java
                            )

                            intentPago.putExtra(
                                "PEDIDO_ID",
                                pedidoId
                            )

                            startActivity(intentPago)
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun confirmarPedido() {

        if (procesandoPedido || resultadoProcesado) {
            return
        }

        val uid = FirebaseAuth.getInstance()
            .currentUser?.uid

        if (uid.isNullOrBlank()) {

            Toast.makeText(
                this,
                "Inicia sesión antes de realizar tu pedido",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val direccion = edtDireccion.text
            .toString()
            .trim()

        val referencia = edtReferencia.text
            .toString()
            .trim()

        if (direccion.isBlank()) {

            edtDireccion.error =
                "Ingresa tu dirección de entrega"

            edtDireccion.requestFocus()
            return
        }

        val latitud = latitudEntrega
        val longitud = longitudEntrega

        if (latitud == null ||
            longitud == null ||
            !coordenadasValidas(latitud, longitud)
        ) {

            Toast.makeText(
                this,
                "Selecciona tu ubicación de entrega en el mapa",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val items = CarritoRepository.items.value

        if (items.isEmpty()) {

            Toast.makeText(
                this,
                "Tu carrito está vacío",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val productos = items.map { item ->

            ProductoPedidoRequest(
                productoId = item.productoId,
                tallaId = item.tallaId,
                cantidad = item.cantidad
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

        // El ViewModel controla el estado de carga.
        viewModel.crearPedido(request)
    }

    override fun onSaveInstanceState(outState: Bundle) {

        latitudEntrega?.let {
            outState.putDouble(KEY_LATITUD, it)
        }

        longitudEntrega?.let {
            outState.putDouble(KEY_LONGITUD, it)
        }

        outState.putBoolean(
            KEY_RESULTADO_PROCESADO,
            resultadoProcesado
        )

        super.onSaveInstanceState(outState)
    }

    private fun dinero(valor: Double): String =
        String.format(
            Locale.US,
            "S/ %.2f",
            valor
        )
}