package com.example.myruns

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.fragment.app.Fragment

class StartFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_start, container, false)

        // Get references to the spinners and start button
        val inputTypeSpinner: Spinner = view.findViewById(R.id.input_type_spinner)
        val activityTypeSpinner: Spinner = view.findViewById(R.id.activity_type_spinner)
        val startButton: Button = view.findViewById(R.id.start_button)

        // Set up the adapter for the Input Type spinner
        // This is so that we can iterate over the options from our strings.xml file
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.input_type_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            inputTypeSpinner.adapter = adapter
        }

        // Set up the adapter for the Activity Type spinner
        // This is so that we can iterate over the options from our strings.xml file
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.activity_type_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            activityTypeSpinner.adapter = adapter
        }

        // Set click listener for the Start button
        startButton.setOnClickListener {
            val selectedInputType = inputTypeSpinner.selectedItem.toString()

            if (selectedInputType == "Manual Entry") {
                // Launch ManualEntryActivity if the selected input type is "Manual Entry"
                val intent = Intent(requireContext(), ManualEntryActivity::class.java)
                startActivity(intent)
            } else if (selectedInputType == "GPS" || selectedInputType == "Automatic") {
                // Launch MapDisplayActivity if the selected input type is "GPS" or "Automatic"
                val intent = Intent(requireContext(), MapDisplayActivity::class.java)
                startActivity(intent)
            }
        }

        return view
    }
}
