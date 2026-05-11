# TrainIQ Target State Blueprint

This document is the product-quality target state for TrainIQ. It replaces the earlier high-level foundation notes with a QA- and research-backed standard for implementation, validation, and release readiness.

Target app:

```text
Health Connect -> Domain Intelligence -> Gemini 2.5 Flash -> Material 3 UI -> Personal action
```

Core rule:

```text
Long-term code health > short-term speed
```

## 1. Product Vision

TrainIQ must become an AI-native health coach, not a traditional manual fitness tracker.

The target experience:

- Passive health data is collected safely through Health Connect.
- Training, nutrition, sleep, recovery, and body trends are combined into useful coaching.
- AI features are explicit, bounded, structured, and explainable without exposing model chain-of-thought.
- The app remains fast, calm, and resilient on small phones, large phones, tablets, and foldables.
- Manual logging stays reliable when Health Connect, network, camera, or Gemini are unavailable.

Primary user outcome:

```text
The user always knows what to do next: train, recover, eat, adjust, or inspect.
```

## 2. Current QA Baseline

Evidence gathered during the May 8, 2026 optimization retest, after the second implementation pass:

- `:app:assembleDebug` completed successfully.
- Worker verification passed `:app:testDebugUnitTest`, `:app:compileDebugAndroidTestKotlin`, `:app:lintDebug`, `:macrobenchmark:assembleProfileable`, `:macrobenchmark:compileDebugJavaWithJavac`, and `:app:checkReleaseSigningReadiness`.
- App installed and launched on `emulator-5554`, but first draw was slow: `am start -W` reported `WaitTime: 18568`, the first UI dump still saw launcher/splash state, and a delayed dump showed Home.
- Independent Android QA also reproduced launch timeout behavior: `am start -W` returned `Status: timeout`, `WaitTime: 21671`, and `gfxinfo` reported 93-94% janky frames during top-level navigation/runtime flow.
- Runtime artifacts were captured under:
  - `D:\GitHub\TrainIQ\qa-cycle-runtime\optimization-review`
  - `D:\GitHub\TrainIQ\qa-cycle-runtime\worker-a-current-2026-05-08`
  - `D:\GitHub\TrainIQ\qa-cycle-runtime\optimization-rerun-2026-05-08`
- Crash buffer was empty during the current emulator pass.
- Current compact bottom navigation fits `Start`, `Training`, `Voeding`, `Coach`, and `Meer` at `360x640 @ 320dpi` with font scale `1.3`, but first-viewport content is sparse and key setup content falls below the fold.
- Prior critical implementation gaps are materially improved or closed: Gemini auth moved to `x-goog-api-key`, Android Keystore key storage exists, Health Connect pagination/per-metric tokens/partial sync/background gating exist, Room v12 foreign keys now clean dirty legacy orphans and assert `PRAGMA foreign_key_check`, workout finish has retry recovery, Progress has a Settings entry point, edge-to-edge is enabled, macrobenchmark target lookup fails on missing labels, and local absolute Gradle temp paths were removed.

Important filename note: the requested target file name contained `Blprint`, but the repository contains this file as `TrainIQ_Target_State_Blueprint.md`. This existing file is the canonical target-state document.

Evidence gathered during the May 9, 2026 full QA audit:

- Repo-local Codex skills were added for repeatable `superpowers`, `test-android-apps`, and `trainiq-target-state-qa` workflows.
- `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:lintDebug`, `:app:compileDebugAndroidTestKotlin`, `:macrobenchmark:compileProfileableJavaWithJavac`, `:app:checkReleaseSigningReadiness`, and `:app:connectedDebugAndroidTest` passed on the available environment.
- App installed and launched on `emulator-5554`; cold `am start -W` reached Home with `Status: ok`, `WaitTime: 6749`, and an empty crash buffer.
- The same debug emulator smoke still showed startup/frame risk: `gfxinfo` reported 6 janky frames out of 8 rendered frames on the first-draw sample. This is not release-certifying evidence, but it keeps performance investigation open.
- CI now runs on `pull_request` and protected branch pushes, release shrinking is enabled, and R8/ProGuard configuration exists. Remaining release blockers are owner gates, physical-device performance thresholds/evidence, production AI boundary signoff, accessibility signoff, Play/Data Safety confirmation, and migration-marker release gating.

Refresh evidence from the later May 9, 2026 QA pass:

- `:app:testDebugUnitTest` for `LineChartSemanticsTest`, `:app:assembleDebug`, `:app:lintDebug`, `:app:compileDebugAndroidTestKotlin`, `:app:checkReleaseSigningReadiness`, `:macrobenchmark:compileProfileableJavaWithJavac`, and targeted connected `AppLineChartAccessibilityTest` passed.
- App installed and launched again on `emulator-5554`; cold `am start -W` reached Home with `Status: ok`, `WaitTime: 7260`, and an empty crash buffer.
- Shared line-chart semantics now have automated unit and connected accessibility coverage in the current worktree, but manual TalkBack/Switch Access release signoff remains open for the broader app.

Additional refresh evidence from the latest May 9, 2026 QA pass:

- Targeted unit coverage for camera scanner state, Health Connect background sync retry policy, and line-chart semantics passed; `:app:assembleDebug`, `:app:lintDebug`, `:app:compileDebugAndroidTestKotlin`, targeted connected `AppLineChartAccessibilityTest`, `:app:installDebug`, `:app:checkReleaseSigningReadiness`, and `:macrobenchmark:compileProfileableJavaWithJavac` passed.
- App installed and launched on `emulator-5554`; cold `am start -W` reached Home with `Status: ok`, `WaitTime: 6446`, and an empty crash buffer.
- Debug `gfxinfo` still reported 6 janky frames out of 8 rendered frames on the first-draw sample, so startup/frame performance remains an open risk until profileable/release physical-device evidence is collected.
- Camera scanner permission/error state and Health Connect background failure retry policy are improved in the current worktree, but scanner no-camera/CameraX bind-failure runtime handling and end-to-end revoked/provider Health Connect flows still need device evidence.

Refresh evidence from the May 10, 2026 full QA audit:

- Repo-local `trainiq-target-state-qa` skill now contains the reusable target-state audit workflow and valid frontmatter.
- `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:lintDebug`, `:app:compileDebugAndroidTestKotlin`, `:app:checkReleaseSigningReadiness`, and `:macrobenchmark:compileProfileableJavaWithJavac` passed in one Gradle verification run.
- Release-readiness verification also passed `:app:assembleRelease` and `:app:bundleRelease`; local release signing was not configured, so unsigned local artifacts are expected.
- App installed on `emulator-5554`, and Home rendered after launch, but `am start -W` returned `Status: timeout`, `WaitTime: 20254`. Crash buffer was empty. `dumpsys gfxinfo com.trainiq framestats` was inconclusive because it returned `Failure while dumping the app`.
- Open risks remain concentrated in targeted Room persistence migration, release owner gates, manual accessibility signoff, physical-device performance evidence, migration-marker CI/release gating, Health Connect runtime permission/provider evidence, compact large-font UI proof, and production AI boundary decisions.

## 3. Prioritized Findings

### Current Optimization Risks

1. **Room runtime mutations can leave stale rows behind.**
   - Severity: High.
   - Evidence: `RoomTrainIqRuntimeStore.update()` serializes the updated full state and calls `sink.importTransaction(planner.plan(...))`; `RoomJsonImportSink` only clears mirror tables when `mirrorRun != null`, then upserts incoming rows.
   - Risk: deletes and removals from routines, meals, recipes, measurements, active workouts, or workout history can leave stale Room rows that reappear after restart or corrupt derived state.
   - Target: normal app mutations must use targeted DAO operations or full-replacement semantics that delete stale rows transactionally; upsert-only full-state mirroring is not acceptable for user data.

2. **Startup and first draw remain too slow on the emulator.**
   - Severity: High until measured in profileable/release.
   - Evidence: current retest captured `WaitTime: 18568`, delayed Home rendering, and logcat `Skipped 129 frames`; Worker A reproduced `Status: timeout`, `WaitTime: 21671`, and WindowManager input-dispatch timeout evidence.
   - Risk: startup and first navigation may still feel heavy after the functional fixes.
   - Target: profileable/release macrobenchmark must identify and bound first-draw work; startup must not perform broad Room mirror/import, Health Connect, or heavy Compose initialization on the critical path.

3. **Hot-path mutations are O(app-data-size).**
   - Severity: High.
   - Evidence: 2026-05-11 repository hot-path mutations have been moved off `runtimeStore.update` toward targeted Room writes, but only representative process-restart instrumentation exists so far.
   - Risk: set logging, draft edits, meal saves, recipe edits, and routine changes can slow down as local data grows and can contribute to startup/jank through mirror churn.
   - Target: active workout logging and meal saves must use bounded, targeted database writes with benchmark evidence; no full JSON serialization/import on critical user actions.

4. **Health Connect permission UX still implies all metrics are mandatory.**
   - Severity: Medium.
   - Evidence: `HealthConnectPermissionsRationaleActivity` treats anything less than all six permissions as failure copy, while the data layer now supports partial sync.
   - Risk: the app can technically handle partial permissions but the user is told all six signals are required, undermining trust and consent clarity.
   - Target: rationale copy and permission-result messaging must explain each permission independently and celebrate partial success while clearly marking denied metrics.

5. **Active workout and scanner still need large-font critical-path proof.**
   - Severity: Medium.
   - Evidence: active workout controls still use dense horizontal rows for status metrics, sticky finish controls, and set edit fields; scanner permission/result states have limited large-font/inset evidence.
   - Risk: 360px devices, font scale `1.3+`, landscape, cutouts, and IME can hide critical controls or make logging error-prone.
   - Target: active workout, scanner permission/result states, Settings AI/Health, nutrition AI item editing, and routine generation must pass 360x640/360x800 at font scale `1.3+`.

6. **Top-level navigation jank is release-blocking until explained.**
   - Severity: High.
   - Evidence: Worker A captured `gfxinfo` with 89/95 janky frames during nav flow and 203/215 janky frames in final runtime stats.
   - Risk: even if individual screens are functionally correct, the app can feel broken under real navigation and display changes.
   - Target: top-level tab traversal must meet agreed `gfxinfo`/Macrobenchmark/Perfetto thresholds on a release-like build and on at least one lower-end physical device.

7. **Workout completion still waits for debrief generation.**
   - Severity: Medium.
   - Evidence: session save occurs before debrief, but `finishWorkout` awaits Gemini/local debrief before returning to completion navigation.
   - Risk: slow network or API timeout can hold the processing screen even though the workout is already saved.
   - Target: saving and completion navigation must return immediately with a local summary; Gemini debrief should refresh asynchronously with retry/status.

8. **Production AI boundary remains undecided.**
   - Severity: Medium.
   - Evidence: direct-client BYOK is implemented, while release docs still keep the production AI gateway/BYOK decision open.
   - Risk: billing, abuse/quota, privacy, and third-party sharing ownership can remain unresolved at release time.
   - Target: release readiness must choose and document one mode: BYOK accepted, backend gateway implemented, OAuth-mediated access, or AI scoped out.

9. **Release gates still depend on owner signoff and physical-device evidence.**
   - Severity: Medium.
   - Evidence: GitHub workflow now runs unit tests, lint, Android test compile, macrobenchmark compile, and signing readiness on PR/protected-branch push; release shrinking is enabled. However, migration marker generation is not wired into release build gates, performance thresholds still need product confirmation, physical-device benchmark evidence is missing, and owner release documents still block legal/privacy/accessibility/AI signoff.
   - Risk: regressions are less likely to land silently, but releases can still ship without fresh migration proof, physical-device performance evidence, accessibility signoff, Play/Data Safety confirmation, or a signed production AI boundary.
   - Target: releases must require migration-marker evidence or an owner-approved exception; baseline profiles must be generated, not only required by benchmarks; physical-device macrobenchmark/profileable evidence must meet approved thresholds; owner release blockers must be closed before production submission.

## 4. Architecture Target State

TrainIQ uses:

```text
MVVM + Clean Architecture + Unidirectional Data Flow
```

Required flow:

```text
Room / Health Connect / Remote APIs
    -> Repositories
    -> UseCases
    -> ViewModels
    -> Compose UI
```

Rules:

- Business logic lives in UseCases or domain services, not composables.
- Each screen exposes one `uiState: StateFlow<T>`.
- UI state uses sealed interfaces with explicit `Loading`, `Success`, and `Error` states.
- Repositories are `@Singleton`.
- ViewModel-owned helpers use `@ViewModelScoped` when scoped injection is needed.
- Navigation uses Navigation 2.8+ type-safe `@Serializable` routes only.
- No string-based route contracts.
- No database mapping in UI code.
- `TrainIqDataCoordinator` must continue being decomposed into bounded services or repositories for workout logging, nutrition persistence, progress measurements, Health Connect aggregation, and AI orchestration.

## 5. Data and Room Target State

Room is the primary source of truth for app-owned data.

DataStore may only hold:

- User preferences
- Theme mode
- AI enabled flag
- Non-sensitive feature preferences
- Small sync metadata while it is being migrated to Room

Room requirements:

- Use `AutoMigration` where safe.
- Use explicit SQL migrations for non-trivial changes.
- Export schemas for every version.
- Add Room foreign keys for routines/days/exercises, sessions/sets, recipes/ingredients, meals/items, active workout children, and measurement ownership.
- Runtime mutations must be authoritative in both directions: inserts, updates, and deletes must be represented in Room in the same transaction.
- Do not use upsert-only full-state mirror imports for normal user mutations. Either use targeted DAO mutations or explicit full-replacement semantics that clear stale rows safely.
- Active workout finish, discard, back/retry, meal delete, recipe delete, routine delete, and measurement delete must prove rows cannot resurrect after process restart.
- Validate migration chains with instrumentation tests before release.
- Migration tests must include dirty legacy data: orphaned children, missing parents, duplicate external IDs, null legacy fields, and stale Health Connect cache metadata.
- Any migration that introduces foreign keys must either repair/quarantine legacy orphans or fail closed with a clear recovery path.
- Run `PRAGMA foreign_key_check` after FK-introducing migrations and assert an empty result in tests.
- Index foreign-key child columns unless there is a measured reason not to, because cascades and parent deletes otherwise risk full table scans.
- Before adding fields, update `Entities.kt`, `DomainModels.kt`, `Mappers.kt`, repositories, use cases, and tests in the same change.

Health Connect cache target:

- Short term: DataStore cache is acceptable for current small cache payloads.
- Target: move Health Connect cache and per-metric sync metadata to Room if payloads grow, debugging becomes difficult, or migration/cleanup semantics become important.

## 6. Health Connect Target State

Health Connect is the central external health-data source.

Required metrics:

- Steps
- Heart rate
- Sleep
- Active calories
- Weight
- Workout sessions

Required behavior:

- Always check `HealthConnectClient.getSdkStatus()`.
- Hide or degrade Health Connect integration when unsupported.
- Handle `SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED` with a clear install/update action.
- Show a rationale screen before requesting permissions.
- Rationale copy must not imply that all Health Connect permissions are mandatory if the product supports partial permissions.
- Permission-result UI must distinguish full success, partial success, and denied metrics without shaming or blocking the user.
- Refresh Health Connect status after the actual permission result or lifecycle return, not immediately before the user grants permissions.
- Support partial permissions: granted metrics must sync and display current data even when another metric is denied.
- Maintain per-metric status: `SYNCED`, `STALE`, `DENIED`, `FAILED`, `UNAVAILABLE`.
- Use per-record-type `ChangesToken` where needed so one revoked metric does not poison all sync.
- Page all `readRecords` calls using `pageToken`.
- Use aggregate APIs for cumulative metrics such as steps to reduce double counting.
- Handle `changesTokenExpired` by clearing the affected token and performing a safe full sync for that metric.
- Sync at least once within Health Connect's 30-day changes-token validity window, or expect a controlled token-expiry fallback.
- If one metric token cannot be acquired or expires, do not force repeated full syncs for unrelated healthy metrics.
- Missing steps permission must not clear caches or tokens for heart rate, sleep, active calories, weight, or workouts.
- Keep background sync safe: no startup-blocking full sync, no unbounded background reads, and no retry loops for permanent permission/provider states.
- Background reads require `READ_HEALTH_DATA_IN_BACKGROUND`, Health Connect feature availability, and explicit user-granted background access; otherwise sync only on foreground/on-resume.
- Release readiness must include Play Console Health Apps declaration, Data Safety form alignment, privacy policy parity, and justification for every requested Health Connect data type.

Research anchors:

- Health Connect read/pagination/aggregation: https://developer.android.com/health-and-fitness/guides/health-connect/develop/read-data
- Health Connect sync and changes tokens: https://developer.android.com/health-and-fitness/health-connect/sync-data
- Health Connect permissions UX: https://developer.android.com/health-and-fitness/guides/health-connect/design/permissions
- Health Connect availability: https://developer.android.com/health-and-fitness/health-connect/availability

## 7. Gemini and AI Target State

Default model:

```text
gemini-2.5-flash
```

Fast Mode:

- Meal scan
- Barcode classification if implemented
- Simple classification

Target:

```text
thinkingBudget = 0
responseMimeType = "application/json"
```

Deep Mode:

- Goal advice
- Workout debrief
- Weekly report
- Recovery analysis
- Routine generation

Target:

```text
thinkingBudget = 500-1000
responseMimeType = "application/json"
```

AI requirements:

- Use structured JSON outputs.
- Add `responseJsonSchema` for every production Gemini feature.
- JSON schemas must distinguish required, optional, and nullable fields; do not require low-confidence notes or optional summaries just to satisfy schema shape.
- Schema contract tests must cover valid output, omitted optional fields, nullable fields, invalid enum values, extra fields, and malformed JSON.
- Parse JSON with structured parsers only; never use regex to extract JSON from free text.
- Do not ask for or display model chain-of-thought. Product-facing explanation fields must be named `rationaleSummary`, `coachReasoningSummary`, or similar.
- Keep the persona consistent: Senior Strength Coach, data-driven, direct, calm, and honest.
- Every AI feature must define timeout, retry, cancellation, fallback, and user messaging.
- Centralize Gemini 429/rate-limit handling with exponential backoff, per-feature throttles, and clear local fallback.
- Meal image upload must resize/compress and enforce max dimensions and max bytes before base64 encoding.
- Gemini API auth must use a header/interceptor boundary; API keys must not appear in URLs.
- API key storage must use Android Keystore as the source of truth. Legacy plaintext DataStore keys must be cleared after verified migration.
- BYOK client-side Gemini calls are acceptable for local/dev MVP only; production should prefer a server-side Gemini boundary or OAuth-backed access controls.
- AI calls must be user-initiated unless a future explicit opt-in is added for proactive reports.
- Production must pin explicit stable model IDs; do not use `latest` aliases.

Research anchors:

- Gemini 2.5 thinking budgets: https://ai.google.dev/gemini-api/docs/thinking
- Gemini structured outputs and JSON schema: https://ai.google.dev/gemini-api/docs/structured-output
- Gemini model capabilities: https://ai.google.dev/gemini-api/docs/models
- Gemini rate limits: https://ai.google.dev/gemini-api/docs/rate-limits

## 8. UI, UX, and Design Target State

TrainIQ must feel modern, calm, and precise.

Required:

- Material 3 components and `MaterialTheme.colorScheme`.
- `MaterialTheme.typography` for all text.
- Dynamic Color on Android 12+.
- Any fixed TrainIQ brand colors must be mapped to, or explicitly justified against, `MaterialTheme.colorScheme`; app chrome must not silently bypass Dynamic Color.
- Shimmer/skeleton loading for high-value surfaces.
- Subtle transitions through `AnimatedContent` or equivalent.
- Haptic feedback for important actions only.
- Adaptive layouts for compact, medium, expanded, foldable, and tablet states.
- No text clipping, overlapping controls, or unreachable actions.
- Touch targets must be at least 48dp unless a Material component provides equivalent expanded touch bounds.
- Screen copy must be Dutch, consistent, and action-oriented.
- Empty, loading, error, offline, denied-permission, and partial-data states are first-class UI states.
- Top-level screens must apply the medium/expanded content max-width policy, not merely calculate it, so tablets and foldables avoid stretched reading lines and cards.

Compact navigation target:

- Six fully labeled bottom tabs are not acceptable on 360px-class phones.
- Compact phones must use a destination policy that preserves readability and tap accuracy.
- If compact phones show five tabs, every hidden destination must have a discoverable alternative path and a regression test.
- Progress must remain reachable on compact phones through `Meer`, Home insights, or an equally obvious entry point.
- Bottom navigation must not overlap content, IME, gesture navigation, or 3-button navigation.

Dialog and sheet target:

- Primary actions remain visible on 360x640, 360x800, large font scale, and landscape where supported.
- Long content scrolls inside the sheet/dialog while action buttons remain reachable.
- Generated routine previews, nutrition add sheets, permission rationale, and finish-workout confirmation are mandatory compact-screen QA flows.
- AI routine generation should be an adaptive full-screen route or modal sheet with sticky actions, not a dense alert dialog.
- Primary screen titles, dialog titles, workout exercise labels, and active set labels must wrap deliberately at large font scale instead of hiding essential context behind ellipsis.

Edge-to-edge and immersive target:

- Call `enableEdgeToEdge()` and handle system bars/insets intentionally.
- Normal screens draw backgrounds edge-to-edge while preserving safe content insets.
- Camera, scanner, active workout, and other immersive flows may hide or minimize system bars only when it improves the user task and Android back/home gestures remain reliable.
- Avoid placing tap or drag targets under gesture insets.
- Android 15+ target-SDK edge-to-edge behavior must be visually regression-tested on gesture navigation and 3-button navigation.
- Edge-to-edge checks must include landscape cutouts, IME, caption/freeform windows, and bottom padding on every scrollable screen.

Accessibility target:

- Automated Compose accessibility checks must run for dense custom UI where possible.
- Manual TalkBack and Switch Access passes are required for active workout, scanner permission/result states, Health Connect rationale, AI routine generation, and Settings destructive actions.
- Custom toggles, icon buttons, and compact workout controls must expose meaningful content descriptions, roles, states, and at least 48dp touch targets.
- Charts, metric cards, toggle rows, and custom Canvas visualizations need semantic summaries or merged labels so assistive tech does not expose unlabeled shapes and disjoint controls.

Research anchors:

- Android edge-to-edge design: https://developer.android.com/design/ui/mobile/guides/layout-and-content/edge-to-edge
- Compose edge-to-edge setup: https://developer.android.com/develop/ui/compose/system/setup-e2e
- Window insets: https://developer.android.com/develop/ui/compose/layouts/insets
- Compose accessibility: https://developer.android.com/develop/ui/compose/accessibility
- Core app quality: https://developer.android.com/docs/quality-guidelines/core-app-quality

## 9. Feature Target State

### Home

- Home is the cockpit.
- It must show the next best action, not just passive metrics.
- It must degrade gracefully when profile, Health Connect, nutrition, or workouts are missing.
- Startup must not block on Health Connect full sync or AI calls.

### Training

- Routine creation, routine generation, active workout, history, and completion must be resilient.
- Active workout must preserve state across rotation, app switch, lock/unlock, and process recreation where feasible.
- Active workout logging must use bounded, targeted persistence; set completion and draft updates must not serialize or re-import the entire app state.
- Finish flow must be idempotent.
- Completion AI debrief must not block saving the workout.
- Completion navigation must not wait for Gemini. A failed or slow debrief must show local summary plus retry/status, not lose or delay the workout.

### Nutrition

- Manual meal logging remains complete without AI.
- Meal scan must handle no camera, denied camera, revoked camera, oversized photo, offline, Gemini timeout, invalid JSON, and local fallback.
- Barcode scan target must be explicit:
  - Either it is a manual code capture flow, or
  - It includes product lookup, offline behavior, not-found handling, and manual fallback.

### Coach

- Advice must be grounded in available user data and declare data quality.
- Missing or stale Health Connect metrics must not be treated as known zero values.
- AI advice must return structured JSON and be validated before display.

### Progress

- Progress screens must show trends, data quality, and enough context to avoid false precision.
- Weight, body measurements, performance progression, and recovery signals must remain separable in the data model.

### Settings

- Settings is the control center for Health Connect, AI, privacy, telemetry, theme, feedback, profile, and local data.
- Destructive actions require confirmation.
- Health Connect permissions are managed transparently with direct links to system settings.
- Telemetry, if enabled, requires explicit user opt-in.

## 10. Performance Target State

Performance requirements:

- No heavy work on the main thread.
- No blocking Health Connect sync during initial UI rendering.
- No broad Room mirror/import or full JSON serialization on startup or critical input paths.
- No duplicate network calls after recomposition or lifecycle resume.
- Periodic foreground refresh loops must be visible-lifecycle aware and must not keep doing Health Connect or dashboard work from retained off-screen top-level back stacks.
- Compose parameters should be stable or immutable where practical.
- List rows and cards must avoid avoidable recomposition.
- Startup, first navigation, active workout logging, scanner launch, and settings scroll must be covered by benchmark or profile evidence.
- Debug emulator jank is only a signal; production severity must be assigned from profileable or release macrobenchmark runs.
- Profileable cold start, warm start, top-level navigation switches, settings scroll, and active workout logging must have explicit p50/p95 thresholds before release.
- `adb shell am start -W` must complete without timeout on the QA emulator; any timeout or input-dispatch timeout is release-blocking until explained and accepted.
- Top-level navigation must stay below the agreed jank threshold in `gfxinfo`, Macrobenchmark, or Perfetto artifacts; 90%+ janky frame runs are not acceptable even in debug without a documented root cause.
- Hostile display changes must recover cleanly from width/density/font changes, rotation/resizing, back/home/recents, and app switching without black-frame stalls that persist beyond the next draw.

Baseline Profiles:

- Generate Baseline Profiles with Macrobenchmark `BaselineProfileRule` for every release train.
- Add an actual profile producer/generation workflow using the AndroidX Baseline Profile Gradle plugin or an equivalent documented path.
- Benchmark checks that require a profile are not enough; the release workflow must also regenerate the profile from critical user journeys.
- Target profile must cover:
  - Cold start to Home
  - Bottom navigation between all top-level tabs
  - Active workout start and set logging
  - Nutrition add meal and scanner entry
  - Settings/profile form
  - Room-backed dashboard read
- Macrobenchmark helpers must fail when required UI targets are missing; silent text-tap skips are not acceptable.

Diagnostics:

- JankStats is useful for runtime diagnostics but not a substitute for Macrobenchmark or generated baseline profiles.
- Performance telemetry must be privacy-safe and opt-in before upload.

Research anchors:

- Baseline Profiles: https://developer.android.com/baseline-profiles
- Compose stability: https://developer.android.com/develop/ui/compose/performance/stability
- Strong skipping: https://developer.android.com/develop/ui/compose/performance/stability/strongskipping
- Compose performance codelab: https://developer.android.com/codelabs/jetpack-compose-performance

## 11. Backend, Security, and Developer Experience

API boundary requirements:

- Gemini API key auth uses headers, not query parameters.
- API services expose typed result/failure contracts.
- AI services have feature-specific timeout and retry policy.
- Remote errors are mapped to user-safe messages and diagnostic-safe internal categories.
- Before production release, AI mode must be explicitly decided and signed off: BYOK accepted, server gateway implemented, OAuth-mediated access, or AI scoped out.

Secrets:

- No production secret in `BuildConfig`.
- No committed plaintext API key.
- No URL-based key transmission.
- Keystore-only Gemini key storage after migration.
- Legacy DataStore key is cleared after successful migration.

Telemetry:

- Telemetry upload requires explicit app-level opt-in.
- Tokens must not be embedded statically in the APK.
- Logs must redact tokens, API keys, auth headers, health data, and user-entered nutrition/training notes.
- If telemetry upload is enabled, queued events must flush on a bounded interval and lifecycle boundary, not only when max batch size is reached.
- Telemetry must remain useful when disabled locally: diagnostics should still be inspectable through local, redacted logs during QA.
- Production telemetry requires a documented endpoint, auth/token handling, payload sample, retention policy, and Data Safety review before enabling.

Developer experience:

- CI must run on `pull_request` and protected-branch `push`, not only manual dispatch.
- No committed absolute local-machine paths in Gradle defaults.
- README must describe:
  - JDK requirement
  - Android SDK setup
  - Debug build
  - Unit tests
  - Connected tests
  - Health Connect emulator setup
  - Gemini key setup
  - Release signing readiness
- Dependency updates should prefer stable AndroidX releases unless an alpha feature is explicitly required.
- Health Connect should stay on stable `1.1.0` unless an alpha feature is explicitly required.
- Evaluate Room stable updates before the next modernization pass, then re-run the full migration chain and FK orphan tests.
- Migration readiness marker generation must be wired into CI/release onboarding and release artifacts must be blocked without fresh marker evidence or an owner-approved exception.
- CI/release validation must run migration-marker generation before release artifacts are accepted, or the release owner must record a dated exception explaining why marker evidence is diagnostic-only for that release.
- Settings copy must match implementation: Keystore/encrypted local storage for Gemini keys, not generic app preferences.
- Release builds should enable shrinking/obfuscation or carry an owner-approved written exception.
- Room runtime source-of-truth promotion must be gated by the readiness signal, or the readiness signal must be explicitly reframed as diagnostic-only.

## 12. Test Strategy

Minimum validation for any change:

```text
./gradlew.bat :app:assembleDebug --console=plain
```

Required tests by area:

- Mappers: unit tests.
- UseCases: unit tests.
- Repository logic: unit tests.
- Room migrations: instrumentation tests.
- Health Connect sync policy: unit tests with fake data source and token states.
- AI JSON parsing: unit tests with valid, invalid, missing-field, English, oversized, and fallback cases.
- Navigation routes: unit tests for type-safe route behavior.
- UI state reducers: unit tests for loading/success/error/partial states.
- Runtime Room mutations: unit/integration tests proving deletes remove rows and cannot resurrect after process restart.
- Hot-path persistence: benchmark or test guard showing active set logging and meal save do not perform full-state JSON mirror/import work.

Mandatory exploratory QA matrix before release:

- Fresh install
- Upgrade install
- No profile
- Existing profile
- No Health Connect provider
- Health Connect installed but no permissions
- Partial Health Connect permissions
- Revoked permissions while app is open
- No health data
- Large health dataset requiring pagination
- Offline
- Slow network
- Gemini 429/rate limit
- Gemini invalid JSON
- Camera denied once
- Camera denied permanently
- App switch and return
- Lock/unlock
- Back gesture on every screen
- Rapid taps on primary actions
- Rotation where supported
- 360x640 compact phone
- 360x800 compact phone
- 1080x2400 large phone
- Tablet/foldable width class
- Font scale 1.3 and 1.5
- Light and dark theme
- Gesture navigation and 3-button navigation
- Density override and display-size override
- Back/home/recents recovery after display resize

Optimization review matrix before release:

- `am start -W` cold launch with no timeout
- Profileable cold start, warm start, and hot start
- Top-level navigation jank on compact and large-phone profiles
- First-run setup where Health Connect CTA is below the fold
- Health Connect rationale before system prompt
- Partial Health Connect rationale copy after granting only some metrics
- Partial permissions with at least one granted and one denied metric
- Expired changes token recovery for one metric only
- Missing steps permission while other Health Connect caches remain intact
- Gemini 429 with backoff and local fallback
- Workout finish with slow Gemini response
- Active workout set logging with large local history
- Delete/discard flows followed by process restart
- AI routine generation at 360x640 and font scale 1.3+
- Room migration from dirty v11 data with orphaned child rows
- Macrobenchmark failure behavior when a required label/test tag is absent

## 13. Definition of Done

A TrainIQ feature is complete only when:

- It follows the architecture flow.
- It has explicit loading, success, error, empty, offline, and permission states where relevant.
- It passes compile checks.
- It has tests for business logic and data mapping.
- It does not introduce unbounded main-thread, network, Health Connect, or image-processing work.
- It works on compact phones without clipped controls.
- It preserves state across app switch and back navigation where user data could be lost.
- It handles partial Health Connect and disabled AI.
- It does not log secrets or sensitive health data.
- It updates this blueprint if a target-state decision changes.

## 14. vNext Developer Target Addendum

This addendum converts the 2026-05-10 vNext research into developer-friendly target state. It is intentionally phased: confirmed target state belongs in `Now`; speculative ideas remain `Decision needed` until product, privacy, security, or medical/legal owners approve scope.

Research docs:

- `docs/TrainIQ_vNext_Research.md`
- `docs/TrainIQ_Target_State_Backlog.md`
- `docs/TrainIQ_Architecture_Decisions.md`

### 14.1 Product Backbone

TrainIQ vNext target:

```text
Local-first health data + explicit consent + reliable training/nutrition logs
    -> deterministic readiness and data-quality model
    -> bounded Gemini 2.5 Flash summaries where enabled
    -> one calm next-best action
```

Confirmed target-state requirements:

- First-run onboarding must capture goal, training experience, schedule, equipment, constraints, Health Connect consent, AI mode, notification/reminder preferences, and privacy expectations.
- Home must present one primary next-best action, one reason, a data-quality label, and a fallback action.
- Recovery/readiness must be conservative, data-quality-aware, and non-clinical. It may guide training/recovery choices, but must not diagnose, treat, or claim medical certainty.
- Progress must explain trends with context: timeframe, data quality, relevant inputs, and uncertainty when inputs are missing or stale.
- Export/import/delete controls must be explicit about scope and must never include secrets.

Decision needed:

- Supported goal types and coaching assertiveness.
- Whether to add manual soreness/energy/readiness check-ins.
- Reminder categories and default cadence.
- Export formats and scope.

### 14.2 Screen and Flow Acceptance Criteria

Onboarding:

- User can complete onboarding with Health Connect disabled and AI disabled.
- Skipped capabilities remain visible as Settings/Home actions.
- Entered state survives rotation, app switch, and process recreation where feasible.
- Compact and large-font evidence exists at 360x640 and 360x800, font scale 1.3 and 1.5.

Home:

- First draw does not wait for AI or Health Connect full sync.
- Missing or denied Health Connect metrics are never silently treated as measured zero values.
- Primary action has a local deterministic fallback when AI is disabled, offline, rate-limited, or invalid.

Recovery/readiness:

- Output includes recommendation, confidence/data quality, inputs used, missing/stale inputs, and fallback action.
- Copy avoids diagnosis, treatment, clinical promises, and unsupported medical claims.
- AI output is schema-validated and replaceable by local fallback.

Progress/export:

- Charts have semantic summaries.
- Trends include timeframe and data-quality context.
- Export preview shows included categories before generating a file.
- Export excludes API keys, telemetry tokens, signing data, and internal-only diagnostics by default.

### 14.2.1 Ready-to-Use Screen State Matrix

Every primary screen must make its state explicit and verifiable before release. These requirements apply alongside the screen-specific criteria above.

Home:

- Loading: first draw shows locally available cached/profile data or skeleton content without waiting for AI or a full Health Connect sync.
- Empty: no profile, no Health Connect data, or no training history produces one setup action and does not render missing metrics as zero.
- Error/offline: AI/network/Health Connect failures show local fallback guidance and a retry/manage-access action where relevant.
- Success: one next-best action, reason, data-quality label, and fallback action are visible on compact phones.
- Verification: unit tests for next-best-action selection, UI dump for no-data/partial-data states, launch smoke with empty crash buffer.

Training and active workout:

- Loading: routine/session reads do not block the top-level Training tab indefinitely.
- Empty: no routine and empty active routine states expose a direct setup path.
- Error/offline: local logging remains available when AI/network is unavailable; save failures show retry-safe copy.
- Success: adding a day, adding/removing exercises, logging sets, finishing, and discarding persist through app restart without stale row resurrection.
- Verification: repository/process-restart tests for targeted Room writes, active-workout runtime smoke, compact/font-scale QA for logger controls.

Nutrition and scanner:

- Loading: camera/model work is cancelable and never blocks manual meal entry.
- Empty: no meals/recipes/products produces direct add actions.
- Error/offline: camera denied, camera unavailable, barcode not found, invalid AI JSON, rate limit, and offline states keep manual entry available.
- Success: scan or manual entry produces editable nutrition rows before save.
- Verification: state reducer tests, scanner permission/device smoke, compact/font-scale QA for permission and result states.

Coach:

- Loading: advice generation is cancelable and bounded by feature-specific timeout.
- Empty: missing profile/history explains which setup action unlocks better advice.
- Error/offline: AI disabled, missing key, 429/rate-limit, invalid JSON, and no network use deterministic fallback copy.
- Success: advice includes inputs used, missing/stale inputs, confidence/data quality, and non-clinical wording.
- Verification: AI contract tests, fallback tests, user-safe error copy review.

Progress and settings:

- Loading: local progress/settings render from Room/DataStore without network dependency.
- Empty: no measurements/history explains what to log next.
- Error/offline: export/import/settings actions fail closed with no secret or health-data leakage.
- Success: charts expose semantic summaries; Settings exposes AI, Health Connect, export/delete, theme, and privacy controls.
- Verification: chart semantics tests, Settings copy tests, export inspection, TalkBack/Switch Access manual signoff.

### 14.3 Backend and Data Responsibilities

Confirmed target-state requirements:

- Room remains the authoritative source for app-owned data.
- JSON import/export is compatibility tooling, not normal runtime mutation authority.
- Every normal user mutation must have a bounded targeted DAO path or documented full-replacement transaction semantics.
- Active workout finish/edit/undo, routine edit/delete, meal save/delete, recipe save/delete, measurement add/delete, and profile writes must have process-restart correctness tests before the persistence migration is considered complete.
- Health Connect sync state must preserve per-metric independence: denied, stale, failed, unavailable, and synced states must not poison unrelated metrics.
- Health Connect cache/token metadata may remain in DataStore only while payloads are small and cleanup/debug requirements are simple. Move it to Room when payload growth, migration semantics, or privacy review requires stronger lifecycle control.
- `TrainIqDataCoordinator` should keep being decomposed into focused concrete services/repositories after hot-path persistence is stable.

### 14.4 Privacy, Security, and AI Boundaries

Confirmed target-state requirements:

- Health Connect background reads require explicit user value, runtime feature/permission support, Play/Data Safety parity, and owner signoff.
- Telemetry upload is disabled unless the user explicitly opts in and release docs define endpoint, auth, payload, retention, and redaction.
- Gemini key handling must not use URL query parameters or logs.
- AI features must use structured JSON outputs, feature-specific timeouts, bounded retry/backoff, cancellation propagation, user-safe errors, and deterministic fallback where feasible.

Decision needed:

- Production AI mode: BYOK/direct-client, backend gateway, OAuth-mediated access, hybrid, or AI scoped out.
- Background Health Connect release justification.
- Telemetry upload policy and retention.
- Barcode product lookup data source and offline/not-found policy.

### 14.5 Android Quality and Release Criteria

Confirmed target-state requirements:

- Release readiness is gated, not only code-complete.
- Manual TalkBack and Switch Access signoff is required for onboarding, active workout, scanner, Health Connect rationale, AI routine generation, Settings destructive actions, and Progress charts.
- Health Connect runtime matrix must cover provider missing/update, no permission, partial permission, revoke while open, and background-read unavailable/granted states.
- Performance thresholds must be owner-approved for startup, top-level navigation, active workout logging, scanner launch, and settings scroll.
- Physical-device profileable or release macrobenchmark evidence is required before performance claims.
- CI/release must either generate Room migration verification markers before artifacts are accepted or record a dated owner exception that marker evidence is diagnostic-only for that release.
- Minified/profileable or release-like smoke tests must run before production upload.

### 14.6 Phased vNext Roadmap

Now:

- Close P0/P1 data, release, accessibility, Health Connect, and performance evidence gaps.
- Add guided onboarding target and tests.
- Implement deterministic Home next-best-action selector.
- Continue targeted Room writes one flow at a time.
- Run Health Connect runtime matrix.

Next:

- Add goal/adherence model.
- Add conservative recovery/readiness model.
- Add opt-in reminders.
- Add progress narrative and export.
- Split concrete backend/data services after persistence hot paths are stable.

Later:

- Adaptive plan changes and deload suggestions.
- Barcode product lookup if data-source policy is approved.
- Cloud backup/account sync if privacy/security model is approved.
- Gemini Nano/local multimodal feedback where platform support and product value are clear.
- Wear OS companion only if training/rest-timer use cases justify the maintenance cost.

## 15. Source References From This Review

Local evidence:

- `D:\GitHub\TrainIQ\qa-cycle-runtime\full-review\launch-logcat.txt`
- `D:\GitHub\TrainIQ\qa-cycle-runtime\full-review\home-small.xml`
- `D:\GitHub\TrainIQ\qa-cycle-runtime\optimization-review\foreground-current.xml`
- `D:\GitHub\TrainIQ\qa-cycle-runtime\optimization-review\compact-360x640-font115.xml`
- `D:\GitHub\TrainIQ\qa-cycle-runtime\optimization-rerun-2026-05-08\launch-main.txt`
- `D:\GitHub\TrainIQ\qa-cycle-runtime\optimization-rerun-2026-05-08\home-delayed.xml`
- `D:\GitHub\TrainIQ\qa-cycle-runtime\optimization-rerun-2026-05-08\compact-360x640-font130.xml`
- `D:\GitHub\TrainIQ\qa-cycle-runtime\optimization-rerun-2026-05-08\logcat-after-launch.txt`
- `C:\Users\menno\AppData\Local\Temp\trainiq-runtime-qa-20260508-230642\launch-start-w.txt`
- `C:\Users\menno\AppData\Local\Temp\trainiq-runtime-qa-20260508-230642\gfxinfo-nav-flow.txt`
- `C:\Users\menno\AppData\Local\Temp\trainiq-runtime-qa-20260508-230642\gfxinfo-final.txt`
- `C:\Users\menno\AppData\Local\Temp\trainiq-runtime-qa-20260508-230642\crash-buffer-final.txt`
- `D:\GitHub\TrainIQ\qa-cycle-runtime\worker-a-current-2026-05-08\launch-main.txt`
- `D:\GitHub\TrainIQ\qa-cycle-runtime\worker-a-current-2026-05-08\gfxinfo-framestats.txt`
- `D:\GitHub\TrainIQ\qa-cycle-runtime\worker-a-current-2026-05-08\crash-buffer.txt`
- `D:\GitHub\TrainIQ\runtime-gemini-test\active-workout-start.xml`
- `D:\GitHub\TrainIQ\runtime-gemini-test\routine-ai-after-generate.xml`
- `D:\GitHub\TrainIQ\TrainIQ-Project\app\src\main\java\com\trainiq\data\datasource\HealthConnectDataSource.kt`
- `D:\GitHub\TrainIQ\TrainIQ-Project\app\src\main\java\com\trainiq\data\repository\RoomTrainIqRuntimeStore.kt`
- `D:\GitHub\TrainIQ\TrainIQ-Project\app\src\main\java\com\trainiq\data\migration\JsonRoomImportPlanner.kt`
- `D:\GitHub\TrainIQ\TrainIQ-Project\app\src\main\java\com\trainiq\ai\services\AiServices.kt`
- `D:\GitHub\TrainIQ\TrainIQ-Project\app\src\main\java\com\trainiq\ai\services\GeminiJsonSchemas.kt`
- `D:\GitHub\TrainIQ\TrainIQ-Project\app\src\main\java\com\trainiq\ai\services\AiSupport.kt`
- `D:\GitHub\TrainIQ\TrainIQ-Project\app\src\main\java\com\trainiq\data\remote\GeminiApi.kt`
- `D:\GitHub\TrainIQ\TrainIQ-Project\app\src\main\java\com\trainiq\features\workout\WorkoutScreen.kt`
- `D:\GitHub\TrainIQ\TrainIQ-Project\app\src\main\java\com\trainiq\features\settings\SettingsSection.kt`
- `D:\GitHub\TrainIQ\TrainIQ-Project\app\src\main\java\com\trainiq\core\health\HealthConnectPermissionsRationaleActivity.kt`
- `D:\GitHub\TrainIQ\TrainIQ-Project\app\src\main\java\com\trainiq\navigation\TrainIqNav.kt`
- `D:\GitHub\TrainIQ\TrainIQ-Project\app\src\main\java\com\trainiq\core\database\TrainIqMigrations.kt`
- `D:\GitHub\TrainIQ\TrainIQ-Project\app\src\main\baseline-prof.txt`
- `D:\GitHub\TrainIQ\TrainIQ-Project\macrobenchmark\src\main\java\com\trainiq\macrobenchmark\TrainIqStartupBenchmark.java`
- `D:\GitHub\TrainIQ\TrainIQ-Project\gradle\libs.versions.toml`
- `D:\GitHub\TrainIQ\.github\workflows\android.yml`
- `D:\GitHub\TrainIQ\TrainIQ-Project\docs\release\owner-action-tracker.md`
- `D:\GitHub\TrainIQ\TrainIQ-Project\docs\architecture\ai-gateway-decision-record.md`
- `D:\GitHub\TrainIQ\TrainIQ-Project\docs\qa\talkback-switch-access-test-script.md`

Official and high-quality research:

- Fresh webresearch check, accessed 2026-05-10: Android Developers Health Connect permissions UX confirmed manage-access and granted-permission clarity; Android Developers Compose accessibility/scalable-content guidance confirmed large-font/scalable-content verification; Android Developers Baseline Profiles guidance confirmed release-like startup/profile verification remains required.
- Android core app quality: https://developer.android.com/docs/quality-guidelines/core-app-quality
- Android edge-to-edge: https://developer.android.com/design/ui/mobile/guides/layout-and-content/edge-to-edge
- Compose edge-to-edge setup: https://developer.android.com/develop/ui/compose/system/setup-e2e
- Android 15 behavior changes: https://developer.android.com/about/versions/15/behavior-changes-15
- Android 16 behavior changes: https://developer.android.com/about/versions/16/behavior-changes-16
- Adaptive navigation: https://developer.android.com/develop/ui/compose/layouts/adaptive/build-adaptive-navigation
- Compose accessibility: https://developer.android.com/develop/ui/compose/accessibility
- Compose accessibility testing: https://developer.android.com/develop/ui/compose/accessibility/testing
- Compose scalable content: https://developer.android.com/develop/ui/compose/accessibility/scalable-content
- Compose stability/performance: https://developer.android.com/develop/ui/compose/performance/stability
- Baseline Profiles: https://developer.android.com/baseline-profiles
- Create Baseline Profiles: https://developer.android.com/topic/performance/baselineprofiles/create-baselineprofile
- Configure Baseline Profiles: https://developer.android.com/topic/performance/baselineprofiles/configure-baselineprofiles
- Health Connect read data: https://developer.android.com/health-and-fitness/guides/health-connect/develop/read-data
- Health Connect sync: https://developer.android.com/health-and-fitness/health-connect/sync-data
- Health Connect permissions UX: https://developer.android.com/health-and-fitness/guides/health-connect/design/permissions
- Health Connect publishing declaration: https://developer.android.com/health-and-fitness/health-connect/declare-access
- Health Connect releases: https://developer.android.com/jetpack/androidx/releases/health-connect
- Room releases: https://developer.android.com/jetpack/androidx/releases/room
- Room foreign keys: https://developer.android.com/reference/androidx/room/ForeignKey
- Gemini structured output: https://ai.google.dev/gemini-api/docs/structured-output
- Gemini thinking: https://ai.google.dev/gemini-api/docs/thinking
- Gemini models: https://ai.google.dev/gemini-api/docs/models
- Gemini API keys: https://ai.google.dev/gemini-api/docs/api-key
- Gemini rate limits: https://ai.google.dev/gemini-api/docs/rate-limits
- Google Cloud API key best practices: https://cloud.google.com/docs/authentication/api-keys-best-practices
