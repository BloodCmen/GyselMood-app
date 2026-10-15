
package com.ada.gyselmode.security

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ada.gyselmode.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth

abstract class BaseSecureActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        verificarSesion()
    }

    override fun onResume() {
        super.onResume()
        verificarSesion()
    }

    private fun verificarSesion() {
        if (FirebaseAuth.getInstance().currentUser != null) {
            return
        }

        val intentLogin = Intent(
            this,
            LoginActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intentLogin)
        finish()
    }
}
