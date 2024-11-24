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

class ManualDisplayActivity : AppCompatActivity() {

    private val database by lazy { ExerciseDatabase.getInstance(this) }
    private val repository by lazy { ExerciseRepository(database.exerciseEntryDao) }
    private val viewModel: ExerciseViewModel by lazy {
        ViewModelProvider(this, ExerciseViewModelFactory(repository)).get(ExerciseViewModel::class.java)
    }

    private var entryId: Long = -1L // Default: -1L for invalid ID
    private var currentEntry: ExerciseEntry? = null  // Stores the entire entry (for deletion or viewing)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual_display)

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
                currentEntry = entry
                displayEntryDetails(entry)
            }
        })

        // Set up delete button
        findViewById<Button>(R.id.manual_delete_button).setOnClickListener {
            currentEntry?.let { entry ->
                viewModel.delete(entry)  // Deletes based on ID
                finish()
            }
        }
    }

    private fun displayEntryDetails(entry: ExerciseEntry) {
        // Convert input type and activity type to strings and display
        val inputTypeString = ConverterUtils.getInputTypeString(entry.inputType, this)
        val activityTypeString = ConverterUtils.getActivityTypeString(entry.activityType, this)
        findViewById<TextView>(R.id.manual_input_type_tv).text = inputTypeString
        findViewById<TextView>(R.id.manual_activity_type_tv).text = activityTypeString

        // Convert date and time and display
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        findViewById<TextView>(R.id.manual_date_time_tv).text = dateFormat.format(entry.dateTime)

        // Convert duration based on unit preference and display
        val durationString = ConverterUtils.formatDuration(entry.duration)
        findViewById<TextView>(R.id.manual_duration_tv).text = durationString

        // Convert distance based on unit preference and display
        val sharedPrefs = getSharedPreferences("app_preferences", MODE_PRIVATE)
        val unitPreference = sharedPrefs.getString("unit_preference", "Metric") ?: "Metric"
        val convertedDistance = ConverterUtils.convertDistance(entry.distance, unitPreference)
        val distanceUnit = if (unitPreference == "Metric") getString(R.string.unit_kilometers) else getString(R.string.unit_miles)
        findViewById<TextView>(R.id.manual_distance_tv).text = String.format(Locale.getDefault(), "%.2f %s", convertedDistance, distanceUnit)

        // Format calorie, heart rate, and display
        findViewById<TextView>(R.id.manual_calories_tv).text = String.format(Locale.getDefault(), "%.1f cal", entry.calorie)
        findViewById<TextView>(R.id.manual_heart_rate_tv).text = String.format(Locale.getDefault(), "%.1f bpm", entry.heartRate)
        findViewById<TextView>(R.id.manual_comment_tv).text = entry.comment
    }
}
