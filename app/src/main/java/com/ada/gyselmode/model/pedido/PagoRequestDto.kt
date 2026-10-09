
package com.ada.gyselmode.model.pedido

data class PagoRequestDto(
    val proveedor: String,
    val metodo: String,
    val estado: String,
    val referenciaExterna: String
)