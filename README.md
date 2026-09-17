# Clock-In Companion

Android app that reads fingerprint matches from a USB-serial sensor (AS608 / FPM11A family),
publishes clock-in events over MQTT, and queues events locally when offline.

## Stack

- Kotlin, Jetpack Compose, Hilt
- USB serial: [usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android)
- MQTT: HiveMQ MQTT Client
- Offline: Room + WorkManager

## Build

```bash
./gradlew assembleDebug
./gradlew test
```

Open the project in Android Studio and run on a device (USB OTG required for the hardware path).

## Architecture

`FingerprintReader` hides hardware behind an interface. Hilt binds a
`SwitchableFingerprintReader` that delegates to simulated or USB serial.

Matched scans go through `HandleClockInUseCase`: **save to Room → publish MQTT → mark synced**.
If publish fails, WorkManager retries when the network is available.

## Simulated vs hardware

In **Demo controls**, tap **Simulated** / **USB** to switch at runtime.

Initial mode follows `USE_SIMULATED_READER` in `app/build.gradle.kts` (default `true`).

## Hardware wiring (FPM11A + CP2102)

```
Sensor TX  →  CP2102 RX
Sensor RX  →  CP2102 TX
Sensor VCC →  CP2102 5V
Sensor GND →  CP2102 GND
Phone OTG  →  CP2102 USB-A
```

Grant the USB permission dialog when prompted. Ignore any “USB fingerprint” mode on the module; use UART only.

## MQTT demo (Mosquitto)

1. Install Mosquitto on the laptop (`brew install mosquitto` after Homebrew is set up).
2. Run: `mosquitto` (port **1883**).
3. Put phone and laptop on the same Wi‑Fi / phone hotspot.
4. Note the laptop IP (`ipconfig getifaddr en0` or System Settings).
5. In the app, set **MQTT broker host** to that IP.
6. In MQTT Explorer, connect to `localhost:1883` and subscribe to `workeasy/demo/clockevents`.
7. Tap **Simulate Scan (Match)** (or place an enrolled finger).

Payload:

```json
{
  "eventId": "uuid",
  "employeeId": 1,
  "eventType": "CLOCK_IN",
  "deviceId": "demo-device-001",
  "timestamp": 1726400000000
}
```

Backup if LAN is blocked: set the broker host to a reachable cloud broker and subscribe there instead.

## Offline demo

1. Confirm a match publishes while online.
2. Enable airplane mode on the phone.
3. Simulate / scan again → “Offline — queued” and an amber pending banner.
4. Restore network → WorkManager flushes the queue → banner clears → events appear on the broker.

## Enrollment (hardware)

Use **Enroll slot 1** / **Enroll slot 2** in the debug panel (place finger, lift, place again).
`employeeId` in events is the sensor page/slot id.

## Project layout

```
domain/          models, FingerprintReader, ClockEventPublisher, use cases
data/usb/        UsbSerialManager, As608Protocol
data/reader/     simulated + serial implementations
data/mqtt/       HiveMQ publisher + broker config
data/offline/    Room queue + WorkManager flush
presentation/    Compose UI + ViewModel
di/              Hilt modules
```

## Quick cold-start checklist

- [ ] `./gradlew assembleDebug` (or Run in Android Studio)
- [ ] Mosquitto running; MQTT Explorer subscribed
- [ ] Broker host set to laptop IP
- [ ] Happy path: match → JSON on topic
- [ ] Airplane mode → queued → reconnect → flush
