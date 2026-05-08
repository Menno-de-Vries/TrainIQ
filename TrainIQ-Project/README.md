# TrainIQ

## Release Signing

Release signing is configured without committed secrets. Provide either environment variables or Gradle properties:

- `TRAINIQ_KEYSTORE_FILE` or `trainiq.keystoreFile`
- `TRAINIQ_KEYSTORE_PASSWORD` or `trainiq.keystorePassword`
- `TRAINIQ_KEY_ALIAS` or `trainiq.keyAlias`
- `TRAINIQ_KEY_PASSWORD` or `trainiq.keyPassword`

For local builds, place the Gradle properties in `~/.gradle/gradle.properties` and keep the keystore outside the repository.

GitHub Actions expects these repository secrets for signed release artifacts:

- `TRAINIQ_KEYSTORE_BASE64`
- `TRAINIQ_KEYSTORE_PASSWORD`
- `TRAINIQ_KEY_ALIAS`
- `TRAINIQ_KEY_PASSWORD`

Run `./gradlew :app:checkReleaseSigningReadiness` to verify that signing inputs are complete. If no signing inputs are present, release builds remain unsigned instead of using hardcoded credentials.

Current runtime status:

- The app builds and runs with Compose, Hilt, Navigation, CameraX, Health Connect guards, and Gemini service fallbacks.
- Room is the runtime source of truth through DAO `Flow`s exposed by `RoomTrainIqRuntimeStore`.
- Runtime writes are serialized and committed through Room transactions. Legacy JSON is used only as an import/export/backup compatibility bridge.

Room note:

- Existing legacy JSON data is seeded into Room once when the Room mirror is empty.
- Import planning is idempotent, and failed imports roll back without trusting malformed legacy data.
- KSP incremental processing is disabled in `gradle.properties` to avoid release-cache corruption observed on Windows release builds.

Navigation note:

- Bottom navigation uses one root `NavController`.
- Top-level tabs use `launchSingleTop`, `restoreState`, and `findStartDestination()` to avoid duplicate entries and tab-switch crashes.
