package com.saihgupr.hatvrelay

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.util.Log

class MediaSessionListenerService : NotificationListenerService(), MediaSessionManager.OnActiveSessionsChangedListener {

    private lateinit var mediaSessionManager: MediaSessionManager
    private val controllers = mutableMapOf<String, MediaController>()
    private val callbacks = mutableMapOf<String, MediaController.Callback>()
    private lateinit var mqttClient: MqttRelay

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service Created")
        mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        mqttClient = MqttRelay(this)
        mqttClient.connect()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Listener Connected")
        val componentName = ComponentName(this, MediaSessionListenerService::class.java)
        mediaSessionManager.addOnActiveSessionsChangedListener(this, componentName)
        updateActiveSessions(mediaSessionManager.getActiveSessions(componentName))
    }

    override fun onActiveSessionsChanged(activeControllers: List<MediaController>?) {
        updateActiveSessions(activeControllers)
    }

    private fun updateActiveSessions(activeControllers: List<MediaController>?) {
        // Remove old callbacks
        controllers.keys.forEach { key ->
            controllers[key]?.unregisterCallback(callbacks[key]!!)
        }
        controllers.clear()
        callbacks.clear()

        activeControllers?.forEach { controller ->
            val pkg = controller.packageName
            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    reportState(controller)
                }

                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    reportState(controller)
                }
            }
            controller.registerCallback(callback)
            controllers[pkg] = controller
            callbacks[pkg] = callback
            
            // Initial report
            reportState(controller)
        }
    }

    private fun reportState(controller: MediaController) {
        val playbackState = controller.playbackState
        val metadata = controller.metadata
        
        val stateStr = when (playbackState?.state) {
            PlaybackState.STATE_PLAYING -> "playing"
            PlaybackState.STATE_PAUSED -> "paused"
            PlaybackState.STATE_BUFFERING -> "buffering"
            else -> "idle"
        }

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        val app = controller.packageName

        val payload = """{
            "state": "$stateStr",
            "title": "$title",
            "artist": "$artist",
            "app": "$app"
        }""".trimIndent()

        Log.d(TAG, "Reporting: $payload")
        mqttClient.publish("android_tv/playback_state", payload)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSessionManager.removeOnActiveSessionsChangedListener(this)
        mqttClient.disconnect()
    }

    companion object {
        private const val TAG = "HATVRelay"
    }
}
