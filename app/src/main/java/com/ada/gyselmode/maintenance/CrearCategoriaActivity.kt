package com.ada.gyselmode.maintenance

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

import com.ada.gyselmode.R
import com.ada.gyselmode.helper.NavigationHelper
import com.ada.gyselmode.model.CategoriaCreateRequest
import com.ada.gyselmode.network.RetrofitClient

import kotlinx.coroutines.launch

class CrearCategoriaActivity : AppCompatActivity() {

    private lateinit var edtNombre: EditText
    private lateinit var btnGuardar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_crear_categoria)

        NavigationHelper.setupBottomNavigation(this)
        NavigationHelper.setupBackButton(this)

        edtNombre =
            findViewById(R.id.edtNombreCategoria)

        btnGuardar =
            findViewById(R.id.btnGuardarCategoria)

        btnGuardar.setOnClickListener {
            guardarCategoria()
        }
    }

    private fun guardarCategoria() {

        val nombre =
            edtNombre.text.toString().trim()

        if (nombre.isEmpty()) {

            edtNombre.error =
                "Ingresa el nombre de la categoría"

            edtNombre.requestFocus()

            return
        }

        val codigo = nombre
            .uppercase()
            .replace(" ", "_")

        btnGuardar.isEnabled = false

        lifecycleScope.launch {

            try {

                val request =
                    CategoriaCreateRequest(
                        codigo = codigo,
                        nombre = nombre
                    )

                RetrofitClient.apiService.crearCategoria(
                    request
                )

                Toast.makeText(
                    this@CrearCategoriaActivity,
                    "Categoría creada correctamente",
                    Toast.LENGTH_SHORT
                ).show()

                finish()

            } catch (e: Exception) {

                btnGuardar.isEnabled = true

                Toast.makeText(
                    this@CrearCategoriaActivity,
                    "Error al crear categoría: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

