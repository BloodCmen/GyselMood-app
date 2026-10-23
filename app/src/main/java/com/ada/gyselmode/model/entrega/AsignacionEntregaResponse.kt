
package com.ada.gyselmode.model.entrega

data class AsignacionEntregaResponse(
    val id: Long,
    val pedidoId: Long,
    val repartidorUid: String,
    val estado: String,
    val direccionEntrega: String,
    val referencia: String?,
    val latitudEntrega: Double?,
    val longitudEntrega: Double?,
    val fechaAsignacion: String?
)
