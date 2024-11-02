package com.example.myruns.ui.activities

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.myruns.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@Suppress("unused")
class ProfileActivity : AppCompatActivity() {

    // UI components
    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var classYearEditText: EditText
    private lateinit var majorEditText: EditText
    private lateinit var genderRadioGroup: RadioGroup
    private lateinit var profileImageView: ImageView

    // Camera and gallery launchers
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>

    // Permission request launcher for the camera
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ::handleCameraPermissionResult
    )

    private var currentBitmap: Bitmap? = null
    private var photoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        // Initialize camera launcher with URI
        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && photoUri != null) {
                handleCameraImage(photoUri!!)
            }
        }

        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { handleGalleryImage(it) }
        }

        initializeUIComponents()
        loadProfile()
    }

    // --- Helper Methods ---

    private fun initializeUIComponents() {
        nameEditText = findViewById(R.id.nameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        phoneEditText = findViewById(R.id.phoneEditText)
        classYearEditText = findViewById(R.id.classYearEditText)
        majorEditText = findViewById(R.id.majorEditText)
        genderRadioGroup = findViewById(R.id.genderRadioGroup)
        profileImageView = findViewById(R.id.profileImageView)

        // Set up the Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "MyRuns2"
    }

    private fun loadProfile() {
        val sharedPref = getSharedPreferences("userProfile", MODE_PRIVATE)

        // Load profile text data from SharedPreferences
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

        // Load profile image from internal storage asynchronously
        val savedImagePath = sharedPref.getString("profileImagePath", null)
        if (savedImagePath != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                val bitmap = BitmapFactory.decodeFile(savedImagePath)
                withContext(Dispatchers.Main) {
                    if (bitmap != null) {
                        profileImageView.setImageBitmap(bitmap)
                        currentBitmap = bitmap
                    }
                }
            }
        }
    }

    private fun setImage(bitmap: Bitmap) {
        currentBitmap = bitmap
        profileImageView.setImageBitmap(bitmap)
    }

    // Optimized method to get a scaled bitmap
    private suspend fun getScaledBitmap(uri: Uri): Bitmap? {
        return withContext(Dispatchers.IO) {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                val photoW = options.outWidth
                val photoH = options.outHeight

                // Get the dimensions of the ImageView
                val targetW = profileImageView.width.takeIf { it > 0 } ?: 200
                val targetH = profileImageView.height.takeIf { it > 0 } ?: 200

                // Determine how much to scale down the image
                val scaleFactor = maxOf(1, minOf(photoW / targetW, photoH / targetH))

                // Decode the image file into a Bitmap sized to fill the View
                val options2 = BitmapFactory.Options().apply {
                    inSampleSize = scaleFactor
                    inJustDecodeBounds = false
                }

                contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options2)
                }
            }
        }
    }

    private suspend fun getImageOrientation(uri: Uri): Int {
        return withContext(Dispatchers.IO) {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val exif = ExifInterface(inputStream)
                    when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                } ?: 0 // Return 0 if inputStream is null
            } catch (e: IOException) {
                e.printStackTrace()
                0
            }
        }
    }

    private suspend fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        return withContext(Dispatchers.Default) {
            val matrix = Matrix()
            matrix.postRotate(degrees.toFloat())
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
    }

    private fun handleCameraImage(uri: Uri) {
        lifecycleScope.launch {
            val bitmap = getScaledBitmap(uri)
            val orientation = getImageOrientation(uri)
            val rotatedBitmap = if (orientation != 0 && bitmap != null) {
                rotateBitmap(bitmap, orientation)
            } else {
                bitmap
            }
            rotatedBitmap?.let { setImage(it) }
        }
    }

    private fun handleGalleryImage(uri: Uri) {
        lifecycleScope.launch {
            val bitmap = getScaledBitmap(uri)
            val orientation = getImageOrientation(uri)
            val rotatedBitmap = if (orientation != 0 && bitmap != null) {
                rotateBitmap(bitmap, orientation)
            } else {
                bitmap
            }
            rotatedBitmap?.let { setImage(it) }
        }
    }

    // --- Button Click Handlers ---
    @Suppress("UNUSED_PARAMETER")
    fun onChangePhotoClicked(view: View) {
        showImageSourceDialog()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onSaveClicked(view: View) {
        saveProfile()
        finish()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onCancelClicked(view: View) {
        finish()
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Open Camera", "Select from Gallery")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pick Profile Picture")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> checkCameraPermission()
                1 -> openGallery()
            }
        }
        builder.show()
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val imageFile = File(filesDir, "profile_image_temp.jpg")
        photoUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", imageFile)
        photoUri?.let { uri ->
            cameraLauncher.launch(uri)
        } ?: Toast.makeText(this, "Error creating file for photo", Toast.LENGTH_SHORT).show()
    }

    private fun handleCameraPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission is required to take a photo.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProfile() {
        // Save non-image profile data in SharedPreferences
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

        // Save profile image if there's a new image
        currentBitmap?.let { bitmap ->
            lifecycleScope.launch(Dispatchers.IO) {
                saveProfileImage(bitmap)
                withContext(Dispatchers.Main) {
                    currentBitmap = null // Reset currentBitmap after saving
                    Toast.makeText(this@ProfileActivity, "Profile saved", Toast.LENGTH_SHORT).show()
                }
            }
        } ?: Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show()
    }

    private suspend fun saveProfileImage(bitmap: Bitmap) {
        val filename = "profile_image.png"
        val file = File(filesDir, filename)
        try {
            withContext(Dispatchers.IO) {
                FileOutputStream(file).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                }
            }
            // Save the image path to SharedPreferences
            saveImagePathToPreferences(file.absolutePath)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun saveImagePathToPreferences(filePath: String) {
        val sharedPref = getSharedPreferences("userProfile", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("profileImagePath", filePath)
            apply()
        }
    }
}
