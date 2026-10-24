
package com.ada.gyselmode.auth

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.ada.gyselmode.NoInternetActivity
import com.ada.gyselmode.R
import com.ada.gyselmode.databinding.ActivityLoginBinding
import com.ada.gyselmode.ui.user.MainActivity
import com.ada.gyselmode.utils.ConexionUtils
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val auth by lazy {
        FirebaseAuth.getInstance()
    }

    private lateinit var credentialManager: CredentialManager
    private var autenticando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mantener sesión existente
        if (auth.currentUser != null) {
            if (ConexionUtils.hayInternet(this)) {
                irAlHome()
            } else {
                startActivity(
                    Intent(this, NoInternetActivity::class.java)
                )
                finish()
            }
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        credentialManager = CredentialManager.create(this)

        configurarEventos()
    }

    private fun configurarEventos() {

        // Inicio de sesión con correo
        binding.btnLogin.setOnClickListener {

            if (autenticando ||
                !ConexionUtils.verificarConexion(this)
            ) return@setOnClickListener

            val email = binding.etEmail.text
                .toString().trim()

            val password = binding.etPassword.text
                .toString()

            if (email.isBlank() || password.isBlank()) {
                avisar(getString(R.string.login_fill_all))
                return@setOnClickListener
            }

            iniciarSesion(email, password)
        }

        // Ir al registro
        binding.tvRegister.setOnClickListener {
            startActivity(
                Intent(this, RegisterActivity::class.java)
            )
        }

        // Iniciar sesión con Google
        binding.btnGoogle.setOnClickListener {

            if (autenticando ||
                !ConexionUtils.verificarConexion(this)
            ) return@setOnClickListener

            iniciarSesionGoogle()
        }

        // NUEVO: Recuperar contraseña
        binding.tvForgotPassword.setOnClickListener {

            if (autenticando) return@setOnClickListener

            mostrarDialogoRecuperacion()
        }
    }

    private fun iniciarSesion(
        email: String,
        password: String
    ) {
        cambiarCarga(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->

                cambiarCarga(false)

                if (task.isSuccessful) {
                    avisar(getString(R.string.login_welcome))
                    irAlHome()
                } else {
                    avisar(
                        getString(R.string.login_wrong_credentials)
                    )
                }
            }
    }

    // NUEVO: Formulario de recuperación
    private fun mostrarDialogoRecuperacion() {

        val campoEmail = EditText(this).apply {
            hint = getString(R.string.login_email_hint)
            inputType =
                android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

            setSingleLine(true)

            setText(
                binding.etEmail.text.toString().trim()
            )
        }

        val margen = (24 * resources.displayMetrics.density).toInt()

        val contenedor = FrameLayout(this).apply {
            setPadding(margen, 0, margen, 0)
            addView(
                campoEmail,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        val dialogo = AlertDialog.Builder(this)
            .setTitle(getString(R.string.auth_recover_title))
            .setMessage(
                getString(R.string.auth_recover_message)
            )
            .setView(contenedor)
            .setNegativeButton(
                getString(android.R.string.cancel),
                null
            )
            .setPositiveButton(
                getString(R.string.auth_recover_send),
                null
            )
            .create()

        dialogo.setOnShowListener {

            val botonEnviar = dialogo.getButton(
                AlertDialog.BUTTON_POSITIVE
            )

            botonEnviar.setOnClickListener {

                val correo = campoEmail.text
                    .toString().trim()

                if (correo.isBlank() ||
                    !Patterns.EMAIL_ADDRESS
                        .matcher(correo).matches()
                ) {
                    campoEmail.error =
                        getString(R.string.auth_invalid_email)
                    return@setOnClickListener
                }

                if (!ConexionUtils.verificarConexion(this)) {
                    return@setOnClickListener
                }

                botonEnviar.isEnabled = false

                auth.sendPasswordResetEmail(correo)
                    .addOnCompleteListener(this) { task ->

                        botonEnviar.isEnabled = true

                        if (task.isSuccessful) {
                            avisar(
                                getString(
                                    R.string.auth_recover_success
                                )
                            )
                            dialogo.dismiss()
                        } else {
                            avisar(
                                getString(
                                    R.string.auth_recover_error
                                )
                            )
                        }
                    }
            }
        }

        dialogo.show()
    }

    private fun iniciarSesionGoogle() {

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(
                getString(R.string.default_web_client_id)
            )
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        cambiarCarga(true)

        lifecycleScope.launch {

            try {

                val result = credentialManager.getCredential(
                    context = this@LoginActivity,
                    request = request
                )

                val credential = result.credential

                if (credential is CustomCredential &&
                    credential.type ==
                    GoogleIdTokenCredential
                        .TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {

                    val googleCredential =
                        GoogleIdTokenCredential.createFrom(
                            credential.data
                        )

                    autenticarFirebaseGoogle(
                        googleCredential.idToken
                    )

                } else {

                    cambiarCarga(false)
                    avisar(
                        getString(
                            R.string.login_google_no_account
                        )
                    )
                }

            } catch (e: GetCredentialException) {

                cambiarCarga(false)
                avisar(
                    getString(
                        R.string.login_google_cancelled
                    )
                )

            } catch (e: GoogleIdTokenParsingException) {

                cambiarCarga(false)
                avisar(
                    getString(
                        R.string.login_google_read_error
                    )
                )

            } catch (e: Exception) {

                cambiarCarga(false)

                avisar(
                    e.localizedMessage
                        ?: getString(
                            R.string.login_google_error
                        )
                )
            }
        }
    }

    private fun autenticarFirebaseGoogle(
        idToken: String
    ) {

        val credential =
            GoogleAuthProvider.getCredential(
                idToken,
                null
            )

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->

                cambiarCarga(false)

                if (task.isSuccessful) {

                    avisar(
                        getString(R.string.login_welcome)
                    )

                    irAlHome()

                } else {

                    avisar(
                        task.exception?.localizedMessage
                            ?: getString(
                                R.string.login_google_error
                            )
                    )
                }
            }
    }

    private fun cambiarCarga(cargando: Boolean) {

        autenticando = cargando

        if (::binding.isInitialized) {

            binding.btnLogin.isEnabled = !cargando
            binding.btnGoogle.isEnabled = !cargando
            binding.tvForgotPassword.isEnabled = !cargando
            binding.tvRegister.isEnabled = !cargando
        }
    }

    private fun avisar(mensaje: String) {
        Toast.makeText(
            this,
            mensaje,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun irAlHome() {

        startActivity(
            Intent(
                this,
                MainActivity::class.java
            )
        )

        finish()
    }
}
