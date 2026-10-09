
package com.ada.gyselmode.model.carrito

data class ItemCarrito(
    val productoId: Long,
    val titulo: String,
    val tallaId: Long,
    val talla: String,
    val cantidad: Int,
    val precioUnitario: Double,
    val imagenUrl: String? = null
) {
    val subtotal: Double
        get() = precioUnitario * cantidad
}