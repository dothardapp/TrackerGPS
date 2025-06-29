package com.cco.tracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cco.tracker.data.model.QueuedLocation

// 1. Incrementamos la versión de la base de datos a 2
@Database(entities = [QueuedLocation::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun queuedLocationDao(): QueuedLocationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tracker_database"
                )
                    // 2. Añadimos esta línea para manejar la migración automáticamente
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
