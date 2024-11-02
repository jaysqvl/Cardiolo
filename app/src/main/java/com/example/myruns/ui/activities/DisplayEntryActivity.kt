package com.example.myruns.ui.activities

import android.os.Bundle
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.myruns.R
import com.example.myruns.viewmodel.ExerciseViewModel

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
                // Display entry details, e.g., set text on TextViews
                // Example: findViewById<TextView>(R.id.distance_text).text = entry.distance.toString()
            }
        })

        // Set up delete button
        findViewById<Button>(R.id.delete_button).setOnClickListener {
            viewModel.getEntryById(entryId).value?.let { entry ->
                viewModel.delete(entry)
                finish()
            }
        }
    }
}
