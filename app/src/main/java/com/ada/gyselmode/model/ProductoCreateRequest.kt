package com.ada.gyselmode.model

data class ProductoCreateRequest(
    val titulo: String,
    val descripcion: String,
    val adicional: String,
    val precioUnidad: Double,
    val precioTotal: Double,
    val categoriaId: Int,
    val tallas: List<ProductoTallaRequest>,
    val imagenes: List<ImagenCreateRequest>
)