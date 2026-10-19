package com.ada.gyselmode.adapter

import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.ada.gyselmode.R
import com.ada.gyselmode.model.Imagen

class DetalleGaleriaAdapter(
    private val imagenes: List<Imagen>
) : RecyclerView.Adapter<DetalleGaleriaAdapter.ImagenHolder>() {

    class ImagenHolder(val imagen: ImageView) : RecyclerView.ViewHolder(imagen)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImagenHolder {
        val imageView = ImageView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        return ImagenHolder(imageView)
    }

    override fun onBindViewHolder(holder: ImagenHolder, position: Int) {
        holder.imagen.contentDescription =
            "Imagen ${position + 1} de ${imagenes.size}"

        holder.imagen.load(imagenes[position].url) {
            crossfade(true)
            placeholder(R.drawable.ic_launcher_background)
            error(R.drawable.ic_launcher_background)
        }
    }

    override fun getItemCount(): Int = imagenes.size
}
