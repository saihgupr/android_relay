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

    private val clientId = "AndroidTVRelay_${android.os.Build.ID}_${java.util.UUID.randomUUID().toString().take(4)}"
    private var mqttClient: MqttClient? = null

    fun connect() {
        try {
            Thread {
                try {
                    val uri = serverUri
                    val cid = clientId
                    Log.i("AndroidRelay", "Configuring MQTT client for $uri with ID $cid")
                    mqttClient = MqttClient(uri, cid, MemoryPersistence())
                    val options = MqttConnectOptions()
                    options.isAutomaticReconnect = true
                    options.isCleanSession = true
                    options.connectionTimeout = 10
                    
                    mqttClient?.setCallback(object : MqttCallbackExtended {
                        override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                            Log.i("AndroidRelay", "Connect successful to $serverURI (reconnect: $reconnect)")
                        }
                        override fun connectionLost(cause: Throwable?) {
                            Log.w("AndroidRelay", "Connection lost: ${cause?.message}")
                        }
                        override fun messageArrived(topic: String?, message: MqttMessage?) {}
                        override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                    })

                    Log.i("AndroidRelay", "Attempting to connect to $uri...")
                    mqttClient?.connect(options)
                } catch (e: MqttException) {
                    Log.e("AndroidRelay", "MqttException during connect: ${e.message}", e)
                } catch (e: Exception) {
                    Log.e("AndroidRelay", "Unexpected error during connect: ${e.message}", e)
                }
            }.start()
        } catch (e: Exception) {
            Log.e("AndroidRelay", "Error spawning connect thread", e)
        }
    }

    fun publish(topic: String? = null, payload: String) {
        try {
            val client = mqttClient
            if (client != null && client.isConnected) {
                val targetTopic = topic ?: defaultTopic
                Log.d("AndroidRelay", "Publishing to $targetTopic: $payload")
                val message = MqttMessage(payload.toByteArray())
                message.qos = 1
                client.publish(targetTopic, message)
            } else {
                Log.w("AndroidRelay", "Cannot publish: Client not connected")
            }
        } catch (e: Exception) {
            Log.e("AndroidRelay", "Error publishing", e)
        }
    }

    fun disconnect() {
        try {
            mqttClient?.disconnect()
        } catch (e: Exception) {
            Log.e("AndroidRelay", "Error disconnecting", e)
        }
    }
}
