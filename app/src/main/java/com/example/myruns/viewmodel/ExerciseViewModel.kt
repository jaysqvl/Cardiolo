package com.example.myruns.viewmodel

import androidx.lifecycle.*
import com.example.myruns.model.ExerciseEntry
import com.example.myruns.database.ExerciseRepository
import kotlinx.coroutines.flow.asLiveData

class ExerciseViewModel(private val repository: ExerciseRepository) : ViewModel() {

    // Convert Flow from the repository to LiveData
    val allEntriesLiveData: LiveData<List<ExerciseEntry>> = repository.allEntries.asLiveData()

    fun insert(entry: ExerciseEntry) {
        repository.insert(entry)
    }

    fun deleteEntryById(id: Long) {
        repository.delete(id)
    }

    fun deleteAll() {
        repository.deleteAll()
    }
}