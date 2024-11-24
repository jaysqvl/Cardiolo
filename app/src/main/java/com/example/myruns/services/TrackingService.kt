// TrackingService.kt

package com.example.myruns.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.example.myruns.R
import com.google.android.gms.location.*

class TrackingService : Service() {

    companion object {
        const val CHANNEL_ID = "tracking_channel"
        const val NOTIFICATION_ID = 1
        val locationUpdates = MutableLiveData<Location?>() // LiveData for location updates
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        Log.d("TrackingService", "Service created")

        // Initialize location client and callback
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                Log.d("TrackingService", "onLocationResult triggered")
                for (location in locationResult.locations) {
                    Log.d("TrackingService", "Location received: $location")
                    locationUpdates.postValue(location) // Send updates to observers
                }
            }

            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                super.onLocationAvailability(locationAvailability)
                if (!locationAvailability.isLocationAvailable) {
                    Log.e("TrackingService", "Location services unavailable.")
                }
            }
        }

        // Create notification channel for foreground service
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("TrackingService", "Service started")

        try {
            // Start foreground service with notification
            startForeground(NOTIFICATION_ID, createNotification())
            Log.d("TrackingService", "startForeground() called successfully")

            if (hasRequiredPermissions()) {
                startLocationUpdates()
            } else {
                Log.e("TrackingService", "Required permissions not granted, stopping service.")
                stopSelf()
            }
        } catch (e: SecurityException) {
            Log.e("TrackingService", "SecurityException: ${e.message}")
            stopSelf()
        } catch (e: Exception) {
            Log.e("TrackingService", "Exception in startForeground: ${e.message}")
            stopSelf()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("TrackingService", "Service destroyed")
        stopLocationUpdates()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("InlinedApi")
    private fun hasRequiredPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val foregroundServiceLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not needed below Android 14
        }
        return fineLocation && foregroundServiceLocation
    }

    private fun startLocationUpdates() {
        if (hasRequiredPermissions()) {
            Log.d("TrackingService", "Requesting location updates")
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setMinUpdateIntervalMillis(500) // Every 2 seconds
                .setMaxUpdateDelayMillis(2000) // Maximum delay of 5 seconds
                .build()

            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    mainLooper
                )
                Log.d("TrackingService", "Location updates requested successfully")
            } catch (e: SecurityException) {
                Log.e("TrackingService", "SecurityException while requesting location updates: ${e.message}")
                stopSelf()
            } catch (e: Exception) {
                Log.e("TrackingService", "Exception while requesting location updates: ${e.message}")
                stopSelf()
            }
        } else {
            Log.e("TrackingService", "Required permissions not granted for location updates.")
            stopSelf()
        }
    }

    private fun stopLocationUpdates() {
        Log.d("TrackingService", "Stopping location updates")
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun createNotification(): Notification {
        val notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Tracking Your Activity")
                .setContentText("Location tracking in progress")
                .setSmallIcon(R.drawable.ic_tracking)
                .setOngoing(true) // Sticky notification
                .setCategory(Notification.CATEGORY_SERVICE) // Proper category for foreground services
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Tracking Your Activity")
                .setContentText("Location tracking in progress")
                .setSmallIcon(R.drawable.ic_tracking) // Replace with a valid icon
                .setOngoing(true) // Sticky notification
        }
        return notificationBuilder.build()
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager?.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Tracking Service",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Notifications for tracking location"
                }
                manager?.createNotificationChannel(channel)
                Log.d("TrackingService", "Notification channel created with ID: $CHANNEL_ID")
            } else {
                Log.d("TrackingService", "Notification channel already exists for ID: $CHANNEL_ID")
            }
        }
    }
}
