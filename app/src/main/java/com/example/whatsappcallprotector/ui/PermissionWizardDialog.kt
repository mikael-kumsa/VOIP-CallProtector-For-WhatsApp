package com.example.whatsappcallprotector.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.lal.voipcallprotector.R
import com.example.whatsappcallprotector.util.PermissionChecker

/**
 * Permission Wizard Dialog that guides users through granting all required permissions
 * in a simple, step-by-step manner
 */
class PermissionWizardDialog : DialogFragment() {

    private lateinit var permissionChecker: PermissionChecker
    private var onAllPermissionsGranted: (() -> Unit)? = null
    
    private lateinit var stepIndicator: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var permissionTitle: TextView
    private lateinit var permissionDescription: TextView
    private lateinit var permissionIcon: ImageView
    private lateinit var actionButton: Button
    private lateinit var skipButton: Button
    
    private var currentStep = 0
    private val steps = listOf(
        PermissionStep.DND,
        PermissionStep.ACCESSIBILITY,
        PermissionStep.RUNTIME_PERMISSIONS
    )

    companion object {
        fun newInstance(onAllPermissionsGranted: (() -> Unit)? = null): PermissionWizardDialog {
            return PermissionWizardDialog().apply {
                this.onAllPermissionsGranted = onAllPermissionsGranted
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        permissionChecker = PermissionChecker(context)
        
        val builder = AlertDialog.Builder(context)
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_permission_wizard, null)
        
        initializeViews(view)
        updateStep()
        
        builder.setView(view)
        builder.setCancelable(false)
        
        return builder.create()
    }

    private fun initializeViews(view: View) {
        stepIndicator = view.findViewById(R.id.stepIndicator)
        progressBar = view.findViewById(R.id.progressBar)
        permissionTitle = view.findViewById(R.id.permissionTitle)
        permissionDescription = view.findViewById(R.id.permissionDescription)
        permissionIcon = view.findViewById(R.id.permissionIcon)
        actionButton = view.findViewById(R.id.actionButton)
        skipButton = view.findViewById(R.id.skipButton)
        
        actionButton.setOnClickListener { handleActionClick() }
        skipButton.setOnClickListener { 
            // Skip to next step
            currentStep++
            if (currentStep < steps.size) {
                updateStep()
            } else {
                checkAllPermissions()
            }
        }
    }

    private fun updateStep() {
        if (currentStep >= steps.size) {
            checkAllPermissions()
            return
        }
        
        val step = steps[currentStep]
        val progress = ((currentStep + 1) * 100) / steps.size
        
        // Update progress
        progressBar.progress = progress
        stepIndicator.text = "Step ${currentStep + 1} of ${steps.size}"
        
        // Check if current step is already granted
        val isGranted = when (step) {
            PermissionStep.DND -> permissionChecker.hasDndPermission()
            PermissionStep.ACCESSIBILITY -> permissionChecker.hasAccessibilityPermission()
            PermissionStep.RUNTIME_PERMISSIONS -> 
                permissionChecker.hasMicrophonePermission() && 
                permissionChecker.hasPhoneStatePermission()
        }
        
        if (isGranted) {
            // Skip to next step
            currentStep++
            if (currentStep < steps.size) {
                updateStep()
            } else {
                checkAllPermissions()
            }
            return
        }
        
        // Update UI for current step
        when (step) {
            PermissionStep.DND -> {
                permissionTitle.text = "Do Not Disturb Access"
                permissionDescription.text = "Allow the app to automatically enable Do Not Disturb mode during messaging app calls to prevent interruptions."
                permissionIcon.setImageResource(android.R.drawable.ic_lock_silent_mode)
                actionButton.text = "Open Settings"
                skipButton.visibility = View.VISIBLE
            }
            PermissionStep.ACCESSIBILITY -> {
                permissionTitle.text = "Accessibility Service"
                permissionDescription.text = "Enable the accessibility service to detect when messaging apps are in a call. This only monitors messaging apps and doesn't affect other apps."
                permissionIcon.setImageResource(android.R.drawable.ic_menu_manage)
                actionButton.text = "Open Settings"
                skipButton.visibility = View.VISIBLE
            }
            PermissionStep.RUNTIME_PERMISSIONS -> {
                permissionTitle.text = "Microphone & Phone Access"
                permissionDescription.text = "Allow microphone and phone state access to help distinguish between calls and voice messages."
                permissionIcon.setImageResource(android.R.drawable.ic_menu_camera)
                actionButton.text = "Grant Permissions"
                skipButton.visibility = View.GONE // Can't skip runtime permissions
            }
        }
    }

    private fun handleActionClick() {
        val step = steps[currentStep]
        
        when (step) {
            PermissionStep.DND -> {
                permissionChecker.openDndSettings()
                // Show instruction to come back
                showReturnInstruction()
            }
            PermissionStep.ACCESSIBILITY -> {
                permissionChecker.openAccessibilitySettings()
                // Show instruction to come back
                showReturnInstruction()
            }
            PermissionStep.RUNTIME_PERMISSIONS -> {
                // Request runtime permissions
                if (requireActivity() is com.example.whatsappcallprotector.MainActivity) {
                    permissionChecker.requestMicrophonePermission(requireActivity() as com.example.whatsappcallprotector.MainActivity)
                }
                // Wait for result in onResume
                actionButton.text = "Waiting for Permission..."
                actionButton.isEnabled = false
            }
        }
    }

    private fun showReturnInstruction() {
        actionButton.text = "I've Granted It"
        actionButton.setOnClickListener {
            // Check if granted
            val isGranted = when (steps[currentStep]) {
                PermissionStep.DND -> permissionChecker.hasDndPermission()
                PermissionStep.ACCESSIBILITY -> permissionChecker.hasAccessibilityPermission()
                PermissionStep.RUNTIME_PERMISSIONS -> false
            }
            
            if (isGranted) {
                currentStep++
                updateStep()
                actionButton.setOnClickListener { handleActionClick() }
            } else {
                // Show message to try again
                android.widget.Toast.makeText(
                    requireContext(),
                    "Please grant the permission and then tap 'I've Granted It'",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check if permissions were granted while dialog was open
        if (currentStep < steps.size) {
            val step = steps[currentStep]
            val isGranted = when (step) {
                PermissionStep.DND -> permissionChecker.hasDndPermission()
                PermissionStep.ACCESSIBILITY -> permissionChecker.hasAccessibilityPermission()
                PermissionStep.RUNTIME_PERMISSIONS -> 
                    permissionChecker.hasMicrophonePermission() && 
                    permissionChecker.hasPhoneStatePermission()
            }
            
            if (isGranted) {
                // Re-enable button
                actionButton.isEnabled = true
                actionButton.setOnClickListener { handleActionClick() }
                currentStep++
                updateStep()
            } else if (step == PermissionStep.RUNTIME_PERMISSIONS) {
                // Re-enable button if permission was denied
                actionButton.isEnabled = true
                actionButton.text = "Grant Permissions"
            }
        } else {
            checkAllPermissions()
        }
    }

    private fun checkAllPermissions() {
        if (permissionChecker.hasAllPermissions()) {
            // All permissions granted!
            progressBar.progress = 100
            stepIndicator.text = "All Set!"
            permissionTitle.text = "All Permissions Granted"
            permissionDescription.text = "You're all set! You can now start protecting your messaging app calls."
            permissionIcon.setImageResource(android.R.drawable.ic_menu_recent_history)
            actionButton.text = "Done"
            skipButton.visibility = View.GONE
            actionButton.setOnClickListener {
                dismiss()
                onAllPermissionsGranted?.invoke()
            }
        } else {
            // Still missing some permissions
            currentStep = 0
            updateStep()
        }
    }

    private enum class PermissionStep {
        DND,
        ACCESSIBILITY,
        RUNTIME_PERMISSIONS
    }
}

