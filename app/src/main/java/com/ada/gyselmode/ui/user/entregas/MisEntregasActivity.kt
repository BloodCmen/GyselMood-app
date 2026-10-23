package com.ada.gyselmode.ui.user.entregas

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ada.gyselmode.R
import com.ada.gyselmode.adapter.EntregaAdapter
import com.ada.gyselmode.model.entrega.AsignacionEntregaResponse
import com.ada.gyselmode.network.RetrofitClient
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import retrofit2.HttpException

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MisEntregasActivity : AppCompatActivity() {

    private lateinit var recyclerEntregas: RecyclerView
    private lateinit var progressEntregas: ProgressBar
    private lateinit var txtMensaje: TextView
    private lateinit var btnActualizar: Button

    private lateinit var entregaAdapter: EntregaAdapter

    private val auth = FirebaseAuth.getInstance()

    private var cargando = false
    private var procesandoMovimiento = false


    private var pedidoGpsPendiente: Long? = null

    private val solicitarPermisoGps =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permisos ->

            val concedido =
                permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            val pedidoId = pedidoGpsPendiente
            pedidoGpsPendiente = null

            if (concedido && pedidoId != null) {
                iniciarComparticionGps(pedidoId)
            } else {
                mostrarToast(
                    "Ubicación no compartida. Puedes activarla después."
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_mis_entregas)

        recyclerEntregas = findViewById(R.id.recyclerEntregas)
        progressEntregas = findViewById(R.id.progressEntregas)
        txtMensaje = findViewById(R.id.txtMensajeEntregas)
        btnActualizar = findViewById(R.id.btnActualizarEntregas)


        entregaAdapter = EntregaAdapter(
            onDetalle = { entrega ->
                mostrarDetalleEntrega(entrega)
            },
            onComoLlegar = { entrega ->
                abrirGoogleMaps(entrega)
            },
            onIniciar = { entrega ->
                confirmarMovimiento(
                    entrega.pedidoId,
                    iniciar = true
                )
            },
            onFinalizar = { entrega ->
                confirmarMovimiento(
                    entrega.pedidoId,
                    iniciar = false
                )
            },
            onCompartirGps = { entrega ->
                solicitarComparticionGps(entrega.pedidoId)
            }
        )


        recyclerEntregas.layoutManager =
            LinearLayoutManager(this)

        recyclerEntregas.adapter = entregaAdapter

        btnActualizar.setOnClickListener {
            cargarMisEntregas()
        }

        cargarMisEntregas()
    }

    // =====================================
    // LISTAR ENTREGAS DEL REPARTIDOR
    // =====================================

    private fun cargarMisEntregas() {

        if (cargando || procesandoMovimiento) return

        val usuario = auth.currentUser

        if (usuario == null) {
            entregaAdapter.actualizarLista(emptyList())
            mostrarError("Debes iniciar sesión")
            return
        }

        cargando = true
        progressEntregas.visibility = View.VISIBLE
        btnActualizar.isEnabled = false
        txtMensaje.visibility = View.GONE

        usuario.getIdToken(false)
            .addOnSuccessListener { resultado ->

                val token = resultado.token

                if (token.isNullOrBlank()) {
                    mostrarError("No se obtuvo el token Firebase")
                    finalizarCarga()
                    return@addOnSuccessListener
                }

                lifecycleScope.launch {

                    try {
                        val entregas =
                            RetrofitClient.apiService
                                .listarMisEntregas("Bearer $token")

                        // Evitar mostrar entregas de una
                        // cuenta si la sesión cambió.
                        if (auth.currentUser?.uid != usuario.uid) {
                            entregaAdapter.actualizarLista(emptyList())
                            mostrarError("La sesión cambió")
                            return@launch
                        }

                        entregaAdapter.actualizarLista(entregas)

                        if (entregas.isEmpty()) {
                            txtMensaje.text =
                                "No tienes entregas asignadas"
                            txtMensaje.visibility = View.VISIBLE
                        } else {
                            txtMensaje.visibility = View.GONE
                        }

                    } catch (e: HttpException) {

                        val mensaje = when (e.code()) {
                            401 -> "Sesión Firebase no válida"
                            403 -> "No tienes acceso a las entregas"
                            404 -> "No se encontró el endpoint"
                            500 -> "Error interno de Render"
                            else -> "Error HTTP ${e.code()}"
                        }

                        mostrarError(mensaje)

                    } catch (e: Exception) {
                        mostrarError(
                            e.localizedMessage ?: "Error de conexión"
                        )

                    } finally {
                        finalizarCarga()
                    }
                }
            }
            .addOnFailureListener { error ->

                mostrarError(
                    error.localizedMessage
                        ?: "No se pudo validar Firebase"
                )

                finalizarCarga()
            }
    }

    private fun finalizarCarga() {
        cargando = false
        progressEntregas.visibility = View.GONE
        btnActualizar.isEnabled = !procesandoMovimiento
    }

    private fun mostrarError(mensaje: String) {
        entregaAdapter.actualizarLista(emptyList())
        txtMensaje.text = mensaje
        txtMensaje.visibility = View.VISIBLE
    }

    // =====================================
    // VER DETALLE
    // =====================================

    private fun mostrarDetalleEntrega(
        entrega: AsignacionEntregaResponse
    ) {

        val referencia = entrega.referencia
            ?.takeIf { it.isNotBlank() }
            ?: "Sin referencia"

        val ubicacion = if (
            entrega.latitudEntrega != null &&
            entrega.longitudEntrega != null
        ) {
            "${entrega.latitudEntrega}, ${entrega.longitudEntrega}"
        } else {
            "No registrada"
        }

        val mensaje = """
            Estado: ${entrega.estado}

            Dirección:
            ${entrega.direccionEntrega}

            Referencia:
            $referencia

            Ubicación de destino:
            $ubicacion

            Fecha de asignación:
            ${entrega.fechaAsignacion ?: "No disponible"}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Pedido #${entrega.pedidoId}")
            .setMessage(mensaje)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    // =====================================
    // ABRIR GOOGLE MAPS
    // =====================================

    private fun abrirGoogleMaps(
        entrega: AsignacionEntregaResponse
    ) {

        val lat = entrega.latitudEntrega
        val lng = entrega.longitudEntrega

        val coordenadasValidas =
            lat != null &&
                    lng != null &&
                    lat.isFinite() &&
                    lng.isFinite() &&
                    lat in -90.0..90.0 &&
                    lng in -180.0..180.0

        val destino = if (coordenadasValidas) {
            "$lat,$lng"
        } else {
            entrega.direccionEntrega.trim()
        }

        if (destino.isBlank()) {
            Toast.makeText(
                this,
                "Este pedido no tiene dirección",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val uri = Uri.parse(
            "https://www.google.com/maps/dir/"
        ).buildUpon()
            .appendQueryParameter("api", "1")
            .appendQueryParameter("destination", destino)
            .appendQueryParameter("travelmode", "driving")
            .appendQueryParameter("dir_action", "navigate")
            .build()

        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                this,
                "No se encontró una app de mapas",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =====================================
    // CONFIRMAR INICIO O FINALIZACIÓN
    // =====================================

    private fun confirmarMovimiento(
        pedidoId: Long,
        iniciar: Boolean
    ) {

        if (procesandoMovimiento) return

        val titulo = if (iniciar) {
            "Iniciar entrega"
        } else {
            "Finalizar entrega"
        }

        val mensaje = if (iniciar) {
            "¿Confirmas que comenzarás la entrega " +
                    "del pedido #$pedidoId?"
        } else {
            "¿Confirmas que el pedido #$pedidoId " +
                    "ya fue entregado al cliente?"
        }

        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar") { _, _ ->
                ejecutarMovimiento(pedidoId, iniciar)
            }
            .show()
    }

    // =====================================
    // CAMBIAR ESTADO EN RENDER
    // =====================================

    private fun ejecutarMovimiento(
        pedidoId: Long,
        iniciar: Boolean
    ) {

        if (procesandoMovimiento || cargando) return

        val usuario = auth.currentUser

        if (usuario == null) {
            Toast.makeText(
                this,
                "Debes iniciar sesión",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        procesandoMovimiento = true
        btnActualizar.isEnabled = false

        usuario.getIdToken(false)
            .addOnSuccessListener { resultado ->

                val token = resultado.token

                if (token.isNullOrBlank()) {
                    terminarMovimiento()
                    mostrarToast("No se pudo obtener el token")
                    return@addOnSuccessListener
                }

                lifecycleScope.launch {

                    try {
                        if (auth.currentUser?.uid != usuario.uid) {
                            throw IllegalStateException(
                                "La sesión cambió"
                            )
                        }

                        val respuesta = if (iniciar) {
                            RetrofitClient.apiService
                                .iniciarEntrega(
                                    "Bearer $token",
                                    pedidoId
                                )
                        } else {
                            RetrofitClient.apiService
                                .finalizarEntrega(
                                    "Bearer $token",
                                    pedidoId
                                )
                        }

                        if (!respuesta.isSuccessful) {

                            val error = when (respuesta.code()) {
                                401 -> "Sesión no válida"
                                403 -> "No eres el repartidor asignado"
                                404 -> "Entrega no encontrada"
                                409 -> "El estado no permite esta acción"
                                429 -> "Demasiadas solicitudes"
                                500 -> "Error interno del servidor"
                                else -> "Error HTTP ${respuesta.code()}"
                            }

                            throw IllegalStateException(error)
                        }

                        if (auth.currentUser?.uid != usuario.uid) {
                            throw IllegalStateException(
                                "La sesión cambió durante la operación"
                            )
                        }


                        if (iniciar) {

                            mostrarToast("Entrega iniciada correctamente")

                            // No activar el GPS sin consentimiento.
                            AlertDialog.Builder(this@MisEntregasActivity)
                                .setTitle("Compartir ubicación")
                                .setMessage(
                                    "¿Quieres compartir tu ubicación durante " +
                                            "la entrega del pedido #$pedidoId? " +
                                            "Podrás detenerla desde la notificación."
                                )
                                .setNegativeButton("Ahora no", null)
                                .setPositiveButton("Compartir GPS") { _, _ ->
                                    solicitarComparticionGps(pedidoId)
                                }
                                .show()

                        } else {

                            // La finalización ya fue aceptada por Render.
                            stopService(
                                Intent(
                                    this@MisEntregasActivity,
                                    EntregaLocationService::class.java
                                )
                            )

                            mostrarToast("Entrega finalizada correctamente")
                        }
                        mostrarToast(
                            if (iniciar) {
                                "Entrega iniciada correctamente"
                            } else {
                                "Entrega finalizada correctamente"
                            }
                        )

                    } catch (e: Exception) {
                        mostrarToast(
                            e.localizedMessage ?: "Error de conexión"
                        )

                    } finally {
                        terminarMovimiento()
                        cargarMisEntregas()
                    }
                }
            }
            .addOnFailureListener { error ->
                terminarMovimiento()

                mostrarToast(
                    error.localizedMessage
                        ?: "No se pudo validar Firebase"
                )
            }
    }

    private fun solicitarComparticionGps(pedidoId: Long) {

        val tienePermiso =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

        if (tienePermiso) {
            iniciarComparticionGps(pedidoId)
        } else {
            pedidoGpsPendiente = pedidoId

            solicitarPermisoGps.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun iniciarComparticionGps(pedidoId: Long) {

        val intent = Intent(
            this,
            EntregaLocationService::class.java
        ).apply {
            putExtra(
                EntregaLocationService.EXTRA_PEDIDO_ID,
                pedidoId
            )
        }

        try {
            ContextCompat.startForegroundService(
                this,
                intent
            )

            mostrarToast(
                "Iniciando seguimiento GPS del pedido #$pedidoId"
            )

        } catch (e: Exception) {
            mostrarToast(
                "No se pudo iniciar GPS: " +
                        (e.localizedMessage ?: "Error desconocido")
            )
        }
    }

    private fun terminarMovimiento() {
        procesandoMovimiento = false
        btnActualizar.isEnabled = true
    }

    private fun mostrarToast(mensaje: String) {
        Toast.makeText(
            this,
            mensaje,
            Toast.LENGTH_LONG
        ).show()
    }
}
