package com.example.mistareas.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mistareas.R
import com.example.mistareas.data.Tarea
import com.example.mistareas.databinding.ItemTareaBinding
import java.text.SimpleDateFormat
import java.util.Locale

class TareaAdapter(
    private val onEditar: (Tarea) -> Unit,
    private val onEliminar: (Tarea) -> Unit,
    private val onCambiarEstado: (Tarea, Boolean) -> Unit
) : ListAdapter<Tarea, TareaAdapter.TareaViewHolder>(TareaDiffCallback()) {

    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TareaViewHolder {
        val binding = ItemTareaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TareaViewHolder(binding, formatoFecha, onEditar, onEliminar, onCambiarEstado)
    }

    override fun onBindViewHolder(holder: TareaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TareaViewHolder(
        private val binding: ItemTareaBinding,
        private val formatoFecha: SimpleDateFormat,
        private val onEditar: (Tarea) -> Unit,
        private val onEliminar: (Tarea) -> Unit,
        private val onCambiarEstado: (Tarea, Boolean) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(tarea: Tarea) {
            val context = binding.root.context

            binding.textTitulo.text = tarea.titulo

            if (tarea.descripcion.isBlank()) {
                binding.textDescripcion.visibility = View.GONE
            } else {
                binding.textDescripcion.visibility = View.VISIBLE
                binding.textDescripcion.text = tarea.descripcion
            }

            binding.textFecha.text = context.getString(
                R.string.creada_prefijo,
                formatoFecha.format(tarea.fechaCreacion)
            )

            binding.checkCompletada.setOnCheckedChangeListener(null)
            binding.checkCompletada.isChecked = tarea.estadoCompletado
            aplicarEstiloCompletada(tarea.estadoCompletado)

            binding.checkCompletada.setOnCheckedChangeListener { _, isChecked ->
                aplicarEstiloCompletada(isChecked)
                onCambiarEstado(tarea, isChecked)
            }

            binding.root.setOnClickListener { onEditar(tarea) }
            binding.btnEliminar.setOnClickListener { onEliminar(tarea) }
        }

        private fun aplicarEstiloCompletada(completada: Boolean) {
            val context = binding.root.context

            // Activa o desactiva el tachado directamente sin operaciones bitwise
            binding.textTitulo.paint.isStrikeThruText = completada
            binding.textTitulo.invalidate() // Forzar el redibujado de la vista

            if (completada) {
                binding.textTitulo.setTextColor(context.getColor(R.color.completed_strike))
            } else {
                binding.textTitulo.setTextColor(context.getColor(R.color.text_primary))
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