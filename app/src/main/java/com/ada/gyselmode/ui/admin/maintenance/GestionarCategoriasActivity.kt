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
import com.ada.gyselmode.model.CategoriaCreateRequest
import com.ada.gyselmode.network.RetrofitClient
import com.ada.gyselmode.security.AdminGuard
import com.ada.gyselmode.security.BaseSecureActivity
import kotlinx.coroutines.launch
import java.util.Locale

class GestionarCategoriasActivity : BaseSecureActivity() {

    private lateinit var edtNombre: EditText
    private lateinit var edtBuscar: EditText
    private lateinit var btnGuardar: Button
    private lateinit var contenedor: LinearLayout
    private lateinit var txtEstado: TextView
    private lateinit var txtContador: TextView
    private lateinit var progress: ProgressBar

    private var categorias = emptyList<Pair<Long, String>>()
    private var ocupado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Navbar inferior
        NavigationHelper.setupBottomNavigation(this)

        // Verificar acceso antes de mostrar la administración.
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
            "Gestionar categorías"

        findViewById<TextView>(R.id.txtTituloFormulario).text =
            "Nueva categoría"

        findViewById<TextView>(R.id.txtDescripcionFormulario).text =
            "Organiza las colecciones de tu catálogo."

        findViewById<TextView>(R.id.txtTituloLista).text =
            "Categorías registradas"

        edtNombre.hint = "Nombre de la categoría"
        edtBuscar.hint = "Buscar categoría..."
        btnGuardar.text = "+  GUARDAR CATEGORÍA"

        btnGuardar.setOnClickListener {
            crearCategoria()
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
                    mostrarCategorias()
                }

                override fun afterTextChanged(
                    s: Editable?
                ) = Unit
            }
        )

        cargarCategorias()
    }

    private fun cargarCategorias() {
        lifecycleScope.launch {
            progress.visibility = View.VISIBLE
            txtEstado.visibility = View.GONE

            try {
                val respuesta =
                    RetrofitClient.apiService.listarCategorias()

                categorias = respuesta.map {
                    it.id.toLong() to it.nombre
                }

                mostrarCategorias()

            } catch (e: Exception) {
                txtEstado.text =
                    "No se pudieron cargar las categorías.\n" +
                            "Comprueba tu conexión."
                txtEstado.visibility = View.VISIBLE
                contenedor.removeAllViews()
                txtContador.text = "Error al cargar"
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    private fun mostrarCategorias() {
        if (!::contenedor.isInitialized) return

        val busqueda = edtBuscar.text.toString().trim()

        val filtradas = categorias.filter {
            it.second.contains(busqueda, ignoreCase = true)
        }

        contenedor.removeAllViews()

        txtContador.text =
            "${filtradas.size} de ${categorias.size} categorías"

        txtEstado.visibility =
            if (filtradas.isEmpty()) View.VISIBLE else View.GONE

        if (filtradas.isEmpty()) {
            txtEstado.text = if (busqueda.isEmpty()) {
                "Todavía no hay categorías registradas."
            } else {
                "No encontramos categorías con ese nombre."
            }
        }

        filtradas.forEach { (id, nombre) ->
            agregarFila(id, nombre)
        }
    }

    private fun crearCategoria() {
        if (ocupado) return

        val nombre = edtNombre.text.toString().trim()

        if (nombre.isBlank()) {
            edtNombre.error = "Escribe el nombre de la categoría"
            edtNombre.requestFocus()
            return
        }

        val codigo = nombre.uppercase(Locale.ROOT)
            .replace(Regex("\\s+"), "_")

        lifecycleScope.launch {
            cambiarOcupado(true)

            try {
                RetrofitClient.apiService.crearCategoria(
                    CategoriaCreateRequest(
                        codigo = codigo,
                        nombre = nombre
                    )
                )

                Toast.makeText(
                    this@GestionarCategoriasActivity,
                    "Categoría creada correctamente",
                    Toast.LENGTH_SHORT
                ).show()

                edtNombre.text.clear()
                edtBuscar.text.clear()

                cargarCategorias()

            } catch (e: Exception) {
                Toast.makeText(
                    this@GestionarCategoriasActivity,
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
            .setTitle("¿Eliminar categoría?")
            .setMessage(
                "Se eliminará \"$nombre\".\n\n" +
                        "Comprueba que no tenga productos asociados."
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarCategoria(id)
            }
            .show()
    }

    private fun eliminarCategoria(id: Long) {
        lifecycleScope.launch {
            cambiarOcupado(true)

            try {
                RetrofitClient.apiService.eliminarCategoria(id)

                Toast.makeText(
                    this@GestionarCategoriasActivity,
                    "Categoría eliminada",
                    Toast.LENGTH_SHORT
                ).show()

                cargarCategorias()

            } catch (e: Exception) {
                Toast.makeText(
                    this@GestionarCategoriasActivity,
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
            textSize = 15f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            maxLines = 2

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
            backgroundTintList =
                ColorStateList.valueOf(
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
