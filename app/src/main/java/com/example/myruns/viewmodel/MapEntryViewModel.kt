package com.example.myruns.viewmodel

import androidx.lifecycle.*
import com.example.myruns.model.ExerciseEntry
import com.example.myruns.database.ExerciseRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.gson.Gson
import android.util.Log
import com.example.myruns.utils.ConverterUtils

class MapEntryViewModel(private val repository: ExerciseRepository) : ViewModel() {

    // ----------------------- LiveData Variables -----------------------

    // Stores the list of LatLng points representing the user's path.
    private val _pathPoints = MutableLiveData<MutableList<LatLng>>(mutableListOf())
    val pathPoints: LiveData<MutableList<LatLng>> = _pathPoints

    // Total distance traveled in kilometers.
    private val _totalDistance = MutableLiveData<Double>(0.0)
    val totalDistance: LiveData<Double> = _totalDistance

    // Total calories burned.
    private val _caloriesBurned = MutableLiveData<Double>(0.0)
    val caloriesBurned: LiveData<Double> = _caloriesBurned

    // Current speed (e.g., km/h or mph based on user preference).
    private val _currentSpeed = MutableLiveData<Double>(0.0)
    val currentSpeed: LiveData<Double> = _currentSpeed

    // Average speed since tracking started.
    private val _averageSpeed = MutableLiveData<Double>(0.0)
    val averageSpeed: LiveData<Double> = _averageSpeed

    // ----------------------- Tracking Variables -----------------------

    // Timestamp when tracking started.
    var startTime: Long = 0
        private set

    // Timestamp of the last location update.
    private var lastUpdateTime: Long = 0

    // The previous LatLng point to calculate distance and speed.
    private var lastLatLng: LatLng? = null

    // ----------------------- Initialization -----------------------

    fun startTracking() {
        startTime = System.currentTimeMillis()
        lastUpdateTime = startTime
        lastLatLng = null
        resetTrackingData()
        Log.d("MapEntryViewModel", "Tracking started at $startTime")
    }

    fun resetTrackingData() {
        _pathPoints.value = mutableListOf()
        _totalDistance.value = 0.0
        _caloriesBurned.value = 0.0
        _currentSpeed.value = 0.0
        _averageSpeed.value = 0.0
        lastLatLng = null
        lastUpdateTime = startTime
        Log.d("MapEntryViewModel", "Tracking data reset.")
    }

    // ----------------------- Tracking -----------------------
    fun addPathPoint(point: LatLng, timestamp: Long, speed: Float) {
        val currentPoints = _pathPoints.value ?: mutableListOf()
        currentPoints.add(point)
        _pathPoints.value = currentPoints
        Log.d("ViewModel_Debug", "Path Points Updated: ${currentPoints.size}")

        if (lastLatLng != null) {
            // Calculate distance between the last point and the current point.
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                lastLatLng!!.latitude, lastLatLng!!.longitude,
                point.latitude, point.longitude,
                results
            )
            val distanceIncrementMeters = results[0].toDouble()

            // Update total distance (in kilometers).
            val updatedTotalDistance = (_totalDistance.value ?: 0.0) + (distanceIncrementMeters / 1000.0)
            _totalDistance.postValue(updatedTotalDistance)
            Log.d("ViewModel_Debug", "Total Distance Updated: $updatedTotalDistance km")

            // Calculate time difference in hours.
            val durationMillis = timestamp - lastUpdateTime
            val durationHours = durationMillis / (1000.0 * 3600.0)

            // Use the provided speed value for current speed.
            _currentSpeed.value = ConverterUtils.convertSpeed(speed.toDouble(), "Metric") // Convert m/s to km/h
            Log.d("ViewModel_Debug", "Current Speed Updated: ${_currentSpeed.value} km/h")

            // Calculate average speed since tracking started.
            val totalDurationHours = (timestamp - startTime) / (1000.0 * 3600.0)
            val avgSpeed = if (totalDurationHours > 0) updatedTotalDistance / totalDurationHours else 0.0
            _averageSpeed.value = avgSpeed
            Log.d("ViewModel_Debug", "Average Speed Updated: $avgSpeed km/h")

            // Update calories burned.
            val weightKg = 70.0 // Example weight; consider making this dynamic.
            val met = 8.0 // MET value for running; consider parameterizing this.
            val calories = met * weightKg * durationHours
            val updatedCalories = (_caloriesBurned.value ?: 0.0) + calories
            _caloriesBurned.value = updatedCalories
            Log.d("ViewModel_Debug", "Calories Burned Updated: $updatedCalories")
        } else {
            Log.d("ViewModel_Debug", "Initializing lastLatLng: $point")
        }

        // Update lastLatLng and lastUpdateTime
        lastLatLng = point
        lastUpdateTime = timestamp
    }

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
            Log.d("MapEntryViewModel", "Exercise entry saved to database: $entry")
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
