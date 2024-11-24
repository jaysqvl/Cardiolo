package com.example.myruns.ui.fragments

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myruns.R
import com.example.myruns.adapter.ExerciseEntryAdapter
import com.example.myruns.database.ExerciseDatabase
import com.example.myruns.database.ExerciseRepository
import com.example.myruns.ui.activities.ManualDisplayActivity
import com.example.myruns.ui.activities.MapDisplayActivity
import com.example.myruns.viewmodel.ExerciseViewModel
import com.example.myruns.viewmodel.ExerciseViewModelFactory

class HistoryFragment : Fragment() {

    private val database by lazy { ExerciseDatabase.getInstance(requireContext()) }
    private val repository by lazy { ExerciseRepository(database.exerciseEntryDao) }
    private val viewModel: ExerciseViewModel by viewModels {
        ExerciseViewModelFactory(repository)
    }

    private lateinit var adapter: ExerciseEntryAdapter
    private lateinit var sharedPrefs: SharedPreferences
    private val sharedPreferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "unit_preference") {
            val updatedPreference = sharedPrefs.getString("unit_preference", "Metric") ?: "Metric"
            adapter.updateUnitPreference(updatedPreference) // Update the adapter
            adapter.notifyDataSetChanged() // Refresh the UI
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        // Initialize SharedPreferences
        sharedPrefs = requireContext().getSharedPreferences("app_preferences", android.content.Context.MODE_PRIVATE)
        val unitPreference = sharedPrefs.getString("unit_preference", "Metric") ?: "Metric"

        // Setup RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initialize adapter with onItemClick listener and unit preference
        adapter = ExerciseEntryAdapter(requireContext(), unitPreference) { entry ->
            val intent = if (entry.inputType == 0) { // Manual Entry
                Intent(requireContext(), ManualDisplayActivity::class.java)
            } else { // GPS or Automatic Entry
                Intent(requireContext(), MapDisplayActivity::class.java)
            }
            intent.putExtra("ENTRY_ID", entry.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // Observe the data and update the adapter
        viewModel.allEntries.observe(viewLifecycleOwner) { entries ->
            entries?.let { adapter.submitList(it) }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        // Register the preference change listener
        sharedPrefs.registerOnSharedPreferenceChangeListener(sharedPreferenceListener)
    }

    override fun onPause() {
        super.onPause()
        // Unregister the preference change listener
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(sharedPreferenceListener)
    }
}
