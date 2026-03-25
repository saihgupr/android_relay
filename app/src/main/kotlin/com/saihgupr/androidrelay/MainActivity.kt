package com.saihgupr.androidrelay

import android.content.ComponentName
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
        val mqttAuthSwitch = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.mqtt_auth_switch)
        val mqttAuthContainer = findViewById<android.widget.LinearLayout>(R.id.mqtt_auth_container)
        val mqttUsernameEdit = findViewById<android.widget.EditText>(R.id.mqtt_username_edit)
        val mqttPasswordEdit = findViewById<android.widget.EditText>(R.id.mqtt_password_edit)
        val saveButton = findViewById<Button>(R.id.save_config_button)
        val testButton = findViewById<Button>(R.id.test_mqtt_button)

        // Load existing config
        val prefs = getSharedPreferences("mqtt_config", MODE_PRIVATE)
        mqttBrokerEdit.setText(prefs.getString("broker_ip", "192.168.1.199"))
        mqttTopicEdit.setText(prefs.getString("topic", "android_tv/playback_state"))
        
        val useAuth = prefs.getBoolean("use_auth", false)
        mqttAuthSwitch.isChecked = useAuth
        mqttAuthContainer.visibility = if (useAuth) android.view.View.VISIBLE else android.view.View.GONE
        mqttUsernameEdit.setText(prefs.getString("username", ""))
        mqttPasswordEdit.setText(prefs.getString("password", ""))

        mqttAuthSwitch.setOnCheckedChangeListener { _, isChecked ->
            mqttAuthContainer.visibility = if (isChecked) android.view.View.VISIBLE else android.view.View.GONE
        }

        permissionButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        saveButton.setOnClickListener {
            val ip = mqttBrokerEdit.text.toString()
            val topic = mqttTopicEdit.text.toString()
            val auth = mqttAuthSwitch.isChecked
            val user = mqttUsernameEdit.text.toString()
            val pass = mqttPasswordEdit.text.toString()
            
            prefs.edit()
                .putString("broker_ip", ip)
                .putString("topic", topic)
                .putBoolean("use_auth", auth)
                .putString("username", user)
                .putString("password", pass)
                .apply()
            
            // Restart service to pick up changes
            val componentName = ComponentName(this, MediaSessionListenerService::class.java)
            android.service.notification.NotificationListenerService.requestRebind(componentName)
            android.widget.Toast.makeText(this, "Relay Service Re-initialized", android.widget.Toast.LENGTH_SHORT).show()
        }

        val testResultText = findViewById<TextView>(R.id.test_result_text)
        var isTesting = false

        testButton.setOnClickListener {
            if (isTesting) return@setOnClickListener
            
            isTesting = true
            val mqtt = MqttRelay(this)
            testResultText.text = "Initializing..."
            testResultText.setTextColor(getColor(R.color.text_secondary))
            
            mqtt.connect(object : MqttRelay.MqttStatusListener {
                override fun onStatusUpdate(message: String, isError: Boolean) {
                    runOnUiThread {
                        testResultText.text = message
                        if (isError) {
                            testResultText.setTextColor(getColor(R.color.status_error))
                            isTesting = false
                        } else if (message.contains("Connected")) {
                            testResultText.setTextColor(getColor(R.color.status_ok))
                            mqtt.publish(null, "{\"status\": \"test\", \"message\": \"Hello from Relay UI\"}")
                            isTesting = false
                        }
                    }
                }
            })
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
            statusText.text = "Connected and Authorized"
            statusText.setTextColor(getColor(R.color.status_ok))
            permissionButton.visibility = android.view.View.GONE
            adbInstructions.visibility = android.view.View.GONE
        } else {
            statusText.text = "Permission Required"
            statusText.setTextColor(getColor(R.color.status_error))
            permissionButton.visibility = android.view.View.VISIBLE
            adbInstructions.visibility = android.view.View.VISIBLE
        }
    }
}
