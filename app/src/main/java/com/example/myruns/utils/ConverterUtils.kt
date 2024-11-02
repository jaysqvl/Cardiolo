package com.example.myruns.utils

import android.content.Context
import com.example.myruns.R

// Helper functions to convert between different units and formats
object ConverterUtils {
    fun convertDistance(distanceInKm: Double, unitPreference: String): Double {
        return if (unitPreference == "Metric") {
            distanceInKm
        } else {
            // Convert kilometers to miles
            distanceInKm * 0.621371
        }
    }

    fun formatDuration(durationInMinutes: Double): String {
        val totalSeconds = (durationInMinutes * 60).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "$minutes mins $seconds secs"
    }

    fun getActivityTypeString(code: Int, context: Context): String {
        val activityTypes = context.resources.getStringArray(R.array.activity_type_options)
        return if (code in activityTypes.indices) {
            activityTypes[code]
        } else {
            "Unknown"
        }
    }

    fun getInputTypeString(code: Int, context: Context): String {
        val inputTypes = context.resources.getStringArray(R.array.input_type_options)
        return if (code in inputTypes.indices) {
            inputTypes[code]
        } else {
            "Unknown"
        }
    }
}
