
package com.ada.gyselmode.account

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.ada.gyselmode.R
import com.ada.gyselmode.auth.LoginActivity
import com.ada.gyselmode.helper.NavigationHelper
import com.ada.gyselmode.ui.pedidos.MisPedidosActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class AccountActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var txtNombre: TextView
    private lateinit var txtCorreo: TextView
    private lateinit var txtAvatar: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        auth = FirebaseAuth.getInstance()

        NavigationHelper.setupBottomNavigation(this)
        NavigationHelper.setupBackButton(this)

        txtNombre = findViewById(R.id.txtNombre)
        txtCorreo = findViewById(R.id.txtCorreo)
        txtAvatar = findViewById(R.id.txtAvatar)

        // MIS PEDIDOS
        findViewById<TextView>(R.id.btnPedidos).setOnClickListener {
            startActivity(
                Intent(this, MisPedidosActivity::class.java)
            )
        }

        // MI PERFIL
        findViewById<TextView>(R.id.btnPerfil).setOnClickListener {
            editarPerfil()
        }

        // CONFIGURACIÓN
        findViewById<TextView>(R.id.btnConfiguracion).setOnClickListener {
            mostrarConfiguracion()
        }

        // CERRAR SESIÓN
        findViewById<TextView>(R.id.btnCerrarSesion).setOnClickListener {
            confirmarCerrarSesion()
        }
    }

    override fun onResume() {
        super.onResume()

        if (auth.currentUser == null) {
            irAlLogin()
        } else {
            cargarUsuario()
        }
    }

    private fun cargarUsuario() {
        val usuario = auth.currentUser ?: return

        val correo = usuario.email.orEmpty()

        val nombre = usuario.displayName
            ?.takeIf { it.isNotBlank() }
            ?: correo.substringBefore("@").ifBlank { "Usuario" }

        txtNombre.text = nombre
        txtCorreo.text = correo.ifBlank { "Sin correo registrado" }

        txtAvatar.text = nombre
            .trim()
            .firstOrNull()
            ?.uppercaseChar()
            ?.toString()
            ?: "U"
    }

    private fun editarPerfil() {
        val usuario = auth.currentUser ?: return

        val campoNombre = EditText(this).apply {
            setSingleLine(true)
            setText(usuario.displayName.orEmpty())
            hint = "Tu nombre"
            setPadding(40, 28, 40, 28)
            selectAll()
        }

        val dialogo = AlertDialog.Builder(this)
            .setTitle("Editar mi perfil")
            .setMessage("Actualiza tu nombre visible en GyselMood.")
            .setView(campoNombre)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()

        dialogo.setOnShowListener {
            dialogo.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val nuevoNombre =
                    campoNombre.text.toString().trim()

                if (nuevoNombre.isBlank()) {
                    campoNombre.error = "Ingresa tu nombre"
                    return@setOnClickListener
                }

                if (nuevoNombre.length > 60) {
                    campoNombre.error = "Máximo 60 caracteres"
                    return@setOnClickListener
                }

                val cambios =
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(nuevoNombre)
                        .build()

                dialogo.getButton(
                    AlertDialog.BUTTON_POSITIVE
                ).isEnabled = false

                usuario.updateProfile(cambios)
                    .addOnCompleteListener { tarea ->
                        if (tarea.isSuccessful) {
                            cargarUsuario()
                            Toast.makeText(
                                this,
                                "Nombre actualizado",
                                Toast.LENGTH_SHORT
                            ).show()
                            dialogo.dismiss()
                        } else {
                            Toast.makeText(
                                this,
                                tarea.exception?.localizedMessage
                                    ?: "No se pudo actualizar el nombre",
                                Toast.LENGTH_LONG
                            ).show()

                            dialogo.getButton(
                                AlertDialog.BUTTON_POSITIVE
                            ).isEnabled = true
                        }
                    }
            }
        }

        dialogo.show()
    }

    private fun mostrarConfiguracion() {
        val opciones = arrayOf(
            "Verificar correo electrónico",
            "Ajustes de la aplicación"
        )

        AlertDialog.Builder(this)
            .setTitle("Configuración")
            .setItems(opciones) { _, opcion ->

                when (opcion) {
                    0 -> verificarCorreo()
                    1 -> abrirAjustesSistema()
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun verificarCorreo() {
        val usuario = auth.currentUser ?: return

        if (usuario.email.isNullOrBlank()) {
            Toast.makeText(
                this,
                "Tu cuenta no tiene correo registrado",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (usuario.isEmailVerified) {
            Toast.makeText(
                this,
                "Tu correo ya está verificado",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        usuario.sendEmailVerification()
            .addOnCompleteListener { tarea ->
                val mensaje = if (tarea.isSuccessful) {
                    "Te enviamos un correo de verificación"
                } else {
                    tarea.exception?.localizedMessage
                        ?: "No se pudo enviar el correo"
                }

                Toast.makeText(
                    this,
                    mensaje,
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun abrirAjustesSistema() {
        val intentAjustes = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName")
        )

        startActivity(intentAjustes)
    }

    private fun confirmarCerrarSesion() {
        AlertDialog.Builder(this)
            .setTitle("¿Cerrar sesión?")
            .setMessage(
                "Tendrás que iniciar sesión nuevamente para acceder a tu cuenta."
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Cerrar sesión") { _, _ ->

                auth.signOut()
                irAlLogin()
            }
            .show()
    }

    private fun irAlLogin() {
        val intentLogin = Intent(
            this,
            LoginActivity::class.java
        )

        intentLogin.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intentLogin)
        finish()
    }
}
