
package com.ada.gyselmode.model.pedido

data class PedidoResponseDto(
    val id: Long,
    val firebaseUid: String,
    val estado: String,
    val subtotal: Double,
    val costoEnvio: Double,
    val total: Double,
    val direccionEntrega: String,
    val referencia: String,
    val fechaCreacion: String?,
    val productos: List<DetallePedidoResponseDto>
)