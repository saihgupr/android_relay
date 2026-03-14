package com.saihgupr.androidrelay

import android.content.Context
import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttRelay(private val context: Context) {
    private val prefs = context.getSharedPreferences("mqtt_config", Context.MODE_PRIVATE)
    
    private val serverUri: String
        get() {
            val host = prefs.getString("broker_ip", "192.168.1.199")
            return "tcp://$host:1883"
        }
        
    private val defaultTopic: String
        get() = prefs.getString("topic", "android_tv/playback_state") ?: "android_tv/playback_state"

    private val clientId = "AndroidTVRelay_${android.os.Build.ID}"
    private var mqttClient: MqttClient? = null

    fun connect() {
        try {
            mqttClient = MqttClient(serverUri, clientId, MemoryPersistence())
            val options = MqttConnectOptions()
            options.isAutomaticReconnect = true
            options.isCleanSession = true
            
            mqttClient?.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    Log.i("MqttRelay", "Connected to $serverURI")
                }
                override fun connectionLost(cause: Throwable?) {}
                override fun messageArrived(topic: String?, message: MqttMessage?) {}
                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })

            mqttClient?.connect(options)
        } catch (e: MqttException) {
            Log.e("MqttRelay", "Error connecting", e)
        }
    }

    fun publish(topic: String? = null, payload: String) {
        try {
            if (mqttClient?.isConnected == true) {
                val targetTopic = topic ?: defaultTopic
                val message = MqttMessage(payload.toByteArray())
                message.qos = 1
                mqttClient?.publish(targetTopic, message)
            }
        } catch (e: Exception) {
            Log.e("MqttRelay", "Error publishing", e)
        }
    }

    fun disconnect() {
        try {
            mqttClient?.disconnect()
        } catch (e: Exception) {
            Log.e("MqttRelay", "Error disconnecting", e)
        }
    }
}
