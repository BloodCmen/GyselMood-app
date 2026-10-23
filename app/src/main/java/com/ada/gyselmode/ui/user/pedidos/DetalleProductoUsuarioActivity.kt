package com.ada.gyselmode.ui.user.pedidos

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.ada.gyselmode.R
import com.ada.gyselmode.ui.admin.maintenance.adapter.DetalleGaleriaAdapter
import com.ada.gyselmode.data.carrito.CarritoRepository
import com.ada.gyselmode.helper.NavigationHelper
import com.ada.gyselmode.helper.ProductoWhatsAppHelper
import com.ada.gyselmode.model.Imagen
import com.ada.gyselmode.model.Producto
import com.ada.gyselmode.model.Talla
import com.ada.gyselmode.model.carrito.ItemCarrito
import java.util.Locale

class DetalleProductoUsuarioActivity : AppCompatActivity() {

    private lateinit var producto: Producto
    private lateinit var galeria: ViewPager2
    private lateinit var indicador: TextView
    private lateinit var contenedorTallas: LinearLayout
    private lateinit var textoCantidad: TextView
    private lateinit var textoSubtotal: TextView

    private var cantidad = 1
    private var tallaSeleccionada: Talla? = null
    private var imagenes: List<Imagen> = emptyList()

    companion object {
        const val EXTRA_PRODUCTO = "extra_producto"
        private const val KEY_CANTIDAD = "detalle_cantidad"
        private const val KEY_TALLA_ID = "detalle_talla_id"
        private const val KEY_IMAGEN = "detalle_imagen"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_producto_usuario)

        @Suppress("DEPRECATION")
        val recibido = intent.getSerializableExtra(EXTRA_PRODUCTO) as? Producto

        if (recibido == null) {
            Toast.makeText(this, "No se pudo cargar el producto", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        producto = recibido

        cantidad = (savedInstanceState?.getInt(KEY_CANTIDAD) ?: 1).coerceIn(1, 99)
        val tallaGuardada = savedInstanceState?.getInt(KEY_TALLA_ID, -1) ?: -1
        tallaSeleccionada = producto.tallas.firstOrNull { it.id == tallaGuardada }
            ?: producto.tallas.firstOrNull()

        NavigationHelper.setupBottomNavigation(this)
        galeria = findViewById(R.id.viewPagerImagenesProducto)
        indicador = findViewById(R.id.txtIndicadorGaleria)
        contenedorTallas = findViewById(R.id.layoutTallasUsuario)
        textoCantidad = findViewById(R.id.txtCantidadUsuario)
        textoSubtotal = findViewById(R.id.txtSubtotalProductoUsuario)

        mostrarDatos()
        configurarGaleria(savedInstanceState)
        dibujarTallas()
        configurarCantidad()

        findViewById<Button>(R.id.btnAgregarCarritoUsuario).apply {
            isEnabled = producto.tallas.isNotEmpty()
            setOnClickListener {
                agregarAlCarrito()
            }
        }

// Personalización mediante WhatsApp
        findViewById<Button>(
            R.id.btnPersonalizarWhatsApp
        ).setOnClickListener {

            ProductoWhatsAppHelper.solicitarPersonalizacion(
                this@DetalleProductoUsuarioActivity,
                producto
            )
        }

    }

    private fun mostrarDatos() {
        findViewById<TextView>(R.id.txtNombreProductoUsuario).text = producto.titulo
        findViewById<TextView>(R.id.txtCategoriaProductoUsuario).text = producto.categoria
        findViewById<TextView>(R.id.txtPrecioProductoUsuario).text = dinero(producto.precioUnidad)
        findViewById<TextView>(R.id.txtDescripcionProductoUsuario).text =
            producto.descripcion.ifBlank { "Sin descripción disponible." }

        findViewById<TextView>(R.id.txtAdicionalProductoUsuario).apply {
            text = producto.adicional
            visibility = if (producto.adicional.isBlank()) View.GONE else View.VISIBLE
        }

        if (producto.tallas.isEmpty()) {
            findViewById<TextView>(R.id.txtEtiquetaTalla).text =
                "Sin tallas disponibles"
        }
        actualizarCantidadYSubtotal()
    }

    private fun configurarGaleria(savedInstanceState: Bundle?) {
        imagenes = producto.imagenes
            .filter { it.url.isNotBlank() }
            .sortedByDescending { it.esPrincipal }
            .take(3)

        val respaldo = findViewById<ImageView>(R.id.imgProductoDetalleUsuario)
        if (imagenes.isEmpty()) {
            galeria.visibility = View.GONE
            respaldo.visibility = View.VISIBLE
            indicador.visibility = View.GONE
            return
        }

        galeria.visibility = View.VISIBLE
        respaldo.visibility = View.GONE
        galeria.adapter = DetalleGaleriaAdapter(imagenes)
        val pagina = (savedInstanceState?.getInt(KEY_IMAGEN) ?: 0)
            .coerceIn(0, imagenes.lastIndex)
        galeria.setCurrentItem(pagina, false)
        actualizarIndicador(pagina)

        galeria.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                actualizarIndicador(position)
            }
        })
    }

    private fun actualizarIndicador(posicion: Int) {
        indicador.text = "${posicion + 1} / ${imagenes.size}"
        indicador.visibility = if (imagenes.size > 1) View.VISIBLE else View.GONE
    }

    private fun dibujarTallas() {
        contenedorTallas.removeAllViews()

        producto.tallas.forEach { talla ->
            val seleccionada = tallaSeleccionada?.id == talla.id
            val chip = TextView(this).apply {
                text = talla.nombre
                textSize = 15f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setTypeface(null, if (seleccionada) Typeface.BOLD else Typeface.NORMAL)
                isClickable = true
                isFocusable = true
                isSelected = seleccionada
                contentDescription = "Talla ${talla.nombre}${if (seleccionada) ", seleccionada" else ""}"
                background = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(Color.parseColor(if (seleccionada) "#A52C36" else "#29292E"))
                    setStroke(dp(1), Color.parseColor(if (seleccionada) "#FF7777" else "#424248"))
                }
                setOnClickListener {
                    tallaSeleccionada = talla
                    dibujarTallas()
                }
            }
            val params = LinearLayout.LayoutParams(dp(68), dp(49)).apply {
                marginEnd = dp(9)
            }
            contenedorTallas.addView(chip, params)
        }
    }

    private fun configurarCantidad() {
        findViewById<Button>(R.id.btnRestarCantidadUsuario).setOnClickListener {
            if (cantidad > 1) {
                cantidad--
                actualizarCantidadYSubtotal()
            }
        }
        findViewById<Button>(R.id.btnSumarCantidadUsuario).setOnClickListener {
            if (cantidad < 99) {
                cantidad++
                actualizarCantidadYSubtotal()
            } else {
                Toast.makeText(this, "Máximo 99 unidades", Toast.LENGTH_SHORT).show()
            }
        }
        actualizarCantidadYSubtotal()
    }

    private fun actualizarCantidadYSubtotal() {
        textoCantidad.text = cantidad.toString()
        textoSubtotal.text = dinero(producto.precioUnidad * cantidad)
    }

    private fun agregarAlCarrito() {
        val talla = tallaSeleccionada
        if (talla == null) {
            Toast.makeText(this, "Selecciona una talla", Toast.LENGTH_SHORT).show()
            return
        }

        val principal = imagenes.firstOrNull { it.esPrincipal } ?: imagenes.firstOrNull()
        val item = ItemCarrito(
            productoId = producto.id.toLong(),
            titulo = producto.titulo,
            tallaId = talla.id.toLong(),
            talla = talla.nombre,
            cantidad = cantidad,
            precioUnitario = producto.precioUnidad,
            imagenUrl = principal?.url
        )
        CarritoRepository.agregar(item)
        Toast.makeText(this, "Producto añadido al carrito", Toast.LENGTH_SHORT).show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_CANTIDAD, cantidad)
        outState.putInt(KEY_TALLA_ID, tallaSeleccionada?.id ?: -1)
        if (::galeria.isInitialized) {
            outState.putInt(KEY_IMAGEN, galeria.currentItem)
        }
        super.onSaveInstanceState(outState)
    }

    private fun dinero(valor: Double): String =
        String.format(Locale.US, "S/ %.2f", valor)

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}