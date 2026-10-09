
package com.ada.gyselmode.model.pedido

data class ProductoPedidoRequest(
    val productoId: Long,
    val tallaId: Long,
    val cantidad: Int
)