
package com.ada.gyselmode.utils

import android.app.Activity
import android.content.Intent
import com.ada.gyselmode.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth

object SesionUtils {

    fun verificarSesion(activity: Activity): Boolean {

        val usuario = FirebaseAuth.getInstance().currentUser

        if (usuario != null) {
            return true
        }

        val intent = Intent(
            activity,
            LoginActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        activity.startActivity(intent)
        activity.finish()

        return false
    }
}
