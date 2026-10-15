package com.ada.gyselmode.model.pedido

data class CrearPedidoRequest(
    val firebaseUid: String,
    val direccionEntrega: String,
    val referencia: String,
    val latitudEntrega: Double,
    val longitudEntrega: Double,
    val costoEnvio: Double,
    val productos: List<ProductoPedidoRequest>
)
