package com.example.myruns.ui.activities

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.myruns.R
import com.example.myruns.viewmodel.ExerciseViewModel
import java.text.SimpleDateFormat
import java.util.Locale

class DisplayEntryActivity : AppCompatActivity() {

    private val viewModel: ExerciseViewModel by viewModels()
    private var entryId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_entry)

        // Get entry ID from intent
        entryId = intent.getLongExtra("ENTRY_ID", 0)

        // Observe entry details and display them
        viewModel.getEntryById(entryId).observe(this, Observer { entry ->
            entry?.let {
                // Display entry details
                findViewById<TextView>(R.id.ad_input_type_tv).text = String.format(Locale.getDefault(), "%d", entry.inputType)
                findViewById<TextView>(R.id.ad_activity_type_tv).text = String.format(Locale.getDefault(), "%d", entry.activityType)
                val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                findViewById<TextView>(R.id.ad_date_time_tv).text = dateFormat.format(entry.dateTime)
                findViewById<TextView>(R.id.ad_duration_tv).text = String.format(Locale.getDefault(), "%.1f", entry.duration)
                findViewById<TextView>(R.id.ad_distance_tv).text = String.format(Locale.getDefault(), "%.2f", entry.distance)
                findViewById<TextView>(R.id.ad_calories_tv).text = String.format(Locale.getDefault(), "%d", entry.calorie)
                findViewById<TextView>(R.id.ad_heart_rate_tv).text = String.format(Locale.getDefault(), "%d", entry.heartRate)
                findViewById<TextView>(R.id.ad_comment_tv).text = entry.comment
            }
        })

        // Set up delete button
        findViewById<Button>(R.id.ad_delete_button).setOnClickListener {
            viewModel.getEntryById(entryId).value?.let { entry ->
                viewModel.delete(entry)  // Deletes based on ID
                finish()
            }
        }
    }
}
