package com.example.myruns.ui.fragments

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.myruns.ui.activities.ProfileActivity
import com.example.myruns.R
import com.example.myruns.ui.activities.MainActivity

class SettingsFragment : Fragment() {

    private lateinit var privacySettingCheckBox: CheckBox
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize SharedPreferences
        sharedPrefs = requireContext().getSharedPreferences("app_preferences", android.content.Context.MODE_PRIVATE)
        if (!sharedPrefs.contains("unit_preference")) {
            sharedPrefs.edit().putString("unit_preference", "Metric").apply()
        }

        // Account Preferences click listener
        val accountPreferencesLayout = view.findViewById<LinearLayout>(R.id.name_email_class_layout)
        accountPreferencesLayout.setOnClickListener {
            val intent = Intent(requireContext(), ProfileActivity::class.java)
            startActivity(intent)
        }

        // Privacy Setting Switch
        privacySettingCheckBox = view.findViewById(R.id.privacy_setting_checkbox)
        // Load and set the initial state of the switch
        val isPrivate = sharedPrefs.getBoolean("privacy_setting", true)
        privacySettingCheckBox.isChecked = isPrivate

        // Set listener for Switch changes
        privacySettingCheckBox.setOnCheckedChangeListener { _, isChecked ->
            // Save the new privacy setting to SharedPreferences
            sharedPrefs.edit().putBoolean("privacy_setting", isChecked).apply()
            val status = if (isChecked) "Private" else "Public"
            Toast.makeText(requireContext(), "Privacy set to $status", Toast.LENGTH_SHORT).show()
        }

        // Unit Preference click listener
        val unitPreferenceLayout = view.findViewById<LinearLayout>(R.id.unit_preference_layout)
        unitPreferenceLayout.setOnClickListener {
            showUnitPreferenceDialog()
        }

        // Comments click listener
        val commentsLayout = view.findViewById<LinearLayout>(R.id.comments_layout)
        commentsLayout.setOnClickListener {
            showCommentsDialog()
        }

        // Webpage click listener
        val webpageLayout = view.findViewById<LinearLayout>(R.id.webpage_layout)
        webpageLayout.setOnClickListener {
            val webpageUrl = "https://www.sfu.ca/computing.html"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(webpageUrl)
            startActivity(intent)
        }
    }

    // Function to show the Unit Preference dialog
    private fun showUnitPreferenceDialog() {
        val options = arrayOf("Metric (Kilometers)", "Imperial (Miles)")
        val currentPreference = sharedPrefs.getString("unit_preference", "Metric") ?: "Metric"
        val currentSelection = if (currentPreference == "Metric") 0 else 1

        AlertDialog.Builder(requireContext())
            .setTitle("Unit Preference")
            .setSingleChoiceItems(options, currentSelection) { dialog, which ->
                val preference = if (which == 0) "Metric" else "Imperial"

                // Save the new preference
                sharedPrefs.edit().putString("unit_preference", preference).apply()

                // Notify MainActivity or other components
                (activity as? MainActivity)?.refreshHistoryFragment()

                Toast.makeText(requireContext(), "$preference selected", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("CANCEL") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }


    private fun showCommentsDialog() {
        val savedComment = sharedPrefs.getString("comment", "")

        // Create a container (LinearLayout) to hold the EditText
        val container = LinearLayout(requireContext())
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(40, 40, 40, 40) // Add padding (left, top, right, bottom)

        // Create an EditText programmatically
        val editText = EditText(requireContext()).apply {
            hint = "Enter your comment"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minHeight = 48 // Optional: setting a minimum height
            setText(savedComment)
        }

        // Add the EditText to the container
        container.addView(editText)

        // Create the dialog with title and buttons
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Comments")
            .setView(container) // Set the container as the content of the dialog
            .setPositiveButton("OK") { dialog, _ ->
                val comment = editText.text.toString()
                // Save the comment to SharedPreferences
                sharedPrefs.edit().putString("comment", comment).apply()

                if (comment.isNotEmpty()) {
                    Toast.makeText(requireContext(), "Comment saved", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "No comment entered", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("CANCEL") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }
}
