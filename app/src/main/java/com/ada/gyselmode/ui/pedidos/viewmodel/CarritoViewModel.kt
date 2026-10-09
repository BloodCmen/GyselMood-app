
package com.ada.gyselmode.ui.pedidos.viewmodel

import androidx.lifecycle.ViewModel
import com.ada.gyselmode.data.carrito.CarritoRepository
import com.ada.gyselmode.model.carrito.ItemCarrito

class CarritoViewModel : ViewModel() {

    val items = CarritoRepository.items

    fun agregar(item: ItemCarrito) {
        CarritoRepository.agregar(item)
    }

    fun cambiarCantidad(
        productoId: Long,
        tallaId: Long,
        cantidad: Int
    ) {
        CarritoRepository.cambiarCantidad(
            productoId,
            tallaId,
            cantidad
        )
    }

    fun eliminar(productoId: Long, tallaId: Long) {
        CarritoRepository.eliminar(productoId, tallaId)
    }

    fun vaciar() {
        CarritoRepository.vaciar()
    }

    fun obtenerSubtotal(): Double =
        items.value.sumOf { it.subtotal }
}