package com.example.myruns.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.myruns.R
import com.example.myruns.adapter.ExerciseEntryAdapter
import com.example.myruns.database.ExerciseRepository
import com.example.myruns.viewmodel.ExerciseViewModel
import com.example.myruns.viewmodel.ExerciseViewModelFactory

class HistoryFragment : Fragment() {

    private lateinit var exerciseViewModel: ExerciseViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        // Initialize the database, repository, ViewModelFactory, and ViewModel
        val database = ExerciseDatabase.getInstance(requireContext())
        val repository = ExerciseRepository(database.exerciseEntryDao())
        val viewModelFactory = ExerciseViewModelFactory(repository)
        exerciseViewModel = ViewModelProvider(this, viewModelFactory).get(ExerciseViewModel::class.java)

        // Observe LiveData from ViewModel and update UI when data changes
        exerciseViewModel.allEntriesLiveData.observe(viewLifecycleOwner, Observer { entries ->
            // Update UI with the list of entries, for example by updating a RecyclerView adapter
            // Example: recyclerViewAdapter.submitList(entries)
        })

        val adapter = ExerciseEntryAdapter()
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)  // Replace with your RecyclerView ID
        recyclerView.adapter = adapter

        // Observe entries and submit to adapter when data changes
        exerciseViewModel.allEntriesLiveData.observe(viewLifecycleOwner, Observer { entries ->
            adapter.submitList(entries)
        })

        return view
    }
}