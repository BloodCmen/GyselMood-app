package com.ada.gyselmode.maintenance

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ada.gyselmode.DetalleProductoActivity
import com.ada.gyselmode.R
import com.ada.gyselmode.adapter.ProductoAdminAdapter
import com.ada.gyselmode.helper.NavigationHelper
import com.ada.gyselmode.network.RetrofitClient
import kotlinx.coroutines.launch

class GestionarProductosActivity : AppCompatActivity() {

    private lateinit var recyclerProductos: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_gestionar_productos)

        NavigationHelper.setupBottomNavigation(this)

        recyclerProductos = findViewById(R.id.recyclerProductos)

        recyclerProductos.layoutManager =
            LinearLayoutManager(this)

        cargarProductos()
    }

    private fun cargarProductos() {

        lifecycleScope.launch {

            try {

                val productos =
                    RetrofitClient.apiService.listarProductosActivos()

                recyclerProductos.adapter =
                    ProductoAdminAdapter(productos) { producto ->


                        val intent = Intent(
                            this@GestionarProductosActivity,
                            EditarProductoActivity::class.java
                        )

                        intent.putExtra("producto", producto)
                        startActivity(intent)
                    }

            } catch (e: Exception) {

                Toast.makeText(
                    this@GestionarProductosActivity,
                    getString(R.string.products_load_error, e.message),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}