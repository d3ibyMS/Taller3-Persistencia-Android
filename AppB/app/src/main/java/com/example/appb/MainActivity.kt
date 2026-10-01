package com.example.appb

import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appb.adapter.TareaCompartidaAdapter
import com.example.appb.model.TareaCompartida

/**
 * App B (Punto 3) — Consulta de Tareas.
 *
 * App B NO abre el archivo .db de App A ni conoce su base de datos Room.
 * El único punto de entrada es el ContentProvider de App A, al que se accede
 * mediante ContentResolver + la URI de contenido que expone.
 *
 * Flujo: App B -> ContentResolver -> ContentProvider de App A -> Room/SQLite
 * de App A. Android intercepta la petición y verifica el permiso
 * READ_TASKS antes de dejarla pasar.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        // Estos valores DEBEN coincidir exactamente con los declarados en
        // TareaContentProvider.kt y el AndroidManifest.xml de App A.
        private const val AUTHORITY = "com.example.mistareas.provider"
        private const val TABLA_TAREAS = "tareas"
        private val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$TABLA_TAREAS")

        // Nombres de columna expuestos por el Provider de App A.
        private const val COL_ID = "id"
        private const val COL_TITULO = "titulo"
        private const val COL_DESCRIPCION = "descripcion"
        private const val COL_ESTADO_COMPLETADO = "estadoCompletado"
        private const val COL_FECHA_CREACION = "fechaCreacion"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TareaCompartidaAdapter
    private lateinit var tvEstado: TextView
    private lateinit var tvContador: TextView
    private lateinit var btnConsultar: Button
    private lateinit var btnActualizar: Button

    // Declaración del ContentObserver para actualizaciones automáticas
    private lateinit var tareaObserver: ContentObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()
        window.statusBarColor = androidx.core.content.ContextCompat.getColor(
            this, R.color.blue_primary_dark
        )
        // Instrucción agregada para que los íconos de la barra de estado sean oscuros
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        recyclerView = findViewById(R.id.recyclerViewTareas)
        tvEstado = findViewById(R.id.tvEstado)
        tvContador = findViewById(R.id.tvContador)
        btnConsultar = findViewById(R.id.btnConsultar)
        btnActualizar = findViewById(R.id.btnActualizar)

        adapter = TareaCompartidaAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnConsultar.setOnClickListener { consultarTareas() }
        btnActualizar.setOnClickListener { consultarTareas() }

        // Lógica agregada para navegar a la pantalla del Punto 4 (Seguridad)
        val btnIrSeguridad = findViewById<Button>(R.id.btnIrSeguridad)
        btnIrSeguridad.setOnClickListener {
            startActivity(Intent(this, SeguridadActivity::class.java))
        }

        // Inicialización del ContentObserver
        tareaObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                // Se dispara automáticamente cada vez que App A notifica un cambio
                consultarTareas()
            }
        }
    }

    // Registrar el Observer cuando la app se vuelve visible
    override fun onStart() {
        super.onStart()
        contentResolver.registerContentObserver(CONTENT_URI, true, tareaObserver)
        // Llama a consultarTareas automáticamente al iniciar
        consultarTareas()
    }

    // Desvincular el Observer cuando la app pasa a segundo plano para ahorrar recursos
    override fun onStop() {
        super.onStop()
        contentResolver.unregisterContentObserver(tareaObserver)
    }

    /**
     * Este es el método central del Punto 3 para App B: usa ContentResolver
     * para pedirle los datos al ContentProvider de App A, manejando los tres
     * escenarios que pide la guía de pruebas.
     */
    private fun consultarTareas() {
        mostrarCargando()

        val cursor = try {
            contentResolver.query(
                CONTENT_URI,
                null,               // projection: null = todas las columnas
                null,               // selection
                null,               // selectionArgs
                "$COL_FECHA_CREACION DESC"
            )
        } catch (e: SecurityException) {
            mostrarError(
                "Acceso denegado. Esta app no tiene el permiso " +
                        "'READ_TASKS' para consultar App A, o Android lo bloqueó."
            )
            return
        } catch (e: IllegalArgumentException) {
            mostrarError(
                "La URI del ContentProvider no es válida, o App A no está " +
                        "instalada/configurada correctamente en este dispositivo."
            )
            return
        } catch (e: Exception) {
            mostrarError("Error inesperado al consultar App A: ${e.message}")
            return
        }

        if (cursor == null) {
            mostrarError(
                "No se pudo contactar el ContentProvider de App A. " +
                        "Verifica que App A esté instalada en este dispositivo."
            )
            return
        }

        val lista = mutableListOf<TareaCompartida>()
        cursor.use {
            if (it.moveToFirst()) {
                val idxId = it.getColumnIndexOrThrow(COL_ID)
                val idxTitulo = it.getColumnIndexOrThrow(COL_TITULO)
                val idxDescripcion = it.getColumnIndexOrThrow(COL_DESCRIPCION)
                val idxEstado = it.getColumnIndexOrThrow(COL_ESTADO_COMPLETADO)
                val idxFecha = it.getColumnIndexOrThrow(COL_FECHA_CREACION)
                do {
                    lista.add(
                        TareaCompartida(
                            id = it.getInt(idxId),
                            titulo = it.getString(idxTitulo) ?: "",
                            descripcion = it.getString(idxDescripcion) ?: "",
                            completada = it.getInt(idxEstado) != 0,
                            fechaCreacion = it.getLong(idxFecha)
                        )
                    )
                } while (it.moveToNext())
            }
        }

        adapter.actualizarLista(lista)
        recyclerView.visibility = if (lista.isEmpty()) View.GONE else View.VISIBLE
        tvContador.text = "${lista.size} tareas recibidas"
        tvEstado.text = if (lista.isEmpty()) {
            "Consulta exitosa. App A todavía no tiene tareas registradas."
        } else {
            "Datos obtenidos correctamente desde App A mediante ContentProvider."
        }
    }

    private fun mostrarCargando() {
        tvEstado.text = "Consultando a App A..."
        tvContador.text = ""
    }

    private fun mostrarError(mensaje: String) {
        tvEstado.text = mensaje
        tvContador.text = "0 tareas recibidas"
        adapter.actualizarLista(emptyList())
        recyclerView.visibility = View.GONE
    }
}