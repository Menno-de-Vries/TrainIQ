# TrainIQ Android

TrainIQ is a native Android app built with Kotlin, Compose Material 3, Hilt, Room, Health Connect, CameraX, and Gemini 2.5 Flash.

## Requirements

- JDK 17 or newer. The Gradle wrapper runs with the configured Java toolchain.
- Android Studio or Android SDK command-line tools.
- Android SDK platform matching `compileSdk` in `app/build.gradle.kts`.
- A local emulator or device for connected tests and manual QA.

## Build and Test

Run from this directory:

```powershell
.\gradlew.bat :app:assembleDebug --console=plain
.\gradlew.bat :app:testDebugUnitTest --console=plain
.\gradlew.bat :app:lintDebug --console=plain
```

Connected validation requires an emulator or device:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
```

Performance evidence is captured through the `macrobenchmark` module:

```powershell
.\gradlew.bat :macrobenchmark:assembleProfileable --console=plain
.\gradlew.bat :macrobenchmark:connectedProfileableAndroidTest --console=plain
```

Run Macrobenchmark on a physical device for valid numbers. The benchmark task intentionally fails on emulators unless the AndroidX benchmark emulator suppression flag is supplied, because emulator timings are not representative.

If `adb` is not on `PATH`, use the SDK copy, for example:

```powershell
C:\Users\<user>\AppData\Local\Android\Sdk\platform-tools\adb.exe devices -l
```

## Health Connect QA

Health Connect is optional at runtime, but blueprint-critical flows should be tested with:

- no Health Connect provider installed;
- provider installed but no permissions granted;
- partial permissions;
- permissions revoked while the app is open;
- large datasets that require paged reads.

On an emulator, install or update the Health Connect provider package when available, then open Settings in TrainIQ and use the Health Connect rationale flow before launching the system permission prompt.

TrainIQ uses the Health Connect Play Store onboarding overlay for provider-missing devices and stores change tokens per metric/record type so one revoked or failing metric does not invalidate all cached health data.

## Gemini Setup

Gemini is disabled until the user explicitly enables AI and stores an API key in Settings. Keys are stored through Android Keystore after migration from any legacy plaintext preference.

Runtime AI calls use:

- `gemini-2.5-flash`;
- `responseMimeType = "application/json"`;
- `responseJsonSchema` for production output contracts;
- `thinkingBudget = 0` for fast scan/classification flows;
- `thinkingBudget = 1000` for coach, debrief, report, and routine-generation flows.

Do not commit API keys or place production secrets in `BuildConfig`.

## Release Signing

Release signing is configured without committed secrets. Provide either environment variables or Gradle properties:

- `TRAINIQ_KEYSTORE_FILE` or `trainiq.keystoreFile`
- `TRAINIQ_KEYSTORE_PASSWORD` or `trainiq.keystorePassword`
- `TRAINIQ_KEY_ALIAS` or `trainiq.keyAlias`
- `TRAINIQ_KEY_PASSWORD` or `trainiq.keyPassword`

For local builds, place Gradle properties in `~/.gradle/gradle.properties` and keep the keystore outside the repository.

GitHub Actions expects these repository secrets for signed release artifacts:

- `TRAINIQ_KEYSTORE_BASE64`
- `TRAINIQ_KEYSTORE_PASSWORD`
- `TRAINIQ_KEY_ALIAS`
- `TRAINIQ_KEY_PASSWORD`

Verify signing inputs:

```powershell
.\gradlew.bat :app:checkReleaseSigningReadiness --console=plain
```

If no signing inputs are present, release builds remain unsigned instead of using hardcoded credentials.

## Current Runtime Notes

- The app builds and runs with Compose, Hilt, Navigation, CameraX, Health Connect guards, and Gemini service fallbacks.
- Room is the runtime source of truth through DAO `Flow`s exposed by `RoomTrainIqRuntimeStore`.
- Runtime writes are serialized and committed through Room transactions. Legacy JSON is used only as an import/export/backup compatibility bridge.
- Existing legacy JSON data is seeded into Room once when the Room mirror is empty.
- Import planning is idempotent, and failed imports roll back without trusting malformed legacy data.
- KSP incremental processing is disabled in `gradle.properties` to avoid release-cache corruption observed on Windows release builds.

## Navigation and QA

- Bottom navigation uses one root `NavController`.
- Top-level tabs use `launchSingleTop`, `restoreState`, and `findStartDestination()` to avoid duplicate entries and tab-switch crashes.
- Compact phones use an adaptive destination policy so bottom navigation remains readable.

Before release, validate fresh install, upgrade install, compact phones, tablet/foldable width classes, dark mode, font scale 1.3/1.5, Health Connect states, offline/slow network, camera denied, Gemini invalid JSON, app switch/return, lock/unlock, and rapid taps on primary actions.
