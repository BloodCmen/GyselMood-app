package com.ada.gyselmode

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ada.gyselmode.adapter.ProductAdapter
import com.ada.gyselmode.helper.NavigationHelper
import com.ada.gyselmode.network.RetrofitClient
import kotlinx.coroutines.launch



class ProductosActivity : AppCompatActivity() {

    private lateinit var recyclerProductos: androidx.recyclerview.widget.RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_products)

        NavigationHelper.setupBottomNavigation(this)

        recyclerProductos = findViewById(R.id.recyclerProductos)

        recyclerProductos.layoutManager =
            androidx.recyclerview.widget.GridLayoutManager(this, 2)

        cargarProductos()
    }

    private fun cargarProductos() {
        lifecycleScope.launch {
            try {
                val productos =
                    RetrofitClient.apiService.listarProductosActivos()

                recyclerProductos.adapter = ProductAdapter(productos) { producto ->
                    val intent = android.content.Intent(
                        this@ProductosActivity,
                        DetalleProductoUsuarioActivity::class.java
                    )

                    intent.putExtra(
                        DetalleProductoUsuarioActivity.EXTRA_PRODUCTO,
                        producto
                    )

                    startActivity(intent)
                }

            } catch (e: Exception) {
                e.printStackTrace()

                android.widget.Toast.makeText(
                    this@ProductosActivity,
                    getString(R.string.products_load_error, e.message),
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    }