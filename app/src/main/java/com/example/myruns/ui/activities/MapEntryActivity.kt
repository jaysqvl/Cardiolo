package com.example.myruns.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
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
import com.example.myruns.service.TrackingService
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

    // ----------------------- Map-Related Variables -----------------------
    private lateinit var googleMap: GoogleMap
    private var startingLocation: LatLng? = null
    private var currentMarker: Marker? = null
    private var polyline: Polyline? = null

    // ----------------------- Permission and Map Readiness Flags -----------------------
    private var isMapReady = false
    private var permissionsGranted = false

    // ----------------------- UI Elements -----------------------
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    private lateinit var typeTextView: TextView
    private lateinit var distanceTextView: TextView
    private lateinit var caloriesTextView: TextView
    private lateinit var climbTextView: TextView
    private lateinit var avgSpeedTextView: TextView
    private lateinit var curSpeedTextView: TextView

    // ----------------------- Unit Preference -----------------------
    private var unitPreference: String = "Metric"

    // ----------------------- Input and Activity Type Codes and Strings -----------------------
    private var inputTypeCode: Int = -1
    private var activityTypeCode: Int = -1
    private var inputTypeString: String = "Unknown"
    private var activityTypeString: String = "Unknown"

    // ----------------------- ViewModel -----------------------
    private val mapEntryViewModel: MapEntryViewModel by viewModels {
        MapEntryViewModelFactory(
            ExerciseRepository(
                ExerciseDatabase.getInstance(this).exerciseEntryDao
            )
        )
    }

    // ----------------------- Constants -----------------------
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    // ----------------------- Lifecycle Methods -----------------------
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
        observeViewModel()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
    }

    override fun onResume() {
        super.onResume()
        if (isMapReady && permissionsGranted) {
            startTrackingAndObserve()
        }
    }

    // ----------------------- UI Initialization Methods -----------------------
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

    // ----------------------- Permission Handling Methods -----------------------
    @SuppressLint("InlinedApi")
    private fun hasLocationPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val foregroundServiceLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not needed below Android 14
        }
        return fineLocation && foregroundServiceLocation
    }

    @SuppressLint("InlinedApi")
    private fun checkAndRequestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
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
                startTrackingAndObserve()
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
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    break
                }
            }

            if (allGranted) {
                permissionsGranted = true
                if (isMapReady) {
                    startTrackingAndObserve()
                }
            } else {
                // Permission denied, handle accordingly
                Toast.makeText(this, "Location permissions are required for tracking.", Toast.LENGTH_LONG).show()
                Log.e("MapEntryActivity", "Location permissions denied.")
                // Optionally, disable tracking features or close the activity
                finish()
            }
        }
    }

    // ----------------------- Map Ready Callback -----------------------
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        isMapReady = true
        setupMap()

        if (permissionsGranted) {
            startTrackingAndObserve()
        }

        // **Initialize polyline here if not already initialized**
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
            Log.e("MapEntryActivity", "Location permissions not granted. Cannot fetch last known location.")
        }
    }


    @SuppressLint("MissingPermission")
    private fun configureMapSettings() {
        googleMap.mapType  = GoogleMap.MAP_TYPE_HYBRID
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = true
        googleMap.isBuildingsEnabled = false

        // Check if permissions are granted before enabling My Location layer
        if (hasLocationPermissions()) {
            // Don't need to worry about permissions here because we're just setting it to false
            // Throws the error whenever we do anything with location by default
            googleMap.isMyLocationEnabled = false
        } else {
            Log.e("MapEntryActivity", "Location permissions not granted. Cannot disable My Location layer.")
        }
    }


    @SuppressLint("MissingPermission")
    private fun fetchLastKnownLocation() {
        val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                Log.d("MapEntryActivity", "Last known location: $latLng")
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                initializeMarkers(latLng)

                // Add the starting point to the ViewModel's pathPoints
                val speed = it.speed // Speed in m/s (may default to 0.0 for last known location)
                mapEntryViewModel.addPathPoint(latLng, it.time, speed)
                Log.d("MapEntryActivity", "Starting point added to ViewModel's pathPoints with speed: $speed")
            } ?: Log.e("MapEntryActivity", "Last location is null")
        }
    }

    private fun initializeMarkers(latLng: LatLng) {
        if (startingLocation == null) {
            startingLocation = latLng
            // Add green marker for starting location
            googleMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Start")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )
            Log.d("MapEntryActivity", "Starting location set: $latLng")
        }

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
            polyline?.points = mapEntryViewModel.pathPoints.value ?: mutableListOf()

            Log.d("MapEntryActivity", "Polyline updated with new point $location")

            // Create LatLngBounds to fit both starting location and current location
            if (startingLocation != null) {
                val boundsBuilder = LatLngBounds.Builder()
                boundsBuilder.include(startingLocation!!) // Include starting location
                boundsBuilder.include(location) // Include current location

                val bounds = boundsBuilder.build()

                // Adjust the camera to fit the bounds with some padding
                val padding = 100 // Padding in pixels
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
            } else {
                // If starting location is not yet set, move camera to current location
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
            }
        }
    }

    // ----------------------- ViewModel Observation -----------------------
    private fun observeViewModel() {
        // Observe path points to update the map
        mapEntryViewModel.pathPoints.observe(this, Observer { points ->
            if (points.isNotEmpty()) {
                val lastPoint = points.last()
                updateMap(lastPoint)
            }
        })

        // Observe total distance to update the UI
        mapEntryViewModel.totalDistance.observe(this, Observer { distance ->
            distanceTextView.text = getString(
                R.string.map_status_distance,
                ConverterUtils.formatDistance(distance, unitPreference)
            )
        })

        // Observe calories burned to update the UI
        mapEntryViewModel.caloriesBurned.observe(this, Observer { calories ->
            caloriesTextView.text = getString(
                R.string.map_status_calories,
                calories // Ensure calories is a Double or Float
            )
        })

        // Observe average speed to update the UI
        mapEntryViewModel.averageSpeed.observe(this, Observer { avgSpeed ->
            avgSpeedTextView.text = getString(
                R.string.map_status_avg_speed,
                ConverterUtils.formatSpeed(avgSpeed, unitPreference)
            )
        })

        // Observe current speed to update the UI
        mapEntryViewModel.currentSpeed.observe(this, Observer { currentSpeed ->
            curSpeedTextView.text = getString(
                R.string.map_status_cur_speed,
                ConverterUtils.formatSpeed(currentSpeed, unitPreference)
            )
        })

        // Observe climb to update the UI
//        mapEntryViewModel.climb.observe(this, Observer { climb ->
//            climbTextView.text = getString(
//                R.string.map_status_climb,
//                ConverterUtils.formatClimb(climb, unitPreference)
//            )
//        })
    }


    // ----------------------- Tracking Methods -----------------------
    @SuppressLint("MissingPermission")
    private fun startTrackingAndObserve() {
        // Initialize tracking in ViewModel
        mapEntryViewModel.startTracking()
        Log.d("MapEntryActivity", "Tracking started in ViewModel.")

        // Start the TrackingService
        val intent = Intent(this, TrackingService::class.java)
        ContextCompat.startForegroundService(this, intent)
        Log.d("MapEntryActivity", "TrackingService started.")

        // Observe location updates from TrackingService
        TrackingService.locationUpdates.observe(this, Observer { location ->
            location?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                val timestamp = it.time
                val speed = it.speed // Speed in m/s

                Log.d("Tracking_Debug", "Location Update: $currentLatLng at $timestamp with speed ${speed * 3.6} km/h")

                // Add the new path point and use the speed value
                mapEntryViewModel.addPathPoint(currentLatLng, timestamp, speed)
            }
        })
    }

    private fun saveRoute() {
        val pathPoints = mapEntryViewModel.pathPoints.value ?: emptyList()
        if (pathPoints.isEmpty()) {
            Toast.makeText(this, "No route to save", Toast.LENGTH_SHORT).show()
            Log.d("MapEntryActivity", "No path points available to save.")
            return
        }

        val durationInMillis = System.currentTimeMillis() - mapEntryViewModel.startTime
        val durationInMinutes = durationInMillis / 60000.0
        val totalDistance = mapEntryViewModel.totalDistance.value ?: 0.0
        val caloriesBurned = mapEntryViewModel.caloriesBurned.value ?: 0.0
        val avgSpeed = mapEntryViewModel.averageSpeed.value ?: 0.0
        val avgPace = if (totalDistance > 0) durationInMinutes / totalDistance else 0.0

        mapEntryViewModel.saveExerciseEntry(
            inputTypeCode = inputTypeCode,
            activityTypeCode = activityTypeCode,
            startTime = mapEntryViewModel.startTime,
            durationInMinutes = durationInMinutes,
            totalDistance = totalDistance,
            calories = caloriesBurned,
            avgSpeed = avgSpeed,
            avgPace = avgPace,
            pathPoints = pathPoints
        )
        Log.d("MapEntryActivity", "Route saved to database.")

        Toast.makeText(this, "Route saved successfully", Toast.LENGTH_SHORT).show()
    }

    private fun stopTracking() {
        // Stop the TrackingService
        val intent = Intent(this, TrackingService::class.java)
        stopService(intent)
        Log.d("MapEntryActivity", "Tracking service stopped.")
    }
}
