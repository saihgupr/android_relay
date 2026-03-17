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
    private val lastPublishedStates = mutableMapOf<String, MediaState>()
    private lateinit var mqttClient: MqttRelay

    private data class MediaState(
        val state: String,
        val title: String,
        val artist: String,
        val app: String,
        val duration: Long
    )

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service Created")
        mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        mqttClient = MqttRelay(this)
        mqttClient.connect()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Listener Connected")
        val componentName = ComponentName(this, MediaSessionListenerService::class.java)
        mediaSessionManager.addOnActiveSessionsChangedListener(this, componentName)
        updateActiveSessions(mediaSessionManager.getActiveSessions(componentName))
    }

    override fun onActiveSessionsChanged(activeControllers: List<MediaController>?) {
        Log.d(TAG, "Active sessions changed: ${activeControllers?.size}")
        updateActiveSessions(activeControllers)
    }

    private fun updateActiveSessions(activeControllers: List<MediaController>?) {
        val activePackages = activeControllers?.map { it.packageName }?.toSet() ?: emptySet()
        Log.d(TAG, "Active packages: $activePackages")

        // Remove old callbacks for apps that are no longer active
        val iterator = controllers.entries.iterator()
        while (iterator.hasNext()) {
            val (pkg, controller) = iterator.next()
            if (pkg !in activePackages) {
                controller.unregisterCallback(callbacks[pkg]!!)
                callbacks.remove(pkg)
                lastPublishedStates.remove(pkg)
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

        val currentState = MediaState(stateStr, title, artist, app, durationSec)

        // Cache the parsed state per-app to prevent redundant JSON creation and
        // MQTT publishes on position updates. `onPlaybackStateChanged` fires
        // frequently for progress updates, causing unnecessary object allocation
        // and network I/O if the state hasn't actually changed.
        if (currentState == lastPublishedStates[app]) {
            return
        }
        lastPublishedStates[app] = currentState

        val payload = JSONObject().apply {
            put("state", currentState.state)
            put("title", currentState.title)
            put("artist", currentState.artist)
            put("app", currentState.app)
            put("duration", currentState.duration)
        }.toString()

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
