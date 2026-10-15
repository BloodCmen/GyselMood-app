
package com.ada.gyselmode.security

import android.app.Activity
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

object AdminGuard {

    fun verificarAdmin(
        activity: Activity,
        permitido: () -> Unit
    ) {

        val usuario = FirebaseAuth.getInstance().currentUser

        if (usuario == null) {
            Toast.makeText(
                activity,
                "Debes iniciar sesión",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        FirebaseFirestore.getInstance()
            .collection("usuarios")
            .document(usuario.uid)
            .get(Source.SERVER)
            .addOnSuccessListener { documento ->

                if (activity.isFinishing || activity.isDestroyed) {
                    return@addOnSuccessListener
                }

                val rol = documento.getString("rol")

                if (rol == "ADMIN") {

                    permitido()

                } else {

                    Toast.makeText(
                        activity,
                        "Acceso exclusivo para administradores",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener {

                if (!activity.isFinishing && !activity.isDestroyed) {
                    Toast.makeText(
                        activity,
                        "No se pudo verificar tu rol. Revisa tu conexión.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}
