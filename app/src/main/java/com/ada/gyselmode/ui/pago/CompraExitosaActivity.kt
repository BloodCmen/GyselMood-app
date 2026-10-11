package com.ada.gyselmode.ui.pago

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ada.gyselmode.MainActivity
import com.ada.gyselmode.databinding.ActivityCompraExitosaBinding

class CompraExitosaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompraExitosaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCompraExitosaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pedidoId = intent.getLongExtra("PEDIDO_ID", -1L)

        if (pedidoId <= 0L) {
            Toast.makeText(this, "Pedido no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.txtNumeroPedido.text = "Pedido #$pedidoId"

        binding.btnVolverCatalogo.setOnClickListener {
            val intentInicio = Intent(
                this,
                MainActivity::class.java
            )

            intentInicio.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intentInicio)
            finish()
        }
    }
}
