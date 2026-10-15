
package com.ada.gyselmode.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ada.gyselmode.data.local.entity.ProductoEntity

@Dao
interface ProductoDao {

    @Query("SELECT * FROM productos_cache ORDER BY id DESC")
    suspend fun obtenerProductos(): List<ProductoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarProductos(
        productos: List<ProductoEntity>
    )

    @Query("DELETE FROM productos_cache")
    suspend fun eliminarProductos()

    @Transaction
    suspend fun reemplazarCatalogo(
        productos: List<ProductoEntity>
    ) {
        eliminarProductos()
        guardarProductos(productos)
    }
}
