package com.example.whatsappcallprotector

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.lal.voipcallprotector.R
import com.google.android.material.button.MaterialButton

/**
 * About Activity to display app information and author details
 */
class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        
        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        // Set app version
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            val versionCode = packageInfo.longVersionCode
            findViewById<TextView>(R.id.versionText).text = "Version $versionName (Build $versionCode)"
        } catch (e: Exception) {
            findViewById<TextView>(R.id.versionText).text = "Version 1.0"
        }
    }

    private fun setupClickListeners() {
        findViewById<MaterialButton>(R.id.backButton).setOnClickListener {
            finish()
        }
        
        findViewById<MaterialButton>(R.id.privacyPolicyButton).setOnClickListener {
            val privacyPolicyUrl = getString(R.string.privacy_policy_url)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl))
            startActivity(intent)
        }
    }
}

