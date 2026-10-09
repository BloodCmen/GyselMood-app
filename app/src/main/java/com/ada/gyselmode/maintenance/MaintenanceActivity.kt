package com.ada.gyselmode

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.ada.gyselmode.maintenance.CrearProductoActivity
import com.ada.gyselmode.helper.NavigationHelper
import com.ada.gyselmode.maintenance.GestionarCategoriasActivity
import com.ada.gyselmode.maintenance.GestionarProductosActivity
import com.ada.gyselmode.maintenance.GestionarTallasActivity


class MaintenanceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_maintenance)

        // Navbar inferior
        NavigationHelper.setupBottomNavigation(this)

        // Botones/Tarjetas de mantenimiento
        val btnNuevoProducto = findViewById<CardView>(R.id.btnNuevoProducto)
        val btnGestionarProductos = findViewById<CardView>(R.id.btnGestionarProductos)
        val btnNuevaCategoria = findViewById<CardView>(R.id.btnNuevaCategoria)
        val btnNuevaTalla = findViewById<CardView>(R.id.btnNuevaTalla)
        val btnGestionarImagenes = findViewById<CardView>(R.id.btnGestionarImagenes)

        // --- ACCIONES CON INTENTS ---


        btnNuevoProducto.setOnClickListener {
            val intent = Intent(this, CrearProductoActivity::class.java)
            startActivity(intent)
        }

        btnGestionarProductos.setOnClickListener {
            val intent = Intent(this, GestionarProductosActivity::class.java)
            startActivity(intent)
        }

        btnNuevaCategoria.setOnClickListener {
            val intent = Intent(this, GestionarCategoriasActivity::class.java)
            startActivity(intent)

        }

        btnNuevaTalla.setOnClickListener {
            val intent = Intent(this, GestionarTallasActivity::class.java)
            startActivity(intent)
        }

        btnGestionarImagenes.setOnClickListener {
            // val intent = Intent(this, GestionarImagenesActivity::class.java)
            // startActivity(intent)
            mostrarMensaje(getString(R.string.maint_manage_images))
        }


    }

    private fun mostrarMensaje(mensaje: String) {
        Toast.makeText(
            this,
            getString(R.string.maint_coming_soon, mensaje),
            Toast.LENGTH_SHORT
        ).show()
    }
}