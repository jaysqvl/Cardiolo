package com.example.myruns

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class MapDisplayActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_display)

        // Set up the toolbar with title "Map" title
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Map"

        // Handle "Save" button click
        val saveButton: Button = findViewById(R.id.save_button)
        saveButton.setOnClickListener {
            // Not supposed to be implemented yet but will later
            // Finish activity when Save is pressed
            finish()
        }

        // Handle "Cancel" button click
        val cancelButton: Button = findViewById(R.id.cancel_button)
        cancelButton.setOnClickListener {
            // Finish activity when Cancel is pressed
            finish()
        }
    }
}
