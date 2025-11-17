package com.example.whatsappcallprotector.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages application preferences using SharedPreferences
 */
class AppPreferences(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        AppConstants.PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    // Detection delay settings
    var detectionDelay: Long
        get() = prefs.getLong(AppConstants.KEY_DETECTION_DELAY, AppConstants.DEFAULT_CALL_DETECTION_DELAY)
        set(value) = prefs.edit().putLong(AppConstants.KEY_DETECTION_DELAY, value).apply()
    
    // Protection enabled/disabled
    var isProtectionEnabled: Boolean
        get() = prefs.getBoolean(AppConstants.KEY_ENABLE_PROTECTION, true)
        set(value) = prefs.edit().putBoolean(AppConstants.KEY_ENABLE_PROTECTION, value).apply()
    
    // Show notifications preference
    var showNotifications: Boolean
        get() = prefs.getBoolean(AppConstants.KEY_SHOW_NOTIFICATIONS, true)
        set(value) = prefs.edit().putBoolean(AppConstants.KEY_SHOW_NOTIFICATIONS, value).apply()
    
    // Call statistics
    var totalCalls: Int
        get() = prefs.getInt(AppConstants.KEY_TOTAL_CALLS, 0)
        set(value) = prefs.edit().putInt(AppConstants.KEY_TOTAL_CALLS, value).apply()
    
    var totalCallDuration: Long
        get() = prefs.getLong(AppConstants.KEY_TOTAL_CALL_DURATION, 0L)
        set(value) = prefs.edit().putLong(AppConstants.KEY_TOTAL_CALL_DURATION, value).apply()
    
    var lastCallTime: Long
        get() = prefs.getLong(AppConstants.KEY_LAST_CALL_TIME, 0L)
        set(value) = prefs.edit().putLong(AppConstants.KEY_LAST_CALL_TIME, value).apply()
    
    /**
     * Increment call count
     */
    fun incrementCallCount() {
        totalCalls = totalCalls + 1
    }
    
    /**
     * Add call duration to total
     */
    fun addCallDuration(durationMs: Long) {
        totalCallDuration = totalCallDuration + durationMs
    }
    
    /**
     * Reset all statistics
     */
    fun resetStatistics() {
        prefs.edit()
            .putInt(AppConstants.KEY_TOTAL_CALLS, 0)
            .putLong(AppConstants.KEY_TOTAL_CALL_DURATION, 0L)
            .putLong(AppConstants.KEY_LAST_CALL_TIME, 0L)
            .apply()
    }
    
    /**
     * Get formatted total call duration
     */
    fun getFormattedTotalDuration(): String {
        val totalSeconds = totalCallDuration / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
}

