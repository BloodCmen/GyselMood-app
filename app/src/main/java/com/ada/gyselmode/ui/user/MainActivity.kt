package com.ada.gyselmode.ui.user

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.SystemClock
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.ada.gyselmode.R
import com.ada.gyselmode.ui.admin.maintenance.adapter.HomeBannerAdapter
import com.ada.gyselmode.ui.admin.maintenance.adapter.ProductAdapter
import com.ada.gyselmode.data.local.GyselMoodDatabase
import com.ada.gyselmode.data.local.entity.ProductoEntity
import com.ada.gyselmode.helper.NavigationHelper
import com.ada.gyselmode.helper.UsuarioFirestoreHelper
import com.ada.gyselmode.model.Producto
import com.ada.gyselmode.network.RetrofitClient
import com.ada.gyselmode.ui.user.pedidos.CartActivity
import com.ada.gyselmode.ui.user.pedidos.DetalleProductoUsuarioActivity
import com.ada.gyselmode.utils.ConexionUtils
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var edtBuscar: EditText
    private lateinit var btnLimpiar: ImageButton
    private lateinit var layoutCategorias: LinearLayout
    private lateinit var recyclerProductos: RecyclerView
    private lateinit var viewPager: ViewPager2
    private lateinit var txtIndicador: TextView
    private lateinit var txtContador: TextView
    private lateinit var txtEstado: TextView
    private lateinit var progress: View

    private val gson = Gson()

    private val productoDao by lazy {
        GyselMoodDatabase
            .obtenerInstancia(applicationContext)
            .productoDao()
    }

    private var todosLosProductos: List<Producto> = emptyList()
    private var categoriaSeleccionada: String? = null

    private val rojo = Color.parseColor("#B52B37")
    private val gris = Color.parseColor("#29292E")
    private val blanco = Color.WHITE

    // Carrusel automático
    private val intervaloCarrusel = 5000L
    private val pausaInteraccion = 8000L

    private var pausarCarruselHasta = 0L

    companion object {
        private const val KEY_BUSQUEDA_HOME = "busqueda_home"
        private const val KEY_CATEGORIA_HOME = "categoria_home"
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        NavigationHelper.setupBottomNavigation(this)

        // Sincronizar el nombre del usuario con Firestore.
        UsuarioFirestoreHelper.sincronizarNombre()

        inicializarVistas()


        // Recuperar la categoría seleccionada
        categoriaSeleccionada = savedInstanceState
            ?.getString(KEY_CATEGORIA_HOME)

        configurarBuscador()
        configurarAcciones()
        configurarCarrusel()

        // Recuperar la búsqueda anterior
        val busquedaGuardada = savedInstanceState
            ?.getString(KEY_BUSQUEDA_HOME)
            .orEmpty()

        if (busquedaGuardada.isNotEmpty()) {
            edtBuscar.setText(busquedaGuardada)
            edtBuscar.setSelection(edtBuscar.text.length)
        }

        cargarProductos()
    }


    private fun inicializarVistas() {
        edtBuscar = findViewById(R.id.edtBuscarHome)
        btnLimpiar = findViewById(R.id.btnLimpiarBusqueda)
        layoutCategorias = findViewById(R.id.layoutCategoriasHome)
        recyclerProductos = findViewById(R.id.recyclerProductosHome)
        viewPager = findViewById(R.id.viewPagerDestacados)
        txtIndicador = findViewById(R.id.txtIndicadorCarrusel)
        txtContador = findViewById(R.id.txtContadorHome)
        txtEstado = findViewById(R.id.txtEstadoHome)
        progress = findViewById(R.id.progressHome)

        recyclerProductos.layoutManager =
            GridLayoutManager(this, 2)

        recyclerProductos.isNestedScrollingEnabled = false
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
                btnLimpiar.visibility =
                    if (s.isNullOrEmpty()) View.GONE
                    else View.VISIBLE

                filtrarProductos()
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        btnLimpiar.setOnClickListener {
            edtBuscar.text.clear()
        }
    }

    private fun configurarAcciones() {

        findViewById<ImageButton>(R.id.btnCart)
            .setOnClickListener {
                startActivity(
                    Intent(this, CartActivity::class.java)
                )
            }

        findViewById<TextView>(R.id.btnVerTodos)
            .setOnClickListener {
                abrirCatalogo()
            }

        findViewById<TextView>(R.id.btnVerCategorias)
            .setOnClickListener {
                categoriaSeleccionada = null
                dibujarCategorias()
                filtrarProductos()
                abrirCatalogo()
            }
    }


    private fun configurarCarrusel() {

        viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {

                override fun onPageSelected(position: Int) {
                    actualizarIndicador(position)
                }

                override fun onPageScrollStateChanged(state: Int) {

                    // Cuando el usuario comienza a deslizar
                    if (state == ViewPager2.SCROLL_STATE_DRAGGING) {

                        pausarCarruselHasta =
                            SystemClock.elapsedRealtime() + pausaInteraccion
                    }
                }
            }
        )

        // Ejecutar solo mientras el Home esté visible
        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.STARTED) {

                while (true) {

                    delay(intervaloCarrusel)

                    val cantidad =
                        viewPager.adapter?.itemCount ?: 0

                    val tiempoActual =
                        SystemClock.elapsedRealtime()

                    val puedeAvanzar =
                        cantidad > 1 &&
                                tiempoActual >= pausarCarruselHasta &&
                                viewPager.scrollState ==
                                ViewPager2.SCROLL_STATE_IDLE &&
                                viewPager.isShown

                    if (puedeAvanzar) {

                        val siguiente =
                            (viewPager.currentItem + 1) % cantidad

                        viewPager.setCurrentItem(
                            siguiente,
                            true
                        )
                    }
                }
            }
        }
    }


    private fun cargarProductos() {
        lifecycleScope.launch {
            progress.visibility = View.VISIBLE

            // Primero: mostrar el catálogo de Room.
            try {
                val guardados = productoDao.obtenerProductos()
                    .map {
                        gson.fromJson(
                            it.datosJson,
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
                    "GYSELMOOD_HOME",
                    "Error leyendo Room",
                    e
                )
            }

            // Segundo: actualizar desde Railway si hay Internet.
            if (ConexionUtils.hayInternet(this@MainActivity)) {
                try {
                    val productos =
                        RetrofitClient.apiService
                            .listarProductosActivos()
                            .filter { it.activo }

                    val entidades = productos.map {
                        ProductoEntity(
                            id = it.id,
                            datosJson = gson.toJson(it)
                        )
                    }

                    productoDao.reemplazarCatalogo(entidades)
                    actualizarCatalogo(productos)

                } catch (e: CancellationException) {
                    throw e

                } catch (e: Exception) {
                    Log.e(
                        "GYSELMOOD_HOME",
                        "Error consultando Railway",
                        e
                    )
                }
            }

            progress.visibility = View.GONE

            if (todosLosProductos.isEmpty()) {
                txtEstado.text =
                    "No hay productos disponibles. Revisa tu conexión e inténtalo nuevamente."
                txtEstado.visibility = View.VISIBLE
            }
        }
    }

    private fun actualizarCatalogo(productos: List<Producto>) {
        todosLosProductos = productos

        // Si la categoría desapareció del catálogo,
        // volvemos a mostrar todas.
        val categoriasDisponibles =
            productos.map { it.categoria }.toSet()

        if (categoriaSeleccionada != null &&
            categoriaSeleccionada !in categoriasDisponibles
        ) {
            categoriaSeleccionada = null
        }

        dibujarCategorias()
        filtrarProductos()
        cargarCarrusel(productos)
    }

    private fun dibujarCategorias() {
        layoutCategorias.removeAllViews()

        agregarChip("Todos", categoriaSeleccionada == null) {
            categoriaSeleccionada = null
            dibujarCategorias()
            filtrarProductos()
        }

        val categorias = todosLosProductos
            .map { it.categoria }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        categorias.forEach { categoria ->
            agregarChip(
                categoria,
                categoriaSeleccionada == categoria
            ) {
                categoriaSeleccionada = categoria
                dibujarCategorias()
                filtrarProductos()
            }
        }
    }

    private fun agregarChip(
        nombre: String,
        seleccionado: Boolean,
        onClick: () -> Unit
    ) {
        val chip = TextView(this).apply {
            text = nombre
            setTextColor(blanco)
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(
                dp(18),
                dp(10),
                dp(18),
                dp(10)
            )

            setTypeface(
                null,
                if (seleccionado) Typeface.BOLD
                else Typeface.NORMAL
            )

            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(30).toFloat()
                setColor(if (seleccionado) rojo else gris)
            }

            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            dp(42)
        ).apply {
            marginEnd = dp(10)
        }

        layoutCategorias.addView(chip, params)
    }

    private fun filtrarProductos() {
        val busqueda = edtBuscar.text
            .toString()
            .trim()

        val filtrados = todosLosProductos.filter { producto ->

            val coincideCategoria =
                categoriaSeleccionada == null ||
                        producto.categoria == categoriaSeleccionada

            val coincideBusqueda =
                busqueda.isBlank() ||
                        producto.titulo.contains(busqueda, ignoreCase = true) ||
                        producto.categoria.contains(busqueda, ignoreCase = true) ||
                        producto.descripcion.contains(busqueda, ignoreCase = true)

            coincideCategoria && coincideBusqueda
        }

        txtContador.text =
            "${filtrados.size} productos encontrados"

        txtEstado.visibility =
            if (filtrados.isEmpty()) View.VISIBLE
            else View.GONE

        if (filtrados.isEmpty()) {
            txtEstado.text =
                if (todosLosProductos.isEmpty())
                    "Todavía no hay productos disponibles."
                else
                    "No encontramos prendas con esos filtros."
        }

        // Home: máximo 8 prendas. El catálogo tendrá todas.
        recyclerProductos.adapter = ProductAdapter(
            productos = filtrados.take(8),
            onProductoClick = { producto ->
                abrirDetalle(producto)
            },
            esHome = true
        )
    }

    private fun cargarCarrusel(productos: List<Producto>) {

        val destacados = productos
            .filter { producto ->
                producto.imagenes.any {
                    it.url.isNotBlank()
                }
            }
            .shuffled()
            .take(5)

        viewPager.adapter = HomeBannerAdapter(destacados) {
            abrirDetalle(it)
        }

        txtIndicador.visibility =
            if (destacados.isEmpty()) View.GONE
            else View.VISIBLE

        viewPager.visibility =
            if (destacados.isEmpty()) View.GONE
            else View.VISIBLE

        actualizarIndicador(0)
    }

    private fun actualizarIndicador(posicion: Int) {
        val total = viewPager.adapter?.itemCount ?: 0

        txtIndicador.text = (0 until total).joinToString("  ") {
            if (it == posicion) "●" else "○"
        }
    }

    private fun abrirDetalle(producto: Producto) {
        val intent = Intent(
            this,
            DetalleProductoUsuarioActivity::class.java
        )

        intent.putExtra(
            DetalleProductoUsuarioActivity.EXTRA_PRODUCTO,
            producto
        )

        startActivity(intent)
    }

    private fun abrirCatalogo() {
        val intent = Intent(
            this,
            ProductosActivity::class.java
        )

        // Más adelante usaremos estos filtros
        // también en ProductosActivity.
        categoriaSeleccionada?.let {
            intent.putExtra("FILTRO_CATEGORIA", it)
        }

        val busqueda = edtBuscar.text.toString().trim()

        if (busqueda.isNotEmpty()) {
            intent.putExtra("FILTRO_BUSQUEDA", busqueda)
        }

        if (!ConexionUtils.hayInternet(this)) {
            intent.putExtra("MODO_OFFLINE", true)
        }

        startActivity(intent)
    }

    private fun dp(valor: Int): Int =
        (valor * resources.displayMetrics.density).toInt()


    override fun onSaveInstanceState(outState: Bundle) {

        outState.putString(
            KEY_BUSQUEDA_HOME,
            edtBuscar.text.toString()
        )

        outState.putString(
            KEY_CATEGORIA_HOME,
            categoriaSeleccionada
        )

        super.onSaveInstanceState(outState)
    }

}