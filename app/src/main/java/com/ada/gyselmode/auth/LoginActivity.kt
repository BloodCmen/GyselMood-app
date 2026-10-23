package com.ada.gyselmode.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.ada.gyselmode.ui.user.MainActivity
import com.ada.gyselmode.NoInternetActivity
import com.ada.gyselmode.R
import com.ada.gyselmode.databinding.ActivityLoginBinding
import com.ada.gyselmode.utils.ConexionUtils
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private lateinit var credentialManager: CredentialManager
    private var autenticando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser != null) {
            if (ConexionUtils.hayInternet(this)) {
                irAlHome()
            } else {
                startActivity(Intent(this, NoInternetActivity::class.java))
                finish()
            }
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        credentialManager = CredentialManager.create(this)

        binding.btnLogin.setOnClickListener {
            if (autenticando || !ConexionUtils.verificarConexion(this)) return@setOnClickListener
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (email.isBlank() || password.isBlank()) {
                avisar(getString(R.string.login_fill_all))
                return@setOnClickListener
            }
            iniciarSesion(email, password)
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.btnGoogle.setOnClickListener {
            if (autenticando || !ConexionUtils.verificarConexion(this)) return@setOnClickListener
            iniciarSesionGoogle()
        }
    }

    private fun iniciarSesion(email: String, password: String) {
        cambiarCarga(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                cambiarCarga(false)
                if (task.isSuccessful) {
                    avisar(getString(R.string.login_welcome))
                    irAlHome()
                } else {
                    avisar(getString(R.string.login_wrong_credentials))
                }
            }
    }

    private fun iniciarSesionGoogle() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.default_web_client_id))
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
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    autenticarFirebaseGoogle(googleCredential.idToken)
                } else {
                    cambiarCarga(false)
                    avisar(getString(R.string.login_google_no_account))
                }
            } catch (e: GetCredentialException) {
                cambiarCarga(false)
                avisar(getString(R.string.login_google_cancelled))
            } catch (e: GoogleIdTokenParsingException) {
                cambiarCarga(false)
                avisar(getString(R.string.login_google_read_error))
            } catch (e: Exception) {
                cambiarCarga(false)
                avisar(e.localizedMessage ?: getString(R.string.login_google_error))
            }
        }
    }

    private fun autenticarFirebaseGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                cambiarCarga(false)
                if (task.isSuccessful) {
                    avisar(getString(R.string.login_welcome))
                    irAlHome()
                } else {
                    avisar(task.exception?.localizedMessage ?: getString(R.string.login_google_error))
                }
            }
    }

    private fun cambiarCarga(cargando: Boolean) {
        autenticando = cargando
        if (::binding.isInitialized) {
            binding.btnLogin.isEnabled = !cargando
            binding.btnGoogle.isEnabled = !cargando
        }
    }

    private fun avisar(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }

    private fun irAlHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
