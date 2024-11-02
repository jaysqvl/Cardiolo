package com.example.myruns.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.myruns.model.ExerciseEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseEntryDao {
    @Insert
    suspend fun insertEntry(entry: ExerciseEntry)

    @Query("SELECT * FROM exercise_table ORDER BY date_time DESC")
    fun getAllEntries(): Flow<List<ExerciseEntry>>

    @Query("SELECT * FROM exercise_table WHERE id = :entryId LIMIT 1")
    fun getEntryById(entryId: Long): LiveData<ExerciseEntry>

    @Query("DELETE FROM exercise_table WHERE id = :entryId")
    suspend fun deleteEntry(entryId: Long)
}

