package com.ada.gyselmode.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ada.gyselmode.R
import com.ada.gyselmode.model.Producto
import coil.load

class ProductAdapter(
    private var productos: List<Producto>
) : RecyclerView.Adapter<ProductAdapter.ProductoViewHolder>() {

    class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val imgProducto: ImageView =
            itemView.findViewById(R.id.imgProducto)

        val txtTitulo: TextView =
            itemView.findViewById(R.id.txtTitulo)

        val txtCategoria: TextView =
            itemView.findViewById(R.id.txtCategoria)

        val txtPrecio: TextView =
            itemView.findViewById(R.id.txtPrecio)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProductoViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)

        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ProductoViewHolder,
        position: Int
    ) {
        val producto = productos[position]

        holder.txtTitulo.text = producto.titulo
        holder.txtCategoria.text = producto.categoria
        holder.txtPrecio.text = "S/ %.2f".format(producto.precioUnidad)

        val imagenPrincipal = producto.imagenes
            .firstOrNull { it.esPrincipal }
            ?: producto.imagenes.firstOrNull()

        holder.imgProducto.load(imagenPrincipal?.url) {
            crossfade(true)
            placeholder(R.drawable.ic_launcher_background)
            error(R.drawable.ic_launcher_background)
        }
    }

    override fun getItemCount(): Int {
        return productos.size
    }
}