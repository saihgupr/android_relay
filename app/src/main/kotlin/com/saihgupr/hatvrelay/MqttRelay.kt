package com.saihgupr.hatvrelay

import android.content.Context
import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttRelay(context: Context) {
    private val serverUri = "tcp://192.168.1.199:1883"
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

    fun publish(topic: String, payload: String) {
        try {
            if (mqttClient?.isConnected == true) {
                val message = MqttMessage(payload.toByteArray())
                message.qos = 1
                mqttClient?.publish(topic, message)
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
