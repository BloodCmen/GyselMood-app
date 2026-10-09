package com.ada.gyselmode.maintenance

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

import com.ada.gyselmode.R
import com.ada.gyselmode.helper.NavigationHelper
import com.ada.gyselmode.model.TallaCreateRequest
import com.ada.gyselmode.network.RetrofitClient

import kotlinx.coroutines.launch

class CrearTallaActivity : AppCompatActivity() {

    private lateinit var edtNombre: EditText
    private lateinit var btnGuardar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_crear_talla)

        NavigationHelper.setupBottomNavigation(this)
        NavigationHelper.setupBackButton(this)

        edtNombre =
            findViewById(R.id.edtNombreTalla)

        btnGuardar =
            findViewById(R.id.btnGuardarTalla)

        btnGuardar.setOnClickListener {
            guardarTalla()
        }
    }

    private fun guardarTalla() {

        val nombre =
            edtNombre.text.toString().trim()

        if (nombre.isEmpty()) {

            edtNombre.error =
                getString(R.string.size_name_required)

            edtNombre.requestFocus()

            return
        }

        btnGuardar.isEnabled = false

        lifecycleScope.launch {

            try {

                val request =
                    TallaCreateRequest(
                        nombre = nombre
                    )

                RetrofitClient.apiService.crearTalla(
                    request
                )

                Toast.makeText(
                    this@CrearTallaActivity,
                    getString(R.string.size_created),
                    Toast.LENGTH_SHORT
                ).show()

                finish()

            } catch (e: Exception) {

                btnGuardar.isEnabled = true

                Toast.makeText(
                    this@CrearTallaActivity,
                    getString(R.string.size_create_error, e.message),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

