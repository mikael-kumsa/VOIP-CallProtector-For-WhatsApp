package com.example.whatsappcallprotector.util

/**
 * Application-wide constants
 */
object AppConstants {
    
    // Detection delays (in milliseconds)
    const val DEFAULT_CALL_DETECTION_DELAY = 500L // Reduced from 1500ms for faster detection
    const val WINDOW_STATE_CHANGE_DELAY = 800L // Reduced from 1500ms
    const val WINDOW_CONTENT_CHANGE_DELAY = 500L // Reduced from 1000ms
    const val POTENTIAL_CALL_VERIFICATION_DELAY = 500L // Reduced from 1000ms
    
    // State monitoring intervals
    const val STATE_CHECK_INTERVAL = 3000L // Check every 3 seconds
    
    // Notification
    const val NOTIFICATION_CHANNEL_ID = "call_protection_channel"
    const val NOTIFICATION_ID = 1
    
    // SharedPreferences keys
    const val PREFS_NAME = "whatsapp_call_protector_prefs"
    const val KEY_DETECTION_DELAY = "detection_delay"
    const val KEY_ENABLE_PROTECTION = "enable_protection"
    const val KEY_SHOW_NOTIFICATIONS = "show_notifications"
    const val KEY_TOTAL_CALLS = "total_calls"
    const val KEY_TOTAL_CALL_DURATION = "total_call_duration"
    const val KEY_LAST_CALL_TIME = "last_call_time"
    
    // Accessibility service timeout
    const val ACCESSIBILITY_NOTIFICATION_TIMEOUT = 100L
    
    // Max recursion depth for node scanning (performance optimization)
    const val MAX_NODE_SCAN_DEPTH = 10
}

