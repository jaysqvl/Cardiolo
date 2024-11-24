package com.example.myruns.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.example.myruns.R
import com.example.myruns.services.TrackingService
import com.example.myruns.utils.ConverterUtils
import com.example.myruns.viewmodel.MapEntryViewModel
import com.example.myruns.viewmodel.MapEntryViewModelFactory
import com.example.myruns.database.ExerciseDatabase
import com.example.myruns.database.ExerciseRepository
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*

class MapEntryActivity : AppCompatActivity(), OnMapReadyCallback {

    // Map Variables
    private lateinit var googleMap: GoogleMap
    private var startingLocation: LatLng? = null
    private var currentMarker: Marker? = null
    private var polyline: Polyline? = null
    private var lastCameraUpdateTime = 0L
    private val cameraUpdateInterval = 5000L // Update every 5 seconds

    // Permissions and Readiness Flags
    private var isMapReady = false
    private var permissionsGranted = false

    // UI Variables
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    private lateinit var typeTextView: TextView
    private lateinit var distanceTextView: TextView
    private lateinit var caloriesTextView: TextView
    private lateinit var climbTextView: TextView
    private lateinit var avgSpeedTextView: TextView
    private lateinit var curSpeedTextView: TextView

    // Current Unit Metric Preference
    private var unitPreference: String = "Metric"

    // Input Type and Activity Type Variables (Intent and AutoGen)
    private var inputTypeCode: Int = -1
    private var activityTypeCode: Int = -1
    private var inputTypeString: String = "Unknown"
    private var activityTypeString: String = "Unknown"

    // ViewModel
    private val mapEntryViewModel: MapEntryViewModel by viewModels {
        MapEntryViewModelFactory(
            ExerciseRepository(
                ExerciseDatabase.getInstance(this).exerciseEntryDao
            )
        )
    }

    // Tracking Service Binding
    private var trackingService: TrackingService? = null
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TrackingService.TrackingBinder
            trackingService = binder.getService()
            isServiceBound = true
            observeServiceData()
            Log.d("MapEntryActivity", "Service connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            trackingService = null
            isServiceBound = false
            Log.d("MapEntryActivity", "Service disconnected")
        }
    }

    // Readability Constants
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
//        private const val INPUT_TYPE_GPS = 1 // Never used, commented  out (for future use)
        private const val INPUT_TYPE_AUTOMATIC = 2
    }

    // -----------------------------------------------
    // -------- Activity Lifecycle Code --------------
    // -----------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_entry)

        // Initialize all setup functions
        initializeUIElements()
        extractIntentData()
        setupButtons()
        setupMapFragment()
        retrieveUnitPreference()
        checkAndRequestPermissions()
    }

    override fun onStart() {
        super.onStart()
        // Bind to TrackingService
        Intent(this, TrackingService::class.java).also { intent ->
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        // Unbind from the service
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    // ---------------------------------------
    // -------- Setup Functions --------------
    // ---------------------------------------
    private fun initializeUIElements() {
        saveButton = findViewById(R.id.save_button)
        cancelButton = findViewById(R.id.cancel_button)
        typeTextView = findViewById(R.id.type_text)
        distanceTextView = findViewById(R.id.distance_text)
        caloriesTextView = findViewById(R.id.calories_text)
        climbTextView = findViewById(R.id.climb_text)
        avgSpeedTextView = findViewById(R.id.avg_speed_text)
        curSpeedTextView = findViewById(R.id.cur_speed_text)
    }

    private fun extractIntentData() {
        // Extract inputType and activityType from the Intent
        inputTypeCode = intent.getIntExtra("inputType", -1)
        activityTypeCode = intent.getIntExtra("activityType", -1)

        // Convert codes to string representations
        inputTypeString = ConverterUtils.getInputTypeString(inputTypeCode, this)
        activityTypeString = ConverterUtils.getActivityTypeString(activityTypeCode, this)

        // Display the activity type in the status display area
        typeTextView.text = resources.getString(R.string.map_status_type, activityTypeString)
    }

    private fun setupButtons() {
        saveButton.setOnClickListener {
            saveRoute()
            stopTracking()
            finish() // Finish and save the route
        }

        cancelButton.setOnClickListener {
            stopTracking()
            finish() // Finish without saving
        }
    }

    private fun setupMapFragment() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_container_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun retrieveUnitPreference() {
        val sharedPrefs = getSharedPreferences("app_preferences", MODE_PRIVATE)
        unitPreference = sharedPrefs.getString("unit_preference", "Metric") ?: "Metric"
    }

    // ---------------------------------------
    // -------- Permissions Functions --------
    // ---------------------------------------

    @SuppressLint("InlinedApi")
    private fun hasLocationPermissions(): Boolean {
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

    @SuppressLint("InlinedApi")
    private fun checkAndRequestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        // Always required
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.FOREGROUND_SERVICE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsNeeded.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
            }
        }

        // Request BODY_SENSORS permission if in Automatic mode
        if (inputTypeCode == INPUT_TYPE_AUTOMATIC) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BODY_SENSORS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsNeeded.add(Manifest.permission.BODY_SENSORS)
            }
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsNeeded.toTypedArray(),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Permissions already granted
            permissionsGranted = true
            if (isMapReady) {
                startTrackingService()
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            var allGranted = true
            for (i in grantResults.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    val permission = permissions[i]
                    Log.e("MapEntryActivity", "Permission denied: $permission")
                }
            }

            if (allGranted) {
                permissionsGranted = true
                if (isMapReady) {
                    startTrackingService()
                }
            } else {
                // Permission denied, handle accordingly
                Toast.makeText(
                    this,
                    "Location and sensor permissions are required for tracking.",
                    Toast.LENGTH_LONG
                ).show()
                Log.e("MapEntryActivity", "Required permissions denied.")
                finish()
            }
        }
    }

    // ---------------------------------------
    // ---------- Map Functions --------------
    // ---------------------------------------
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        isMapReady = true
        setupMap()

        if (permissionsGranted) {
            startTrackingService()
        }

        // Initialize polyline here if not already initialized
        polyline = googleMap.addPolyline(
            PolylineOptions()
                .color(Color.BLUE) // Choose a visible color
                .width(8f)
        )
    }

    private fun setupMap() {
        configureMapSettings()
        if (hasLocationPermissions()) {
            fetchLastKnownLocation()
        } else {
            Log.e(
                "MapEntryActivity",
                "Location permissions not granted. Cannot fetch last known location."
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun configureMapSettings() {
        googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = true
        googleMap.isBuildingsEnabled = false

        // Check we've got permissions before enabling My Location layer
        if (hasLocationPermissions()) {
            googleMap.isMyLocationEnabled = false
        } else {
            Log.e(
                "MapEntryActivity",
                "Location permissions not granted. Cannot disable My Location layer."
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLastKnownLocation() {
        val fusedLocationClient =
            com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                Log.d("MapEntryActivity", "Last known location: $latLng")
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                // Do not set startingLocation or add markers here
            } ?: Log.e("MapEntryActivity", "Last location is null")
        }
    }

    private fun initializeMarkers(latLng: LatLng) {
        // Initialize current location marker
        if (currentMarker == null) {
            currentMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Current Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
            Log.d("MapEntryActivity", "Current location marker added")
        }
    }

    private fun updateMap(latLng: LatLng?) {
        latLng?.let { location ->
            // Update the current location marker
            currentMarker?.position = location

            // Update the polyline with the latest path points
            trackingService?.pathPoints?.value?.let { points ->
                polyline?.points = points
            }

//            Log.d("MapEntryActivity", "Polyline updated with new point $location")

            // Update the map whenever the user or program hasn't updated it in the last 5 secs
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastCameraUpdateTime > cameraUpdateInterval) {
                // Create LatLngBounds to fit both starting location and current location
                if (startingLocation != null) {
                    val boundsBuilder = LatLngBounds.Builder()
                    boundsBuilder.include(startingLocation!!)
                    boundsBuilder.include(location)
                    val bounds = boundsBuilder.build()
                    val padding = 100 // Padding in pixels
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                } else {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
                }
                lastCameraUpdateTime = currentTime
            }
        }
    }

    // ---------------------------------------
    // -------- Tracking Functions -----------
    // ---------------------------------------
    private fun startTrackingService() {
        // Start the TrackingService with input and activity types
        val intent = Intent(this, TrackingService::class.java).apply {
            putExtra("inputType", inputTypeCode)
            putExtra("activityType", activityTypeCode)
        }
        ContextCompat.startForegroundService(this, intent)
        Log.d("MapEntryActivity", "TrackingService started.")
    }

    private fun observeServiceData() {
        trackingService?.let { service ->
            service.pathPoints.observe(this, Observer { points ->
                if (points.isNotEmpty()) {
                    // Set starting location if it's null
                    if (startingLocation == null) {
                        startingLocation = points.first()
                        // Add green marker for starting location
                        googleMap.addMarker(
                            MarkerOptions()
                                .position(startingLocation!!)
                                .title("Start")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                        )
                        Log.d("MapEntryActivity", "Starting location set: $startingLocation")
                    }

                    val lastPoint = points.last()
                    initializeMarkers(lastPoint) // Ensure current marker is initialized
                    updateMap(lastPoint)
                }
            })

            service.totalDistance.observe(this, Observer { distance ->
                distanceTextView.text = getString(
                    R.string.map_status_distance,
                    ConverterUtils.formatDistance(distance, unitPreference)
                )
            })

            service.caloriesBurned.observe(this, Observer { calories ->
                caloriesTextView.text = getString(
                    R.string.map_status_calories,
                    calories
                )
            })

            service.averageSpeed.observe(this, Observer { avgSpeed ->
                avgSpeedTextView.text = getString(
                    R.string.map_status_avg_speed,
                    ConverterUtils.formatSpeed(avgSpeed, unitPreference)
                )
            })

            service.currentSpeed.observe(this, Observer { currentSpeed ->
                curSpeedTextView.text = getString(
                    R.string.map_status_cur_speed,
                    ConverterUtils.formatSpeed(currentSpeed, unitPreference)
                )
            })

            service.totalClimb.observe(this, Observer { climb ->
                climbTextView.text = getString(
                    R.string.map_status_climb,
                    ConverterUtils.formatClimb(climb, unitPreference)
                )
            })

            if (inputTypeCode == INPUT_TYPE_AUTOMATIC) {
                // Observe currentActivityLabel
                service.currentActivityLabel.observe(this, Observer { activityLabel ->
                    activityTypeString = activityLabel
                    activityTypeCode = service.activityTypeCode

                    // Update the typeTextView with the correct activity type string
                    typeTextView.text = getString(
                        R.string.map_status_type,
                        ConverterUtils.getActivityTypeString(activityTypeCode, this)
                    )
                })
            }
        }
    }

    private fun stopTracking() {
        // Stop the TrackingService
        val intent = Intent(this, TrackingService::class.java)
        stopService(intent)
    }

    // ---------------------------------------
    // ------------ Handle Save --------------
    // ---------------------------------------
    private fun saveRoute() {
        if (trackingService == null) {
            Toast.makeText(this, "Tracking service not available", Toast.LENGTH_SHORT).show()
            return
        }

        // Sync with tracking service (What you see is what you get to save)
        val pathPoints = trackingService?.pathPoints?.value ?: emptyList()
        if (pathPoints.isEmpty()) {
            Toast.makeText(this, "No route to save", Toast.LENGTH_SHORT).show()
            return
        }

        val startTime = trackingService?.startTime ?: System.currentTimeMillis()
        val durationInMillis = System.currentTimeMillis() - startTime
        val durationInMinutes = durationInMillis / 60000.0
        val totalDistance = trackingService?.totalDistance?.value ?: 0.0
        val caloriesBurned = trackingService?.caloriesBurned?.value ?: 0.0
        val avgSpeed = trackingService?.averageSpeed?.value ?: 0.0
        val avgPace = if (totalDistance > 0) durationInMinutes / totalDistance else 0.0
        val climb = trackingService?.totalClimb?.value ?: 0.0

        // Update activityTypeCode if in Automatic mode
        if (inputTypeCode == INPUT_TYPE_AUTOMATIC) {
            activityTypeCode = trackingService?.activityTypeCode ?: activityTypeCode
        }

        mapEntryViewModel.saveExerciseEntry(
            inputTypeCode = inputTypeCode,
            activityTypeCode = activityTypeCode,
            startTime = startTime,
            durationInMinutes = durationInMinutes,
            totalDistance = totalDistance,
            calories = caloriesBurned,
            avgSpeed = avgSpeed,
            avgPace = avgPace,
            climb = climb,
            pathPoints = pathPoints
        )

        Toast.makeText(this, "Route saved successfully", Toast.LENGTH_SHORT).show()
    }
}
