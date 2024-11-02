package com.example.myruns.ui.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.myruns.R
import com.example.myruns.model.ExerciseEntry
import com.example.myruns.viewmodel.ExerciseViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myruns.viewmodel.ExerciseViewModelFactory
import com.example.myruns.database.ExerciseDatabase
import com.example.myruns.database.ExerciseRepository
import java.util.*

class ManualEntryActivity : AppCompatActivity() {

    private val database by lazy { ExerciseDatabase.getInstance(this) }
    private val repository by lazy { ExerciseRepository(database.exerciseEntryDao) }
    private val viewModel: ExerciseViewModel by lazy {
        ViewModelProvider(this, ExerciseViewModelFactory(repository)).get(ExerciseViewModel::class.java)
    }

    private lateinit var dateTextView: TextView
    private lateinit var timeTextView: TextView
    private lateinit var durationTextView: TextView
    private lateinit var distanceTextView: TextView
    private lateinit var caloriesTextView: TextView
    private lateinit var heartRateTextView: TextView
    private lateinit var commentTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual_entry)

        // Find the TextViews so we can reference them later
        dateTextView = findViewById(R.id.date_textview)
        timeTextView = findViewById(R.id.time_textview)
        durationTextView = findViewById(R.id.duration_textview)
        distanceTextView = findViewById(R.id.distance_textview)
        caloriesTextView = findViewById(R.id.calories_textview)
        heartRateTextView = findViewById(R.id.heartrate_textview)
        commentTextView = findViewById(R.id.comment_textview)

        // Show Date Picker OnClick
        dateTextView.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    dateTextView.text = "$selectedYear/${selectedMonth + 1}/$selectedDay"
                }, year, month, day)
            datePickerDialog.show()
        }

        // Show Time Picker OnClick
        timeTextView.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(this,
                { _, selectedHour, selectedMinute ->
                    timeTextView.text = "$selectedHour:$selectedMinute"
                }, hour, minute, false)
            timePickerDialog.show()
        }

        // Dynamically set the dialog for its associated button press
        durationTextView.setOnClickListener {
            showCustomDialog("Duration", "Enter duration", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        }

        distanceTextView.setOnClickListener {
            showCustomDialog("Distance", "Enter distance", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        }

        caloriesTextView.setOnClickListener {
            showCustomDialog("Calories", "Enter calories", InputType.TYPE_CLASS_NUMBER)
        }

        heartRateTextView.setOnClickListener {
            showCustomDialog("Heart Rate", "Enter heart rate", InputType.TYPE_CLASS_NUMBER)
        }

        commentTextView.setOnClickListener {
            showCustomDialog("Comment", "Enter your comment", InputType.TYPE_CLASS_TEXT)
        }

        // Handle Save button
        val saveButton: TextView = findViewById(R.id.save_button)
        saveButton.setOnClickListener {
            // Save to a database (when implemented in further MyRuns)
            Toast.makeText(this, "Entry Saved", Toast.LENGTH_SHORT).show()
            saveEntry() // Save the entry to the database
        }

        // Handle Cancel button
        val cancelButton: TextView = findViewById(R.id.cancel_button)
        cancelButton.setOnClickListener {
            finish() // Close activity
        }
    }

    // Method to show the custom dialog for various fields (without using XML)
    private fun showCustomDialog(title: String, hint: String, inputType: Int) {
        // Create an EditText programmatically
        val inputEditText = EditText(this)
        inputEditText.hint = hint
        inputEditText.inputType = inputType

        // Create an AlertDialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setView(inputEditText)

        // Add buttons for OK and Cancel
        builder.setPositiveButton("OK") { dialog, _ ->
            val input = inputEditText.text.toString()
            Toast.makeText(this, "$title set to: $input", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        // Show the dialog
        builder.show()
    }

    // Method to save the entry to the database
    private fun saveEntry() {
        // Parse the input values and create an ExerciseEntry
        val entry = ExerciseEntry(
            inputType = 1,  // Assuming 1 means manual input
            activityType = 1,  // Replace with actual activity type
            dateTime = System.currentTimeMillis(),  // Current time for simplicity
            duration = durationTextView.text.toString().toDoubleOrNull() ?: 0.0,
            distance = distanceTextView.text.toString().toDoubleOrNull() ?: 0.0,
            avgPace = 0.0,  // This can be calculated if needed
            avgSpeed = 0.0,  // This can be calculated if needed
            calorie = caloriesTextView.text.toString().toDoubleOrNull() ?: 0.0,
            climb = 0.0,  // Placeholder
            heartRate = heartRateTextView.text.toString().toDoubleOrNull() ?: 0.0,
            comment = commentTextView.text.toString()
        )

        // Insert the entry into the database using the ViewModel
        viewModel.insert(entry)
        Toast.makeText(this, "Entry Saved", Toast.LENGTH_SHORT).show()
        finish() // Close the activity
    }
}
