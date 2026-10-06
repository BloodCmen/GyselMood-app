
package com.ada.gyselmode.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ada.gyselmode.MainActivity
import com.ada.gyselmode.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Registrar usuario
        binding.btnRegister.setOnClickListener {

            val nombre = binding.etName.text.toString().trim()
            val correo = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (nombre.isEmpty() || correo.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    this,
                    "Completa todos los campos",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(
                    this,
                    "La contraseña debe tener mínimo 6 caracteres",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            registrarUsuario(nombre, correo, password)
        }

        // Volver al login
        binding.tvBackLogin.setOnClickListener {
            finish()
        }
    }

    private fun registrarUsuario(
        nombre: String,
        correo: String,
        password: String
    ) {

        auth.createUserWithEmailAndPassword(correo, password)
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {

                    val usuario = auth.currentUser

                    // Guardar nombre en el perfil de Firebase
                    val perfil = userProfileChangeRequest {
                        displayName = nombre
                    }

                    usuario?.updateProfile(perfil)
                        ?.addOnCompleteListener {

                            Toast.makeText(
                                this,
                                "¡Cuenta creada correctamente!",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Ir al Home
                            val intent = Intent(
                                this,
                                MainActivity::class.java
                            )

                            startActivity(intent)
                            finish()
                        }

                } else {

                    Toast.makeText(
                        this,
                        task.exception?.localizedMessage
                            ?: "No se pudo crear la cuenta",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}