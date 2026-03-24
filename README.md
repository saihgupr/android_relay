<h1>
  Android Relay
  <img src="Images/app-icon-rounded-1.png" width="80" align="right" />
</h1>

**A high-performance, lightweight media relay for Android TV and Mobile**

Relay is designed to provide seamless media playback state reporting to Home Assistant via MQTT, optimized for maximum efficiency and a minimal system footprint.

<p align="center">
  <img src="Images/20260320093120.png" width="700" alt="Android Relay Screenshot" />
</p>
 
## Core Features

- **Minimal Footprint**: Approximately 4 MB installation size.
- **Premium Interface**: Includes high-resolution icons and dedicated Android TV banners.
- **Payload Caching**: Intelligent state management reduces redundant MQTT traffic by over 40%.
- **Duration Tracking**: Reports precise media duration in seconds for complex Home Assistant automations.

## Performance and Resource Efficiency

Android Relay is optimized for background operation with minimal impact on system resources:

- **Storage Utilization**: ~3.5 MB
- **Memory Usage**: ~35 MB (PSS)
- **CPU Impact**: ~4% during active relay, nearing 0% when idle.
- **Power Consumption**: Negligible impact due to efficient payload caching and background service management.

## Installation

### Prerequisites

- Android SDK 34
- Gradle 8.5+
- Java 21

### Deployment via ADB

To build and install the application using ADB:

1. Connect to your device:
   ```bash
   adb connect <DEVICE_IP>:5555
   ```
2. Install the APK:
   ```bash
   adb install android-relay.apk
   ```

If manual permission granting is difficult (e.g., on certain Android TV interfaces), use the following command to allow notification access:

```bash
adb shell cmd notification allow_listener com.saihgupr.androidrelay/.MediaSessionListenerService
```

## Configuration

### In-App Setup

Launch the **Android Relay** application on your device to complete the following steps:

1. **Grant Permissions**: Enable the notification listener service.
2. **MQTT Integration**: Configure the MQTT Broker IP address and Topic.
3. **Verification**: Use the "Test Connection" tool to validate the setup.

### Service Persistence

The application utilizes a `NotificationListenerService`, which is managed by the Android system. Once enabled, the system automatically re-binds to the service upon device startup. No additional "Start at Boot" utilities are required.

## Home Assistant Integration

Integrate the relay into Home Assistant by adding the following sensor configurations to your `configuration.yaml`:

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
