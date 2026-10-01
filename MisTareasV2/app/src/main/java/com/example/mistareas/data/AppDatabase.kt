package com.example.mistareas.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos Room que persiste sobre SQLite de forma local y permanente.
 * Los datos sobreviven al cierre de la app y al reinicio del dispositivo,
 * ya que se almacenan en el archivo .db del almacenamiento interno de la app.
 */
@Database(entities = [Tarea::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun tareaDao(): TareaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mis_tareas_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
