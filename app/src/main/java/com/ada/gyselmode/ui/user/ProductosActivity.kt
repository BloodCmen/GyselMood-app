package com.ada.gyselmode.ui.user

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ada.gyselmode.R
import com.ada.gyselmode.ui.admin.maintenance.adapter.ProductAdapter
import com.ada.gyselmode.data.local.GyselMoodDatabase
import com.ada.gyselmode.data.local.entity.ProductoEntity
import com.ada.gyselmode.helper.NavigationHelper
import com.ada.gyselmode.model.Producto
import com.ada.gyselmode.network.RetrofitClient
import com.ada.gyselmode.ui.user.pedidos.DetalleProductoUsuarioActivity
import com.ada.gyselmode.utils.ConexionUtils
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ProductosActivity : AppCompatActivity() {

    private lateinit var recyclerProductos: RecyclerView
    private lateinit var edtBuscar: EditText
    private lateinit var layoutCategorias: LinearLayout
    private lateinit var btnTalla: TextView
    private lateinit var btnOrdenar: TextView
    private lateinit var txtResultados: TextView
    private lateinit var txtEstado: TextView
    private lateinit var progress: ProgressBar
    private lateinit var btnBorrarBusqueda: View

    private val gson = Gson()

    private val productoDao by lazy {
        GyselMoodDatabase
            .obtenerInstancia(applicationContext)
            .productoDao()
    }

    private var todosLosProductos: List<Producto> = emptyList()

    private var filtroCategoria: String? = null
    private var filtroTalla: String? = null
    private var filtroBusqueda: String = ""

    private var ordenSeleccionado = "RELEVANCIA"
    private var catalogoCargado = false

    private val rojo = Color.parseColor("#A52C36")
    private val gris = Color.parseColor("#29292E")
    private val blanco = Color.WHITE

    companion object {
        private const val KEY_CATEGORIA = "catalogo_categoria"
        private const val KEY_TALLA = "catalogo_talla"
        private const val KEY_BUSQUEDA = "catalogo_busqueda"
        private const val KEY_ORDEN = "catalogo_orden"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_products)
        NavigationHelper.setupBottomNavigation(this)

        inicializarVistas()

        filtroCategoria = savedInstanceState
            ?.getString(KEY_CATEGORIA)
            ?: intent.getStringExtra("FILTRO_CATEGORIA")

        filtroTalla = savedInstanceState
            ?.getString(KEY_TALLA)

        filtroBusqueda = savedInstanceState
            ?.getString(KEY_BUSQUEDA)
            ?: intent.getStringExtra("FILTRO_BUSQUEDA").orEmpty()

        ordenSeleccionado = savedInstanceState
            ?.getString(KEY_ORDEN)
            ?: "RELEVANCIA"

        configurarBuscador()
        configurarFiltros()

        edtBuscar.setText(filtroBusqueda)

        actualizarTextoTalla()
        actualizarTextoOrden()

        cargarProductos()
    }

    private fun inicializarVistas() {
        recyclerProductos = findViewById(R.id.recyclerProductos)
        edtBuscar = findViewById(R.id.edtBuscarCatalogo)
        layoutCategorias = findViewById(R.id.layoutCategoriasCatalogo)
        btnTalla = findViewById(R.id.btnFiltroTalla)
        btnOrdenar = findViewById(R.id.btnOrdenarCatalogo)
        txtResultados = findViewById(R.id.txtResultadosCatalogo)
        txtEstado = findViewById(R.id.txtEstadoCatalogo)
        progress = findViewById(R.id.progressCatalogo)

        btnBorrarBusqueda =
            findViewById(R.id.btnLimpiarBusquedaCatalogo)

        recyclerProductos.layoutManager =
            GridLayoutManager(this, 2)

        recyclerProductos.setHasFixedSize(false)
    }

    private fun configurarBuscador() {

        edtBuscar.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) = Unit

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                filtroBusqueda = s?.toString().orEmpty()

                btnBorrarBusqueda.visibility =
                    if (filtroBusqueda.isBlank()) View.GONE
                    else View.VISIBLE

                aplicarFiltros()
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        btnBorrarBusqueda.setOnClickListener {
            edtBuscar.text.clear()
        }
    }

    private fun configurarFiltros() {

        btnTalla.setOnClickListener {
            abrirMenuTallas()
        }

        btnOrdenar.setOnClickListener {
            abrirMenuOrdenamiento()
        }

        findViewById<TextView>(R.id.btnLimpiarFiltrosCatalogo)
            .setOnClickListener {
                filtroCategoria = null
                filtroTalla = null
                ordenSeleccionado = "RELEVANCIA"

                edtBuscar.text.clear()

                actualizarTextoTalla()
                actualizarTextoOrden()
                dibujarCategorias()
                aplicarFiltros()
            }
    }

    private fun cargarProductos() {

        lifecycleScope.launch {

            progress.visibility = View.VISIBLE

            // Primero recuperar Room
            try {

                val guardados = productoDao.obtenerProductos()
                    .map { entidad ->
                        gson.fromJson(
                            entidad.datosJson,
                            Producto::class.java
                        )
                    }
                    .filter { it.activo }

                if (guardados.isNotEmpty()) {
                    actualizarCatalogo(guardados)
                }

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                Log.e(
                    "GYSELMOOD_CATALOGO",
                    "Error al leer productos de Room",
                    e
                )
            }

            val modoOffline =
                intent.getBooleanExtra("MODO_OFFLINE", false)

            // Si no hay conexión, trabajamos con el catálogo local.
            if (!modoOffline &&
                ConexionUtils.hayInternet(this@ProductosActivity)
            ) {
                try {

                    val productos =
                        RetrofitClient.apiService
                            .listarProductosActivos()
                            .filter { it.activo }

                    val entidades = productos.map { producto ->
                        ProductoEntity(
                            id = producto.id,
                            datosJson = gson.toJson(producto)
                        )
                    }

                    productoDao.reemplazarCatalogo(entidades)

                    actualizarCatalogo(productos)

                } catch (e: CancellationException) {
                    throw e

                } catch (e: Exception) {

                    Log.e(
                        "GYSELMOOD_CATALOGO",
                        "Error actualizando desde Railway",
                        e
                    )

                    Toast.makeText(
                        this@ProductosActivity,
                        "No se pudo actualizar. Mostrando catálogo guardado.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            catalogoCargado = true
            progress.visibility = View.GONE
            aplicarFiltros()
        }
    }

    private fun actualizarCatalogo(productos: List<Producto>) {
        todosLosProductos = productos

        val categorias = productos
            .map { it.categoria }
            .toSet()

        if (filtroCategoria != null &&
            filtroCategoria !in categorias
        ) {
            filtroCategoria = null
        }

        dibujarCategorias()
        actualizarTextoTalla()
        aplicarFiltros()
    }

    private fun dibujarCategorias() {

        layoutCategorias.removeAllViews()

        agregarChip(
            nombre = "Todos",
            seleccionado = filtroCategoria == null
        ) {
            filtroCategoria = null
            dibujarCategorias()
            aplicarFiltros()
        }

        val categorias = todosLosProductos
            .map { it.categoria }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        categorias.forEach { categoria ->

            agregarChip(
                nombre = categoria,
                seleccionado = filtroCategoria == categoria
            ) {
                filtroCategoria = categoria
                dibujarCategorias()
                aplicarFiltros()
            }
        }
    }

    private fun agregarChip(
        nombre: String,
        seleccionado: Boolean,
        alSeleccionar: () -> Unit
    ) {

        val chip = TextView(this).apply {

            text = nombre
            textSize = 13f
            gravity = Gravity.CENTER

            setTextColor(blanco)

            setPadding(
                dp(17),
                dp(9),
                dp(17),
                dp(9)
            )

            setTypeface(
                null,
                if (seleccionado) Typeface.BOLD
                else Typeface.NORMAL
            )

            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(28).toFloat()

                setColor(
                    if (seleccionado) rojo else gris
                )
            }

            isClickable = true
            isFocusable = true

            setOnClickListener {
                alSeleccionar()
            }
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            dp(42)
        ).apply {
            marginEnd = dp(9)
        }

        layoutCategorias.addView(chip, params)
    }

    private fun abrirMenuTallas() {

        val menu = PopupMenu(this, btnTalla)

        menu.menu.add("Todas las tallas")

        val tallas = todosLosProductos
            .flatMap { producto -> producto.tallas }
            .map { talla -> talla.nombre }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        tallas.forEach {
            menu.menu.add(it)
        }

        menu.setOnMenuItemClickListener { item ->

            filtroTalla =
                if (item.title.toString() == "Todas las tallas") {
                    null
                } else {
                    item.title.toString()
                }

            actualizarTextoTalla()
            aplicarFiltros()
            true
        }

        menu.show()
    }

    private fun actualizarTextoTalla() {
        btnTalla.text =
            "Talla: ${filtroTalla ?: "Todas"}  ▾"
    }

    private fun abrirMenuOrdenamiento() {

        val opciones = listOf(
            "Relevancia",
            "Precio: menor a mayor",
            "Precio: mayor a menor",
            "Nombre: A - Z",
            "Nombre: Z - A",
            "Más recientes"
        )

        val menu = PopupMenu(this, btnOrdenar)

        opciones.forEachIndexed { index, texto ->
            menu.menu.add(0, index, index, texto)
        }

        menu.setOnMenuItemClickListener { item ->

            ordenSeleccionado = when (item.itemId) {
                1 -> "PRECIO_ASC"
                2 -> "PRECIO_DESC"
                3 -> "NOMBRE_ASC"
                4 -> "NOMBRE_DESC"
                5 -> "RECIENTES"
                else -> "RELEVANCIA"
            }

            actualizarTextoOrden()
            aplicarFiltros()
            true
        }

        menu.show()
    }

    private fun actualizarTextoOrden() {

        val nombre = when (ordenSeleccionado) {
            "PRECIO_ASC" -> "Precio ↑"
            "PRECIO_DESC" -> "Precio ↓"
            "NOMBRE_ASC" -> "Nombre A-Z"
            "NOMBRE_DESC" -> "Nombre Z-A"
            "RECIENTES" -> "Recientes"
            else -> "Relevancia"
        }

        btnOrdenar.text = "$nombre  ▾"
    }

    private fun aplicarFiltros() {

        val busqueda = filtroBusqueda.trim()

        val filtrados = todosLosProductos.filter { producto ->

            val categoriaCorrecta =
                filtroCategoria == null ||
                        producto.categoria.equals(
                            filtroCategoria,
                            ignoreCase = true
                        )

            val tallaCorrecta =
                filtroTalla == null ||
                        producto.tallas.any {
                            it.nombre.equals(
                                filtroTalla,
                                ignoreCase = true
                            )
                        }

            val busquedaCorrecta =
                busqueda.isBlank() ||
                        producto.titulo.contains(
                            busqueda,
                            ignoreCase = true
                        ) ||
                        producto.descripcion.contains(
                            busqueda,
                            ignoreCase = true
                        ) ||
                        producto.categoria.contains(
                            busqueda,
                            ignoreCase = true
                        )

            categoriaCorrecta &&
                    tallaCorrecta &&
                    busquedaCorrecta
        }

        val ordenados = when (ordenSeleccionado) {

            "PRECIO_ASC" ->
                filtrados.sortedBy { it.precioUnidad }

            "PRECIO_DESC" ->
                filtrados.sortedByDescending { it.precioUnidad }

            "NOMBRE_ASC" ->
                filtrados.sortedBy { it.titulo.lowercase() }

            "NOMBRE_DESC" ->
                filtrados.sortedByDescending { it.titulo.lowercase() }

            "RECIENTES" ->
                filtrados.sortedByDescending { it.id }

            else -> filtrados
        }

        txtResultados.text =
            "${ordenados.size} productos encontrados"

        txtEstado.visibility =
            if (catalogoCargado && ordenados.isEmpty())
                View.VISIBLE
            else
                View.GONE

        txtEstado.text =
            if (todosLosProductos.isEmpty()) {
                "Todavía no hay productos guardados o disponibles."
            } else {
                "No encontramos prendas que coincidan con tus filtros.\nPrueba otra categoría, talla o búsqueda."
            }

        recyclerProductos.adapter = ProductAdapter(
            productos = ordenados,
            onProductoClick = { producto ->

                val intentDetalle = Intent(
                    this,
                    DetalleProductoUsuarioActivity::class.java
                )

                intentDetalle.putExtra(
                    DetalleProductoUsuarioActivity.EXTRA_PRODUCTO,
                    producto
                )

                startActivity(intentDetalle)
            },
            esCatalogo = true
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {

        outState.putString(KEY_CATEGORIA, filtroCategoria)
        outState.putString(KEY_TALLA, filtroTalla)
        outState.putString(KEY_BUSQUEDA, filtroBusqueda)
        outState.putString(KEY_ORDEN, ordenSeleccionado)

        super.onSaveInstanceState(outState)
    }

    private fun dp(valor: Int): Int =
        (valor * resources.displayMetrics.density).toInt()
}