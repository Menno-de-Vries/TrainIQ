# TrainIQ QA Findings To Improve

Audit date: 2026-05-09

Refresh date: 2026-05-10, current worktree

Scope: target-state blueprint, Android app source, Gradle/CI, docs, tests, emulator smoke, Health Connect, Gemini/AI, Room/data, UI/UX/accessibility, release readiness.

## 2026-07-10 Compact Health Connect Presentation

- Home now shows one concise Health Connect card with the displayed step count, friendly source label, last update time and refresh action. Raw, aggregate, query-window and parity diagnostics no longer compete with the primary dashboard information.
- Home renders either that compact sync card or the Health Connect permission/status card, never both in the same state.
- Settings now defaults to status, `N stappen via bron`, the compact last-checked time and necessary actions. All existing source, raw, aggregate, direct-SDK, parity, priority, workout-overlap and copy diagnostics remain available through the saveable `Technische details tonen` disclosure.
- This is presentation-only: Samsung official-raw selection, Health Connect reads, cache behavior, permissions and diagnostic values are unchanged. Focused Home/Settings/source-copy regressions pass, followed by the complete debug unit, assemble, lint, AndroidTest compile and Samsung parity build gate.
- Physical Samsung smoke: update install on SM-S931B succeeded without clearing data; a cold `com.trainiq/.MainActivity` launch returned `Status: ok` in 536 ms, the process remained alive and the crash buffer was empty. Final copy/layout and refreshed step parity remain for the user's visual check on-device.

## 2026-07-10 Samsung Health Official Raw Step Fallback

- physical-device evidence: at 01:38 on the user's Samsung, Samsung Health showed 84 steps while TrainIQ showed 5. TrainIQ's copied diagnosis exposed the exact mismatch: general Health Connect aggregate 5, Samsung-origin aggregate 5, official Samsung raw `StepsRecord` sum 84, and direct Samsung Health Data SDK unavailable with authorization error 2003.
- corrected authority: a successful direct Samsung Data SDK `TOTAL`, including zero, remains first. Otherwise the highest positive official Samsung raw/aggregate value is authoritative; the general Health Connect aggregate is only used when no usable Samsung-specific value exists. The reported `5/5/84` case therefore resolves to 84.
- source guard: raw authority is limited case-insensitively to exact `com.sec.android.app.shealth`. Packages merely containing `samsung` can remain diagnostic labels but cannot contribute raw counts or Samsung aggregate authority.
- cache/privacy behavior: no raw record IDs, timestamps, or record lists are newly persisted. A fresh Samsung raw/aggregate value cannot be overwritten by a stale direct-SDK cache, and cached selected Samsung scalars no longer invent raw or aggregate provenance when that provenance is unavailable.
- diagnostics: Settings explicitly identifies when the official Samsung Health raw fallback is displayed and keeps displayed, general aggregate, Samsung aggregate, Samsung raw and direct status separate for fresh reads.
- regression evidence: RED/GREEN coverage includes aggregate 5 versus official raw 84, official-package/lookalike filtering, direct `TOTAL` including zero, Samsung-specific priority over a higher general aggregate, fresh raw versus cached direct during aggregate failure, and stale provenance. The complete debug unit suite, `:app:assembleDebug`, `:app:lintDebug`, `:app:compileDebugAndroidTestKotlin`, and `:app:assembleSamsungHealthParityDebug` pass; SDK readiness reports `samsung-health-data-api-1.1.0.aar` ready. Independent re-review is clean.
- emulator runtime: final APK update install returned `Success`. Runtime launch evidence is partial, not a pass: the x86 emulator killed both cold and warm starts with `failed to complete startup` while logcat showed unusually slow class verification and 50% kernel CPU; there was no TrainIQ exception, but `cmd package compile -m speed -f com.trainiq` also exceeded the 244-second hard timeout. This emulator state is not accepted as product runtime proof.
- remaining acceptance: the emulator cannot validate Samsung Health parity. The user must install this candidate on the physical Samsung, open Samsung Health first, refresh TrainIQ, and confirm that TrainIQ now shows the same value as Samsung Health; Settings should say the official Samsung Health raw fallback is active while direct SDK error 2003 remains.

## 2026-07-09 Samsung Health Step Parity Source and Authority Fix

> Historical snapshot. The 2026-07-10 physical-device evidence supersedes this section's raw-diagnostic-only fallback policy.

- status: the local parity candidate is code-complete and regression-verified; exact Samsung Health All steps acceptance remains pending the user's own-phone comparison.
- source-of-truth note: this dated entry supersedes the older higher-Health-Connect/lower-direct selection rule described in the preserved untracked handoff; successful direct Samsung `TOTAL`, including zero, is now intentionally authoritative.
- root causes fixed: TrainIQ now recognizes Samsung Health's official `com.sec.android.app.shealth` package from one shared constant; a successful direct Samsung value could previously lose to a higher non-Samsung value; legacy display cache could override direct Samsung; aggregate failures could become a successful zero; and scalar day totals had no owning local date.
- parity behavior: every successful consented Samsung Health Data SDK `DataType.StepsType.TOTAL` read, including a valid zero, is authoritative for the displayed day total. If direct Samsung is unavailable, TrainIQ uses deduplication-aware Health Connect aggregates. Raw `StepsRecord` sums remain diagnostics only and can never win display selection or modern zero-cache mapping.
- resilience: nullable Samsung aggregate buckets contribute zero instead of aborting the direct read; non-numeric values still fail explicitly. Full and incremental sync share one local-day range, same-day transient aggregate failures preserve only same-day cache, a date rollover never reuses yesterday's scalar total, explicit Samsung permission loss never reuses a cached direct total, and unrelated change-token failures cannot masquerade as an aggregate failure.
- cache/UI behavior: legacy Gson mapping no longer lets an old display scalar override an available direct Samsung scalar; unknown-date legacy step scalars expire on the next sync instead of being presented as today, so no appdata clear is required. Fresh zero is shown as a measured `0` only with successful step evidence; missing permission/error remains `Geen data`. Reused display/direct cache is labeled stale in Settings and copied diagnostics.
- privacy/token hardening: raw step records are no longer mapped into the persisted cache and legacy raw rows are purged during normalization. Expired ChangesTokens now persist their full-sync replacement or an explicit removal tombstone, and revoked metric token updates cannot re-enter storage.
- tests: focused RED/PASS cycles cover official package classification, direct authority including zero, legacy Gson cache, nullable/non-numeric aggregate values, raw-diagnostic-only policy, aggregate failure isolation, permission loss, shared day range, same-day preservation, midnight rollover/outer fallback, cache provenance, token expiry/revocation, raw-record minimization, measured-zero state, and Home/Settings zero rendering. The complete `HealthConnectPermissionPolicyTest`, related mapper/Home/Settings/use-case tests, and the broad debug/unit/lint/Android-test compile gate pass.
- build/runtime evidence: `:app:assembleSamsungHealthParityDebug --no-configuration-cache` passes and `:app:checkSamsungHealthDataSdkReadiness` reports `samsung-health-data-api-1.1.0.aar` ready. Final emulator update install/cold launch passed with `Status: ok`, `TotalTime: 5289`, TrainIQ remained the resumed activity with process `11896`, Samsung Health was absent as expected, and the crash buffer was empty.
- external sources: Samsung documents `StepsType.TOTAL` as the phone-plus-wearables deduplicated total comparable to its Pedometer/All steps tracker; Android requires `StepsRecord.COUNT_TOTAL` aggregation for Health Connect cumulative-step fallback and warns against general raw-record summing because it can double count.
- remaining risk: the emulator cannot execute Samsung Health Data SDK. Final acceptance requires the user to grant `Samsung toegang geven`, refresh immediately after Samsung Health Sync now, and compare Samsung Health All steps with TrainIQ plus the copied Settings diagnosis on the physical Samsung. Raw Samsung Health Connect sums are diagnostic evidence only and are never final parity proof.

## 2026-06-21 Samsung Health Step Display Follow-Up

> Historical snapshot. The 2026-07-09 entry supersedes this section's raw-record fallback and positive-only direct-value behavior: raw records are now diagnostics only, and a successful direct Samsung zero is authoritative.

- status: partially-done for making TrainIQ's displayed daily step count use the highest Health Connect-visible daily total while preserving Health Connect best practices; physical Samsung Health parity still needs a retest on the user's device.
- files changed: Health Connect step sync now keeps the general `StepsRecord.COUNT_TOTAL` aggregate, additionally reads Samsung Health-origin `StepsRecord.COUNT_TOTAL` via `AggregateRequest.dataOriginFilter`, stores both diagnostic values plus the chosen display value, and maps Home/Settings step totals through a small resolver that never lets a lower Samsung-origin export hide a higher Health Connect aggregate. TrainIQ discovers Samsung package origins from Health Connect records and also tries the public Samsung Health package `com.sec.android.app.shealth`, but only accepts positive Samsung values so an empty Samsung-origin read cannot zero out the dashboard. The main manifest now also declares `com.sec.android.app.shealth` in package visibility queries so Android package visibility does not hide Samsung Health from the Samsung-specific parity path. If the Samsung aggregate under-reports while Samsung-origin raw `StepsRecord` exports contain a higher total, TrainIQ now uses that higher Samsung raw export as a compatibility fallback for Samsung Health matching and makes that visible in Settings. If multiple Samsung package origins are seen, TrainIQ reads each origin independently, ignores per-origin failures, and uses the highest positive Samsung-origin aggregate instead of summing package aggregates, avoiding double counting from the same provider family. Home now names whether the refreshed display value is `Samsung Health-stappen` or `Health Connect-stappen`, and stale Home copy points to Samsung Health `Sync now` instead of a vague cloud-sync route. Settings exposes the fuller Samsung comparison next to source, query-window, exact displayed/Health Connect/Samsung aggregate/Samsung raw/direct values, Samsung source recency timing, Health Connect visible step count, and workout-overlap diagnostics, plus a copy action for the Samsung steps diagnostic so physical-device mismatch evidence can be shared without retyping. `SamsungHealthDirectStepsDataSource` now contains a compile-safe reflection adapter for Samsung Health Data SDK: without the AAR it returns an explicit unavailable status, with the AAR it checks Samsung `DataTypes.STEPS` read permission and reads `DataType.StepsType.TOTAL` over the same local-day range through `HealthDataStore.aggregateData`. The adapter now resolves `DataType.StepsType.TOTAL.requestBuilder` defensively across static-field, static-getter, and Kotlin companion-property bytecode shapes and returns explicit diagnostic failures if the SDK surface differs from Samsung's docs. It also supports Samsung's documented grouped aggregate request shape by falling back from `setLocalTimeFilter(...)` to `setLocalTimeFilterWithGroup(LocalTimeFilter.of(...), LocalTimeGroup.of(LocalTimeGroupUnit.MINUTELY, 30))` before building the total request. It also classifies Samsung's documented `ResolvablePlatformException`, `AuthorizationException`, `InvalidRequestException`, `PlatformInternalException`, and `HealthDataException` into user-facing Settings statuses so physical-device parity failures identify whether Samsung Health needs a resolvable action, permission repair, request repair, or platform retry. When the Settings Samsung permission action receives a resolvable Samsung Health platform exception, TrainIQ now invokes Samsung's `resolve(activity)` flow reflectively and asks the user to finish that Samsung action before refreshing TrainIQ. Settings now also exposes a Samsung Health step-permission action behind `SAMSUNG_HEALTH_DATA_SDK_AAR_PRESENT`, calls Samsung `requestPermissions` for the missing steps permission, then refreshes Health Connect/Samsung diagnostics so the direct SDK total can be used immediately after consent. The display resolver and diagnostics prefer that direct value when it is positive, Gradle includes `app/libs/*.aar`, and the BuildConfig flag prevents hard SDK imports from breaking normal builds. `scripts/collect-samsung-step-parity-evidence.ps1` captures the remaining physical-device proof path: Samsung AAR presence, physical device metadata, Samsung Health package state, TrainIQ launch/crash evidence, and manual three-point Samsung Health All steps versus TrainIQ comparison placeholders. `docs/qa/samsung-health-step-parity-acceptance-2026-06-22.md` records the physical-device acceptance path and Samsung Health Data SDK requirements. Physical-device evidence showed a representative Samsung Health 600 versus TrainIQ 180 mismatch that persisted during the day, so the fallback now treats aggregate under-reporting as a real compatibility path rather than only a stale-sync explanation.
- implementation hardening: the Samsung direct adapter now resolves `DataTypes.STEPS`, `AccessType.READ`, and `Permission.of(...)` across static field, static getter, and Kotlin companion property/function bytecode shapes, matching the defensive `DataType.StepsType.TOTAL` handling so a real Samsung SDK binary does not silently fall back to lower Health Connect-only totals because the permission lookup failed before the direct steps read.
- implementation hardening: the Samsung aggregate response reader now sums the documented `dataList` item values through method-or-field lookup and reports explicit SDK-surface diagnostics when `dataList`, `value`, or a numeric value is missing, instead of quietly returning zero and leaving TrainIQ visibly below Samsung Health.
- implementation hardening: the Samsung Health Data SDK `TOTAL` request now prefers Samsung's documented grouped aggregate shape, `setLocalTimeFilterWithGroup(LocalTimeFilter, LocalTimeGroup)`, before falling back to an ungrouped local-time filter, reducing the risk that a physical Samsung direct read under-reports compared with Samsung Health All steps while keeping the direct read available if a Samsung SDK binary exposes only the ungrouped local-time setter.
- implementation hardening: the Samsung Health Data SDK grouped `TOTAL` request now uses Samsung's documented `LocalTimeGroupUnit.HOURLY, 1` shape first and keeps the previous `MINUTELY, 30` group only as a compatibility fallback, aligning the direct All steps read with Samsung's Pedometer tracker verification example.
- implementation hardening: the Samsung Health Data SDK grouped `TOTAL` request now also applies Samsung's documented `Ordering.ASC` before `build()` when that SDK surface is available, while staying fallback-safe if the ordering class or setter is absent.
- implementation hardening: direct Samsung Health Data SDK steps are now persisted as `samsungHealthDirectStepsToday` in the Health Connect cache and included in domain step mapping plus cached failure diagnostics, so a successful direct Samsung All steps read cannot live only in transient diagnostics and later fall back to a lower Health Connect-visible value.
- implementation hardening: Home fresh-step diagnostics now surface the same `Pariteit:` gap that Settings exposes when the direct Samsung Health Data SDK source is not active, so a phone showing a low TrainIQ value immediately points to the missing Samsung Data SDK API AAR / direct All steps route instead of only saying the lower Health Connect-visible value was refreshed.
- project readiness: Gradle now exposes `:app:checkSamsungHealthDataSdkReadiness` and an opt-in `-Ptrainiq.requireSamsungHealthDataSdk=true` build gate for debug/profileable Samsung parity builds, preventing another physical Samsung Health All steps comparison build from being installed without `samsung-health-data-api*.aar` in `app/libs`.
- project readiness: `scripts/install-samsung-health-data-sdk-aar.ps1` now installs a user-supplied Samsung Health Data SDK API AAR from an exact `.aar`, SDK folder, or SDK zip into `app/libs`, rejects legacy/non-API Samsung Health AARs, records SHA-256/status evidence, and points to the readiness and parity-build commands.
- project readiness: the same install helper now has `-HelpSamsungDownload`, prints Samsung's official Health Data SDK overview/download page, steps codelab, and release-note URLs, and fails fast with `SourcePath is required unless -HelpSamsungDownload is used.` when no local AAR/folder/zip is supplied. The overview page exposes Samsung's SDK download behind SDK terms, so the helper intentionally keeps a user-supplied local source flow instead of bypassing Samsung's download/terms flow.
- project readiness: `scripts/build-samsung-step-parity-debug.ps1` now provides a single physical Samsung parity path: optionally install a user-supplied `samsung-health-data-api*.aar`, run `:app:checkSamsungHealthDataSdkReadiness`, require one selected physical Samsung Android 10+ target, reject emulators through `ro.kernel.qemu`, verify `com.sec.android.app.shealth` with Samsung Health 6.30.2+, build/install with `-Ptrainiq.requireSamsungHealthDataSdk=true`, and then collect the standard Samsung step-parity evidence packet.
- project readiness: the app module now exposes explicit `:app:assembleSamsungHealthParityDebug` and `:app:installSamsungHealthParityDebug` tasks, both gated by `:app:checkSamsungHealthDataSdkReadiness`, so the physical Samsung Health All steps test does not rely on a normal debug build that can still be Health Connect-only.
- project readiness: because Samsung Health Data SDK API AAR v1.1.0 declares `minSdkVersion 29`, the app manifest now uses a focused `tools:overrideLibrary="com.samsung.android.sdk.health.data"` while the direct Samsung datasource hard-gates all Samsung SDK class-loading on Android 10/API 29+. This keeps TrainIQ's normal `minSdk 26` support while preventing the direct Samsung path from running on unsupported Android runtimes.
- implementation hardening: the Samsung permission check/request path now parses both direct permission sets and Samsung's documented granted-permissions wrapper shape, falls back to a fresh granted-permissions read after `requestPermissions` when the request result is not a set, and wraps the Settings Samsung access action so unexpected Samsung SDK/runtime failures become a snackbar/status instead of an uncaught app crash.
- implementation hardening: the Samsung Health Data SDK suspend bridge now forwards success/failure into the original downstream continuation instead of using unqualified `::resume` / `::resumeWithException` on the wrapper continuation. Physical SM-S931B launch evidence reproduced the pre-fix `StackOverflowError` at `SamsungHealthDirectStepsDataSource.kt:447`, and the focused regression guard now rejects that recursive callable-reference shape.
- physical-device follow-up: the user's latest phone retest still showed TrainIQ under-reporting versus Samsung Health through the day. Local inspection on 2026-06-22 found no `TrainIQ-Project/app/libs` directory and no `samsung-health-data-api*.aar` in the repo or Downloads, so exact Samsung Health All steps parity remains unproven and blocked by the missing Samsung Health Data SDK API AAR plus a fresh physical Samsung comparison.
- project readiness: the app now follows Samsung Health Data SDK migration readiness by applying Kotlin Parcelize, declaring Gson directly, adding `com.samsung.android.sdk.health.data.MIGRATION_COMPLETED=true` manifest metadata, and guarding against legacy Samsung Health SDK permission metadata returning.
- project readiness: the Samsung SDK readiness flag and evidence helper now only count `samsung-health-data-api*.aar` as direct Data SDK-ready, preventing a legacy `samsung-health-data*.aar` from falsely enabling the direct Samsung steps path.
- diagnostics: the in-app Samsung direct status now names the exact missing `samsung-health-data-api*.aar` file pattern, so Settings and copied diagnostics do not imply that any legacy Samsung AAR can enable direct All steps parity.
- diagnostics: the app now also exposes a separate `SAMSUNG_HEALTH_NON_API_AAR_PRESENT` build flag and status for the case where a Samsung Health AAR exists but is not the required Data SDK API AAR, so physical parity runs can distinguish a missing SDK from a wrong SDK file.
- diagnostics: Samsung AAR filename detection is now case-insensitive for the `.aar` extension in both BuildConfig and the parity evidence helper, preventing `.AAR` bundles from being missed during physical-device setup.
- diagnostics: the parity evidence helper now writes `samsung-health-readiness.txt` with Samsung Health install status and `versionName`/`versionCode` evidence, so physical-device runs can verify Samsung's 6.30.2+ Data SDK runtime requirement before comparing Samsung Health All steps to TrainIQ.
- diagnostics: the in-app Samsung direct status now also reports whether Samsung Health is installed and whether its visible version meets Samsung's 6.30.2+ Data SDK runtime requirement, so Settings can explain a physical-device mismatch even before the direct All steps read succeeds.
- diagnostics: the Samsung Health version readiness comparison is now behavior-tested against the 6.30.2 minimum, including lower, exact, patch-suffixed, higher, and unknown version names.
- diagnostics: when the Samsung Health Data SDK API AAR is present, TrainIQ now checks Samsung Health runtime readiness before calling Samsung `HealthDataService`; missing or too-old Samsung Health returns an immediate actionable Settings status instead of a lower-level SDK failure, while unknown version text remains non-blocking.
- diagnostics: the full Samsung Health runtime-readiness decision is now behavior-tested, including missing Samsung Health, below-minimum runtime, exact-minimum runtime, and unknown version text.
- diagnostics: the parity evidence helper now writes `parity-result.txt` and classifies captured Samsung Health versus TrainIQ values as `MATCH`, `TRAINIQ_UNDER_REPORTS`, `TRAINIQ_OVER_REPORTS`, or `NOT_CAPTURED`, including absolute difference and TrainIQ percent of Samsung Health.
- diagnostics: the copied Settings step diagnostic now includes a `Pariteit:` line explaining whether direct Samsung Health Data SDK parity is available, blocked by missing AAR, blocked by missing Samsung permission, blocked by missing/old Samsung Health runtime, or still required after Health Connect-visible Samsung export remains lower.
- diagnostics: the Settings Health Connect section now shows the same `Pariteit:` Samsung gap summary inline, so physical-device testing can see the active mismatch cause without first copying the diagnostic text.
- diagnostics: Home and Settings diagnostics now label a displayed direct Samsung Health Data SDK step value as direct Samsung data instead of generic Health Connect data, and Home now says `Live uit Samsung Health` for that winning direct source, so a successful All steps direct read is not mistaken for the lower Health Connect aggregate during physical-device parity testing.
- diagnostics: the parity evidence helper now writes `device-readiness.txt` with manufacturer/model/Android version, Android 10+ readiness, `ro.kernel.qemu`, Samsung manufacturer detection, emulator detection, physical Samsung likelihood, combined Samsung Data SDK runtime target readiness, and Samsung's no-emulator Data SDK limitation.
- diagnostics: Settings and the copied Samsung step diagnostic now also explain Health Connect App priorities when multiple step sources are visible, because Android documents that Activity/Steps aggregates are deduplicated according to the user's Health Connect app-priority order. This gives the physical-phone mismatch test one more source-backed check before relying solely on the direct Samsung Health Data SDK path.
- diagnostics: when multiple Health Connect step sources are visible, Settings now shows a focused `Prioriteiten openen` action next to the priority explanation. It reuses the official Health Connect settings intent already used by TrainIQ, avoiding an undocumented deep link while making the physical-phone parity check one tap faster.
- project readiness: the Samsung Data SDK readiness guard now explicitly checks that the app module keeps Java 17 source/target compatibility and Kotlin JVM toolchain 17, matching Samsung's SDK requirement.
- diagnostics: the parity evidence helper now writes `acceptance-gates.txt`, which combines physical Samsung Android 10+ readiness, Samsung Data SDK API AAR presence, Samsung Health install status, Samsung Health 6.30.2+ readiness, captured step values, and value equality into `Exact Samsung Health All steps parity proof ready`.
- verification evidence:
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --tests "com.trainiq.features.settings.SettingsUiStateTest" --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --tests "com.trainiq.features.home.HomeDashboardRefreshTest" --tests "com.trainiq.features.settings.SettingsUiStateTest" --console=plain --no-configuration-cache`.
  - PASS: after the higher-visible-total policy update, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --tests "com.trainiq.features.home.HomeDashboardRefreshTest" --tests "com.trainiq.features.settings.SettingsUiStateTest" --console=plain --no-configuration-cache`.
  - PASS: after the Samsung Health Data SDK readiness guard and `Sync now` Home-copy guard, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --tests "com.trainiq.features.home.HomeDashboardRefreshTest" --tests "com.trainiq.features.settings.SettingsUiStateTest" --console=plain --no-configuration-cache`.
  - PASS: after adding the exact Settings step-value snapshot, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --tests "com.trainiq.features.home.HomeDashboardRefreshTest" --tests "com.trainiq.features.settings.SettingsUiStateTest" --console=plain --no-configuration-cache`.
  - PASS: after adding Samsung source recency timing, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --tests "com.trainiq.features.home.HomeDashboardRefreshTest" --tests "com.trainiq.features.settings.SettingsUiStateTest" --console=plain --no-configuration-cache`.
  - PASS: after adding the copyable Samsung steps diagnostic, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --tests "com.trainiq.features.home.HomeDashboardRefreshTest" --tests "com.trainiq.features.settings.SettingsUiStateTest" --console=plain --no-configuration-cache`.
  - PASS: after adding the direct Samsung Health Data SDK seam, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --tests "com.trainiq.features.home.HomeDashboardRefreshTest" --tests "com.trainiq.features.settings.SettingsUiStateTest" --console=plain --no-configuration-cache`.
  - PASS: after adding optional `app/libs/*.aar` SDK wiring, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --tests "com.trainiq.features.home.HomeDashboardRefreshTest" --tests "com.trainiq.features.settings.SettingsUiStateTest" --console=plain --no-configuration-cache`.
  - PASS: after adding the Samsung Health Data SDK AAR BuildConfig readiness flag, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --tests "com.trainiq.features.home.HomeDashboardRefreshTest" --tests "com.trainiq.features.settings.SettingsUiStateTest" --console=plain --no-configuration-cache`.
  - PASS: after aligning the default Samsung direct status text, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --tests "com.trainiq.features.home.HomeDashboardRefreshTest" --tests "com.trainiq.features.settings.SettingsUiStateTest" --console=plain --no-configuration-cache`.
  - PASS: after implementing the compile-safe Samsung Health Data SDK reflection adapter, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after wiring the Samsung Health Data SDK step-permission action into Settings, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --tests "com.trainiq.features.settings.SettingsUiStateTest" --console=plain --no-configuration-cache`.
  - PASS: after adding the repeatable Samsung step-parity evidence helper, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after hardening the Samsung Health Data SDK reflection adapter for `DataType.StepsType.TOTAL.requestBuilder`, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after adding Samsung Health package visibility, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.core.health.HealthConnectReadPermissionsTest" --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after adding Samsung Health Data SDK exception-specific diagnostics, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after adding Samsung resolvable-platform action launch from the Settings Samsung permission path, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after adding the Samsung documented grouped aggregate-request fallback, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after hardening `DataTypes.STEPS` reflection lookup, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after hardening Samsung `Permission.of(DataTypes.STEPS, AccessType.READ)` reflection lookup, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after hardening Samsung aggregate response `dataList` / `value` parsing, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after making the Samsung direct `TOTAL` request prefer the documented grouped aggregate shape, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after making the grouped Samsung `TOTAL` request fallback-safe for SDK binaries that only expose the ungrouped setter, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after aligning the Samsung grouped `TOTAL` request with Samsung's documented `LocalTimeGroupUnit.HOURLY, 1` Pedometer verification example, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after adding optional Samsung `Ordering.ASC` to the direct grouped `TOTAL` request, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after Samsung Health Data SDK project-readiness metadata/dependency/plugin update, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.core.health.HealthConnectReadPermissionsTest" --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`; the first run timed out at the shell after plugin resolution changed, and the immediate rerun passed.
  - PASS: after narrowing Samsung AAR readiness to `samsung-health-data-api*.aar`, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after making the in-app missing-AAR status name `samsung-health-data-api*.aar`, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after adding the non-API Samsung AAR diagnostic flag/status, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after making Samsung `.aar` extension detection case-insensitive in BuildConfig and the parity evidence helper, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after adding Samsung Health runtime-version evidence to the parity helper, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after adding Samsung Health runtime-version evidence to the parity helper, `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\collect-samsung-step-parity-evidence.ps1 -OutputDir $env:TEMP\trainiq-samsung-step-parity-readiness-smoke` produced `samsung-health-readiness.txt`; current emulator correctly reported Samsung Health not installed and `versionName=(not found)`.
  - PASS: after adding Samsung Health runtime-version evidence to the parity helper, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after adding in-app Samsung Health runtime readiness to the Samsung direct status, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after adding in-app Samsung Health runtime readiness to the Samsung direct status, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after behavior-testing Samsung Health version readiness against the 6.30.2+ Data SDK minimum, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after behavior-testing Samsung Health version readiness against the 6.30.2+ Data SDK minimum, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after gating direct Samsung SDK reads and permission requests on Samsung Health runtime readiness, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after gating direct Samsung SDK reads and permission requests on Samsung Health runtime readiness, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after extracting and behavior-testing the full Samsung Health runtime-readiness decision, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after extracting and behavior-testing the full Samsung Health runtime-readiness decision, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after adding scripted Samsung/TrainIQ parity classification, `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\collect-samsung-step-parity-evidence.ps1 -OutputDir $env:TEMP\trainiq-samsung-step-parity-mismatch-smoke -SamsungHealthAllSteps 600 -TrainIqDisplayedSteps 180` produced `parity-result.txt` with difference `420`, TrainIQ percent `30%`, and status `TRAINIQ_UNDER_REPORTS`.
  - FAIL/PASS: physical SM-S931B launch reproduced a Samsung Device Care app-error dialog and `java.lang.StackOverflowError` in the Samsung Health direct suspend bridge before the continuation fix; after the fix, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache` passed.
  - PASS: after the continuation forwarding fix, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache` passed.
  - PASS: physical SM-S931B cold launch after the fix returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 569`, `TrainIQVisible=True`, `AppErrorVisible=False`, `CrashBufferHasTrainIQFatal=False`, and a quick `/sdcard` scan found no TrainIQ-named test files left on the phone.
  - PASS: after adding scripted Samsung/TrainIQ parity classification, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after adding scripted Samsung/TrainIQ parity classification, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after adding the copied Settings `Pariteit:` diagnostic line for direct Samsung parity gap causes, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after adding the copied Settings `Pariteit:` diagnostic line for direct Samsung parity gap causes, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after showing the Samsung `Pariteit:` gap summary inline in Settings, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.settings.SettingsUiStateTest" --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after showing the Samsung `Pariteit:` gap summary inline in Settings, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after labeling direct Samsung Health Data SDK step values correctly in Home and diagnostics, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.home.HomeDashboardRefreshTest" --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after making Home's live source prefix say `Samsung Health` when the direct Samsung Data SDK value wins, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.home.HomeDashboardRefreshTest" --console=plain --no-configuration-cache`.
  - PASS: after adding physical Samsung/emulator readiness output to the parity helper, `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\collect-samsung-step-parity-evidence.ps1 -OutputDir $env:TEMP\trainiq-samsung-step-parity-device-readiness-smoke -SamsungHealthAllSteps 600 -TrainIqDisplayedSteps 180` produced `device-readiness.txt` marking the current Google `sdk_gphone64_x86_64` target as emulator detected and `Physical Samsung device likely: False`.
  - PASS: after adding physical Samsung/emulator readiness output to the parity helper, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after adding physical Samsung/emulator readiness output to the parity helper, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after adding Android 10+ runtime readiness to `device-readiness.txt`, `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\collect-samsung-step-parity-evidence.ps1 -OutputDir $env:TEMP\trainiq-samsung-step-parity-android-runtime-smoke -SamsungHealthAllSteps 600 -TrainIqDisplayedSteps 180` reported Android `16`, `Android 10 or later: True`, emulator detected, and `Device meets Samsung Health Data SDK runtime target: False`.
  - PASS: after adding Android 10+ runtime readiness to `device-readiness.txt`, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after adding Android 10+ runtime readiness to `device-readiness.txt`, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after guarding Java 17 Samsung Data SDK readiness, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after guarding Java 17 Samsung Data SDK readiness, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after adding exact-parity acceptance gates to the evidence helper, `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\collect-samsung-step-parity-evidence.ps1 -OutputDir $env:TEMP\trainiq-samsung-step-parity-acceptance-gates-smoke -SamsungHealthAllSteps 600 -TrainIqDisplayedSteps 180` produced `acceptance-gates.txt` with captured values true, matching values false, and `Exact Samsung Health All steps parity proof ready: False`.
  - PASS: after adding exact-parity acceptance gates to the evidence helper, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after adding exact-parity acceptance gates to the evidence helper, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after wiring Samsung Health 6.30.2+ into the exact-parity acceptance gate, `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\collect-samsung-step-parity-evidence.ps1 -OutputDir $env:TEMP\trainiq-samsung-step-parity-health-version-gate-smoke -SamsungHealthAllSteps 600 -TrainIqDisplayedSteps 180` produced `acceptance-gates.txt` with `Samsung Health version 6.30.2 or later: False` and `Exact Samsung Health All steps parity proof ready: False` on the current non-Samsung emulator.
  - PASS: after making Samsung `.aar` extension detection case-insensitive, `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\collect-samsung-step-parity-evidence.ps1 -OutputDir $env:TEMP\trainiq-samsung-step-parity-aar-case-smoke` reported no local Data SDK API AAR and no legacy/other Samsung Health AARs with clean status formatting.
  - PASS: after narrowing Samsung AAR readiness to `samsung-health-data-api*.aar`, `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\collect-samsung-step-parity-evidence.ps1 -OutputDir $env:TEMP\trainiq-samsung-step-parity-aar-detection-smoke` reported no Data SDK API AAR and no legacy/other Samsung Health AARs.
  - PASS: after direct Samsung cache hardening, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after Home parity-gap diagnostics and the opt-in Samsung SDK readiness gate, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.home.HomeDashboardRefreshTest" --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after adding Health Connect App priorities diagnostics for multiple visible step sources, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --tests "com.trainiq.features.settings.SettingsUiStateTest" --console=plain --no-configuration-cache`.
  - PASS: after the same diagnostics update, broad gate `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: emulator debug install/cold launch on `emulator-5554` returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 8266`, with no filtered AndroidRuntime crash output.
  - PASS: after adding the focused `Prioriteiten openen` Health Connect settings action, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.settings.SettingsUiStateTest" --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: after the same action, broad gate `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: emulator debug install/cold launch on `emulator-5554` returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 7903`, with no filtered AndroidRuntime crash output.
  - EXPECTED FAIL: `./gradlew.bat :app:assembleSamsungHealthParityDebug --console=plain --no-configuration-cache` failed at `:app:checkSamsungHealthDataSdkReadiness` with `Samsung Health Data SDK API AAR missing`, proving the parity-only task cannot be mistaken for a Health Connect-only build while the AAR is absent.
  - PASS: after adding explicit Samsung parity Gradle tasks and updating the physical parity helper to use them, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --tests "com.trainiq.features.settings.SettingsUiStateTest" --console=plain --no-configuration-cache`.
  - PASS: after the same parity-task hardening, broad gate `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after the same parity-task hardening, emulator debug install/cold launch on `emulator-5554` returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 9424`, with no filtered AndroidRuntime crash output.
  - EXPECTED FAIL: `./gradlew.bat :app:checkSamsungHealthDataSdkReadiness --console=plain --no-configuration-cache` failed with `Samsung Health Data SDK API AAR missing... app\libs`, confirming this checkout is not yet capable of exact Samsung Health All steps parity builds.
  - PASS: `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\install-samsung-health-data-sdk-aar.ps1 -HelpSamsungDownload` printed official Samsung SDK download/codelab/release-note guidance and exited successfully.
  - EXPECTED FAIL: `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\install-samsung-health-data-sdk-aar.ps1` printed the same guidance and failed with `SourcePath is required unless -HelpSamsungDownload is used.`, confirming the missing-AAR state is explicit.
  - PASS: `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\install-samsung-health-data-sdk-aar.ps1 -SourcePath <temp-dummy-source> -DestinationDir <temp-dummy-dest>` installed a dummy `samsung-health-data-api-1.1.0.aar` into a temporary destination and wrote `samsung-health-data-sdk-aar-status.txt` with SHA-256 plus next readiness/parity commands.
  - PASS: after adding the official Samsung SDK overview/download page to the AAR helper, `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\install-samsung-health-data-sdk-aar.ps1 -HelpSamsungDownload` printed the SDK download, codelab, and release-note URLs plus the SDK-terms reminder.
  - PASS: after the same helper update, targeted Samsung/Settings tests passed with `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --tests "com.trainiq.features.settings.SettingsUiStateTest" --console=plain --no-configuration-cache`.
  - PASS: after the same helper update, broad gate passed with `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after the same helper update, `.\gradlew.bat :app:installDebug --console=plain --no-configuration-cache` installed on emulator `emulator-5554`; cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 6255`, and filtered logcat showed no `AndroidRuntime`/fatal crash.
  - PASS: after fixing the Samsung AAR `minSdkVersion 29` manifest-merge failure with a focused Samsung override plus Android 10/API 29 runtime gate, targeted manifest/Samsung/Settings tests passed with `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.core.health.HealthConnectReadPermissionsTest" --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --tests "com.trainiq.features.settings.SettingsUiStateTest" --console=plain --no-configuration-cache`.
  - PASS: `powershell -NoProfile -ExecutionPolicy Bypass -File "D:\GitHub\TrainIQ\TrainIQ-Project\scripts\build-samsung-step-parity-debug.ps1" -AdbPath "C:\Users\menno\AppData\Local\Android\Sdk\platform-tools\adb.exe" -Serial "adb-RFCY60HNHNJ-Jf2gXF._adb-tls-connect._tcp"` completed successfully and wrote evidence to `.codex\device-qa\samsung-step-parity-build-2026-06-22-110239`.
  - PASS: that physical-device evidence identified `SM-S931B`, manufacturer `samsung`, Android `16`, `ro.kernel.qemu: 0`, Samsung Health installed, and Samsung Health `versionName=6.32.0.001`, satisfying the helper's physical Samsung Android 10+ and Samsung Health 6.30.2+ gates.
  - PASS: after the physical Samsung parity build/install succeeded, broad gate passed with `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after Samsung permission-result crash hardening, targeted Samsung/Settings tests passed with `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --tests "com.trainiq.features.settings.SettingsUiStateTest" --console=plain --no-configuration-cache`.
  - PASS: after the same crash hardening, broad gate passed with `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: emulator crash smoke installed the Samsung AAR-present debug build on `emulator-5554`, cold-launched `com.trainiq/.MainActivity` with `Status: ok`, reached Settings, displayed `Samsung Health runtime: app niet gevonden`, tapped `Samsung toegang geven`, kept the `com.trainiq` process alive, and left the crash buffer empty. Evidence: `.codex/device-qa/emulator-crash-smoke-2026-06-22/`.
  - PASS: after adding the Samsung Data SDK API AAR install helper, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS expected-fail physical parity helper smoke: `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-samsung-step-parity-debug.ps1 -SkipInstall` stopped before device install/build at `:app:checkSamsungHealthDataSdkReadiness` with `Samsung Health Data SDK API AAR readiness failed`, which is the intended guard while no `samsung-health-data-api*.aar` exists.
  - PASS: `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\collect-samsung-step-parity-evidence.ps1 -OutputDir $env:TEMP\trainiq-samsung-step-parity-script-smoke` executed on the current emulator environment, produced summary/manual comparison/diagnosis/crash files, and correctly reported no local Samsung Health Data SDK AAR.
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin :app:assembleProfileable :macrobenchmark:compileProfileableJavaWithJavac --console=plain --no-configuration-cache`.
  - PASS: after the higher-visible-total policy update, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after the Samsung Health Data SDK readiness guard and `Sync now` Home-copy guard, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after adding the exact Settings step-value snapshot, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after adding Samsung source recency timing, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after adding the copyable Samsung steps diagnostic, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after adding the direct Samsung Health Data SDK seam, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after adding optional `app/libs/*.aar` SDK wiring, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after adding the Samsung Health Data SDK AAR BuildConfig readiness flag, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after aligning the default Samsung direct status text, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after implementing the compile-safe Samsung Health Data SDK reflection adapter, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after wiring the Samsung Health Data SDK step-permission action into Settings, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after adding and smoke-testing the repeatable Samsung step-parity evidence helper, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - PASS: `git diff --check` returned only LF-to-CRLF warnings.
  - PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; emulator `emulator-5554` cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 5698`, and filtered AndroidRuntime crash buffer was empty.
  - PASS: after hardening the Samsung request-builder reflection, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after hardening the Samsung request-builder reflection, `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; emulator `emulator-5554` cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 5461`, and filtered AndroidRuntime crash buffer was empty.
  - PASS: after Samsung SDK exception-specific diagnostics, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after Samsung SDK exception-specific diagnostics, `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; emulator `emulator-5554` cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 6686`, and filtered AndroidRuntime crash buffer was empty.
  - PASS: after Samsung resolvable-platform action launch, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after Samsung resolvable-platform action launch, `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; emulator `emulator-5554` cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 5667`, and filtered AndroidRuntime crash buffer was empty.
  - PASS: after adding the Samsung documented grouped aggregate-request fallback, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after adding the Samsung documented grouped aggregate-request fallback, `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; emulator `emulator-5554` cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 5829`, and filtered AndroidRuntime crash buffer was empty.
  - PASS: after hardening `DataTypes.STEPS` reflection lookup, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after hardening `DataTypes.STEPS` reflection lookup, `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; emulator `emulator-5554` cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 5795`, and filtered AndroidRuntime crash buffer was empty.
  - PASS: after hardening Samsung `Permission.of(DataTypes.STEPS, AccessType.READ)` reflection lookup, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after hardening Samsung `Permission.of(DataTypes.STEPS, AccessType.READ)` reflection lookup, `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; emulator `emulator-5554` cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 6435`, and filtered AndroidRuntime crash buffer was empty.
  - PASS: after hardening Samsung aggregate response `dataList` / `value` parsing, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after hardening Samsung aggregate response `dataList` / `value` parsing, `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; emulator `emulator-5554` cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 5861`, and filtered AndroidRuntime crash buffer was empty.
  - PASS: after Samsung Health Data SDK project-readiness metadata/dependency/plugin update, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after Samsung Health Data SDK project-readiness metadata/dependency/plugin update, `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; emulator `emulator-5554` cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 5598`, and filtered AndroidRuntime crash buffer was empty.
  - PASS: after narrowing Samsung AAR readiness to `samsung-health-data-api*.aar`, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after narrowing Samsung AAR readiness to `samsung-health-data-api*.aar`, `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; emulator `emulator-5554` cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 6245`, and filtered AndroidRuntime crash buffer was empty.
  - PASS: after making the in-app missing-AAR status name `samsung-health-data-api*.aar`, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after making the in-app missing-AAR status name `samsung-health-data-api*.aar`, `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; emulator `emulator-5554` cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 6905`, and filtered AndroidRuntime crash buffer was empty.
  - PASS: after adding the non-API Samsung AAR diagnostic flag/status, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after adding the non-API Samsung AAR diagnostic flag/status, `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; emulator `emulator-5554` cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 6360`, and filtered AndroidRuntime crash buffer was empty.
  - PASS: after making Samsung `.aar` extension detection case-insensitive, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: after making Samsung `.aar` extension detection case-insensitive, `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; emulator `emulator-5554` cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 5637`, and filtered AndroidRuntime crash buffer was empty.
- external sources used:
  - Android Developers Health Connect read-data guidance: cumulative `StepsRecord` should use `aggregate()` instead of raw `readRecords()` summing to avoid double counting.
  - Android Developers aggregate-data guidance: `AggregateRequest` supports `StepsRecord.COUNT_TOTAL` and optional `dataOriginFilter` for app-origin-specific aggregates.
  - Samsung support: Samsung Health `All steps` is the combined step total for the phone and connected devices.
  - Google Play Samsung Health listing: Samsung Health's public Android package is `com.sec.android.app.shealth`.
  - Samsung Health Data SDK overview, app-module guide, permission-request guide, StepsType API, aggregate-data guide, codelab, and release notes: Samsung's current SDK can read Samsung Health steps from phone plus connected wearables, exposes `DataTypes.STEPS` / `DataType.StepsType.TOTAL`, uses the `samsung-health-data-api-1.1.0.aar` codelab path, needs Samsung Health runtime readiness/permission handling, and is not supported on emulators.
- remaining risk: if Samsung Health has not written its latest watch/phone total into Health Connect at all, TrainIQ can only show the highest Health Connect-visible aggregate or Samsung raw export; the UI now surfaces that mismatch path and keeps `Sync now` guidance. A full long-term match to Samsung Health's own graph requires Samsung Health Data SDK integration plus physical Samsung-device verification once the SDK AAR and Samsung developer-mode/partnership requirements are available.

## 2026-06-21 Active Workout Scroll + Samsung Step Diagnostic Follow-Up

- status: done for reducing the reported active strength-workout scroll stutter risk and clarifying Samsung Health step-count ambiguity; active workout scroll composition was reduced, profileable scroll benchmark coverage was added, and emulator runtime proof now covers the seeded active-workout up/down scroll path. Physical-device frame-timing remains a recommended follow-up for final performance certification.
- files changed: active workout cards now receive per-exercise state slices instead of the full `ActiveWorkoutUiState`; the active workout `LazyColumn` now declares stable content types for header, rest timer, exercise, superset, and debrief items; draft-only persistence no longer replays the whole active session into UI state on every keystroke; the profileable benchmark seed now creates a longer active workout and `TrainIqStartupBenchmark` includes up/down active workout scroll measurement; Health Connect step diagnostics now keep the daily `StepsRecord.COUNT_TOTAL` aggregate as the authoritative step count while separately reporting steps that fall inside Health Connect workout windows when workout permission/data is available; Settings explains that workout overlap is diagnostic and not subtracted.
- verification evidence:
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --tests "com.trainiq.features.settings.SettingsUiStateTest" --console=plain --no-configuration-cache`.
  - First after-change targeted run completed with `BUILD SUCCESSFUL` but the shell returned a timeout at 121 seconds after Gradle had finished.
  - After-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --tests "com.trainiq.features.settings.SettingsUiStateTest" --console=plain --no-configuration-cache`.
  - Broad PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin :app:assembleProfileable :macrobenchmark:compileProfileableJavaWithJavac --console=plain --no-configuration-cache`.
  - Broad JVM PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - Runtime PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; emulator `emulator-5554` launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 4698`, and filtered AndroidRuntime crash buffer was empty.
  - Profileable PASS: `./gradlew.bat :app:assembleProfileable :macrobenchmark:compileProfileableJavaWithJavac --console=plain --no-configuration-cache`.
  - Active-scroll benchmark PASS on emulator with emulator warning suppressed: `./gradlew.bat :macrobenchmark:compileProfileableJavaWithJavac :macrobenchmark:connectedProfileableAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.macrobenchmark.TrainIqStartupBenchmark#activeWorkoutScrollFrames" "-Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR" --console=plain --no-configuration-cache`.
- external sources used:
  - Android Developers Health Connect read-data guidance: cumulative steps should use aggregate reads to avoid duplicate data from multiple apps/devices.
  - Android Developers Health Connect aggregate-data guidance: `StepsRecord.COUNT_TOTAL` can be read with `AggregateRequest`.
  - Android Health Connect sample: exercise-session steps can be calculated by aggregating steps over the session time range.
  - Samsung support: automatic workout detection can start after 10 minutes, and Samsung Health `All steps` is a combined step total across connected devices/sources.
  - Android Developers Compose performance/lazy-list guidance: keep lazy list identity stable and verify jank with profileable or release-like measurement.
- remaining risk: emulator benchmark proof is useful as a regression guard but not a final performance certificate; confirm frame timing on a physical Samsung device if the user's exact device still stutters. Samsung Health workout-window overlap depends on granted Workout permission and the provider actually exposing exercise-session windows through Health Connect.

## 2026-06-20 Active Routine Scroll Performance Polish

- status: done for reducing active-routine card scroll jank by removing the generic wrapping action layout from the fixed two-button active routine action area.
- files changed: `ActiveRoutineCard` now delegates start/edit actions to a lightweight `ActiveRoutineActionRow` with stable equal-width buttons and a single computed start label; workout start and routine detail callbacks are unchanged.
- verification evidence:
  - RED: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest.activeRoutineCardUsesStableActionRowForScrollPerformance" --console=plain --no-configuration-cache` failed before implementation because the active routine card still used `WrappingActionRow`.
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest.activeRoutineCardUsesStableActionRowForScrollPerformance" --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --tests "com.trainiq.core.ui.ActionButtonLayoutPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; emulator `emulator-5554` cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 2797`, and filtered AndroidRuntime crash buffer was empty.
- external sources used: none; local source and the reported scroll behavior were sufficient.
- remaining risk: install/launch proof confirms no crash; real frame timing can still vary by device and seeded data size.

## 2026-06-20 Nutrition AI Header Action Full-Width Follow-Up

- status: done for making the single third Foto/AI action in Producten and Recepten span the full row width instead of sitting as a half-width orphan below the first button.
- files changed: shared `EqualNutritionHeaderActions` now keeps paired rows as equal 50/50 cells but renders a single final row item with `Modifier.fillMaxWidth()` and no spacer; Producten and Recepten inherit the same layout.
- verification evidence:
  - RED: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.NutritionInputValidationTest.nutritionLibraryHeaderActionsUseEqualWidthCells" --console=plain --no-configuration-cache` failed before implementation because the helper still used a spacer for odd rows.
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.NutritionInputValidationTest.nutritionLibraryHeaderActionsUseEqualWidthCells" --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.NutritionInputValidationTest" --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; emulator `emulator-5554` cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 2446`, and filtered AndroidRuntime crash buffer was empty.
- external sources used: none; local screenshot/request context and source guards were sufficient.
- remaining risk: install/launch proof confirms no crash; runtime visual proof on the exact seeded Producten/Recepten state remains optional because the shared source guard now protects both tabs.

## 2026-06-20 Training/Nutrition Action Alignment Polish

- status: done for small visual alignment polish in Training routine actions, Producten/Recepten header actions, and expanded Nutrition history details without changing persistence, AI routing, scanner routing, meal reuse/delete behavior, or workout start behavior.
- files changed: inactive routine overview actions now render through a fixed equal-width strip for Details/Actief maken/Start; Producten and Recepten creation/scan/photo actions now share equal-width header cells instead of a full-width odd action; expanded Nutrition history meals now use a detail card with meal-type chip, Kcal/Eiwit/Kh/Vet metric grid, snapshot item rows, and aligned reuse/delete actions.
- verification evidence:
  - RED: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest.routineActionsUseWrappingSharedButtons" --tests "com.trainiq.features.nutrition.NutritionInputValidationTest.nutritionLibraryHeaderActionsUseEqualWidthCells" --tests "com.trainiq.features.nutrition.NutritionInputValidationTest.mealHistoryDetailsUseMetricCardsAndAlignedActions" --console=plain --no-configuration-cache` failed before implementation on the missing equal action strip/header cells/history detail components.
  - FAIL then fixed: the first after-change targeted run failed at compile because `Modifier.weight(...)` was constructed outside the `Row` scope; the modifier is now created inside the row scope.
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest.routineActionsUseWrappingSharedButtons" --tests "com.trainiq.features.nutrition.NutritionInputValidationTest.nutritionLibraryHeaderActionsUseEqualWidthCells" --tests "com.trainiq.features.nutrition.NutritionInputValidationTest.mealHistoryDetailsUseMetricCardsAndAlignedActions" --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --tests "com.trainiq.features.nutrition.NutritionInputValidationTest" --tests "com.trainiq.core.ui.ActionButtonLayoutPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - PASS: `git diff --check` returned only existing LF-to-CRLF warnings.
  - PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; emulator `emulator-5554` cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 2370`, and filtered AndroidRuntime crash buffer was empty.
- external sources used: none; local screenshots/request context, Compose source, and existing TrainIQ source guards were sufficient.
- remaining risk: runtime proof confirms install/launch/no crash; seeded small-screen tap-through of all three polished surfaces can still be added when representative Training/Nutrition data is preloaded.

## 2026-06-20 Nutrition History Day Summary Polish

- status: done for replacing the per-meal nutrition history list with compact day summaries while preserving meal reuse/delete behavior.
- files changed: Nutrition history now groups logged meals by local day using existing `LoggedMeal.timestamp`; each day card shows Kcal/Eiwit/Kh/Vet with the shared centered metric strip, meal/item counts, and meal-type summary; individual meal snapshots are available behind `Maaltijden bekijken` / `Verbergen`, where `Opnieuw gebruiken` and `Verwijderen` remain available.
- verification evidence:
  - RED: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.NutritionInputValidationTest" --console=plain --no-configuration-cache` failed before implementation because `groupedHistoryDays` and day-summary properties did not exist.
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.NutritionInputValidationTest" --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - PASS: `git diff --check` returned only existing LF-to-CRLF warnings.
  - PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; emulator `emulator-5554` cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 2570`, and filtered AndroidRuntime crash buffer was empty.
- external sources used: none; local source, tests, screenshot/request context, and emulator smoke were sufficient.
- remaining risk: runtime smoke confirms install/launch/no crash; a seeded tap-through of expanding multiple history day cards can be added if visual QA data is required.

## 2026-06-20 Hidden Nutrition AI Result Routing Polish

- status: done for hiding the Nutrition `AI-resultaat` section from manual navigation while keeping it as an internal result surface for meal, product, and recipe AI/photo flows.
- files changed: `AI-resultaat` is no longer listed in the visible Voeding section menu; internal tab index `2` and the existing AI result rendering remain available for automatic routing; Producten now offers `Foto/AI product`; Recepten and recipe-editor AI/photo actions route to the same hidden result surface; the AI result card now uses target-specific copy and primary actions for `Aan maaltijd toevoegen`, `Producten opslaan`, and `Als ingrediënten toevoegen`; AI product saves reuse the existing `FoodSourceType.AI` batch-save path.
- verification evidence:
  - RED: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.NutritionInputValidationTest" --console=plain --no-configuration-cache` failed before implementation on hidden AI-result tab, product AI action, target enum/routing, and target-specific AI actions.
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.NutritionInputValidationTest" --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.NutritionInputValidationTest" --tests "com.trainiq.features.ui.CompactSectionTabsSourceTest" --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - PASS: `git diff --check` returned only existing LF-to-CRLF warnings.
  - PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; emulator `emulator-5554` cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 3162`, and filtered AndroidRuntime crash buffer was empty.
- external sources used: none; local source, tests, requested screenshot/context, and emulator smoke were sufficient.
- remaining risk: runtime proof confirms install/launch/no crash; a seeded end-to-end AI scan proof still needs a configured AI provider and test photo/camera input.

## 2026-06-20 Nutrition Product/Recipe Creation Flow Polish

- status: done for aligning Producten/Recepten creation and add flows without changing nutrition persistence, AI contracts, barcode routes, or meal-draft data behavior.
- files changed: Nutrition section menu now shows `Producten` before `Recepten` while preserving internal tab indexes for scanner/barcode routing; Producten and Recepten header actions now use comparable primary/secondary controls; the Recepten action sheet no longer mixes in the direct meal-photo concept flow; the recipe editor now groups ingredient sources as `Uit producten`, `Nieuw product`, `Barcode`, and `Foto/AI`; recipe ingredient creation now mirrors the product field order and labels itself as saving a product into the recipe; saved recipes now have search/filtering similar to saved products.
- verification evidence:
  - RED: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.NutritionInputValidationTest" --console=plain --no-configuration-cache` failed before implementation on `nutritionTabTitles_keepOverviewEntryAndAiResultSeparated`, `nutritionScreen_usesSectionMenuInsteadOfPersistentTabRow`, `recipeCreationFlowMirrorsProductFlowWithoutMealConceptShortcut`, and `savedRecipesCanBeSearchedLikeProducts`.
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.NutritionInputValidationTest" --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.NutritionInputValidationTest" --tests "com.trainiq.features.ui.CompactSectionTabsSourceTest" --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - PASS: `git diff --check` returned only existing LF-to-CRLF warnings.
  - PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; emulator `emulator-5554` cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 4183`, and filtered AndroidRuntime crash buffer was empty.
- external sources used: none; local source, tests, app screenshots/request context, and emulator smoke were sufficient.
- remaining risk: runtime proof confirms install/launch/no crash; a seeded end-to-end tap-through for creating a recipe from product, barcode, and AI-photo sources remains a useful follow-up when seeded nutrition data and scanner inputs are available.

## 2026-06-20 Training Action Overlap + Nutrition Metric Strip Polish

- status: done for source-guarded Training routine action overlap prevention and Nutrition day/meal-section metric strip polish.
- files changed: shared `ActionButtonRow`/`WrappingActionRow` now passes a safe per-action modifier instead of rendering all child buttons inside one measured box; medium phone widths stack long Dutch routine labels earlier; Training routine action rows apply the shared modifier to primary, secondary, and text actions; Nutrition day and meal-section totals now use centered 1x4 Kcal/Eiwit/Kh/Vet metric strips; logged meal 2x2 nutrition pills now center their label/value content.
- verification evidence:
  - RED: targeted tests failed before implementation for medium-phone routine edit label stacking, shared action-row per-action modifier structure, Training action modifier usage, and Nutrition 1x4 centered metric strips.
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.core.ui.ActionButtonLayoutPolicyTest" --tests "com.trainiq.core.ui.WarmFuturisticUiSourceTest" --tests "com.trainiq.features.workout.WorkoutInputValidationTest.routineActionsUseWrappingSharedButtons" --tests "com.trainiq.features.nutrition.NutritionInputValidationTest.dailyAndMealSectionTotalsUseCenteredOneByFourMetricStrip" --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.core.ui.ActionButtonLayoutPolicyTest" --tests "com.trainiq.core.ui.WarmFuturisticUiSourceTest" --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --tests "com.trainiq.features.nutrition.NutritionInputValidationTest" --tests "com.trainiq.features.ui.WarmFuturisticScreenPolishSourceTest" --tests "com.trainiq.navigation.TrainDetailModeChromeTest" --tests "com.trainiq.domain.usecase.StartWorkoutSessionUseCaseTest" --tests "com.trainiq.data.repository.ActiveWorkoutSessionMutationsTest" --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; emulator `emulator-5554` cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 4280`, and filtered AndroidRuntime crash buffer was empty.
- external sources used: none; local screenshots, source, tests, and emulator smoke were sufficient.
- remaining risk: the emulator stayed on setup-gated Start during quick tab taps, so Training/Voeding visual proof is source/unit guarded and launch-smoked but not fully runtime-click verified in seeded data state for this pass.

## 2026-06-20 Active Workout Conflict + Routine/Nutrition Polish

- status: done for explicit active-workout start conflict handling, routine action wrapping, and fixed 2x2 logged-meal nutrition values.
- files changed: active-workout start now detects a different unfinished active session and shows explicit `Oude training hervatten`, `Nieuwe training starten`, and `Annuleren` actions; replacing the conflict discards the old active session by `sessionId` through the targeted Room discard path before loading the requested routine; active-workout navigation now only suppresses same-day duplicate navigation; routine actions use shared wrapping action buttons; logged meal Kcal/Eiwit/Kh/Vet values render in a fixed 2x2 grid.
- verification evidence:
  - RED: targeted tests failed before implementation for missing discard-by-session use case, active-workout route helper, conflict dialog, routine wrapping actions, and nutrition 2x2 grid.
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --tests "com.trainiq.features.nutrition.NutritionInputValidationTest" --tests "com.trainiq.navigation.TrainDetailModeChromeTest" --tests "com.trainiq.domain.usecase.StartWorkoutSessionUseCaseTest" --tests "com.trainiq.data.repository.ActiveWorkoutSessionMutationsTest" --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; emulator `emulator-5554` cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 4242`, and filtered AndroidRuntime crash buffer was empty.
  - PASS: `git diff --check` returned only existing LF-to-CRLF warnings.
- external sources used: none; local source, tests, and emulator smoke were sufficient.
- remaining risk: the explicit conflict dialog is source/unit guarded and app-launch smoked; a full end-to-end seeded UI interaction for replacing an active workout can still be added to connected tests if this path becomes release-critical.

## 2026-06-05 Home/Reminder/Training/Nutrition/Sleep Polish

- status: done for compact Home Health Connect copy, varied opt-in reminders, active-routine edit access, clearer Training history layout, less cramped Nutrition day rows, and main-session recent sleep display.
- files changed: Home now keeps Samsung/Health Connect troubleshooting off the Start card while preserving last-sync, aggregate update time, day window, and source labels; reminder content rotates through concise, subtle emoji variants without changing WorkManager cadence, channel, or permissions; active routines now expose `Routine aanpassen` even when they can start; workout history uses metric tiles and separate debrief blocks; meal rows give long names full width and move calories into macro pills; Health Connect sleep metrics now choose the likely main recent sleep session instead of summing every cached sleep record; Settings shows multiple Health Connect sleep records as compact context only.
- verification evidence:
  - RED: targeted tests failed before implementation for compact Home copy, reminder variety, active routine edit action, Training history metric tiles, Nutrition meal-row layout, and main-sleep mapping.
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.home.HomeDashboardRefreshTest" --tests "com.trainiq.core.reminders.ReminderPolicyTest" --tests "com.trainiq.features.workout.WorkoutInputValidationTest.activeRoutineCardOffersEditActionWhenRoutineCanStart" --tests "com.trainiq.features.workout.WorkoutInputValidationTest.workoutHistoryCardUsesReadableMetricTilesAndSeparateAdviceSections" --tests "com.trainiq.features.nutrition.NutritionInputValidationTest.mealEntryRowGivesLongNamesFullWidthAndMovesCaloriesIntoMacroArea" --tests "com.trainiq.data.mapper.MappersTest" --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:testDebugUnitTest --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - PASS: `git diff --check` returned only existing CRLF conversion warnings.
  - PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; emulator `emulator-5554` cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 2945`, rendered Home/Start navigation, and filtered AndroidRuntime crash buffer was empty.
- external sources used: Android Health Connect sleep-session docs and `SleepSessionRecord` API reference for interval-record behavior; Android notification creation and mobile notification guidance for concise user-controlled reminder notifications; Material onboarding/communication guidance for concise, useful user-facing copy.
- remaining risk: sleep display now avoids inflated totals from multiple records, but provider-specific duplicate/fragment behavior still needs real Samsung Health Connect device verification with fresh sleep data.

## 2026-06-05 Steps Diagnostic + Onboarding/Coach Target Polish

- status: done for the requested tour copy cleanup, Samsung Health diagnostic polish, exact calorie target override, auto macro recalculation, and AI prompt constraint.
- files changed: guided tour copy now keeps routines in Training and gives Coach only profile/calorie/macro/advice wording; onboarding copy now explains setup, optional Health Connect/AI/reminders, skipped setup follow-up, and the post-setup tour more concretely; Health Connect step diagnostics now include aggregate authority copy, local query window, freshness, Samsung source presence, latest Samsung source timestamp, and Samsung sync guidance while keeping displayed steps on `StepsRecord.COUNT_TOTAL`; Coach Doelen now accepts optional exact `Jouw calorie doel`, saves final calorie/protein/carbs/fat targets in `UserProfile`, and feeds fixed targets to Home/Nutrition/Coach/AI context through the existing profile/advice paths.
- verification evidence:
  - RED: targeted tests failed before implementation on missing manual calorie target APIs, missing richer `HealthConnectStepDiagnostic` fields/freshness behavior, and incorrect guided-tour copy expectations.
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.navigation.OnboardingNavigationTest" --tests "com.trainiq.features.coach.GoalAdviceInputTest" --tests "com.trainiq.domain.model.EnergyMathTest" --tests "com.trainiq.ai.services.AiServicesTest.generateGoalAdvice_withManualCalorieTargetPassesFixedTargetsToGeminiPrompt" --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:testDebugUnitTest --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; emulator `emulator-5554` cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 2762`, rendered Home/Start navigation, and filtered AndroidRuntime crash buffer was empty.
  - PASS: `git diff --check` returned only existing CRLF conversion warnings.
- external sources used: Android Health Connect steps/aggregate/sync docs for `StepsRecord`, aggregate `StepsRecord.COUNT_TOTAL`, no displayed-total `dataOriginFilter`, local time ranges, and changes-token direction; Samsung Health Connect FAQ and Samsung step support docs for Samsung Health-to-Health Connect permissions, `Sync now`, Galaxy Watch phone sync, and `All steps` combining phone/watch sources; Android Health Connect UX/onboarding guidance for clarity/transparency around permissions; USDA MyPlate and National Academies DRI references for personalized calorie planning and macro distribution context.
- remaining risk: exact parity with Samsung Health remains limited by what Samsung Health has synchronized into Health Connect; no Samsung private SDK/API or manual step correction was added. Manual Health Connect edge-state QA for missing/provider/partial-permission/stale real Samsung sync states still needs a physical-device pass.

## 2026-06-05 Steps Accuracy Foreground Refresh + First-Run Onboarding

- status: done for immediate Home foreground Health Connect step refresh, user-facing step freshness diagnostics, first-run onboarding, and Settings resume/reopen support.
- files changed: Home no longer waits behind the prior initial Health Connect delay before requesting status refresh; Health Connect status now carries step freshness and last-step-update state without logging raw health payloads; daily steps stay on `StepsRecord.COUNT_TOTAL` aggregate over the local day range without a `DataOrigin` filter; onboarding preferences are persisted in DataStore separately from profile fields; type-safe navigation gates first launch through `Onboarding`; Settings can reopen onboarding and shows skipped setup actions.
- verification evidence:
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "*HealthConnect*" --tests "*Home*" --tests "*Onboarding*" --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: emulator `emulator-5554` fresh appdata smoke with `:app:installDebug`; cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 5682`; first screen showed `Welkom bij TrainIQ`; tapping `Overslaan` reached Start/Home; Settings showed `Onboarding`, `Nog open`, `Health Connect koppelen`, and `AI-coach instellen`; `Onboarding openen` reopened `Eerste setup`; filtered logcat had no `FATAL EXCEPTION`.
- external sources rechecked: Android Health Connect aggregate docs for `StepsRecord.COUNT_TOTAL` and `TimeRangeFilter`; Android Health Connect sync docs for changes-token direction; Samsung Health Connect FAQ/blog for Samsung Health-to-Health Connect permissions, sync timing, and the `All steps` to `StepsRecord` mapping.
- remaining risk: exact parity with Samsung Health still depends on Samsung Health and Galaxy Watch data being synced into Health Connect and TrainIQ having `READ_STEPS`; direct Samsung Health SDK access remains out of scope.

## 2026-06-05 Onboarding Tour + Samsung Steps Diagnostic Follow-up

- status: done for no-flash onboarding startup gating, setup-plus-tour flow, clearer AI/Health Connect onboarding choices, Settings reopen preservation, and Samsung Health step-source diagnostics.
- files changed: Main startup now waits for a real DataStore onboarding emission before rendering `TrainIqApp`; onboarding preferences now persist guided-tour completion/skipped state and deferred-AI intent; Settings onboarding open no longer marks first-run incomplete; top-level navigation hosts a guided tour across Start, Training, Voeding, Voortgang, Coach, and Instellingen; Health Connect status now carries aggregate step diagnostics with raw source labels used only for troubleshooting.
- verification evidence:
  - RED: targeted onboarding/tour/diagnostic test run failed before implementation on missing `MainOnboardingState`, `HealthConnectStepDiagnostic`, guided-tour helpers, and distinct AI events.
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.MainOnboardingStateTest" --tests "com.trainiq.navigation.OnboardingNavigationTest" --tests "com.trainiq.features.onboarding.OnboardingStateTest" --tests "com.trainiq.core.datastore.OnboardingPreferencesSourceTest" --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --tests "com.trainiq.features.settings.SettingsUiStateTest" --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:testDebugUnitTest --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
- external sources used: Android Health Connect read/steps/sync docs for aggregate-first step totals, no `DataOrigin` filtering for displayed totals, and changes-token behavior; Samsung Health Connect FAQ and Samsung step-count support docs for Samsung Health All steps, phone/watch sync behavior, and Health Connect synchronization dependency.
- remaining risk: Samsung Health All steps can only match when Samsung Health has synchronized the same phone/watch data into Health Connect; no direct Samsung Health private SDK integration was added.

## 2026-06-04 Steps Accuracy + Home/Product Portion Regression Fix

- status: done for the Health Connect-only step-count correction path, Home streak/steps metric readability fix, and product default-serving editor IME visibility fix.
- files changed: Health Connect daily steps now aggregate `StepsRecord.COUNT_TOTAL` over a local `LocalDateTime` day range without `DataOrigin` filtering; shared warm cards use a measured background layer instead of unbounded `fillMaxSize()`; Home streak/steps metric cards span full rows on compact width and use full-width readable subtitle surfaces; the product editor bottom sheet is scrollable with navigation/IME padding, and the default serving field requests a later post-IME bring-into-view pass.
- verification evidence:
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests com.trainiq.data.datasource.HealthConnectPermissionPolicyTest --tests com.trainiq.features.home.HomeDashboardRefreshTest --tests com.trainiq.features.nutrition.NutritionInputValidationTest --tests com.trainiq.core.ui.WarmFuturisticUiSourceTest --tests com.trainiq.features.ui.WarmFuturisticScreenPolishSourceTest --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; emulator cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 7639`, and AndroidRuntime crash buffer was empty.
  - PASS: emulator Home UI dump showed `TrainIQ`, `Start`, `Training`, and `Voeding`.
- external sources used: Android Health Connect aggregate/time-range docs for `StepsRecord.COUNT_TOTAL` and local `TimeRangeFilter` day ranges; Samsung Health support docs for the product limitation that Samsung Health "All steps" can combine phone and wearable sources that may not all be present in Health Connect.
- remaining risk: exact parity with Samsung Health "All steps" still depends on Samsung Health syncing the same phone/wearable sources into Health Connect. A direct Samsung Health SDK source remains out of scope for this Health Connect-only fix.

## 2026-06-03 JSON Import + Health Connect Steps Correctness

- status: done for safe local JSON import preview/confirm and the reported Health Connect step mismatch class.
- files changed: Settings now supports `Data importeren uit JSON` next to export, validates selected JSON with a preview dialog before destructive replacement, and labels data-storage actions for accessibility; export-wrapper JSON is accepted by the Room import planner; confirmed import uses the existing Room import coordinator/sink transactionally; Home refresh now uses the Health Connect status/cache path so fresh `StepsRecord.COUNT_TOTAL` aggregate steps update the dashboard and Settings consistently.
- verification evidence:
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests com.trainiq.features.settings.SettingsUiStateTest --tests com.trainiq.data.migration.JsonRoomImportPlannerTest --tests com.trainiq.data.datasource.HealthConnectPermissionPolicyTest --tests com.trainiq.features.home.HomeDashboardRefreshTest --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; emulator cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 4827`, and crash buffer was empty.
  - PASS: emulator Settings UI dump showed `Gegevens / opslag`, `Data exporteren als JSON`, and `Data importeren uit JSON`.
- external sources used: Android Health Connect aggregate data docs for using aggregate metrics such as `StepsRecord.COUNT_TOTAL` as the deduplication-aware daily step source.
- remaining risk: file-picker import confirmation was source/unit guarded and Settings-render smoke verified; a full end-to-end SAF import with a real selected JSON file remains a manual/device QA follow-up because automating Android DocumentsUI file selection safely in this workspace is brittle.

## 2026-06-03 Home/Training History/Settings Polish Follow-up

- status: done for the requested Home card strip, Settings theme-button alignment, scored exercise-library filtering, and workout-history feedback visibility.
- files changed: shared `AppCard` gradient now fills the whole card surface; Settings theme choices wrap in a single aligned `FlowRow`; Training library items now include sessions, score/rank, last-performed, best estimated 1RM, and total volume; Training history cards now show routine name, duration, volume, exercise/set counts, strongest set, recovery score, stored debrief summary, recommendation, and next-session focus.
- verification evidence:
  - PASS: `./gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest --tests com.trainiq.features.workout.WorkoutInputValidationTest --tests com.trainiq.features.settings.SettingsUiStateTest --tests com.trainiq.core.ui.WarmFuturisticUiSourceTest --tests com.trainiq.data.repository.TrainIqRepositoryTest --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; emulator cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 6008`, and the crash buffer was empty.
- remaining risk: manual screenshot/font-scale signoff is still needed for the exact Home/Settings visual result on the user's device class; no new AI provider behavior was introduced in this follow-up.

## 2026-06-03 Training/Nutrition/Trend/Coach Polish Update

- status: partially-done for related P2 layout polish and AI routine duplicate-prevention; routine merge is proposal-first and non-destructive unless the user opens/compares routines.
- files changed: Training now separates Routines, Bibliotheek, and Geschiedenis inside the Train tab; active routine is no longer duplicated in the routine list; AI routine save reuses existing exercises by returned ID or conservative normalized matching; Nutrition daily meal cards and recipe editor are lighter; Trend is split into Lichaam/Kracht/Historie; Coach is split into Week/Doelen/Advies.
- verification evidence:
  - PASS: `./gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest --tests com.trainiq.features.workout.WorkoutInputValidationTest --tests com.trainiq.ai.services.RoutineGeneratorServiceTest --tests com.trainiq.data.repository.TrainIqRepositoryTest --tests com.trainiq.features.nutrition.NutritionInputValidationTest --tests com.trainiq.features.coach.GoalAdviceInputTest --tests com.trainiq.features.progress.ProgressMeasurementValidationTest --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; emulator cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 8014`, and the crash buffer was empty.
- remaining risk: routine merge currently surfaces overlap proposals and compare actions, but a full confirmed transactional merge/archive workflow remains future work; manual screenshot/font-scale signoff is still needed.

## 2026-06-03 Safe UI/AI/Product Portion Polish Update

- status: partially-done for related P2 polish and AI-output confidence items; no blocked release/accessibility owner gates were closed.
- files changed: Settings/Navigation removed the duplicate Settings-to-Voortgang CTA; Home metrics moved to shared warm metric cards; Coach goal advice uses wrapping warm sections; Nutrition product storage/editor/quick-add now supports `default_serving_grams`; Room schema advanced to v15 with a 14->15 migration and marker update.
- verification evidence:
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.NutritionInputValidationTest" --tests "com.trainiq.features.settings.SettingsUiStateTest" --tests "com.trainiq.features.coach.GoalAdviceInputTest" --tests "com.trainiq.features.ui.WarmFuturisticScreenPolishSourceTest" --tests "com.trainiq.navigation.AdaptiveNavigationPolicyTest" --tests "com.trainiq.ai.services.AiServicesTest" --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --tests "com.trainiq.data.mapper.MappersTest" --tests "com.trainiq.data.migration.RoomMigrationChainVerificationProviderTest" --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; emulator cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 6366`, and the crash buffer was empty.
- remaining risk: manual font-scale/TalkBack visual signoff, live Gemini key-backed workout debrief proof, and connected migration execution remain follow-up evidence items.

## Findings

### QA-2026-05-09-001

- finding_id: QA-2026-05-09-001
- priority: P0
- area: data, performance, Android lifecycle
- status: partially-done
- owner suggestion: Android data/platform owner
- current evidence with file references:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/RoomTrainIqRuntimeStore.kt:141` serializes the updated full app state with `gson.toJson(updated)`.
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/migration/JsonRoomImportPlanner.kt:327` clears mirror tables during mirror imports before broad reinsert/upsert work.
  - The refreshed 2026-05-09 emulator smoke launched successfully but `am start -W` reported `WaitTime: 6446`, and `gfxinfo` reported 6 janky frames out of 8 rendered frames, keeping the startup performance risk open.
  - Current worktree moves active-workout rest timer updates from full JSON mirror import to a targeted `UPDATE active_workout_sessions` path through `TrainIQ-Project/app/src/main/java/com/trainiq/core/database/TrainIqDao.kt`, `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/RoomTrainIqRuntimeStore.kt`, and `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/TrainIqRepository.kt`.
  - Current worktree moves active-workout start/resume from full JSON mirror import to targeted active-session, draft-session, active-draft, and performed-exercise writes through the same DAO/runtime-store/repository path.
  - Current worktree moves active-workout draft updates from full JSON mirror import to a targeted `active_workout_drafts` upsert plus `active_workout_sessions.updatedAt` update through the same DAO/runtime-store/repository path.
  - Current worktree moves active-workout discard from full JSON mirror import to targeted deletes for workout log-event snapshots, workout log events, active workout session children, performed exercises, and the draft workout session through the same DAO/runtime-store/repository path.
  - Current worktree moves active-workout set logging from full JSON mirror import to targeted upserts for the active session, draft, active set, undo log event, and undo snapshot rows through the same DAO/runtime-store/repository path.
  - Current worktree moves active-workout set editing from full JSON mirror import to targeted active set, draft, rest-timer, and current undo snapshot updates through the same DAO/runtime-store/repository path.
  - Current worktree moves active-workout set type editing from full JSON mirror import to targeted active set type, current undo snapshot type, and session timestamp updates.
  - Current worktree moves active-workout set deletion from full JSON mirror import to targeted active set deletion, pending add-event cleanup, and session timestamp updates.
  - Current worktree moves active-workout collapse/expand toggles from full JSON mirror import to targeted `active_workout_collapsed_exercises` insert/delete plus session timestamp update.
  - Current worktree moves active-workout finish from full JSON mirror import to a targeted Room transaction for completed `workout_sessions`, `performed_exercises`, `workout_sets`, debrief fields, and active-workout runtime cleanup.
  - Current worktree moves active-workout undo from full JSON mirror import to a targeted Room transaction for restored `active_workout_sets`, pending undo event snapshots, and active session timestamp updates.
  - Current worktree moves body measurement add/delete from full JSON mirror import to targeted `body_measurements` insert/delete paths through the same DAO/runtime-store/repository path.
  - Current worktree moves meal save/delete from full JSON mirror import to targeted `meals` and `meal_items` upsert/delete transactions.
  - Current worktree moves profile save/reset from full JSON mirror import to targeted `user_profile` upsert/delete calls.
  - Current worktree moves active routine selection from full JSON mirror import to targeted `workout_routines` active-flag update.
  - Current worktree moves superset grouping from full JSON mirror import to targeted `workout_exercises.superset_group_id` updates.
  - Current worktree moves workout exercise plan updates from full JSON mirror import to targeted workout-exercise upsert plus per-exercise routine-set replacement.
  - Current worktree moves planned and active-workout exercise replacement from full JSON mirror import to targeted `workout_exercises` upsert for the affected row, with active-session timestamp update when applicable.
  - Current worktree moves routine set edits from full JSON mirror import to targeted `routine_sets` upserts plus synchronized `workout_exercises` target updates.
  - Current worktree moves delayed startup exercise-library seeding from full JSON mirror import to a targeted `exercises` upsert path through `ExerciseLibrarySeeder.missingCanonicalExercises(...)`, `RoomTrainIqRuntimeStore.seedExerciseLibrary(...)`, and `TrainIqDao.insertExercises(...)`.
  - 2026-05-11 source scan `rg "runtimeStore\.update\(" TrainIQ-Project/app/src/main/java/com/trainiq -n` returned no app-source callers; `RoomTrainIqRuntimeStore.update(transform)` remains only as legacy/import infrastructure.
  - Current worktree removes the public `RoomTrainIqRuntimeStore.update(transform)` API entirely; legacy JSON seeding remains private in `seedRoomFromLegacyJsonIfNeeded()`, while mirror-run/dry-run infrastructure remains in `data/migration`.
  - `TrainIQ-Project/app/src/test/java/com/trainiq/architecture/RoomAuthorityArchitectureTest.kt` now guards that `updateActiveWorkoutRestTimer(...)`, `updateActiveWorkoutDraft(...)`, `logActiveWorkoutSet(...)`, and `discardActiveWorkout(...)` use targeted Room updates/deletes instead of `runtimeStore.update { ... }`.
- expected target-state behavior: Normal user mutations use bounded targeted DAO transactions. Startup and critical input paths do not perform full-state JSON serialization, broad import planning, or broad mirror table replacement.
- concrete recommended fix: Keep JSON import for legacy/import tooling only. Add targeted DAO-backed repository mutations for active workout logging, meal save/delete, routine edit/delete, measurement edit/delete, finish/discard, and profile writes. Add a regression guard that these hot paths do not call `RoomTrainIqRuntimeStore.update()`.
- regression risk: High. This touches persistence and process-restart correctness; migrate flow by flow behind tests instead of replacing all mutations at once.
- minimal verification command/check: `./gradlew.bat :app:testDebugUnitTest :app:connectedDebugAndroidTest --console=plain`, plus an active-workout logging smoke with `adb shell dumpsys gfxinfo com.trainiq framestats`.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/core/database/TrainIqDao.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/RoomTrainIqRuntimeStore.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/ExerciseLibrarySeeder.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/TrainIqRepository.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/architecture/RoomAuthorityArchitectureTest.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/data/repository/ExerciseLibrarySeederTest.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/data/repository/WorkoutSessionTransactionTest.kt`
- verification evidence:
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 9163`; crash buffer was empty.
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 8013`; crash buffer was empty.
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 6942`; crash buffer was empty.
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 11091`; crash buffer was empty.
  - 2026-05-10 baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set edit after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set edit after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set edit after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set edit after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
  - 2026-05-11 exercise-library seeding after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.repository.ExerciseLibrarySeederTest" --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-11 exercise-library seeding source scan PASS: `rg "runtimeStore\.update\(" TrainIQ-Project/app/src/main/java/com/trainiq -n` returned no matches.
  - 2026-05-11 exercise-library seeding broad gate PASS: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 exercise-library seeding device smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; SM-S931B cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 710`; after waiting for the delayed seed job, `logcat-crash-slice.txt` was empty. Evidence: `TrainIQ-Project/.codex/device-qa/2026-05-11-post-exercise-seed-launch/`.
  - 2026-05-11 runtime update API removal PASS: focused `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.repository.WorkoutSessionTransactionTest" --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-11 runtime update API removal PASS: broad `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 runtime update API removal PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; SM-S931B cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 698`; after startup delay, `logcat-crash-slice.txt` was empty. Evidence: `TrainIQ-Project/.codex/device-qa/2026-05-11-post-runtime-update-removal-launch/`.
  - 2026-05-11 post-removal connected persistence PASS: `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache`; report `TEST-SM-S931B - 16-_app-.xml` recorded `tests="26" failures="0" errors="0" skipped="0"`.
  - 2026-05-10 active-collapse after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 active-collapse after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-collapse after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 active-collapse after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set-type edit after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set-type edit after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set-type edit after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set-type edit after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set delete after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set delete after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set delete after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set delete after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-workout finish persistence after-change PASS: `./gradlew :app:testDebugUnitTest --tests com.trainiq.architecture.RoomAuthorityArchitectureTest`.
  - 2026-05-10 active-workout finish persistence after-change PASS: `./gradlew :app:testDebugUnitTest --tests com.trainiq.data.repository.TrainIqRepositoryTest --tests com.trainiq.data.repository.WorkoutSessionTransactionTest --tests com.trainiq.data.repository.ActiveWorkoutSessionMutationsTest --tests com.trainiq.data.repository.WorkoutLogEventTest --tests com.trainiq.data.repository.WorkoutCompletionSummaryTest`.
  - 2026-05-10 active-workout finish persistence after-change PASS: `./gradlew :app:assembleDebug`.
  - 2026-05-10 active-workout finish persistence after-change PASS: `./gradlew :app:lintDebug`.
  - 2026-05-10 active-workout undo persistence after-change PASS: `./gradlew :app:testDebugUnitTest --tests com.trainiq.architecture.RoomAuthorityArchitectureTest --tests com.trainiq.data.repository.WorkoutLogEventTest`.
  - 2026-05-10 active-workout undo persistence after-change PASS: `./gradlew :app:testDebugUnitTest --tests com.trainiq.data.repository.TrainIqRepositoryTest --tests com.trainiq.data.repository.ActiveWorkoutSessionMutationsTest --tests com.trainiq.data.repository.WorkoutLogEventTest --tests com.trainiq.domain.usecase.StartWorkoutSessionUseCaseTest --tests com.trainiq.features.workout.WorkoutInputValidationTest`.
  - 2026-05-10 active-workout undo persistence after-change PASS: `./gradlew :app:assembleDebug`.
  - 2026-05-10 active-workout undo persistence after-change PASS: `./gradlew :app:lintDebug`.
  - 2026-05-10 routine core persistence after-change PASS: `./gradlew :app:testDebugUnitTest --tests com.trainiq.architecture.RoomAuthorityArchitectureTest`.
  - 2026-05-10 routine core persistence after-change PASS: `./gradlew :app:testDebugUnitTest --tests com.trainiq.data.repository.TrainIqRepositoryTest --tests com.trainiq.features.workout.WorkoutInputValidationTest --tests com.trainiq.domain.usecase.StartWorkoutSessionUseCaseTest`.
  - 2026-05-10 routine core persistence after-change PASS: `./gradlew :app:assembleDebug`.
  - 2026-05-10 routine core persistence after-change PASS: `./gradlew :app:lintDebug`.
  - 2026-05-10 routine set add/delete/move persistence after-change PASS: `./gradlew :app:testDebugUnitTest --tests com.trainiq.architecture.RoomAuthorityArchitectureTest --tests com.trainiq.data.repository.TrainIqRepositoryTest`.
  - 2026-05-10 routine set add/delete/move persistence after-change PASS: `./gradlew :app:testDebugUnitTest --tests com.trainiq.features.workout.WorkoutInputValidationTest --tests com.trainiq.domain.usecase.StartWorkoutSessionUseCaseTest --tests com.trainiq.data.repository.TrainIqRepositoryTest`.
  - 2026-05-10 routine set add/delete/move persistence after-change PASS: `./gradlew :app:assembleDebug`.
  - 2026-05-10 routine set add/delete/move persistence after-change PASS: `./gradlew :app:lintDebug`.
  - 2026-05-10 workout day add/remove persistence after-change PASS: `./gradlew :app:testDebugUnitTest --tests com.trainiq.architecture.RoomAuthorityArchitectureTest --tests com.trainiq.data.repository.TrainIqRepositoryTest`.
  - 2026-05-10 workout day add/remove persistence after-change PASS: `./gradlew :app:testDebugUnitTest --tests com.trainiq.features.workout.WorkoutInputValidationTest --tests com.trainiq.domain.usecase.StartWorkoutSessionUseCaseTest --tests com.trainiq.data.repository.TrainIqRepositoryTest`.
  - 2026-05-10 workout day add/remove persistence after-change PASS: `./gradlew :app:assembleDebug`.
  - 2026-05-10 workout day add/remove persistence after-change PASS: `./gradlew :app:lintDebug`.
  - 2026-05-10 workout exercise add/remove persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --tests "com.trainiq.data.repository.TrainIqRepositoryTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 workout exercise add/remove persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-10 workout exercise add/remove physical-device smoke PASS on SM-S931B: `:app:installDebug`, `adb shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 794`, Home rendered in `TrainIQ-Project/.codex/trainiq-app-ready-smoke.xml`, and crash buffer was empty.
  - 2026-05-10 add-exercise-to-routine persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --tests "com.trainiq.data.repository.TrainIqRepositoryTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 add-exercise-to-routine persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-10 add-exercise-to-routine physical-device smoke PASS on SM-S931B: `:app:installDebug`, `adb shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 692`, Home rendered in `TrainIQ-Project/.codex/trainiq-app-ready-smoke-latest.xml`, and crash buffer was empty.
  - 2026-05-10 session delete persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --tests "com.trainiq.data.repository.TrainIqRepositoryTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 session delete persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-10 session delete physical-device smoke PASS on SM-S931B: `:app:installDebug`, `adb shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 701`, Home rendered in `TrainIQ-Project/.codex/trainiq-app-ready-smoke-final.xml`, and crash buffer was empty.
  - 2026-05-10 generated-routine save persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --tests "com.trainiq.data.repository.TrainIqRepositoryTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 generated-routine save persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-10 generated-routine save physical-device smoke PASS on SM-S931B: `:app:installDebug`, `adb shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 699`, Home rendered in `TrainIQ-Project/.codex/trainiq-app-ready-smoke-generated-routine.xml`, and crash buffer was empty.
  - 2026-05-10 recipe/food persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --tests "com.trainiq.data.repository.TrainIqRepositoryTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 recipe/food persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-10 recipe/food physical-device smoke PASS on SM-S931B: `:app:installDebug`, `adb shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 714`, Home rendered in `TrainIQ-Project/.codex/trainiq-app-ready-smoke-food-recipe.xml`, and crash buffer was empty.
  - 2026-05-10 process-restart correctness instrumentation PASS: `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B. The test writes targeted routine, nutrition, meal, profile, measurement, workout day/exercise, and session rows, closes/reopens the database, and verifies inserted rows persist while deleted session/nutrition/meal/measurement/workout rows do not resurrect.
  - 2026-05-11 meal restart coverage follow-up PASS: same targeted connected class on SM-S931B recorded `tests="5" failures="0" errors="0"` including `targetedMealMutationsSurviveDatabaseReopen`.
  - 2026-05-11 active-workout start persistence PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`; `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with `tests="6" failures="0" errors="0"`; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 tooling note: an initial parallel Gradle verification attempt failed with Kotlin/Hilt incremental cache file registration/exists errors; after `./gradlew.bat --stop`, the same checks passed serially.
  - 2026-05-11 active routine selection persistence PASS: `./gradlew.bat clean :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`; `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 7 tests, 0 failures, 0 errors; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 tooling note: the first post-change compile showed stale unresolved-reference errors across existing helper imports; `clean` rebuilt Kotlin/KSP state and verification then passed.
  - 2026-05-11 superset persistence PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`; `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 8 tests, 0 failures, 0 errors; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 workout exercise plan persistence PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`; `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 9 tests, 0 failures, 0 errors; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 replace-exercise-in-plan persistence PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`; `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 10 tests, 0 failures, 0 errors; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 replace-exercise-in-active-workout persistence PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`; `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 11 tests, 0 failures, 0 errors; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 active-workout runtime mutation restart coverage PASS: `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 12 tests, 0 failures, 0 errors including `targetedActiveWorkoutRuntimeMutationsSurviveDatabaseReopen`; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 active-workout finish/undo restart coverage PASS: `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 14 tests, 0 failures, 0 errors including `targetedActiveWorkoutFinishSurvivesDatabaseReopenAndClearsRuntimeRows` and `targetedActiveWorkoutUndoSurvivesDatabaseReopen`; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 routine-set edit/replace restart coverage PASS: `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 15 tests, 0 failures, 0 errors including `targetedRoutineSetEditAndReplaceSurvivesDatabaseReopen`; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 routine lifecycle restart coverage PASS: `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 16 tests, 0 failures, 0 errors including `targetedRoutineCreateUpdateDeleteSurvivesDatabaseReopen`; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 exercise reorder restart coverage PASS: `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 17 tests, 0 failures, 0 errors including `targetedExerciseReorderSurvivesDatabaseReopen`; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 generated-routine graph restart coverage PASS: `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 18 tests, 0 failures, 0 errors including `targetedGeneratedRoutineGraphSurvivesDatabaseReopen`; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 profile reset restart coverage PASS: `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 19 tests, 0 failures, 0 errors including `targetedProfileResetSurvivesDatabaseReopen`; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 active-set delete restart coverage PASS: `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 20 tests, 0 failures, 0 errors including `targetedActiveWorkoutSetDeleteSurvivesDatabaseReopen`; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 active-set type edit restart coverage PASS: `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 21 tests, 0 failures, 0 errors including `targetedActiveWorkoutSetTypeEditSurvivesDatabaseReopen`; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 active-set value edit restart coverage PASS: `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 22 tests, 0 failures, 0 errors including `targetedActiveWorkoutSetValueEditSurvivesDatabaseReopen`; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 active-collapse expand restart coverage PASS: `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 23 tests, 0 failures, 0 errors including `targetedActiveWorkoutCollapseExpandSurvivesDatabaseReopen`; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 active-workout discard restart coverage PASS: `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 24 tests, 0 failures, 0 errors including `targetedActiveWorkoutDiscardSurvivesDatabaseReopenAndClearsRuntimeRows`; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 workout debrief refresh restart coverage PASS: `./gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 25 tests, 0 failures, 0 errors including `targetedWorkoutDebriefRefreshSurvivesDatabaseReopen`; `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 routine cascade delete restart coverage PASS: `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache` on SM-S931B with 26 tests, 0 failures, 0 errors including `targetedRoutineCascadeDeleteSurvivesDatabaseReopen`.
  - 2026-05-11 post-routine-cascade broad/device gate PASS: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`; `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; SM-S931B cold launch returned `Status: ok`, `WaitTime: 892`, and the crash buffer was empty. Evidence: `TrainIQ-Project/.codex/device-qa/2026-05-11-post-cascade-launch/`.
  - 2026-05-10 meal persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 meal persistence after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 meal persistence after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 meal persistence after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 profile persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 profile persistence after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 profile persistence after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 profile persistence after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 routine-set edit persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 routine-set edit persistence after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 routine-set edit persistence after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 routine-set edit persistence after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
- external sources used: None. Local Room DAO patterns and existing architecture tests were sufficient; no Android/Room API ambiguity blocked this batch.
- remaining risk: This moves active workout start/resume, rest timer, active draft, active set logging/editing/type editing/deletion/undo, active collapse/expand, active discard/finish, workout debrief refresh, active-workout exercise replacement, session delete, routine create/update/delete, active routine selection, generated-routine save, exercise reorder, superset grouping, workout exercise replacement in plans, workout exercise plan updates, workout day add/remove, workout exercise add/remove, add-exercise-to-routine, routine set add/edit/delete/move, meal save/delete, recipe/food save/delete, profile save/reset, and body measurement add/delete paths to targeted Room persistence. Instrumentation process-restart tests now cover representative targeted generated-routine graph save, routine lifecycle, standalone routine cascade delete, exercise reorder, routine-set edit/replace, active routine selection, superset grouping, active-workout runtime mutations, active-workout collapse/expand deletion, active-workout set delete/type-edit/value-edit cleanup, active-workout discard/finish/undo cleanup, workout debrief refresh, workout exercise replacement in plans and active workouts, workout exercise plan update, nutrition save/delete, meal save/delete, active-workout start, profile save/reset, profile/measurement add-delete, session delete, workout day/exercise add, workout exercise delete, and workout day cascade delete persistence after database reopen; broader process-restart coverage for every individual mutation can still be expanded before fully closing QA-001.

### QA-2026-05-09-002

- finding_id: QA-2026-05-09-002
- priority: P0
- area: release, privacy, security
- status: blocked
- owner suggestion: product/legal/release owner
- current evidence with file references:
  - `TrainIQ-Project/docs/release/owner-action-tracker.md:5` marks release status as blocked.
  - `TrainIQ-Project/docs/release/play-console-owner-checklist.md` requires owner confirmation for Health Apps declaration, Data Safety, privacy policy URL, and signing.
  - `TrainIQ-Project/app/src/main/AndroidManifest.xml:4` through `TrainIQ-Project/app/src/main/AndroidManifest.xml:12` declare camera, internet, six Health Connect read permissions, and background health read.
- expected target-state behavior: Play submission, Data Safety, Health Connect declarations, privacy policy, signing, and production AI boundary are explicitly approved before release.
- concrete recommended fix: Complete the release owner checklist, decide and document production AI mode, approve Data Safety answers, confirm background Health Connect read justification, and record signing ownership.
- regression risk: Medium. Documentation and release gate changes are low code risk but high compliance risk if inaccurate.
- minimal verification command/check: Review `TrainIQ-Project/docs/release/owner-action-tracker.md` and confirm all P0 owner gates are closed or have written release exceptions.

### QA-2026-05-09-003

- finding_id: QA-2026-05-09-003
- priority: P0
- area: accessibility, UX, release
- status: blocked
- owner suggestion: accessibility/manual QA owner
- current evidence with file references:
  - `TrainIQ-Project/docs/qa/human-assistive-tech-qa-signoff.md:26` through `TrainIQ-Project/docs/qa/human-assistive-tech-qa-signoff.md:35` list required TalkBack/Switch Access flows as `NOT_RUN`.
  - `TrainIQ-Project/docs/qa/talkback-switch-access-test-script.md` defines the manual flow but final signoff remains unchecked.
- expected target-state behavior: Active workout, scanner, Health Connect rationale, AI routine generation, Settings destructive actions, font scaling, and dark mode have signed TalkBack and Switch Access evidence before release.
- concrete recommended fix: Run the manual accessibility script on current build, attach evidence paths, update signoff, and file code issues for failed flows.
- regression risk: Low for documentation, potentially medium for UI fixes discovered by the pass.
- minimal verification command/check: Complete `TrainIQ-Project/docs/qa/talkback-switch-access-test-script.md` on a device/emulator with TalkBack and Switch Access enabled.

### QA-2026-05-09-004

- finding_id: QA-2026-05-09-004
- priority: P1
- area: Android lifecycle, performance
- status: done
- owner suggestion: Android app owner
- current evidence with file references:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/home/HomeScreen.kt:129` starts a `while (true)` refresh loop from `HomeViewModel`.
  - The loop refreshes dashboard and Health Connect status on a timer even if the Home destination is retained but no longer visible.
- expected target-state behavior: Periodic foreground refresh is visible-lifecycle aware and does not keep retained off-screen top-level destinations doing Health Connect/dashboard work.
- concrete recommended fix: Move periodic refresh triggering to lifecycle-aware UI collection or a visibility signal, and keep the ViewModel refresh API idempotent. Add a test for off-screen Home not scheduling refresh work.
- regression risk: Medium. Home freshness can regress if lifecycle boundaries are too strict.
- minimal verification command/check: `./gradlew.bat :app:testDebugUnitTest --tests "*Home*" --console=plain` plus manual top-level navigation with log evidence that refresh pauses off-screen.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/home/HomeScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/home/HomeDashboardRefreshTest.kt`
- verification evidence:
  - RED: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.home.HomeDashboardRefreshTest" --console=plain` failed while `HomeViewModel` still owned the retained periodic `while (true)` refresh loop.
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.home.HomeDashboardRefreshTest" --console=plain`
  - 2026-05-12 PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.home.HomeDashboardRefreshTest" --console=plain --no-configuration-cache`.
  - 2026-05-12 tooling note: `./gradlew.bat :app:testDebugUnitTest --tests "*Home*" --console=plain --no-configuration-cache` failed because the broad wildcard filter resolved to a non-test include (`ui-home.xml`); use the exact `HomeDashboardRefreshTest` class filter for repeatable verification.
- remaining risk: Periodic refresh now runs under `HomeRoute` `repeatOnLifecycle(Lifecycle.State.STARTED)` and the exact source/unit guard passes, but manual top-level navigation log evidence that refresh pauses off-screen was not captured because adding runtime diagnostic logging solely for this evidence would be more invasive than the current risk.

### QA-2026-05-09-005

- finding_id: QA-2026-05-09-005
- priority: P1
- area: performance, release
- status: blocked
- owner suggestion: product/Android performance owner
- current evidence with file references:
  - `TrainIQ-Project/docs/qa/performance-threshold-decision-record.md` still requires product confirmation for numeric thresholds.
  - `TrainIQ-Project/macrobenchmark/src/main/java/com/trainiq/macrobenchmark/TrainIqStartupBenchmark.java` defines startup/frame benchmarks, but emulator benchmark results are not release-certifying.
  - 2026-05-09 debug emulator smoke remains risky: latest `am start -W` reported `WaitTime: 6446`; latest `gfxinfo` showed 6/8 janky frames on the first-draw sample.
- expected target-state behavior: Profileable/release startup, navigation, active workout logging, scanner launch, and settings scroll have approved p50/p95/jank thresholds and physical-device evidence.
- concrete recommended fix: Set numeric thresholds, run macrobenchmarks on at least one physical lower-end device and one representative modern device, and track profileable/release results separately from debug emulator signals.
- regression risk: Low for measurement setup, medium if performance fixes alter startup/data flow.
- minimal verification command/check: `./gradlew.bat :macrobenchmark:connectedProfileableAndroidTest --console=plain` on a physical device with approved benchmark suppression policy only when justified.

### QA-2026-05-09-006

- finding_id: QA-2026-05-09-006
- priority: P1
- area: security, privacy, backend
- status: needs-decision
- owner suggestion: product/backend/security/legal owner
- current evidence with file references:
  - `TrainIQ-Project/docs/architecture/ai-gateway-decision-record.md` keeps the production AI boundary unresolved.
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/remote/GeminiApi.kt:14` correctly sends the API key in `x-goog-api-key`, and app services use Gemini 2.5 Flash, but the current production mode remains BYOK/direct-client unless changed.
- expected target-state behavior: Production release chooses one signed-off AI mode: BYOK accepted, backend gateway, OAuth-mediated access, hybrid, or AI scoped out.
- concrete recommended fix: Close the AI boundary decision record and update release/privacy/Data Safety docs to match the chosen mode.
- regression risk: High if changing architecture from BYOK to backend; low if documenting a BYOK MVP exception.
- minimal verification command/check: Confirm `TrainIQ-Project/docs/architecture/ai-gateway-decision-record.md` has a final decision and owner signoff.

### QA-2026-05-09-007

- finding_id: QA-2026-05-09-007
- priority: P1
- area: backend, UX
- status: done
- owner suggestion: AI/platform owner
- current evidence with file references:
  - Prior evidence found `TrainIQ-Project/app/src/main/java/com/trainiq/ai/services/AiSupport.kt` only mapping HTTP 429 into a typed AI rate-limit exception and no explicit `withTimeout` policy.
  - Current worktree adds `AiFeature` timeout budgets, `AiTimeoutException`, `withTimeout(...)`, and cancellation propagation in `TrainIQ-Project/app/src/main/java/com/trainiq/ai/services/AiSupport.kt`.
  - Current worktree adds feature-scoped in-memory 429 throttles through `AiFeatureThrottle` and `AiFeatureThrottledException` in `TrainIQ-Project/app/src/main/java/com/trainiq/ai/services/AiSupport.kt`.
  - Gemini meal scan, workout debrief, goal advice, weekly report, and routine generation now call `callGeminiWithBoundedRetry(feature = ...)` in `TrainIQ-Project/app/src/main/java/com/trainiq/ai/services/AiServices.kt` and `TrainIQ-Project/app/src/main/java/com/trainiq/ai/services/RoutineGeneratorService.kt`.
  - Routine generation rethrows rate-limit/throttle failures so the existing snackbar path can show `toAiUserMessage(...)`; other AI features keep local fallback output with existing fallback copy/source markers.
- expected target-state behavior: Every AI feature has explicit timeout, cancellation, retry, fallback, rate-limit, and user-message policy.
- concrete recommended fix: Add central typed AI result/failure mapping, per-feature timeout constants, cancellation propagation, and feature throttles. Cover Gemini timeout, 429, invalid JSON, offline, and local fallback in tests.
- regression risk: Medium. Timeouts can prematurely fallback on slow but valid responses if set too aggressively.
- minimal verification command/check: `./gradlew.bat :app:testDebugUnitTest --tests "*Ai*" --console=plain`.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/ai/services/AiSupport.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/ai/services/AiServices.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/ai/services/RoutineGeneratorService.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/ai/services/AiServicesTest.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/ai/services/RoutineGeneratorServiceTest.kt`
- verification evidence:
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.ai.services.AiServicesTest" --tests "com.trainiq.ai.services.RoutineGeneratorServiceTest" --console=plain`
  - After-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.ai.services.AiServicesTest" --tests "com.trainiq.ai.services.RoutineGeneratorServiceTest" --console=plain`
  - RED: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.ai.services.AiServicesTest" --tests "com.trainiq.ai.services.RoutineGeneratorServiceTest" --console=plain` failed while the new test called `toAiUserMessage(...)` on a nullable `Throwable?` without a safe call.
  - PASS after fix: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.ai.services.AiServicesTest" --tests "com.trainiq.ai.services.RoutineGeneratorServiceTest" --console=plain`
  - After-change PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain`
  - After-change PASS: `./gradlew.bat :app:test --console=plain`
  - After-change PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain`
  - After-change PASS: `./gradlew.bat :app:test --console=plain`
  - Emulator check NOT RUN: first availability guard misread multi-line `adb devices` output and skipped despite `emulator-5554` being present; rerun targeted the emulator directly.
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 7888`; crash buffer was empty.
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain`; `adb shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 9918`; crash buffer was empty.
- external sources used:
  - Kotlin null safety documentation: https://kotlinlang.org/docs/null-safety.html. Used to confirm the compiler failure should be fixed with a safe-call/null-safe expression in the test assertion.
- remaining risk: Throttles are process-local and reset after app process death. That is acceptable for an MVP client boundary, but production release still depends on QA-006's signed AI boundary decision.

### QA-2026-05-09-008

- finding_id: QA-2026-05-09-008
- priority: P2
- area: Android lifecycle, UX
- status: done
- owner suggestion: Android UI owner
- current evidence with file references:
  - Prior evidence found camera permission/error state stored in non-saveable composable state.
  - Current worktree uses `rememberSaveable`/`CameraScannerRestorableState.Saver` for permission denied and camera error state in `TrainIQ-Project/app/src/main/java/com/trainiq/features/nutrition/CameraScannerScreen.kt`.
- expected target-state behavior: Camera denied/error/capture states survive configuration changes where user context could otherwise become misleading.
- concrete recommended fix: Move camera permission and capture state into ViewModel or `rememberSaveable` where appropriate, and add a state restoration test for denied/error states.
- regression risk: Low to medium. Avoid persisting transient capture-in-progress state incorrectly after process death.
- minimal verification command/check: `./gradlew.bat :app:testDebugUnitTest --tests "*Camera*" --console=plain` plus rotate/recreate scanner smoke.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/nutrition/CameraScannerScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/nutrition/CameraScannerStateTest.kt`
- verification evidence:
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "*Camera*" --console=plain`
  - After-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "*Camera*" --console=plain`
  - 2026-05-12 PASS: `./gradlew.bat :app:testDebugUnitTest --tests "*Camera*" --console=plain --no-configuration-cache`.
  - 2026-05-12 PASS: disposable emulator smoke in `TrainIQ-Project/.codex/device-qa/2026-05-12-scanner-rotate-recreate-qa/` launched the AI scanner permission gate from the Ochtend meal sheet at 360x640/mdpi/font scale 1.5 without granting camera permission, rotated portrait to landscape and back to portrait, and captured `110-ai-scanner-before-rotate.xml`, `111-ai-scanner-landscape.xml`, and `112-ai-scanner-portrait-restored.xml` with `Cameratoegang nodig`, `Toegang geven`, `Terug`, `NAF=0`, and empty crash buffer.
- remaining risk: `isCapturing` intentionally remains transient and resets after recreation to avoid resuming a stale photo capture. The permission-gate rotate/recreate smoke now passes; preview/capture rotation still needs safe camera-use signoff before release.

### QA-2026-05-09-009

- finding_id: QA-2026-05-09-009
- priority: P2
- area: accessibility, UI
- status: done
- owner suggestion: Android UI/accessibility owner
- current evidence with file references:
  - Prior evidence found unlabeled Canvas chart surfaces in `TrainIQ-Project/app/src/main/java/com/trainiq/core/ui/AppDesign.kt` and `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`.
  - Current dirty worktree adds `lineChartContentDescription(...)` in `TrainIQ-Project/app/src/main/java/com/trainiq/core/ui/AppDesign.kt` and applies chart semantics in both shared and workout chart call sites.
- expected target-state behavior: Charts and custom visualizations expose meaningful semantic summaries for assistive technology.
- concrete recommended fix: Keep the new `Modifier.semantics { contentDescription = ... }` chart summaries, broaden the same pattern to remaining custom visualizations, and confirm TalkBack output manually before release.
- regression risk: Low. Risk is mainly inaccurate summaries if data labels do not match chart values.
- minimal verification command/check: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.core.ui.LineChartSemanticsTest" --console=plain` and `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.core.ui.AppLineChartAccessibilityTest" --console=plain`.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/core/ui/AppDesign.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/core/ui/LineChartSemanticsTest.kt`
  - `TrainIQ-Project/app/src/androidTest/java/com/trainiq/core/ui/AppLineChartAccessibilityTest.kt`
- verification evidence:
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.core.ui.LineChartSemanticsTest" :app:compileDebugAndroidTestKotlin --console=plain`
  - PASS: `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.core.ui.AppLineChartAccessibilityTest" --console=plain` on `emulator-5554`
- remaining risk: Chart summaries are intentionally concise and derived from existing chart point labels/values; manual TalkBack review is still needed for release signoff, so the broader accessibility release item remains open.

### QA-2026-05-09-010

- finding_id: QA-2026-05-09-010
- priority: P2
- area: Android lifecycle, data
- status: done
- owner suggestion: Android platform owner
- current evidence with file references:
  - Prior evidence found `TrainIQ-Project/app/src/main/java/com/trainiq/core/health/HealthConnectBackgroundSyncWorker.kt` retrying all thrown failures.
  - Current worktree adds `shouldRetryHealthConnectBackgroundSyncFailure(...)` and avoids retrying `SecurityException`, `IllegalArgumentException`, and `UnsupportedOperationException`.
- expected target-state behavior: Background sync retries transient failures only and does not loop on permanent provider, permission, or configuration states.
- concrete recommended fix: Classify exceptions into transient/permanent categories, return `Result.failure()` or `Result.success()` for permanent states, and keep retry for network/transient Health Connect errors.
- regression risk: Medium. Misclassification could stop recovery from real transient failures.
- minimal verification command/check: `./gradlew.bat :app:testDebugUnitTest --tests "*HealthConnectBackgroundSync*" --console=plain`.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/core/health/HealthConnectBackgroundSyncWorker.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/core/health/HealthConnectBackgroundSyncWorkerTest.kt`
- verification evidence:
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "*HealthConnectBackgroundSync*" --console=plain`
  - After-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "*HealthConnectBackgroundSync*" --console=plain`
- remaining risk: Retry classification is intentionally conservative; unknown failures still retry, while permission/configuration/provider unsupported exceptions stop immediate retry loops. End-to-end WorkManager behavior with actual revoked permissions/provider states was not run.

### QA-2026-05-09-011

- finding_id: QA-2026-05-09-011
- priority: P2
- area: UI, UX, accessibility
- status: partially-done
- owner suggestion: Android UI owner
- current evidence with file references:
  - Prior runtime artifact `runtime-gemini-test/active-workout-start.xml` shows dense active-workout rows with clipped/tiny bounds for `Set 1`.
  - Prior runtime artifact `runtime-gemini-test/routine-ai-dialog.xml` includes `NAF="true"` nodes and English labels such as `Days per week`, `Available equipment`, and `Experience level`.
  - Current worktree uses a `ModalBottomSheet` for generated routine preview and replaces no-op metadata `AssistChip` controls with non-clickable `GeneratedRoutineInfoPill` labels in `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/RoutineDialogs.kt`.
  - Current worktree changes active/routine set scan-row metric labels from `Reps` to compact Dutch `Herh.` through `RepetitionsMetricLabel` in `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`.
  - Current worktree changes active-workout rest timer icon-only controls from terse labels like `30 seconden minder` to contextual Dutch labels such as `Rusttimer 30 seconden korter` in `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`.
  - Current worktree changes the active-workout session status metric label from English `Rest` to Dutch `Rust` through `activeWorkoutRestStatusLabel()` in `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`.
  - Current worktree gives active-workout session summary metrics equal weighted columns and merged `Label: value` accessibility summaries through `StatusMetric(...)` and `statusMetricContentDescription(...)` in `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`.
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutDialogPresentationPolicyTest.kt` now guards that generated routine preview avoids dense `AlertDialog`, keeps scroll support, avoids stale English labels, and uses read-only metadata labels.
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt` now guards the active/routine set metric labels, active-workout rest status label, status metric accessibility summaries, and rest timer action descriptions.
  - 2026-05-11 compact/font-scale QA on SM-S931B captured Training and AI routine dialog dumps at font scale 1.3 and 1.5 under `TrainIQ-Project/.codex/device-qa/2026-05-11-compact-font-workout-qa/`; pre-fix font scale 1.5 AI dialog exposed one `NAF="true"` equipment field when its visual label was partially clipped.
  - 2026-05-11 polish added an explicit reusable `accessibilityLabel` path to `TapOnlyOutlinedTextField` and applied it to the AI routine `Beschikbaar materiaal` field, preserving the visual label while keeping the accessibility name available at large font scale.
- expected target-state behavior: Active workout and AI routine generation remain reachable, Dutch, labeled, and unclipped at 360px-class widths and font scale 1.3+.
- concrete recommended fix: Re-run current compact/font-scale QA, then replace dense alert-dialog/routine controls with adaptive full-screen or sticky-action sheet layouts and fix untranslated labels/semantics.
- regression risk: Medium. Layout changes can affect workout speed and routine generation conversion.
- minimal verification command/check: Emulator/device smoke at 360x640 and font scale 1.3+ with `uiautomator dump`; inspect for clipped bounds, `NAF="true"`, and English copy.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/RoutineDialogs.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutDialogPresentationPolicyTest.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`
- verification evidence:
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutDialogPresentationPolicyTest" --console=plain`
  - After-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutDialogPresentationPolicyTest" --console=plain`
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain`
  - After-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain`
  - After-change PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain`
  - After-change PASS: `./gradlew.bat :app:test --console=plain`
  - After-change PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain`
  - After-change PASS: `./gradlew.bat :app:test --console=plain`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 12422`; crash buffer was empty.
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 8696`; crash buffer was empty.
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain`
  - TIMED OUT: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain` exceeded the two-minute command timeout without returning output.
  - After-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 10652`; crash buffer was empty.
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 8548`; crash buffer was empty.
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - After-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 8563`; crash buffer was empty.
  - 2026-05-11 compact/font-scale baseline PASS/PARTIAL: SM-S931B font scale 1.3 and 1.5 Training dumps had `NAF=0`, no stale English copy, and empty crash slices; the AI routine dialog had `NAF=0` at 1.3 but `NAF=1` at 1.5 for the equipment text field.
  - 2026-05-11 after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`.
  - 2026-05-11 after-change PASS: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 after-change PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; SM-S931B font scale 1.5 AI routine dialog dump `font-scale-1.5-after-fix-v2-ai-dialog.xml` recorded `NAF=0`, no stale English copy, `AI-routine genereren`, `Genereren`, and `content-desc="Beschikbaar materiaal"`; crash slice was empty and font scale was restored to `1.0`.
  - 2026-05-11 active-workout compact/font-scale PASS: SM-S931B setup created `QAFontRoutine`, added `Ab Wheel Rollout`, launched active workout, and captured `45-after-copy-active-workout-font-1.5-v2.xml` with `Actieve training`, `Training afronden`, `Ab Wheel Rollout`, `0/3 sets - 8-12 herh.`, `NAF=0`, no stale `Reps`/`Rest`/`8-12 reps` copy, empty crash slice, and restored font scale `1.0`.
- external sources used: None. Local source and tests were sufficient; no Android, Material, accessibility, or Gradle ambiguity blocked this batch.
- remaining risk: AI routine preview metadata, active set metric copy, active-workout rest/status metrics, rest timer icon-only actions, and first active-workout row semantics are less likely to expose no-op controls, stale English labels, context-free labels, or uneven metric columns. AI routine dialog and representative active-workout font-scale 1.5 semantics are verified on one 360dp-class physical device; deeper active-workout rows after logged sets and manual TalkBack/Switch Access verification remain open.

### QA-2026-05-09-012

- finding_id: QA-2026-05-09-012
- priority: P2
- area: tests, accessibility
- status: partially-done
- owner suggestion: Android QA owner
- current evidence with file references:
  - Current dirty worktree adds shared line chart semantics tests in `TrainIQ-Project/app/src/test/java/com/trainiq/core/ui/LineChartSemanticsTest.kt` and `TrainIQ-Project/app/src/androidTest/java/com/trainiq/core/ui/AppLineChartAccessibilityTest.kt`.
  - Current worktree adds a generated routine preview source guard in `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutDialogPresentationPolicyTest.kt`.
  - Current worktree adds active/routine set metric label guards in `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`.
  - Current worktree adds an active-workout rest status label guard in `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`.
  - Current worktree adds a merged active-workout sticky status semantics summary in `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`, with a unit guard in `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`.
  - Current worktree adds a merged active-workout bottom bar semantics summary in `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`, with a unit guard in `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`.
  - Current worktree adds merged status metric accessibility summaries in `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`, with unit guards in `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`.
  - Current worktree adds a merged rest-timer card semantics summary in `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`, with a unit guard in `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`.
  - Current worktree adds rest timer action-description guards, including a contextual skip action label, in `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`.
  - Current worktree adds Health Connect rationale reason guards in `TrainIQ-Project/app/src/test/java/com/trainiq/core/health/HealthConnectReadPermissionsTest.kt`.
  - Current worktree adds Settings destructive action confirmation copy guards in `TrainIQ-Project/app/src/test/java/com/trainiq/features/settings/SettingsUiStateTest.kt`.
  - Current worktree adds scanner permission-gate copy guards in `TrainIQ-Project/app/src/test/java/com/trainiq/features/nutrition/CameraScannerStateTest.kt`.
  - Current worktree adds scanner processing/completed/empty sheet state and action copy guards in `TrainIQ-Project/app/src/test/java/com/trainiq/features/nutrition/CameraScannerStateTest.kt`.
  - Signed manual accessibility coverage still lacks release evidence.
- expected target-state behavior: Dense custom surfaces have at least basic automated accessibility coverage in addition to manual TalkBack/Switch Access certification.
- concrete recommended fix: Keep the new chart semantics tests and add Compose UI or instrumentation assertions for active workout controls, AI routine generation, scanner states, Settings destructive actions, and Health Connect rationale. Enable broader Android accessibility checks where compatible with the stack.
- regression risk: Low. Some checks can be flaky if they depend on rendered text or device configuration.
- minimal verification command/check: `./gradlew.bat :app:connectedDebugAndroidTest --console=plain`.
- files changed:
  - `TrainIQ-Project/app/src/androidTest/java/com/trainiq/core/ui/AppLineChartAccessibilityTest.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/core/ui/LineChartSemanticsTest.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutDialogPresentationPolicyTest.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`
- verification evidence:
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.core.ui.LineChartSemanticsTest" :app:compileDebugAndroidTestKotlin --console=plain`
  - PASS: `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.core.ui.AppLineChartAccessibilityTest" --console=plain` on `emulator-5554`
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutDialogPresentationPolicyTest" --console=plain`
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain`
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain`
  - PASS: `./gradlew.bat :app:test --console=plain`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 12422`; crash buffer was empty.
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 8696`; crash buffer was empty.
  - TIMED OUT: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain` exceeded the two-minute command timeout without returning output.
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 10652`; crash buffer was empty.
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.core.health.HealthConnectReadPermissionsTest" --tests "*Settings*" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.core.health.HealthConnectReadPermissionsTest" --tests "com.trainiq.features.settings.SettingsUiStateTest" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 9589`; crash buffer was empty.
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.CameraScannerStateTest" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.CameraScannerStateTest" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 9120`; crash buffer was empty.
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.CameraScannerStateTest" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.CameraScannerStateTest" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 8724`; crash buffer was empty.
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 8611`; crash buffer was empty.
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 9828`; crash buffer was empty.
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 8744`; crash buffer was empty.
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - FAIL then fixed: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache` failed at compile because the new test referenced missing `sampleWorkoutDay`; the test now constructs a minimal `WorkoutDay` inline.
  - PASS after fix: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 9717`; crash buffer was empty.
  - Baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`
  - PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`
  - Emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 8563`; crash buffer was empty.
- remaining risk: Automated coverage now exists for shared line chart semantics, generated routine preview presentation/copy guards, active/routine set metric label guards, active-workout sticky status summary semantics, active-workout bottom bar summary semantics, status metric summary semantics, rest-timer card summary semantics, rest timer action descriptions including skip, Health Connect rationale reasons, Settings destructive confirmation copy, camera fallback policy/copy, scanner permission-gate copy, scanner sheet state/action copy, and shared nutrition field label semantics, but signed manual accessibility coverage remains open.
  - 2026-05-11 scanner/nutrition compact-font PASS/PARTIAL: SM-S931B font scale 1.5 evidence in `TrainIQ-Project/.codex/device-qa/2026-05-11-scanner-fontscale-qa/` verified the Nutrition first viewport and meal source sheet with `NAF=0`; `Foto / AI-inschatting` was visible but disabled because AI was not configured/enabled on this device. The manual product form first reproduced `NAF=2` on the clipped numeric field row, then `NutritionTextField` and `NutritionNumberField` gained merged label semantics. Focused `CameraScannerStateTest`, broad Gradle gate, debug reinstall, `50-after-number-field-product-form-top.xml`, and `51-after-number-field-product-form-lower.xml` passed with `NAF=0`, product/barcode/numeric labels visible, `content-desc` on `kcal / 100g` and `Eiwit / 100g`, `Barcode scannen`/`Product opslaan` visible in the lower dump, empty crash slice, and restored font scale `1.0`. Follow-up barcode launch evidence in `TrainIQ-Project/.codex/device-qa/2026-05-11-barcode-scanner-fontscale-qa/04-after-barcode-tap.xml` reached the camera permission gate with `Cameratoegang nodig`, `Geef cameratoegang om de scanner te gebruiken.`, `Toegang geven`, `Terug`, `NAF=0`, empty crash slice, and restored font scale `1.0`. Post-permission preview evidence in `TrainIQ-Project/.codex/device-qa/2026-05-11-barcode-scanner-preview-fontscale-qa/05-barcode-preview.xml` temporarily granted camera permission, reached `Barcodescanner`, `Richt de camera op de barcode van het product.`, `Annuleren`, `NAF=0`, empty crash slice, restored font scale `1.0`, and restored camera permission to its original denied state.
  - 2026-05-11 Health Connect rationale compact-font PASS: non-mutating SM-S931B font scale 1.5 evidence in `TrainIQ-Project/.codex/device-qa/2026-05-11-health-connect-rationale-fontscale-qa/` launched `HealthConnectPermissionsRationaleActivity` directly without tapping the permission button. `02-top-font-1.5.xml`, `03-scrolled-font-1.5.xml`, and `06-bottom-font-1.5.xml` passed with `NAF=0`, visible rationale text and permission reasons, visible `TrainIQ verbinden`, `Health Connect-toegang geven`, and `Doorgaan naar TrainIQ`; `09-logcat-trainiq-crash.txt` was empty and font scale was restored to `1.0`.
  - 2026-05-11 Settings destructive-actions compact-font PASS: SM-S931B font scale 1.5 evidence in `TrainIQ-Project/.codex/device-qa/2026-05-11-settings-destructive-fontscale-qa/` safely opened and canceled all destructive confirmation dialogs. Pre-fix evidence found a clipped Health Connect action button with `NAF=1`; `SettingsSection.kt` now gives Health Connect Settings actions explicit semantics labels and `SettingsUiStateTest` guards those labels. Focused `SettingsUiStateTest`, debug reinstall, broad Gradle gate, `71-after-fix-health-clipped.xml`, `72-after-fix-profile-actions.xml`, and `80` through `82` dialog dumps passed with `NAF=0`, visible `Annuleren`/`Bevestigen`, empty TrainIQ crash slice, and restored font scale `1.0`.
  - 2026-05-11 360x640 emulator compact-font PARTIAL/PASS: `Medium_Phone` was launched headless with `-skin 360x640`, installed the current debug build, and captured font scale 1.5 evidence in `TrainIQ-Project/.codex/device-qa/2026-05-11-360x640-emulator-fontscale-qa/`. Initial evidence showed the fixed-height labeled bottom nav and shared header consumed nearly the whole first viewport. `TrainIqNav.kt` now condenses compact short-height bottom navigation, `AppDesign.kt` uses a denser shared header on screens `<= 640dp` high, and tests guard the short-screen policy. Focused navigation/workout/settings tests and the broad Gradle gate passed. Rerun evidence `201-after-header-fix-home-font-1.5.xml`, `202-after-header-fix-settings-font-1.5.xml`, and `203-after-header-fix-home-multiscroll-font-1.5.xml` passed with `NAF=0`, successful cold launch, empty TrainIQ crash slice, restored font scale, and scroll-reachable Home body copy. Remaining risk: the first 360x640 font scale 1.5 viewport still prioritizes header/nav over rich content, so broader 360x640 screen-by-screen design signoff remains open.
  - 2026-05-11 360x640 Training compact-font PASS/PARTIAL: debug and profileable emulator evidence in `TrainIQ-Project/.codex/device-qa/2026-05-11-360x640-training-fontscale-qa/`, `TrainIQ-Project/.codex/device-qa/2026-05-11-360x640-active-workout-fontscale-qa/`, and `TrainIQ-Project/.codex/device-qa/2026-05-11-360x640-active-workout-route-after-compact-set-type/` verified Training top/scrolled/routine-detail plus seeded active-workout top/scrolled states at font scale 1.5 with `NAF=0`, empty TrainIQ crash slices, and restored font scale. Profileable benchmark seed made `Benchmark routine` and `Training starten` reachable on the disposable emulator. Pre-fix deeper Training scroll evidence exposed clipped routine-creation buttons with `NAF=1`; `RoutineCreationCard` now adds explicit `Lege routine maken` and `Met AI genereren` semantics labels, guarded by `WorkoutInputValidationTest`. Pre-fix active-workout route evidence exposed a clipped scrolled set-type chip with `NAF=1`; active-workout set-type selection now uses the existing compact dropdown mode on `<= 640dp` high screens, guarded by `WorkoutInputValidationTest`. Focused workout tests and the profileable build passed. Remaining risk: broader 360x640 screen-by-screen design signoff remains partial.
  - 2026-05-12 360x640 top-level compact-font PASS/PARTIAL: debug emulator evidence in `TrainIQ-Project/.codex/device-qa/2026-05-12-360x640-top-level-fontscale-qa/` captured Start, Training, Voeding, Coach, Instellingen, and Settings-to-Voortgang top/scrolled states at font scale 1.5 with `NAF=0` and empty crash buffers. Remaining risk: this is UIAutomator/font-scale smoke evidence, not manual TalkBack/Switch Access signoff or full interaction coverage for every nested flow.

### QA-2026-05-09-013

- finding_id: QA-2026-05-09-013
- priority: P2
- area: Android lifecycle, UX
- status: done
- owner suggestion: Android camera/nutrition owner
- current evidence with file references:
  - `TrainIQ-Project/app/src/main/AndroidManifest.xml:14` declares `android.hardware.camera` as `required="false"`.
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/nutrition/CameraScannerScreen.kt:352` creates a `LifecycleCameraController`.
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/nutrition/CameraScannerScreen.kt:364` calls `controller.bindToLifecycle(lifecycleOwner)` when permission is granted, without a visible `runCatching`/fallback path around bind failure.
- expected target-state behavior: Scanner entry handles no usable camera, CameraX bind failure, revoked camera, and device-specific camera errors with a clear user-facing fallback instead of crash or blank scanner.
- concrete recommended fix: Check camera feature/camera provider availability before showing scanner actions, wrap camera binding in a failure path that sets `cameraError`, and provide manual meal/barcode fallback actions. Add a fake/no-camera or bind-failure state test.
- regression risk: Low to medium. Scanner startup behavior can change on devices where CameraX initializes slowly; keep retry/manual fallback available.
- minimal verification command/check: Camera scanner emulator/device smoke with camera disabled or unavailable where possible, plus `./gradlew.bat :app:testDebugUnitTest --tests "*Camera*" --console=plain`.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/nutrition/CameraScannerScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/nutrition/CameraScannerStateTest.kt`
- verification evidence:
  - RED: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.CameraScannerStateTest" --console=plain` failed while camera fallback helpers were absent.
  - PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.CameraScannerStateTest" --console=plain`
- remaining risk: Unit coverage verifies fallback policy and copy. Device smoke with a physically unavailable/disabled camera was not available in this pass. At font scale 1.5, the source sheet, AI-disabled photo action, manual product form, barcode scanner permission gate, AI scanner permission-gate rotate/recreate path, and post-permission barcode preview rendered without NAF/crash. Real barcode recognition and AI photo capture still need manual/device signoff with safe camera usage.

## Refresh Audit - 2026-05-10

Audit scope: full target-state QA refresh against `TrainIQ_Target_State_Blueprint.md`, current Android source, build/test config, existing QA docs, release docs, emulator availability, and official Android/Gemini documentation.

### QA-2026-05-10-014

- finding_id: QA-2026-05-10-014
- priority: P0
- area: data, performance, Android lifecycle
- status: partially-done
- owner suggestion: Android data/platform owner
- current evidence with file references:
  - `RoomTrainIqRuntimeStore.update(transform)` has been removed. The only remaining full-state JSON import path inside `RoomTrainIqRuntimeStore` is the private one-time legacy seed from `TrainIqLocalStore.exportLegacyState()`, and explicit mirror-run/dry-run infrastructure remains under `TrainIQ-Project/app/src/main/java/com/trainiq/data/migration`.
  - Known repository mutation hot paths and delayed exercise-library seeding now use targeted Room writes; remaining work is to expand runtime QA and decide whether the private one-time legacy seed can be deleted after migration support is no longer needed.
  - Routine create/update/delete, exercise reorder, workout day add/remove, workout exercise add/remove, and routine set add/edit/delete/move now use targeted Room writes.
  - 2026-05-11 source scan confirms no `runtimeStore.update(...)` callers remain under `TrainIQ-Project/app/src/main/java/com/trainiq`; recipe/food mutations, active set editing/deletion, meal save/delete, profile save/reset, body measurement add/delete, and exercise-library seeding now use targeted Room writes.
  - 2026-05-10 emulator smoke installed the app and reached Home, but `adb shell am start -W -n com.trainiq/.MainActivity` returned `Status: timeout`, `WaitTime: 20254`; crash buffer was empty.
- external sources used:
  - None for the local persistence finding. Local source and emulator evidence were sufficient.
- expected target-state behavior: Normal user mutations use bounded targeted DAO transactions. Startup and critical input paths avoid full-state JSON serialization, broad import planning, or broad Room mirror replacement.
- concrete recommended fix: Continue QA-2026-05-09-001 one flow at a time: remaining evidence gaps should receive targeted process-restart correctness tests and runtime QA.
- regression risk: High. Persistence changes can resurrect deleted rows or lose active workout data if transaction boundaries are wrong.
- minimal verification command/check: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`, repository process-restart tests for each migrated path, `./gradlew.bat :app:connectedDebugAndroidTest --console=plain --no-configuration-cache`, and emulator launch/logcat smoke.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/core/database/TrainIqDao.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/RoomTrainIqRuntimeStore.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/TrainIqRepository.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/ExerciseLibrarySeeder.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/architecture/RoomAuthorityArchitectureTest.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/data/repository/ExerciseLibrarySeederTest.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/data/repository/WorkoutSessionTransactionTest.kt`
- verification evidence:
  - 2026-05-10 baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set edit after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set edit after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set edit after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set edit after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-collapse after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set-type edit after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set-type edit after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set-type edit after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set-type edit after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set delete after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set delete after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set delete after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 active-set delete after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 meal persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 meal persistence after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 meal persistence after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 meal persistence after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 profile persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 profile persistence after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 profile persistence after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 profile persistence after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 routine-set edit persistence after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 routine-set edit persistence after-change PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 routine-set edit persistence after-change PASS: `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
  - 2026-05-10 routine-set edit persistence after-change PASS: `./gradlew.bat :app:lintDebug --console=plain --no-configuration-cache`.
  - 2026-05-11 exercise-library seeding after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.repository.ExerciseLibrarySeederTest" --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-11 exercise-library seeding source scan PASS: `rg "runtimeStore\.update\(" TrainIQ-Project/app/src/main/java/com/trainiq -n` returned no matches.
  - 2026-05-11 exercise-library seeding broad gate PASS: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 exercise-library seeding device smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; SM-S931B cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 710`; after waiting for the delayed seed job, `logcat-crash-slice.txt` was empty. Evidence: `TrainIQ-Project/.codex/device-qa/2026-05-11-post-exercise-seed-launch/`.
  - 2026-05-11 runtime update API removal PASS: focused `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.repository.WorkoutSessionTransactionTest" --tests "com.trainiq.architecture.RoomAuthorityArchitectureTest" --console=plain --no-configuration-cache`.
  - 2026-05-11 runtime update API removal PASS: broad `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 runtime update API removal PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; SM-S931B cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 698`; after startup delay, `logcat-crash-slice.txt` was empty. Evidence: `TrainIQ-Project/.codex/device-qa/2026-05-11-post-runtime-update-removal-launch/`.
  - 2026-05-11 post-removal connected persistence PASS: `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain --no-configuration-cache`; report `TEST-SM-S931B - 16-_app-.xml` recorded `tests="26" failures="0" errors="0" skipped="0"`.
- remaining risk: Normal app-source callers cannot use `RoomTrainIqRuntimeStore.update(transform)` because the API has been removed. Full closure still needs broader runtime QA, and a later engineering decision on when the private one-time legacy seed can be deleted after migration support is no longer needed.

### QA-2026-05-10-015

- finding_id: QA-2026-05-10-015
- priority: P0
- area: release, accessibility
- status: blocked
- owner suggestion: accessibility/manual QA owner
- current evidence with file references:
  - `TrainIQ-Project/docs/qa/human-assistive-tech-qa-signoff.md:24` through `TrainIQ-Project/docs/qa/human-assistive-tech-qa-signoff.md:35` still list all critical TalkBack/Switch Access flows as `NOT_RUN`.
  - `TrainIQ-Project/docs/qa/human-assistive-tech-qa-signoff.md:39` through `TrainIQ-Project/docs/qa/human-assistive-tech-qa-signoff.md:44` show all release signoff checkboxes unchecked.
  - `TrainIQ-Project/docs/release/owner-action-tracker.md:13` keeps A11Y-001 open and release-blocking.
- external sources used:
  - Android Developers, Compose accessibility and scalable content docs, accessed 2026-05-10: https://developer.android.com/develop/ui/compose/accessibility and https://developer.android.com/develop/ui/compose/accessibility/scalable-content
- expected target-state behavior: Critical flows have signed TalkBack, Switch Access, large font, and dark-mode evidence before release.
- concrete recommended fix: Run the existing manual assistive-tech script on the current build, attach evidence paths, update the signoff file, and file code issues for failed flows.
- regression risk: Low for documentation; medium if UI fixes are required after manual QA.
- minimal verification command/check: Complete `TrainIQ-Project/docs/qa/talkback-switch-access-test-script.md` and update `TrainIQ-Project/docs/qa/human-assistive-tech-qa-signoff.md` with tester/device/build/font/theme evidence.

### QA-2026-05-10-016

- finding_id: QA-2026-05-10-016
- priority: P1
- area: UI, UX, accessibility
- status: partially-done
- owner suggestion: Android UI owner
- current evidence with file references:
  - 2026-05-10 polish: `TrainIQ-Project/app/src/main/java/com/trainiq/core/ui/AppDesign.kt` no longer forces shared `AppScreenHeader` title/subtitle text into ellipsized one-line/two-line clamps.
  - 2026-05-10 polish: `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt` no longer disables wrapping for the routine set index/type labels.
  - 2026-05-10 polish: `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt` now guards those critical shared header and routine set label wrapping constraints.
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt` still presents AI routine generation in an `AlertDialog`, but 2026-05-11 SM-S931B compact/font-scale evidence now proves the current first-viewport dialog state has labeled controls at font scale 1.5 after adding the equipment field accessibility label.
- external sources used:
  - Android Developers, Compose scalable content, accessed 2026-05-10: https://developer.android.com/develop/ui/compose/accessibility/scalable-content
- expected target-state behavior: Critical titles, workout labels, generated routine previews, and action areas wrap/reflow deliberately at 360x640, 360x800, font scale 1.3 and 1.5, without hiding essential context or actions.
- concrete recommended fix: Keep the shared header/routine-set wrapping guards and AI equipment accessibility label in place; next, verify deeper scrolled AI routine controls and active-workout dense rows at compact width/font scale before deciding whether a full-screen/sheet conversion is still needed.
- regression risk: Medium. Wrapping can increase vertical pressure on dense workout screens, so verify compact layouts after changes.
- verification evidence:
  - 2026-05-10 baseline PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 RED: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest.critical headers and set labels allow wrapping at large font scale" --console=plain --no-configuration-cache` failed while the wrapping guard detected the old clamps.
  - 2026-05-10 after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest.critical headers and set labels allow wrapping at large font scale" --console=plain --no-configuration-cache`.
  - 2026-05-10 after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`.
  - 2026-05-10 after-change PASS: `./gradlew.bat :app:assembleDebug :app:test :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-10 emulator smoke PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; `adb shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 11218`; Training tab UI dump rendered; crash buffer was empty.
  - 2026-05-11 baseline PASS/PARTIAL: SM-S931B font scale 1.3/1.5 Training dumps had `NAF=0`, no stale English copy, and empty crash slices; AI routine dialog had `NAF=0` at 1.3 and `NAF=1` at 1.5 before the equipment-label fix.
  - 2026-05-11 after-change PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`.
  - 2026-05-11 after-change PASS: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 after-change PASS: `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; SM-S931B font scale 1.5 AI routine dialog dump recorded `NAF=0`, no stale English copy, `content-desc="Beschikbaar materiaal"`, visible `AI-routine genereren` and `Genereren`, empty crash slice, and font scale restored to `1.0`.
  - 2026-05-11 active-workout compact/font-scale PASS: routine-detail setup first exposed a partially visible `Sessie toevoegen` field with `NAF=1`; after adding `accessibilityLabel = "Sessienaam optioneel"`, the same routine-detail state recorded `NAF=0`, `content-desc="Sessienaam optioneel"`, no stale English copy, and empty crash slice.
  - 2026-05-11 active-workout compact/font-scale PASS: after replacing the active exercise summary copy from `8-12 reps` to `8-12 herh.`, focused `WorkoutInputValidationTest`, broad `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin`, reinstall, and SM-S931B active-workout font scale 1.5 UI dump all passed; the final dump recorded `Actieve training`, `Training afronden`, `Ab Wheel Rollout`, `NAF=0`, no stale English copy, and an empty crash slice.
  - 2026-05-11 logged-set edit compact/font-scale PASS: SM-S931B runtime QA in `TrainIQ-Project/.codex/device-qa/2026-05-11-active-workout-logged-set-fontscale-qa/` started `QAFontRoutine`, logged one `Ab Wheel Rollout` set, opened the logged-set correction state, and captured `18-edit-logged-set-font-1.5.xml` with `NAF=0`, visible `Gewicht`, `Reps`, `RPE`, `Zelfde opnieuw`, `Training afronden`, and empty `19-font-1.5-logcat-errors.txt`; font scale was restored to `1.0`. Earlier `NAF=1/2` dumps were clipped offscreen/top-viewport icon-button artifacts and disappeared when the relevant controls were fully visible.
  - 2026-05-11 deeper AI routine compact/font-scale PASS: SM-S931B runtime QA in `TrainIQ-Project/.codex/device-qa/2026-05-11-ai-routine-deeper-fontscale-qa/` reproduced a pre-fix `NAF=1` deload switch in `33-ai-dialog-after-precise-scroll.xml`, then added a switch content description and verified `38-after-fix-ai-dialog-scrolled-controls.xml` with `Ervaringsniveau`, `Beginner`, `Gemiddeld`, `Gevorderd`, `Sessieduur: 60 min`, `Deload-richtlijn opnemen`, `content-desc="Deload-richtlijn opnemen"`, `NAF=0`, empty `39-after-fix-logcat-errors.txt`, and restored font scale `1.0`.
- files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/core/ui/AppDesign.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`
- remaining risk: First-viewport and deeper scrolled AI routine dialog behavior, the routine-detail session-name field, a representative active-workout first exercise row, visible logger controls, logged-set correction controls, seeded 360x640 active-workout top/scrolled states, and top-level 360x640 Start/Training/Voeding/Coach/Instellingen/Voortgang states are verified at font scale 1.5. Manual TalkBack/Switch Access and deeper nested-flow interaction evidence remain open.
- minimal verification command/check: Compact emulator UI dump/screenshot pass for active workout and AI routine generation at 360x640/360x800 and font scale 1.3/1.5.

### QA-2026-05-10-017

- finding_id: QA-2026-05-10-017
- priority: P1
- area: performance, release
- status: blocked
- owner suggestion: product/Android performance owner
- current evidence with file references:
  - `TrainIQ-Project/docs/qa/performance-threshold-decision-record.md:15` through `TrainIQ-Project/docs/qa/performance-threshold-decision-record.md:19` still require product confirmation for startup and frame-jank thresholds.
  - `TrainIQ-Project/macrobenchmark/src/main/java/com/trainiq/macrobenchmark/TrainIqStartupBenchmark.java:31` through `TrainIQ-Project/macrobenchmark/src/main/java/com/trainiq/macrobenchmark/TrainIqStartupBenchmark.java:82` define baseline profile and macrobenchmark coverage, but current release docs still require physical-device evidence.
  - 2026-05-10 emulator launch reached Home with empty crash buffer but `am start -W` timed out at `WaitTime: 20254`; `dumpsys gfxinfo com.trainiq framestats` returned `Failure while dumping the app`.
- external sources used:
  - Android Developers, Baseline Profiles overview and Create Baseline Profiles, accessed 2026-05-10: https://developer.android.com/baseline-profiles and https://developer.android.com/topic/performance/baselineprofiles/create-baselineprofile
- expected target-state behavior: Release/profileable startup, top-level navigation, settings scroll, scanner launch, and active workout logging have approved thresholds and physical-device macrobenchmark evidence.
- concrete recommended fix: Approve numeric thresholds, run the device-lab performance plan on at least one lower-end and one representative modern physical device, and keep debug-emulator timeout as a signal until release/profileable evidence explains or eliminates it.
- regression risk: Medium. Performance fixes may touch startup data flow, baseline profile generation, or Compose initialization.
- minimal verification command/check: `./gradlew.bat :macrobenchmark:connectedProfileableAndroidTest --console=plain --no-configuration-cache` on physical devices, plus `adb shell am start -W -n com.trainiq/.MainActivity` without timeout.

### QA-2026-05-10-018

- finding_id: QA-2026-05-10-018
- priority: P1
- area: privacy, security, release
- status: needs-decision
- owner suggestion: product/backend/security/legal owner
- current evidence with file references:
  - `TrainIQ-Project/docs/release/owner-action-tracker.md:11` through `TrainIQ-Project/docs/release/owner-action-tracker.md:14` keep Data Safety, performance, accessibility, and AI owner gates open.
  - `TrainIQ-Project/docs/release/owner-action-tracker.md:33` through `TrainIQ-Project/docs/release/owner-action-tracker.md:38` state release remains blocked until those gates are approved.
  - `TrainIQ-Project/app/src/main/AndroidManifest.xml:12` declares `android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND`, which needs release justification and owner confirmation.
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/remote/GeminiApi.kt:10` through `TrainIQ-Project/app/src/main/java/com/trainiq/data/remote/GeminiApi.kt:16` correctly keep Gemini key transport in the `x-goog-api-key` header, but production BYOK/gateway/OAuth mode remains an owner decision.
- external sources used:
  - Android Developers, Health Connect sync data, accessed 2026-05-10: https://developer.android.com/health-and-fitness/health-connect/sync-data
  - Google AI for Developers, Gemini thinking and structured output docs, accessed 2026-05-10: https://ai.google.dev/gemini-api/docs/thinking and https://ai.google.dev/gemini-api/docs/structured-output
- expected target-state behavior: Play/Data Safety, privacy policy, Health Connect background read, signing ownership, and production AI boundary are approved before release.
- concrete recommended fix: Close LEGAL-001, AI-001, and background Health Connect justification with owner evidence; update Data Safety/privacy docs to match the final AI and telemetry mode.
- regression risk: High for release/compliance accuracy if docs diverge from implementation.
- minimal verification command/check: Owner review of `TrainIQ-Project/docs/release/owner-action-tracker.md`, `TrainIQ-Project/docs/architecture/production-ai-boundary-decision-gate.md`, Data Safety worksheet, and privacy-policy draft.

### QA-2026-05-10-019

- finding_id: QA-2026-05-10-019
- priority: P2
- area: Android lifecycle, data
- status: partially-done
- owner suggestion: Android Health Connect owner
- current evidence with file references:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt:90` through `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt:99` gate background reads on SDK availability, feature availability, and granted background permission.
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt:193` through `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt:221` support per-metric sync state.
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt:325` through `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt:336` use per-record-type changes tokens.
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt:348` through `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt:367` page `readRecords` calls.
  - 2026-05-10 physical-device evidence on SM-S931B confirmed the app launches, Settings renders Health Connect status copy (`Health Connect: Toegang vereist`), the Android Health Connect controller package is present as `com.google.android.healthconnect.controller`, and crash buffers after launch/settings were empty under `TrainIQ-Project/.codex/device-qa/2026-05-10-health-connect-runtime/`.
  - 2026-05-10 follow-up evidence found the physical device exposes Health Connect as `com.google.android.healthconnect.controller`; `AndroidManifest.xml` now declares visibility for that package and `com.android.vending` in addition to the older Play Health Connect package used by the provider-update onboarding intent.
  - 2026-05-11 repeatable non-mutating evidence collection is available at `TrainIQ-Project/scripts/collect-health-connect-runtime-evidence.ps1`; latest SM-S931B output is `TrainIQ-Project/.codex/device-qa/2026-05-11-health-connect-scripted-baseline-debug-v4/`.
  - End-to-end provider-missing, revoked permission, partial permission, and background-read flows remain unexecuted in this audit.
- external sources used:
  - Android Developers, Health Connect sync data, accessed 2026-05-10: https://developer.android.com/health-and-fitness/health-connect/sync-data
- expected target-state behavior: Health Connect behaves correctly across provider missing/update required, no permission, partial permission, revoked permission while open, and background-read availability states.
- concrete recommended fix: Extend the device/emulator Health Connect smoke from the current no-permission Settings evidence to provider missing/update, partial grants, revocation while app is open, and background-read unavailable/granted states; attach UI dumps/logcat evidence.
- regression risk: Medium. Permission flow fixes can alter consent clarity or accidentally block partial metrics.
- minimal verification command/check: Emulator/device manual script covering Health Connect provider and permission states, plus `./gradlew.bat :app:testDebugUnitTest --tests "*HealthConnect*" --console=plain --no-configuration-cache`.
- verification evidence:
  - 2026-05-10 PASS: `./gradlew.bat :app:testDebugUnitTest --tests "*HealthConnect*" --console=plain --no-configuration-cache`.
  - 2026-05-10 PARTIAL: SM-S931B launch/settings Health Connect smoke captured `health-packages.txt`, `launch-main.txt`, Settings UI dumps, and empty crash buffers in `TrainIQ-Project/.codex/device-qa/2026-05-10-health-connect-runtime/`.
  - 2026-05-10 PASS: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.core.health.HealthConnectReadPermissionsTest" --tests "*HealthConnect*" --console=plain --no-configuration-cache`.
  - 2026-05-10 PASS: `./gradlew.bat :app:processDebugMainManifest :app:assembleDebug :app:lintDebug --console=plain --no-configuration-cache`.
  - 2026-05-10 PASS: post-manifest visibility `:app:installDebug` and SM-S931B launch returned `Status: ok`, `WaitTime: 708`; evidence is in `TrainIQ-Project/.codex/device-qa/2026-05-10-health-connect-followup/`.
  - 2026-05-11 PASS: added a source-level regression guard that `AndroidManifest.xml` still declares `android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND`, because `HealthConnectBackgroundSyncScheduler` depends on `HealthConnectDataSource.canReadInBackground()` and the release owner gate depends on this permission being explicit.
  - 2026-05-11 PASS: `./gradlew.bat :app:testDebugUnitTest --tests "*HealthConnect*" --console=plain --no-configuration-cache`.
  - 2026-05-11 PASS: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - 2026-05-11 NOT_RUN: provider-missing, partial-grant, revoke-while-open, and background-read granted/unavailable runtime cases were not executed because the connected SM-S931B is a real device with Health Connect installed and no disposable permission profile was confirmed.
  - 2026-05-11 PASS/PARTIAL: `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\collect-health-connect-runtime-evidence.ps1 -AdbPath 'C:\Users\menno\AppData\Local\Android\Sdk\platform-tools\adb.exe' -OutputDir '.codex\device-qa\2026-05-11-health-connect-scripted-baseline-debug-v4'` captured cold main launch (`Status: ok`, `WaitTime: 706`), rationale launch (`Status: ok`), Health Connect manage-access launch (`Status: ok`), all requested health permissions ungranted, and an empty crash slice. Mutable provider/permission cases remain `NOT_RUN`.
  - 2026-05-11 PASS: non-mutating Health Connect rationale compact-font smoke in `TrainIQ-Project/.codex/device-qa/2026-05-11-health-connect-rationale-fontscale-qa/` launched `com.trainiq.core.health.HealthConnectPermissionsRationaleActivity` directly at font scale 1.5, captured top/scrolled/bottom UI dumps with `NAF=0`, visible rationale reasons and connect actions, empty TrainIQ crash slice, and restored font scale to `1.0`.

### QA-2026-05-10-020

- finding_id: QA-2026-05-10-020
- priority: P1
- area: tests, release
- status: done
- owner suggestion: Android/release owner
- current evidence with file references:
  - `TrainIQ-Project/app/build.gradle.kts:162` through `TrainIQ-Project/app/build.gradle.kts:255` define Room migration-chain marker generation tasks.
  - `.github/workflows/android.yml:28` through `.github/workflows/android.yml:41` run unit tests, lint, Android test compilation, macrobenchmark compilation, and signing readiness, but do not run marker generation.
  - `.github/workflows/android.yml:78` builds signed release artifacts with `:app:checkReleaseSigningReadiness :app:assembleRelease :app:bundleRelease`, but does not require `generateReleaseRoomMigrationChainVerificationMarker`.
  - `TrainIQ_Target_State_Blueprint.md` requires release artifacts to be blocked without fresh migration-marker evidence or an owner-approved exception.
- external sources used:
  - None. Local Gradle and CI config were sufficient.
- expected target-state behavior: Release artifacts require fresh Room migration-chain runtime proof or an explicit owner-approved exception.
- concrete recommended fix: Wire `generateCiRoomMigrationChainVerificationMarkers` or release-specific marker generation into CI/release jobs, or document the marker as diagnostic-only and update the blueprint/release gates accordingly.
- regression risk: Medium. CI runtime can increase substantially because marker generation depends on connected tests.
- minimal verification command/check: CI job or local equivalent runs `./gradlew.bat :app:generateReleaseRoomMigrationChainVerificationMarker --console=plain --no-configuration-cache` before release artifact generation.
- files changed:
  - `.github/workflows/android.yml`
- verification evidence:
  - 2026-05-10 polish: `.github/workflows/android.yml` now adds a `room-migration-marker` job that runs `:app:generateCiRoomMigrationChainVerificationMarkers` inside an Android emulator runner.
  - 2026-05-10 polish: `signed-release` now depends on both `validate` and `room-migration-marker`, so signed release artifacts cannot be built by the workflow unless the Room migration marker gate passes.
  - 2026-05-10 PASS: `./gradlew.bat :app:generateCiRoomMigrationChainVerificationMarkers --dry-run --console=plain --no-configuration-cache`.
  - 2026-05-10 PASS: `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
- external sources used: None. Local Gradle task wiring and workflow scope were sufficient.
- remaining risk: The first GitHub-hosted emulator run can still expose infrastructure issues, but the release workflow is now gated on marker generation instead of treating marker evidence as optional.

### QA-2026-05-10-021

- finding_id: QA-2026-05-10-021
- priority: P3
- area: release
- status: needs-decision
- owner suggestion: release owner
- current evidence with file references:
  - `TrainIQ-Project/app/build.gradle.kts:29` sets `versionCode = 1` and `versionName = "1.0"`.
  - Release owner gates remain open in `TrainIQ-Project/docs/release/owner-action-tracker.md:11` through `TrainIQ-Project/docs/release/owner-action-tracker.md:14`.
- external sources used:
  - None. Local release config was sufficient.
- expected target-state behavior: Play upload uses an owner-approved versioning strategy and release metadata.
- concrete recommended fix: Confirm whether first Play upload should remain `1.0`/`1`, or set a pre-release/internal-track version scheme before signed release generation.
- regression risk: Low for code; medium for release operations if a Play track already used versionCode 1.
- minimal verification command/check: Release owner records versioning decision before `:app:bundleRelease` artifacts are uploaded.

## 2026-05-10 Verification Summary

- `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin :app:checkReleaseSigningReadiness :macrobenchmark:compileProfileableJavaWithJavac --console=plain --no-configuration-cache`: PASS.
- Release-readiness worker also verified `:app:assembleRelease` and `:app:bundleRelease`: PASS; local signing was not configured, so unsigned local release artifacts are expected.
- `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`: PASS on `emulator-5554`.
- `adb -s emulator-5554 shell am start -W -n com.trainiq/.MainActivity`: FAIL/PERF RISK, returned `Status: timeout`, `WaitTime: 20254`; Home rendered in UI dump after launch.
- `adb -s emulator-5554 logcat -d -b crash`: PASS, empty crash buffer.
- `adb -s emulator-5554 shell dumpsys gfxinfo com.trainiq framestats`: FAIL/INCONCLUSIVE, returned `Failure while dumping the app`.
- `:app:lintDebug`: PASS with warnings reported by the release-readiness worker, including blocking `SharedPreferences.commit()` in `AndroidKeystoreGeminiKeyStore.kt`, unused legacy color resources, and dependency update warnings.

## 2026-05-10 Physical Device Normal/Weird Flow QA

- Device: Samsung SM-S931B, Android 16, physical device via `C:\Users\menno\AppData\Local\Android\Sdk\platform-tools\adb.exe`.
- Evidence folder: `.codex/device-qa/2026-05-10-normal-weird-flow/`.
- Build/install: PASS, `./gradlew.bat :app:assembleDebug :app:installDebug --console=plain --no-configuration-cache`.
- Unit verification: PASS, `./gradlew.bat :app:testDebugUnitTest --console=plain --no-configuration-cache`.
- Cold launch after unlock: PASS, `adb shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 721`; Home rendered with onboarding CTA and bottom navigation.
- Force-stop resume: PASS, `WaitTime: 702`; Home rendered after `am force-stop` and relaunch.
- Normal flow coverage: PASS for top-level Start, Training, Voeding, Coach, Meer/Instellingen navigation; settings scroll; empty routine dialog; AI routine generation dialog; nutrition add bottom sheet; manual product entry.
- Weird flow coverage: PASS/no crash for repeated back presses, rapid tab tapping, horizontal swipes, landscape rotation and portrait restore, force-stop resume, and back-stack exits.
- Crash evidence: PASS, `crash-buffer.txt`, `targeted-crash-buffer.txt`, `deep-crash-buffer.txt`, and `scanner-crash-buffer.txt` were empty.
- Findings from this pass:
  - Back-spam exits to the launcher from top-level screens. This appears platform-normal, but it means follow-up automated weird-flow scripts must relaunch before continuing tap sequences.
  - Scanner/photo permission flow was not precisely reached in this coordinate pass; follow-up should target the `Foto / AI-inschatting` action from the nutrition sheet with UIAutomator node bounds instead of approximate taps.

## 2026-05-10 Physical Device Scanner Follow-up QA

- Device: Samsung SM-S931B, Android 16, physical device via `C:\Users\menno\AppData\Local\Android\Sdk\platform-tools\adb.exe`.
- Evidence folder: `.codex/device-qa/2026-05-10-scanner-permission-precise/`.
- Build/install: PASS, `./gradlew.bat :app:assembleDebug :app:installDebug --console=plain --no-configuration-cache`.
- Cold launch: PASS, `adb shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 739`.
- Nutrition source sheet: PASS, `Toevoegen aan Ochtend` sheet rendered with `Handmatig product maken`, `Opgeslagen product gebruiken`, `Opgeslagen recept gebruiken`, `AI-context voor foto`, and `Foto / AI-inschatting`.
- Scanner entry: PASS, direct tap on `Foto / AI-inschatting` node bounds `[72,1842][1008,1986]` opened `Camerascanner`.
- Scanner capture/back flow: PASS, `Foto maken` action remained stable; Back returned from scanner to Voeding, then Back returned to Start.
- Crash evidence: PASS, `crash-buffer.txt`, `direct-crash-buffer.txt`, and `capture-crash-buffer.txt` were empty.
- Remaining risk: Camera permission denial was not shown because the device already allowed or did not prompt during this run. A true denial-path pass still needs app permission reset or a fresh install/user profile before release accessibility signoff.

## 2026-05-10 Physical Device Active-Workout Completion Attempt

- Device: Samsung SM-S931B, Android 16, physical device via `C:\Users\menno\AppData\Local\Android\Sdk\platform-tools\adb.exe`.
- Evidence folder: `.codex/device-qa/2026-05-10-active-workout-completion-debrief/`.
- Cold launch: PASS, `adb shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 771`; a later relaunch after the blocked setup returned `WaitTime: 716`.
- Routine setup: PARTIAL, `Lege routine maken` opened the expected `Routine maken` dialog. After hiding the Samsung keyboard before pressing `Maken`, the app showed `Routine aangemaakt.` and `QA routine` as the active routine.
- Completion/debrief coverage: NOT RUN/BLOCKED. The newly created empty routine showed `Open deze routine hieronder en voeg eerst een trainingsdag met oefening toe voordat je start.`, but no routine-detail, day-add, exercise-add, or workout-start control was visible in the UI dump after repeated taps and scroll attempts.
- Crash evidence: PASS, `crash-buffer.txt` was empty.
- Verification: PASS, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest --console=plain --no-configuration-cache`.
- Follow-up needed: create or expose a reliable QA fixture/setup path for an active routine with at least one workout day, exercise, and routine set, then rerun active-workout finish and completion/debrief runtime QA.

## 2026-05-10 Training Setup Entry Polish

- Files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`
- Fix: empty active routines now expose a visible `Routine inrichten` action from the active-routine card. The action reuses the existing routine detail mode instead of adding a new builder path.
- Regression coverage: source-level tests now guard that the empty active routine card exposes the setup callback/label, the label remains Dutch, and the routine overview keeps the existing `Details` action.
- Verification: PASS, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`.
- Verification: PASS, `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
- Physical-device evidence: PASS on SM-S931B, installed debug build, launched with `WaitTime: 1151`, created empty `QA routine`, verified `Routine inrichten` appears after scrolling the active-routine card, and verified tapping it opens the existing routine detail screen with `Info`/`Sessies`.
- Crash evidence: PASS, `.codex/device-qa/2026-05-10-training-setup-entry-after/final-crash-buffer.txt` was empty.

## 2026-05-10 Training Setup Tab Polish

- Files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`
- Fix: not-startable routines now open routine detail on `Sessies` first, so the `Routine inrichten` path lands where `Eerste oefening toevoegen` is available. Startable routines still open on `Info`, preserving the existing normal detail flow.
- Regression coverage: targeted tests guard the default detail tab for empty routines, routines with empty days, and routines with a startable exercise.
- Verification: RED, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache` failed on missing `initialRoutineDetailTab`.
- Verification: PASS, same targeted test after implementation.
- Verification: PASS, `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
- Remaining risk: runtime device proof should now rerun the full empty-routine setup, add-first-exercise, active-workout finish, and completion/debrief flow.

## 2026-05-10 Training Setup QA Then Copy Polish

- QA scope: reran the targeted workout regression suite against the current setup-entry and setup-tab changes before making another app change.
- QA finding: the empty active-routine helper copy still said to open the routine below, while the UI now exposes a direct `Routine inrichten` action. This was a low-risk UX mismatch in the setup flow.
- Files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`
- Fix: updated the helper copy to `Tik op Routine inrichten en voeg eerst een oefening toe voordat je start.`
- Verification: QA baseline PASS, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`.
- Verification: RED, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest.active routine without exercises explains the editor action" --console=plain --no-configuration-cache` failed on the stale copy.
- Verification: PASS, same targeted test after copy polish.
- Verification: PASS, full `WorkoutInputValidationTest`.
- Verification: PASS, `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
- Remaining risk: physical-device add-first-exercise and completion/debrief runtime QA remains the next step.

## 2026-05-10 Training Setup QA Then Button Polish

- QA scope: reran targeted workout regression coverage for the setup-entry/setup-tab path and inspected the current active-routine card source.
- QA finding: the `Routine inrichten` primary setup action was text-only while comparable add/setup actions in the Training screen use the Add icon for quick scanning. The existing source-level guard also sliced too broadly, so it could pass by seeing Add icons outside `ActiveRoutineCard`.
- Files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`
- Fix: added `Icons.Rounded.Add` to the `Routine inrichten` button and tightened the regression guard to inspect only `ActiveRoutineCard`.
- Verification: QA baseline PASS, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`.
- Verification: RED, targeted setup-entry test failed once the source slice was narrowed and the Add icon was required.
- Verification: PASS, same targeted test after button affordance polish.
- Verification: PASS, full `WorkoutInputValidationTest`.
- Verification: PASS, `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
- Remaining risk: physical-device add-first-exercise and completion/debrief runtime QA remains the next step.

## 2026-05-10 Training Setup Runtime QA Then Scroll Polish

- QA scope: physical-device runtime QA on Samsung SM-S931B for the empty active-routine setup path after the setup-entry, setup-tab, copy, and button-affordance polish.
- Evidence folder: `.codex/device-qa/2026-05-10-qa-polish-training-setup-runtime/`.
- QA evidence:
  - PASS build/install: `./gradlew.bat :app:assembleDebug :app:installDebug --console=plain --no-configuration-cache`.
  - PASS clean launch: `adb shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 936`.
  - PASS recovery from weird keyboard/settings interruption: app returned to the create-routine dialog without crash and retained the entered routine name.
  - PASS empty active routine card showed the updated helper copy and `Routine inrichten`.
  - PASS tapping `Routine inrichten` opened detail mode with `Sessies` selected and `Eerste oefening toevoegen` reachable.
  - QA finding: opening detail from a scrolled Training list could preserve the old scroll offset, clipping the detail header/back affordance at the top.
- Files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`
- Fix: Training now keeps a `LazyListState` and scrolls to item 0 when routine detail mode opens.
- Verification: RED, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest.routine detail resets training list scroll when opened" --console=plain --no-configuration-cache` failed before the scroll reset.
- Verification: PASS, same targeted test after polish.
- Verification: PASS, full `WorkoutInputValidationTest`.
- Verification: PASS, `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
- Verification: PASS, `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; after force-stop relaunch `Status: ok`, `WaitTime: 705`.
- Runtime smoke after polish: PASS, retapping `Routine inrichten` opened detail with `Terug naar routines`, routine title, `Info`, and selected `Sessies` visible at the top instead of clipped.
- Crash evidence: PASS, `.codex/device-qa/2026-05-10-qa-polish-training-setup-runtime/14-after-polish-crash-buffer.txt` was empty.
- Remaining risk: full add-first-exercise, active-workout start, finish, and completion/debrief runtime QA remains open.

## 2026-05-10 Extended QA Timebox

- QA scope: broad Android QA pass across build/test/lint, top-level navigation, Training setup, Nutrition, Settings/More, crash buffers, and source-level consistency checks.
- Timebox start: 2026-05-10T20:17:57+02:00.
- Evidence folder: `.codex/device-qa/2026-05-10-hour-qa/`.
- Build/static verification:
  - PASS, `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
  - PASS source scan: no broad TODO/FIXME/HACK hotspots in app source; typed navigation use remains concentrated in `TrainIqNav.kt`.
  - PASS/expected: decorative `contentDescription = null` icons exist inside labeled buttons/rows; actionable workout controls inspected in this pass have labels or merged semantics.
- Physical-device QA:
  - Device: Samsung SM-S931B, Android 16, `RFCY60HNHNJ`.
  - PASS clean launch: `adb shell am start -W -n com.trainiq/.MainActivity` returned `Status: ok`, `WaitTime: 735`.
  - PASS top-level traversal: Start, Training, Voeding, Coach, and Meer rendered without crash.
  - PASS crash evidence: `.codex/device-qa/2026-05-10-hour-qa/04-tab-traverse-crash-buffer.txt` was empty.
  - PASS Nutrition smoke: Voeding rendered Vandaag/Toevoegen/AI-resultaat/Recepten tabs, meal sections, and add actions; crash buffer stayed empty.
  - PASS Settings/More smoke: Settings rendered status, theme mode, AI status, Health Connect status, and progress entry without crash.
  - PARTIAL Training setup: active empty routine card showed `Routine inrichten`; setup path remains reachable, but full add-first-exercise/start/finish/debrief was not completed in this pass because the coordinate attempt missed the setup button and scrolled into the exercise library instead of opening detail.
- New QA finding:
  - finding_id: QA-2026-05-10-022
  - priority: P2
  - area: UX, accessibility
  - status: done
  - owner suggestion: Android UI owner
  - current evidence with file references:
    - `.codex/device-qa/2026-05-10-hour-qa/03-tab-training.xml` shows that after clean launch with an existing empty active routine, the Training first viewport prioritizes `Routine maken` before `Actieve routine`.
    - In the same dump, the active routine card starts at `bounds="[48,1195][1032,1746]"` and the `Routine inrichten` button extends to `bounds="[96,1666][984,1818]"`, below the scroll viewport ending at `1746`, so the primary next setup action is partly clipped.
    - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt` currently emits `RoutineCreationCard` before `ActiveRoutineCard` in the Training `LazyColumn`.
  - expected target-state behavior: When an active routine exists, Training should prioritize the current next action. Empty active-routine setup should be visible and tappable without relying on precise scroll position.
  - concrete recommended fix: In the Training screen, render `ActiveRoutineCard` before `RoutineCreationCard` when an active routine exists, while keeping routine creation available directly below. Add a source-level ordering guard and rerun the physical-device setup smoke.
  - regression risk: Low to medium. It changes first-viewport ordering but does not remove any action; verify empty/no-active-routine onboarding still shows routine creation clearly.
  - minimal verification command/check: `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`, `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`, and physical-device Training first-viewport smoke.
- External sources used: None. Local runtime evidence and source inspection were sufficient.
- Remaining risk: full add-first-exercise, active-workout start, finish, and completion/debrief runtime QA remains open.

## 2026-05-10 Training First-Viewport Order Polish

- Finding closed: QA-2026-05-10-022.
- Files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`
- Fix: Training now renders `ActiveRoutineCard` before `RoutineCreationCard` when `overview.activeRoutine` exists, while preserving the original no-active-routine flow where routine creation appears first.
- Regression coverage: added a targeted source-level guard that verifies the active-routine-first branch and the no-active-routine creation-first fallback.
- Verification: RED, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest.active routine is prioritized before routine creation when present" --console=plain --no-configuration-cache` failed on the old ordering.
- Verification: PASS, same targeted test after implementation.
- Verification: PASS, full `WorkoutInputValidationTest`.
- Verification: PASS, `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
- Physical-device evidence: PASS on SM-S931B, installed debug build, launched with `am start -S -W` `Status: ok`, navigated to Training, verified `Actieve routine` and fully visible `Routine inrichten` appear before `Routine maken`; evidence in `.codex/device-qa/2026-05-10-training-first-viewport-after-order-polish/15-training-final.xml`.
- Crash evidence: PASS, `.codex/device-qa/2026-05-10-training-first-viewport-after-order-polish/16-crash-buffer-final.txt` was empty.
- External sources used: None. Local runtime evidence and source inspection were sufficient.
- Remaining risk: full add-first-exercise, active-workout start, finish, and completion/debrief runtime QA remains open.

## 2026-05-10 Settings Gemini Key Help Polish

- Target-state link: Settings is the control center for AI, privacy, and local key handling; Gemini keys must not be committed or placed in `BuildConfig`.
- Files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/settings/SettingsSection.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/settings/SettingsUiStateTest.kt`
  - `TrainIQ-Project/README.md`
- Fix: Settings now shows a compact Gemini API-key setup instruction, the official Google AI Studio API Keys URL, and a warning not to share or commit the key. The Android README mirrors the same short setup path.
- Regression coverage: added a targeted Settings guard for the Google AI Studio label, URL, paste/save instruction, and no-commit warning.
- Verification: baseline PASS, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.settings.SettingsUiStateTest" --console=plain --no-configuration-cache`.
- Verification: RED, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.settings.SettingsUiStateTest.geminiApiKeyHelpPointsToGoogleAiStudioWithoutEncouragingCommittedSecrets" --console=plain --no-configuration-cache` failed before the helper functions existed.
- Verification: PASS, same targeted test after implementation.
- Verification: PASS, full `SettingsUiStateTest`.
- Verification: PASS, `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
- Physical-device evidence: PASS on SM-S931B, installed debug build, launched Settings, verified `AI / Gemini`, `Google AI Studio`, `https://aistudio.google.com/app/apikey`, and `commit hem nooit`; evidence in `.codex/device-qa/2026-05-10-settings-gemini-key-help-polish/03-settings-ai.xml`.
- Crash evidence: PASS, `.codex/device-qa/2026-05-10-settings-gemini-key-help-polish/04-crash-buffer.txt` was empty.
- External sources used: Google AI for Developers, Using Gemini API keys, accessed 2026-05-10: https://ai.google.dev/gemini-api/docs/api-key. It documents creating/managing Gemini API keys in Google AI Studio and links to the API Keys page.
- Remaining risk: BYOK/direct-client production mode remains an owner decision under existing release/privacy findings.

## 2026-05-10 Training Setup To Completion Polish

- Target-state link: Training setup should let users move from an empty active routine to a saved workout and completion/debrief without hidden validation traps.
- QA evidence before fix: physical-device QA on SM-S931B could create a session, add `Ab Wheel Rollout`, start the workout, and reach the active logger, but tapping `Set loggen` failed with `Voer een gewicht tussen 0 en 1000 kg in.` even though the bodyweight set visually had no planned kg.
- Files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`
- Fix: active workout logger defaults missing planned weight to `0` for bodyweight/no-weight sets, and both UI draft rendering and `logSet` use the same effective draft that fills missing saved-draft fields from the planned set.
- Regression coverage: added targeted tests for bodyweight draft weight text and effective UI draft fallback, including persisted drafts with blank weight but planned reps.
- Verification: baseline PASS, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`.
- Verification: RED, targeted bodyweight draft tests failed before `activeSetDraftWeightText` and `activeSetUiDraft` existed.
- Verification: PASS, full `WorkoutInputValidationTest` after implementation.
- Verification: PASS, `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`.
- Tooling note: parallel Gradle invocations caused Kotlin/KSP incremental-cache lock failures; `./gradlew.bat --stop` followed by sequential test/build resolved the tooling issue.
- Physical-device evidence: PASS on SM-S931B. The app launched with `Status: ok`, Training started the QA routine, the active logger accepted `Set loggen` without weight/reps errors, `1 set gelogd` appeared, finish confirmation saved the partial session, and completion rendered `Voltooid`, `Slimme samenvatting`, `Lokale fallback`, `Sets 1`, and `Sterkste set: 0 kg x 12`.
- Crash evidence: PASS, `.codex/device-qa/2026-05-10-training-setup-to-completion-polish/40-after-save-crash-buffer.txt` was empty.
- External sources used: None. Local runtime evidence and existing app tests were sufficient.
- Remaining risk: completion with Gemini-enabled debrief still needs API-key/network-path evidence; this pass verified local fallback completion.

## 2026-05-12 Warm Futuristic UI Polish

- Target-state link: Primary screens should feel modern, calm, readable, compact-safe, and aligned with the new warm futuristic mockups without changing core data, navigation, Health Connect, Gemini, or Room behavior.
- Scope: conservative visual polish for shared theme/components, Home, Nutrition, Progress, Coach, and Active Workout using existing app state only. Settings received only shared component/theme effects.
- Files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/core/theme/Theme.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/core/ui/AppDesign.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/home/HomeScreen.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/progress/ProgressScreen.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/coach/CoachScreen.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`
  - focused UI/source regression tests under `TrainIQ-Project/app/src/test/java/com/trainiq/`
- Fix: added warm moodboard tokens, warm-glass shared styling, pill/metric/action layout helpers, safer full-width or wrapping actions for long Dutch labels, stronger hierarchy on Home/Nutrition/Progress/Coach, and a clearer Active Workout summary/action layout.
- Verification: baseline PASS, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
- Verification: RED/PASS, focused warm moodboard/shared UI tests and source guards.
- Verification: PASS, `WorkoutInputValidationTest`, `ProgressMeasurementValidationTest`, Home/Nutrition/Coach/Settings/architecture targeted tests.
- Verification: PASS, after-change broad gate `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
- Runtime QA: PASS on `emulator-5554` after reinstall. Fresh launch returned `Status: ok`, `WaitTime: 5303`; Start, Training, Voeding, Coach, Meer/Settings, and Voortgang opened without crash; crash buffers were empty.
- Font-scale QA: PASS/PARTIAL at system font scale `1.3`; Start, Training, Voeding, Coach, and Meer/Settings rendered and crash buffer stayed empty. Active Workout seeded-flow font-scale proof remains covered by earlier targeted evidence and was not rerun in this pass.
- Evidence folder: `TrainIQ-Project/.codex/device-qa/2026-05-12-warm-futuristic-ui-polish/`.
- External sources used: None. Local mockups, blueprint criteria, source inspection, tests, and emulator evidence were sufficient.
- Remaining risk: this pass did not certify manual TalkBack/Switch Access, physical-device performance, Gemini-enabled workout debrief, or deeper seeded active-workout runtime flows.

## 2026-05-13 Nutrition Flow Polish

- Target-state link: Nutrition logging should be calmer, direct from the selected meal moment, and editable without forcing users to search products or recipes again.
- Files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/nutrition/NutritionInputValidationTest.kt`
- Fix: replaced the persistent nutrition tab row with a compact `|||` section menu, kept meal-moment `Toevoegen` on the current screen with the contextual add-to-meal sheet, and made existing meal quantity editing explicit with `Hoeveelheid wijzigen` plus edit-mode save copy.
- Regression coverage: added targeted nutrition helper tests and source guards for the section menu, contextual add flow, and existing-meal edit draft flow.
- Verification: baseline PASS, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.NutritionInputValidationTest" --console=plain --no-configuration-cache`.
- Verification: RED, the targeted nutrition test failed on missing section-menu/edit helper functions before implementation.
- Verification: PASS, targeted nutrition test after implementation.
- Verification: PASS, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
- Verification: PASS, `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
- Runtime QA: PASS after emulator storage was increased. `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache` installed on `emulator-5554`; cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 6170`.
- Runtime QA: PASS, UI dump showed `Voeding`, `|||`, and content description `Voeding secties openen`; tapping the section control opened `Voeding secties` with `Vandaag`, `Toevoegen`, `AI-resultaat`, `Recepten`, `Producten`, and `Historie`.
- Runtime QA: PASS, tapping `Toevoegen` in the `Ochtend` section opened the contextual `Toevoegen aan Ochtend` sheet with manual product, saved product, saved recipe, AI context, and photo/AI actions.
- Crash evidence: PASS, `adb logcat -d -t 2000 AndroidRuntime:E '*:S'` was empty after launch, section menu, and add-sheet checks.
- External sources used: Microsoft Learn ADB0060 guidance was used only for the earlier install blocker; after storage increase, local runtime evidence was sufficient.
- Remaining risk: existing-meal quantity edit runtime proof still needs seeded/created meal data; source guards and unit tests cover the edit draft path.

## 2026-05-13 Nutrition Plus And AI Routine Layout Polish

- Target-state link: Nutrition logging should respond immediately, meal-moment add actions should be direct and compact, and AI routine creation/preview should remain readable and actionable on compact screens.
- Parallel-agent execution:
  - Nutrition worker owned `NutritionScreen.kt` and `NutritionInputValidationTest.kt`.
  - AI routine worker owned `WorkoutScreen.kt`, `RoutineDialogs.kt`, `WorkoutInputValidationTest.kt`, and `WorkoutDialogPresentationPolicyTest.kt`.
- Files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/WorkoutScreen.kt`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/workout/RoutineDialogs.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/nutrition/NutritionInputValidationTest.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutInputValidationTest.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/workout/WorkoutDialogPresentationPolicyTest.kt`
- Fix: removed the focus-clearing pointer interceptor from normal Nutrition browsing lists, replaced meal-section `Toevoegen` text buttons with accessible plus icon actions, preserved the selected meal moment through saved-food/saved-recipe/AI/photo/reuse add flows, wrapped AI routine suggestion and experience controls, localized generator labels, and moved generated-routine preview actions into a persistent full-width bottom action area.
- Regression coverage: added focused Nutrition source/helper guards for plus actions, immediate browsing scroll policy, contextual meal targets, and draft routing; added Workout source guards for Dutch generator labels, wrapped chip/choice controls, deload semantics, and generated-routine preview action placement.
- Verification: baseline PASS, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.NutritionInputValidationTest" --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --tests "com.trainiq.features.workout.WorkoutDialogPresentationPolicyTest" --console=plain --no-configuration-cache`.
- Verification: after-change PASS, same targeted Gradle command.
- Verification: after-change PASS, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
- Verification: after-change PASS, `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
- Verification: PASS, `git diff --check` reported only CRLF conversion warnings.
- Runtime QA: PASS on `emulator-5554`, installed debug APK, cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 5979`; evidence folder `TrainIQ-Project/.codex/device-qa/2026-05-13-nutrition-ai-routine-polish/`.
- Runtime QA: PASS, immediately after opening `Voeding` an input swipe produced a scrolled Nutrition dump with `Voedingsdag`, meal sections, `Toevoegen aan Ochtend/Middag/Avond` plus-button content descriptions, and no visible meal-section `Toevoegen` text.
- Runtime QA: PASS, tapping the Ochtend plus action opened `Toevoegen aan Ochtend` with manual product, saved product, saved recipe, AI-context, and photo AI actions.
- Runtime QA: PASS/PARTIAL, Training opened the AI routine dialog with Dutch labels (`Dagen per week`, `Beschikbaar materiaal`, `Ervaringsniveau`, `Sessieduur`) and wrapped suggestion/experience controls; AndroidRuntime crash buffers stayed empty.
- Runtime QA: NOT RUN for generated-routine preview after-change runtime proof: tapping `Genereren` kept the dialog in loading during the smoke window on this emulator, likely because no usable AI provider/key path was available. Source guards and compile/unit checks cover the persistent preview action area; runtime proof should be rerun with a configured AI provider or deterministic local-fallback path.
- External sources used: None. Local source, tests, existing target-state evidence, and emulator smoke were sufficient.
- Remaining risk: generated-routine preview bottom actions still need fresh runtime proof after generation completes; saved-food/saved-recipe add-to-draft behavior is source/unit guarded but needs seeded nutrition data for a full end-to-end runtime proof.

## 2026-05-13 Nutrition Scroll Regression Follow-Up

- Target-state link: Nutrition logging should respond immediately and add-source sheets should not consume initial drag gestures.
- Files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/nutrition/NutritionInputValidationTest.kt`
- Fix: removed the remaining `clearFocusOnScrollOrDrag()` usage from Nutrition add sheets. `RecipeActionBottomSheet` no longer uses a focus-clearing gesture modifier, and `AddToMealActionSheet` uses tap-outside focus clearing instead of drag/scroll focus clearing.
- Regression coverage: added a focused Nutrition source guard that the browsing list and Nutrition action sheets do not use the scroll/drag focus-clear interceptor while the meal add sheet still has tap-outside focus behavior.
- Verification: baseline PASS, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.NutritionInputValidationTest" --console=plain --no-configuration-cache`.
- Verification: after-change PASS, same targeted Nutrition test.
- Verification: after-change PASS, targeted no-regression command for `NutritionInputValidationTest`, `WorkoutInputValidationTest`, and `WorkoutDialogPresentationPolicyTest`.
- Verification: after-change PASS, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
- Verification: after-change PASS, `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
- Runtime QA: PASS on `emulator-5554`, `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 5356`.
- Runtime QA: PASS, after opening `Voeding`, a fast immediate vertical swipe moved the visible meal sections from top-of-day `Ochtend/Middag/Avond` to scrolled `Middag/Avond/Snacks`, confirming the main Nutrition list responds immediately. Evidence: `TrainIQ-Project/.codex/device-qa/2026-05-13-nutrition-scroll-regression/11-nutrition-tab-before.xml`, `12-nutrition-tab-after-fast-swipe.xml`, and `13-nutrition-tab-after-late-swipe.xml`.
- Runtime QA: PASS, tapping the visible meal plus action opened `Toevoegen aan Middag` with manual product, saved product, saved recipe, `AI-context voor foto`, `Foto / AI-inschatting`, and `Sluiten`. Evidence: `TrainIQ-Project/.codex/device-qa/2026-05-13-nutrition-scroll-regression/24-add-sheet-open-midday.xml`.
- Crash evidence: PASS, AndroidRuntime crash buffers captured during the Nutrition scroll/add-sheet smokes were empty.
- External sources used: None. Local source, tests, and emulator evidence were sufficient.
- Remaining risk: add-sheet runtime scroll was opened and inspected, but on the large emulator the sheet content fit without requiring a scroll range; the source guard covers the removed drag-interceptor regression, and compact/font-scale sheet scroll proof can be rerun on a smaller viewport if needed.

## 2026-05-13 Voeding Initial Scroll Stability Polish

- Target-state link: Nutrition should be calm and responsive immediately when the user opens the tab, without a first loading surface that resets input before the loaded content appears.
- Files changed:
  - `TrainIQ-Project/app/src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt`
  - `TrainIQ-Project/app/src/test/java/com/trainiq/features/nutrition/NutritionInputValidationTest.kt`
- Fix: removed the full `AnimatedContent` replacement between `NutritionUiState.Loading` and `Success`. The Voeding header and `nutritionListState`-backed `LazyColumn` now stay mounted from first composition, and loading/error/success render as items inside that stable scroll surface.
- Regression coverage: added a focused source guard that Voeding uses one stable browsing `LazyColumn`, no longer wraps the body in `AnimatedContent`, and renders `NutritionUiState.Loading` inside the stable list with enough shimmer items to accept immediate scroll.
- Verification: baseline PASS, `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.NutritionInputValidationTest" --console=plain --no-configuration-cache`.
- Verification: after-change PASS, same targeted Nutrition test.
- Verification: after-change PASS, targeted no-regression command for `NutritionInputValidationTest`, `WorkoutInputValidationTest`, and `WorkoutDialogPresentationPolicyTest`.
- Verification: after-change PASS, `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-configuration-cache`.
- Verification: after-change PASS, `./gradlew.bat :app:test --console=plain --no-configuration-cache`.
- Runtime QA: PASS on `emulator-5554`, `./gradlew.bat :app:installDebug --console=plain --no-configuration-cache`; cold launch returned `Status: ok`, `LaunchState: COLD`, `WaitTime: 5595`.
- Runtime QA: PASS, after opening `Voeding`, the UI dump showed the stable `Voeding` header and one scrollable content node; an immediate vertical swipe produced the scrolled nutrition dump while plus-button content descriptions remained present. Evidence: `TrainIQ-Project/.codex/device-qa/2026-05-13-voeding-initial-scroll-stable/02-voeding-before-immediate.xml`, `03-voeding-after-immediate-swipe.xml`, and `04-voeding-settled.xml`.
- Runtime QA: PASS, tapping a visible meal plus action opened `Toevoegen aan Avond` with manual product, saved product, saved recipe, `AI-context voor foto`, `Foto / AI-inschatting`, and `Sluiten`. Evidence: `TrainIQ-Project/.codex/device-qa/2026-05-13-voeding-initial-scroll-stable/05-voeding-add-sheet.xml`.
- Crash evidence: PASS, AndroidRuntime crash buffer captured during the initial-scroll smoke was empty.
- External sources used: None. Local source, tests, and emulator evidence were sufficient.
- Remaining risk: the emulator's local nutrition overview settled very quickly, so runtime evidence mainly proves the stable loaded scroll and source guards prove the initial loading path. A slower seeded startup profile can further prove placeholder-scroll behavior if needed.

## 2026-07-10 Compact Guided Tour and Focused Jank Follow-up

- Status: partially-done (UI and code-level performance fixes verified; physical-device frame certification remains open).
- Guided tour: replaced the oversized three-equal-button overlay with a compact panel above app/system navigation. `Later afronden` is now a quiet header action; `Terug` and `Volgende` keep 48dp minimum touch targets and the six-step flow remains intact.
- App-wide diagnostics: replaced the unbounded boxed frame-duration list with a bounded primitive ring buffer, keeping per-frame recording O(1) and moving sorting to summary generation.
- Active workout: removed per-second elapsed/rest values from broad `ActiveWorkoutUiState`; only the session summary, rest card, and bottom bar read the stable clock state.
- Benchmark readiness: top-level and baseline-profile journeys now seed completed onboarding before navigation. The emulator profileable run no longer failed on the missing `Training` target, but did not finish within 180 seconds; metric status is `NOT RUN`.
- Sources: Android Compose performance guidance (`https://developer.android.com/develop/ui/compose/performance`, `https://developer.android.com/develop/ui/compose/performance/bestpractices`) and Compose accessibility defaults (`https://developer.android.com/develop/ui/compose/accessibility/api-defaults`).
- Verification: focused RED/PASS tests; PASS broad `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin :macrobenchmark:compileProfileableJavaWithJavac`; PASS adb cold launch (`Status: ok`, `LaunchState: COLD`, `TotalTime: 4792`) with an empty fatal buffer; PASS UI-tree/screenshot for `Stap 1 van 6`, unwrapped `Volgende`, and navigation to `Stap 2 van 6` / Training.
- Remaining risk: repeat top-level and active-workout profileable benchmarks plus fast workout scrolling on a physical Samsung before closing performance certification.
