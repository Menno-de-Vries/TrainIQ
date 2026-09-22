# Sleep alarm reliability and daily health entry points

Date: 2026-09-09. Base: `a5f5e339d78c60d590920fabe020b8952b76d2cf` (`origin/main`).

This supersedes the sleep timing/audio sections of `barcode-save-sleep-routine.md`.

## Diagnosis and behavior

The existing Android implementation targets API 26–36. There is no iOS target.

Before changing the scheduler, local regression checks found:

- Saving the same enabled time erased an active, unconfirmed routine and scheduled tomorrow. The domain regression failed with an empty `routineDay` and tomorrow's `nextAt`. Identical configuration is now idempotent; time-zone rebasing is a separate operation that preserves active routines.
- An already due but not yet activated routine could also be dropped by clock/time-zone rebasing. `clockChangeCannotDiscardAnAlreadyDueRoutine` failed before the fix; rebasing now activates overdue routines and only recalculates future inactive ones. This narrow time-change path has its own domain proof; the natural-time alarm-chain evidence below exercises unchanged paths.
- Native channel inspection and the instrumented regression found audio usage **10 (`USAGE_NOTIFICATION_EVENT`)**, rather than **4 (`USAGE_ALARM`)**. Choosing an alarm ringtone URI alone did not make it use the alarm stream.
- The next persisted alert was **900,000 ms** away; the new three-successor regression requires **600,000 ms**. The ordinary pending-state policy did not itself mark an ignored routine complete. These findings do not establish the exact cause on the user's physical phone without its configuration/runtime evidence.
- Inspection found notification delivery preceded saving/scheduling the successor. A delivery exception could abort that path. Successor persistence and OS scheduling now precede notification delivery. An injected notification-access failure against the real Room repository and scheduler verifies that three successive failures cannot erase the durable chain.

`SleepRepeatMillis` is the single ten-minute escalation interval, including UI copy. The unique WorkManager recovery job uses AndroidX's minimum periodic interval, **not** the escalation cadence. AlarmManager owns every initial/repeated warning. `setAlarmClock` is used when exact access is available, with `setAndAllowWhileIdle` fallback and visible permission guidance otherwise. One stable private broadcast PendingIntent replaces the previous alarm; disabling cancels it. No background audio loop/service or volume/DND mutation was added.

The new `trainiq_sleep_alarm_v2` channel uses alarm audio attributes. Migration retains the old channel's importance, sound (including null/mute), and vibration preference. An existing v2 channel is never reset. The old ID is retained only as a migration source. Notification ID/request code **2010** remains scoped to sleep; other reminder paths are unchanged.

The confirmed countdown uses notification key **(`sleep_countdown`, 2010)**. The playback service owns the untagged **2010** warning; Android's asynchronous foreground-service updates must not share the confirmed status key. Explicit cancellation and stale starts remove the service-owned warning, while an ordinary audio interruption still detaches and retains the unconfirmed warning. Cleanup cancels both status keys; queued cancelled starts retain their separate silent **2011** foreground acknowledgement. See the [2026-09-22 audit and handoff regression evidence](../qa/TrainIQ_App_Polish_2026-09-22.md). This changes notification ownership, not alarm timing, confirmation or persistence rules.

Every alarm is insistent and opens the private `SleepRoutineActivity` directly, including cold starts. Its explicit confirmation button shares the repository path with the in-app screen. Full-screen presentation is attached only when `canUseFullScreenIntent` permits it (API 34+); missing access has a system-settings action. The manifest declares `USE_FULL_SCREEN_INTENT` for this user-configured alarm. This is not a claim of automatic Play eligibility. The activity supports lock-screen presentation without dismissing device authentication.

Confirmation cancels the warning and replaces its successor with countdown completion. Room remains authoritative, and repeated confirmation preserves the original deadline. After completion the next local daily occurrence is scheduled. Boot, package replacement, exact-access restoration, clock/time-zone changes and app startup retain reconciliation. Force-stop still requires reopening the app. Time changes do not discard an already pending routine.

Start now has a **Dagelijkse gezondheid** card with **Slaap · alarm en bevestiging** and **Gewicht · bijhouden en voortgang**. The latter opens the existing typed Progress destination and existing body/weight functionality. No bottom tab, weight redesign, schema migration, dependency or nutrition/workout change was introduced.

## Local verification

Commands run from `TrainIQ-Project` with Android Studio JBR as `JAVA_HOME`, installed SDK, repository JVM/toolchains, and `ANDROID_SERIAL=emulator-5580` for connected checks. Generated evidence stays in ignored `.codex/device-qa/sleep-alarm-fix/` and `app/build/`.

Reproduction:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests 'com.trainiq.domain.sleep.SleepRoutinePolicyTest' --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.features.sleep.SleepAlarmRegressionTest,com.trainiq.features.sleep.SleepNotificationInstrumentedTest,com.trainiq.features.sleep.SleepRoutineInstrumentedTest' --console=plain
```

The unchanged-settings JVM test failed as intended; native regressions failed on usage 10 and 900,000 ms. The first existing AVD contained a signed release, so its signature mismatch was not resolved by uninstalling it. It was stopped and preserved. One new agent-owned AVD, `TrainIQ_Agent_Sleep_20260909`, was created using the installed API 36 image. Initial first-boot/install attempts were unsuccessful; boot completion was subsequently verified. Its default GPU run became unresponsive during the old notification test. Only its verified process was stopped, and the same AVD restarted with software graphics; no data wipe or image download occurred.

The updated notification test initially raced activity rendering and did not distinguish the first insistent alert from escalation. It now waits for the escalation title and actual Compose hierarchy; the real notification-to-screen-to-confirmation test then passed. No valid assertion was removed to hide a production failure.

Applicable checks:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.features.sleep.SleepAlarmRegressionTest,com.trainiq.features.sleep.SleepNotificationInstrumentedTest,com.trainiq.features.sleep.SleepRoutineInstrumentedTest,com.trainiq.features.FeatureRecoveryInstrumentedTest' --console=plain
```

Final JVM suite: **902 tests, zero failures/errors/skips**. Debug build and lint: **PASS**, lint **0 errors / 65 existing warnings**. Native coverage was retained per surface: six sleep state/audio/failure/permission tests passed in `connected.log`; the corrected real notification flow passed in `notification-green.log`; all four Home/Progress/recovery tests passed in `home-final-fixed.log`. The new Home fixture initially used a positional argument where `HealthConnectStatus` requires a named message; that test-only compilation error was corrected. A manual Home check caught the health actions initially being present only in the loading branch; the success branch and a no-profile Home regression now cover the actual normal entry points.

Manual runtime uses **360×640 dp, font scale 1.3**. Start → Weight opened the existing body/weight screen. Start → Sleep → enable → notification permission → exact-access settings → 23:01 proved the native settings routes. The app process was killed with `am kill` after moving Home; `pidof com.trainiq` was empty, and the display was put to sleep. The initial OS record was an exact alarm clock at **2026-09-08 23:01:00 GMT**. It cold-started the private confirmation activity, with a successor at **23:11:00.947 GMT**. AudioService recorded a SystemUI MediaPlayer **state:started**, **USAGE_ALARM**, **CONTENT_TYPE_SONIFICATION**, **mutedState:none**, routed to speaker device 2. This proves the emulator's actual playback pipeline started, not human-audible output on a physical phone.

A real reboot preserved the same **23:11:00.947** pending alarm after boot reconciliation, without opening TrainIQ. The second actual trigger scheduled **23:21:00.974**, and in **VIBRATE** mode AudioService again reported **state:started / USAGE_ALARM / mutedState:none**. Both light and dark confirmation screens were visually inspected; the primary confirmation action remained visible at font scale 1.3. Opening the real notification action reached this screen. Saving unchanged **23:01** in the actual time picker preserved **23:21:00.974**, rather than erasing the pending routine.

Full-screen access was then disabled through Android settings on this agent-only emulator. The sleep screen exposed the corresponding warning, and its **Alarmscherm toestaan** button opened the correct system screen. Exact-alarm access remained allowed. The device's own ringer selector was set to **SILENT** for the next alarm; the app never changes those settings.

The third real trigger occurred at approximately **23:21:02.242 GMT**, creating the next **23:31:02.242** alarm. In **SILENT** mode, AudioService again showed **state:started / USAGE_ALARM / mutedState:none**. With full-screen permission denied, the ordinary notification action still opened the confirmation screen. Confirmation at **23:21:53.512** removed the **23:31** escalation and replaced it with **23:23:53.512** countdown completion. The visible status counted down from 2:00, the notification had `showChronometer=true` and `chronometerCountDown=true`, and AudioService had no started player after confirmation.

With the app back in the background, countdown completion removed the notification and scheduled only **2026-09-09 23:01:00 GMT**. The natural-time sequence therefore covered two complete ten-minute intervals, process death, reboot, normal/vibrate/silent playback routing, allowed/denied full-screen presentation, real notification navigation, unchanged-time persistence, explicit confirmation, background countdown completion and next-day scheduling. Sound pipeline observations do not certify the user's physical speaker or OEM behavior.

Physical-phone audible playback in silent/vibrate mode, OEM battery policy, and manual TalkBack/Switch Access certification require device verification. Emulator notification flags, channel attributes and scheduling records alone are not audible-playback proof. Alarm volume zero, a muted channel, DND, denied special access and force-stop remain actual platform constraints. The app cannot read personal alarm selections from another Clock app.

Final strengthened native run (`final-connected.log`): **7/7 sleep tests, zero failures/errors/skips**, including real pending-OS-alarm counts across repeat, delivery failure, repeated reconciliation, confirmation, disable, re-enable and time replacement. Combined with the retained **4/4 Home/Progress tests**, this is **11 passing targeted native tests**. `final-baseline.log` records the successful final debug build, **902 JVM tests** and lint. The final crash buffer was empty. The agent-started emulator was stopped; the created `TrainIQ_Agent_Sleep_20260909` AVD and ignored local evidence are retained. The previous release AVD and both existing APK worktrees were preserved.

Git publication uses the task branch `codex/sleep-alarm-reliability`, base `main`, and a `[skip ci]` commit because hosted test execution is not authorized by the repository's local-only contract. No merge, release build/signing, upload or email is part of this task.

## Official references

Checked 2026-09-09:

- [AlarmManager and setAlarmClock](https://developer.android.com/reference/android/app/AlarmManager)
- [Alarm scheduling and exact access](https://developer.android.com/develop/background-work/services/alarms)
- [Notification channels and immutable behaviors](https://developer.android.com/develop/ui/compose/notifications/channels)
- [AudioAttributes.USAGE_ALARM](https://developer.android.com/reference/android/media/AudioAttributes#USAGE_ALARM)
- [Full-screen intent access and limits](https://source.android.com/docs/core/permissions/fsi-limits)
