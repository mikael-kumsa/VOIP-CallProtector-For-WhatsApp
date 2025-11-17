package com.example.whatsappcallprotector.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.INTERRUPTION_FILTER_ALL
import android.app.NotificationManager.INTERRUPTION_FILTER_NONE
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.lal.voipcallprotector.BuildConfig
import com.lal.voipcallprotector.R
import com.example.whatsappcallprotector.accessibility.WhatsAppAccessibilityService
import com.example.whatsappcallprotector.util.AppConstants
import com.example.whatsappcallprotector.util.AppPreferences
import com.example.whatsappcallprotector.util.PermissionChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Main foreground service that coordinates WhatsApp call detection and DND management
 */
class CallMonitoringService : LifecycleService() {

    companion object {
        private const val TAG = "CallMonitoringService"
        
        // Notification channel and ID (moved to AppConstants)
        
        // Service state tracking
        var isRunning = false
            private set
    
        // Reference to active service instance
        private var activeService: CallMonitoringService? = null
            
        // External event handlers for Accessibility Service
        fun onWhatsAppCallStarted() {
            Log.i(TAG, "WhatsApp call started event received from Accessibility Service")
            // Trigger DND enable on the active service instance
            activeService?.enableDndForCall()
        }
        
        fun onWhatsAppCallEnded() {
            Log.i(TAG, "WhatsApp call ended event received from Accessibility Service")
            // Trigger DND disable on the active service instance
            activeService?.disableDndAfterCall()
        }
        
        fun onAccessibilityServiceConnected() {
            Log.i(TAG, "Accessibility service connected")
        }
        
        fun onAccessibilityServiceDisconnected() {
            Log.i(TAG, "Accessibility service disconnected")
        }
    }

    // Call state tracking
    private var isInWhatsAppCall = false
    private var wasDndEnabledByApp = false
    private var originalInterruptionFilter = INTERRUPTION_FILTER_ALL
    
    // Audio focus tracking
    private var hasVoiceCallAudioFocus = false
    
    // Service lifecycle tracking
    private var isServiceActive = false
    private var monitoringJob: Job? = null
    
    // External event handlers
    private var onCallStarted: (() -> Unit)? = null
    private var onCallEnded: (() -> Unit)? = null

    // Service binding
    private val binder = LocalBinder()
    
    // Coroutine scope for background tasks
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    
    // System services
    private lateinit var audioManager: AudioManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var permissionChecker: PermissionChecker
    private lateinit var appPreferences: AppPreferences
    
    // Call tracking for statistics
    private var callStartTime: Long = 0
    
    // Modern audio focus request (Android O+)
    private var audioFocusRequest: AudioFocusRequest? = null
    
    // Audio focus listener for detecting voice calls
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        Log.d(TAG, "Audio focus changed: $focusChange")
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "Audio focus gained")
                handleAudioFocusGained()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "Audio focus lost")
                handleAudioFocusLost()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(TAG, "Audio focus lost transiently")
                // Common during call setup
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "Audio focus lost transiently - can duck")
                // Not typically used for voice calls
            }
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> {
                Log.d(TAG, "Audio focus gained transiently - likely voice call")
                handleVoiceCallAudioFocus()
            }
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE -> {
                Log.d(TAG, "Audio focus gained transiently exclusive - confirmed voice call")
                handleVoiceCallAudioFocus()
            }
        }
    }

    /**
     * Called when the service is created
     */
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "CallMonitoringService created")
        
        // Initialize system services
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        permissionChecker = PermissionChecker(this)
        appPreferences = AppPreferences(this)
        
        // Create notification channel
        createNotificationChannel()
        
        // Set this as the active service instance
        activeService = this
        
        isRunning = true
        isServiceActive = true
        
        // Start state monitoring
        startStateMonitoring()
    }

    /**
     * Called when the service is started
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle stop action from notification
        if (intent?.action == "STOP_SERVICE") {
            Log.d(TAG, "Stop service requested from notification")
            stopSelf()
            return START_NOT_STICKY
        }
        
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "CallMonitoringService started")
        
        // Start as foreground service with persistent notification
        startForeground(AppConstants.NOTIFICATION_ID, createNotification())
        
        // Start monitoring for calls
        startCallMonitoring()
        
        // Return START_STICKY to keep service running if killed by system
        return START_STICKY
    }

    /**
     * Called when service is bound
     */
    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    /**
     * Called when service is destroyed
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "CallMonitoringService destroyed")
        
        // Stop monitoring first
        isServiceActive = false
        monitoringJob?.cancel()
        monitoringJob = null
        
        // Clean up resources
        stopCallMonitoring()
        restoreDndIfNeeded()
        
        // Clear active service reference
        activeService = null
        
        isRunning = false
        isInWhatsAppCall = false
        hasVoiceCallAudioFocus = false
    }

    /**
     * Start monitoring for WhatsApp calls with modern audio focus API
     */
    private fun startCallMonitoring() {
        Log.i(TAG, "Starting call monitoring")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startCallMonitoringModern()
        } else {
            startCallMonitoringLegacy()
        }
    }

    /**
     * Modern audio focus API for Android O+
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun startCallMonitoringModern() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(audioAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build()
        
        audioFocusRequest = focusRequest
        
        val result = audioManager.requestAudioFocus(focusRequest)
        
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.d(TAG, "Audio focus request granted (modern API)")
        } else {
            Log.w(TAG, "Audio focus request failed: $result (modern API)")
        }
    }

    /**
     * Legacy audio focus API for older Android versions
     */
    @SuppressWarnings("deprecation")
    private fun startCallMonitoringLegacy() {
        val result = audioManager.requestAudioFocus(
            audioFocusChangeListener,
            AudioManager.STREAM_VOICE_CALL,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        )
        
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.d(TAG, "Audio focus request granted (legacy API)")
        } else {
            Log.w(TAG, "Audio focus request failed: $result (legacy API)")
        }
    }

    /**
     * Stop monitoring for WhatsApp calls with modern audio focus API
     */
    private fun stopCallMonitoring() {
        Log.i(TAG, "Stopping call monitoring")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
            stopCallMonitoringModern()
        } else {
            stopCallMonitoringLegacy()
        }
        
        // Ensure DND is restored
        restoreDndIfNeeded()
    }

    /**
     * Modern audio focus abandonment for Android O+
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun stopCallMonitoringModern() {
        audioManager.abandonAudioFocusRequest(audioFocusRequest!!)
        audioFocusRequest = null
        Log.d(TAG, "Audio focus abandoned (modern API)")
    }

    /**
     * Legacy audio focus abandonment for older Android versions
     */
    @SuppressWarnings("deprecation")
    private fun stopCallMonitoringLegacy() {
        audioManager.abandonAudioFocus(audioFocusChangeListener)
        Log.d(TAG, "Audio focus abandoned (legacy API)")
    }

    /**
     * Start periodic state monitoring
     */
    private fun startStateMonitoring() {
        // Cancel any existing monitoring job
        monitoringJob?.cancel()
        
        monitoringJob = serviceScope.launch {
            while (isServiceActive) {
                try {
                checkCallState()
                    delay(AppConstants.STATE_CHECK_INTERVAL) // Use configurable interval
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) {
                        throw e // Re-throw cancellation to properly stop the coroutine
                    }
                    Log.e(TAG, "Error in state monitoring", e)
                    delay(AppConstants.STATE_CHECK_INTERVAL) // Continue monitoring even if there's an error
                }
            }
        }
    }

    /**
     * Check current call state and update DND if needed
     */
    private fun checkCallState() {
        // Verify DND permission is still granted
        if (!permissionChecker.hasDndPermission()) {
            Log.w(TAG, "DND permission revoked - disabling DND if active")
            if (wasDndEnabledByApp) {
                disableDndAfterCall()
            }
            return
        }
        
        val isWhatsAppCallActive = WhatsAppAccessibilityService.isInWhatsAppCall
        val isInCallMode = audioManager.mode == AudioManager.MODE_IN_CALL || 
                           audioManager.mode == AudioManager.MODE_IN_COMMUNICATION
        
        Log.d(TAG, "State check - WhatsAppCall: $isWhatsAppCallActive, AudioMode: ${audioManager.mode}, InCallMode: $isInCallMode")
        
        // Enable DND if WhatsApp call is active (audio mode check is secondary verification)
        // Primary detection is from Accessibility Service, audio mode is just confirmation
        if (isWhatsAppCallActive && !isInWhatsAppCall) {
            Log.i(TAG, "WhatsApp call detected - enabling DND")
            enableDndForCall()
        } 
        // Disable DND if WhatsApp call ended but DND is still active
        else if (!isWhatsAppCallActive && isInWhatsAppCall) {
            Log.i(TAG, "WhatsApp call ended - disabling DND")
            disableDndAfterCall()
        }
    }

    /**
     * Handle voice call audio focus detection
     */
    private fun handleVoiceCallAudioFocus() {
        hasVoiceCallAudioFocus = true
        Log.d(TAG, "Voice call audio focus detected")
        
        // Trigger immediate state check
        checkCallState()
    }

    /**
     * Handle audio focus gained (call ended or other audio)
     */
    private fun handleAudioFocusGained() {
        // This might indicate call ended
        if (hasVoiceCallAudioFocus) {
            Log.d(TAG, "Audio focus gained after voice call - call likely ended")
            hasVoiceCallAudioFocus = false
            checkCallState()
        }
    }

    /**
     * Handle audio focus lost
     */
    private fun handleAudioFocusLost() {
        hasVoiceCallAudioFocus = false
        checkCallState()
    }

    /**
     * Enable Do Not Disturb mode for WhatsApp call
     */
    fun enableDndForCall() {
        if (!permissionChecker.hasDndPermission()) {
            Log.w(TAG, "Cannot enable DND - permission not granted")
            return
        }
        
        try {
            // Save current DND state
            originalInterruptionFilter = notificationManager.currentInterruptionFilter
            Log.d(TAG, "Saved original interruption filter: $originalInterruptionFilter")
            
            // Enable DND
            notificationManager.setInterruptionFilter(INTERRUPTION_FILTER_NONE)
            wasDndEnabledByApp = true
            isInWhatsAppCall = true
            callStartTime = System.currentTimeMillis() // Track call start time for statistics
            
            // Update statistics
            appPreferences.incrementCallCount()
            appPreferences.lastCallTime = callStartTime
            
            Log.i(TAG, "DND enabled for WhatsApp call")
            
            // Update notification
            updateNotification()
            
            // Show debug toast
            showDebugToast("DND Enabled for messaging app call")
            
            // Notify listeners
            onCallStarted?.invoke()
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when enabling DND", e)
            showDebugToast("DND Error: Security Exception")
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling DND", e)
            showDebugToast("DND Error: ${e.message}")
        }
    }

    /**
     * Disable Do Not Disturb mode after WhatsApp call ends
     */
    fun disableDndAfterCall() {
        if (!wasDndEnabledByApp) {
            return
        }
        
        try {
            // Calculate call duration and update statistics
            if (callStartTime > 0) {
                val callDuration = System.currentTimeMillis() - callStartTime
                appPreferences.addCallDuration(callDuration)
                Log.d(TAG, "Call duration: ${callDuration}ms")
                callStartTime = 0
            }
            
            // Restore original DND state
            notificationManager.setInterruptionFilter(originalInterruptionFilter)
            wasDndEnabledByApp = false
            isInWhatsAppCall = false
            
            Log.i(TAG, "DND disabled after WhatsApp call")
            Log.d(TAG, "Restored interruption filter: $originalInterruptionFilter")
            
            // Update notification
            updateNotification()
            
            // Show debug toast
            showDebugToast("DND Disabled - Call Ended")
            
            // Notify listeners
            onCallEnded?.invoke()
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when disabling DND", e)
            showDebugToast("DND Error: Security Exception")
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling DND", e)
            showDebugToast("DND Error: ${e.message}")
        }
    }

    /**
     * Restore DND state if it was modified by the app
     */
    private fun restoreDndIfNeeded() {
        if (wasDndEnabledByApp) {
            Log.i(TAG, "Restoring DND state on service stop")
            disableDndAfterCall()
        }
    }

    /**
     * Show debug toast message (only in debug builds)
     */
    private fun showDebugToast(message: String) {
        if (BuildConfig.DEBUG) {
        serviceScope.launch(Dispatchers.Main) {
            android.widget.Toast.makeText(this@CallMonitoringService, message, android.widget.Toast.LENGTH_SHORT).show()
        }
        }
        // Always log for debugging purposes
        Log.d(TAG, "Debug Toast: $message")
    }

    /**
     * Create notification channel for Android O+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AppConstants.NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Create foreground service notification with actions
     */
    private fun createNotification(): Notification {
        val notificationText = if (isInWhatsAppCall) {
            "Monitoring messaging app call - DND active"
        } else {
            "Monitoring for messaging app calls"
        }
        
        // Create intent for opening app
        val openAppIntent = Intent(this, com.example.whatsappcallprotector.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = android.app.PendingIntent.getActivity(
            this, 0, openAppIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create intent for stopping service
        val stopIntent = Intent(this, CallMonitoringService::class.java).apply {
            action = "STOP_SERVICE"
        }
        val stopPendingIntent = android.app.PendingIntent.getService(
            this, 1, stopIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, AppConstants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(notificationText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Protection",
                stopPendingIntent
            )
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
            .build()
    }
    

    /**
     * Update the foreground notification
     */
    private fun updateNotification() {
        notificationManager.notify(AppConstants.NOTIFICATION_ID, createNotification())
    }

    /**
     * Local binder for service communication
     */
    inner class LocalBinder : Binder() {
        fun getService(): CallMonitoringService = this@CallMonitoringService
    }

    /**
     * Set callback for when call starts
     */
    fun setOnCallStartedListener(listener: () -> Unit) {
        onCallStarted = listener
    }

    /**
     * Set callback for when call ends
     */
    fun setOnCallEndedListener(listener: () -> Unit) {
        onCallEnded = listener
    }

    /**
     * Get current service status for UI updates
     */
    fun getServiceStatus(): String {
        return when {
            !isRunning -> "Service not running"
            isInWhatsAppCall -> "Active call - DND enabled"
            else -> "Monitoring for calls"
        }
    }
}