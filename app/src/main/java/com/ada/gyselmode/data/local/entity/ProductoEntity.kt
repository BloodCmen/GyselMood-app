
package com.ada.gyselmode.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "productos_cache")
data class ProductoEntity(

    @PrimaryKey
    val id: Int,

    // Copia completa del producto:
    // descripción, precios, categoría, tallas e imágenes
    val datosJson: String
)
