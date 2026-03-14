# Android TV Media Relay

An ultra-lightweight Android TV application that monitors system-wide media sessions and relays playback state to Home Assistant in real-time via MQTT.

## 🚀 How it Works
This app uses a background `NotificationListenerService` to observe `MediaSession` events. Unlike polling-based solutions, it only triggers when the system reports a change in playback state (Play, Pause, Stop) or metadata (Title, Artist).

- **Ultra-lightweight**: Minimal CPU and memory footprint.
- **TV Optimized**: Supports Leanback launcher and dark theme.
- **Real-time**: Near-zero latency reporting to MQTT.

## 🛠 Prerequisites
- **Android TV device** on the local network.
- **MQTT Broker** (e.g., Mosquitto in Home Assistant).
- **ADB access** to the TV.

## 📦 Installation

### 1. Build and Install
If you have the APK, install it via ADB:
```bash
adb connect <TV_IP>:5555
adb install app-debug.apk
```

### 2. Grant Permissions
Android TV hides the "Notification Access" menu. Grant the required permission via ADB:
```bash
adb shell cmd notification allow_listener com.chrisl.hatvrelay/.MediaSessionListenerService
```

### 3. Launch
Open the "HA TV Relay" app on your TV to ensure the service initials. You can then close it; the service runs in the background.

## 🏠 Home Assistant Configuration

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
```

## 📝 Configuration
The MQTT broker and topic are currently configured in `MqttRelay.kt`:
- **Broker**: `tcp://192.168.1.199:1883`
- **Topic**: `android_tv/playback_state`

## 🏗 Build Requirements
- Android SDK 34
- Gradle 7.6.6
- Java 17
