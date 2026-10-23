package com.ada.gyselmode.model.entrega

data class SeguimientoEntregaResponse(
    val pedidoId: Long,
    val estadoPedido: String,
    val estadoEntrega: String,
    val latitud: Double?,
    val longitud: Double?,
    val ultimaActualizacion: String?
)
