
package com.ada.gyselmode

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ada.gyselmode.adapter.ProductAdapter
import com.ada.gyselmode.data.local.GyselMoodDatabase
import com.ada.gyselmode.data.local.entity.ProductoEntity
import com.ada.gyselmode.helper.NavigationHelper
import com.ada.gyselmode.model.Producto
import com.ada.gyselmode.network.RetrofitClient
import com.ada.gyselmode.utils.ConexionUtils
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException

class ProductosActivity : AppCompatActivity() {

    private lateinit var recyclerProductos: RecyclerView

    private val gson = Gson()

    private val productoDao by lazy {
        GyselMoodDatabase
            .obtenerInstancia(applicationContext)
            .productoDao()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_products)

        NavigationHelper.setupBottomNavigation(this)

        recyclerProductos = findViewById(R.id.recyclerProductos)

        recyclerProductos.layoutManager =
            GridLayoutManager(this, 2)

        cargarProductos()
    }

    private fun cargarProductos() {

        lifecycleScope.launch {

            // 1. Mostrar primero los productos guardados
            mostrarProductosGuardados()

            // 2. Si estamos offline, no llamar a Railway
            val modoOffline =
                intent.getBooleanExtra("MODO_OFFLINE", false)

            if (modoOffline || !ConexionUtils.hayInternet(this@ProductosActivity)) {
                return@launch
            }

            // 3. Intentar actualizar desde la API
            try {

                val productos =
                    RetrofitClient.apiService.listarProductosActivos()

                // Convertir productos en entidades Room
                val entidades = productos.map { producto ->
                    ProductoEntity(
                        id = producto.id,
                        datosJson = gson.toJson(producto)
                    )
                }

                // Guardar catálogo actualizado
                productoDao.reemplazarCatalogo(entidades)

                // Mostrar catálogo actualizado
                mostrarProductos(productos)

            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e

            } catch (e: Exception) {

                android.util.Log.e(
                    "GYSELMOOD_CACHE",
                    "No se pudo actualizar el catálogo",
                    e
                )

                Toast.makeText(
                    this@ProductosActivity,
                    "No se pudo actualizar el catálogo. Mostrando productos guardados, si existen.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private suspend fun mostrarProductosGuardados() {

        try {

            val entidades = productoDao.obtenerProductos()

            if (entidades.isEmpty()) {

                android.util.Log.d(
                    "GYSELMOOD_CACHE",
                    "No hay productos guardados todavía"
                )

                return
            }

            val productos = entidades.map { entidad ->
                gson.fromJson(
                    entidad.datosJson,
                    Producto::class.java
                )
            }

            mostrarProductos(productos)

        } catch (e: Exception) {

            android.util.Log.e(
                "GYSELMOOD_CACHE",
                "Error leyendo productos de Room",
                e
            )
        }
    }

    private fun mostrarProductos(productos: List<Producto>) {

        recyclerProductos.adapter =
            ProductAdapter(productos) { producto ->

                val intentDetalle = Intent(
                    this,
                    DetalleProductoUsuarioActivity::class.java
                )

                intentDetalle.putExtra(
                    DetalleProductoUsuarioActivity.EXTRA_PRODUCTO,
                    producto
                )

                startActivity(intentDetalle)
            }
    }
}
