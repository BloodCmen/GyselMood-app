package com.ada.gyselmode.maintenance

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

import com.ada.gyselmode.R
import com.ada.gyselmode.helper.NavigationHelper
import com.ada.gyselmode.network.RetrofitClient

import kotlinx.coroutines.launch

class GestionarCategoriasActivity : AppCompatActivity() {

    private lateinit var contenedorCategorias: LinearLayout
    private lateinit var btnNuevaCategoria: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_gestionar_categorias)

        NavigationHelper.setupBottomNavigation(this)
        NavigationHelper.setupBackButton(this)

        contenedorCategorias =
            findViewById(R.id.contenedorCategorias)

        btnNuevaCategoria =
            findViewById(R.id.btnNuevaCategoria)

        btnNuevaCategoria.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    CrearCategoriaActivity::class.java
                )
            )
        }

        cargarCategorias()
    }

    override fun onResume() {
        super.onResume()
        cargarCategorias()
    }

    private fun cargarCategorias() {

        lifecycleScope.launch {

            try {

                val categorias =
                    RetrofitClient.apiService.listarCategorias()

                contenedorCategorias.removeAllViews()

                if (categorias.isEmpty()) {

                    val texto =
                        TextView(
                            this@GestionarCategoriasActivity
                        )

                    texto.text =
                        getString(R.string.categories_empty)

                    texto.textSize = 16f

                    texto.setTextColor(
                        getColor(R.color.gysel_white)
                    )

                    texto.setPadding(
                        20,
                        30,
                        20,
                        30
                    )

                    contenedorCategorias.addView(texto)

                    return@launch
                }

                categorias.forEach { categoria ->

                    val fila =
                        LinearLayout(
                            this@GestionarCategoriasActivity
                        )

                    fila.orientation =
                        LinearLayout.HORIZONTAL

                    fila.setPadding(
                        15,
                        15,
                        15,
                        15
                    )

                    val nombre =
                        TextView(
                            this@GestionarCategoriasActivity
                        )

                    nombre.text =
                        categoria.nombre

                    nombre.textSize = 18f

                    nombre.setTextColor(
                        getColor(R.color.gysel_white)
                    )

                    nombre.layoutParams =
                        LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        )

                    val btnEliminar =
                        Button(
                            this@GestionarCategoriasActivity
                        )

                    btnEliminar.text =
                        getString(R.string.common_delete)

                    btnEliminar.setTextColor(
                        getColor(R.color.gysel_white)
                    )

                    btnEliminar.setBackgroundColor(
                        getColor(R.color.gysel_red)
                    )

                    btnEliminar.setOnClickListener {

                        eliminarCategoria(
                            categoria.id.toLong(),
                            categoria.nombre
                        )
                    }

                    fila.addView(nombre)
                    fila.addView(btnEliminar)

                    contenedorCategorias.addView(fila)
                }

            } catch (e: Exception) {

                Toast.makeText(
                    this@GestionarCategoriasActivity,
                    getString(R.string.categories_load_error, e.message),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun eliminarCategoria(
        id: Long,
        nombre: String
    ) {

        lifecycleScope.launch {

            try {

                RetrofitClient.apiService.eliminarCategoria(
                    id
                )

                Toast.makeText(
                    this@GestionarCategoriasActivity,
                    getString(R.string.category_deleted, nombre),
                    Toast.LENGTH_SHORT
                ).show()

                cargarCategorias()

            } catch (e: Exception) {

                Toast.makeText(
                    this@GestionarCategoriasActivity,
                    getString(R.string.delete_error, e.message),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

