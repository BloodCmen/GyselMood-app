package com.ada.gyselmode

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.ada.gyselmode.data.carrito.CarritoRepository
import com.ada.gyselmode.helper.NavigationHelper
import com.ada.gyselmode.model.Producto
import com.ada.gyselmode.model.carrito.ItemCarrito

class DetalleProductoUsuarioActivity : AppCompatActivity() {

    private lateinit var producto: Producto
    private var cantidad = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_producto_usuario)

        @Suppress("DEPRECATION")
        val recibido = intent.getSerializableExtra(EXTRA_PRODUCTO) as? Producto

        if (recibido == null) {
            Toast.makeText(
                this,
                "No se pudo cargar el producto",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        producto = recibido

        NavigationHelper.setupBottomNavigation(this)
        mostrarProducto()
        configurarCantidad()

        findViewById<Button>(R.id.btnAgregarCarritoUsuario)
            .setOnClickListener {
                agregarAlCarrito()
            }
    }

    private fun mostrarProducto() {
        findViewById<TextView>(R.id.txtNombreProductoUsuario).text =
            producto.titulo

        findViewById<TextView>(R.id.txtCategoriaProductoUsuario).text =
            producto.categoria

        findViewById<TextView>(R.id.txtPrecioProductoUsuario).text =
            "S/ %.2f".format(producto.precioUnidad)

        findViewById<TextView>(R.id.txtDescripcionProductoUsuario).text =
            producto.descripcion

        val adicional = findViewById<TextView>(
            R.id.txtAdicionalProductoUsuario
        )

        adicional.text = producto.adicional
        adicional.visibility =
            if (producto.adicional.isBlank()) View.GONE else View.VISIBLE

        val imagenPrincipal = producto.imagenes
            .firstOrNull { it.esPrincipal }
            ?: producto.imagenes.firstOrNull()

        findViewById<ImageView>(R.id.imgProductoDetalleUsuario)
            .load(imagenPrincipal?.url) {
                crossfade(true)
                placeholder(R.drawable.ic_launcher_background)
                error(R.drawable.ic_launcher_background)
            }

        val spinner = findViewById<Spinner>(R.id.spinnerTallasUsuario)

        val tallas = producto.tallas

        val nombresTallas = if (tallas.isEmpty()) {
            listOf("Sin tallas disponibles")
        } else {
            tallas.map { it.nombre }
        }

        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            nombresTallas
        ).also {
            it.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
        }

        findViewById<Button>(R.id.btnAgregarCarritoUsuario).isEnabled =
            tallas.isNotEmpty()
    }

    private fun configurarCantidad() {
        val txtCantidad = findViewById<TextView>(R.id.txtCantidadUsuario)

        findViewById<Button>(R.id.btnRestarCantidadUsuario)
            .setOnClickListener {
                if (cantidad > 1) {
                    cantidad--
                    txtCantidad.text = cantidad.toString()
                }
            }

        findViewById<Button>(R.id.btnSumarCantidadUsuario)
            .setOnClickListener {
                cantidad++
                txtCantidad.text = cantidad.toString()
            }
    }

    private fun agregarAlCarrito() {
        val spinner = findViewById<Spinner>(R.id.spinnerTallasUsuario)

        val talla = producto.tallas.getOrNull(spinner.selectedItemPosition)

        if (talla == null) {
            Toast.makeText(
                this,
                "Este producto no tiene tallas disponibles",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val imagenPrincipal = producto.imagenes
            .firstOrNull { it.esPrincipal }
            ?: producto.imagenes.firstOrNull()

        val item = ItemCarrito(
            productoId = producto.id.toLong(),
            titulo = producto.titulo,
            tallaId = talla.id.toLong(),
            talla = talla.nombre,
            cantidad = cantidad,
            precioUnitario = producto.precioUnidad,
            imagenUrl = imagenPrincipal?.url
        )

        CarritoRepository.agregar(item)

        Toast.makeText(
            this,
            "Producto añadido al carrito",
            Toast.LENGTH_SHORT
        ).show()
    }

    companion object {
        const val EXTRA_PRODUCTO = "extra_producto"
    }
}