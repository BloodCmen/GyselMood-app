package com.ada.gyselmode.model

import java.io.Serializable

data class Producto(
    val id: Int,
    val titulo: String,
    val descripcion: String,
    val adicional: String,
    val precioUnidad: Double,
    val precioTotal: Double,
    val categoriaId: Int,
    val categoria: String,
    val activo: Boolean,
    val tallas: List<Talla>,
    val imagenes: List<Imagen>
) : Serializable