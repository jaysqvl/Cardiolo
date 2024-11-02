package com.example.myruns.ui.activities

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.myruns.R
import com.example.myruns.database.ExerciseDatabase
import com.example.myruns.database.ExerciseRepository
import com.example.myruns.model.ExerciseEntry
import com.example.myruns.utils.ConverterUtils
import com.example.myruns.viewmodel.ExerciseViewModel
import com.example.myruns.viewmodel.ExerciseViewModelFactory
import java.text.SimpleDateFormat
import java.util.Locale

class DisplayEntryActivity : AppCompatActivity() {

    private val database by lazy { ExerciseDatabase.getInstance(this) }
    private val repository by lazy { ExerciseRepository(database.exerciseEntryDao) }
    private val viewModel: ExerciseViewModel by lazy {
        ViewModelProvider(this, ExerciseViewModelFactory(repository)).get(ExerciseViewModel::class.java)
    }
    private var entryId: Long = -1L  // Use -1L to signify an invalid ID by default
    private var currentEntry: ExerciseEntry? = null  // Variable to store the entry once observed

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_entry)

        // To fix back button not working
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        // Get entry ID from intent
        entryId = intent.getLongExtra("ENTRY_ID", -1L)
        if (entryId == -1L) {
            finish()
            return
        }

        // Observe entry details and display them
        viewModel.getEntryById(entryId).observe(this, Observer { entry ->
            if (entry == null) {
                Log.e("DisplayEntryActivity", "No entry found for ID: $entryId")
            } else {
                currentEntry = entry  // Store the entry so it can be used later for deletion
                displayEntryDetails(entry)
            }
        })

        // Set up delete button
        findViewById<Button>(R.id.ad_delete_button).setOnClickListener {
            currentEntry?.let { entry ->
                viewModel.delete(entry)  // Deletes based on ID
                finish()  // Close activity after deletion
            }
        }
    }

    private fun displayEntryDetails(entry: ExerciseEntry) {
        val inputTypeString = ConverterUtils.getInputTypeString(entry.inputType, this)
        val activityTypeString = ConverterUtils.getActivityTypeString(entry.activityType, this)

        findViewById<TextView>(R.id.ad_input_type_tv).text = inputTypeString
        findViewById<TextView>(R.id.ad_activity_type_tv).text = activityTypeString

        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        findViewById<TextView>(R.id.ad_date_time_tv).text = dateFormat.format(entry.dateTime)

        val durationString = ConverterUtils.formatDuration(entry.duration)
        findViewById<TextView>(R.id.ad_duration_tv).text = durationString

        val sharedPrefs = getSharedPreferences("app_preferences", MODE_PRIVATE)
        val unitPreference = sharedPrefs.getString("unit_preference", "Metric") ?: "Metric"
        val convertedDistance = ConverterUtils.convertDistance(entry.distance, unitPreference)
        val distanceUnit = if (unitPreference == "Metric") getString(R.string.unit_kilometers) else getString(R.string.unit_miles)
        findViewById<TextView>(R.id.ad_distance_tv).text = String.format(Locale.getDefault(), "%.2f %s", convertedDistance, distanceUnit)

        findViewById<TextView>(R.id.ad_calories_tv).text = String.format(Locale.getDefault(), "%.1f cal", entry.calorie)
        findViewById<TextView>(R.id.ad_heart_rate_tv).text = String.format(Locale.getDefault(), "%.1f bpm", entry.heartRate)
        findViewById<TextView>(R.id.ad_comment_tv).text = entry.comment
    }
}
