package com.example.whatsappcallprotector.util

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat

/**
 * Utility class to check and manage all required permissions for the app
 * Handles DND access, Accessibility service, and Microphone permissions
 */
class PermissionChecker(private val context: Context) {

    companion object {
        // Request code for microphone permission
        const val REQUEST_MICROPHONE_PERMISSION = 1001

        // WhatsApp package name for accessibility service targeting
        const val WHATSAPP_PACKAGE_NAME = "com.whatsapp"
    }

    // Track individual permission states for better management
    private var microphonePermissionGranted = false
    private var phoneStatePermissionGranted = false

    /**
     * Check if the app has permission to control Do Not Disturb mode
     * @return Boolean true if DND permission is granted
     */
    fun hasDndPermission(): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.isNotificationPolicyAccessGranted
    }

    /**
     * Check if the accessibility service is enabled for this app
     * @return Boolean true if accessibility service is enabled
     */
    fun hasAccessibilityPermission(): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        
        val packageName = context.packageName
        return enabledServices.any { service ->
            service.resolveInfo.serviceInfo.packageName == packageName
        }
    }

    /**
     * Check if the app has microphone permission
     * @return Boolean true if microphone permission is granted
     */
    fun hasMicrophonePermission(): Boolean {
        // Check both tracked state and system state
        return microphonePermissionGranted || 
               ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if the app has phone state permission (for call detection)
     * @return Boolean true if phone state permission is granted
     */
    fun hasPhoneStatePermission(): Boolean {
        // Check both tracked state and system state
        return phoneStatePermissionGranted || 
               ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if all required permissions are granted
     * @return Boolean true if all permissions are granted
     */
    fun hasAllPermissions(): Boolean {
        return hasDndPermission() && 
               hasAccessibilityPermission() && 
               hasMicrophonePermission() && 
               hasPhoneStatePermission()
    }

    /**
     * Get a list of missing permissions
     * @return List of permission names that are missing
     */
    fun getMissingPermissions(): List<String> {
        val missingPermissions = mutableListOf<String>()
        
        if (!hasDndPermission()) {
            missingPermissions.add("Do Not Disturb Access")
        }
        
        if (!hasAccessibilityPermission()) {
            missingPermissions.add("Accessibility Service")
        }
        
        if (!hasMicrophonePermission()) {
            missingPermissions.add("Microphone Access")
        }
        
        if (!hasPhoneStatePermission()) {
            missingPermissions.add("Phone State Access")
        }
        
        return missingPermissions
    }

    /**
     * Request microphone permission from the user
     * This should be called from an Activity
     */
    fun requestMicrophonePermission(activity: android.app.Activity) {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.requestPermissions(permissions, REQUEST_MICROPHONE_PERMISSION)
        }
    }

    /**
     * Handle the result of permission requests
     * @param requestCode The request code passed in requestPermissions
     * @param permissions The requested permissions
     * @param grantResults The grant results for the corresponding permissions
     */
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        if (requestCode == REQUEST_MICROPHONE_PERMISSION) {
            // Reset tracked states
            microphonePermissionGranted = false
            phoneStatePermissionGranted = false
            
            // Check each permission individually
            for (i in permissions.indices) {
                when (permissions[i]) {
                    Manifest.permission.RECORD_AUDIO -> {
                        microphonePermissionGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED
                        Log.d("PermissionChecker", "Microphone permission granted: $microphonePermissionGranted")
                    }
                    Manifest.permission.READ_PHONE_STATE -> {
                        phoneStatePermissionGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED
                        Log.d("PermissionChecker", "Phone state permission granted: $phoneStatePermissionGranted")
                    }
                }
            }
            
            // Return true only if BOTH permissions are granted
            val allGranted = microphonePermissionGranted && phoneStatePermissionGranted
            Log.d("PermissionChecker", "All required permissions granted: $allGranted")
            return allGranted
        }
        return false
    }

    /**
     * Open system settings for DND permission
     */
    fun openDndSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    /**
     * Open system settings for Accessibility service
     */
    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    /**
     * Open app info settings where user can grant individual permissions
     */
    fun openAppInfoSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:${context.packageName}")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    /**
     * Get a descriptive message about what each permission is used for
     * @return Map of permission names to their descriptions
     */
    fun getPermissionDescriptions(): Map<String, String> {
        return mapOf(
            "Do Not Disturb Access" to "Allows the app to automatically enable Do Not Disturb mode during WhatsApp calls",
            "Accessibility Service" to "Allows the app to detect when WhatsApp is in a call by monitoring the screen",
            "Microphone Access" to "Allows the app to detect when the microphone is active to distinguish between calls and voice messages",
            "Phone State Access" to "Allows the app to detect incoming calls and phone state changes"
        )
    }

    /**
     * Check if the app can request all permissions automatically
     * Some permissions like DND and Accessibility require manual user action in settings
     */
    fun canRequestAllPermissionsAutomatically(): Boolean {
        // Only microphone and phone state can be requested automatically
        // DND and Accessibility require manual setup in settings
        return hasDndPermission() && hasAccessibilityPermission()
    }

    /**
     * Get detailed status of each permission for debugging
     */
    fun getPermissionStatus(): Map<String, Boolean> {
        return mapOf(
            "DND" to hasDndPermission(),
            "Accessibility" to hasAccessibilityPermission(),
            "Microphone" to hasMicrophonePermission(),
            "PhoneState" to hasPhoneStatePermission()
        )
    }
}