package com.example.mistareas.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mistareas.data.Tarea
import com.example.mistareas.databinding.ItemTareaBinding
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter de la lista de tareas. Usa ListAdapter + DiffUtil para actualizar
 * la RecyclerView de forma eficiente cuando Room emite una nueva lista.
 */
class TareaAdapter(
    private val onEditar: (Tarea) -> Unit,
    private val onEliminar: (Tarea) -> Unit,
    private val onCambiarEstado: (Tarea, Boolean) -> Unit
) : ListAdapter<Tarea, TareaAdapter.TareaViewHolder>(TareaDiffCallback()) {

    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TareaViewHolder {
        val binding = ItemTareaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TareaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TareaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TareaViewHolder(private val binding: ItemTareaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(tarea: Tarea) {
            binding.textTitulo.text = tarea.titulo

            if (tarea.descripcion.isBlank()) {
                binding.textDescripcion.visibility = android.view.View.GONE
            } else {
                binding.textDescripcion.visibility = android.view.View.VISIBLE
                binding.textDescripcion.text = tarea.descripcion
            }

            binding.textFecha.text = binding.root.context.getString(
                com.example.mistareas.R.string.creada_prefijo,
                formatoFecha.format(tarea.fechaCreacion)
            )

            // Evita disparar el listener al reciclar la vista
            binding.checkCompletada.setOnCheckedChangeListener(null)
            binding.checkCompletada.isChecked = tarea.estadoCompletado
            aplicarEstiloCompletada(tarea.estadoCompletado)

            binding.checkCompletada.setOnCheckedChangeListener { _, isChecked ->
                aplicarEstiloCompletada(isChecked)
                onCambiarEstado(tarea, isChecked)
            }

            binding.contenedorTexto.setOnClickListener { onEditar(tarea) }
            binding.root.setOnClickListener { onEditar(tarea) }
            binding.btnEliminar.setOnClickListener { onEliminar(tarea) }
        }

        private fun aplicarEstiloCompletada(completada: Boolean) {
            if (completada) {
                // 16 es el valor numérico interno de Paint.STRIKE_THRU_FLAG
                binding.textTitulo.paintFlags = binding.textTitulo.paintFlags or 16
                binding.textTitulo.setTextColor(
                    binding.root.context.getColor(com.example.mistareas.R.color.completed_strike)
                )
            } else {
                // ~16 limpia la bandera del texto tachado
                binding.textTitulo.paintFlags = binding.textTitulo.paintFlags and 16.inv()
                binding.textTitulo.setTextColor(
                    binding.root.context.getColor(com.example.mistareas.R.color.text_primary)
                )
            }
        }

    }

    class TareaDiffCallback : DiffUtil.ItemCallback<Tarea>() {
        override fun areItemsTheSame(oldItem: Tarea, newItem: Tarea): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Tarea, newItem: Tarea): Boolean =
            oldItem == newItem
    }
}
