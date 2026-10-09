
package com.ada.gyselmode.model.pedido

data class DetallePedidoResponseDto(
    val productoId: Long,
    val producto: String,
    val tallaId: Long,
    val talla: String,
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotal: Double
)