## 2024-05-24 - Excessive MQTT Publishes on Media Position Updates
**Learning:** `onPlaybackStateChanged` in `MediaSessionListenerService` fires frequently for position updates (e.g., progress bar advancing), triggering unnecessary JSON generation and MQTT publishes even when state, title, and artist haven't changed.
**Action:** Implement payload caching. Compare newly generated payload with the previously sent one, and skip publishing if they match to save network I/O and TV resources.

## 2024-05-26 - Unnecessary Garbage Collection from JSON Serialization during High-Frequency Callbacks
**Learning:** `onPlaybackStateChanged` fires continuously for duration/progress updates. Previously, the caching mechanism serialized a new `JSONObject` *before* checking the cache. This led to high GC pressure due to continuous string allocations, even when the payload ultimately wasn't sent.
**Action:** In high-frequency Android callbacks, defer expensive operations (like JSON serialization or string formatting) until *after* state-change checks. Parse basic types into a lightweight data class, compare those, and only serialize if the state genuinely changed.