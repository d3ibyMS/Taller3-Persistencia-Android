package com.example.mistareas.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.sqlite.db.SimpleSQLiteQuery
import com.example.mistareas.data.AppDatabase

/**
 * ContentProvider de App A (Punto 3).
 *
 * Es la única puerta de entrada para que aplicaciones externas (como App B)
 * accedan a los datos locales de tareas. Ninguna app externa abre el archivo
 * .db directamente: todo pasa por query()/insert()/update()/delete() y se
 * protege con los permisos READ_TASKS / WRITE_TASKS declarados en el Manifest.
 *
 * URI de la colección: content://com.example.mistareas.provider/tareas
 * URI de un elemento  : content://com.example.mistareas.provider/tareas/{id}
 *
 * Internamente reutiliza la misma base de datos Room/SQLite de App A
 * (AppDatabase / tabla "tareas"), tal como exige la guía del Punto 3.
 */
class TareaContentProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.example.mistareas.provider"
        const val TABLA_TAREAS = "tareas"

        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$TABLA_TAREAS")

        private const val CODIGO_COLECCION = 1
        private const val CODIGO_ELEMENTO = 2

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, TABLA_TAREAS, CODIGO_COLECCION)
            addURI(AUTHORITY, "$TABLA_TAREAS/#", CODIGO_ELEMENTO)
        }

        // Nombres de columnas expuestas. Deben coincidir con los campos
        // reales de la entidad Tarea (tabla Room "tareas").
        const val COL_ID = "id"
        const val COL_TITULO = "titulo"
        const val COL_DESCRIPCION = "descripcion"
        const val COL_ESTADO_COMPLETADO = "estadoCompletado"
        const val COL_FECHA_CREACION = "fechaCreacion"
    }

    private lateinit var appDatabase: AppDatabase

    override fun onCreate(): Boolean {
        val contexto = context ?: return false
        appDatabase = AppDatabase.getDatabase(contexto)
        return true
    }

    override fun getType(uri: Uri): String = when (uriMatcher.match(uri)) {
        CODIGO_COLECCION -> "vnd.android.cursor.dir/vnd.$AUTHORITY.$TABLA_TAREAS"
        CODIGO_ELEMENTO -> "vnd.android.cursor.item/vnd.$AUTHORITY.$TABLA_TAREAS"
        else -> throw IllegalArgumentException("URI no soportada: $uri")
    }

    // READ (usado por App B mediante ContentResolver.query)
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        if (uriMatcher.match(uri) == UriMatcher.NO_MATCH) {
            throw IllegalArgumentException("URI no soportada: $uri")
        }

        val db = appDatabase.openHelper.readableDatabase
        val columnas = if (projection.isNullOrEmpty()) "*" else projection.joinToString(", ")

        val sql = StringBuilder("SELECT $columnas FROM $TABLA_TAREAS")
        val condiciones = mutableListOf<String>()
        val argumentos = mutableListOf<Any?>()

        if (!selection.isNullOrBlank()) {
            condiciones.add("($selection)")
            selectionArgs?.forEach { argumentos.add(it) }
        }
        if (uriMatcher.match(uri) == CODIGO_ELEMENTO) {
            condiciones.add("$COL_ID = ?")
            argumentos.add(ContentUris.parseId(uri))
        }
        if (condiciones.isNotEmpty()) {
            sql.append(" WHERE ").append(condiciones.joinToString(" AND "))
        }
        sql.append(" ORDER BY ")
            .append(if (sortOrder.isNullOrBlank()) "$COL_FECHA_CREACION DESC" else sortOrder)

        val cursor = db.query(SimpleSQLiteQuery(sql.toString(), argumentos.toTypedArray()))
        cursor.setNotificationUri(context?.contentResolver, uri)
        return cursor
    }

    // CREATE
    override fun insert(uri: Uri, values: ContentValues?): Uri {
        require(uriMatcher.match(uri) == CODIGO_COLECCION) {
            "Solo se puede insertar sobre la colección /tareas"
        }
        val valoresFinales = ContentValues(values ?: ContentValues())

        val titulo = valoresFinales.getAsString(COL_TITULO)
        if (titulo.isNullOrBlank()) {
            throw IllegalArgumentException("El campo '$COL_TITULO' es obligatorio")
        }
        if (!valoresFinales.containsKey(COL_FECHA_CREACION)) {
            valoresFinales.put(COL_FECHA_CREACION, System.currentTimeMillis())
        }
        if (!valoresFinales.containsKey(COL_ESTADO_COMPLETADO)) {
            valoresFinales.put(COL_ESTADO_COMPLETADO, 0)
        }

        val db = appDatabase.openHelper.writableDatabase
        val nuevoId = db.insert(TABLA_TAREAS, SQLiteDatabase.CONFLICT_REPLACE, valoresFinales)

        val uriResultado = ContentUris.withAppendedId(CONTENT_URI, nuevoId)
        context?.contentResolver?.notifyChange(uriResultado, null)
        return uriResultado
    }

    // UPDATE
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        val db = appDatabase.openHelper.writableDatabase
        val valoresFinales = values ?: ContentValues()
        val (clausula, argumentos) = construirClausulaConId(uri, selection, selectionArgs)

        val filas = db.update(TABLA_TAREAS, SQLiteDatabase.CONFLICT_REPLACE, valoresFinales, clausula, argumentos)
        if (filas > 0) context?.contentResolver?.notifyChange(uri, null)
        return filas
    }

    // DELETE
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val db = appDatabase.openHelper.writableDatabase
        val (clausula, argumentos) = construirClausulaConId(uri, selection, selectionArgs)

        val filas = db.delete(TABLA_TAREAS, clausula, argumentos)
        if (filas > 0) context?.contentResolver?.notifyChange(uri, null)
        return filas
    }

    /**
     * Si la URI apunta a un elemento concreto (/tareas/{id}), agrega la
     * condición id = ? a la cláusula WHERE recibida.
     */
    private fun construirClausulaConId(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Pair<String?, Array<Any?>?> {
        return if (uriMatcher.match(uri) == CODIGO_ELEMENTO) {
            val id = ContentUris.parseId(uri)
            val clausula = if (selection.isNullOrBlank()) "$COL_ID = ?" else "($selection) AND $COL_ID = ?"
            val listaArgumentos = (selectionArgs?.toMutableList() ?: mutableListOf()).apply { add(id.toString()) }
            clausula to listaArgumentos.map { it as Any? }.toTypedArray()
        } else {
            selection to selectionArgs?.map { it as Any? }?.toTypedArray()
        }
    }
}
