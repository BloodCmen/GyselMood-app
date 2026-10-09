
package com.ada.gyselmode.ui.pedidos.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ada.gyselmode.R
import com.ada.gyselmode.model.carrito.ItemCarrito
import java.util.Locale

class CarritoAdapter(
    private val onSumar: (ItemCarrito) -> Unit,
    private val onRestar: (ItemCarrito) -> Unit,
    private val onEliminar: (ItemCarrito) -> Unit
) : RecyclerView.Adapter<CarritoAdapter.CarritoViewHolder>() {

    private var items: List<ItemCarrito> = emptyList()

    fun actualizarLista(nuevaLista: List<ItemCarrito>) {
        items = nuevaLista.toList()
        notifyDataSetChanged()
    }

    inner class CarritoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val producto: TextView = view.findViewById(R.id.txtProductoCarrito)
        val talla: TextView = view.findViewById(R.id.txtTallaCarrito)
        val precio: TextView = view.findViewById(R.id.txtPrecioCarrito)
        val cantidad: TextView = view.findViewById(R.id.txtCantidadCarrito)
        val subtotal: TextView = view.findViewById(R.id.txtSubtotalItem)
        val sumar: Button = view.findViewById(R.id.btnSumarCantidad)
        val restar: Button = view.findViewById(R.id.btnRestarCantidad)
        val eliminar: Button = view.findViewById(R.id.btnEliminarCarrito)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarritoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carrito, parent, false)
        return CarritoViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarritoViewHolder, position: Int) {
        val item = items[position]
        holder.producto.text = item.titulo
        holder.talla.text = "Talla: ${item.talla}"
        holder.precio.text = "Precio: ${dinero(item.precioUnitario)}"
        holder.cantidad.text = item.cantidad.toString()
        holder.subtotal.text = "Subtotal: ${dinero(item.subtotal)}"

        holder.sumar.setOnClickListener { onSumar(item) }
        holder.restar.setOnClickListener { onRestar(item) }
        holder.eliminar.setOnClickListener { onEliminar(item) }
    }

    override fun getItemCount(): Int = items.size

    private fun dinero(valor: Double): String =
        "S/ ${String.format(Locale.US, "%.2f", valor)}"
}