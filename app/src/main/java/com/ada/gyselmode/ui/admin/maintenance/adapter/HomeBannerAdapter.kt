package com.ada.gyselmode.ui.admin.maintenance.adapter

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

class HomeBannerAdapter(
    private val productos: List<Producto>,
    private val onProductoClick: (Producto) -> Unit
) : RecyclerView.Adapter<HomeBannerAdapter.BannerViewHolder>() {

    class BannerViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val imagen: ImageView =
            view.findViewById(R.id.imgBannerProducto)

        val titulo: TextView =
            view.findViewById(R.id.txtBannerTitulo)

        val precio: TextView =
            view.findViewById(R.id.txtBannerPrecio)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BannerViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_home_banner,
                parent,
                false
            )

        return BannerViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: BannerViewHolder,
        position: Int
    ) {
        val producto = productos[position]

        val imagen = producto.imagenes
            .firstOrNull { it.esPrincipal && it.url.isNotBlank() }
            ?: producto.imagenes.firstOrNull { it.url.isNotBlank() }

        holder.imagen.load(imagen?.url) {
            crossfade(true)
            placeholder(R.drawable.ic_launcher_background)
            error(R.drawable.ic_launcher_background)
        }

        holder.titulo.text = producto.titulo

        holder.precio.text = String.format(
            Locale.US,
            "Desde S/ %.2f",
            producto.precioUnidad
        )

        holder.itemView.setOnClickListener {
            onProductoClick(producto)
        }
    }

    override fun getItemCount(): Int = productos.size
}
