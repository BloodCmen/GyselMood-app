
package com.ada.gyselmode.data.pedido

import com.ada.gyselmode.model.pedido.ActualizarEstadoPedidoRequest
import com.ada.gyselmode.model.pedido.CrearPedidoRequest
import com.ada.gyselmode.network.RetrofitClient

class PedidoRepository {

    private val api = RetrofitClient.apiService

    suspend fun crearPedido(request: CrearPedidoRequest) =
        api.crearPedido(request)

    suspend fun listarPedidosUsuario(firebaseUid: String) =
        api.listarPedidosUsuario(firebaseUid)

    suspend fun obtenerPedido(id: Long, firebaseUid: String) =
        api.obtenerPedido(id, firebaseUid)

    suspend fun cancelarPedido(id: Long, firebaseUid: String) =
        api.cancelarPedido(id, firebaseUid)

    suspend fun obtenerHistorial(id: Long, firebaseUid: String) =
        api.obtenerHistorialPedido(id, firebaseUid)

    suspend fun actualizarEstado(
        id: Long,
        request: ActualizarEstadoPedidoRequest
    ) = api.actualizarEstadoPedido(id, request)
}