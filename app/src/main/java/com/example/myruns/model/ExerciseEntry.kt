package com.example.myruns.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.android.gms.maps.model.LatLng
import java.util.*

@Entity(tableName = "exercise_entry_table")
@TypeConverters(Converters::class)
data class ExerciseEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val inputType: Int,
    val activityType: Int,
    val dateTime: Calendar,
    val duration: Double,
    val distance: Double,
    val avgPace: Double,
    val avgSpeed: Double,
    val calorie: Double,
    val climb: Double,
    val heartRate: Double,
    val comment: String,
    val locationList: List<LatLng>?
)
