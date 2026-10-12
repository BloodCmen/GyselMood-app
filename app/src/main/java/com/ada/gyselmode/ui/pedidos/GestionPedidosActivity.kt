package com.ada.gyselmode.ui.pedidos

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ada.gyselmode.databinding.ActivityGestionPedidosBinding
import com.ada.gyselmode.model.pedido.ActualizarEstadoPedidoRequest
import com.ada.gyselmode.model.pedido.PedidoResponseDto
import com.ada.gyselmode.network.RetrofitClient
import com.ada.gyselmode.ui.pedidos.adapter.MisPedidosAdapter
import kotlinx.coroutines.launch
import java.util.Locale

class GestionPedidosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGestionPedidosBinding
    private var procesando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGestionPedidosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerGestionPedidos.layoutManager =
            LinearLayoutManager(this)
    }

    override fun onResume() {
        super.onResume()
        cargarPedidos()
    }

    private fun cargarPedidos() {
        if (procesando) return

        lifecycleScope.launch {
            procesando = true
            binding.progressGestion.visibility = View.VISIBLE
            binding.txtSinPedidos.visibility = View.GONE

            try {
                val pedidos =
                    RetrofitClient.apiService.listarTodosPedidos()

                binding.txtTotalPedidos.text =
                    "Total de pedidos: ${pedidos.size}"

                binding.recyclerGestionPedidos.adapter =
                    MisPedidosAdapter(pedidos) { pedido ->
                        mostrarDetalle(pedido)
                    }

                binding.txtSinPedidos.visibility =
                    if (pedidos.isEmpty()) View.VISIBLE else View.GONE

            } catch (e: Exception) {
                binding.txtTotalPedidos.text = "Error al cargar pedidos"
                binding.txtSinPedidos.text =
                    "No se pudieron obtener los pedidos"
                binding.txtSinPedidos.visibility = View.VISIBLE
                binding.recyclerGestionPedidos.adapter = null

                Toast.makeText(
                    this@GestionPedidosActivity,
                    e.message ?: "Error de conexión",
                    Toast.LENGTH_LONG
                ).show()

            } finally {
                procesando = false
                binding.progressGestion.visibility = View.GONE
            }
        }
    }

    private fun mostrarDetalle(pedido: PedidoResponseDto) {

        val productos = pedido.productos.joinToString("\n\n") {
            "${it.producto}\n" +
                    "Talla: ${it.talla} | Cantidad: ${it.cantidad}\n" +
                    "Subtotal: ${dinero(it.subtotal)}"
        }

        val fecha = pedido.fechaCreacion
            ?.replace("T", " ")
            ?.take(16)
            ?: "Sin fecha"

        val mensaje = """
            Cliente UID:
            ${pedido.firebaseUid}

            Estado: ${pedido.estado}
            Fecha: $fecha

            PRODUCTOS
            $productos

            TOTAL: ${dinero(pedido.total)}

            Dirección:
            ${pedido.direccionEntrega}

            Referencia:
            ${pedido.referencia}
        """.trimIndent()

        val dialogo = AlertDialog.Builder(this)
            .setTitle("Pedido #${pedido.id}")
            .setMessage(mensaje)
            .setNegativeButton("Cerrar", null)

        if (pedido.estado.equals("PAGADO", ignoreCase = true)) {
            dialogo.setPositiveButton("Confirmar pedido") { _, _ ->
                confirmarCambio(pedido)
            }
        }

        dialogo.show()
    }

    private fun confirmarCambio(pedido: PedidoResponseDto) {

        AlertDialog.Builder(this)
            .setTitle("Confirmar pedido #${pedido.id}")
            .setMessage(
                "¿Cambiar el estado de PAGADO a CONFIRMADO?"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar") { _, _ ->
                actualizarEstado(pedido.id)
            }
            .show()
    }

    private fun actualizarEstado(pedidoId: Long) {
        if (procesando) return

        lifecycleScope.launch {
            procesando = true
            binding.progressGestion.visibility = View.VISIBLE

            try {
                val request = ActualizarEstadoPedidoRequest(
                    estado = "CONFIRMADO",
                    comentario = "Pedido confirmado por administración"
                )

                val response =
                    RetrofitClient.apiService.actualizarEstadoPedidoAdmin(
                        pedidoId,
                        request
                    )

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@GestionPedidosActivity,
                        "Pedido confirmado correctamente",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@GestionPedidosActivity,
                        "Error HTTP ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@GestionPedidosActivity,
                    e.message ?: "Error de conexión",
                    Toast.LENGTH_LONG
                ).show()

            } finally {
                procesando = false
                binding.progressGestion.visibility = View.GONE
                cargarPedidos()
            }
        }
    }

    private fun dinero(valor: Double): String =
        "S/ ${String.format(Locale.US, "%.2f", valor)}"
}
