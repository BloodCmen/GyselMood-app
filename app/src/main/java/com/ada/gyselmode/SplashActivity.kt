package com.ada.gyselmode

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ada.gyselmode.auth.LoginActivity
import com.ada.gyselmode.ui.user.MainActivity
import com.ada.gyselmode.utils.ConexionUtils
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val usuario = FirebaseAuth.getInstance().currentUser

        val intent = when {
            usuario != null && ConexionUtils.hayInternet(this) -> {
                Intent(this, MainActivity::class.java)
            }

            usuario != null && !ConexionUtils.hayInternet(this) -> {
                Intent(this, NoInternetActivity::class.java)
            }

            else -> {
                Intent(this, LoginActivity::class.java)
            }
        }

        startActivity(intent)
        finish()
    }
}