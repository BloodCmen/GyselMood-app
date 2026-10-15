
package com.ada.gyselmode.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ada.gyselmode.data.local.dao.ProductoDao
import com.ada.gyselmode.data.local.entity.ProductoEntity

@Database(
    entities = [ProductoEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GyselMoodDatabase : RoomDatabase() {

    abstract fun productoDao(): ProductoDao

    companion object {

        @Volatile
        private var INSTANCE: GyselMoodDatabase? = null

        fun obtenerInstancia(
            context: Context
        ): GyselMoodDatabase {

            return INSTANCE ?: synchronized(this) {

                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GyselMoodDatabase::class.java,
                    "gyselmood_database"
                ).build().also {
                    INSTANCE = it
                }
            }
        }
    }
}
