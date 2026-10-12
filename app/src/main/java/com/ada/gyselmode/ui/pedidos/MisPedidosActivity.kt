
package com.ada.gyselmode.ui.pedidos

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ada.gyselmode.databinding.ActivityMisPedidosBinding
import com.ada.gyselmode.model.pedido.PedidoResponseDto
import com.ada.gyselmode.network.RetrofitClient
import com.ada.gyselmode.ui.pago.PagoSimuladoActivity
import com.ada.gyselmode.ui.pedidos.adapter.MisPedidosAdapter
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Locale

class MisPedidosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMisPedidosBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMisPedidosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar RecyclerView
        binding.recyclerMisPedidos.layoutManager =
            LinearLayoutManager(this)
    }

    override fun onResume() {
        super.onResume()
        cargarPedidos()
    }

    private fun cargarPedidos() {

        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid.isNullOrBlank()) {
            Toast.makeText(
                this,
                "Debes iniciar sesión",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        lifecycleScope.launch {

            binding.progressMisPedidos.visibility = View.VISIBLE
            binding.txtPedidosVacios.visibility = View.GONE

            try {
                // Obtener pedidos desde Railway
                val pedidos =
                    RetrofitClient.apiService.listarPedidosUsuario(uid)

                // Mostrar pedidos
                binding.recyclerMisPedidos.adapter =
                    MisPedidosAdapter(pedidos) { pedido ->
                        mostrarDetalle(pedido)
                    }

                // Mensaje si no hay pedidos
                binding.txtPedidosVacios.text =
                    "Todavía no tienes pedidos"

                binding.txtPedidosVacios.visibility =
                    if (pedidos.isEmpty()) View.VISIBLE else View.GONE

            } catch (e: Exception) {

                binding.recyclerMisPedidos.adapter = null

                binding.txtPedidosVacios.text =
                    "No se pudieron cargar los pedidos"

                binding.txtPedidosVacios.visibility = View.VISIBLE

                Toast.makeText(
                    this@MisPedidosActivity,
                    e.message ?: "Error de conexión",
                    Toast.LENGTH_LONG
                ).show()

            } finally {
                binding.progressMisPedidos.visibility = View.GONE
            }
        }
    }

    private fun mostrarDetalle(pedido: PedidoResponseDto) {

        // Productos del pedido
        val productos = pedido.productos.joinToString("\n\n") {
            "${it.producto}\n" +
                    "Talla: ${it.talla} | Cantidad: ${it.cantidad}\n" +
                    "Subtotal: ${dinero(it.subtotal)}"
        }

        // Fecha (puede ser null)
        val fecha = pedido.fechaCreacion
            ?.replace("T", " ")
            ?.take(16)
            ?: "Sin fecha"

        // Detalles completos
        val mensaje = """
            Estado: ${pedido.estado}
            Fecha: $fecha

            PRODUCTOS
            $productos

            Subtotal: ${dinero(pedido.subtotal)}
            Envío: ${dinero(pedido.costoEnvio)}
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

        if (pedido.estado.equals("PENDIENTE", ignoreCase = true)) {
            dialogo.setPositiveButton("Completar pago") { _, _ ->

                val intentPago = Intent(
                    this,
                    PagoSimuladoActivity::class.java
                )

                intentPago.putExtra("PEDIDO_ID", pedido.id)
                startActivity(intentPago)
            }
        }

        dialogo.show()
    }

    private fun dinero(valor: Double): String {
        return "S/ ${String.format(Locale.US, "%.2f", valor)}"
    }
}
