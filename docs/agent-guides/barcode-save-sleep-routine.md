# Optional barcode saving and sleep preparation

Implementation date: 2026-09-08. Base: `a07ba314ce9c02e1f6f7031a2c83c719e26b4c45` (`main`).

## Behavior and design

- The contextual barcode editor offers **Opslaan bij mijn producten**, initially unchecked. Its choice survives saved-instance restoration and resets for a new product. The existing meal snapshot/draft and final **Maaltijd opslaan** flow are preserved. Optional product saving uses `SaveFoodItemUseCase` and the existing transactional barcode match in `RoomTrainIqRuntimeStore.saveFood`; repeated scans reuse the existing ID. A failed optional save leaves the meal draft available and shows an explicit failure with recovery instructions.
- Settings opens the typed `SleepPreparation` route. The notification's private activity reuses the same screen. The current status and explicit **Ik ga binnen 2 minuten slapen** action are shown before configuration controls.
- A singleton Room 17 row stores enabled state, local preparation minute, next trigger, pending routine date, confirmation timestamp and last completed date. This is routine acknowledgement, never measured sleep. There is no history, tracking, AI call or new network dependency. AutoMigration 16→17 creates the empty table; existing imports and reports use the new schema version. Device-specific routine state is not added to the legacy JSON export format. **Alle lokale data wissen** disables and clears the routine.
- Daily timing uses `AlarmManager`, exact when special access is available, otherwise `setAndAllowWhileIdle`. A unique 15-minute WorkManager recovery job repairs scheduling after access revocation or transient failure. Boot, package replacement, clock/time-zone changes and exact-access broadcasts reconcile the saved state. Time changes recalculate a future inactive routine; an already pending routine remains pending.
- The first high-importance local notification asks the user to prepare for sleep. An unconfirmed routine repeats approximately every 15 minutes, requesting `FLAG_INSISTENT` on escalation. Only explicit in-app confirmation sets the timestamp and replaces escalation with a silent two-minute notification chronometer and visible screen status. Completion is derived from the persisted deadline and subsequently reconciled to the next future daily occurrence. Repeated confirmation is idempotent. An ignored routine stays pending until confirmation, disabling or changing its time.

## Platform limits

- `SCHEDULE_EXACT_ALARM` is optional special access, not `USE_EXACT_ALARM`. Denial retains inexact scheduling and visible corrective guidance. Notification permission, disabled application/channel notifications and a silent channel are surfaced independently.
- Android controls actual delivery, Doze/battery delays, notification cooldown, heads-up presentation, DND, volume and whether insistent audio is honored. The app neither bypasses these controls nor claims uninterruptible sound. Opening/dismissing a notification can stop its current sound; it does not confirm the routine. Force-stopping the app can prevent delivery until it is opened again. A powered-off phone cannot deliver an alarm.
- The channel uses Android's symbolic default alarm URI, with a default notification URI fallback if unavailable, and notification audio attributes. It cannot read arbitrary personal alarms from the user's Clock app. Users can change or silence the channel. There is no full-screen-intent permission or forced activity launch.
- Countdown status remains correct from its deadline while the app is away; background cleanup and next-alarm scheduling can occur later if Android delays execution. No physical-device audio/reliability or TalkBack/Switch Access certification is claimed from emulator evidence.

## Verification scope

All commands run locally from `TrainIQ-Project` with Android Studio JBR 25.0.2 as `JAVA_HOME`, the repository's Gradle daemon JVM 21 and Java 17 compilation toolchain, the installed SDK, and `ANDROID_SERIAL=emulator-5580` (agent-started existing `TrainIQ_Agent_API36_20260806`, Android 16/API 36, 360×640 dp, font scale 1.3). No AVD was created. No remote test service is used.

Baseline: existing nutrition and reminder JVM tests passed before implementation. The added barcode choice test failed on the missing control before its implementation. The initial new sleep test failed to compile because its new domain type did not yet exist; this is not a behavioral red-test claim. The complete JVM suite exposed one stale import schema-version constant introduced by the migration; it was corrected. Notification/UI test development also corrected missing test-only APIs, a receiver-persistence race in the fixture, and a scroll action on a non-scrollable menu. No valid test was removed or weakened.

Focused proof covers default-off/opt-in persistence, saved-product reselection without another scan, optional-save failure preserving the meal, all meal categories, barcode lookup failure/retry, scanner cancellation/restoration, existing duplicate-barcode transactions, Room reopening, the v2→current migration chain, sleep on/off and time changes, DST gap/overlap, midnight, repeated acknowledgement, two-minute boundaries, permission guidance, the real private receiver, notification escalation/privacy, notification→screen→confirmation and countdown.

The five-class connected run passed 71 tests. The migration marker passed separately with all 12 migration tests, including v2→current and 16→17. The initial combined marker/lint invocation hit the existing Gradle implicit-output-dependency validation; the documented separate marker command avoids that unrelated task-graph issue. No Gradle dependency wiring was changed to hide it.

Connected coverage command (the `class` property contains these five classes, comma separated):

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.features.nutrition.BarcodeMealFlowInstrumentedTest,com.trainiq.features.sleep.SleepRoutineInstrumentedTest,com.trainiq.features.sleep.SleepNotificationInstrumentedTest,com.trainiq.core.database.TrainIqDatabaseMigrationTest,com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest' --console=plain
.\gradlew.bat :app:generateDebugRoomMigrationChainVerificationMarker '-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.core.database.TrainIqDatabaseMigrationTest' --console=plain
```

The final screen review caught a stale-clock display of `2:01` immediately after confirmation. A new failing JVM regression test reproduced it; the display now clamps to two minutes. The final affected-surface command reruns the full unit suite, build/lint and four sleep instrumented tests; unaffected food/data/migration results above are retained:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.features.sleep.SleepRoutineInstrumentedTest,com.trainiq.features.sleep.SleepNotificationInstrumentedTest' --console=plain
```

Final results: **PASS** — debug build, **898 JVM tests (0 failures)**, **4/4 final sleep instrumented tests**, lint (**0 errors, 65 existing advisory warnings**, none in the new sleep files). Retained unaffected coverage: **71/71 connected tests**, plus **12/12 migration tests and the v2→17 verification marker**. The emulator crash buffer was empty. Final screenshots confirm the default-off/checked product control, accessible confirmation action, dark-mode colors and bounded countdown at font scale 1.3. No release, hosted CI, physical-device audio test, or full manual assistive-technology certification was performed.

Generated reports stay under `app/build/reports/` and `app/build/outputs/androidTest-results/`; selected local captures and retained reports stay under the ignored `.codex/device-qa/sleep-routine/` directory at repository root. Screenshots supplement Compose semantics assertions. Review found and corrected dark-mode text color inheritance using Material 3 `Surface`.

## Sources and publication

Official sources checked on 2026-09-08:

- [Android alarm scheduling and exact/inexact fallback](https://developer.android.com/develop/background-work/services/alarms)
- [Android 14 exact-alarm access](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms)
- [Notification flags, including insistent sound](https://developer.android.com/reference/android/app/Notification#FLAG_INSISTENT)
- [Default ringtone URIs](https://developer.android.com/reference/android/media/RingtoneManager#getDefaultUri(int))
- [Notification channel controls](https://developer.android.com/reference/android/app/NotificationChannel)
- [Full-screen intent restrictions](https://source.android.com/docs/core/permissions/fsi-limits)
- [GitHub workflow skipping](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/skip-workflow-runs)

The repository has an enabled `pull_request` GitHub Actions workflow, while `AGENTS.md` requires local-only verification. Publication uses `[skip ci]` on the PR head to avoid starting hosted jobs; skipped/pending remote checks are not passing evidence. The PR remains unmerged.
