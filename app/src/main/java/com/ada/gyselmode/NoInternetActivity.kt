package com.ada.gyselmode

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ada.gyselmode.databinding.ActivityNoInternetBinding
import com.ada.gyselmode.ui.user.MainActivity
import com.ada.gyselmode.ui.user.ProductosActivity
import com.ada.gyselmode.utils.ConexionUtils
import com.google.firebase.auth.FirebaseAuth

class NoInternetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoInternetBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoInternetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnReintentar.setOnClickListener {
            if (ConexionUtils.hayInternet(this)) {
                irSegunSesion()
            }
        }


        binding.btnVerProductosCache.setOnClickListener {

            val intentCatalogo = Intent(
                this,
                ProductosActivity::class.java
            )

            intentCatalogo.putExtra("MODO_OFFLINE", true)

            startActivity(intentCatalogo)
            finish()
        }

    }

    override fun onResume() {
        super.onResume()

        // Si vuelve el internet y esta pantalla está abierta, avanzamos automáticamente
        if (ConexionUtils.hayInternet(this)) {
            irSegunSesion()
        }
    }

    private fun irSegunSesion() {
        val usuario = FirebaseAuth.getInstance().currentUser

        val intent = if (usuario != null) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, com.ada.gyselmode.auth.LoginActivity::class.java)
        }

        startActivity(intent)
        finish()
    }
}