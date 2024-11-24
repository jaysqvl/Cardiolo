package com.example.myruns.viewmodel

import androidx.lifecycle.*
import com.example.myruns.model.ExerciseEntry
import com.example.myruns.database.ExerciseRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.gson.Gson

class MapEntryViewModel(private val repository: ExerciseRepository) : ViewModel() {

    // ----------------------- Saving -----------------------
    fun saveExerciseEntry(
        inputTypeCode: Int,
        activityTypeCode: Int,
        startTime: Long,
        durationInMinutes: Double,
        totalDistance: Double,
        calories: Double,
        avgSpeed: Double,
        avgPace: Double,
        pathPoints: List<LatLng>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val entry = ExerciseEntry(
                inputType = inputTypeCode,
                activityType = activityTypeCode,
                dateTime = startTime,
                duration = durationInMinutes,
                distance = totalDistance,
                avgPace = avgPace,
                avgSpeed = avgSpeed,
                calorie = calories,
                climb = 0.0,
                heartRate = 0.0,
                comment = "",
                locationList = serializePathPoints(pathPoints)
            )
            repository.insertEntry(entry)
        }
    }

    private fun serializePathPoints(pathPoints: List<LatLng>): ByteArray {
        val gson = Gson()
        val json = gson.toJson(pathPoints)
        return json.toByteArray(Charsets.UTF_8)
    }
}

@Suppress("UNCHECKED_CAST")
class MapEntryViewModelFactory(private val repository: ExerciseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapEntryViewModel::class.java)) {
            return MapEntryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
