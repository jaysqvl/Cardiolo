package com.example.myruns.ui.activities

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.example.myruns.R
import java.io.File

class ProfileActivity : AppCompatActivity() {

    // UI data fields
    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var classYearEditText: EditText
    private lateinit var majorEditText: EditText
    private lateinit var genderRadioGroup: RadioGroup
    private lateinit var profileImageView: ImageView

    // Camera launcher for capturing images
    private lateinit var cameraLauncher: ActivityResultLauncher<Void?>

    // Register the permission launcher
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ::handleCameraPermissionResult  // Callback to check if permission was granted
    )

    // Temporary bitmap to hold the new profile image until saved
    private var tempBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        // Set up the Toolbar as the ActionBar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Set the title for the ActionBar
        supportActionBar?.title = "MyRuns2"

        // Initialize UI data fields
        nameEditText = findViewById(R.id.nameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        phoneEditText = findViewById(R.id.phoneEditText)
        classYearEditText = findViewById(R.id.classYearEditText)
        majorEditText = findViewById(R.id.majorEditText)
        genderRadioGroup = findViewById(R.id.genderRadioGroup)
        profileImageView = findViewById(R.id.profileImageView)

        // Initialize the camera launcher with a callback to handle captured images
        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) {
                // Store the captured image temporarily
                tempBitmap = bitmap
                // Update the ImageView to display the new image (sets it but doesn't save)
                profileImageView.setImageBitmap(bitmap)
            }
        }

        // Load saved profile data when activity is created
        loadProfile()
    }

    // --- Button click handlers ---
    // Handler for "Change" button click
    fun onChangePhotoClicked(view: View) {
        checkCameraPermission()
    }

    // Handler for "Save" button click
    fun onSaveClicked(view: View) {
        saveProfile()
        finish() // Close the activity after saving changes
    }

    // Handler for "Cancel" button click
    fun onCancelClicked(view: View) {
        finish() // Close the activity don't save changes
    }

    // --- Camera and image handling ---
    // Launch the camera to capture an image
    private fun openCamera() {
        cameraLauncher.launch(null)
    }

    // Separate method to handle permission result
    private fun handleCameraPermissionResult(isGranted: Boolean) {
        if (isGranted) { // Checks if the camera permission was granted by the user
            openCamera()
        } else {
            Toast.makeText(
                this,
                "Camera permission is required to take a photo.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Function to check and request camera permission
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted, open the camera
            openCamera()
        } else {
            // Launch the permission request dialog using the permission launcher
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // --- Persistence ---
    // Load the user's profile data from SharedPreferences
    private fun loadProfile() {
        val sharedPref = getSharedPreferences("userProfile", MODE_PRIVATE)

        // Set the text of the EditText fields from the saved data
        nameEditText.setText(sharedPref.getString("name", ""))
        emailEditText.setText(sharedPref.getString("email", ""))
        phoneEditText.setText(sharedPref.getString("phone", ""))
        classYearEditText.setText(sharedPref.getString("classYear", ""))
        majorEditText.setText(sharedPref.getString("major", ""))

        // Set the selected gender RadioButton based on saved data
        val genderId = sharedPref.getInt("gender", -1)
        if (genderId != -1) {
            genderRadioGroup.check(genderId)
        }

        // Load the saved profile image
        loadProfileImage()
    }

    // Load the user's profile image from internal storage
    private fun loadProfileImage() {
        val sharedPref = getSharedPreferences("userProfile", MODE_PRIVATE)
        val filename = sharedPref.getString("profileImage", null)
        if (filename != null) {
            try {
                val file = File(filesDir, filename)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeStream(openFileInput(filename))
                    profileImageView.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Save the user's profile data to SharedPreferences
    private fun saveProfile() {
        val sharedPref = getSharedPreferences("userProfile", MODE_PRIVATE)

        with(sharedPref.edit()) {
            putString("name", nameEditText.text.toString())
            putString("email", emailEditText.text.toString())
            putString("phone", phoneEditText.text.toString())
            putString("classYear", classYearEditText.text.toString())
            putString("major", majorEditText.text.toString())
            putInt("gender", genderRadioGroup.checkedRadioButtonId)
            apply()
        }

        // Save the new profile image if one was captured
        if (tempBitmap != null) {
            // (tempBitmap is guaranteed to be non-null due to the guard above it)
            saveProfileImage(tempBitmap!!)
            tempBitmap = null // Reset the temp bitmap after saving
        }

        // Show a toast to indicate that the profile has been saved
        Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show()
    }

    // Save the profile image to internal storage
    private fun saveProfileImage(bitmap: Bitmap) {
        val filename = "profile_image.png"
        // Save the bitmap to internal storage
        openFileOutput(filename, MODE_PRIVATE).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        // Save the filename in SharedPreferences
        val sharedPref = getSharedPreferences("userProfile", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("profileImage", filename)
            apply()
        }
    }
}
