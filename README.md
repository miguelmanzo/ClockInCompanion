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

Fingerprint match (simulated or USB serial) publishes a clock-in JSON event over MQTT (HiveMQ client → Mosquitto).

### MQTT demo

1. On the laptop: `brew install mosquitto && mosquitto`
2. Put phone + laptop on the same hotspot; note the laptop IP
3. In the app debug field, set **MQTT broker host** to that IP
4. Subscribe with MQTT Explorer to `workeasy/demo/clockevents`
5. Simulate match (or scan a finger) → event appears on the broker

Payload shape:

```json
{
  "eventId": "uuid",
  "employeeId": 1,
  "eventType": "CLOCK_IN",
  "deviceId": "demo-device-001",
  "timestamp": 1726400000000
}
```

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

- `domain/` — models, `FingerprintReader`, `ClockEventPublisher`, use cases
- `data/usb/` — `UsbSerialManager`, `As608Protocol`
- `data/reader/` — simulated and serial implementations
- `data/mqtt/` — HiveMQ publisher + broker config
- `presentation/clockin/` — Compose UI + ViewModel
- `di/` — Hilt bindings
