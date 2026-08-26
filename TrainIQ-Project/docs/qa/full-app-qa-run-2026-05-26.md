# TrainIQ Full-App QA Run Template

Date: 2026-05-26
Tester: Codex using `trainiq-full-app-qa`
Build variant: debug
App version/build id: versionName `1.0.1-A`, versionCode `2` from `app/build.gradle.kts`
Commit/build identifier: not recorded in this run
Device/emulator: Medium_Phone_2 AVD (`emulator-5554`, `sdk_gphone64_x86_64`)
Android version: 16 as reported by Gradle test target label
Theme: default/system
Font scale: default
Network state: not controlled
Health Connect state: not mutated; existing docs show no-permission baseline only
AI provider/key state: not configured for real provider calls in this run

Status values: `PASS`, `FAIL`, `NOT RUN`.

## Automated Baseline

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| Debug build | `.\gradlew.bat :app:assembleDebug --console=plain` | PASS | Re-run successful on 2026-05-26. |
| JVM unit tests | `.\gradlew.bat :app:testDebugUnitTest --console=plain` | PASS | Re-run successful on 2026-05-26. |
| Lint | `.\gradlew.bat :app:lintDebug --console=plain` | PASS | Re-run successful on 2026-05-26; report available at `app/build/reports/lint-results-debug.html`. |
| Connected tests | `.\gradlew.bat :app:connectedDebugAndroidTest --console=plain` | PASS | Re-run successful on 2026-05-26: 44 tests on `Medium_Phone_2(AVD) - 16`. |
| AI/nutrition targeted | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.ai.services.AiServicesTest" --console=plain` | PASS | Re-run successful on 2026-05-26; includes multi-component scan identity guards. |
| Profileable benchmark build | `.\gradlew.bat :app:assembleProfileable :macrobenchmark:assembleAndroidTest --console=plain` | PASS | Build successful on 2026-05-26. This is build readiness only, not performance certification. |
| Macrobenchmark physical device | `.\gradlew.bat :macrobenchmark:connectedProfileableAndroidTest --console=plain` | NOT RUN | No physical device performance run was executed; emulator timings are not release-certification evidence. |

## Targeted Feature Unit Checks

| Area | Command | Status | Evidence/notes |
|---|---|---|---|
| Navigation | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.navigation.*" --console=plain` | PASS | Re-run successful on 2026-05-26. |
| Data layer | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.*" --console=plain` | PASS | Deep re-run successful on 2026-05-26. |
| Domain/use cases | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.domain.*" --console=plain` | PASS | Deep re-run successful on 2026-05-26. |
| Training | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.*" --console=plain` | PASS | Re-run successful on 2026-05-26. |
| Voeding | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.*" --console=plain` | PASS | Initial parallel run hit a Gradle test-result file collision; sequential re-run passed. |
| Voortgang | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.progress.*" --console=plain` | PASS | Re-run successful on 2026-05-26. |
| Coach | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.coach.*" --console=plain` | PASS | Re-run successful on 2026-05-26. |
| Meer/Instellingen | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.settings.*" --console=plain` | PASS | Re-run successful on 2026-05-26. |

## Start/Home

| Flow | Status | Evidence/notes |
|---|---|---|
| First run without profile/Health Connect/routine/data | NOT RUN | |
| Dashboard cards render and remain readable | PASS | Manual emulator UI dump after app launch shows `TrainIQ`, `Vandaag in een slimme cockpit`, setup cards and bottom navigation. Evidence: `docs/qa/evidence/2026-05-26-tab-start.xml` and `.png`. |
| CTA to profile/settings works | NOT RUN | Profile/settings CTA was visible, but not tapped in this deeper pass. |
| CTA to Health Connect works | NOT RUN | Health Connect CTA was visible, but permission/system flow was not opened in this pass. |
| CTA to Training works | PASS | Bottom navigation tap opened Training. Evidence: `docs/qa/evidence/2026-05-26-tab-training.xml`. |
| CTA to Coach works | NOT RUN | |
| Loading/empty/partial/error states are understandable | NOT RUN | |
| Dashboard data survives app restart where expected | NOT RUN | |
| Dark mode, large font, dynamic color, tablet/foldable layout | NOT RUN | |

## Training

| Flow | Status | Evidence/notes |
|---|---|---|
| Routine list opens | PASS | Manual emulator UI dump shows Training tab, `Routine maken`, active routine empty state, routines empty state and exercise library. Evidence: `docs/qa/evidence/2026-05-26-tab-training.xml`. |
| Create routine | NOT RUN | |
| Edit/delete routine | NOT RUN | |
| Generated routine preview save/cancel | NOT RUN | |
| Exercise library and picker | NOT RUN | |
| Exercise history | NOT RUN | |
| Start active workout | NOT RUN | |
| Add/edit/delete/undo set | NOT RUN | |
| Weight/reps/RPE/set type/rest timer | NOT RUN | |
| Active workout restore after recreate/restart | NOT RUN | |
| Finish workout and processing route | NOT RUN | |
| Workout completion screen | NOT RUN | |
| AI debrief valid Gemini response | NOT RUN | |
| AI debrief fallback reasons: disabled/missing key/malformed/timeout/rate-limit/offline | NOT RUN | |
| Dense controls: touch targets, no overlap at large font | NOT RUN | |

## Voeding

| Flow | Status | Evidence/notes |
|---|---|---|
| AI meal scanner opens | NOT RUN | Nutrition tab opened, but scanner route was not launched in this manual pass. |
| Multi-component scan keeps item identity | NOT RUN | |
| Suspicious duplicate AI output shows review warning | NOT RUN | |
| Manual food add/edit/delete | NOT RUN | |
| Products list and quick add | NOT RUN | Nutrition tab visible; section sheet/product editor was not opened in this pass. |
| Recipes add/edit/delete/use in meal | NOT RUN | |
| Meal logging save/reopen/restart | NOT RUN | |
| Meal history and reuse meal | NOT RUN | |
| Barcode scanner and lookup success/fail | NOT RUN | |
| Camera denied/no camera/manual fallback | NOT RUN | |
| Missing AI key/invalid AI response/local fallback | NOT RUN | |
| Long forms, keyboard/IME, dark mode, large font | NOT RUN | Partial evidence only: Runtime launch was captured in dark mode with font scale 1.5, but blocked by Android immersive-mode overlay until dismissed. Evidence: `docs/qa/evidence/2026-05-26-runtime-start-dark-font15.png`. |
| Historical meal snapshots do not silently change after product/recipe edits | NOT RUN | |

## Voortgang

| Flow | Status | Evidence/notes |
|---|---|---|
| Add body measurement | NOT RUN | |
| Edit/delete measurement | NOT RUN | |
| Invalid values show clear validation | NOT RUN | |
| Save/reopen/restart measurement integrity | NOT RUN | |
| Smart-scale scanner valid result | NOT RUN | |
| Smart-scale scanner partial/no result/manual fallback | NOT RUN | |
| Charts render and expose accessibility summary | NOT RUN | Partial evidence only: Voortgang opened via Meer and showed empty-state measurement UI. Chart data/accessibility summary not validated because there is no seeded progress data. Evidence: `docs/qa/evidence/2026-05-26-tab-voortgang-via-meer-exact.xml`. |
| Empty states, dark mode, large font | NOT RUN | |

## Coach

| Flow | Status | Evidence/notes |
|---|---|---|
| Goal advice form and result | NOT RUN | Partial evidence only: Coach tab opened and displayed profile/goal form fields. Result generation not exercised. Evidence: `docs/qa/evidence/2026-05-26-tab-coach.xml`. |
| Weekly report | NOT RUN | |
| Training insights | NOT RUN | |
| Nutrition coach message | NOT RUN | |
| AI enabled valid JSON | NOT RUN | |
| AI disabled/missing key/invalid/English/timeout/rate-limit/offline | NOT RUN | |
| Profile/calorie baseline is not overwritten by AI | NOT RUN | |
| Source labels, bullets, loading/error/fallback clarity | NOT RUN | Partial evidence only: Coach initial/profile-required state is readable. AI result/fallback states not exercised manually in this pass. |
| Deep-mode thinking budget and JSON schema contract | NOT RUN | |

## Meer/Instellingen

| Flow | Status | Evidence/notes |
|---|---|---|
| Theme mode changes | NOT RUN | Partial evidence only: Settings showed theme controls and runtime dark mode/font-scale launch was captured. Theme buttons were not manually toggled inside app. |
| Telemetry opt-in/out | NOT RUN | |
| Gemini API key save/delete | NOT RUN | |
| OpenAI API key save/delete | NOT RUN | |
| Provider preference | NOT RUN | |
| Health Connect status refresh | NOT RUN | Health Connect status text was visible; refresh action was not tapped. |
| Health Connect rationale | NOT RUN | Not opened because this pass avoided external/system permission mutation. |
| Health Connect settings/install/update links | NOT RUN | |
| Local data clear confirmation and effect | NOT RUN | |
| Destructive dialogs safe and accessible | NOT RUN | |
| Secrets absent from logs, URLs, screenshots, BuildConfig production values | PASS | Repo scan outside build outputs found no `AIza`, `sk-`, or `sk-proj` key patterns. Logcat scan found no key patterns. Screenshots captured first-run/empty states without secrets. |

## Cross-Tab Runtime

| Flow | Status | Evidence/notes |
|---|---|---|
| Tab switching Start -> Training -> Voeding -> Coach -> Meer -> Voortgang via Meer | PASS | Connected smoke passes and manual adb taps captured Start, Training, Voeding, Coach, Meer and Voortgang via Meer. Evidence under `docs/qa/evidence/2026-05-26-tab-*.xml`. |
| Back stack behavior | NOT RUN | |
| Scanner return values through savedStateHandle | NOT RUN | |
| Camera permission denied/granted | NOT RUN | |
| Health Connect no permission | NOT RUN | |
| Health Connect partial/revoke/background-read on safe profile | NOT RUN | |
| Offline/slow network for AI and barcode | NOT RUN | |
| App background/foreground, lock/unlock | NOT RUN | |
| Rotation/recreate on high-risk screens | NOT RUN | |
| Logcat crash/ANR slice after smoke | PASS | `adb logcat` scans after connected/manual runtime pass found no `FATAL EXCEPTION` or `ANR`. AndroidRuntime matches were from uiautomator command startup/shutdown, not app crash evidence. |

## Accessibility And Design

| Flow | Status | Evidence/notes |
|---|---|---|
| TalkBack high-risk flows | NOT RUN | |
| Switch Access high-risk flows | NOT RUN | |
| Font scale 1.3 and 1.5 | NOT RUN | Partial evidence only: Font scale 1.5 launch screenshot captured. Font scale 1.3 and deep screen traversal not run. |
| Dark mode and dynamic color | NOT RUN | Partial evidence only: Dark mode launch screenshot captured. Dynamic color validation not run. |
| Touch targets | NOT RUN | |
| Content descriptions and focus order | NOT RUN | |
| Text overlap/clipping check | NOT RUN | |
| Modal/dialog focus containment | NOT RUN | |

## Findings

Add findings below using the schema from `docs/qa/full-app-qa-basis.md`.

## Finding QA-2026-05-26-001

- priority: P1
- area: tests, data, release
- tab/flow: Room migration/runtime readiness gate
- status: fixed
- current evidence: Fixed Room migration-chain fixture drift from Room version 12 to current version 13. `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.migration.RoomMigrationChainVerificationProviderTest" --console=plain` passed. Full `.\gradlew.bat :app:testDebugUnitTest --console=plain` passed.
- expected behavior: Room migration-chain verification tests define a trusted fail-closed gate and pass consistently.
- actual behavior: Fixed; full JVM unit test gate passes.
- repro steps: Run `.\gradlew.bat :app:testDebugUnitTest --console=plain` from `TrainIQ-Project`.
- recommended fix: Completed by aligning tests with `CurrentRoomVersion = 13` without weakening fail-closed marker behavior.
- regression risk: High; this is a migration/readiness gate, so incorrect fixes can weaken data migration safety.
- minimal verification: `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.migration.RoomMigrationChainVerificationProviderTest" --console=plain`
- owner suggestion: Android/data owner

## Finding QA-2026-05-26-002

- priority: P1
- area: tests, navigation, UX
- tab/flow: Compact bottom navigation and swipe policy
- status: fixed
- current evidence: Restored compact bottom navigation overflow policy to 5 visible destinations: Start, Training, Voeding, Coach, Meer. `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.navigation.AdaptiveNavigationPolicyTest" --console=plain` passed. Full `.\gradlew.bat :app:testDebugUnitTest --console=plain` passed.
- expected behavior: Compact bottom navigation policy and swipe navigation policy remain aligned with visible destinations.
- actual behavior: Fixed; compact navigation and swipe policy match expected visible destinations.
- repro steps: Run `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.navigation.AdaptiveNavigationPolicyTest" --console=plain`.
- recommended fix: Completed by keeping compact phones on 5 tabs + Meer, with Voortgang discoverable from Settings/Meer.
- regression risk: Medium-high; incorrect navigation policy can hide tabs, break swipe behavior, or confuse compact phone users.
- minimal verification: `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.navigation.AdaptiveNavigationPolicyTest" --console=plain`
- owner suggestion: Android/UI owner

## Finding QA-2026-05-26-003

- priority: P1
- area: tests, frontend, navigation
- tab/flow: Full top-level tab smoke Start -> Training -> Voeding -> Coach -> Meer -> Voortgang
- status: fixed
- current evidence: Updated smoke flow to the compact 5-tabs + Meer policy and current UI copy. `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.flow.TrainIqFlowSmokeInstrumentedTest" --console=plain` passed. Full `.\gradlew.bat :app:connectedDebugAndroidTest --console=plain` passed with 44 tests on `Medium_Phone_2(AVD) - 16`.
- expected behavior: Clean first-run smoke can navigate through all top-level tabs and expose guidance/fallback states.
- actual behavior: Fixed; connected full-tab smoke passes.
- repro steps: Run `.\gradlew.bat :app:connectedDebugAndroidTest --console=plain` with emulator `Medium_Phone_2(AVD) - 16`.
- recommended fix: Completed by using compact navigation selectors, current Nutrition/Settings copy, and checkpoint-specific timeout messages.
- regression risk: Medium-high; this is the automated guard for top-level frontend coverage.
- minimal verification: `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.flow.TrainIqFlowSmokeInstrumentedTest --console=plain`
- owner suggestion: Android/UI QA owner

## Finding QA-2026-05-26-004

- priority: P2
- area: frontend, UX/design, accessibility
- tab/flow: App launch / first manual runtime launch
- status: fixed
- current evidence: Fixed on 2026-05-27 by removing global navigation-bar hiding from `MainActivity` while keeping `enableEdgeToEdge()`. Runtime verification passed after `pm clear` and cold start: UI dump contains TrainIQ first-launch content and compact bottom navigation, and contains no `immersive_cling`, `Viewing full screen`, or `Got it` system overlay. Evidence: `docs/qa/evidence/2026-05-27-first-launch-no-immersive.xml` and `docs/qa/evidence/2026-05-27-first-launch-no-immersive.png`.
- expected behavior: First app launch should show TrainIQ onboarding/dashboard directly without a system overlay blocking initial QA/user interaction unless full-screen mode is intentionally required.
- actual behavior: Fixed; TrainIQ first-launch UI appears directly without Android immersive-mode education overlay.
- repro steps: Install debug app on `Medium_Phone_2(AVD) - 16`, clear app data, run `adb shell am start -W -n com.trainiq/.MainActivity`, then dump UI with `uiautomator`.
- recommended fix: Completed by removing global calls to hide navigation bars in `MainActivity`; scanner/fullscreen behavior remains separate from normal app tabs.
- regression risk: Medium; changing window flags can affect edge-to-edge layout, navigation bar padding, scanner surfaces and keyboard/IME behavior.
- minimal verification: Emulator verification passed on 2026-05-27: `.\gradlew.bat :app:installDebug --console=plain`, `adb shell pm clear com.trainiq`, `adb shell am start -W -n com.trainiq/.MainActivity`, `uiautomator dump`; dump shows TrainIQ UI and no immersive overlay. Physical-device verification remains part of release gate.
- owner suggestion: Android/UI owner

## Final QA Decision

Overall status: `PARTIAL`

Highest-risk resolved issues:

- P1 fixed: Full JVM unit test baseline now passes after Room migration-chain and adaptive navigation fixes.
- P1 fixed: Connected full-tab smoke and full connected test suite now pass on emulator.

Highest-risk open issues:

- P2 fixed: Android immersive-mode system overlay no longer blocks first manual runtime launch on emulator after removing global navigation-bar hiding.
- Release certification remains blocked by manual accessibility, physical-device performance thresholds/evidence, and incomplete Health Connect runtime matrix.

Release gates still open:

- Accessibility: TalkBack/Switch Access/large-font/dark-mode signoff remains `NOT RUN`.
- Performance: physical-device matrix and numeric thresholds remain open.
- Health Connect: provider missing/update, partial permissions, revoke while open, and background-read states remain incomplete.
- Privacy/security: owner/legal confirmation still required for production data safety decisions if production behavior changes.

Next safest action:

Continue deeper manual full-tab QA for Training, Voeding, Voortgang, Coach and Meer now that automated P1 blockers are cleared.
