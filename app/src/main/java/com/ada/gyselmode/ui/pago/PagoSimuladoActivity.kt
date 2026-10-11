package com.ada.gyselmode.ui.pago

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ada.gyselmode.databinding.ActivityPagoSimuladoBinding
import com.ada.gyselmode.model.pago.CrearPagoRequest
import com.ada.gyselmode.network.RetrofitClient
import kotlinx.coroutines.launch
import org.json.JSONObject

class PagoSimuladoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPagoSimuladoBinding
    private var pedidoId: Long = -1L
    private var procesando = false

    private var pagoAprobado = false

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

        binding.txtPedidoPago.text = "Pedido #$pedidoId"

        binding.btnConfirmarPago.setOnClickListener {
            registrarPago()
        }

        consultarPago()
    }

    private fun cambiarCarga(cargando: Boolean) {
        procesando = cargando

        binding.progressPago.visibility =
            if (cargando) View.VISIBLE else View.GONE

        binding.btnConfirmarPago.isEnabled =
            !cargando && !pagoAprobado
    }

    private fun mostrarAprobado() {
        pagoAprobado = true

        val intentExito = Intent(
            this@PagoSimuladoActivity,
            CompraExitosaActivity::class.java
        )

        intentExito.putExtra("PEDIDO_ID", pedidoId)

        startActivity(intentExito)
        finish()
    }
    private fun consultarPago() {
        lifecycleScope.launch {
            cambiarCarga(true)

            try {
                val response =
                    RetrofitClient.apiService.obtenerPago(pedidoId)

                if (response.isSuccessful) {
                    val contenido = response.body()?.string()

                    if (!contenido.isNullOrBlank()) {
                        val json = JSONObject(contenido)

                        if (json.optString("estado")
                                .equals("APROBADO", ignoreCase = true)
                        ) {
                            mostrarAprobado()
                        }
                    }
                } else if (response.code() != 404) {
                    binding.txtEstadoPago.text =
                        "No se pudo verificar el pago anterior"
                }

            } catch (e: Exception) {
                binding.txtEstadoPago.text =
                    "Sin conexión para consultar el pago"
            } finally {
                cambiarCarga(false)
            }
        }
    }

    private fun registrarPago() {
        if (procesando) return

        lifecycleScope.launch {
            cambiarCarga(true)

            try {
                // Consultar primero para no registrar otro pago
                val consulta =
                    RetrofitClient.apiService.obtenerPago(pedidoId)

                if (consulta.isSuccessful) {
                    val contenido = consulta.body()?.string()

                    if (!contenido.isNullOrBlank()) {
                        val json = JSONObject(contenido)

                        if (json.optString("estado")
                                .equals("APROBADO", ignoreCase = true)
                        ) {
                            mostrarAprobado()
                            return@launch
                        }
                    }

                    // Si existe otro pago, no crear uno duplicado.
                    binding.txtEstadoPago.text =
                        "Ya existe un registro de pago. Revisar su estado."
                    return@launch
                }

                if (consulta.code() != 404) {
                    binding.txtEstadoPago.text =
                        "Error al consultar pago: HTTP ${consulta.code()}"

                    android.util.Log.e(
                        "PAGO_DEBUG",
                        "GET pago $pedidoId → HTTP ${consulta.code()} " +
                                "${consulta.errorBody()?.string()}"
                    )

                    return@launch
                }

                val request = CrearPagoRequest(
                    proveedor = "PRUEBA",
                    metodo = "TARJETA",
                    estado = "APROBADO",
                    referenciaExterna = "DEMO-PEDIDO-$pedidoId"
                )

                val response =
                    RetrofitClient.apiService.registrarPago(
                        pedidoId,
                        request
                    )

                if (response.isSuccessful) {
                    // Confirmamos el estado en el backend
                    val verificacion =
                        RetrofitClient.apiService.obtenerPago(pedidoId)

                    if (verificacion.isSuccessful) {
                        val contenido = verificacion.body()?.string()
                        val estado = if (!contenido.isNullOrBlank()) {
                            JSONObject(contenido).optString("estado")
                        } else {
                            ""
                        }

                        if (estado.equals("APROBADO", true)) {
                            mostrarAprobado()
                        } else {
                            binding.txtEstadoPago.text =
                                "Pago registrado. Estado: $estado"
                        }
                    } else {
                        binding.txtEstadoPago.text =
                            "Pago enviado. Verifica su estado."
                    }
                } else {
                    binding.txtEstadoPago.text =
                        "Error al registrar: HTTP ${response.code()}"
                }

            } catch (e: Exception) {
                binding.txtEstadoPago.text =
                    "No se pudo confirmar. Revisa antes de reintentar."
            } finally {
                cambiarCarga(false)
            }
        }
    }
}
