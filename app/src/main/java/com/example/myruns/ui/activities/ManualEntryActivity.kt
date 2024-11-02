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
import androidx.lifecycle.ViewModelProvider
import com.example.myruns.R
import com.example.myruns.model.ExerciseEntry
import com.example.myruns.viewmodel.ExerciseViewModel
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

    private var selectedYear: Int = 0
    private var selectedMonth: Int = 0
    private var selectedDay: Int = 0
    private var selectedHour: Int = 0
    private var selectedMinute: Int = 0

    private var activityType: Int = 0

    // Variables to store input values without showing them in the TextViews
    private var duration: Double = 0.0
    private var distance: Double = 0.0
    private var calories: Double = 0.0
    private var heartRate: Double = 0.0
    private var comment: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual_entry)

        activityType = intent.getIntExtra("activityType", 0) // Default to 0 if not found

        dateTextView = findViewById(R.id.date_textview)
        timeTextView = findViewById(R.id.time_textview)

        // Date Picker
        dateTextView.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this,
                { _, year, month, day ->
                    selectedYear = year
                    selectedMonth = month
                    selectedDay = day
                    dateTextView.text = "$year/${month + 1}/$day"
                }, year, month, day)
            datePickerDialog.show()
        }

        // Time Picker
        timeTextView.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(this,
                { _, hour, minute ->
                    selectedHour = hour
                    selectedMinute = minute
                    timeTextView.text = "$hour:$minute"
                }, hour, minute, false)
            timePickerDialog.show()
        }

        // Set up onClick listeners for the fields that show dialogs (but won't update the TextViews)
        findViewById<TextView>(R.id.duration_textview).setOnClickListener {
            showCustomDialog("Duration", "Enter duration",
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL) {
                value -> duration = value.toDoubleOrNull() ?: 0.0
            }
        }

        findViewById<TextView>(R.id.distance_textview).setOnClickListener {
            showCustomDialog("Distance", "Enter distance",
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL) {
                value -> distance = value.toDoubleOrNull() ?: 0.0
            }
        }


        findViewById<TextView>(R.id.calories_textview).setOnClickListener {
            showCustomDialog(
                "Calories",
                "Enter calories",
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            ) { value -> calories = value.toDoubleOrNull() ?: 0.0 }
        }

        findViewById<TextView>(R.id.heartrate_textview).setOnClickListener {
            showCustomDialog(
                "Heart Rate",
                "Enter heart rate",
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            ) { value -> heartRate = value.toDoubleOrNull() ?: 0.0 }
        }

        findViewById<TextView>(R.id.comment_textview).setOnClickListener {
            showCustomDialog("Comment", "Enter your comment",
            InputType.TYPE_CLASS_TEXT) {
                value -> comment = value
            }
        }

        // Save Button
        findViewById<TextView>(R.id.save_button).setOnClickListener {
            saveEntry()
            Toast.makeText(this, "Entry Saved", Toast.LENGTH_SHORT).show()
        }

        // Cancel Button
        findViewById<TextView>(R.id.cancel_button).setOnClickListener {
            finish() // Close activity
        }
    }

    private fun showCustomDialog(title: String, hint: String, inputType: Int, onSave: (String) -> Unit) {
        val inputEditText = EditText(this)
        inputEditText.hint = hint
        inputEditText.inputType = inputType

        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setView(inputEditText)

        builder.setPositiveButton("OK") { dialog, _ ->
            val input = inputEditText.text.toString()
            onSave(input) // Store the input in the corresponding variable
            Toast.makeText(this, "$title set to: $input", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun saveEntry() {
        // Set calendar with selected date and time
        val calendar = Calendar.getInstance()
        calendar.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute)
        val dateTimeInMillis = calendar.timeInMillis

        // Calculate avgSpeed and avgPace
        val durationInHours = duration / 60.0
        val avgSpeed = if (durationInHours > 0) distance / durationInHours else 0.0
        val avgPace = if (distance > 0) duration / distance else 0.0

        // Fixed inputType for manual entry
        val inputType = 0

        val entry = ExerciseEntry(
            inputType = inputType,
            activityType = activityType,
            dateTime = dateTimeInMillis,
            duration = duration,
            distance = distance,
            avgPace = avgPace,
            avgSpeed = avgSpeed,
            calorie = calories,
            climb = 0.0,
            heartRate = heartRate,
            comment = comment
        )

        viewModel.insert(entry)
        finish()
    }
}
