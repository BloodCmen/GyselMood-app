
package com.ada.gyselmode.helper

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object UsuarioFirestoreHelper {

    fun sincronizarNombre() {

        val usuario = FirebaseAuth.getInstance()
            .currentUser ?: return

        val uid = usuario.uid
        val nombre = usuario.displayName
            ?.trim()
            ?.takeIf { it.isNotBlank() && it.length <= 60 }
            ?: return

        val referencia = FirebaseFirestore.getInstance()
            .collection("usuarios")
            .document(uid)

        referencia.get()
            .addOnSuccessListener { documento ->

                // No sobrescribir un nombre ya guardado.
                val nombreExistente = documento
                    .getString("nombre")
                    ?.trim()

                if (!nombreExistente.isNullOrBlank()) {
                    return@addOnSuccessListener
                }

                if (documento.exists()) {
                    // El documento ya existe:
                    // actualizar solo nombre.
                    referencia.update(
                        "nombre",
                        nombre
                    ).addOnFailureListener { error ->
                        Log.e(
                            "GYSELMOOD_FIRESTORE",
                            "Error actualizando nombre",
                            error
                        )
                    }

                } else {
                    // Usuario sin documento:
                    // crear UID + nombre.
                    referencia.set(
                        mapOf(
                            "uid" to uid,
                            "nombre" to nombre
                        )
                    ).addOnFailureListener { error ->
                        Log.e(
                            "GYSELMOOD_FIRESTORE",
                            "Error creando perfil",
                            error
                        )
                    }
                }
            }
            .addOnFailureListener { error ->
                Log.e(
                    "GYSELMOOD_FIRESTORE",
                    "Error consultando perfil",
                    error
                )
            }
    }
}
