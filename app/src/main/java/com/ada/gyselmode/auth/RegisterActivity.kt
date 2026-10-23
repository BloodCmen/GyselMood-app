package com.ada.gyselmode.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ada.gyselmode.ui.user.MainActivity
import com.ada.gyselmode.R
import com.ada.gyselmode.databinding.ActivityRegisterBinding
import com.ada.gyselmode.utils.ConexionUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private var registrando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            if (registrando || !ConexionUtils.verificarConexion(this)) return@setOnClickListener

            val nombre = binding.etName.text.toString().trim()
            val correo = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (nombre.isBlank() || correo.isBlank() || password.isBlank()) {
                avisar(getString(R.string.login_fill_all))
                return@setOnClickListener
            }
            if (password.length < 6) {
                avisar(getString(R.string.register_password_min))
                return@setOnClickListener
            }

            registrarUsuario(nombre, correo, password)
        }

        binding.tvBackLogin.setOnClickListener { finish() }
    }

    private fun registrarUsuario(nombre: String, correo: String, password: String) {
        cambiarCarga(true)

        auth.createUserWithEmailAndPassword(correo, password)
            .addOnCompleteListener(this) { creacion ->
                if (!creacion.isSuccessful) {
                    cambiarCarga(false)
                    avisar(creacion.exception?.localizedMessage ?: getString(R.string.register_error))
                    return@addOnCompleteListener
                }

                val usuario = creacion.result?.user ?: auth.currentUser
                if (usuario == null) {
                    cambiarCarga(false)
                    avisar("No se pudo recuperar la cuenta creada")
                    return@addOnCompleteListener
                }

                val perfil = userProfileChangeRequest { displayName = nombre }
                usuario.updateProfile(perfil)
                    .addOnCompleteListener(this) { perfilResultado ->
                        if (!perfilResultado.isSuccessful) {
                            android.util.Log.w(
                                "GYSELMOOD_REGISTRO",
                                "No se pudo actualizar displayName",
                                perfilResultado.exception
                            )
                        }
                        guardarNombreFirestore(usuario, nombre)
                    }
            }
    }

    private fun guardarNombreFirestore(usuario: FirebaseUser, nombre: String) {
        val uid = usuario.uid

        // El ID del documento ya ES el UID. Guardamos también uid como campo
        // para hacerlo visible fácilmente desde la consola.
        // No escribimos 'rol' ni sobrescribimos otros datos existentes.
        val datos = mapOf(
            "uid" to uid,
            "nombre" to nombre
        )

        firestore.collection("usuarios")
            .document(uid)
            .set(datos, SetOptions.merge())
            .addOnSuccessListener {
                cambiarCarga(false)
                avisar(getString(R.string.register_success))
                irAlHome()
            }
            .addOnFailureListener { error ->


                android.util.Log.e(
                    "GYSELMOOD_FIRESTORE",
                    "ERROR GUARDANDO NOMBRE: ${error.message}",
                    error
                )

                Toast.makeText(
                    this,
                    "Firestore: ${error.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()

                // La cuenta Auth YA está creada. No repetimos createUser.
                // MainActivity puede reintentar la sincronización del nombre.
                android.util.Log.e(
                    "GYSELMOOD_REGISTRO",
                    "Cuenta $uid creada, pero falló la escritura en Firestore",
                    error
                )
                cambiarCarga(false)
                avisar(
                    "Cuenta creada, pero no se guardó el nombre en Firestore. " +
                            "Revisa los permisos o la conexión."
                )
                irAlHome()
            }
    }

    private fun cambiarCarga(cargando: Boolean) {
        registrando = cargando
        binding.btnRegister.isEnabled = !cargando
    }

    private fun avisar(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
    }

    private fun irAlHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
