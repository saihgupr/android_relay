package com.saihgupr.androidrelay

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.util.Log
import org.json.JSONObject

class MediaSessionListenerService : NotificationListenerService(), MediaSessionManager.OnActiveSessionsChangedListener {

    private lateinit var mediaSessionManager: MediaSessionManager
    private val controllers = mutableMapOf<String, MediaController>()
    private val callbacks = mutableMapOf<String, MediaController.Callback>()
    private val lastPublishedPayloads = mutableMapOf<String, String>()
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
        val activePackages = activeControllers?.map { it.packageName }?.toSet() ?: emptySet()

        // Remove old callbacks for apps that are no longer active
        val iterator = controllers.entries.iterator()
        while (iterator.hasNext()) {
            val (pkg, controller) = iterator.next()
            if (pkg !in activePackages) {
                controller.unregisterCallback(callbacks[pkg]!!)
                callbacks.remove(pkg)
                lastPublishedPayloads.remove(pkg)
                iterator.remove()
            }
        }

        activeControllers?.forEach { controller ->
            val pkg = controller.packageName
            if (!controllers.containsKey(pkg)) {
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
        val durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val durationSec = if (durationMs > 0) durationMs / 1000 else 0L
        val app = controller.packageName

        val payload = JSONObject().apply {
            put("state", stateStr)
            put("title", title)
            put("artist", artist)
            put("app", app)
            put("duration", durationSec)
        }.toString()

        // Cache the payload per-app to prevent redundant MQTT publishes on position updates.
        // `onPlaybackStateChanged` fires frequently for progress updates, which
        // causes unnecessary network I/O if the state/title/artist haven't actually changed.
        if (payload == lastPublishedPayloads[app]) {
            return
        }
        lastPublishedPayloads[app] = payload

        Log.d(TAG, "Reporting: $payload")
        mqttClient.publish(null, payload)
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
