package com.example.myruns.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myruns.R
import com.example.myruns.adapter.ExerciseEntryAdapter
import com.example.myruns.database.ExerciseDatabase
import com.example.myruns.database.ExerciseRepository
import com.example.myruns.ui.activities.DisplayEntryActivity // Import your DisplayEntryActivity here
import com.example.myruns.viewmodel.ExerciseViewModel
import com.example.myruns.viewmodel.ExerciseViewModelFactory

class HistoryFragment : Fragment() {

    private val database by lazy { ExerciseDatabase.getInstance(requireContext()) }
    private val repository by lazy { ExerciseRepository(database.exerciseEntryDao) }
    private val viewModel: ExerciseViewModel by viewModels {
        ExerciseViewModelFactory(repository)
    }

    private lateinit var adapter: ExerciseEntryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        // Setup RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initialize adapter with onItemClick listener
        adapter = ExerciseEntryAdapter(requireContext()) { entry ->
            // Create intent to launch DisplayEntryActivity
            val intent = Intent(requireContext(), DisplayEntryActivity::class.java).apply {
                putExtra("ENTRY_ID", entry.id) // Pass the entry ID to the activity
            }
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // Observe the data and update the adapter
        viewModel.allEntries.observe(viewLifecycleOwner, Observer { entries ->
            entries?.let { adapter.submitList(it) }
        })

        return view
    }
}
