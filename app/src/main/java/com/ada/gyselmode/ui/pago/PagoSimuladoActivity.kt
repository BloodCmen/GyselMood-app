
package com.ada.gyselmode.ui.pago

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ada.gyselmode.databinding.ActivityPagoSimuladoBinding
import com.ada.gyselmode.network.RetrofitClient
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

import android.util.Log
import com.ada.gyselmode.network.ConciliacionResponse
import com.ada.gyselmode.network.MercadoPagoConciliacionClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
class PagoSimuladoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPagoSimuladoBinding

    private val api get() = RetrofitClient.apiService

    private var pedidoId = -1L
    private var procesando = false
    private var pagoAprobado = false
    private var checkoutAbierto = false

    private var trabajoVerificacion: Job? = null

    private val uid: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPagoSimuladoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pedidoId = intent.getLongExtra("PEDIDO_ID", -1L)

        if (pedidoId <= 0L) {
            Toast.makeText(
                this,
                "No se encontró el pedido",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        checkoutAbierto = savedInstanceState
            ?.getBoolean("CHECKOUT_ABIERTO", false) ?: false

        binding.txtPedidoPago.text = "Pedido #$pedidoId"
        binding.txtEstadoPago.text =
            "Consultando el estado de tu pedido..."

        actualizarBoton()

        binding.btnConfirmarPago.setOnClickListener {
            if (procesando || pagoAprobado) return@setOnClickListener

            if (checkoutAbierto) {
                verificarPago()
            } else {
                iniciarMercadoPago()
            }
        }

        // Verificar si este pedido ya estaba pagado.
        verificarPago(automatico = false)
    }

    override fun onResume() {
        super.onResume()

        // Cuando el usuario vuelve del navegador,
        // consultar a Railway sin aprobar nada en Android.
        if (checkoutAbierto && !pagoAprobado) {
            verificarPago(automatico = true)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("CHECKOUT_ABIERTO", checkoutAbierto)
        super.onSaveInstanceState(outState)
    }

    private fun actualizarBoton() {
        binding.btnConfirmarPago.text = when {
            pagoAprobado -> "Pago confirmado"
            checkoutAbierto -> "Verificar pago"
            else -> "Pagar con Mercado Pago"
        }

        binding.btnConfirmarPago.isEnabled =
            !procesando && !pagoAprobado
    }

    private fun cambiarCarga(cargando: Boolean) {
        procesando = cargando

        binding.progressPago.visibility =
            if (cargando) View.VISIBLE else View.GONE

        actualizarBoton()
    }

    private fun iniciarMercadoPago() {
        if (procesando || pagoAprobado) return

        lifecycleScope.launch {
            cambiarCarga(true)

            try {
                val usuarioId = uid

                if (usuarioId.isNullOrBlank()) {
                    binding.txtEstadoPago.text =
                        "Debes iniciar sesión para pagar."
                    return@launch
                }

                // Antes de abrir MP, validar el estado
                // que figura realmente en el backend.
                val pedido = api.obtenerPedido(
                    pedidoId,
                    usuarioId
                )

                when (pedido.estado.uppercase()) {
                    "PAGADO" -> {
                        mostrarAprobado()
                        return@launch
                    }

                    "PENDIENTE" -> {
                        // Podemos generar preferencia.
                    }

                    else -> {
                        binding.txtEstadoPago.text =
                            "El pedido está en estado ${pedido.estado}. " +
                                    "No se puede iniciar el pago."
                        return@launch
                    }
                }

                binding.txtEstadoPago.text =
                    "Preparando Mercado Pago..."

                // Spring Boot genera la preferencia.
                val preferencia =
                    api.crearPreferenciaMercadoPago(pedidoId)

                val url = preferencia.initPoint

                if (!url.startsWith("https://")) {
                    binding.txtEstadoPago.text =
                        "Mercado Pago devolvió una URL inválida."
                    return@launch
                }

                // Abrir el Checkout Pro en navegador.
                val navegador = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                ).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                }

                checkoutAbierto = true
                actualizarBoton()

                try {
                    startActivity(navegador)

                    binding.txtEstadoPago.text =
                        "Completa tu pago en Mercado Pago. " +
                                "Cuando regreses, verificaremos el estado."

                } catch (e: ActivityNotFoundException) {
                    checkoutAbierto = false

                    binding.txtEstadoPago.text =
                        "No hay un navegador disponible " +
                                "para abrir Mercado Pago."
                }

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                binding.txtEstadoPago.text =
                    "No se pudo abrir Mercado Pago. " +
                            "Revisa tu conexión e inténtalo nuevamente."

                android.util.Log.e(
                    "MERCADO_PAGO",
                    "Error iniciando Checkout Pro",
                    e
                )

            } finally {
                cambiarCarga(false)
            }
        }
    }


    private fun verificarPago(automatico: Boolean = false) {

        if (pagoAprobado) return
        if (trabajoVerificacion?.isActive == true) return

        trabajoVerificacion = lifecycleScope.launch {

            cambiarCarga(true)

            try {
                val usuarioId = uid

                if (usuarioId.isNullOrBlank()) {
                    binding.txtEstadoPago.text =
                        "Inicia sesión para verificar tu pedido."
                    return@launch
                }

                val intentos = if (automatico) 12 else 1

                for (intento in 1..intentos) {

                    if (!isActive || pagoAprobado) return@launch

                    // 1. Consultar estado actual en Railway
                    val pedido = api.obtenerPedido(
                        pedidoId,
                        usuarioId
                    )

                    when (pedido.estado.uppercase()) {

                        "PAGADO" -> {
                            mostrarAprobado()
                            return@launch
                        }

                        "PENDIENTE" -> {

                            binding.txtEstadoPago.text =
                                "Verificando pago con Mercado Pago..."

                            // 2. Consultar directamente Mercado Pago
                            // mediante nuestro endpoint de conciliación
                            val confirmado =
                                suspendCancellableCoroutine<Boolean> { continuation ->

                                    conciliarPagoMercadoPago(pedidoId) { pagado ->

                                        if (continuation.isActive) {
                                            continuation.resume(pagado)
                                        }
                                    }
                                }

                            if (confirmado) {
                                mostrarAprobado()
                                return@launch
                            }

                            binding.txtEstadoPago.text =
                                if (automatico) {
                                    "Esperando confirmación del pago " +
                                            "($intento/$intentos)..."
                                } else {
                                    "El pago todavía está pendiente."
                                }
                        }

                        else -> {
                            binding.txtEstadoPago.text =
                                "Estado del pedido: ${pedido.estado}"
                            return@launch
                        }
                    }

                    // 3. Esperar antes de volver a consultar
                    if (intento < intentos) {
                        delay(5000)
                    }
                }

                if (checkoutAbierto && !pagoAprobado) {

                    binding.txtEstadoPago.text =
                        "El pago todavía no está confirmado. " +
                                "Si ya pagaste, pulsa Verificar pago. " +
                                "No realices otro pago hasta comprobarlo."
                }

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {

                binding.txtEstadoPago.text =
                    "No se pudo verificar el pago. " +
                            "Comprueba tu conexión e inténtalo nuevamente."

                Log.e(
                    "MERCADO_PAGO",
                    "Error verificando pedido $pedidoId",
                    e
                )

            } finally {
                cambiarCarga(false)
            }
        }
    }

    private fun mostrarAprobado() {
        if (pagoAprobado) return

        pagoAprobado = true
        actualizarBoton()

        Toast.makeText(
            this,
            "¡Pago confirmado!",
            Toast.LENGTH_SHORT
        ).show()

        val intentExito = Intent(
            this,
            CompraExitosaActivity::class.java
        ).apply {
            putExtra("PEDIDO_ID", pedidoId)
        }

        startActivity(intentExito)
        finish()
    }


    private fun conciliarPagoMercadoPago(
        pedidoId: Long,
        alTerminar: (Boolean) -> Unit
    ) {

        val usuario = FirebaseAuth.getInstance().currentUser

        if (usuario == null) {
            Toast.makeText(
                this,
                "Inicia sesión para verificar tu pago",
                Toast.LENGTH_LONG
            ).show()

            alTerminar(false)
            return
        }

        // Firebase obtiene un token válido automáticamente.
        usuario.getIdToken(false)
            .addOnSuccessListener { resultado ->

                val idToken = resultado.token

                if (idToken.isNullOrBlank()) {
                    alTerminar(false)
                    return@addOnSuccessListener
                }

                val llamada = MercadoPagoConciliacionClient.api
                    .conciliarPago(
                        pedidoId,
                        "Bearer $idToken"
                    )

                llamada.enqueue(
                    object : Callback<ConciliacionResponse> {

                        override fun onResponse(
                            call: Call<ConciliacionResponse>,
                            response: Response<ConciliacionResponse>
                        ) {

                            if (!response.isSuccessful) {
                                Log.e(
                                    "MERCADO_PAGO",
                                    "Conciliación HTTP ${response.code()}"
                                )
                                alTerminar(false)
                                return
                            }

                            val datos = response.body()

                            Log.d(
                                "MERCADO_PAGO",
                                "Estado: ${datos?.estado}, " +
                                        "Resultado: ${datos?.resultado}"
                            )

                            alTerminar(
                                datos?.estado == "PAGADO"
                            )
                        }

                        override fun onFailure(
                            call: Call<ConciliacionResponse>,
                            error: Throwable
                        ) {
                            Log.e(
                                "MERCADO_PAGO",
                                "Error consultando conciliación",
                                error
                            )

                            alTerminar(false)
                        }
                    }
                )
            }
            .addOnFailureListener { error ->

                Log.e(
                    "FIREBASE",
                    "No se pudo obtener el ID Token",
                    error
                )

                alTerminar(false)
            }
    }

}
