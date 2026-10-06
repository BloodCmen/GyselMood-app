
package com.ada.gyselmode.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ada.gyselmode.MainActivity
import com.ada.gyselmode.R
import com.ada.gyselmode.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()

        // Verificar si existe una sesión activa
        if (auth.currentUser != null) {

            irAlHome()
            return
        }

        // Mostrar pantalla de login
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        credentialManager = CredentialManager.create(this)

        // LOGIN CON CORREO
        binding.btnLogin.setOnClickListener {

            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {

                Toast.makeText(
                    this,
                    "Completa todos los campos",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            iniciarSesion(email, password)
        }

        // REGISTRO
        binding.tvRegister.setOnClickListener {

            startActivity(
                Intent(this, RegisterActivity::class.java)
            )
        }

        // LOGIN CON GOOGLE
        binding.btnGoogle.setOnClickListener {
            iniciarSesionGoogle()
        }
    }

    // LOGIN CON CORREO Y CONTRASEÑA
    private fun iniciarSesion(
        email: String,
        password: String
    ) {

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {

                    Toast.makeText(
                        this,
                        "¡Bienvenido a GyselMood!",
                        Toast.LENGTH_SHORT
                    ).show()

                    irAlHome()

                } else {

                    Toast.makeText(
                        this,
                        "Correo o contraseña incorrectos",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    // LOGIN CON GOOGLE
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

        lifecycleScope.launch {

            try {

                val result = credentialManager.getCredential(
                    context = this@LoginActivity,
                    request = request
                )

                val credential = result.credential

                if (
                    credential is CustomCredential &&
                    credential.type ==
                    GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {

                    val googleCredential =
                        GoogleIdTokenCredential.createFrom(
                            credential.data
                        )

                    autenticarFirebaseGoogle(
                        googleCredential.idToken
                    )

                } else {

                    Toast.makeText(
                        this@LoginActivity,
                        "No se pudo obtener la cuenta de Google",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: GetCredentialException) {

                Toast.makeText(
                    this@LoginActivity,
                    "Se canceló o no se pudo iniciar con Google",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: GoogleIdTokenParsingException) {

                Toast.makeText(
                    this@LoginActivity,
                    "Error al leer la cuenta de Google",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // AUTENTICAR GOOGLE CON FIREBASE
    private fun autenticarFirebaseGoogle(idToken: String) {

        val credential = GoogleAuthProvider.getCredential(
            idToken,
            null
        )

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {

                    Toast.makeText(
                        this,
                        "¡Bienvenido a GyselMood!",
                        Toast.LENGTH_SHORT
                    ).show()

                    irAlHome()

                } else {

                    Toast.makeText(
                        this,
                        task.exception?.localizedMessage
                            ?: "Error al iniciar sesión con Google",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    // IR AL HOME
    private fun irAlHome() {

        val intent = Intent(
            this,
            MainActivity::class.java
        )

        startActivity(intent)
        finish()
    }
}