package com.ada.gyselmode.ui.user.pedidos

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ada.gyselmode.R
import com.ada.gyselmode.network.RetrofitClient
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import retrofit2.HttpException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class SeguimientoPedidoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PEDIDO_ID = "pedido_id"
        private const val INTERVALO_MS = 10_000L
        private const val ESTILO_MAPA =
            "https://tiles.openfreemap.org/styles/liberty"
    }

    private lateinit var mapView: MapView
    private lateinit var txtTitulo: TextView
    private lateinit var txtEstado: TextView
    private lateinit var txtActualizacion: TextView
    private lateinit var btnCentrar: Button

    private val auth = FirebaseAuth.getInstance()

    private var mapa: MapLibreMap? = null
    private var marcador: Marker? = null
    private var ultimaPosicion: LatLng? = null
    private var primerPunto = true

    private var pedidoId = -1L
    private var uidInicial: String? = null
    private var trabajo: Job? = null
    private var finalizado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar MapLibre antes de cargar el XML.
        MapLibre.getInstance(this)

        setContentView(R.layout.activity_seguimiento_pedido)

        mapView = findViewById(R.id.mapaSeguimiento)
        txtTitulo = findViewById(R.id.txtTituloSeguimiento)
        txtEstado = findViewById(R.id.txtEstadoSeguimiento)
        txtActualizacion =
            findViewById(R.id.txtUltimaActualizacion)
        btnCentrar = findViewById(R.id.btnCentrarRepartidor)

        pedidoId = intent.getLongExtra(EXTRA_PEDIDO_ID, -1L)
        uidInicial = auth.currentUser?.uid

        txtTitulo.text = "Seguimiento del pedido #$pedidoId"

        btnCentrar.isEnabled = false
        btnCentrar.setOnClickListener {
            ultimaPosicion?.let { posicion ->
                mapa?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        posicion,
                        16.0
                    )
                )
            }
        }

        mapView.onCreate(savedInstanceState)

        mapView.getMapAsync { mapaListo ->
            mapa = mapaListo

            mapaListo.setStyle(ESTILO_MAPA) {
                ultimaPosicion?.let { actualizarMarcador(it) }
            }
        }

        if (pedidoId <= 0 || uidInicial == null) {
            txtEstado.text = "Pedido o sesión no disponible"
            finalizado = true
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()

        if (finalizado || trabajo?.isActive == true) return

        trabajo = lifecycleScope.launch {
            while (isActive && !finalizado) {
                consultarSeguimiento()
                if (!finalizado) delay(INTERVALO_MS)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        trabajo?.cancel()
        trabajo = null
        mapView.onStop()
        super.onStop()
    }

    private suspend fun consultarSeguimiento() {
        val usuario = auth.currentUser

        if (usuario == null || usuario.uid != uidInicial) {
            txtEstado.text = "La sesión cambió"
            detenerConsultas()
            return
        }

        try {
            val token = suspendCoroutine<String> { continuacion ->
                usuario.getIdToken(false)
                    .addOnSuccessListener { resultado ->
                        val valor = resultado.token
                        if (valor.isNullOrBlank()) {
                            continuacion.resumeWithException(
                                IllegalStateException("Token vacío")
                            )
                        } else {
                            continuacion.resume(valor)
                        }
                    }
                    .addOnFailureListener { error ->
                        continuacion.resumeWithException(error)
                    }
            }

            val respuesta = RetrofitClient.apiService
                .obtenerSeguimientoEntrega(
                    "Bearer $token",
                    pedidoId
                )

            if (auth.currentUser?.uid != uidInicial) {
                detenerConsultas()
                return
            }

            val estado = respuesta.estadoEntrega.uppercase()
            txtEstado.text = "Entrega: $estado"

            when (estado) {
                "EN_CAMINO" -> {
                    val lat = respuesta.latitud
                    val lng = respuesta.longitud

                    if (lat != null && lng != null &&
                        lat.isFinite() && lng.isFinite() &&
                        lat in -90.0..90.0 &&
                        lng in -180.0..180.0
                    ) {
                        actualizarMarcador(LatLng(lat, lng))
                        txtActualizacion.text =
                            "Último GPS: " +
                                    (respuesta.ultimaActualizacion
                                        ?: "Disponible")
                    } else {
                        limpiarMarcador()
                        txtActualizacion.text =
                            "Esperando ubicación reciente..."
                    }
                }

                "ENTREGADA", "CANCELADA" -> {
                    limpiarMarcador()
                    txtActualizacion.text =
                        "Seguimiento finalizado"
                    detenerConsultas()
                }

                else -> {
                    limpiarMarcador()
                    txtActualizacion.text =
                        "El repartidor todavía no inicia el recorrido"
                }
            }

        } catch (e: CancellationException) {
            throw e

        } catch (e: HttpException) {
            txtEstado.text = "Error HTTP ${e.code()}"

            if (e.code() in listOf(401, 403, 404)) {
                limpiarMarcador()
                detenerConsultas()
            }

        } catch (e: Exception) {
            txtEstado.text =
                "Sin conexión. Reintentando..."
        }
    }

    private fun actualizarMarcador(posicion: LatLng) {
        ultimaPosicion = posicion

        val mapaActual = mapa ?: return

        if (marcador == null) {
            marcador = mapaActual.addMarker(
                MarkerOptions()
                    .position(posicion)
                    .title("Tu repartidor")
            )
        } else {
            marcador?.position = posicion
        }

        btnCentrar.isEnabled = true

        if (primerPunto) {
            mapaActual.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    posicion,
                    16.0
                )
            )
            primerPunto = false
        }
    }

    private fun limpiarMarcador() {
        marcador?.remove()
        marcador = null
        ultimaPosicion = null
        primerPunto = true
        btnCentrar.isEnabled = false
    }

    private fun detenerConsultas() {
        finalizado = true
        trabajo?.cancel()
        trabajo = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mapView.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        trabajo?.cancel()
        mapView.onDestroy()
        super.onDestroy()
    }
}
