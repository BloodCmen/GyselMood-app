package com.ada.gyselmode.maintenance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.ada.gyselmode.R
import com.ada.gyselmode.model.Imagen

class ImagenEditAdapter(
    private val imagenes: MutableList<Imagen>,
    private val onPrincipalSeleccionada: (Int) -> Unit,
    private val onEliminar: (Int) -> Unit
) : RecyclerView.Adapter<ImagenEditAdapter.ImagenViewHolder>() {

    inner class ImagenViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view) {

        val imagen: ImageView =
            view.findViewById(R.id.imgPreview)

        val texto: TextView =
            view.findViewById(R.id.txtPrincipal)

        val botonPrincipal: Button =
            view.findViewById(
                R.id.btnMarcarPrincipal
            )

        val botonEliminar: Button =
            view.findViewById(
                R.id.btnEliminarImagen
            )
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ImagenViewHolder {

        val view =
            LayoutInflater.from(parent.context)
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

        val imagen =
            imagenes[position]

        holder.imagen.load(imagen.url) {

            crossfade(true)

            placeholder(
                android.R.drawable.ic_menu_gallery
            )

            error(
                android.R.drawable.ic_menu_report_image
            )
        }

        if (imagen.esPrincipal) {

            holder.texto.text =
                holder.itemView.context.getString(R.string.image_main_star)

            holder.botonPrincipal.text =
                holder.itemView.context.getString(R.string.image_main_check)

        } else {

            holder.texto.text =
                holder.itemView.context.getString(R.string.image_number, position + 1)

            holder.botonPrincipal.text =
                holder.itemView.context.getString(R.string.image_set_main)
        }

        holder.botonPrincipal.setOnClickListener {

            val posicion =
                holder.bindingAdapterPosition

            if (
                posicion !=
                RecyclerView.NO_POSITION
            ) {

                onPrincipalSeleccionada(
                    posicion
                )
            }
        }

        holder.botonEliminar.setOnClickListener {

            val posicion =
                holder.bindingAdapterPosition

            if (
                posicion !=
                RecyclerView.NO_POSITION
            ) {

                onEliminar(posicion)
            }
        }
    }

    override fun getItemCount(): Int =
        imagenes.size
}