# SubRadar

SubRadar is now a native Android app written in Kotlin.

It keeps subscription records, renewal dates, balances, notes, attachments, language preference, theme preference, and reminder settings on the device. There is no server component.

## Features

- Native Kotlin Android app
- Jetpack Compose UI wrapped in Miuix
- Local JSON storage through Android shared preferences
- Monthly, quarterly, yearly, and custom billing cycles
- CNY and USD subscription totals
- Automatic balance deduction and date rollover
- Search, edit, delete, and one-tap renewal
- Optional local renewal notifications
- Light, dark, and system theme modes
- English and Simplified Chinese UI
- Image attachment support

## Build

Requirements:

- JDK 17
- Android SDK
- Gradle 8.11.1 or newer

Build a debug APK:

```bash
gradle assembleDebug
```

The APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Release

The GitHub Actions workflow builds the native Android project directly. Push a tag like `v2.0.0.6` or run the workflow manually.

## Tech

- Kotlin
- Android Gradle Plugin
- Jetpack Compose
- Miuix for Compose
- Android local notifications

## License

MIT
