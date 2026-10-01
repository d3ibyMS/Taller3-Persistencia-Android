package com.example.mistareas.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO con el CRUD completo sobre la tabla "tareas".
 * Todas las operaciones son suspend / Flow para no bloquear el hilo principal
 * y para que la UI se actualice automáticamente cuando cambian los datos.
 */
@Dao
interface TareaDao {

    // CREATE
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(tarea: Tarea)

    // READ - se observa como Flow para reflejar cambios en tiempo real en la lista
    @Query("SELECT * FROM tareas ORDER BY fechaCreacion DESC")
    fun obtenerTareas(): Flow<List<Tarea>>

    @Query("SELECT * FROM tareas WHERE id = :id")
    suspend fun obtenerPorId(id: Int): Tarea?

    // UPDATE
    @Update
    suspend fun actualizar(tarea: Tarea)

    // DELETE
    @Delete
    suspend fun eliminar(tarea: Tarea)
}
