package com.example.myruns.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myruns.databinding.ItemExerciseEntryBinding
import com.example.myruns.model.ExerciseEntry

class ExerciseEntryAdapter : RecyclerView.Adapter<ExerciseEntryAdapter.ViewHolder>() {

    private var entries = listOf<ExerciseEntry>()

    fun submitList(newEntries: List<ExerciseEntry>) {
        entries = newEntries
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExerciseEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(entries[position])
    }

    override fun getItemCount() = entries.size

    inner class ViewHolder(private val binding: ItemExerciseEntryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: ExerciseEntry) {
            binding.textViewDate.text = entry.dateTime.toString()  // Format as needed
            binding.textViewDistance.text = entry.distance.toString()  // Format as needed
            // Set other views as needed
        }
    }
}
