# Android Room Marker Readiness

Last updated: 2026-05-12

Current app-ready context: this is a historical completed subtask note. The marker-readiness subtask remains done, but TrainIQ release/app-ready status is still `BLOCKED` in `.codex/automation-state/trainiq-app-ready-goal.md` pending owner/manual/safe-device gates.

## Current status

done

## Current blocker

none known

## Implemented

- Added explicit release and profileable Room migration-chain marker generation tasks alongside the existing debug task.
- Added aggregate CI task: `:app:generateCiRoomMigrationChainVerificationMarkers`.
- All marker generation tasks depend on `connectedDebugAndroidTest`, the connected migration/import verification gate available in this project.
- Release/profileable/debug markers are generated into variant-specific assets under `app/build/generated/roomMigrationVerification/<variant>/assets`.
- Release/profileable/debug source sets package only their own variant marker.
- Marker generation accepts `-ProomMigrationVerificationTimestampMillis=<millis>` and tracks it as an input for deterministic CI/testable marker metadata.
- Release APK packaging was verified to contain `assets/room_migration_chain_verification_marker.json`.
- Runtime Room reads remain JSON-authoritative; marker validation only feeds the readiness gate and does not switch source of truth.

## Attempted fixes

| Issue | Attempts | Outcome |
| --- | --- | --- |
| Release marker task missing | Added `generateReleaseRoomMigrationChainVerificationMarker` and variant asset source | PASS |
| CI/profileable marker path missing | Added `generateProfileableRoomMigrationChainVerificationMarker` and `generateCiRoomMigrationChainVerificationMarkers` | PASS |
| Marker timestamp not tracked as task input | Added `inputs.property("roomMigrationVerificationTimestampMillis", ...)` | PASS |
| Throwaway Gemini QA profile | Created secondary emulator user, installed app there, but background-user launch/UI was unreliable; stopped and removed the user | Resolved by using fresh primary install after confirming no existing `com.trainiq` package for user 0 |

## Web research

- Triggered after secondary-user app launch failed twice.
- Sources: official Android adb documentation for `am start --user`; official Android multi-user documentation for user switching behavior.
- Decision: switch foreground user before launch was attempted; when emulator UI remained unreliable, the throwaway user was removed and a fresh primary install was used only after confirming no installed `com.trainiq` package existed for user 0.

## Verification

| Command / check | Result |
| --- | --- |
| `.\gradlew.bat :app:testDebugUnitTest --tests com.trainiq.data.migration.RoomMigrationChainVerificationProviderTest --tests com.trainiq.data.migration.RoomRuntimeReadinessGateTest --console=plain` | PASS |
| `.\gradlew.bat :app:generateReleaseRoomMigrationChainVerificationMarker "-ProomMigrationVerificationTimestampMillis=<now>" --console=plain` | PASS |
| `.\gradlew.bat :app:generateCiRoomMigrationChainVerificationMarkers "-ProomMigrationVerificationTimestampMillis=<now>" --console=plain` | PASS |
| `.\gradlew.bat :app:testDebugUnitTest --console=plain` | PASS |
| `.\gradlew.bat :app:assembleDebug :app:assembleRelease --console=plain` | PASS |
| `.\gradlew.bat :app:lintDebug --console=plain` | PASS |
| `jar tf app-release-unsigned.apk` marker asset check | PASS |
| `.\gradlew.bat :app:installDebug --console=plain` | PASS |
| `adb shell am start -W -n com.trainiq/.MainActivity` | PASS after fresh install; cold launch status ok on restart |
| Gemini fake-key save on fresh install | PASS; UI showed masked configured key |
| Gemini process restart/reopen | PASS; UI still showed masked configured key and update button |
| TrainIQ process logcat filter | PASS; no app crash, Room/migration, marker, Gemini, or security exceptions |

## Remaining risks

none known

## Next safe action

none for this marker-readiness subtask. For the active app-ready goal, follow `.codex/automation-state/trainiq-app-ready-goal.md`: stop until owner decisions, manual accessibility evidence, approved performance evidence, disposable Health Connect profile/device, safe camera setup, or approved Gemini credentials/network use are available.
