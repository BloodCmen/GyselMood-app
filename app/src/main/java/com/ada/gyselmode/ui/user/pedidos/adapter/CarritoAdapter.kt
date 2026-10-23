package com.ada.gyselmode.ui.user.pedidos.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.ada.gyselmode.R
import com.ada.gyselmode.model.carrito.ItemCarrito
import java.util.Locale

class CarritoAdapter(
    private val onSumar: (ItemCarrito) -> Unit,
    private val onRestar: (ItemCarrito) -> Unit,
    private val onEliminar: (ItemCarrito) -> Unit
) : RecyclerView.Adapter<CarritoAdapter.CarritoViewHolder>() {

    private var items: List<ItemCarrito> = emptyList()

    class CarritoViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val imagen: ImageView =
            view.findViewById(R.id.imgProductoCarrito)

        val titulo: TextView =
            view.findViewById(R.id.txtProductoCarrito)

        val talla: TextView =
            view.findViewById(R.id.txtTallaCarrito)

        val precio: TextView =
            view.findViewById(R.id.txtPrecioCarrito)

        val cantidad: TextView =
            view.findViewById(R.id.txtCantidadCarrito)

        val subtotal: TextView =
            view.findViewById(R.id.txtSubtotalItem)

        val btnSumar: Button =
            view.findViewById(R.id.btnSumarCantidad)

        val btnRestar: Button =
            view.findViewById(R.id.btnRestarCantidad)

        val btnEliminar: Button =
            view.findViewById(R.id.btnEliminarCarrito)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CarritoViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_carrito,
                parent,
                false
            )

        return CarritoViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: CarritoViewHolder,
        position: Int
    ) {
        val item = items[position]

        holder.titulo.text = item.titulo
        holder.talla.text = "Talla: ${item.talla}"

        holder.precio.text = dinero(item.precioUnitario)
        holder.cantidad.text = item.cantidad.toString()
        holder.subtotal.text = dinero(item.subtotal)

        // NUEVO: deshabilitar el botón − cuando queda 1 unidad
        holder.btnRestar.isEnabled = item.cantidad > 1
        holder.btnRestar.alpha =
            if (item.cantidad > 1) 1f else 0.4f

        holder.imagen.load(item.imagenUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_launcher_background)
            error(R.drawable.ic_launcher_background)
        }

        holder.btnSumar.setOnClickListener {
            onSumar(item)
        }

        holder.btnRestar.setOnClickListener {
            onRestar(item)
        }

        holder.btnEliminar.setOnClickListener {
            AlertDialog.Builder(
                holder.itemView.context
            )
                .setTitle("¿Eliminar producto?")
                .setMessage(
                    "¿Deseas quitar \"${item.titulo}\" de tu carrito?"
                )
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar") { _, _ ->
                    onEliminar(item)
                }
                .show()
                .getButton(
                    AlertDialog.BUTTON_POSITIVE
                )
                .setTextColor(
                    ContextCompat.getColor(
                        holder.itemView.context,
                        R.color.gysel_red
                    )
                )
        }
    }

    override fun getItemCount(): Int = items.size

    fun actualizarLista(nuevosItems: List<ItemCarrito>) {
        items = nuevosItems.toList()
        notifyDataSetChanged()
    }

    private fun dinero(valor: Double): String =
        String.format(
            Locale.US,
            "S/ %.2f",
            valor
        )
}
