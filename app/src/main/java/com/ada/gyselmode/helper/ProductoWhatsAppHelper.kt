package com.ada.gyselmode.helper

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.ada.gyselmode.model.Producto

object ProductoWhatsAppHelper {

    // Reemplazar por el número COMERCIAL real.
    // Perú: 51 + número de celular, sin + ni espacios.
    private const val NUMERO_TIENDA = "51949626583"

    fun solicitarPersonalizacion(
        context: Context,
        producto: Producto
    ) {
        if (!NUMERO_TIENDA.matches(Regex("\\d{11,15}"))) {
            Toast.makeText(
                context,
                "Configura el número comercial de WhatsApp",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val mensaje = """
            Hola, GyselMood 👋

            Me interesa personalizar esta prenda:

            🛍 Producto: ${producto.titulo}
            🔖 Código de producto: #${producto.id}

            Quisiera consultar modificaciones en el diseño,
            color, estampado o algún otro detalle.

            Les enviaré mis referencias y especificaciones
            por este chat.

            ¿Podrían indicarme la disponibilidad y
            el precio final?
        """.trimIndent()

        val uri = Uri.parse(
            "https://wa.me/$NUMERO_TIENDA"
        ).buildUpon()
            .appendQueryParameter("text", mensaje)
            .build()

        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri)
            )
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "No se pudo abrir WhatsApp",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
