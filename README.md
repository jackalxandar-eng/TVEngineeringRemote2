# TV Engineering Remote

Android IR Blaster engineering console built with **plain Android Java** to keep GitHub Actions builds simple and reliable.

## Features

- Physical IR emission through Android `ConsumerIrManager`.
- Detects whether the phone exposes a Consumer IR emitter.
- Shows carrier-frequency ranges reported by the device.
- Remote screen with power, source, navigation, audio, channels, media and numeric keys.
- Multiple local TV profiles.
- Import/export profile JSON files.
- Engineering Lab for creating and testing:
  - RAW mark/space microseconds
  - NEC address/command
  - generic 32-bit pulse-distance frames
  - learned Pronto Hex `0000`
- Engineer-mode lock.
- Separate Service / Hidden Commands view.
- Sensitive-command confirmation before transmission.
- Event/IR log.

## Important IR limits

IR is normally one-way. A phone can transmit but generally cannot confirm that the TV received the command. Android `ConsumerIrManager` also does not provide a generic IR-learning receiver API. For learning an original remote, use an external IR receiver and import RAW/Pronto data.

Vendor service-menu codes are intentionally not invented. Add documented codes for the exact model through a profile and mark them as `service` / `dangerous` when appropriate.

## GitHub Actions APK build

Every push to `main` runs `.github/workflows/build-apk.yml`.

When the workflow succeeds, download the artifact named:

`TV-Engineering-Remote-APK`

It contains:

`TV-Engineering-Remote.apk`

## Build stack

- Android Gradle Plugin 8.7.3
- Gradle 8.9
- Java 17
- compileSdk / targetSdk 35
- No AndroidX or third-party runtime dependencies
