# Project State

Current status: done

Date: 2026-05-08

Evidence:
- `./gradlew.bat :app:testDebugUnitTest --console=plain`: PASS
- `./gradlew.bat :app:lintDebug --console=plain`: PASS
- `./gradlew.bat :app:connectedDebugAndroidTest --console=plain`: PASS, 13 tests on `Medium_Phone(AVD) - 16`
- `./gradlew.bat :app:checkReleaseSigningReadiness :app:assembleRelease :app:bundleRelease --no-build-cache --no-configuration-cache --console=plain`: PASS
- Android QA screenshots and UI dumps are in `.codex/android-qa`.
- Crash log buffers: `.codex/android-qa/manual-crash-logcat.txt` and `.codex/android-qa/manual-crash-logcat-final.txt` are empty.
- Type-safe navigation `ScannerMode` is annotated with `@Keep`; the `MissingKeepAnnotation` lint finding is no longer present.
- `TrainIqDataCoordinator` delegates active workout session mutations, progression/readiness policy, and exercise library seeding to focused behavior-tested collaborators.

Web research:
- AndroidX Test release notes were used for `NoSuchMethodException: android.hardware.input.InputManager.getInstance []`; Espresso was updated from `3.6.1` to `3.7.0`.

Remaining risks: none known.
