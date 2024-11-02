package com.example.myruns.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.myruns.model.ExerciseEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseEntryDao {

    @Insert
    suspend fun insert(entry: ExerciseEntry)

    @Query("SELECT * FROM exercise_entry_table ORDER BY dateTime DESC")
    fun getAllEntries(): Flow<List<ExerciseEntry>>

    @Query("DELETE FROM exercise_entry_table WHERE id = :entryId")
    suspend fun deleteEntry(entryId: Long)

    @Query("DELETE FROM exercise_entry_table")
    suspend fun deleteAllEntries()
}