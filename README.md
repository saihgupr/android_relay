# Android Relay

An ultra-lightweight Android TV application that monitors system-wide media sessions and relays playback state to Home Assistant in real-time via MQTT.

## How it Works
This app uses a background `NotificationListenerService` to observe `MediaSession` events. Unlike polling-based solutions, it only triggers when the system reports a change in playback state (Play, Pause, Stop) or metadata (Title, Artist).

- **Ultra-lightweight**: Minimal CPU and memory footprint.
- **Payload Caching ("Bolt"):** Only sends updates when state or metadata changes to minimize network overhead.
- **Duration Tracking:** Reports media duration in seconds for Home Assistant integration.

## 🚀 Performance & Resource Usage
Android Relay is designed to be extremely lightweight for background operation:

- **Storage Space:** 4.13 MB
- **Memory (RAM):** ~37 MB (PSS)
- **CPU Usage:** ~4.3% during active relay (near 0% when idle)
- **Battery Impact:** Negligible due to payload caching and efficient background service management.

## Installation

### 1. Build and Install
Install the APK via ADB:
```bash
adb connect <TV_IP>:5555
adb install app-debug.apk
```

If you cannot access the settings menu easily on your TV, you can grant the required notification permission via ADB:
```bash
adb shell cmd notification allow_listener com.saihgupr.androidrelay/.MediaSessionListenerService
```

### 2. Configure
Launch the **Android Relay** app on your TV to:
1. **Grant Permission**: Allow the app to listen to media notifications.
2. **Set MQTT**: Enter your MQTT Broker IP and Topic directly in the app.
3. **Test**: Use the "Test Connection" button to verify your setup.

## Home Assistant Configuration

Add these sensors to your `configuration.yaml`:

```yaml
mqtt:
  sensor:
    - name: "Android TV Media State"
      state_topic: "android_tv/playback_state"
      value_template: "{{ value_json.state }}"
      json_attributes_topic: "android_tv/playback_state"
      icon: mdi:television-play

    - name: "Android TV Current App"
      state_topic: "android_tv/playback_state"
      value_template: "{{ value_json.app }}"
      icon: mdi:application

    - name: "Android TV Current Title"
      state_topic: "android_tv/playback_state"
      value_template: "{{ value_json.title }}"
      icon: mdi:music-note

    - name: "Android TV Media Duration"
      state_topic: "android_tv/playback_state"
      value_template: "{{ value_json.duration }}"
      unit_of_measurement: "s"
      icon: mdi:timer-outline
```

## Build Requirements
- Android SDK 34
- Gradle 8.5+
- Java 21

## Persistence
The `NotificationListenerService` is a system-managed component. Once enabled, Android will automatically re-bind to the service on every boot. No manual intervention or "Start at Boot" apps are required.
