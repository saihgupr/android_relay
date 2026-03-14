package com.saihgupr.hatvrelay

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

        val permissionButton = findViewById<Button>(R.id.permission_button)
        val mqttBrokerEdit = findViewById<android.widget.EditText>(R.id.mqtt_broker_edit)
        val mqttTopicEdit = findViewById<android.widget.EditText>(R.id.mqtt_topic_edit)
        val saveButton = findViewById<Button>(R.id.save_config_button)
        val testButton = findViewById<Button>(R.id.test_mqtt_button)

        // Load existing config
        val prefs = getSharedPreferences("mqtt_config", MODE_PRIVATE)
        mqttBrokerEdit.setText(prefs.getString("broker_ip", "192.168.1.199"))
        mqttTopicEdit.setText(prefs.getString("topic", "android_tv/playback_state"))

        permissionButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        saveButton.setOnClickListener {
            val ip = mqttBrokerEdit.text.toString()
            val topic = mqttTopicEdit.text.toString()
            
            prefs.edit()
                .putString("broker_ip", ip)
                .putString("topic", topic)
                .apply()
            
            // Restart service to pick up changes
            val intent = Intent(this, MediaSessionListenerService::class.java)
            stopService(intent)
            startService(intent)
            
            android.widget.Toast.makeText(this, "Configuration Saved", android.widget.Toast.LENGTH_SHORT).show()
        }

        testButton.setOnClickListener {
            val mqtt = MqttRelay(this)
            mqtt.connect()
            // Wait a bit for connection before publishing
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                mqtt.publish(null, "{\"status\": \"test\", \"message\": \"Hello from Android Relay UI\"}")
                android.widget.Toast.makeText(this, "Test published (check logs)", android.widget.Toast.LENGTH_SHORT).show()
            }, 2000)
        }

        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val statusText = findViewById<TextView>(R.id.status_text)
        val permissionButton = findViewById<Button>(R.id.permission_button)
        val adbInstructions = findViewById<TextView>(R.id.adb_instructions)

        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val isEnabled = enabledListeners?.contains(packageName) == true
        
        if (isEnabled) {
            statusText.text = "Status: Connected & Authorized"
            statusText.setTextColor(android.graphics.Color.GREEN)
            permissionButton.visibility = android.view.View.GONE
            adbInstructions.visibility = android.view.View.GONE
        } else {
            statusText.text = "Status: Permission Required"
            statusText.setTextColor(android.graphics.Color.RED)
            permissionButton.visibility = android.view.View.VISIBLE
            adbInstructions.visibility = android.view.View.VISIBLE
        }
    }
}
