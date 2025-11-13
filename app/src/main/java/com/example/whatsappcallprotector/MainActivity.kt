package com.example.whatsappcallprotector

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.whatsappcallprotector.service.CallMonitoringService
import com.example.whatsappcallprotector.util.PermissionChecker

/**
 * Main Activity for WhatsApp Call Protector app Handles UI and permission management for the call
 * protection service
 */
class MainActivity : AppCompatActivity() {

    // UI components
    private lateinit var statusText: TextView
    private lateinit var statusDescription: TextView
    private lateinit var dndPermissionStatus: TextView
    private lateinit var accessibilityPermissionStatus: TextView
    private lateinit var microphonePermissionStatus: TextView
    private lateinit var grantPermissionsButton: Button
    private lateinit var startProtectionButton: Button
    private lateinit var stopProtectionButton: Button

    // Permission checker instance
    private lateinit var permissionChecker: PermissionChecker

    /** Called when the activity is first created */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize permission checker
        permissionChecker = PermissionChecker(this)

        // Initialize UI components
        initializeViews()

        // Set up button click listeners
        setupClickListeners()

        // Update UI based on current state
        updateUI()
    }

    /** Initialize all UI views by finding them from the layout */
    private fun initializeViews() {
        statusText = findViewById(R.id.statusText)
        statusDescription = findViewById(R.id.statusDescription)
        dndPermissionStatus = findViewById(R.id.dndPermissionStatus)
        accessibilityPermissionStatus = findViewById(R.id.accessibilityPermissionStatus)
        microphonePermissionStatus = findViewById(R.id.microphonePermissionStatus)
        grantPermissionsButton = findViewById(R.id.grantPermissionsButton)
        startProtectionButton = findViewById(R.id.startProtectionButton)
        stopProtectionButton = findViewById(R.id.stopProtectionButton)
    }

    /** Set up click listeners for all buttons */
    private fun setupClickListeners() {
        // Grant Permissions button
        grantPermissionsButton.setOnClickListener { openPermissionSettings() }

        // Start Protection button
        startProtectionButton.setOnClickListener { startCallProtection() }

        // Stop Protection button
        stopProtectionButton.setOnClickListener { stopCallProtection() }
        // privacy policy button
        findViewById<Button>(R.id.privacyPolicyButton).setOnClickListener {
            val privacyPolicyUrl = getString(R.string.privacy_policy_url)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl))
            startActivity(intent)
        }
    }

    /** Open system settings to grant required permissions */
    private fun openPermissionSettings() {
        // Check which permissions are missing and guide user accordingly
        if (!permissionChecker.hasDndPermission()) {
            // Open DND permission settings
            permissionChecker.openDndSettings()
        } else if (!permissionChecker.hasAccessibilityPermission()) {
            // Open Accessibility settings
            permissionChecker.openAccessibilitySettings()
        } else if (!permissionChecker.hasMicrophonePermission()) {
            // Request microphone permission
            permissionChecker.requestMicrophonePermission(this)
        } else if (!permissionChecker.hasPhoneStatePermission()) {
            // Request phone state permission
            permissionChecker.requestMicrophonePermission(this) // Both are requested together
        }
    }

    /** Start the call protection service */
    private fun startCallProtection() {
        if (permissionChecker.hasAllPermissions()) {
            // Start the foreground service
            val serviceIntent = Intent(this, CallMonitoringService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            updateUI()
        } else {
            // Show message that permissions are required
            openPermissionSettings()
        }
    }

    /** Stop the call protection service */
    private fun stopCallProtection() {
        // Stop the service
        val serviceIntent = Intent(this, CallMonitoringService::class.java)
        stopService(serviceIntent)
        updateUI()
    }

    /** Update the UI based on current service state and permissions */
    private fun updateUI() {
        // Update permission statuses
        updatePermissionStatus()

        // Update service status
        updateServiceStatus()

        // Update button states
        updateButtonStates()
    }

    /** Update permission status displays */
    private fun updatePermissionStatus() {
        // DND permission
        if (permissionChecker.hasDndPermission()) {
            dndPermissionStatus.text = "Granted"
            dndPermissionStatus.setTextColor(getColor(R.color.success_green))
        } else {
            dndPermissionStatus.text = "Not Granted"
            dndPermissionStatus.setTextColor(getColor(R.color.error_red))
        }

        // Accessibility permission
        if (permissionChecker.hasAccessibilityPermission()) {
            accessibilityPermissionStatus.text = "Granted"
            accessibilityPermissionStatus.setTextColor(getColor(R.color.success_green))
        } else {
            accessibilityPermissionStatus.text = "Not Granted"
            accessibilityPermissionStatus.setTextColor(getColor(R.color.error_red))
        }

        // Microphone permission
        if (permissionChecker.hasMicrophonePermission()) {
            microphonePermissionStatus.text = "Granted"
            microphonePermissionStatus.setTextColor(getColor(R.color.success_green))
        } else {
            microphonePermissionStatus.text = "Not Granted"
            microphonePermissionStatus.setTextColor(getColor(R.color.error_red))
        }
    }

    /** Update service status display */
    private fun updateServiceStatus() {
        val isServiceRunning = CallMonitoringService.isRunning

        if (isServiceRunning) {
            statusText.text = getString(R.string.service_running)
            statusText.setTextColor(getColor(R.color.success_green))
            statusDescription.text = "DND will be automatically enabled during WhatsApp calls"
        } else {
            statusText.text = getString(R.string.service_stopped)
            statusText.setTextColor(getColor(R.color.error_red))
            statusDescription.text =
                    "Start protection to automatically enable DND during WhatsApp calls"
        }
    }

    /** Update button states based on permissions and service status */
    private fun updateButtonStates() {
        val hasAllPermissions = permissionChecker.hasAllPermissions()
        val isServiceRunning = CallMonitoringService.isRunning

        // Enable/disable buttons based on state
        startProtectionButton.isEnabled = hasAllPermissions && !isServiceRunning
        stopProtectionButton.isEnabled = isServiceRunning

        // Show/hide grant permissions button
        grantPermissionsButton.isVisible = !hasAllPermissions
    }

    /** Called when the activity resumes */
    override fun onResume() {
        super.onResume()
        // Update UI when returning to the app (e.g., from permission settings)
        updateUI()
    }

    /** Handle permission request results */
    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionChecker.onRequestPermissionsResult(requestCode, permissions, grantResults)
        updateUI()
    }
}
