package com.example.whatsappcallprotector

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.lal.voipcallprotector.R
import com.example.whatsappcallprotector.util.AppPreferences
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

/**
 * Settings Activity to view statistics and configure preferences
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var appPreferences: AppPreferences
    
    // Statistics views
    private lateinit var totalCallsText: TextView
    private lateinit var totalDurationText: TextView
    private lateinit var lastCallText: TextView
    
    // Control buttons
    private lateinit var resetStatsButton: MaterialButton
    private lateinit var aboutButton: MaterialButton
    private lateinit var backButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        appPreferences = AppPreferences(this)
        
        initializeViews()
        setupClickListeners()
        updateStatistics()
    }

    private fun initializeViews() {
        totalCallsText = findViewById(R.id.totalCallsText)
        totalDurationText = findViewById(R.id.totalDurationText)
        lastCallText = findViewById(R.id.lastCallText)
        resetStatsButton = findViewById(R.id.resetStatsButton)
        aboutButton = findViewById(R.id.aboutButton)
        backButton = findViewById(R.id.backButton)
    }

    private fun setupClickListeners() {
        resetStatsButton.setOnClickListener {
            // Show confirmation dialog
            android.app.AlertDialog.Builder(this)
                .setTitle("Reset Statistics")
                .setMessage("Are you sure you want to reset all call statistics? This cannot be undone.")
                .setPositiveButton("Reset") { _, _ ->
                    appPreferences.resetStatistics()
                    updateStatistics()
                    android.widget.Toast.makeText(
                        this,
                        "Statistics reset",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        
        aboutButton.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }
        
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun updateStatistics() {
        // Update total calls
        val totalCalls = appPreferences.totalCalls
        totalCallsText.text = totalCalls.toString()
        
        // Update total duration
        val totalDuration = appPreferences.getFormattedTotalDuration()
        totalDurationText.text = totalDuration
        
        // Update last call time
        val lastCallTime = appPreferences.lastCallTime
        if (lastCallTime > 0) {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
            lastCallText.text = dateFormat.format(Date(lastCallTime))
        } else {
            lastCallText.text = "No calls yet"
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatistics()
    }
}

