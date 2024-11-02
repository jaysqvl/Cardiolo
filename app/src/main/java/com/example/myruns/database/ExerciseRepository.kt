package com.example.myruns.database

import com.example.myruns.model.ExerciseEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ExerciseRepository(private val exerciseEntryDao: ExerciseEntryDao) {

    val allEntries: Flow<List<ExerciseEntry>> = exerciseEntryDao.getAllEntries()

    suspend fun insert(entry: ExerciseEntry) {
        withContext(Dispatchers.IO) {
            exerciseEntryDao.insert(entry)
        }
    }

    suspend fun deleteEntry(entryId: Long) {
        withContext(Dispatchers.IO) {
            exerciseEntryDao.deleteEntry(entryId)
        }
    }

    suspend fun deleteAllEntries() {
        withContext(Dispatchers.IO) {
            exerciseEntryDao.deleteAllEntries()
        }
    }
}
