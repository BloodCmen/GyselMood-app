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
import com.ada.gyselmode.ui.user.pedidos.MisPedidosActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import android.view.View
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.ada.gyselmode.network.RetrofitClient
import com.ada.gyselmode.ui.user.entregas.MisEntregasActivity

class AccountActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var txtNombre: TextView
    private lateinit var txtCorreo: TextView
    private lateinit var txtAvatar: TextView
    private lateinit var btnMisEntregas: View


    private lateinit var imgAvatar: ImageView
    private lateinit var btnCambiarFoto: ImageView

    private var subiendoFoto = false

    private val selectorImagen =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                subirFotoPerfil(uri)
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)


        imgAvatar = findViewById(R.id.imgAvatar)
        btnCambiarFoto = findViewById(R.id.btnCambiarFoto)

        btnCambiarFoto.setOnClickListener {
            if (!subiendoFoto) {
                selectorImagen.launch("image/*")
            }
        }

        auth = FirebaseAuth.getInstance()

        NavigationHelper.setupBottomNavigation(this)
        NavigationHelper.setupBackButton(this)

        txtNombre = findViewById(R.id.txtNombre)
        txtCorreo = findViewById(R.id.txtCorreo)
        txtAvatar = findViewById(R.id.txtAvatar)

        // Oculto hasta validar rol REPARTIDOR desde Firestore.
        btnMisEntregas = findViewById(R.id.btnMisEntregas)
        btnMisEntregas.visibility = View.GONE
        btnMisEntregas.setOnClickListener { abrirEntregasSiEsRepartidor() }

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
            verificarRolParaEntregas()
        }
    }

    private fun verificarRolParaEntregas() {
        btnMisEntregas.visibility = View.GONE
        val uid = auth.currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("usuarios")
            .document(uid)
            .get(Source.SERVER)
            .addOnSuccessListener { documento ->
                if (isFinishing || isDestroyed || auth.currentUser?.uid != uid) return@addOnSuccessListener
                btnMisEntregas.visibility = if (documento.getString("rol") == "REPARTIDOR") {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
            .addOnFailureListener {
                if (!isFinishing && !isDestroyed) {
                    btnMisEntregas.visibility = View.GONE
                }
            }
    }

    private fun abrirEntregasSiEsRepartidor() {
        val uid = auth.currentUser?.uid ?: return
        btnMisEntregas.isEnabled = false

        FirebaseFirestore.getInstance()
            .collection("usuarios")
            .document(uid)
            .get(Source.SERVER)
            .addOnSuccessListener { documento ->
                if (isFinishing || isDestroyed || auth.currentUser?.uid != uid) return@addOnSuccessListener
                btnMisEntregas.isEnabled = true
                if (documento.getString("rol") == "REPARTIDOR") {
                    startActivity(Intent(this, MisEntregasActivity::class.java))
                } else {
                    btnMisEntregas.visibility = View.GONE
                    Toast.makeText(this, "Acceso solo para repartidores", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                if (!isFinishing && !isDestroyed) {
                    btnMisEntregas.isEnabled = true
                    Toast.makeText(this, "No se pudo validar el rol", Toast.LENGTH_SHORT).show()
                }
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


        val fotoUrl = usuario.photoUrl?.toString()

        if (!fotoUrl.isNullOrBlank()) {
            imgAvatar.visibility = View.VISIBLE
            txtAvatar.visibility = View.INVISIBLE

            Glide.with(this)
                .load(fotoUrl)
                .circleCrop()
                .error(android.R.drawable.ic_menu_myplaces)
                .into(imgAvatar)

        } else {
            imgAvatar.visibility = View.GONE
            txtAvatar.visibility = View.VISIBLE
        }

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

                            val datos = mapOf(
                                "nombre" to nuevoNombre
                            )

                            FirebaseFirestore.getInstance()
                                .collection("usuarios")
                                .document(usuario.uid)
                                .set(datos, SetOptions.merge())
                                .addOnSuccessListener {
                                    cargarUsuario()
                                    Toast.makeText(
                                        this,
                                        "Nombre actualizado",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener {
                                    cargarUsuario()
                                    Toast.makeText(
                                        this,
                                        "Nombre actualizado en Firebase Auth, " +
                                                "pero no se sincronizó con Firestore",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

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



    private fun subirFotoPerfil(uri: Uri) {
        val usuario = auth.currentUser ?: return
        if (subiendoFoto) return

        // Se utiliza Firebase para autenticar la subida.
        subiendoFoto = true
        btnCambiarFoto.isEnabled = false
        btnCambiarFoto.alpha = 0.5f

        usuario.getIdToken(false)
            .addOnSuccessListener { resultadoToken ->

                val token = resultadoToken.token

                if (token.isNullOrBlank()) {
                    finalizarSubida()
                    Toast.makeText(
                        this,
                        "No se pudo obtener tu sesión Firebase",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                lifecycleScope.launch {
                    try {
                        val tipo = contentResolver.getType(uri)

                        if (tipo != "image/jpeg" &&
                            tipo != "image/png") {
                            throw IllegalArgumentException(
                                "Selecciona una imagen JPG o PNG"
                            )
                        }

                        val bytes = contentResolver
                            .openInputStream(uri)
                            ?.use { input ->
                                // Límite de lectura: 3 MB + 1 byte.
                                input.readNBytes(
                                    3 * 1024 * 1024 + 1
                                )
                            }
                            ?: throw IllegalArgumentException(
                                "No se pudo leer la imagen"
                            )

                        if (bytes.isEmpty() ||
                            bytes.size > 3 * 1024 * 1024) {
                            throw IllegalArgumentException(
                                "La imagen debe pesar máximo 3 MB"
                            )
                        }

                        val requestBody = bytes.toRequestBody(
                            tipo.toMediaType()
                        )

                        val archivo = MultipartBody.Part
                            .createFormData(
                                "file",
                                if (tipo == "image/png") {
                                    "avatar.png"
                                } else {
                                    "avatar.jpg"
                                },
                                requestBody
                            )

                        val respuesta = RetrofitClient.apiService
                            .subirAvatar(
                                "Bearer $token",
                                archivo
                            )

                        // Comprobar que sigue siendo la misma cuenta.
                        if (auth.currentUser?.uid != usuario.uid) {
                            throw IllegalStateException(
                                "La sesión cambió durante la subida"
                            )
                        }

                        val cambios =
                            UserProfileChangeRequest.Builder()
                                .setPhotoUri(
                                    Uri.parse(respuesta.url)
                                )
                                .build()

                        usuario.updateProfile(cambios)
                            .addOnCompleteListener { tarea ->
                                finalizarSubida()

                                if (tarea.isSuccessful) {
                                    cargarUsuario()

                                    Toast.makeText(
                                        this@AccountActivity,
                                        "Foto actualizada correctamente",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        this@AccountActivity,
                                        "Foto subida, pero Firebase " +
                                                "no pudo actualizar el perfil",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }

                    } catch (e: Exception) {
                        finalizarSubida()

                        Toast.makeText(
                            this@AccountActivity,
                            e.message ?: "No se pudo subir la foto",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .addOnFailureListener { error ->
                finalizarSubida()

                Toast.makeText(
                    this,
                    error.localizedMessage
                        ?: "No se pudo validar tu sesión",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun finalizarSubida() {
        subiendoFoto = false

        btnCambiarFoto.isEnabled = true
        btnCambiarFoto.alpha = 1f
    }

}

