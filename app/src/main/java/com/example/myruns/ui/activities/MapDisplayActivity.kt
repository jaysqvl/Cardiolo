package com.example.myruns.ui.activities

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.myruns.R
import com.example.myruns.database.ExerciseDatabase
import com.example.myruns.database.ExerciseRepository
import com.example.myruns.model.ExerciseEntry
import com.example.myruns.utils.ConverterUtils
import com.example.myruns.viewmodel.ExerciseViewModel
import com.example.myruns.viewmodel.ExerciseViewModelFactory
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

class MapDisplayActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var deleteButton: Button
    private var entryId: Long = -1L
    private var currentEntry: ExerciseEntry? = null

    private val database by lazy { ExerciseDatabase.getInstance(this) }
    private val repository by lazy { ExerciseRepository(database.exerciseEntryDao) }
    private val viewModel: ExerciseViewModel by lazy {
        ViewModelProvider(this, ExerciseViewModelFactory(repository)).get(ExerciseViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_display)

        mapView = findViewById(R.id.map_view)
        deleteButton = findViewById(R.id.delete_button)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        entryId = intent.getLongExtra("ENTRY_ID", -1L)
        if (entryId == -1L) {
            finish()
            return
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        viewModel.getEntryById(entryId).observe(this, Observer { entry ->
            if (entry != null) {
                currentEntry = entry
                displayStats(entry)
                drawRouteOnMap(entry)
            } else {
                Log.e("MapDisplayActivity", "No entry found for ID: $entryId")
                finish()
            }
        })

        deleteButton.setOnClickListener {
            currentEntry?.let { entry ->
                viewModel.delete(entry)
                finish()
            }
        }
    }

    private fun displayStats(entry: ExerciseEntry) {
        val sharedPrefs = getSharedPreferences("app_preferences", MODE_PRIVATE)
        val unitPreference = sharedPrefs.getString("unit_preference", "Metric") ?: "Metric"

        findViewById<TextView>(R.id.type_text).text = getString(
            R.string.map_status_type,
            ConverterUtils.getActivityTypeString(entry.activityType, this)
        )

        findViewById<TextView>(R.id.avg_speed_text).text = getString(
            R.string.map_status_avg_speed,
            ConverterUtils.formatSpeed(entry.avgSpeed, unitPreference)
        )

        findViewById<TextView>(R.id.cur_speed_text).text = getString(
            R.string.map_status_cur_speed,
            "N/A"
        ) // Update if there's a dynamic value for "Cur Speed"

        findViewById<TextView>(R.id.calories_text).text = getString(
            R.string.map_status_calories,
            entry.calorie
        )

        findViewById<TextView>(R.id.climb_text).text = getString(
            R.string.map_status_climb,
            ConverterUtils.formatClimb(entry.climb, unitPreference)
        )

        findViewById<TextView>(R.id.distance_text).text = getString(
            R.string.map_status_distance,
            ConverterUtils.formatDistance(entry.distance, unitPreference)
        )
    }

    private fun drawRouteOnMap(entry: ExerciseEntry) {
        val locationList = ConverterUtils.deserializeLocationList(entry.locationList) ?: emptyList()
        if (locationList.isNotEmpty()) {
            // Draw the polyline
            val polylineOptions = PolylineOptions()
                .addAll(locationList)
                .width(8f)
                .color(Color.BLUE) // Use theme-safe color
            googleMap.addPolyline(polylineOptions)

            // Add a green marker for the first location
            googleMap.addMarker(
                MarkerOptions()
                    .position(locationList.first())
                    .title("Start")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )

            // Add a red marker for the last location
            googleMap.addMarker(
                MarkerOptions()
                    .position(locationList.last())
                    .title("End")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )

            // Adjust the camera to include all points
            adjustCameraToRoute(locationList)
        }
    }

    private fun adjustCameraToRoute(locationList: List<LatLng>) {
        if (locationList.isEmpty()) return

        if (locationList.size == 1) {
            // Only one point, center the map on it with a default zoom level
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locationList.first(), 15f))
            return
        }

        val builder = LatLngBounds.Builder()
        for (location in locationList) {
            builder.include(location)
        }

        val bounds = builder.build()
        val padding = 100 // Padding in pixels

        // Move the camera to the bounds with padding
        googleMap.setOnMapLoadedCallback {
            try {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
            } catch (e: IllegalStateException) {
                // Handle the exception if the map's layout isn't ready
                Log.e("MapDisplayActivity", "Map layout not ready for camera update: ${e.message}")
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
        googleMap.isBuildingsEnabled = false
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
