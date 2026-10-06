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
                "No se pudo cargar el producto",
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
                "Eliminar: ${producto.titulo} (lo conectamos ahora)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun mostrarDatos() {

        findViewById<TextView>(R.id.txtNombreDetalle).text =
            producto.titulo

        findViewById<TextView>(R.id.txtCategoriaDetalle).text =
            "Categoría: ${producto.categoria}"

        findViewById<TextView>(R.id.txtPrecioDetalle).text =
            "Precio: S/ %.2f".format(producto.precioTotal)

        findViewById<TextView>(R.id.txtDescripcionDetalle).text =
            producto.descripcion.ifBlank { "Sin descripción" }

        findViewById<TextView>(R.id.txtAdicionalDetalle).text =
            producto.adicional.ifBlank { "Sin información adicional" }

        val tallasTexto = producto.tallas
            .joinToString(", ") { it.nombre }

        findViewById<TextView>(R.id.txtTallasDetalle).text =
            tallasTexto.ifBlank { "Sin tallas registradas" }

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