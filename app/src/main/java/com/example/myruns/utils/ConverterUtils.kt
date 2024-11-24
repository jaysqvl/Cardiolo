package com.example.myruns.utils

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.myruns.R
import java.util.Locale

object ConverterUtils {
    // Constants for unit conversion
    private const val METERS_IN_KILOMETER = 1000.0
    private const val FEET_IN_KILOMETER = 3280.84
    private const val MILES_IN_KILOMETER = 0.621371

    // Activity Type Codes that match the spinner indices
    const val RUNNING_CODE = 0
    const val WALKING_CODE = 1
    const val STANDING_CODE = 2
    const val OTHER_CODE = 13

    fun convertSpeed(speedInMps: Double, unitPreference: String): Double {
        return if (unitPreference == "Metric") {
            speedInMps * 3.6 // Convert m/s to km/h
        } else {
            speedInMps * 2.23694 // Convert m/s to mph
        }
    }

    fun convertDistance(distanceInMeters: Double, unitPreference: String): Double {
        return if (unitPreference == "Metric") {
            distanceInMeters / METERS_IN_KILOMETER // Convert to kilometers
        } else {
            distanceInMeters * 0.000621371 // Convert to miles
        }
    }

    fun convertClimb(climbInMeters: Double, unitPreference: String): Double {
        return if (unitPreference == "Metric") {
            climbInMeters // Already in meters
        } else {
            climbInMeters * 3.28084 // Convert to feet
        }
    }

    fun formatDistance(distanceInKilometers: Double, unitPreference: String): String {
        return when (unitPreference) {
            "Metric" -> {
                val distanceInMeters = distanceInKilometers * METERS_IN_KILOMETER
                when {
                    distanceInKilometers >= 1.0 -> {
                        // Display in kilometers with two decimal places
                        String.format(Locale.getDefault(), "%.2f km", distanceInKilometers)
                    }
                    else -> {
                        // Display in meters with no decimal places
                        String.format(Locale.getDefault(), "%.0f m", distanceInMeters)
                    }
                }
            }
            "Imperial" -> {
                val distanceInFeet = distanceInKilometers * FEET_IN_KILOMETER
                val distanceInMiles = distanceInKilometers * MILES_IN_KILOMETER
                when {
                    distanceInMiles >= 0.5 -> {
                        // Display in miles with two decimal places
                        String.format(Locale.getDefault(), "%.2f mi", distanceInMiles)
                    }
                    distanceInFeet >= 100 -> {
                        // Display in yards with no decimal places (1 yard = 3 feet)
                        val distanceInYards = distanceInFeet / 3
                        String.format(Locale.getDefault(), "%.0f yd", distanceInYards)
                    }
                    else -> {
                        // Display in feet with no decimal places
                        String.format(Locale.getDefault(), "%.0f ft", distanceInFeet)
                    }
                }
            }
            else -> {
                // Default to Metric
                val distanceInMeters = distanceInKilometers * METERS_IN_KILOMETER
                when {
                    distanceInKilometers >= 1.0 -> {
                        String.format(Locale.getDefault(), "%.2f km", distanceInKilometers)
                    }
                    else -> {
                        String.format(Locale.getDefault(), "%.0f m", distanceInMeters)
                    }
                }
            }
        }
    }

    fun formatClimb(climbInMeters: Double, unitPreference: String): String {
        val climb = convertClimb(climbInMeters, unitPreference)
        val unit = if (unitPreference == "Metric") "m" else "ft"
        return String.format(Locale.getDefault(), "%.1f %s", climb, unit)
    }

    fun formatSpeed(speedInMps: Double, unitPreference: String): String {
        val speed = convertSpeed(speedInMps, unitPreference)
        val unit = if (unitPreference == "Metric") "km/h" else "mph"
        return String.format(Locale.getDefault(), "%.1f %s", speed, unit)
    }

    fun formatDuration(durationInMinutes: Double): String {
        val totalSeconds = (durationInMinutes * 60).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "$minutes mins $seconds secs"
    }

    // Maps Activity Type Code to String (from spinner)
    fun getActivityTypeString(code: Int, context: Context): String {
        val activityTypes = context.resources.getStringArray(R.array.activity_type_options)
        return if (code in activityTypes.indices) {
            activityTypes[code]
        } else {
            "Unknown"
        }
    }

    // Maps Input Type Code to String (from spinner)
    fun getInputTypeString(code: Int, context: Context): String {
        val inputTypes = context.resources.getStringArray(R.array.input_type_options)
        return if (code in inputTypes.indices) {
            inputTypes[code]
        } else {
            "Unknown"
        }
    }

    // Turns byte array into list of LatLng objects
    fun deserializeLocationList(locationList: ByteArray?): List<LatLng>? {
        if (locationList == null) return null
        val json = String(locationList, Charsets.UTF_8)
        val type = object : TypeToken<List<LatLng>>() {}.type
        return Gson().fromJson(json, type)
    }

    // Maps Classifier Index to Activity Code
    fun getActivityCodeFromClassifierIndex(classifierIndex: Int): Int {
        return when (classifierIndex) {
            0 -> STANDING_CODE    // Classifier index 0 corresponds to 'Standing'
            1 -> WALKING_CODE     // Classifier index 1 corresponds to 'Walking'
            2 -> RUNNING_CODE     // Classifier index 2 corresponds to 'Running'
            3 -> OTHER_CODE       // Classifier index 3 corresponds to 'Others'
            else -> OTHER_CODE    // Default to 'Other'
        }
    }
}
