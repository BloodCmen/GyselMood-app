package com.ada.gyselmode.ui.user.entregas


import android.Manifest
import android.R
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.ada.gyselmode.network.RetrofitClient
import com.ada.gyselmode.model.entrega.UbicacionEntregaRequest
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*

class EntregaLocationService : Service() {

    companion object {
        const val EXTRA_PEDIDO_ID = "pedido_id"
        const val ACTION_DETENER = "DETENER_GPS_ENTREGA"

        private const val CANAL_ID = "gyselmood_delivery_gps"
        private const val NOTIFICACION_ID = 2101
    }

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    private lateinit var fusedClient: FusedLocationProviderClient
    private var callback: LocationCallback? = null

    private var pedidoActivo: Long = -1
    private var uidActivo: String? = null
    private var compartiendo = false
    private var enviando = false

    override fun onCreate() {
        super.onCreate()

        fusedClient = LocationServices
            .getFusedLocationProviderClient(this)

        crearCanal()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (intent?.action == ACTION_DETENER) {
            stopSelf()
            return START_NOT_STICKY
        }

        val pedidoId = intent?.getLongExtra(
            EXTRA_PEDIDO_ID, -1L
        ) ?: -1L

        val usuario = FirebaseAuth.getInstance().currentUser

        if (pedidoId <= 0 || usuario == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!tienePermisoUbicacion()) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (compartiendo && pedidoActivo == pedidoId) {
            return START_NOT_STICKY
        }

        if (compartiendo) {
            detenerActualizaciones()
        }

        pedidoActivo = pedidoId
        uidActivo = usuario.uid

        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICACION_ID,
                crearNotificacion(pedidoId),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )

            iniciarActualizaciones()

        } catch (e: Exception) {
            Log.e(
                "EntregaGPS",
                "No se pudo iniciar el servicio GPS",
                e
            )
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun tienePermisoUbicacion(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun iniciarActualizaciones() {

        val solicitud = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10_000L
        )
            .setMinUpdateIntervalMillis(5_000L)
            .setMinUpdateDistanceMeters(0f)
            .build()

        val nuevoCallback = object : LocationCallback() {
            override fun onLocationResult(
                resultado: LocationResult
            ) {
                val ubicacion = resultado.lastLocation ?: return

                Log.d(
                    "EntregaGPS",
                    "Ubicación recibida: lat=${ubicacion.latitude}, lng=${ubicacion.longitude}"
                )

                if (!compartiendo || enviando) return

                if (!ubicacion.latitude.isFinite() ||
                    !ubicacion.longitude.isFinite()
                ) return

                enviarUbicacion(
                    ubicacion.latitude,
                    ubicacion.longitude
                )
            }
        }

        callback = nuevoCallback
        compartiendo = true

        try {
            fusedClient.requestLocationUpdates(
                solicitud,
                nuevoCallback,
                Looper.getMainLooper()
            ).addOnFailureListener { error ->
                Log.e(
                    "EntregaGPS",
                    "No se pudo obtener ubicación",
                    error
                )
                stopSelf()
            }

        } catch (e: SecurityException) {
            stopSelf()
        }
    }

    private fun enviarUbicacion(
        latitud: Double,
        longitud: Double
    ) {
        if (enviando || !compartiendo) return

        val usuario = FirebaseAuth.getInstance().currentUser

        if (usuario == null || usuario.uid != uidActivo) {
            stopSelf()
            return
        }

        enviando = true

        usuario.getIdToken(false)
            .addOnSuccessListener { resultado ->

                val token = resultado.token

                if (token.isNullOrBlank() || !compartiendo) {
                    enviando = false
                    return@addOnSuccessListener
                }

                val id = pedidoActivo

                scope.launch {
                    try {
                        val respuesta =
                            RetrofitClient.apiService
                                .actualizarUbicacionEntrega(
                                    "Bearer $token",
                                    id,
                                    UbicacionEntregaRequest(
                                        latitud,
                                        longitud
                                    )
                                )

                        if (!respuesta.isSuccessful) {
                            Log.w(
                                "EntregaGPS",
                                "Error HTTP ${respuesta.code()}"
                            )

                            // El reparto terminó, fue cancelado
                            // o la cuenta no está autorizada.
                            if (respuesta.code() in
                                listOf(401, 403, 404, 409)
                            ) {
                                stopSelf()
                            }
                        }

                    } catch (e: Exception) {
                        // Si se corta Internet, el siguiente
                        // punto intentará enviarse normalmente.
                        Log.e(
                            "EntregaGPS",
                            "Error enviando GPS",
                            e
                        )
                    } finally {
                        enviando = false
                    }
                }
            }
            .addOnFailureListener { error ->
                enviando = false
                Log.e(
                    "EntregaGPS",
                    "No se pudo obtener token Firebase",
                    error
                )
                stopSelf()
            }
    }

    private fun crearCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_ID,
                "Seguimiento de entregas",
                NotificationManager.IMPORTANCE_LOW
            )

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(canal)
        }
    }

    private fun crearNotificacion(pedidoId: Long): Notification {

        val detener = Intent(
            this,
            EntregaLocationService::class.java
        ).apply {
            action = ACTION_DETENER
        }

        val pendingDetener = PendingIntent.getService(
            this,
            0,
            detener,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CANAL_ID)
            .setSmallIcon(R.drawable.ic_menu_mylocation)
            .setContentTitle("GyselMood · Entrega activa")
            .setContentText(
                "Compartiendo ubicación del pedido #$pedidoId"
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                R.drawable.ic_media_pause,
                "Dejar de compartir",
                pendingDetener
            )
            .build()
    }

    private fun detenerActualizaciones() {
        compartiendo = false
        callback?.let {
            fusedClient.removeLocationUpdates(it)
        }
        callback = null
    }

    override fun onDestroy() {
        detenerActualizaciones()
        scope.cancel()
        super.onDestroy()
    }
}
