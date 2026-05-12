# TrainIQ QA Findings - Flow Audit

Audit date: 2026-05-12

Scope: read-only `trainiq-cycle` QA audit for normal flow, break-app flow, and user-doesn't-know flow. The audit inspected Android source, existing QA/progress docs, recent device evidence, Gradle verification, and a minimal physical-device smoke on SM-S931B.

## Verification Summary

- PASS: `.\\gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`
- PASS: `.\\gradlew.bat :app:testDebugUnitTest --console=plain --no-configuration-cache`
- PASS: `.\\gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`
- PASS: `.\\gradlew.bat :app:installDebug --console=plain --no-configuration-cache` on SM-S931B (`RFCY60HNHNJ`)
- PASS/PARTIAL: physical-device smoke launched `com.trainiq/.MainActivity` cold with `Status: ok`, `LaunchState: COLD`, `WaitTime: 742`, and no `FATAL EXCEPTION`/ANR lines in the checked logcat slice.
- PASS/PARTIAL: top-level traversal reached Start, Training, Voeding, Coach, and Meer/Instellingen on SM-S931B. Progress was reachable through Settings as `Voortgang openen`.
- NOT RUN: destructive or state-resetting first-run cleanup. The connected device retained previous QA data, so the true clean-install user-doesn't-know flow was assessed from source and prior evidence rather than by clearing device data.
- NOT RUN: full Health Connect provider-missing, partial-grant, revoke-while-open, and background-read runtime states. Existing docs already mark those as open and they require a disposable/safe permission profile.

## Findings

### QA-2026-05-12-FLOW-001

- finding_id: QA-2026-05-12-FLOW-001
- priority: P2
- area: tests, Android lifecycle, UX
- status: open
- owner suggestion: Android QA / instrumentation owner
- current evidence with file references:
  - The requested normal, break-app, and user-doesn't-know paths are currently verified by a mix of source guards, manual device dumps, and ad hoc smoke commands rather than one repeatable flow suite.
  - `TrainIQ-Project/app/src/test/java/com/trainiq/navigation/AdaptiveNavigationPolicyTest.kt:14` through `TrainIQ-Project/app/src/test/java/com/trainiq/navigation/AdaptiveNavigationPolicyTest.kt:24` guard compact navigation policy, but they do not launch the app and exercise full user flows.
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/nutrition/CameraScannerStateTest.kt` covers scanner labels/state helpers, while existing runtime evidence in `TrainIQ-Project/.codex/device-qa/2026-05-12-scanner-rotate-recreate-qa/` covers one scanner permission-gate rotation path.
  - 2026-05-12 smoke in this audit launched and traversed top-level tabs on SM-S931B, but it did not persist as a checked-in connected test or script.
- external sources used: None. Local source, tests, and runtime evidence were sufficient.
- expected target-state behavior: Normal, break-app, and first-time/confused-user paths have a repeatable connected QA entrypoint that can be rerun without relying on manual tap coordinates or stale device state.
- concrete recommended fix: Add a small connected UIAutomator or Compose instrumentation flow suite that installs/launches the app, verifies top-level navigation, opens Settings Health Connect/Gemini guidance, verifies Nutrition manual and scanner-denied fallbacks, and checks Training setup copy. Keep it non-destructive by using a deterministic debug/test fixture or a disposable app-data reset mode approved for QA.
- regression risk: Medium. Flow tests can become flaky if they depend on text timing, device state, or existing user data; keep selectors text/semantics based and isolate seed/reset setup.
- minimal verification command/check: `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=<new flow test class>" --console=plain --no-configuration-cache`

### QA-2026-05-12-FLOW-002

- finding_id: QA-2026-05-12-FLOW-002
- priority: P2
- area: accessibility, UI
- status: open
- owner suggestion: Android UI/accessibility owner
- current evidence with file references:
  - SM-S931B runtime dump during this audit reached Settings and exposed a clipped/NAF node in the `Weergave` section: a theme chip at bounds `[96,1796][326,1818]` was marked `NAF="true"` while the visible scroll container ended at `[0,91][1080,1746]`.
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/settings/SettingsSection.kt:524` starts the Settings content with `ScreenHeader`, then `Snelle status`, then `Weergave`; on a tall compact phone the third theme chip can land partially clipped at the bottom of the first viewport.
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/settings/SettingsUiStateTest.kt:112` through `TrainIQ-Project/app/src/test/java/com/trainiq/features/settings/SettingsUiStateTest.kt:116` guard Health Connect action semantics, but there is no equivalent guard for the theme-mode chips.
- external sources used: None. Local UI dump and source were sufficient.
- expected target-state behavior: Settings controls that are focusable/clickable should expose accessible text or content descriptions even when near a scroll boundary, and compact first-viewport dumps should not surface actionable `NAF=true` nodes.
- concrete recommended fix: Add explicit semantics labels to theme-mode chips or adjust the Settings `Weergave` layout/spacing so partially clipped chips are not focusable without labels. Add a focused Settings accessibility/source guard for theme-mode chip labels and rerun compact-device UI dump evidence.
- regression risk: Low. This is localized Settings UI semantics/layout polish, but it touches a shared first-run Settings path.
- minimal verification command/check: Focused `SettingsUiStateTest`, broad `:app:lintDebug`, and a compact-device UI dump with `NAF=0` for Settings first viewport and scrolled `Weergave`.

### QA-2026-05-12-FLOW-003

- finding_id: QA-2026-05-12-FLOW-003
- priority: P2
- area: tests, UX, data
- status: open
- owner suggestion: Android QA / data owner
- current evidence with file references:
  - This audit intentionally did not clear app data. The SM-S931B Start dump showed the setup checklist with `Klaar - Routine maken of starten`, and the Training dump showed an existing `QAFontRoutine`, so the runtime smoke was not a clean first-run user-doesn't-know session.
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/home/HomeScreen.kt:453` through `TrainIQ-Project/app/src/main/java/com/trainiq/features/home/HomeScreen.kt:477` defines the source-level first-setup checklist and actions.
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt:6238` and `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt:6244` define `Routine inrichten` copy for not-startable routines, but current physical-device state could not validate the empty/no-routine branch without data reset.
- external sources used: None. Local runtime state and source were sufficient.
- expected target-state behavior: QA can verify the confused first-time user path from a known empty profile/no routine/no AI key/no Health Connect permission state without destroying a developer's real local state.
- concrete recommended fix: Provide a safe debug-only first-run fixture or documented disposable-profile workflow that seeds no profile, no routine, no AI key, and no Health Connect grants. Use it for normal and user-doesn't-know flow QA before closing first-run UX findings.
- regression risk: Medium. Reset/fixture tooling can accidentally hide persistence issues or clear useful local evidence if not scoped to debug/test runs.
- minimal verification command/check: Launch a debug/test build from the approved empty fixture and capture Start, Training, Nutrition, Coach, Settings, and scanner-denied states with no retained QA routine or user profile.

### QA-2026-05-12-FLOW-004

- finding_id: QA-2026-05-12-FLOW-004
- priority: P3
- area: UX, navigation
- status: open
- owner suggestion: Android UX owner
- current evidence with file references:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/navigation/TrainIqNav.kt:370` through `TrainIQ-Project/app/src/main/java/com/trainiq/navigation/TrainIqNav.kt:375` remove `Progress` from compact bottom navigation.
  - `TrainIQ-Project/app/src/test/java/com/trainiq/navigation/AdaptiveNavigationPolicyTest.kt:14` through `TrainIQ-Project/app/src/test/java/com/trainiq/navigation/AdaptiveNavigationPolicyTest.kt:19` confirms this is intentional current policy.
  - SM-S931B smoke reached Settings via compact `Meer`, then showed `Voortgang openen` in the Settings `Snelle status` card. A new user must infer that Progress lives under `Meer`, not in the bottom navigation.
- external sources used: None. Local source, tests, and runtime dump were sufficient.
- expected target-state behavior: Normal flow should make every core top-level destination discoverable on compact phones without requiring users to know that `Voortgang` is nested under `Meer`.
- concrete recommended fix: Keep the compact overflow policy if needed, but make `Meer` read as a real overflow destination for Progress and Settings. Options include a dedicated `Meer` menu surface, clearer `Voortgang` affordance near the top of Settings, or a compact navigation pattern that exposes Progress when progress is a current next action.
- regression risk: Medium. Navigation changes can affect compact layout, swipe order, restore-state behavior, and existing adaptive navigation tests.
- minimal verification command/check: Update adaptive navigation tests for the chosen policy and rerun a compact-device top-level traversal confirming Start, Training, Voeding, Coach, Settings, and Voortgang are discoverable.

### QA-2026-05-12-FLOW-005

- finding_id: QA-2026-05-12-FLOW-005
- priority: P1
- area: Health Connect, Android lifecycle, release
- status: blocked
- owner suggestion: Android Health Connect / release QA owner
- current evidence with file references:
  - `docs/TrainIQ_Target_State_Progress.md:50` states Health Connect runtime evidence is only partially complete for app launch, Settings status, and no-permission copy; provider-missing, revoked, partial-permission, and background-read flows still need runtime evidence.
  - `docs/TrainIQ_Target_State_Progress.md:136` through `docs/TrainIQ_Target_State_Progress.md:138` record Health Connect runtime matrix gaps and background-read guard coverage.
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/settings/SettingsSection.kt:635` through `TrainIQ-Project/app/src/main/java/com/trainiq/features/settings/SettingsSection.kt:683` exposes Health Connect states and actions, but this audit only observed `Toegang vereist` on the connected physical device.
- external sources used: None in this audit. Existing progress docs already cite the prior Health Connect source research.
- expected target-state behavior: Break-app flow covers Health Connect missing provider, no permissions, partial grants, revoke while app is open, background read unavailable, and background read granted/unavailable states before release.
- concrete recommended fix: Run the existing Health Connect runtime matrix on a disposable device/profile where permissions and provider state can be safely changed. Record exact UI dumps, crash slices, and owner-readable PASS/FAIL status for each state.
- regression risk: Low for QA documentation, medium for any follow-up code changes in Health Connect permission handling.
- minimal verification command/check: Complete `TrainIQ-Project/docs/qa/health-connect-runtime-matrix-2026-05-10.md` rerun steps on a disposable profile and update the matrix with evidence paths.

## Flow Assessment

- Normal flow: PASS/PARTIAL. Build, unit tests, lint, install, cold launch, and compact top-level traversal passed. Current compact Progress discoverability remains a P3 UX risk.
- Break-app flow: PARTIAL. Camera/scanner rotation and denied-permission behavior have recent evidence, but this run did not safely cover all Health Connect edge states or a fully scripted adversarial flow.
- User-doesn't-know flow: PARTIAL. Source provides Dutch setup guidance for profile, routine, manual nutrition, AI key, and Health Connect, but the connected device retained QA data, so a true clean first-run runtime pass remains open.

## Files Changed

- `docs/TrainIQ_QA_Findings_2026-05-12_Flow_Audit.md`: new standalone findings file for later implementation work.
