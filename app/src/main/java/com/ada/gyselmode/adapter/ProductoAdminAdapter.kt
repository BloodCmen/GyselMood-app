package com.ada.gyselmode.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.ada.gyselmode.R
import com.ada.gyselmode.model.Producto
import java.util.Locale

class ProductoAdminAdapter(
    private val productos: List<Producto>,
    private val onProductoClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductoAdminAdapter.ProductoViewHolder>() {

    class ProductoViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val imgProducto: ImageView =
            itemView.findViewById(R.id.imgProducto)

        val txtNombre: TextView =
            itemView.findViewById(R.id.txtNombre)

        val txtCategoria: TextView =
            itemView.findViewById(R.id.txtCategoria)

        val txtPrecio: TextView =
            itemView.findViewById(R.id.txtPrecio)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProductoViewHolder {

        val vista = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_producto_admin,
                parent,
                false
            )

        return ProductoViewHolder(vista)
    }

    override fun onBindViewHolder(
        holder: ProductoViewHolder,
        position: Int
    ) {

        val producto = productos[position]

        holder.txtNombre.text = producto.titulo
        holder.txtCategoria.text = producto.categoria

        holder.txtPrecio.text = String.format(
            Locale.US,
            "S/ %.2f",
            producto.precioTotal
        )

        val imagenPrincipal =
            producto.imagenes.firstOrNull {
                it.esPrincipal
            } ?: producto.imagenes.firstOrNull()

        if (imagenPrincipal != null) {

            holder.imgProducto.load(imagenPrincipal.url) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_report_image)
            }

        } else {

            holder.imgProducto.setImageResource(
                android.R.drawable.ic_menu_gallery
            )
        }

        holder.itemView.setOnClickListener {
            onProductoClick(producto)
        }
    }

    override fun getItemCount(): Int {
        return productos.size
    }
}