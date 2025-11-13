package com.example.whatsappcallprotector.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.whatsappcallprotector.service.CallMonitoringService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Accessibility Service specifically designed to detect WhatsApp call screens */
class WhatsAppAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "WhatsAppAccessibility"

        // WhatsApp package name
        const val WHATSAPP_PACKAGE = "com.whatsapp"

        // Track current call state
        var isInWhatsAppCall = false
            private set
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    /** Called when the service is started */
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "WhatsApp Accessibility Service connected")

        // Configure the service
        configureAccessibilityService()

        // Notify CallMonitoringService that we're ready
        CallMonitoringService.onAccessibilityServiceConnected()
    }

    /** Configure the accessibility service settings */
    private fun configureAccessibilityService() {
        val info =
                AccessibilityServiceInfo().apply {
                    // Set the package names we want to monitor (only WhatsApp)
                    packageNames = arrayOf(WHATSAPP_PACKAGE)

                    // Set the event types we want to listen for
                    eventTypes =
                            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED

                    // Set feedback type
                    feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN

                    // Set flags
                    flags =
                            AccessibilityServiceInfo.DEFAULT or
                                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS

                    // Set timeout
                    notificationTimeout = 100
                }

        this.serviceInfo = info
    }

    /** Called when accessibility events occur */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            // Only process WhatsApp events
            if (event.packageName != WHATSAPP_PACKAGE) return

            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    handleWindowStateChanged(event)
                }
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                    handleWindowContentChanged(event)
                }
            }
        }
    }

    /** Handle window state changes (screen changes) */
    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        Log.d(TAG, "Window state changed: ${event.className}")

        // Check if this is a WhatsApp call screen with delay to avoid false positives
        serviceScope.launch {
            delay(1500) // Wait 1.5 seconds to confirm it's not a transient state

            val rootNode = rootInActiveWindow ?: return@launch
            val isCallScreen = detectCallScreen(rootNode)

            if (isCallScreen && !isInWhatsAppCall) {
                Log.i(TAG, "WhatsApp call screen detected after delay - starting call monitoring")
                isInWhatsAppCall = true
                CallMonitoringService.onWhatsAppCallStarted()
            } else if (!isCallScreen && isInWhatsAppCall) {
                Log.i(TAG, "WhatsApp call screen ended - stopping call monitoring")
                isInWhatsAppCall = false
                CallMonitoringService.onWhatsAppCallEnded()
            }
        }
    }

    /** Handle window content changes (UI updates) with delay to avoid false positives */
    private fun handleWindowContentChanged(event: AccessibilityEvent) {
        Log.d(
                TAG,
                "Window content changed - Source: ${event.source?.className}, Text: ${event.text}"
        )

        // First, check the event source for quick call indicators
        event.source?.let { source ->
            if (isCallIndicatorNode(source)) {
                Log.d(TAG, "Found call indicator in event source")
                handlePotentialCallStart()
                return
            }
        }

        // Also check the event text for call-related content
        event.text.let { text ->
            if (text.any { textItem ->
                        textItem.toString().contains("call", ignoreCase = true) &&
                                (textItem.toString().contains("ongoing", ignoreCase = true) ||
                                        textItem.toString().contains("in call", ignoreCase = true))
                    }
            ) {
                Log.d(TAG, "Found call indicator in event text: ${event.text}")
                handlePotentialCallStart()
                return
            }
        }

        // If no direct indicators in event, scan the full window with delay
        serviceScope.launch {
            delay(1000) // Wait 1 second to see if it's a real call

            val rootNode = rootInActiveWindow ?: return@launch

            // 🔹 Simplified and reliable detection
            if (scanForCallIndicators(rootNode)) {
                if (!isInWhatsAppCall) {
                    isInWhatsAppCall = true
                    Log.i(TAG, "Detected WhatsApp call started")
                    CallMonitoringService.onWhatsAppCallStarted()
                }
            } else if (isInWhatsAppCall) {
                isInWhatsAppCall = false
                Log.i(TAG, "Detected WhatsApp call ended")
                CallMonitoringService.onWhatsAppCallEnded()
            }
        }
    }

    /** Handle potential call start with verification */
    private fun handlePotentialCallStart() {
        serviceScope.launch {
            delay(1000) // Wait 1 second to confirm

            val rootNode = rootInActiveWindow ?: return@launch
            val callIndicatorsFound = scanForCallIndicators(rootNode)

            if (callIndicatorsFound && !isInWhatsAppCall) {
                Log.i(TAG, "Call confirmed from event source - call started")
                isInWhatsAppCall = true
                CallMonitoringService.onWhatsAppCallStarted()
            }
        }
    }

    /** Detect if the current screen is a WhatsApp call screen */
    private fun detectCallScreen(node: AccessibilityNodeInfo): Boolean {
        return scanForCallIndicators(node)
    }

    /** Scan the node hierarchy for call indicators with better precision */
    private fun scanForCallIndicators(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""

        // Only these words mean an *active* call
        val activeCallKeywords =
                listOf(
                        "end call",
                        "hang up",
                        "mute",
                        "speaker",
                        "in call",
                        "calling…",
                        "ongoing call"
                )

        // These are harmless UI elements (should NOT trigger)
        val passiveWords = listOf("voice call", "video call", "call", "call again")

        if (activeCallKeywords.any { text.contains(it) || desc.contains(it) }) {
            Log.d(TAG, "Active call indicator found: $text / $desc")
            return true
        }

        // Skip nodes that just have passive keywords
        if (passiveWords.any { text == it || desc == it }) {
            return false
        }

        // Recursively check children
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child -> if (scanForCallIndicators(child)) return true }
        }

        return false
    }

    /** Check if a node indicates a WhatsApp call with better precision */
    private fun isCallIndicatorNode(node: AccessibilityNodeInfo): Boolean {
        // Check view ID for specific call indicators (not just general WhatsApp UI)
        node.viewIdResourceName?.let { viewId ->
            val callIndicators =
                    listOf(
                            "voip__video_ongoing_background",
                            "call_button",
                            "endCall",
                            "call_duration",
                            "video_call",
                            "voice_call",
                            "call_toolbar",
                            "call_header"
                    )

            if (callIndicators.any { indicator -> viewId.contains(indicator, ignoreCase = true) }) {
                Log.d(TAG, "Found call indicator in view ID: $viewId")
                return true
            }
        }

        // Check for specific call-related text (not just "calling")
        node.text?.let { text ->
            val textStr = text.toString().lowercase()
            val callTextIndicators =
                    listOf("in call", "call ended", "end call", "call duration", "ongoing call")

            if (callTextIndicators.any { indicator ->
                        textStr.contains(indicator, ignoreCase = true)
                    }
            ) {
                Log.d(TAG, "Found call indicator in text: $textStr")
                return true
            }

            // Check for participant count in group calls
            if (textStr.contains("participant") && textStr.contains("call", ignoreCase = true)) {
                return true
            }
        }

        // Check content description for call indicators
        node.contentDescription?.let { description ->
            val descStr = description.toString().lowercase()
            if (descStr.contains("call") &&
                            (descStr.contains("ongoing") ||
                                    descStr.contains("in call") ||
                                    descStr.contains("end"))
            ) {
                Log.d(TAG, "Found call indicator in content description: $descStr")
                return true
            }
        }

        return false
    }

    /** Called when the system wants to interrupt the feedback this service is providing */
    override fun onInterrupt() {
        Log.d(TAG, "WhatsApp Accessibility Service interrupted")
    }

    /** Called when the system is about to shut down the service */
    override fun onUnbind(intent: android.content.Intent?): Boolean {
        Log.d(TAG, "WhatsApp Accessibility Service unbound")
        isInWhatsAppCall = false
        CallMonitoringService.onAccessibilityServiceDisconnected()
        return super.onUnbind(intent)
    }

    /** Get current call state for external queries */
    fun getCurrentCallState(): Boolean {
        return isInWhatsAppCall
    }
}
