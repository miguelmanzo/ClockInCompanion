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

USB-serial fingerprint path (AS608/FPM11A over CP2102) plus a simulated reader for development without hardware.

### Simulated vs hardware

`USE_SIMULATED_READER` in `app/build.gradle.kts` (BuildConfig):

- `true` (default) — debug simulate / fake enroll buttons
- `false` — opens CP2102 @ 57600, polls `PS_AutoIdentify`, debug enroll slots 1–2

### Wiring (hardware)

- Sensor TX → CP2102 RX
- Sensor RX → CP2102 TX
- Sensor VCC → CP2102 **5V** (FPM11A)
- GND → GND
- Phone OTG → CP2102 USB-A

Grant the USB permission dialog when prompted.

## Project layout

- `domain/` — models and `FingerprintReader` / debug interfaces
- `data/usb/` — `UsbSerialManager`, `As608Protocol`
- `data/reader/` — simulated and serial implementations
- `presentation/clockin/` — Compose UI + ViewModel
- `di/` — Hilt bindings (swaps reader via BuildConfig)
