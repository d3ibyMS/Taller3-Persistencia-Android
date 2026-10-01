package com.example.mistareas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mistareas.adapter.TareaAdapter
import com.example.mistareas.data.AppDatabase
import com.example.mistareas.data.Tarea
import com.example.mistareas.databinding.ActivityMainBinding
import com.example.mistareas.databinding.DialogTareaBinding
import com.example.mistareas.provider.TareaContentProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * Pantalla principal: "MIS TAREAS".
 *
 * Responsabilidades:
 *  - Mostrar la lista de tareas guardadas en Room/SQLite (READ).
 *  - Crear tareas nuevas desde el FloatingActionButton (CREATE).
 *  - Editar una tarea existente al pulsarla (UPDATE).
 *  - Marcar una tarea como completada/pendiente desde el CheckBox (UPDATE).
 *  - Eliminar una tarea (DELETE).
 *
 * Toda la persistencia es 100% local (Room sobre SQLite) y funciona sin conexión
 * a Internet, tal como lo exige el Punto 2 del taller.
 *
 * Para el Punto 3, esta pantalla no cambia su forma de acceder a los datos
 * (sigue usando TareaDao directamente): lo que se agrega es un panel de
 * resumen (Pendientes/Completadas) y un panel de diagnóstico que confirma
 * que el TareaContentProvider está activo y expone la URI que usará App B.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: TareaAdapter

    // Acceso al DAO a través de la base de datos Room (singleton)
    private val tareaDao by lazy { AppDatabase.getDatabase(applicationContext).tareaDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarToolbar()
        configurarRecyclerView()
        configurarDiagnosticoProvider()
        observarTareas()

        binding.fabAgregar.setOnClickListener {
            mostrarDialogoTarea(tareaExistente = null)
        }
        binding.btnCrearPrimeraTarea.setOnClickListener {
            mostrarDialogoTarea(tareaExistente = null)
        }
    }

    private fun configurarToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.title = getString(R.string.app_name)
        binding.toolbar.subtitle = getString(R.string.subtitulo_principal)
    }

    /**
     * Muestra la URI del ContentProvider en el panel de diagnóstico,
     * confirmando visualmente que queda disponible para App B (Punto 3).
     */
    private fun configurarDiagnosticoProvider() {
        binding.textDiagnosticoUri.text = TareaContentProvider.CONTENT_URI.toString()
    }

    private fun configurarRecyclerView() {
        adapter = TareaAdapter(
            onEditar = { tarea -> mostrarDialogoTarea(tareaExistente = tarea) },
            onEliminar = { tarea -> confirmarEliminacion(tarea) },
            onCambiarEstado = { tarea, completada ->
                actualizarTarea(tarea.copy(estadoCompletado = completada))
            }
        )
        binding.recyclerViewTareas.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewTareas.adapter = adapter
    }

    /**
     * READ: observa el Flow expuesto por Room. Cada vez que la tabla "tareas"
     * cambia (insert/update/delete), este bloque se vuelve a ejecutar y la
     * RecyclerView se actualiza automáticamente. Esto funciona sin conexión
     * a Internet porque la fuente de datos es exclusivamente local.
     */
    private fun observarTareas() {
        lifecycleScope.launch {
            tareaDao.obtenerTareas().collect { lista ->
                adapter.submitList(lista)
                actualizarEstadoVacio(lista.isEmpty())
                actualizarResumen(lista)
            }
        }
    }

    private fun actualizarEstadoVacio(vacio: Boolean) {
        binding.emptyState.visibility = if (vacio) View.VISIBLE else View.GONE
        binding.recyclerViewTareas.visibility = if (vacio) View.GONE else View.VISIBLE
    }

    /**
     * Actualiza el panel "Pendientes: X | Completadas: Y". Se recalcula cada
     * vez que Room emite una nueva lista (tras crear, editar, completar o
     * eliminar una tarea), tal como pide la mejora funcional del Punto 3.
     */
    private fun actualizarResumen(lista: List<Tarea>) {
        val completadas = lista.count { it.estadoCompletado }
        val pendientes = lista.size - completadas
        binding.textResumenContador.text = getString(R.string.resumen_contador, pendientes, completadas)
    }

    /**
     * Muestra el formulario de creación o edición dentro de un AlertDialog.
     * Si [tareaExistente] es null se trata de una creación (CREATE);
     * si no es null, se precargan sus datos para editarla (UPDATE).
     */
    private fun mostrarDialogoTarea(tareaExistente: Tarea?) {
        val dialogBinding = DialogTareaBinding.inflate(LayoutInflater.from(this))

        if (tareaExistente != null) {
            dialogBinding.editTitulo.setText(tareaExistente.titulo)
            dialogBinding.editDescripcion.setText(tareaExistente.descripcion)
            dialogBinding.checkCompletadaDialog.isChecked = tareaExistente.estadoCompletado
        }

        val tituloDialogo = if (tareaExistente == null)
            getString(R.string.titulo_nueva_tarea) else getString(R.string.titulo_editar_tarea)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(tituloDialogo)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_guardar, null) // se sobreescribe abajo para validar antes de cerrar
            .setNegativeButton(R.string.btn_cancelar, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val titulo = dialogBinding.editTitulo.text?.toString()?.trim().orEmpty()
                val descripcion = dialogBinding.editDescripcion.text?.toString()?.trim().orEmpty()
                val completada = dialogBinding.checkCompletadaDialog.isChecked

                if (titulo.isEmpty()) {
                    dialogBinding.layoutTitulo.error = getString(R.string.titulo_requerido)
                    return@setOnClickListener
                }
                dialogBinding.layoutTitulo.error = null

                if (tareaExistente == null) {
                    crearTarea(titulo, descripcion, completada)
                } else {
                    actualizarTarea(
                        tareaExistente.copy(
                            titulo = titulo,
                            descripcion = descripcion,
                            estadoCompletado = completada
                        )
                    )
                }
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    // CREATE
    private fun crearTarea(titulo: String, descripcion: String, completada: Boolean) {
        val nuevaTarea = Tarea(
            titulo = titulo,
            descripcion = descripcion,
            estadoCompletado = completada,
            fechaCreacion = System.currentTimeMillis()
        )
        lifecycleScope.launch {
            tareaDao.insertar(nuevaTarea)
        }
    }

    // UPDATE
    private fun actualizarTarea(tarea: Tarea) {
        lifecycleScope.launch {
            tareaDao.actualizar(tarea)
        }
    }

    // DELETE (con confirmación)
    private fun confirmarEliminacion(tarea: Tarea) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialogo_eliminar_titulo)
            .setMessage(getString(R.string.dialogo_eliminar_mensaje, tarea.titulo))
            .setNegativeButton(R.string.btn_cancelar, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                lifecycleScope.launch {
                    tareaDao.eliminar(tarea)
                }
            }
            .show()
    }
}
