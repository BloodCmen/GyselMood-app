
package com.ada.gyselmode.data.carrito

import com.ada.gyselmode.model.carrito.ItemCarrito
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object CarritoRepository {

    private val _items = MutableStateFlow<List<ItemCarrito>>(emptyList())
    val items = _items.asStateFlow()

    fun agregar(item: ItemCarrito) {
        val actuales = _items.value.toMutableList()
        val indice = actuales.indexOfFirst {
            it.productoId == item.productoId &&
                    it.tallaId == item.tallaId
        }

        if (indice >= 0) {
            val existente = actuales[indice]
            actuales[indice] = existente.copy(
                cantidad = existente.cantidad + item.cantidad
            )
        } else {
            actuales.add(item)
        }

        _items.value = actuales
    }

    fun cambiarCantidad(productoId: Long, tallaId: Long, cantidad: Int) {
        if (cantidad <= 0) {
            eliminar(productoId, tallaId)
            return
        }

        _items.value = _items.value.map {
            if (it.productoId == productoId && it.tallaId == tallaId) {
                it.copy(cantidad = cantidad)
            } else {
                it
            }
        }
    }

    fun eliminar(productoId: Long, tallaId: Long) {
        _items.value = _items.value.filterNot {
            it.productoId == productoId && it.tallaId == tallaId
        }
    }

    fun vaciar() {
        _items.value = emptyList()
    }
}