package com.ada.gyselmode.ui.user.pago

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ada.gyselmode.ui.user.MainActivity
import com.ada.gyselmode.databinding.ActivityCompraExitosaBinding

class CompraExitosaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompraExitosaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCompraExitosaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pedidoId = intent.getLongExtra(
            "PEDIDO_ID",
            -1L
        )

        if (pedidoId <= 0L) {

            Toast.makeText(
                this,
                "No se encontró el número del pedido",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        binding.txtNumeroPedido.text =
            "Pedido #$pedidoId"

        binding.txtTituloExito.text =
            "¡Pago confirmado!"

        binding.txtDescripcionExito.text =
            "El pago de tu pedido fue confirmado por " +
                    "nuestro sistema. Gracias por comprar en GyselMood."

        binding.txtEstadoConfirmado.text =
            "✓ PAGO APROBADO"

        binding.txtNotaDemo.text =
            "Conserva el número de pedido para consultar " +
                    "el estado de tu compra."

        binding.btnVolverCatalogo.setOnClickListener {

            val intentInicio = Intent(
                this,
                MainActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            startActivity(intentInicio)
            finish()
        }
    }
}
