package com.ada.gyselmode.ui.admin.maintenance

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.ada.gyselmode.R
import com.ada.gyselmode.helper.NavigationHelper
import com.ada.gyselmode.model.TallaCreateRequest
import com.ada.gyselmode.network.RetrofitClient
import com.ada.gyselmode.security.AdminGuard
import com.ada.gyselmode.security.BaseSecureActivity
import kotlinx.coroutines.launch

class GestionarTallasActivity : BaseSecureActivity() {

    private lateinit var edtNombre: EditText
    private lateinit var edtBuscar: EditText
    private lateinit var btnGuardar: Button
    private lateinit var contenedor: LinearLayout
    private lateinit var txtEstado: TextView
    private lateinit var txtContador: TextView
    private lateinit var progress: ProgressBar

    private var tallas = emptyList<Pair<Long, String>>()
    private var ocupado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Navbar inferior
        NavigationHelper.setupBottomNavigation(this)

        AdminGuard.verificarAdmin(this) {
            if (!isFinishing && !isDestroyed) {
                inicializarPantalla()
            }
        }
    }

    private fun inicializarPantalla() {
        setContentView(R.layout.activity_gestionar_catalogo_simple)

        NavigationHelper.setupBackButton(this)

        edtNombre = findViewById(R.id.edtNombreElemento)
        edtBuscar = findViewById(R.id.edtBuscarElemento)
        btnGuardar = findViewById(R.id.btnGuardarElemento)
        contenedor = findViewById(R.id.contenedorElementos)
        txtEstado = findViewById(R.id.txtEstadoLista)
        txtContador = findViewById(R.id.txtContadorElementos)
        progress = findViewById(R.id.progressGestion)

        findViewById<TextView>(R.id.txtTituloGestion).text =
            "Gestionar tallas"

        findViewById<TextView>(R.id.txtTituloFormulario).text =
            "Nueva talla"

        findViewById<TextView>(R.id.txtDescripcionFormulario).text =
            "Agrega las tallas disponibles para tus prendas."

        findViewById<TextView>(R.id.txtTituloLista).text =
            "Tallas registradas"

        edtNombre.hint = "Ej.: S, M, L, XL..."
        edtBuscar.hint = "Buscar talla..."
        btnGuardar.text = "+  GUARDAR TALLA"

        btnGuardar.setOnClickListener {
            crearTalla()
        }

        edtBuscar.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int,
                    count: Int, after: Int
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?, start: Int,
                    before: Int, count: Int
                ) {
                    mostrarTallas()
                }

                override fun afterTextChanged(
                    s: Editable?
                ) = Unit
            }
        )

        cargarTallas()
    }

    private fun cargarTallas() {
        lifecycleScope.launch {
            progress.visibility = View.VISIBLE
            txtEstado.visibility = View.GONE

            try {
                val respuesta =
                    RetrofitClient.apiService.listarTallas()

                tallas = respuesta.map {
                    it.id.toLong() to it.nombre
                }

                mostrarTallas()

            } catch (e: Exception) {
                txtEstado.text =
                    "No se pudieron cargar las tallas.\n" +
                            "Comprueba tu conexión."
                txtEstado.visibility = View.VISIBLE
                contenedor.removeAllViews()
                txtContador.text = "Error al cargar"
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    private fun mostrarTallas() {
        if (!::contenedor.isInitialized) return

        val busqueda = edtBuscar.text.toString().trim()

        val filtradas = tallas.filter {
            it.second.contains(busqueda, ignoreCase = true)
        }

        contenedor.removeAllViews()

        txtContador.text =
            "${filtradas.size} de ${tallas.size} tallas"

        txtEstado.visibility =
            if (filtradas.isEmpty()) View.VISIBLE else View.GONE

        if (filtradas.isEmpty()) {
            txtEstado.text = if (busqueda.isEmpty()) {
                "Todavía no hay tallas registradas."
            } else {
                "No encontramos tallas con ese nombre."
            }
        }

        filtradas.forEach { (id, nombre) ->
            agregarFila(id, nombre)
        }
    }

    private fun crearTalla() {
        if (ocupado) return

        val nombre = edtNombre.text.toString().trim()
            .uppercase()

        if (nombre.isBlank()) {
            edtNombre.error = "Escribe el nombre de la talla"
            edtNombre.requestFocus()
            return
        }

        lifecycleScope.launch {
            cambiarOcupado(true)

            try {
                RetrofitClient.apiService.crearTalla(
                    TallaCreateRequest(nombre = nombre)
                )

                Toast.makeText(
                    this@GestionarTallasActivity,
                    "Talla creada correctamente",
                    Toast.LENGTH_SHORT
                ).show()

                edtNombre.text.clear()
                edtBuscar.text.clear()

                cargarTallas()

            } catch (e: Exception) {
                Toast.makeText(
                    this@GestionarTallasActivity,
                    "No se pudo crear: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                cambiarOcupado(false)
            }
        }
    }

    private fun confirmarEliminar(id: Long, nombre: String) {
        if (ocupado) return

        AlertDialog.Builder(this)
            .setTitle("¿Eliminar talla?")
            .setMessage(
                "¿Deseas eliminar la talla \"$nombre\"?\n\n" +
                        "Si está asociada a productos, " +
                        "el backend podría impedir su eliminación."
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarTalla(id)
            }
            .show()
    }

    private fun eliminarTalla(id: Long) {
        lifecycleScope.launch {
            cambiarOcupado(true)

            try {
                RetrofitClient.apiService.eliminarTalla(id)

                Toast.makeText(
                    this@GestionarTallasActivity,
                    "Talla eliminada correctamente",
                    Toast.LENGTH_SHORT
                ).show()

                cargarTallas()

            } catch (e: Exception) {
                Toast.makeText(
                    this@GestionarTallasActivity,
                    "No se pudo eliminar. Puede estar en uso.",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                cambiarOcupado(false)
            }
        }
    }

    private fun cambiarOcupado(valor: Boolean) {
        ocupado = valor
        btnGuardar.isEnabled = !valor
        btnGuardar.alpha = if (valor) 0.6f else 1f
    }

    private fun agregarFila(id: Long, nombre: String) {
        val tarjeta = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(15), dp(12), dp(12), dp(12))

            background = GradientDrawable().apply {
                setColor(Color.rgb(36, 36, 41))
                cornerRadius = dp(15).toFloat()
                setStroke(dp(1), Color.rgb(57, 57, 64))
            }
        }

        val texto = TextView(this).apply {
            text = nombre
            textSize = 16f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val eliminar = Button(this).apply {
            text = "Eliminar"
            textSize = 12f
            isAllCaps = false
            minWidth = 0
            minimumWidth = 0
            setTextColor(Color.rgb(255, 150, 150))
            backgroundTintList = ColorStateList.valueOf(
                Color.rgb(65, 43, 49)
            )
            setOnClickListener {
                confirmarEliminar(id, nombre)
            }
        }

        tarjeta.addView(texto)
        tarjeta.addView(eliminar)

        contenedor.addView(
            tarjeta,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
        )
    }

    private fun dp(valor: Int): Int =
        (valor * resources.displayMetrics.density).toInt()
}
