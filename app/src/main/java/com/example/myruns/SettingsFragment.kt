package com.example.myruns

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Account Preferences click listener
        val accountPreferencesLayout = view.findViewById<LinearLayout>(R.id.name_email_class_layout)
        accountPreferencesLayout.setOnClickListener {
            val intent = Intent(requireContext(), ProfileActivity::class.java)
            startActivity(intent)
        }

        // Privacy Setting click listener
        val privacySettingLayout = view.findViewById<LinearLayout>(R.id.privacy_setting_layout)
        privacySettingLayout.setOnClickListener {
            // Save the current privacy setting to SharedPreferences
        }

        // Unit Preference click listener
        val unitPreferenceLayout = view.findViewById<LinearLayout>(R.id.unit_preference_layout)
        unitPreferenceLayout.setOnClickListener {
            showUnitPreferenceDialog()
            // Save the selected unit preference to app SharedPreferences
        }

        // Comments click listener
        val commentsLayout = view.findViewById<LinearLayout>(R.id.comments_layout)
        commentsLayout.setOnClickListener {
            showCommentsDialog()
            // Save the entered comment to app SharedPreferences to later upload it to dev?
        }

        // Webpage click listener
        val webpageLayout = view.findViewById<LinearLayout>(R.id.webpage_layout)
        webpageLayout.setOnClickListener {
            val webpageUrl = "https://www.sfu.ca/computing.html"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(webpageUrl)
            startActivity(intent) // Opens the browser with the provided URL
        }
    }

    // Dynamic function to show the Unit Preference dialog
    private fun showUnitPreferenceDialog() {
        val options = arrayOf("Metric (Kilometers)", "Imperial (Miles)")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Unit Preference")
            .setSingleChoiceItems(options, -1) { dialog, which ->
                when (which) {
                    0 -> {
                        Toast.makeText(context, "Metric (Kilometers) selected", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        Toast.makeText(context, "Imperial (Miles) selected", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("CANCEL", DialogInterface.OnClickListener { dialog, _ ->
                dialog.dismiss()
            })
        builder.create().show()
    }

    private fun showCommentsDialog() {
        // Create a container (LinearLayout) to hold the EditText
        val container = LinearLayout(requireContext())
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(40, 40, 40, 40) // Add padding (left, top, right, bottom)

        // Create an EditText programmatically
        val editText = EditText(requireContext()).apply {
            hint = "Enter your comment"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minHeight = 48 // Optional: setting a minimum height
        }

        // Add the EditText to the container
        container.addView(editText)

        // Create the dialog with title and buttons
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Comments")
            .setView(container) // Set the container as the content of the dialog
            .setPositiveButton("OK") { dialog, _ ->
                val comment = editText.text.toString()
                if (comment.isNotEmpty()) {
                    Toast.makeText(requireContext(), "Comment: $comment", Toast.LENGTH_SHORT).show()
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
