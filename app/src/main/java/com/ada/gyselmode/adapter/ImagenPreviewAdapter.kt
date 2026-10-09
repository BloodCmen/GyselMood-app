
package com.ada.gyselmode.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView

import coil.load

import com.ada.gyselmode.R

class ImagenPreviewAdapter(
    private val imagenes: MutableList<Uri>,
    private val onPrincipalSeleccionada: (Int) -> Unit
) : RecyclerView.Adapter<ImagenPreviewAdapter.ImagenViewHolder>() {

    var posicionPrincipal = 0

    inner class ImagenViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val imagen: ImageView =
            view.findViewById(R.id.imgPreview)

        val texto: TextView =
            view.findViewById(R.id.txtPrincipal)

        val boton: Button =
            view.findViewById(R.id.btnMarcarPrincipal)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ImagenViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_imagen_preview,
                parent,
                false
            )

        return ImagenViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ImagenViewHolder,
        position: Int
    ) {

        holder.imagen.load(imagenes[position])

        val esPrincipal = position == posicionPrincipal

        holder.texto.text =
            if (esPrincipal) holder.itemView.context.getString(R.string.image_main_star)
            else holder.itemView.context.getString(R.string.image_number, position + 1)

        holder.boton.text =
            if (esPrincipal) holder.itemView.context.getString(R.string.image_main_check)
            else holder.itemView.context.getString(R.string.image_set_main)

        holder.boton.setOnClickListener {

            val nuevaPosicion = holder.bindingAdapterPosition

            if (nuevaPosicion != RecyclerView.NO_POSITION) {

                val anterior = posicionPrincipal

                posicionPrincipal = nuevaPosicion

                onPrincipalSeleccionada(nuevaPosicion)

                notifyItemChanged(anterior)
                notifyItemChanged(nuevaPosicion)
            }
        }
    }

    override fun getItemCount(): Int =
        imagenes.size
}