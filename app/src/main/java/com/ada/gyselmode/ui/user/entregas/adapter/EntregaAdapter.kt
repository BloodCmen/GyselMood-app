
package com.ada.gyselmode.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ada.gyselmode.R
import com.ada.gyselmode.model.entrega.AsignacionEntregaResponse

class EntregaAdapter(
    private val onDetalle: (AsignacionEntregaResponse) -> Unit,
    private val onComoLlegar: (AsignacionEntregaResponse) -> Unit,
    private val onIniciar: (AsignacionEntregaResponse) -> Unit,
    private val onFinalizar: (AsignacionEntregaResponse) -> Unit,
    private val onCompartirGps: (AsignacionEntregaResponse) -> Unit
) : RecyclerView.Adapter<EntregaAdapter.EntregaViewHolder>() {

    private val entregas = mutableListOf<AsignacionEntregaResponse>()

    class EntregaViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val txtPedido: TextView =
            view.findViewById(R.id.txtPedidoEntrega)

        val txtEstado: TextView =
            view.findViewById(R.id.txtEstadoItemEntrega)

        val txtDireccion: TextView =
            view.findViewById(R.id.txtDireccionItemEntrega)

        val btnDetalle: Button =
            view.findViewById(R.id.btnDetalleItemEntrega)

        val btnComoLlegar: Button =
            view.findViewById(R.id.btnComoLlegar)

        val btnIniciar: Button =
            view.findViewById(R.id.btnIniciarEntrega)

        val btnFinalizar: Button =
            view.findViewById(R.id.btnFinalizarEntrega)

        val btnCompartirGps: Button =
            view.findViewById(R.id.btnCompartirGps)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EntregaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_entrega, parent, false)

        return EntregaViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: EntregaViewHolder,
        position: Int
    ) {
        val entrega = entregas[position]
        val estado = entrega.estado.uppercase()

        holder.txtPedido.text = "Pedido #${entrega.pedidoId}"
        holder.txtEstado.text = "Estado: $estado"
        holder.txtDireccion.text =
            "Dirección: ${entrega.direccionEntrega}"

        holder.btnIniciar.visibility =
            if (estado == "ASIGNADA") View.VISIBLE else View.GONE

        holder.btnFinalizar.visibility =
            if (estado == "EN_CAMINO") View.VISIBLE else View.GONE

        holder.btnComoLlegar.visibility =
            if (estado == "ASIGNADA" || estado == "EN_CAMINO")
                View.VISIBLE
            else
                View.GONE

        holder.btnCompartirGps.visibility =
            if (estado == "EN_CAMINO") {
                View.VISIBLE
            } else {
                View.GONE
            }

        holder.btnDetalle.setOnClickListener {
            onDetalle(entrega)
        }

        holder.itemView.setOnClickListener {
            onDetalle(entrega)
        }

        holder.btnComoLlegar.setOnClickListener {
            onComoLlegar(entrega)
        }

        holder.btnIniciar.setOnClickListener {
            onIniciar(entrega)
        }

        holder.btnFinalizar.setOnClickListener {
            onFinalizar(entrega)
        }

        holder.btnCompartirGps.setOnClickListener {
            onCompartirGps(entrega)
        }
    }

    override fun getItemCount(): Int = entregas.size

    fun actualizarLista(
        nuevosDatos: List<AsignacionEntregaResponse>
    ) {
        entregas.clear()
        entregas.addAll(nuevosDatos)
        notifyDataSetChanged()
    }
}
