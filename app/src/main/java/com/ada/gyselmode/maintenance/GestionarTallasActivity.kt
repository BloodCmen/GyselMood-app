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

class GestionarTallasActivity : AppCompatActivity() {

    private lateinit var contenedorTallas: LinearLayout
    private lateinit var btnNuevaTalla: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_gestionar_tallas)

        NavigationHelper.setupBottomNavigation(this)
        NavigationHelper.setupBackButton(this)

        contenedorTallas =
            findViewById(R.id.contenedorTallas)

        btnNuevaTalla =
            findViewById(R.id.btnNuevaTalla)

        btnNuevaTalla.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    CrearTallaActivity::class.java
                )
            )
        }

        cargarTallas()
    }

    override fun onResume() {
        super.onResume()
        cargarTallas()
    }

    private fun cargarTallas() {

        lifecycleScope.launch {

            try {

                val tallas =
                    RetrofitClient.apiService.listarTallas()

                contenedorTallas.removeAllViews()

                if (tallas.isEmpty()) {

                    val texto =
                        TextView(
                            this@GestionarTallasActivity
                        )

                    texto.text =
                        getString(R.string.sizes_empty)

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

                    contenedorTallas.addView(texto)

                    return@launch
                }

                tallas.forEach { talla ->

                    val fila =
                        LinearLayout(
                            this@GestionarTallasActivity
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
                            this@GestionarTallasActivity
                        )

                    nombre.text =
                        talla.nombre

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
                            this@GestionarTallasActivity
                        )

                    btnEliminar.text = getString(R.string.common_delete)

                    btnEliminar.setTextColor(
                        getColor(R.color.gysel_white)
                    )

                    btnEliminar.setBackgroundColor(
                        getColor(R.color.gysel_red)
                    )

                    btnEliminar.setOnClickListener {

                        eliminarTalla(
                            talla.id.toLong(),
                            talla.nombre
                        )
                    }

                    fila.addView(nombre)
                    fila.addView(btnEliminar)

                    contenedorTallas.addView(fila)
                }

            } catch (e: Exception) {

                Toast.makeText(
                    this@GestionarTallasActivity,
                    getString(R.string.sizes_load_error, e.message),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun eliminarTalla(
        id: Long,
        nombre: String
    ) {

        lifecycleScope.launch {

            try {

                RetrofitClient.apiService.eliminarTalla(
                    id
                )

                Toast.makeText(
                    this@GestionarTallasActivity,
                    getString(R.string.size_deleted, nombre),
                    Toast.LENGTH_SHORT
                ).show()

                cargarTallas()

            } catch (e: Exception) {

                Toast.makeText(
                    this@GestionarTallasActivity,
                    getString(R.string.delete_error, e.message),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

