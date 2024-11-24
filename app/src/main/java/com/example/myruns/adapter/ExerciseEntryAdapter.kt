package com.example.myruns.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myruns.R
import com.example.myruns.model.ExerciseEntry
import com.example.myruns.utils.ConverterUtils
import java.text.SimpleDateFormat
import java.util.Locale

class ExerciseEntryAdapter(
    private val context: Context,
    private var unitPreference: String,
    private val onItemClick: (ExerciseEntry) -> Unit
) : ListAdapter<ExerciseEntry, ExerciseEntryAdapter.EntryViewHolder>(EntryDiffCallback()) {

    fun updateUnitPreference(preference: String) {
        unitPreference = preference
        notifyDataSetChanged() // Refresh the adapter to reflect the changes
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_exercise_entry, parent, false)
        return EntryViewHolder(view, context, unitPreference, onItemClick)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        val entry = getItem(position)
        holder.bind(entry)
    }

    class EntryViewHolder(
        itemView: View,
        private val context: Context,
        private val unitPreference: String,
        private val onItemClick: (ExerciseEntry) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val entrySummaryTextView: TextView = itemView.findViewById(R.id.ad_entry_summary_tv)
        private val distanceDurationTextView: TextView = itemView.findViewById(R.id.ad_distance_duration_tv)

        fun bind(entry: ExerciseEntry) {
            // Format the date and time
            val dateFormat = SimpleDateFormat("HH:mm:ss MMM d yyyy", Locale.getDefault())
            val formattedDate = dateFormat.format(entry.dateTime)

            // Convert inputType and activityType indices to strings
            val inputTypeString = ConverterUtils.getInputTypeString(entry.inputType, context)
            val activityTypeString = ConverterUtils.getActivityTypeString(entry.activityType, context)

            // Format entry summary with input type, activity type, date, and time
            val entrySummary = "$inputTypeString: $activityTypeString, $formattedDate"
            entrySummaryTextView.text = entrySummary

            // Format distance using the updated formatDistance method
            val distanceString = ConverterUtils.formatDistance(entry.distance, unitPreference)

            // Format duration
            val durationString = ConverterUtils.formatDuration(entry.duration)

            // Combine distance and duration
            val distanceDuration = context.getString(
                R.string.distance_duration_format, // Ensure this string has two placeholders, e.g., "%1$s, %2$s"
                distanceString,
                durationString
            )
            distanceDurationTextView.text = distanceDuration

            // Set up click listener for each item
            itemView.setOnClickListener {
                onItemClick(entry)
            }
        }
    }

    class EntryDiffCallback : DiffUtil.ItemCallback<ExerciseEntry>() {
        override fun areItemsTheSame(oldItem: ExerciseEntry, newItem: ExerciseEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ExerciseEntry, newItem: ExerciseEntry): Boolean {
            return oldItem == newItem
        }
    }
}
