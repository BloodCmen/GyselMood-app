package com.ada.gyselmode.ui.pedidos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ada.gyselmode.data.pedido.PedidoRepository
import com.ada.gyselmode.model.pedido.CrearPedidoRequest
import com.ada.gyselmode.model.pedido.PedidoResponseDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PedidoViewModel(
    private val repository: PedidoRepository = PedidoRepository()
) : ViewModel() {

    private val _resultado = MutableStateFlow<PedidoResponseDto?>(null)
    val resultado = _resultado.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando = _cargando.asStateFlow()

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje = _mensaje.asStateFlow()

    fun crearPedido(request: CrearPedidoRequest) {
        viewModelScope.launch {
            _cargando.value = true
            _mensaje.value = null
            _resultado.value = null

            try {
                _resultado.value = repository.crearPedido(request)
                _mensaje.value = "Pedido creado correctamente"
            } catch (e: Exception) {
                _mensaje.value =
                    "No se pudo crear el pedido: ${e.message ?: "error de conexión"}"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun limpiarResultado() {
        _resultado.value = null
        _mensaje.value = null
    }
}