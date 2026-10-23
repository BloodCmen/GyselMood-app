package com.ada.gyselmode.ui.user.pedidos.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ada.gyselmode.databinding.ItemMisPedidosBinding
import com.ada.gyselmode.model.pedido.PedidoResponseDto
import java.util.Locale

class MisPedidosAdapter(
    private var pedidos: List<PedidoResponseDto>,
    private val onDetalle: (PedidoResponseDto) -> Unit
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

        binding.txtItemNumero.text = "Pedido #${pedido.id}"

        val estado = pedido.estado.uppercase(Locale.ROOT)
        binding.txtItemEstado.text = estado

        val colorEstado = when (estado) {
            "PAGADO", "ENTREGADO" ->
                Color.parseColor("#86DBA8")

            "CONFIRMADO", "EN_PROCESO" ->
                Color.parseColor("#91BBFF")

            "ENVIADO", "EN_REPARTO" ->
                Color.parseColor("#D2A2FF")

            "CANCELADO", "RECHAZADO" ->
                Color.parseColor("#FF898F")

            else ->
                Color.parseColor("#FFD18C")
        }

        binding.txtItemEstado.setTextColor(colorEstado)

        binding.txtItemFecha.text =
            pedido.fechaCreacion
                ?.replace("T", " · ")
                ?.take(19)
                ?: "Sin fecha"

        binding.txtItemProductos.text =
            pedido.productos.joinToString("\n") {
                "${it.cantidad} × ${it.producto} · Talla ${it.talla}"
            }.ifBlank {
                "Sin productos registrados"
            }

        binding.txtItemTotal.text = String.format(
            Locale.US,
            "S/ %.2f",
            pedido.total
        )

        binding.root.setOnClickListener {
            onDetalle(pedido)
        }

        binding.txtItemDetalle.setOnClickListener {
            onDetalle(pedido)
        }
    }

    fun actualizarPedidos(nuevos: List<PedidoResponseDto>) {
        pedidos = nuevos
        notifyDataSetChanged()
    }
}
