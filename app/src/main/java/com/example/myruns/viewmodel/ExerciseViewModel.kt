package com.example.myruns.viewmodel

import androidx.lifecycle.*
import com.example.myruns.model.ExerciseEntry
import com.example.myruns.database.ExerciseRepository
import kotlinx.coroutines.launch

class ExerciseViewModel(private val repository: ExerciseRepository) : ViewModel() {
    val allEntries: LiveData<List<ExerciseEntry>> = repository.allEntries.asLiveData()

    fun insert(entry: ExerciseEntry) {
        viewModelScope.launch {
            repository.insertEntry(entry)
        }
    }

    fun delete(entry: ExerciseEntry) {
        viewModelScope.launch {
            repository.deleteEntry(entry.id)
        }
    }

    fun getEntryById(entryId: Long): LiveData<ExerciseEntry> {
        return repository.getEntryById(entryId)
    }
}

@Suppress("UNCHECKED_CAST")
class ExerciseViewModelFactory(private val repository: ExerciseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExerciseViewModel::class.java)) {
            return ExerciseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}