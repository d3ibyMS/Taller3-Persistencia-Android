package com.example.appb.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.appb.R
import com.example.appb.model.TareaCompartida
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TareaCompartidaAdapter(
    private var items: List<TareaCompartida> = emptyList()
) : RecyclerView.Adapter<TareaCompartidaAdapter.TareaViewHolder>() {

    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    fun actualizarLista(nuevaLista: List<TareaCompartida>) {
        items = nuevaLista
        notifyDataSetChanged()
    }

    class TareaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo: TextView = view.findViewById(R.id.tvTituloTarea)
        val tvDescripcion: TextView = view.findViewById(R.id.tvDescripcionTarea)
        val tvFecha: TextView = view.findViewById(R.id.tvFechaTarea)
        val tvEstado: TextView = view.findViewById(R.id.tvEstadoTarea)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TareaViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tarea_compartida, parent, false)
        return TareaViewHolder(vista)
    }

    override fun onBindViewHolder(holder: TareaViewHolder, position: Int) {
        val tarea = items[position]
        val contexto = holder.itemView.context

        holder.tvTitulo.text = tarea.titulo
        holder.tvDescripcion.text = tarea.descripcion
        holder.tvFecha.text = formatoFecha.format(Date(tarea.fechaCreacion))

        if (tarea.completada) {
            holder.tvEstado.text = "Completada"
            holder.tvEstado.setBackgroundResource(R.drawable.chip_completada)
            holder.tvEstado.setTextColor(ContextCompat.getColor(contexto, R.color.green_chip_text))
            holder.tvTitulo.paintFlags = holder.tvTitulo.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.tvEstado.text = "Pendiente"
            holder.tvEstado.setBackgroundResource(R.drawable.chip_pendiente)
            holder.tvEstado.setTextColor(ContextCompat.getColor(contexto, R.color.blue_chip_text))
            holder.tvTitulo.paintFlags = holder.tvTitulo.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }

    override fun getItemCount(): Int = items.size
}