package com.example.myruns

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class ManualEntryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual_entry)

        // Find the TextViews so we can reference them later
        val dateTextView: TextView = findViewById(R.id.date_textview)
        val timeTextView: TextView = findViewById(R.id.time_textview)
        val durationTextView: TextView = findViewById(R.id.duration_textview)
        val distanceTextView: TextView = findViewById(R.id.distance_textview)
        val caloriesTextView: TextView = findViewById(R.id.calories_textview)
        val heartRateTextView: TextView = findViewById(R.id.heartrate_textview)
        val commentTextView: TextView = findViewById(R.id.comment_textview)

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
            showCustomDialog("Duration", "Enter duration")
        }

        distanceTextView.setOnClickListener {
            showCustomDialog("Distance", "Enter distance")
        }

        caloriesTextView.setOnClickListener {
            showCustomDialog("Calories", "Enter calories")
        }

        heartRateTextView.setOnClickListener {
            showCustomDialog("Heart Rate", "Enter heart rate")
        }

        commentTextView.setOnClickListener {
            showCustomDialog("Comment", "Enter your comment")
        }

        // Handle Save button
        val saveButton: TextView = findViewById(R.id.save_button)
        saveButton.setOnClickListener {
            // Save to a database (when implemented in further MyRuns)
            Toast.makeText(this, "Entry Saved", Toast.LENGTH_SHORT).show()
            finish() // Finish activity after saving
        }

        // Handle Cancel button
        val cancelButton: TextView = findViewById(R.id.cancel_button)
        cancelButton.setOnClickListener {
            finish() // Close activity
        }
    }

    // Method to show the custom dialog for various fields (without using XML)
    private fun showCustomDialog(title: String, hint: String) {
        // Create an EditText programmatically
        val inputEditText = EditText(this)
        inputEditText.hint = hint

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
}
