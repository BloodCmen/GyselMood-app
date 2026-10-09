
package com.ada.gyselmode.model.pedido

data class ActualizarEstadoPedidoRequest(
    val estado: String,
    val comentario: String? = null
)