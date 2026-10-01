package com.example.appb.model

/**
 * Representación LOCAL, en App B, de cada fila que llega desde el
 * ContentProvider de App A a través del Cursor. App B nunca ve la entidad
 * Room "Tarea" de App A directamente: solo ve columnas de texto/número
 * dentro de un Cursor, y las traduce a este modelo propio.
 */
data class TareaCompartida(
    val id: Int,
    val titulo: String,
    val descripcion: String,
    val completada: Boolean,
    val fechaCreacion: Long
)
