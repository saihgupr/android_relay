## 2024-05-24 - [Avoid Manual JSON String Interpolation]
**Vulnerability:** A hardcoded, manually formatted JSON payload string (`"{\"status\": \"test\", \"message\": \"Hello from Android Relay UI\"}"`) was used in `MainActivity.kt` when interacting with MQTT.
**Learning:** Manual JSON string interpolation, especially if it were to include user input later, is a severe injection risk. It bypasses proper encoding and sanitization. In Android and Kotlin development, this often happens for quick tests but can easily escalate into a security gap.
**Prevention:** Always use standard libraries like `org.json.JSONObject` or `Gson` for secure JSON serialization instead of manually concatenating JSON strings, even in test functions or UI components.
