package com.ada.gyselmode.model.pago

data class CrearPagoRequest(
    val proveedor: String,
    val metodo: String,
    val estado: String,
    val referenciaExterna: String
)