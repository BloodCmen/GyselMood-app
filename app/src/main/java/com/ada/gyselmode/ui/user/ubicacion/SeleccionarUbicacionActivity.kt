package com.ada.gyselmode.ui.user.ubicacion

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ada.gyselmode.databinding.ActivitySeleccionarUbicacionBinding
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap



import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale

class SeleccionarUbicacionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySeleccionarUbicacionBinding

    private var mapa: MapLibreMap? = null
    private var marcador: Marker? = null
    private var ubicacionSeleccionada: LatLng? = null

    // Centro aproximado de Trujillo, Perú
    private val centroTrujillo = LatLng(
        -8.1116,
        -79.0287
    )


    private val clienteUbicacion by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private val permisoUbicacion = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->

        val permitido =
            permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (permitido) {
            obtenerUbicacionActual()
        } else {
            Toast.makeText(
                this,
                "Puedes seleccionar tu ubicación manualmente",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun solicitarUbicacion() {

        val permisoPreciso = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val permisoAproximado = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (permisoPreciso || permisoAproximado) {
            obtenerUbicacionActual()
        } else {
            permisoUbicacion.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun obtenerUbicacionActual() {

        val preciso = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val aproximado = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!preciso && !aproximado) return

        val prioridad = if (preciso) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        val cancelacion = CancellationTokenSource()

        clienteUbicacion.getCurrentLocation(
            prioridad,
            cancelacion.token
        ).addOnSuccessListener { ubicacion ->

            if (ubicacion == null) {
                Toast.makeText(
                    this,
                    "No se pudo obtener tu ubicación. Activa el GPS o elige un punto manualmente.",
                    Toast.LENGTH_LONG
                ).show()
                return@addOnSuccessListener
            }

            val punto = LatLng(
                ubicacion.latitude,
                ubicacion.longitude
            )

            mapa?.let { mapaActual ->

                mapaActual.cameraPosition =
                    CameraPosition.Builder()
                        .target(punto)
                        .zoom(17.0)
                        .build()

                seleccionarPunto(mapaActual, punto)
            }
        }.addOnFailureListener {

            Toast.makeText(
                this,
                "No se pudo obtener la ubicación",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar MapLibre antes de cargar el diseño.
        MapLibre.getInstance(this)

        binding = ActivitySeleccionarUbicacionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState != null &&
            savedInstanceState.containsKey("LATITUD") &&
            savedInstanceState.containsKey("LONGITUD")
        ) {
            savedLatitud = savedInstanceState.getDouble("LATITUD")
            savedLongitud = savedInstanceState.getDouble("LONGITUD")
        }

        // Inicializar el ciclo de vida del mapa.
        binding.mapView.onCreate(savedInstanceState)

        binding.btnVolver.setOnClickListener {
            finish()
        }

        binding.btnConfirmarUbicacion.setOnClickListener {
            confirmarUbicacion()
        }

        binding.btnMiUbicacion.setOnClickListener {
            solicitarUbicacion()
        }

        iniciarMapa()
    }

    private fun iniciarMapa() {

        binding.mapView.getMapAsync { mapaListo ->

            mapa = mapaListo

            // Mapa gratuito de OpenFreeMap
            mapaListo.setStyle(
                "https://tiles.openfreemap.org/styles/liberty"
            ) {

                // Ubicar la cámara en Trujillo
                mapaListo.cameraPosition =
                    CameraPosition.Builder()
                        .target(centroTrujillo)
                        .zoom(14.0)
                        .build()

                // Recuperar una ubicación tras rotar la pantalla
                val latGuardada = savedLatitud
                val lonGuardada = savedLongitud

                if (latGuardada != null && lonGuardada != null) {
                    seleccionarPunto(
                        mapaListo,
                        LatLng(latGuardada, lonGuardada)
                    )
                }

                // Cuando el cliente toca el mapa
                mapaListo.addOnMapClickListener { punto ->

                    seleccionarPunto(mapaListo, punto)

                    true
                }
                solicitarUbicacion()
            }
        }
    }

    private var savedLatitud: Double? = null
    private var savedLongitud: Double? = null

    private fun seleccionarPunto(
        mapaLibre: MapLibreMap,
        punto: LatLng
    ) {

        ubicacionSeleccionada = punto

        // Retirar el marcador anterior
        marcador?.let { mapaLibre.removeMarker(it) }

        // Agregar el nuevo marcador
        marcador = mapaLibre.addMarker(
            MarkerOptions()
                .position(punto)
                .title("Entrega GyselMood")
                .snippet("Ubicación seleccionada")
        )

        binding.txtUbicacionSeleccionada.text =
            "Latitud: %.6f\nLongitud: %.6f".format(
                Locale.US,
                punto.latitude,
                punto.longitude
            )
    }

    private fun confirmarUbicacion() {

        val punto = ubicacionSeleccionada

        if (punto == null) {
            Toast.makeText(
                this,
                "Primero toca un punto del mapa",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Devolver las coordenadas a la pantalla anterior
        val resultado = Intent().apply {

            putExtra("LATITUD_ENTREGA", punto.latitude)

            putExtra("LONGITUD_ENTREGA", punto.longitude)
        }

        setResult(RESULT_OK, resultado)

        Toast.makeText(
            this,
            "Ubicación seleccionada correctamente",
            Toast.LENGTH_SHORT
        ).show()

        finish()
    }

    // CICLO DE VIDA DE MAPLIBRE

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        binding.mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        binding.mapView.onStop()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        ubicacionSeleccionada?.let {
            outState.putDouble("LATITUD", it.latitude)
            outState.putDouble("LONGITUD", it.longitude)
        }

        binding.mapView.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        binding.mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }
}
