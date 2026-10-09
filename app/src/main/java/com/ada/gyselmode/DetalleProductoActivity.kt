package com.ada.gyselmode

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.ada.gyselmode.helper.NavigationHelper
import com.ada.gyselmode.maintenance.EditarProductoActivity
import com.ada.gyselmode.model.Producto

class DetalleProductoActivity : AppCompatActivity() {

    private lateinit var producto: Producto

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_detalle_producto)

        NavigationHelper.setupBottomNavigation(this)

        @Suppress("DEPRECATION")
        val recibido = intent.getSerializableExtra("producto") as? Producto

        if (recibido == null) {
            Toast.makeText(
                this,
                getString(R.string.detail_load_error),
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        producto = recibido

        mostrarDatos()

        val btnEditar = findViewById<Button>(R.id.btnEditarProducto)
        val btnEliminar = findViewById<Button>(R.id.btnEliminarProducto)

        btnEditar.setOnClickListener {

            val intent = Intent(
                this,
                EditarProductoActivity::class.java
            )

            intent.putExtra("producto", producto)

            startActivity(intent)
        }

        btnEliminar.setOnClickListener {
            Toast.makeText(
                this,
                getString(R.string.detail_delete_pending, producto.titulo),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun mostrarDatos() {

        findViewById<TextView>(R.id.txtNombreDetalle).text =
            producto.titulo

        findViewById<TextView>(R.id.txtCategoriaDetalle).text =
            getString(R.string.detail_category_format, producto.categoria)

        findViewById<TextView>(R.id.txtPrecioDetalle).text =
            getString(R.string.detail_price_format, producto.precioTotal)

        findViewById<TextView>(R.id.txtDescripcionDetalle).text =
            producto.descripcion.ifBlank { getString(R.string.detail_no_description) }

        findViewById<TextView>(R.id.txtAdicionalDetalle).text =
            producto.adicional.ifBlank { getString(R.string.detail_no_additional) }

        val tallasTexto = producto.tallas
            .joinToString(", ") { it.nombre }

        findViewById<TextView>(R.id.txtTallasDetalle).text =
            tallasTexto.ifBlank { getString(R.string.detail_no_sizes) }

        val imagenPrincipal = producto.imagenes.firstOrNull {
            it.esPrincipal
        } ?: producto.imagenes.firstOrNull()

        val imgDetalle =
            findViewById<ImageView>(R.id.imgProductoDetalle)

        if (imagenPrincipal != null) {
            imgDetalle.load(imagenPrincipal.url) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_report_image)
            }
        } else {
            imgDetalle.setImageResource(
                android.R.drawable.ic_menu_gallery
            )
        }
    }
}