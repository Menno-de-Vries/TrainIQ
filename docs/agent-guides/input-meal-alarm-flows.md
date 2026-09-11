# Input, meal detail and sleep alarm changes

Task base: `origin/main` at `935ed0b`. Task branch: `codex/input-meal-alarm-flows`. Primary worktree: `C:/My-PC-Files/GitHub/TrainIQ`; no task worktree was created. Existing APK worktrees and unrelated branches remain owned by their original tasks.

## Behavior and boundaries

- Coach opens the existing sleep route and body-progress route. Home no longer offers either registration action. Existing persistence, alarm settings and measurement history remain authoritative.
- Active-set drafts retain empty strings and intermediate decimals. Planned values initialize absent drafts; clearing a present draft no longer causes recomposition to restore defaults. Blur restores the value present when focus began only if the field is still empty. Commit retains existing planned fallbacks and numeric validation. Optional fields receive no new defaults.
- Recipe addition uses that recipe's cooked grams; recipes without cooked grams retain the previous 150 g default. Each recipe has independent editable grams. Meal requests continue to use grams and a separate serving count; existing snapshot mathematics and Room transactions are unchanged.
- Existing meal edits expose Cancel; cancellation clears only the draft and system back follows the same path. Saved product/recipe editors use the concise Cancel label.
- Nutrition shows category totals. Home retains the daily EnergyBalanceCard (intake, expenditure, balance and expandable expenditure sources), but contains no meal-category, individual food/recipe or macro overview. The serializable `MealDetail` destination is not a main navigation item. It reuses Nutrition's observation/editor/delete flow and shows the individual meal items. Category detail and editing observe the existing Room-backed nutrition source.
- Camera barcode recognition remains single-flight. A barcode stays in the scanner until provider lookup returns usable data. Missing/error results expose retry/manual continuation; cancellation invalidates pending responses. A successfully resolved product is handed to the parent editor without a second network request. Manual barcode input retains the existing editor lookup/error path. Editing product/ingredient fields invalidates a pending lookup, so an older response cannot overwrite newer manual input. Scanner lookup remains ViewModel-owned across rotation.
- Alarm scheduling keeps the existing PendingIntent identity, exact-alarm checks, fallback scheduling, reconciliation and v2 channel. A non-exported media-playback foreground service owns one `USAGE_ALARM` stream; its notification is silent. Confirmation/configuration/cancellation stop playback. A revision prevents queued starts from reviving canceled playback. Playback is bounded by the existing repeat interval and releases audio focus/resources. MediaPlayer holds a partial wake lock only during playback; WAKE_LOCK was already present through the merged WorkManager manifest and is now declared explicitly for this use. Background-start denial falls back to the existing notification mechanism. No alarm volume, DND policy, channel mute or user-selected ringtone is overridden; no duplicate scheduler or channel migration is introduced.
- Android remains the only app platform (`minSdk 26`, `targetSdk 36`). No Swift target, Xcode project, iOS signing profile or entitlement exists in this repository. iOS silent-mode support is not claimed.

## Platform references

Checked against the existing Android stack on 2026-09-11:

- [Android alarm scheduling](https://developer.android.com/develop/background-work/services/alarms): exact alarm permission and `setAlarmClock` semantics.
- [Foreground-service start restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start): user-requested exact alarms can qualify for background starts; fallback workers do not gain that exemption automatically.
- [Media-playback service type](https://developer.android.com/develop/background-work/services/fgs/service-types#media): foreground-service declaration/permission and Android 15 boot restrictions.
- [Audio attributes](https://developer.android.com/reference/android/media/AudioAttributes#USAGE_ALARM): alarm audio usage.
- [Apple critical alerts](https://developer.apple.com/documentation/usernotifications/unauthorizationoptions/criticalalert): bypassing mute/DND with critical notifications requires Apple's special entitlement. This Android-only change adds no iOS capability.
- [Emulator command-line options](https://developer.android.com/studio/run/emulator-commandline): the existing task-owned AVD required cold boot with `-no-snapshot` after its Quick Boot became unresponsive. No AVD was wiped or downloaded.

## Verification record

All builds/tests run locally with Android Studio's bundled JBR, the installed Android SDK and `--console=plain --max-workers=2`. No hosted runner, production provider call, signing change or artifact publication is part of this implementation.

The initial attempted baseline became invalid when source edits overlapped lint and lint's Kotlin frontend failed. It is not recorded as a passing baseline. A stable-source run subsequently passed `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin`, including 910 JVM tests. Later integration edits require final verification below.

Device: the existing `TrainIQ_Agent_API36_20260806`, started by this task as `emulator-5580`, Android 16/API 36. Starting display: 360 × 640 dp, font scale 1.3, dark theme. Physical audible output, hardware silent switch/OEM behavior and human TalkBack/Switch Access are not certified by emulator semantics or audio-service state.

## Input audit and integration findings

The focused input audit covered shared nutrition number/text fields, workout metric filtering/draft mapping, progress/weight inputs and Coach profile state. The active-set normalize-on-change and ifBlank-on-recomposition path was the cause; other inspected fields retain raw strings and parse at validation/commit. Existing nutrition and profile restoration tests remain applicable.

The real meal integration test exposed a focus/lifecycle problem when back canceled a focused gram editor. Cancel now clears input focus before discarding the draft. The regression uses Android back (including IME dismissal), checks unchanged Room rows and survives activity recreation. No dependency or schema change was needed.

Background playback reference: [Android MediaPlayer wake locks](https://developer.android.com/media/platform/mediaplayer/background).

## Final local gates

- PASS: `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug` with 913 JVM tests, zero failures/errors/skips. The final production source was built and tested in the polish run; the scanner and visual runs subsequently revalidated changed test sources with lint.
- PASS: `:app:connectedDebugAndroidTest` with `-Pandroid.testInstrumentationRunnerArguments.class=` selecting `MealDetailPersistenceTest`, `BarcodeMealFlowInstrumentedTest`, `BarcodeRecognitionInstrumentedTest`, `ActiveWorkoutSetActionsInstrumentedTest`, `ActiveWorkoutRestoreInstrumentedTest`, `SleepRoutineInstrumentedTest`, `SleepNotificationInstrumentedTest`, `SleepAlarmRegressionTest`, `FeatureRecoveryInstrumentedTest`, and `CoachProfileStateRestorationInstrumentedTest`: 37 tests. Names resolve under `com.trainiq.features` in their matching feature package (FeatureRecovery is directly under features).
- The additional 11-test scanner/feature/workout run passed eight tests and exposed three text-expectation failures. Two existing AI scanner assertions expected text absent from unchanged `origin/main` source (`935ed0b`): the context hint and scale import label. These assertions were corrected to the existing visible copy, preserving display checks. The new barcode recovery test was corrected to expect Barcodescanner after retry. PASS: all four `ScannerRecoveryInstrumentedTest` and `AiCameraScannerModesInstrumentedTest` tests in the corrective run. `CameraPermissionScannerInstrumentedTest` passed both cases in the earlier run.
- PASS: compact real-app visual/navigation run: `CoachHealthNavigationInstrumentedTest`, `MealDetailPersistenceTest`, `ActiveWorkoutSetActionsInstrumentedTest` (four tests). This covers Coach -> sleep -> back, Coach -> body/history -> back, cooked recipe grams and explicit overrides in Room, recreation, cancel/back without writes, category totals after edits/deletion, and focused workout editing plus persisted results.
- Visually inspected compact dark/130% captures of Coach, body registration, sleep alarm confirmation, recipe default grams, Home category totals, meal detail, focused workout input and barcode recovery/manual controls. Semantics assertions accompany the captures.

The new barcode ViewModel tests use controlled deferred responses; provider network timing is not simulated with sleeps. Existing barcode image-recognition and lookup/parser tests remain in the selected evidence. No live food-provider or AI requests were made.

Raw logs and images remain untracked under `.codex/device-qa/input-meal/`. No schema, migration, dependency, signing material or release artifact changed. Performance benchmarks and physical audible/hardware silent-mode tests were not run; no such claims are made.

- PASS: the same four real-app tests on the same AVD resized to 1200 x 800 dp (160 dpi), light mode, font scale 1.3. Expanded Coach/recipe/detail/workout captures were visually inspected. The original 720 x 1280 px at 320 dpi and dark mode were restored afterward. Across the selected classes, 44 distinct instrumentation tests passed; the four core routes also passed in the expanded configuration.
- Git review: main remained at `935ed0b` during verification; no effective branch rules or required checks were configured. The repository allows merge commits and recent PRs use that method. Hosted Actions remain skipped via the established `[skip ci]` convention; absence of a remote check is not a passing test.
