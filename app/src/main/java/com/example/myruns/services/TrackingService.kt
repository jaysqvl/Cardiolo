package com.example.myruns.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.example.myruns.R
import com.example.myruns.ui.activities.MapEntryActivity
import com.example.myruns.utils.ConverterUtils
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng

class TrackingService : Service() {

    companion object {
        const val CHANNEL_ID = "tracking_channel"
        const val NOTIFICATION_ID = 1
    }

    // ----------------------- LiveData Variables -----------------------
    val pathPoints = MutableLiveData<MutableList<LatLng>>(mutableListOf())
    val totalDistance = MutableLiveData<Double>(0.0)
    val caloriesBurned = MutableLiveData<Double>(0.0)
    val currentSpeed = MutableLiveData<Double>(0.0)
    val averageSpeed = MutableLiveData<Double>(0.0)

    // ----------------------- Tracking Variables -----------------------
    var startTime: Long = 0
        private set
    private var lastUpdateTime: Long = 0
    private var lastLatLng: LatLng? = null

    // ----------------------- Location Services -----------------------
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // ----------------------- Binder -----------------------
    private val binder = TrackingBinder()

    inner class TrackingBinder : Binder() {
        fun getService(): TrackingService = this@TrackingService
    }

    // ----------------------- Lifecycle Methods -----------------------
    override fun onCreate() {
        super.onCreate()
        Log.d("TrackingService", "Service created")

        // Initialize tracking variables
        startTime = System.currentTimeMillis()
        lastUpdateTime = startTime
        lastLatLng = null

        // Initialize location client and callback
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                Log.d("TrackingService", "onLocationResult triggered")
                for (location in locationResult.locations) {
                    Log.d("TrackingService", "Location received: $location")
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    val timestamp = location.time
                    val speed = location.speed
                    addPathPoint(currentLatLng, timestamp, speed)
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

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    // ----------------------- Permission Methods -----------------------
    @SuppressLint("InlinedApi")
    private fun hasRequiredPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val foregroundServiceLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.FOREGROUND_SERVICE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not needed below Android 14
        }
        return fineLocation && foregroundServiceLocation
    }

    private fun startLocationUpdates() {
        if (hasRequiredPermissions()) {
            Log.d("TrackingService", "Requesting location updates")
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setMinUpdateIntervalMillis(500) // Every 0.5 seconds
                .setMaxUpdateDelayMillis(2000) // Maximum delay of 2 seconds
                .build()

            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    mainLooper
                )
                Log.d("TrackingService", "Location updates requested successfully")
            } catch (e: SecurityException) {
                Log.e(
                    "TrackingService",
                    "SecurityException while requesting location updates: ${e.message}"
                )
                stopSelf()
            } catch (e: Exception) {
                Log.e(
                    "TrackingService",
                    "Exception while requesting location updates: ${e.message}"
                )
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

    // ----------------------- Notification Methods -----------------------
    @SuppressLint("ObsoleteSdkInt")
    private fun createNotification(): Notification {
        // Create an Intent that will open MapEntryActivity when the notification is tapped
        val notificationIntent = Intent(this, MapEntryActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP

        // Create a PendingIntent wrapping the Intent
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Tracking Your Activity")
                .setContentText("Location tracking in progress")
                .setSmallIcon(R.drawable.ic_tracking)
                .setOngoing(true) // Sticky notification
                .setCategory(Notification.CATEGORY_SERVICE) // Proper category for foreground services
                .setContentIntent(pendingIntent) // Set the PendingIntent
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Tracking Your Activity")
                .setContentText("Location tracking in progress")
                .setSmallIcon(R.drawable.ic_tracking) // Replace with a valid icon
                .setOngoing(true) // Sticky notification
                .setContentIntent(pendingIntent) // Set the PendingIntent
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

    // ----------------------- Tracking Methods -----------------------
    private fun addPathPoint(point: LatLng, timestamp: Long, speed: Float) {
        val currentPoints = pathPoints.value ?: mutableListOf()
        if (currentPoints.isEmpty()) {
            // Ensure the first point is added to pathPoints immediately
            pathPoints.postValue(mutableListOf(point))
        } else {
            currentPoints.add(point)
            pathPoints.postValue(currentPoints)
        }
        Log.d("Service_Debug", "Path Points Updated: ${currentPoints.size}")

        if (lastLatLng != null) {
            // Calculate distance between the last point and the current point.
            val results = FloatArray(1)
            Location.distanceBetween(
                lastLatLng!!.latitude, lastLatLng!!.longitude,
                point.latitude, point.longitude,
                results
            )
            val distanceIncrementMeters = results[0].toDouble()

            // Update total distance (in kilometers).
            val updatedTotalDistance =
                (totalDistance.value ?: 0.0) + (distanceIncrementMeters / 1000.0)
            totalDistance.postValue(updatedTotalDistance)
            Log.d("Service_Debug", "Total Distance Updated: $updatedTotalDistance km")

            // Calculate time difference in hours.
            val durationMillis = timestamp - lastUpdateTime
            val durationHours = durationMillis / (1000.0 * 3600.0)

            // Use the provided speed value for current speed.
            currentSpeed.postValue(ConverterUtils.convertSpeed(speed.toDouble(), "Metric")) // Convert m/s to km/h
            Log.d("Service_Debug", "Current Speed Updated: ${currentSpeed.value} km/h")

            // Calculate average speed since tracking started.
            val totalDurationHours = (timestamp - startTime) / (1000.0 * 3600.0)
            val avgSpeed = if (totalDurationHours > 0) updatedTotalDistance / totalDurationHours else 0.0
            averageSpeed.postValue(avgSpeed)
            Log.d("Service_Debug", "Average Speed Updated: $avgSpeed km/h")

            // Update calories burned.
            val weightKg = 70.0 // Example weight; consider making this dynamic.
            val met = 8.0 // MET value for running; consider parameterizing this.
            val calories = met * weightKg * durationHours
            val updatedCalories = (caloriesBurned.value ?: 0.0) + calories
            caloriesBurned.postValue(updatedCalories)
            Log.d("Service_Debug", "Calories Burned Updated: $updatedCalories")
        } else {
            Log.d("Service_Debug", "Initializing lastLatLng: $point")
        }

        // Update lastLatLng and lastUpdateTime
        lastLatLng = point
        lastUpdateTime = timestamp
    }
}
