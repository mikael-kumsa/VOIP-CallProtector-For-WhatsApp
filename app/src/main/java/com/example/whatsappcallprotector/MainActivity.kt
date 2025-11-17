package com.example.whatsappcallprotector

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import com.google.android.material.button.MaterialButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.lal.voipcallprotector.R
import com.example.whatsappcallprotector.service.CallMonitoringService
import com.example.whatsappcallprotector.ui.PermissionWizardDialog
import com.example.whatsappcallprotector.util.AppPreferences
import com.example.whatsappcallprotector.util.PermissionChecker

/**
 * Main Activity for VOIP Call Protector app. Handles UI and permission management for the call
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
    private lateinit var protectionToggleButton: MaterialButton
    private lateinit var viewStatisticsButton: Button
    private lateinit var statisticsSummary: TextView
    private lateinit var statusIndicator: android.view.View

    // Permission checker instance
    private lateinit var permissionChecker: PermissionChecker
    private lateinit var appPreferences: AppPreferences

    /** Called when the activity is first created */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize permission checker and preferences
        permissionChecker = PermissionChecker(this)
        appPreferences = AppPreferences(this)

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
        protectionToggleButton = findViewById(R.id.protectionToggleButton)
        viewStatisticsButton = findViewById(R.id.viewStatisticsButton)
        statisticsSummary = findViewById(R.id.statisticsSummary)
        statusIndicator = findViewById(R.id.statusIndicator)
    }

    /** Set up click listeners for all buttons */
    private fun setupClickListeners() {
        // Grant Permissions button
        grantPermissionsButton.setOnClickListener { openPermissionSettings() }

        // Protection Toggle button - handles both start and stop
        protectionToggleButton.setOnClickListener {
            if (CallMonitoringService.isRunning) {
                stopCallProtection()
            } else {
                startCallProtection()
            }
        }
        // Privacy policy button
        findViewById<Button>(R.id.privacyPolicyButton).setOnClickListener {
            val privacyPolicyUrl = getString(R.string.privacy_policy_url)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl))
            startActivity(intent)
        }
        
        // View statistics button
        viewStatisticsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /** Open system settings to grant required permissions - Simplified flow */
    private fun openPermissionSettings() {
        // Use the permission wizard dialog for a better user experience
        val wizard = PermissionWizardDialog.newInstance {
            // All permissions granted callback
            updateUI()
            android.widget.Toast.makeText(
                this,
                "All permissions granted! You can now start protection.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
        wizard.show(supportFragmentManager, "PermissionWizard")
    }

    /** Start the call protection service */
    private fun startCallProtection() {
        if (permissionChecker.hasAllPermissions()) {
            try {
                // Start the foreground service
                val serviceIntent = Intent(this, CallMonitoringService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
                // Update UI after a short delay to allow service to initialize
                protectionToggleButton.postDelayed({
                    updateUI()
                }, 300)
            } catch (e: IllegalStateException) {
                // Handle case where service cannot be started (e.g., app in background on Android 8+)
                android.widget.Toast.makeText(
                    this,
                    "Cannot start service. Please try again.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                android.util.Log.e("MainActivity", "Failed to start service", e)
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    this,
                    "Error starting protection service: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                android.util.Log.e("MainActivity", "Failed to start service", e)
            }
        } else {
            // Show message that permissions are required
            openPermissionSettings()
        }
    }

    /** Stop the call protection service */
    private fun stopCallProtection() {
        try {
            // Stop the service
            val serviceIntent = Intent(this, CallMonitoringService::class.java)
            stopService(serviceIntent)
            // Update UI after a short delay to allow service to stop
            protectionToggleButton.postDelayed({
                updateUI()
            }, 300)
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                this,
                "Error stopping protection service: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            android.util.Log.e("MainActivity", "Failed to stop service", e)
        }
    }

    /** Update the UI based on current service state and permissions */
    private fun updateUI() {
        // Update permission statuses
        updatePermissionStatus()

        // Update service status
        updateServiceStatus()

        // Update button states
        updateButtonStates()
        
        // Update statistics summary
        updateStatisticsSummary()
    }
    
    /** Update statistics summary on main screen */
    private fun updateStatisticsSummary() {
        val totalCalls = appPreferences.totalCalls
        statisticsSummary.text = "$totalCalls call${if (totalCalls != 1) "s" else ""}"
    }

    /** Update permission status displays */
    private fun updatePermissionStatus() {
        // DND permission
        if (permissionChecker.hasDndPermission()) {
            dndPermissionStatus.text = "Granted"
            dndPermissionStatus.setTextColor(getColor(R.color.white))
            dndPermissionStatus.background = getDrawable(R.drawable.permission_status_bg)
            dndPermissionStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.success_light))
        } else {
            dndPermissionStatus.text = "Not Granted"
            dndPermissionStatus.setTextColor(getColor(R.color.error))
            dndPermissionStatus.background = getDrawable(R.drawable.permission_status_bg)
            dndPermissionStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.error_light))
        }

        // Accessibility permission
        if (permissionChecker.hasAccessibilityPermission()) {
            accessibilityPermissionStatus.text = "Granted"
            accessibilityPermissionStatus.setTextColor(getColor(R.color.white))
            accessibilityPermissionStatus.background = getDrawable(R.drawable.permission_status_bg)
            accessibilityPermissionStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.success_light))
        } else {
            accessibilityPermissionStatus.text = "Not Granted"
            accessibilityPermissionStatus.setTextColor(getColor(R.color.error))
            accessibilityPermissionStatus.background = getDrawable(R.drawable.permission_status_bg)
            accessibilityPermissionStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.error_light))
        }

        // Microphone permission
        if (permissionChecker.hasMicrophonePermission()) {
            microphonePermissionStatus.text = "Granted"
            microphonePermissionStatus.setTextColor(getColor(R.color.white))
            microphonePermissionStatus.background = getDrawable(R.drawable.permission_status_bg)
            microphonePermissionStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.success_light))
        } else {
            microphonePermissionStatus.text = "Not Granted"
            microphonePermissionStatus.setTextColor(getColor(R.color.error))
            microphonePermissionStatus.background = getDrawable(R.drawable.permission_status_bg)
            microphonePermissionStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.error_light))
        }
    }

    /** Update service status display */
    private fun updateServiceStatus() {
        val isServiceRunning = CallMonitoringService.isRunning

        if (isServiceRunning) {
            statusText.text = getString(R.string.service_running)
            statusText.setTextColor(getColor(R.color.success))
            statusDescription.text = "DND will be automatically enabled during messaging app calls"
            statusIndicator.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.success))
        } else {
            statusText.text = getString(R.string.service_stopped)
            statusText.setTextColor(getColor(R.color.error))
            statusDescription.text =
                    "Start protection to automatically enable DND during messaging app calls"
            statusIndicator.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.error))
        }
    }

    /** Update button states based on permissions and service status */
    private fun updateButtonStates() {
        val hasAllPermissions = permissionChecker.hasAllPermissions()
        val isServiceRunning = CallMonitoringService.isRunning

        // Enable/disable toggle button based on permissions
        protectionToggleButton.isEnabled = hasAllPermissions

        if (isServiceRunning) {
            // Protection is active - show stop state
            protectionToggleButton.text = getString(R.string.stop_protection)
            protectionToggleButton.setTextColor(getColor(R.color.white))
            protectionToggleButton.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.error))
            protectionToggleButton.icon = ContextCompat.getDrawable(this, android.R.drawable.ic_media_pause)
        } else {
            // Protection is inactive - show start state
            protectionToggleButton.text = getString(R.string.start_protection)
            protectionToggleButton.setTextColor(getColor(R.color.white))
            protectionToggleButton.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.success))
            protectionToggleButton.icon = ContextCompat.getDrawable(this, android.R.drawable.ic_media_play)
        }

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
        
        // Notify permission wizard dialog if it's open
        val wizard = supportFragmentManager.findFragmentByTag("PermissionWizard") as? PermissionWizardDialog
        wizard?.onResume() // Trigger update to check if permissions were granted
    }
}
