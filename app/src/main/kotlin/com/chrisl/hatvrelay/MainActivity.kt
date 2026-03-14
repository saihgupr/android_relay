package com.chrisl.hatvrelay

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val statusText = findViewById<TextView>(R.id.status_text)
        val permissionButton = findViewById<Button>(R.id.permission_button)

        permissionButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        checkPermission(statusText)
    }

    override fun onResume() {
        super.onResume()
        checkPermission(findViewById(R.id.status_text))
    }

    private fun checkPermission(textView: TextView) {
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val isEnabled = enabledListeners?.contains(packageName) == true
        
        if (isEnabled) {
            textView.text = "Status: Connected & Authorized"
            textView.setTextColor(android.graphics.Color.GREEN)
        } else {
            textView.text = "Status: Permission Required"
            textView.setTextColor(android.graphics.Color.RED)
        }
    }
}
