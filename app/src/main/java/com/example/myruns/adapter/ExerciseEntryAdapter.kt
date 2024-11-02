package com.example.myruns.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import com.example.myruns.model.ExerciseEntry
import com.example.myruns.R
import java.text.SimpleDateFormat
import java.util.Locale

class ExerciseEntryAdapter(private val context: Context) : ListAdapter<ExerciseEntry, ExerciseEntryAdapter.EntryViewHolder>(EntryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_exercise_entry, parent, false)
        return EntryViewHolder(view, context)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        val entry = getItem(position)
        holder.bind(entry)
    }

    class EntryViewHolder(itemView: View, private val context: Context) : RecyclerView.ViewHolder(itemView) {
        private val entrySummaryTextView: TextView = itemView.findViewById(R.id.ad_entry_summary_tv)
        private val distanceDurationTextView: TextView = itemView.findViewById(R.id.ad_distance_duration_tv)

        fun bind(entry: ExerciseEntry) {
            // Format the date and time
            val dateFormat = SimpleDateFormat("HH:mm:ss MMM d yyyy", Locale.getDefault())
            val formattedDate = dateFormat.format(entry.dateTime)

            // Dynamically format entry summary with entry type, activity type, date, and time
            val entrySummary = "${entry.inputType}: ${entry.activityType}, $formattedDate"
            entrySummaryTextView.text = entrySummary

            // Retrieve the unit preference from SharedPreferences
            val sharedPrefs = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
            val unitPreference = sharedPrefs.getString("unit_preference", "Metric")

            // Format distance based on unit preference
            val distance = if (unitPreference == "Metric") {
                "${entry.distance} Kilometers"
            } else {
                "${entry.distance} Miles"
            }

            // Dynamically format duration
            val duration = "${entry.duration} mins ${entry.duration} secs"
            val distanceDuration = "$distance, $duration"
            distanceDurationTextView.text = distanceDuration
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
