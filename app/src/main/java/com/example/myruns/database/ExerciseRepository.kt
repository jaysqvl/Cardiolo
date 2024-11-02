package com.example.myruns.database

import androidx.lifecycle.LiveData
import com.example.myruns.model.ExerciseEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ExerciseRepository(private val dao: ExerciseEntryDao) {

    val allEntries: Flow<List<ExerciseEntry>> = dao.getAllEntries()

    // Insert a new entry into the database access object using the IO coroutine dispatcher
    suspend fun insertEntry(entry: ExerciseEntry) {
        withContext(Dispatchers.IO) {
            dao.insertEntry(entry)
        }
    }

    // Delete entry from the DAO using the IO coroutine dispatcher
    suspend fun deleteEntry(entryId: Long) {
        withContext(Dispatchers.IO) {
            dao.deleteEntry(entryId)
        }
    }

    // Get all entries from the DAO (to be displayed in the RecyclerView)
    fun getEntryById(entryId: Long): LiveData<ExerciseEntry> {
        return dao.getEntryById(entryId)
    }
}
