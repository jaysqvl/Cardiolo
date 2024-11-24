package com.example.myruns.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.*
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
import com.example.myruns.utils.ConverterUtils.OTHER_CODE
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.*
import java.util.concurrent.ArrayBlockingQueue
import kotlin.math.sqrt

class TrackingService : Service(), SensorEventListener {

    companion object {
        const val CHANNEL_ID = "tracking_channel"
        const val NOTIFICATION_ID = 1

        // Input Types
//        private const val INPUT_TYPE_GPS = 1 // Never used, commented out for potential future use
        private const val INPUT_TYPE_AUTOMATIC = 2

        // Constants for activity smoothing
        private const val ACTIVITY_SMOOTHING_WINDOW_SIZE = 5
    }

    // Coroutine Scope (so we don't have to keep remaking them in loops)
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    // Variables for Tracking
    val pathPoints = MutableLiveData<MutableList<LatLng>>(mutableListOf())
    val totalDistance = MutableLiveData<Double>(0.0)
    val caloriesBurned = MutableLiveData<Double>(0.0)
    val currentSpeed = MutableLiveData<Double>(0.0)
    val averageSpeed = MutableLiveData<Double>(0.0)
    val totalClimb = MutableLiveData<Double>(0.0)
    val currentActivityLabel = MutableLiveData<String>()
    var startTime: Long = 0
        private set
    private var lastUpdateTime: Long = 0
    private var lastLatLng: LatLng? = null
    private var lastAltitude: Double? = null

    // Setup LocationServices
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // Tracking Binder
    private val binder = TrackingBinder()

    inner class TrackingBinder : Binder() {
        fun getService(): TrackingService = this@TrackingService
    }

    // Sensor Vars
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var useAccelerometerWithGravityCompensation = false

    // Accelerometer Data Buffer
    private val accelerometerBlockCapacity = 64
    private var accelDataBuffer = ArrayBlockingQueue<Double>(accelerometerBlockCapacity)
    private val featureVectorSize = accelerometerBlockCapacity + 1 // FFT coefficients + max magnitude

    // Variables for gravity compensation
    private val gravity = FloatArray(3) { 0f }
    private val linearAcceleration = FloatArray(3)
    private val alpha = 0.8f // Smoothing factor for the low-pass filter

    // Input and Activity Type Codes
    private var inputTypeCode: Int = -1
    var activityTypeCode: Int = -1

    // Activity smoothing variables
    private val activityBuffer = mutableListOf<Int>()
    private var currentSmoothedActivityCode = -1

    // -----------------------------------------------
    // -------- Activity Lifecycle Code --------------
    // -----------------------------------------------
    override fun onCreate() {
        super.onCreate()
        Log.d("TrackingService", "Service created")

        // Initialize tracking variables
        startTime = System.currentTimeMillis()
        lastUpdateTime = startTime
        lastLatLng = null

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Setup the callback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    val timestamp = location.time
                    val speed = location.speed
                    val altitude = location.altitude
                    addPathPoint(currentLatLng, timestamp, speed, altitude)
                }
            }

            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                super.onLocationAvailability(locationAvailability)
                if (!locationAvailability.isLocationAvailable) {
                    Log.e("TrackingService", "Location services unavailable.")
                }
            }
        }

        // Initialize SensorManager and accelerometer
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        if (accelerometer == null) {
            // Fallback to TYPE_ACCELEROMETER with gravity compensation
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            useAccelerometerWithGravityCompensation = true
            Log.w("TrackingService", "TYPE_LINEAR_ACCELERATION not available; using TYPE_ACCELEROMETER with gravity compensation")
        } else {
            useAccelerometerWithGravityCompensation = false
        }

        // Create notification channel for foreground service
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("TrackingService", "Service started")

        // Extract inputTypeCode and activityTypeCode from the intent
        inputTypeCode = intent?.getIntExtra("inputType", -1) ?: -1
        activityTypeCode = intent?.getIntExtra("activityType", -1) ?: -1

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

            // Register accelerometer listener if in Automatic mode
            if (inputTypeCode == INPUT_TYPE_AUTOMATIC) {
                sensorManager.registerListener(
                    this,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_FASTEST
                )
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
        serviceJob.cancel()
        stopLocationUpdates()

        // Unregister accelerometer listener if in Automatic mode
        if (inputTypeCode == INPUT_TYPE_AUTOMATIC) {
            sensorManager.unregisterListener(this)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    // -----------------------------------------------
    // -------- Permissions Functions --------------
    // -----------------------------------------------
    @SuppressLint("InlinedApi")
    private fun hasRequiredPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val bodySensorsPermission = if (inputTypeCode == INPUT_TYPE_AUTOMATIC) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BODY_SENSORS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required for GPS mode
        }

        // For Android 14+, check FOREGROUND_SERVICE_LOCATION
        val foregroundServiceLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.FOREGROUND_SERVICE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not needed below Android 14
        }

        return fineLocation && foregroundServiceLocation && bodySensorsPermission
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

    // -----------------------------------------------
    // ------------- Notification Code ---------------
    // -----------------------------------------------
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

    // -----------------------------------------------
    // -------------- Sensor Functions ---------------
    // -----------------------------------------------
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER || it.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
                // Accelerometer values
                val x: Double
                val y: Double
                val z: Double

                // If we're using TYPE_ACCELEROMETER, we need to apply gravity compensation
                if (useAccelerometerWithGravityCompensation) {
                    // Separates the gravity component from the linear acceleration
                    gravity[0] = alpha * gravity[0] + (1 - alpha) * it.values[0]
                    gravity[1] = alpha * gravity[1] + (1 - alpha) * it.values[1]
                    gravity[2] = alpha * gravity[2] + (1 - alpha) * it.values[2]

                    // Apply gravity compensation to the linear acceleration
                    linearAcceleration[0] = it.values[0] - gravity[0]
                    linearAcceleration[1] = it.values[1] - gravity[1]
                    linearAcceleration[2] = it.values[2] - gravity[2]

                    // Set the values for x, y, and z
                    x = linearAcceleration[0].toDouble()
                    y = linearAcceleration[1].toDouble()
                    z = linearAcceleration[2].toDouble()
                } else {
                    // If it's TYPE_LINEAR_ACCELERATION, use the raw values directly
                    x = it.values[0].toDouble()
                    y = it.values[1].toDouble()
                    z = it.values[2].toDouble()
                }

                // Get the magnitude of the acceleration vector
                val magnitude = sqrt(x * x + y * y + z * z)

                // Add the magnitude to the buffer
                try {
                    accelDataBuffer.add(magnitude)
                } catch (_: IllegalStateException) {
                    // Handle buffer overflow by expanding the buffer
                    val newBuffer = ArrayBlockingQueue<Double>(accelDataBuffer.size * 2)
                    accelDataBuffer.drainTo(newBuffer)
                    accelDataBuffer = newBuffer
                    accelDataBuffer.add(magnitude)
                }

                // If the buffer is full (buff overflow), double the size, refill, then process
                if (accelDataBuffer.size >= accelerometerBlockCapacity) {
                    val blockData = DoubleArray(accelerometerBlockCapacity)
                    for (i in blockData.indices) {
                        blockData[i] = accelDataBuffer.take()
                    }
                    processAccelerometerData(blockData)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No action needed
    }

    private fun processAccelerometerData(blockData: DoubleArray) {
        serviceScope.launch {
            // Compute FFT coefficients
            val fftCoefficients = computeFFT(blockData)

            // Generate feature vector
            val featureVector = generateFeatureVector(fftCoefficients, blockData.maxOrNull() ?: 0.0)

            // Classify the activity
            // We don't need to store it because classifyActivity() will update currentActivityLabel
            classifyActivity(featureVector)

            // Update the activity label on the main thread
            withContext(Dispatchers.Main) {
                updateActivityLabel()
            }
        }
    }

    private fun computeFFT(blockData: DoubleArray): DoubleArray {
        val fft = FFT(accelerometerBlockCapacity)
        val re = blockData.copyOf()
        val im = DoubleArray(accelerometerBlockCapacity) { 0.0 }

        fft.fft(re, im)

        // Compute magnitudes of the FFT coefficients
        val magnitudes = DoubleArray(accelerometerBlockCapacity)
        for (i in re.indices) {
            magnitudes[i] = sqrt(re[i] * re[i] + im[i] * im[i])
        }
        return magnitudes
    }

    private fun generateFeatureVector(fftCoefficients: DoubleArray, maxMagnitude: Double): Array<Double?> {
        val featureVector = arrayOfNulls<Double>(featureVectorSize)
        for (i in fftCoefficients.indices) {
            featureVector[i] = fftCoefficients[i]
        }
        // Append the max magnitude at the end
        featureVector[accelerometerBlockCapacity] = maxMagnitude
        return featureVector
    }

    private fun classifyActivity(featureVector: Array<Double?>): String {
        return try {
            // Classify using the WekaClassifier and update the featureVector
            val classifierIndex = WekaClassifier.classify(featureVector).toInt()
            // Log.d("ActivityRecognition", "Classifier output index: $classifierIndex")

            // Map classifier index to activity code matching spinner indices
            val activityCode = ConverterUtils.getActivityCodeFromClassifierIndex(classifierIndex)
            activityTypeCode = activityCode

            // Get the activity label from the activity code
            val activityLabel = ConverterUtils.getActivityTypeString(activityCode, this)
            // Log.d("ActivityRecognition", "Mapped Activity Label: $activityLabel")
            activityLabel
        } catch (e: Exception) {
            e.printStackTrace()
            activityTypeCode = OTHER_CODE
            "Unknown"
        }
    }

    private fun updateActivityLabel() {
        // Add the detected activity code to the activity buffer
        activityBuffer.add(activityTypeCode)
        if (activityBuffer.size > ACTIVITY_SMOOTHING_WINDOW_SIZE) {
            activityBuffer.removeAt(0)
        }

        // Determine the most frequent activity in the buffer
        val mostFrequentActivityCode = activityBuffer.groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        // Update the current activity label if it has changed (after applying smoothing)
        if (mostFrequentActivityCode != null && mostFrequentActivityCode != currentSmoothedActivityCode) {
            currentSmoothedActivityCode = mostFrequentActivityCode
            val smoothedActivityLabel = ConverterUtils.getActivityTypeString(currentSmoothedActivityCode, this)
            currentActivityLabel.postValue(smoothedActivityLabel)
            // Log.d("ActivityRecognition", "Smoothed Activity: $smoothedActivityLabel")
        }
    }

    // -----------------------------------------------
    // ----------- Location Update Code --------------
    // -----------------------------------------------
    private fun addPathPoint(point: LatLng, timestamp: Long, speed: Float, altitude: Double) {
        val currentPoints = pathPoints.value ?: mutableListOf()
        if (currentPoints.isEmpty()) {
            pathPoints.postValue(mutableListOf(point))
        } else {
            currentPoints.add(point)
            pathPoints.postValue(currentPoints)
        }
//        Log.d("Service_Debug", "Path Points Updated: ${currentPoints.size}")

        // Copy necessary variables to avoid concurrent modification
        val lastLatLngCopy = lastLatLng
        val lastAltitudeCopy = lastAltitude
        val lastUpdateTimeCopy = lastUpdateTime

        // Update lastLatLng, lastUpdateTime, and lastAltitude immediately to ensure consistency
        lastLatLng = point
        lastUpdateTime = timestamp
        lastAltitude = altitude

        serviceScope.launch {
            if (lastLatLngCopy != null && lastAltitudeCopy != null) {
                // Perform heavy computations here

                // Distance calculation
                val results = FloatArray(1)
                Location.distanceBetween(
                    lastLatLngCopy.latitude, lastLatLngCopy.longitude,
                    point.latitude, point.longitude,
                    results
                )
                val distanceIncrementMeters = results[0].toDouble()

                // Update total distance (in kilometers)
                val updatedTotalDistance =
                    (totalDistance.value ?: 0.0) + (distanceIncrementMeters / 1000.0)
                totalDistance.postValue(updatedTotalDistance)
//                Log.d("Service_Debug", "Total Distance Updated: $updatedTotalDistance km")

                // Time difference in hours
                val durationMillis = timestamp - lastUpdateTimeCopy
                val durationHours = durationMillis / (1000.0 * 3600.0)

                // Update current speed
                val convertedSpeed = ConverterUtils.convertSpeed(speed.toDouble(), "Metric")
                currentSpeed.postValue(convertedSpeed)
//                Log.d("Service_Debug", "Current Speed Updated: $convertedSpeed km/h")

                // Calculate average speed
                val totalDurationHours = (timestamp - startTime) / (1000.0 * 3600.0)
                val avgSpeed = if (totalDurationHours > 0) updatedTotalDistance / totalDurationHours else 0.0
                averageSpeed.postValue(avgSpeed)
//                Log.d("Service_Debug", "Average Speed Updated: $avgSpeed km/h")

                // Update calories burned
                val weightKg = 70.0 // You might want to make this dynamic
                val met = 8.0 // Adjust based on activity
                val calories = met * weightKg * durationHours
                val updatedCalories = (caloriesBurned.value ?: 0.0) + calories
                caloriesBurned.postValue(updatedCalories)
//                Log.d("Service_Debug", "Calories Burned Updated: $updatedCalories")

                // Calculate climb
                val altitudeDifference = altitude - lastAltitudeCopy
                if (altitudeDifference > 0) {
                    val updatedTotalClimb = (totalClimb.value ?: 0.0) + altitudeDifference
                    totalClimb.postValue(updatedTotalClimb)
//                    Log.d("Service_Debug", "Total Climb Updated: $updatedTotalClimb")
                }
            } else {
                Log.d("Service_Debug", "First location point received.")
            }
        }
    }
}
