## 2024-05-24 - Excessive MQTT Publishes on Media Position Updates
**Learning:** `onPlaybackStateChanged` in `MediaSessionListenerService` fires frequently for position updates (e.g., progress bar advancing), triggering unnecessary JSON generation and MQTT publishes even when state, title, and artist haven't changed.
**Action:** Implement payload caching. Compare newly generated payload with the previously sent one, and skip publishing if they match to save network I/O and TV resources.
