
package com.ada.gyselmode.model.pedido

data class CrearPedidoRequest(
    val firebaseUid: String,
    val direccionEntrega: String,
    val referencia: String,
    val costoEnvio: Double,
    val productos: List<ProductoPedidoRequest>
)