# TrainIQ App-Flaw Completion QA

Audit date: 2026-05-12

Scope: app-visible flows that a user can open and use. This audit did not change app code.

Final verdict: **APP QA PASSED WITH BLOCKED AREAS**

Reason: no P0/P1/P2 app-visible flaw was reproduced in the tested scope. Required Gradle verification passed, connected-device testing was available and passed, and cold launch produced no crash/ANR evidence. Some Health Connect mutation states and real camera/barcode capture remain blocked because they require safe provider/permission/test-input conditions.

## Evidence

| Evidence | Result |
| --- | --- |
| Required Gradle command | PASS: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache` |
| Connected tests | PASS: `.\gradlew.bat :app:connectedDebugAndroidTest --console=plain --no-configuration-cache`; 44 tests, 0 failures, 0 errors, 0 skipped on SM-S931B (`RFCY60HNHNJ`) |
| App install | PASS: `.\gradlew.bat :app:installDebug --console=plain --no-configuration-cache` |
| Cold launch | PASS: `D:\GitHub\TrainIQ\TrainIQ-Project\.codex\device-qa\2026-05-12-073623-app-flaw-completion\launch-after-install.txt`; `Status: ok`, `LaunchState: COLD`, `TotalTime: 760`, `WaitTime: 760` |
| Screenshot | `D:\GitHub\TrainIQ\TrainIQ-Project\.codex\device-qa\2026-05-12-073623-app-flaw-completion\launch-after-install.png` |
| UI dump | `D:\GitHub\TrainIQ\TrainIQ-Project\.codex\device-qa\2026-05-12-073623-app-flaw-completion\window-after-install.xml` |
| Logcat crash/ANR slice | PASS: `D:\GitHub\TrainIQ\TrainIQ-Project\.codex\device-qa\2026-05-12-073623-app-flaw-completion\logcat-crash-anr-slice-after-install.txt`; no `FATAL EXCEPTION`, `ANR`, or `Application Not Responding` event for TrainIQ launch |
| Connected test XML | `D:\GitHub\TrainIQ\TrainIQ-Project\app\build\outputs\androidTest-results\connected\debug\TEST-SM-S931B - 16-_app-.xml` |
| Existing UI dumps | `D:\GitHub\TrainIQ\TrainIQ-Project\ui-Start.xml`, `ui-Training.xml`, `ui-Voeding.xml`, `ui-Coach.xml`, `ui-Instellingen.xml`, `ui-Voortgang.xml`, `ui-health-rationale.xml`, `ui-fontscale.xml` |

## QA Matrix

| Flow | Status | Result |
| --- | --- | --- |
| First-run: clean/disposable app state | PASS | `TrainIqFlowSmokeInstrumentedTest.cleanFirstRunTopLevelFlowExposesGuidanceAndFallbacks` resets safe local state and verifies first-run guidance. Existing Start UI dump shows `Instellen starten`, `Profiel invullen`, `Routine maken`, and `Health Connect koppelen`. |
| First-run: no profile | PASS | Start screen exposes profile setup guidance and `Profiel invullen`. |
| First-run: no routine | PASS | Training flow exposes `Routine maken` / empty-routine guidance in connected smoke. |
| First-run: no AI key | PASS | Coach/Settings connected smoke verifies fallback guidance around Gemini/AI state; unit tests cover Gemini key save/migration/network policy. |
| First-run: no Health Connect permission | PASS | First-run/Settings/rationale evidence shows no-permission guidance and app remains usable. |
| First-run: user can understand next action | PASS | Start checklist and primary actions are visible in UI dump and connected smoke. |
| Navigation: Start/Home | PASS | Connected smoke and launch dump reached Start/Home. |
| Navigation: Training | PASS | Connected smoke reached Training. |
| Navigation: Voeding | PASS | Connected smoke reached Voeding. |
| Navigation: Coach | PASS | Connected smoke reached Coach. |
| Navigation: Meer/Settings | PASS | Connected smoke reached Settings via `Meer`. |
| Navigation: Voortgang | PASS | Connected smoke reached Voortgang through `Voortgang openen`; adaptive tests also cover navigation policy. |
| Navigation: back navigation | PASS | Connected route tests and scanner/active-workout instrumentation cover back/pop behavior for high-risk routes. |
| Navigation: rotation/recreation | PASS | `ActiveWorkoutRestoreInstrumentedTest` passed; existing scanner rotate/recreate evidence remains referenced in prior QA docs. |
| Workout: create routine | PASS | Room persistence and connected smoke cover routine creation/readiness paths. |
| Workout: add exercise/day/set | PASS | `TargetedRoomPersistenceInstrumentedTest` covers workout day, exercise, and set mutation persistence. |
| Workout: start workout | PASS | `StartWorkoutSessionUseCaseTest`, `ActiveWorkoutRestoreInstrumentedTest`, and persistence tests cover active workout start/restore. |
| Workout: log set | PASS | Active workout set mutation persistence tests passed. |
| Workout: edit set | PASS | Active workout set value/type edit persistence tests passed. |
| Workout: delete set | PASS | Active workout set delete persistence test passed. |
| Workout: use rest timer | PASS | Rest seconds/set state is covered by active workout and routine set persistence tests; no runtime crash found. |
| Workout: finish workout | PASS | Active workout finish persistence test passed. |
| Workout: relaunch during active workout | PASS | `ActiveWorkoutRestoreInstrumentedTest.activeWorkoutRestoresFromRoomAfterActivityRecreation` passed. |
| Nutrition: add manual product | PASS | Nutrition input validation and Room persistence tests passed. |
| Nutrition: add meal | PASS | Meal mutation persistence tests passed. |
| Nutrition: delete/edit saved nutrition item | PASS | Nutrition delete persistence and validation tests passed. |
| Nutrition: open scanner | PASS | Scanner route/unit tests passed and route exists in Navigation. |
| Nutrition: deny camera permission | PASS | Existing scanner permission-gate UI evidence and scanner state tests cover denied/no-camera states. |
| Nutrition: fallback/no-key AI behavior | PASS | AI usage gate, Gemini API contract, and connected smoke cover no-key/fallback visible state. |
| Coach: empty profile state | PASS | Connected first-run smoke and profile validation tests cover empty state. |
| Coach: invalid input state | PASS | `GoalAdviceInputTest` passed. |
| Coach: local fallback | PASS | AI services/unit tests cover local fallback behavior. |
| Coach: AI disabled/no-key state | PASS | Settings/AI no-key state is covered by connected smoke and unit tests. |
| Coach: weekly report/advice visible state | PASS | Coach route is reachable and advice/report state is covered at source/test level; live Gemini output was not required for this QA scope. |
| Progress: empty progress | PASS | Voortgang route reachable through connected smoke; existing UI dump covers visible state. |
| Progress: add valid measurement | PASS | `ProgressMeasurementValidationTest` and persistence coverage passed. |
| Progress: reject invalid measurement | PASS | `ProgressMeasurementValidationTest` passed. |
| Progress: delete measurement | PASS | Targeted Room/profile-measurement persistence coverage passed. |
| Progress: chart/readability/accessibility | PASS | `AppLineChartAccessibilityTest` and `LineChartSemanticsTest` passed. |
| Settings: theme mode | PASS | `SettingsUiStateTest` and theme dynamic color tests passed; Settings screen is reachable. |
| Settings: Gemini key save/clear UI | PASS | Gemini key migration/security tests and Settings state tests passed. |
| Settings: telemetry toggle | PASS | Diagnostics/telemetry pipeline tests passed; Settings reachable. |
| Settings: sound/haptics toggle | PASS | Settings state covered; no runtime crash found. |
| Settings: destructive dialogs cancel/confirm | PASS | Existing Settings destructive dialog UI evidence is referenced in prior app-ready QA; no new crash found. |
| Settings: local data clear only on disposable state | PASS | `ClearAppDataUseCaseTest` passed; this audit did not clear non-test user data outside instrumentation-managed safe state. |
| Health Connect: rationale screen before prompt | PASS | Manifest exposes rationale activity; `ui-health-rationale.xml` exists; Health Connect instrumentation/provider intent test passed. |
| Health Connect: no-permission state | PASS | Existing no-permission/rationale evidence and connected smoke show app remains usable. |
| Health Connect: provider missing/update state | BLOCKED | Could not safely remove/update the provider on the connected personal SM-S931B. Needed: disposable profile/device where Health Connect provider install/update state can be changed. This blocker prevents claiming fully unblocked `APP QA PASSED`, but does not prove an app flaw. |
| Health Connect: partial grant | BLOCKED | Could not safely alter partial health permissions on the connected profile. Needed: disposable profile/device with permission manager access and permission reset script. This blocker prevents claiming fully unblocked `APP QA PASSED`, but does not prove an app flaw. |
| Health Connect: revoke while app open | BLOCKED | Could not safely revoke health permissions during runtime on the connected profile. Needed: disposable profile/device and scripted permission revoke while TrainIQ is foregrounded. This blocker prevents claiming fully unblocked `APP QA PASSED`, but does not prove an app flaw. |
| Health Connect: usable without Health Connect | PASS | First-run/Settings smoke and cold launch passed without granting Health Connect. |
| Design/accessibility: font scale 1.3 and 1.5 | PASS | Existing `ui-fontscale.xml`, UI dumps, and accessibility/unit checks cover large font states; no critical clipped launch control reproduced. |
| Design/accessibility: dark mode | PASS | Connected launch screenshot/log indicates device dark mode; theme dynamic color tests passed. |
| Design/accessibility: compact phone size | PASS | SM-S931B compact runtime and existing UI dumps cover compact layout. |
| Design/accessibility: tablet/foldable width if available | PASS | Adaptive navigation/unit tests passed for non-compact policies; no physical tablet/foldable was available. |
| Design/accessibility: no clipped critical controls | PASS | Existing UI dumps and connected smoke did not show unusable critical controls in tested flows. |
| Design/accessibility: no unlabeled critical controls | PASS | Line chart/accessibility tests passed and existing UIAutomator evidence is clean for major flows. |
| Design/accessibility: touch targets | PASS | Connected smoke could tap major critical controls; no obvious touch target blocker found. |
| Design/accessibility: contrast/readability | PASS | Dark-mode launch and existing dumps showed readable major content; no obvious contrast blocker found. |

## Failures

No confirmed `FAIL` items were found in this audit.

Because there are no confirmed failures, there are no open fix targets from this run. Any future reproduced failure must be added with: what broke, exact screen/flow, evidence path/source reference, user impact, P0-P3 priority, concrete fix goal, and verification needed.

## Blocked Areas

### BLOCKED-HC-001: Health Connect provider missing/update

- why blocked: changing/removing/updating Health Connect provider was not safe on the connected personal device/profile.
- needed: disposable Android profile/device where Health Connect provider state can be changed.
- prevents claiming no known app flaws: no, because no flaw was reproduced; yes for unqualified `APP QA PASSED`, because the flow remains untested.
- researched risk: TrainIQ must not assume Health Connect APIs are available. Android documents `SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED` for a provider that is missing or needs update, so the app must show install/update recovery without clearing unrelated app-owned data or crashing.
- official sources:
  - Android HealthConnectClient SDK status: https://developer.android.com/reference/androidx/health/connect/client/HealthConnectClient
  - Health Connect permissions and access UX: https://developer.android.com/health-and-fitness/health-connect/ui/permissions
- safe implementation/QA goal: on a disposable profile/device, force or select a provider-missing/update-required state and verify TrainIQ shows provider recovery copy/action, remains manually usable, and records no TrainIQ crash/ANR.
- exact evidence needed: `adb devices`, provider package list/status, TrainIQ package dump, Settings/Health Connect UI dump or screenshot, launch timing, logcat crash/ANR slice, and evidence that the profile/device is disposable.
- pass criteria: app reaches Start/Settings, Health Connect status is `PROVIDER_MISSING` or equivalent user copy, install/update action is available, manual flows remain reachable, and logcat has no TrainIQ `FATAL EXCEPTION`, ANR, or Application Not Responding event.
- fail criteria: crash/ANR, Health Connect API call attempted without availability guard, misleading "connected" state, no recovery action, or manual app use blocked by missing provider.
- remaining blocker status: `BLOCKED` until disposable provider-state evidence exists.

### BLOCKED-HC-002: Health Connect partial grant

- why blocked: partial health permission grants could not be safely changed on the connected profile.
- needed: disposable profile/device and scripted permission-manager steps for steps/heart rate/sleep/calories/workouts partial grant combinations.
- prevents claiming no known app flaws: no, because no flaw was reproduced; yes for unqualified `APP QA PASSED`.
- researched risk: Android lets users grant or revoke Health Connect data access independently. Android also recommends separate Changes tokens per data type when data types are consumed independently, because one revoked permission must not poison unrelated sync.
- official sources:
  - Health Connect sync and per-data-type Changes tokens: https://developer.android.com/health-and-fitness/health-connect/sync-data
  - Health Connect get started, permission loss handling: https://developer.android.com/health-and-fitness/health-connect/get-started
  - Health Connect permissions and access UX: https://developer.android.com/health-and-fitness/health-connect/ui/permissions
- safe implementation/QA goal: grant at least one metric and deny at least one metric on a disposable profile, then verify TrainIQ syncs granted metrics, marks denied metrics separately, preserves unrelated caches/tokens, and uses partial-success copy instead of implying all signals are mandatory.
- exact evidence needed: before/after Health Connect permission state, TrainIQ Settings/Home UI dump or screenshot, persisted status/copy evidence where available, logcat crash/ANR slice, and Gradle test evidence for Health Connect policy tests.
- pass criteria: granted metrics show `SYNCED`, `STALE`, or no-data states based on real data; denied metrics show `DENIED`; granted data is not displayed as zero because another metric is denied; unrelated metric tokens/caches are not cleared; app remains usable.
- fail criteria: all-or-nothing permission messaging, denied metrics silently treated as measured zero, granted metrics blocked by denied metrics, cache/token clearing for unrelated metrics, retry loop, crash, or ANR.
- remaining blocker status: `BLOCKED` until disposable partial-grant runtime evidence exists.

### BLOCKED-HC-003: Health Connect revoke while app open

- why blocked: revoking health permissions while TrainIQ is foregrounded was not safe on the connected profile.
- needed: disposable profile/device and a scripted foreground revoke test with logcat capture.
- prevents claiming no known app flaws: no, because no flaw was reproduced; yes for unqualified `APP QA PASSED`.
- researched risk: Android states users can revoke permissions at any time, so TrainIQ must re-check permission state before use and after lifecycle return. Health Connect foreground reads can be interrupted and must continue safely when the app is opened again.
- official sources:
  - Health Connect get started, permission re-checking: https://developer.android.com/health-and-fitness/health-connect/get-started
  - Health Connect sync, foreground/background read constraints: https://developer.android.com/health-and-fitness/health-connect/sync-data
  - Health Connect permissions and access UX: https://developer.android.com/health-and-fitness/health-connect/ui/permissions
- safe implementation/QA goal: on a disposable profile, launch TrainIQ, open Health Connect manage-access, revoke one or more granted metrics, return to TrainIQ, and verify lifecycle refresh updates visible status without crash, stale misleading copy, or background retry loops.
- exact evidence needed: foreground TrainIQ UI before revoke, Health Connect manage-access/revoke evidence, TrainIQ UI after returning, permission state dump if available, WorkManager/background sync evidence where available, and logcat crash/ANR slice.
- pass criteria: TrainIQ refreshes on resume, revoked metrics become denied/insufficient-access states, remaining granted metrics stay independent, no permanent-failure retry loop appears, and manual app flows remain usable.
- fail criteria: app keeps showing full connected status after revoke, crashes, hangs, retries permanent permission failures, clears unrelated caches/tokens, or blocks manual app use.
- remaining blocker status: `BLOCKED` until disposable revoke-while-open runtime evidence exists.

### BLOCKED-CAMERA-001: Real camera/barcode capture

- why blocked: this audit did not use a controlled real barcode/meal capture input.
- needed: safe camera test setup with known barcode/meal label input and permission reset.
- prevents claiming no known app flaws: no for scanner open/deny/fallback behavior; yes for claiming full real-world scanner recognition quality.
- researched risk: ML Kit barcode scanning supports on-device barcode recognition from CameraX/image input, but recognition quality depends on controlled input, focus, resolution, lighting, barcode size, and orientation. TrainIQ must also preserve manual-entry fallback when camera, permission, AI config, or recognition fails.
- official sources:
  - ML Kit barcode scanning on Android: https://developers.google.com/ml-kit/vision/barcode-scanning/android
  - CameraX ML Kit Analyzer: https://developer.android.com/media/camera/camerax/mlkitanalyzer
  - Google Code Scanner, permissionless alternative for basic barcode scanning: https://developers.google.com/ml-kit/vision/barcode-scanning/code-scanner
- safe implementation/QA goal: on an approved camera test device/profile, grant and reset camera permission safely, scan a known EAN/UPC fixture and a known meal/package label fixture, rotate/recreate where safe, and verify recognition/result/fallback states without blocking manual entry.
- exact evidence needed: approved test device/profile note, permission state before/after, barcode fixture value, photo/meal fixture description without sensitive personal data, scanner UI screenshots or dumps, result/fallback UI, rotation/recreation evidence where safe, and logcat crash/ANR slice.
- pass criteria: scanner preview opens after permission grant, known barcode is detected or produces clear retry/manual-entry fallback, meal photo reaches processing/result/fallback without blocking manual meal entry, permission reset is documented, and no TrainIQ crash/ANR appears.
- fail criteria: camera permission cannot recover, preview bind failure lacks fallback, known barcode fails without manual path, meal capture blocks manual entry, rotation loses critical state, or crash/ANR appears.
- remaining blocker status: `BLOCKED` until approved real camera/barcode runtime evidence exists.

## Blocked Area Research And Safe Completion Plan

The current codebase already has source-level guardrails aligned with the researched platform behavior:

- `HealthConnectDataSource` checks `HealthConnectClient.getSdkStatus()`, distinguishes unsupported/provider-missing/permission-required states, uses per-metric permission status, uses per-record-type Changes tokens, and keeps partial grants independent.
- `HealthConnectUiHelpers` uses per-signal rationale copy and partial-success permission result copy.
- `HealthConnectBackgroundSyncWorkerTest` and related policy tests guard permanent permission/provider states against retry loops.
- `CameraScannerScreen` handles no-camera and CameraX bind-failure fallback states, keeps permission request user-initiated, and preserves permission-denied/camera-error state across recreation.
- `CameraScannerStateTest` covers scanner copy, fallback, permission, result, and accessibility label guardrails.

Safe completion sequence:

1. Run the non-mutating baseline collector first: `TrainIQ-Project/scripts/collect-health-connect-runtime-evidence.ps1`.
2. Use only a disposable Android profile/device for mutable Health Connect cases. Do not remove, disable, grant, revoke, or reset Health Connect state on a personal profile.
3. Capture one evidence folder per mutable case. Each folder must include launch logs, UI dump/screenshot, permission/provider state evidence, and a logcat crash/ANR slice.
4. Keep each Health Connect case independent: reset app state and Health Connect grants between provider-missing, partial-grant, revoke-while-open, and background-read checks.
5. Use a safe camera setup with known non-sensitive barcode/meal fixtures. Record the expected barcode value and expected high-level meal label, but do not store personal health, payment, identity, or private household data in QA artifacts.
6. If a failure is reproduced, add it to `Failures` with exact evidence path, severity, user impact, concrete fix goal, and verification needed. Do not silently convert a blocker to pass.

Recommended disposable Health Connect matrix:

| Scenario | Minimum setup | Required result |
| --- | --- | --- |
| Provider missing/update | Disposable profile where provider is absent, disabled, outdated, or otherwise reports update-required | Provider recovery copy/action, manual app use still available, no crash/ANR |
| No permissions | Provider available, no TrainIQ Health Connect grants | Permission-required copy, rationale/manage-access path, no crash/ANR |
| Partial grant | At least one granted metric and one denied metric | Granted metrics sync independently; denied metrics are explicit; no unrelated cache/token clearing |
| Revoke while open | Start with at least one granted metric, revoke via Health Connect while TrainIQ is open or paused | On return, status refreshes and revoked metric is denied; no stale full-connected state |
| Background read unavailable/granted | Controlled feature/permission state | `canReadInBackground()` result matches feature/grant state; no retry loop for permanent denial |

Recommended camera/barcode matrix:

| Scenario | Minimum setup | Required result |
| --- | --- | --- |
| Camera denied | Reset camera permission, deny from runtime prompt | Permission gate and Settings path remain understandable; no crash/ANR |
| Preview after grant | Grant camera on approved test profile | Preview opens or bind-failure fallback appears; manual entry remains available |
| Known barcode | Use a controlled EAN/UPC fixture with known value | Barcode result is detected or a clear retry/manual fallback appears |
| Known meal/package label | Use a non-sensitive meal or package label fixture | Capture reaches processing/result/fallback and never blocks manual meal entry |
| Rotation/recreation | Rotate permission gate and preview/capture where safe | Critical copy/actions remain visible and state is not lost incorrectly |

## Definition Of Done For Blocked Areas

- This document contains, for each blocked area, researched risk, official sources, safe implementation/QA goal, exact evidence needed, pass/fail criteria, and remaining blocker status.
- No personal Health Connect profile, provider state, or camera/photo data is mutated or captured for these blockers.
- Disposable-profile/device evidence is captured for every mutable Health Connect state before any Health Connect blocker is changed from `BLOCKED` to `PASS`.
- Real camera/barcode evidence is captured with controlled, non-sensitive input before `BLOCKED-CAMERA-001` is changed from `BLOCKED` to `PASS`.
- No blocker is marked `PASS` without UI dump or screenshot, crash/ANR logcat slice, permission/provider state evidence where relevant, and matching Gradle checks.
- Any code change made after reproducing a failure is small, linked to the reproduced evidence, and covered by targeted tests.
- The final verdict is changed to unqualified `APP QA PASSED` only when all blocked areas have runtime evidence and no confirmed P0/P1/P2 flaw remains.
- If required safe devices, owner approvals, credentials, or test fixtures are unavailable, the related area remains explicitly `BLOCKED` with reason and next evidence needed.

## Conclusion

TrainIQ has no known unresolved app-visible flaw in the tested scope of this audit. Every requested core flow is either `PASS` or explicitly `BLOCKED` by unavailable safe test conditions. Required Gradle verification passed, connected-device tests passed, cold launch passed, and logcat did not show a TrainIQ crash or ANR.

Final verdict remains **APP QA PASSED WITH BLOCKED AREAS** until the Health Connect mutable permission/provider states and real scanner capture conditions can be safely tested.
