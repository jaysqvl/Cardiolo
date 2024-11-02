package com.example.myruns.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "exercise_table")
data class ExerciseEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "input_type") val inputType: Int,
    @ColumnInfo(name = "activity_type") val activityType: Int,
    @ColumnInfo(name = "date_time") val dateTime: Long,
    @ColumnInfo(name = "duration") val duration: Double,
    @ColumnInfo(name = "distance") val distance: Double,
    @ColumnInfo(name = "avg_pace") val avgPace: Double,
    @ColumnInfo(name = "avg_speed") val avgSpeed: Double,
    @ColumnInfo(name = "calories") val calorie: Double,
    @ColumnInfo(name = "climb") val climb: Double,
    @ColumnInfo(name = "heart_rate") val heartRate: Double,
    @ColumnInfo(name = "comment") val comment: String,
    @ColumnInfo(name = "location_list", typeAffinity = ColumnInfo.BLOB) val locationList: ByteArray? = null
)
