package com.ada.gyselmode.ui.pedidos.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ada.gyselmode.databinding.ItemMisPedidosBinding
import com.ada.gyselmode.model.pedido.PedidoListaDto
import java.util.Locale

class MisPedidosAdapter(
    private val pedidos: List<PedidoListaDto>,
    private val onDetalle: (PedidoListaDto) -> Unit
) : RecyclerView.Adapter<MisPedidosAdapter.PedidoViewHolder>() {

    inner class PedidoViewHolder(
        val binding: ItemMisPedidosBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PedidoViewHolder {

        val binding = ItemMisPedidosBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return PedidoViewHolder(binding)
    }

    override fun getItemCount(): Int = pedidos.size

    override fun onBindViewHolder(
        holder: PedidoViewHolder,
        position: Int
    ) {
        val pedido = pedidos[position]
        val binding = holder.binding

        // Número del pedido
        binding.txtItemNumero.text = "Pedido #${pedido.id}"

        // Estado del pedido
        binding.txtItemEstado.text = pedido.estado

        val colorEstado = when (pedido.estado.uppercase()) {
            "PAGADO", "CONFIRMADO", "ENTREGADO" ->
                Color.parseColor("#4CAF50")

            "CANCELADO" ->
                Color.parseColor("#E01C1C")

            else ->
                Color.parseColor("#F5C542")
        }

        binding.txtItemEstado.setTextColor(colorEstado)

        // Fecha
        binding.txtItemFecha.text =
            pedido.fechaCreacion.replace("T", " ").take(16)

        // Productos y tallas
        binding.txtItemProductos.text =
            pedido.productos.joinToString("\n") {
                "${it.producto} · Talla ${it.talla} · Cant. ${it.cantidad}"
            }

        // Total
        binding.txtItemTotal.text =
            "S/ ${String.format(Locale.US, "%.2f", pedido.total)}"

        // Abrir detalle al pulsar la tarjeta
        binding.root.setOnClickListener {
            onDetalle(pedido)
        }
    }
}
