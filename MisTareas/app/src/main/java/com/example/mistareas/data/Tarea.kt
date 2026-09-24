package com.example.mistareas.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Tarea.
 * Contiene los cinco campos solicitados en la especificación:
 * id, titulo, descripcion, estadoCompletado y fechaCreacion.
 */
@Entity(tableName = "tareas")
data class Tarea(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val titulo: String,
    val descripcion: String,
    val estadoCompletado: Boolean = false,
    val fechaCreacion: Long = System.currentTimeMillis()
)
