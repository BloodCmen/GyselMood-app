package com.ada.gyselmode.ui.admin.maintenance.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ada.gyselmode.R
import com.ada.gyselmode.model.pedido.PedidoResponseDto
import java.util.Locale

class GestionPedidosAdminAdapter(
    private var pedidos: List<PedidoResponseDto>,
    private val onClick: (PedidoResponseDto) -> Unit
) : RecyclerView.Adapter<GestionPedidosAdminAdapter.PedidoViewHolder>() {

    class PedidoViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val numero: TextView =
            view.findViewById(R.id.txtItemNumero)

        val estado: TextView =
            view.findViewById(R.id.txtItemEstado)

        val fecha: TextView =
            view.findViewById(R.id.txtItemFecha)

        val productos: TextView =
            view.findViewById(R.id.txtItemProductos)

        val total: TextView =
            view.findViewById(R.id.txtItemTotal)

        val detalle: TextView =
            view.findViewById(R.id.txtItemDetalle)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PedidoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_gestion_pedido_admin,
                parent,
                false
            )

        return PedidoViewHolder(view)
    }

    override fun getItemCount(): Int = pedidos.size

    override fun onBindViewHolder(
        holder: PedidoViewHolder,
        position: Int
    ) {
        val pedido = pedidos[position]

        holder.numero.text = "Pedido #${pedido.id}"

        val estado = pedido.estado.uppercase(Locale.ROOT)
        holder.estado.text = estado

        val color = when (estado) {
            "PAGADO", "ENTREGADO" ->
                Color.rgb(119, 217, 160)

            "CONFIRMADO", "EN_PROCESO", "PREPARANDO" ->
                Color.rgb(121, 175, 255)

            "ENVIADO", "EN_REPARTO" ->
                Color.rgb(216, 168, 255)

            "CANCELADO", "RECHAZADO" ->
                Color.rgb(255, 128, 135)

            else ->
                Color.rgb(255, 200, 116)
        }

        holder.estado.setTextColor(color)

        holder.estado.backgroundTintList =
            ColorStateList.valueOf(
                Color.rgb(51, 51, 57)
            )

        holder.fecha.text =
            formatearFecha(pedido.fechaCreacion)

        holder.productos.text =
            pedido.productos.joinToString("\n") {
                "${it.cantidad} × ${it.producto} · ${it.talla}"
            }.ifBlank {
                "Sin detalles de productos"
            }

        holder.total.text = String.format(
            Locale.US,
            "S/ %.2f",
            pedido.total
        )

        holder.itemView.setOnClickListener {
            onClick(pedido)
        }

        holder.detalle.setOnClickListener {
            onClick(pedido)
        }
    }

    fun actualizarPedidos(nuevos: List<PedidoResponseDto>) {
        pedidos = nuevos
        notifyDataSetChanged()
    }

    private fun formatearFecha(fecha: String?): String {
        if (fecha.isNullOrBlank()) return "Sin fecha"

        return fecha
            .replace("T", " · ")
            .take(18)
    }
}
