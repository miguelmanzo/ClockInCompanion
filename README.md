# Clock-In Companion

Android app that reads fingerprint matches from a USB-serial sensor (AS608/FPM11A family), publishes clock-in events over MQTT, and queues events locally when offline.

## Requirements

- Android Studio Ladybug+ / JDK 17
- Android device with USB OTG (for hardware path)
- Optional: Mosquitto on a laptop for MQTT demo

## Build

```bash
./gradlew assembleDebug
./gradlew test
```

## Current milestone

Simulated fingerprint reader behind a `FingerprintReader` interface. Use the debug buttons on a debug build to emit match / no-match events.

Hardware USB serial, MQTT, and offline queue land in later commits.

## Project layout

- `domain/` — models and `FingerprintReader` / debug interfaces
- `data/reader/` — simulated (and later serial) implementations
- `presentation/clockin/` — Compose UI + ViewModel
- `di/` — Hilt bindings
