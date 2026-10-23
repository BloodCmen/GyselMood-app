package com.ada.gyselmode.ui.admin.maintenance

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ada.gyselmode.R
import com.ada.gyselmode.ui.admin.maintenance.adapter.ProductoAdminAdapter
import com.ada.gyselmode.helper.NavigationHelper
import com.ada.gyselmode.model.Producto
import com.ada.gyselmode.network.RetrofitClient
import com.ada.gyselmode.security.BaseSecureActivity
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class GestionarProductosActivity : BaseSecureActivity() {

    private lateinit var recyclerProductos: RecyclerView
    private lateinit var edtBuscar: EditText
    private lateinit var contenedorFiltros: LinearLayout
    private lateinit var txtCantidad: TextView
    private lateinit var txtVacio: TextView
    private lateinit var progress: ProgressBar
    private lateinit var btnOrdenar: TextView

    private var productosOriginales = emptyList<Producto>()
    private var categoriaSeleccionada: String? = null
    private var ordenSeleccionado = 0
    private var pantallaPreparada = false
    private var cargando = false
    private var filtrosCategoria = emptyList<Pair<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_gestionar_productos)

        NavigationHelper.setupBottomNavigation(this)

        findViewById<TextView>(R.id.btnVolver).setOnClickListener {
            finish()
        }

        recyclerProductos = findViewById(R.id.recyclerProductos)
        edtBuscar = findViewById(R.id.edtBuscarProducto)
        contenedorFiltros =
            findViewById(R.id.contenedorFiltrosCategoria)
        txtCantidad = findViewById(R.id.txtCantidadProductos)
        txtVacio = findViewById(R.id.txtVacioProductos)
        progress = findViewById(R.id.progressProductosAdmin)
        btnOrdenar = findViewById(R.id.btnOrdenarProductos)

        recyclerProductos.layoutManager = LinearLayoutManager(this)
        recyclerProductos.setHasFixedSize(false)

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
                    aplicarFiltros()
                }

                override fun afterTextChanged(
                    s: Editable?
                ) = Unit
            }
        )

        btnOrdenar.setOnClickListener {
            mostrarOpcionesOrden()
        }

        pantallaPreparada = true
        crearChipsCategorias()
    }

    override fun onResume() {
        super.onResume()

        if (pantallaPreparada && !cargando) {
            cargarProductos()
        }
    }

    private fun cargarProductos() {
        if (cargando) return

        lifecycleScope.launch {
            cargando = true
            progress.visibility = View.VISIBLE
            txtVacio.visibility = View.GONE

            try {
                productosOriginales =
                    RetrofitClient.apiService.listarProductosActivos()

                // Categorías disponibles desde Spring Boot.
                try {
                    val categorias =
                        RetrofitClient.apiService.listarCategorias()

                    filtrosCategoria = categorias.map {
                        it.id.toString() to it.nombre
                    }
                    crearChipsCategorias()

                } catch (_: Exception) {
                    // La lista de productos sigue funcionando
                    // aunque fallen los filtros por categoría.
                }

                aplicarFiltros()

            } catch (e: Exception) {
                Toast.makeText(
                    this@GestionarProductosActivity,
                    "Error al cargar productos: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                if (productosOriginales.isEmpty()) {
                    txtCantidad.text = "No se pudieron cargar productos"
                    txtVacio.text =
                        "No se pudo conectar con el catálogo."
                    txtVacio.visibility = View.VISIBLE
                }
            } finally {
                cargando = false
                progress.visibility = View.GONE
            }
        }
    }

    private fun aplicarFiltros() {
        if (!pantallaPreparada) return

        val busqueda =
            edtBuscar.text.toString().trim()

        var filtrados = productosOriginales.filter { producto ->

            val coincideNombre = producto.titulo.contains(
                busqueda,
                ignoreCase = true
            )

            val coincideCategoria =
                categoriaSeleccionada == null ||
                        producto.categoriaId.toString() == categoriaSeleccionada

            coincideNombre && coincideCategoria
        }

        filtrados = when (ordenSeleccionado) {
            1 -> filtrados.sortedBy { it.titulo.lowercase() }
            2 -> filtrados.sortedByDescending { it.titulo.lowercase() }
            3 -> filtrados.sortedBy {
                it.precioTotal.toString().toDoubleOrNull()
                    ?: Double.MAX_VALUE
            }
            4 -> filtrados.sortedByDescending {
                it.precioTotal.toString().toDoubleOrNull()
                    ?: Double.MIN_VALUE
            }
            else -> filtrados
        }

        txtCantidad.text =
            "${filtrados.size} de ${productosOriginales.size} productos"

        txtVacio.visibility =
            if (filtrados.isEmpty() && !cargando) {
                View.VISIBLE
            } else {
                View.GONE
            }

        if (filtrados.isEmpty()) {
            txtVacio.text =
                "No encontramos productos con esos filtros."
        }

        recyclerProductos.adapter =
            ProductoAdminAdapter(filtrados) { producto ->

                val intent = Intent(
                    this@GestionarProductosActivity,
                    EditarProductoActivity::class.java
                )

                intent.putExtra("producto", producto)
                startActivity(intent)
            }
    }

    private fun crearChipsCategorias() {
        contenedorFiltros.removeAllViews()

        agregarChip("Todos", null)

        filtrosCategoria.forEach { (id, nombre) ->
            agregarChip(nombre, id)
        }
    }

    private fun agregarChip(nombre: String, id: String?) {
        val seleccionado = categoriaSeleccionada == id

        val chip = MaterialButton(
            this,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = nombre
            isAllCaps = false
            textSize = 12f
            minHeight = 0
            minimumHeight = 0
            cornerRadius = dp(24)
            insetTop = 0
            insetBottom = 0

            setTextColor(
                if (seleccionado) Color.WHITE
                else Color.rgb(220, 220, 225)
            )

            backgroundTintList = ColorStateList.valueOf(
                if (seleccionado) getColor(R.color.gysel_red)
                else Color.rgb(43, 43, 48)
            )

            strokeWidth = 0

            setOnClickListener {
                categoriaSeleccionada = id
                crearChipsCategorias()
                aplicarFiltros()
            }
        }

        contenedorFiltros.addView(
            chip,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(40)
            ).apply {
                marginEnd = dp(8)
            }
        )
    }

    private fun mostrarOpcionesOrden() {
        val menu = PopupMenu(this, btnOrdenar)

        menu.menu.add(0, 0, 0, "Orden predeterminado")
        menu.menu.add(0, 1, 1, "Nombre: A - Z")
        menu.menu.add(0, 2, 2, "Nombre: Z - A")
        menu.menu.add(0, 3, 3, "Precio: menor a mayor")
        menu.menu.add(0, 4, 4, "Precio: mayor a menor")

        menu.setOnMenuItemClickListener { item ->
            ordenSeleccionado = item.itemId
            aplicarFiltros()
            true
        }

        menu.show()
    }

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }
}
