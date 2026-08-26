# TrainIQ Full Tab and Backend Audit

Audit date: 2026-07-12  
Scope: `TrainIQ-Project/app`, all top-level and nested routes, backend/data paths, tests and release readiness.  
Method: read-only source trace, targeted Gradle gates and emulator launch smoke. Existing working-tree changes were treated as user-owned and were not altered.

## Selected Finding Implementation - 2026-07-12

- `TAB-2026-07-12-001`: done. Nutrition editor identity, scalar drafts, recipe/meal lists, editable AI items and contextual targets now survive activity recreation through saveable state and guarded hydration. Save/Cancel reset behavior is preserved.
- `TAB-2026-07-12-004`: done. Nutrition, Coach and Settings fatal data observations are reloadable; each error surface exposes an accessible `Opnieuw proberen` action. Behavioral coroutine coverage proves failure, reload, second subscription and recovery.
- `TAB-2026-07-12-006`: partially done by design. Only workout-completion/debrief scheduling moved into focused use cases and a scheduler boundary; the wider coordinator remains unchanged to avoid an unnecessary architecture rewrite.
- `TAB-2026-07-12-007`: done. Local completion remains immediate and atomic; AI enrichment is unique WorkManager work with network constraints, one bounded three-attempt chain, cold-process-safe direct Room reads and an idempotent conditional update. Completion-summary reads never silently start another chain.
- Verification: `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, and `:app:compileDebugAndroidTestKotlin` PASS. No Room schema migration was needed.

## Result

- All six top-level tabs and their reachable nested flows were mapped to ViewModels, use cases, repositories and persistence/integration boundaries.
- No P0 crash, data-corruption or secret-storage defect was found in the inspected paths.
- Highest risks are lifecycle loss of unsaved Nutrition drafts (P1), absent CI despite documentation claiming CI exists (P1), and unresolved production release owner gates (P1).
- Current estimated target-state alignment: **78%**. Architecture, Room, Health Connect, structured AI output and local tests are strong; lifecycle recovery, automated release enforcement and physical-device evidence are weakest.
- `TrainIQ_Target_State_Blueprint.md` was not changed; the current target is sufficiently explicit for these findings.

## Tab and Flow Coverage

| Area | Covered flows | Code/backend trace | Result |
|---|---|---|---|
| Onboarding | setup, permission explanation, completion, guided tour | preferences, type-safe navigation | Covered; tour-step recreation gap |
| Start | dashboard, setup actions, Health Connect refresh, routes to Training/Coach/Settings | `HomeViewModel` -> dashboard/health use cases -> repositories | Covered; loading/retry present |
| Training | routines, library, history, detail, active workout, exercise history, processing, completion | `WorkoutViewModel` -> workout use cases/repository -> Room and AI debrief | Covered; AI enrichment durability gap |
| Voeding | Vandaag, Producten, Recepten, Historie, manual/AI drafts, barcode and camera | `NutritionViewModel` -> nutrition use cases/repository -> Room/remote lookup/AI | Covered; draft recreation loss and missing fatal-error retry |
| Trend | body/training tabs, manual measurement, scale scan | `ProgressViewModel` -> progress use cases/repository -> Room | Covered; scanner result survives via `SavedStateHandle` |
| Coach | Week, Profiel, Doel/advies | `CoachViewModel` -> coach/profile use cases -> Room/AI | Covered; profile draft recreation/overwrite risk and missing retry |
| Meer | profile, Health Connect, Samsung steps, AI keys, privacy/export, theme, reminders, onboarding reopen | `SettingsViewModel` -> preferences/security/health/import-export services | Covered; missing fatal-error retry |
| Shared navigation | bottom bar/rail, back stacks, scanner results, adaptive layout | serialized routes and saved-state result passing | Covered; type-safe routes and top-level state restoration present |

## Prioritized Findings

### TAB-2026-07-12-001

- finding_id: `TAB-2026-07-12-001`
- priority: `P1`
- area: `Android lifecycle`
- status: `open`
- current evidence: Nutrition keeps editor identity saveable, but product, recipe, meal and AI draft fields in ordinary `remember` state (`features/nutrition/NutritionScreen.kt:551-594`). Recreation can restore an open editor with blank inputs.
- external sources: none required; directly evidenced by local state ownership.
- expected target-state behavior: unfinished structured drafts survive configuration change/process recreation, or the editor closes consistently without appearing to retain an empty draft.
- concrete recommended fix: move structured drafts and editor identity into the ViewModel with `SavedStateHandle`; initialize/reset them through explicit open, save and cancel events.
- regression risk: medium; initialization and reset order can affect editing existing items.
- minimal verification: instrumentation tests for product, recipe, meal and AI editors that enter data, recreate the activity and assert exact restoration plus correct save/cancel behavior.
- owner suggestion: Android/UI-state.

### TAB-2026-07-12-002

- finding_id: `TAB-2026-07-12-002`
- priority: `P1`
- area: `release`
- status: `open`
- current evidence: `.github/` contains no workflow files, while `docs/TrainIQ_Target_State_Progress.md:77` and `TrainIQ-Project/README.md:84-89` describe CI migration and signed-release jobs.
- external sources: none required; repository state and documentation conflict directly.
- expected target-state behavior: unit, lint, assemble, Android-test compile, migration-marker and release-signing gates run automatically on the documented triggers.
- concrete recommended fix: restore/add least-privilege workflows for PR debug gates, emulator migration proof and a dependent signed-release job with protected secrets and artifacts.
- regression risk: low for app behavior; medium for pipeline setup and secret permissions.
- minimal verification: successful PR workflow plus protected signed-release workflow; verify required checks are branch-protection gates.
- owner suggestion: Android/release engineering.

### TAB-2026-07-12-003

- finding_id: `TAB-2026-07-12-003`
- priority: `P1`
- area: `release`
- status: `blocked`
- current evidence: `docs/TrainIQ_Target_State_Progress.md:120-135` leaves privacy/security signoff, manual accessibility evidence, background Health Connect/production AI mode and version strategy open. `DIRECT_APK_BUG_FREE_READINESS_PLAN.md:346-364` blocks readiness until release runtime gates pass.
- external sources: none required; these are explicit owner gates.
- expected target-state behavior: release claims require signed owner decisions and physical/release-like runtime evidence.
- concrete recommended fix: close the owner checklist in order: production AI boundary, Data Safety/privacy parity, accessibility signoff, version policy, signed release install/launch/crash smoke.
- regression risk: low; this is governance/evidence work.
- minimal verification: completed release checklist with linked evidence and a signed release artifact passing install, launch and crash scan.
- owner suggestion: product owner plus release engineering.

### TAB-2026-07-12-004

- finding_id: `TAB-2026-07-12-004`
- priority: `P2`
- area: `UX`
- status: `open`
- current evidence: fatal top-level errors in Nutrition (`NutritionScreen.kt:962-964`), Coach (`CoachScreen.kt:552-554`) and Settings (`SettingsSection.kt:762-777`) show messages without retry. Home already provides the desired retry precedent (`HomeScreen.kt:289-299`).
- external sources: none required; local consistency establishes the expectation.
- expected target-state behavior: recoverable top-level failures expose a clear, accessible retry; unrecoverable failures explain the next action.
- concrete recommended fix: add a ViewModel retry/refresh event per screen and a minimum-48dp action in the error state.
- regression risk: low.
- minimal verification: force repository failure then recovery; retry transitions through loading to success without relaunch.
- owner suggestion: Android/UI-state.

### TAB-2026-07-12-005

- finding_id: `TAB-2026-07-12-005`
- priority: `P2`
- area: `Android lifecycle`
- status: `open`
- current evidence: Coach profile edits use ordinary `remember` (`CoachScreen.kt:490-498`) and `LaunchedEffect(profile)` overwrites fields from persistence (`CoachScreen.kt:512-524`).
- external sources: none required.
- expected target-state behavior: dirty form data remains stable until explicit Save or Cancel.
- concrete recommended fix: own the edit draft and dirty flag in the ViewModel/`SavedStateHandle`; initialize only when not dirty or when profile identity/version intentionally changes.
- regression risk: medium.
- minimal verification: edit all fields, recreate and trigger profile refresh; values and validation remain until Save/Cancel.
- owner suggestion: Android/UI-state.

### TAB-2026-07-12-006

- finding_id: `TAB-2026-07-12-006`
- priority: `P2`
- area: `backend`
- status: `open`
- current evidence: focused repositories mostly delegate to `TrainIqDataCoordinator` (`data/repository/FocusedRepositories.kt:43-160`); workout completion combines IDs, calculations, comparisons, AI input and persistence in `TrainIqRepository.kt:504-624`; several use cases are pass-throughs (`domain/usecase/UseCases.kt:189-258`).
- external sources: none required; this conflicts with the repo's own domain/use-case boundary.
- expected target-state behavior: domain policies and orchestration live in focused use cases; repositories expose bounded atomic data operations.
- concrete recommended fix: incrementally extract workout completion first into an orchestrating use case; do not perform a broad coordinator rewrite.
- regression risk: medium; active-session recovery and completion ordering are sensitive.
- minimal verification: existing completion, repository and Room transaction tests plus focused new orchestrator tests.
- owner suggestion: Android/data-domain.

### TAB-2026-07-12-007

- finding_id: `TAB-2026-07-12-007`
- priority: `P2`
- area: `feature`
- status: `open`
- current evidence: workout completion persists a local fallback, then launches AI enrichment in an unobserved `scope.launch { runCatching { ... } }` without durable pending/retry/failure status (`TrainIqRepository.kt:603-623`).
- external sources: none required.
- expected target-state behavior: local completion is immediate; requested AI enrichment has explicit pending/completed/failed state and idempotent retry.
- concrete recommended fix: persist debrief status and run unique background work or an explicit lifecycle-bound retry path; preserve the local fallback.
- regression risk: low-medium; incorrect unique-work keys could duplicate provider calls.
- minimal verification: offline completion, process death while pending, successful retry and exactly-once provider-call tests.
- owner suggestion: Android/AI integration.

### TAB-2026-07-12-008

- finding_id: `TAB-2026-07-12-008`
- priority: `P2`
- area: `tests`
- status: `open`
- current evidence: 85 JVM test files and 18 instrumentation files exist, but no JaCoCo/Kover plugin, report or threshold is configured.
- external sources: none required.
- expected target-state behavior: coverage risk in critical data/domain paths is measurable and protected without incentivizing superficial tests.
- concrete recommended fix: add a report-only baseline first, then enforce focused critical-package and changed-code thresholds after reviewing the baseline.
- regression risk: low; overly broad initial thresholds could create noisy gates.
- minimal verification: reproducible coverage report plus an intentional uncovered critical branch demonstrating the selected gate.
- owner suggestion: Android/test engineering.

### TAB-2026-07-12-009

- finding_id: `TAB-2026-07-12-009`
- priority: `P2`
- area: `performance`
- status: `open`
- current evidence: the committed baseline profile contains only 18 class rules (`app/src/main/baseline-prof.txt`), while the macrobenchmark module has richer startup/navigation/settings/active-workout journeys. With no CI, profile freshness and benchmark regressions are unguarded.
- external sources: none required for the gap; physical/profileable evidence remains the acceptance source.
- expected target-state behavior: generated profile and release-like benchmark evidence cover critical journeys and are refreshed deliberately.
- concrete recommended fix: add scheduled/device-lab profile generation and benchmark reporting; compare generated output before accepting profile updates.
- regression risk: low for app logic; device variance must be controlled.
- minimal verification: baseline generation plus cold-start and frame benchmarks on a stable physical device.
- owner suggestion: Android/performance.

### TAB-2026-07-12-010

- finding_id: `TAB-2026-07-12-010`
- priority: `P3`
- area: `tests`
- status: `open`
- current evidence: AI routing supports optional cross-provider fallback (`ai/services/AiProviders.kt:182-205`), while retry behavior distinguishes 429 from timeout/permanent failures (`ai/services/AiSupport.kt:75-107`). This privacy-sensitive routing policy needs explicit end-to-end contract coverage.
- external sources: none required.
- expected target-state behavior: fallback order, consent setting, provider used and retry boundaries are deterministic and tested.
- concrete recommended fix: add contract tests for fallback on/off, missing keys, 429, timeout, permanent 4xx and `providerUsed`; confirm Settings copy communicates possible second-provider transfer.
- regression risk: low if initially test-only.
- minimal verification: focused `AiProviderRouter` and `AiSupport` tests.
- owner suggestion: Android/AI integration.

### TAB-2026-07-12-011

- finding_id: `TAB-2026-07-12-011`
- priority: `P3`
- area: `Android lifecycle`
- status: `open`
- current evidence: guided-tour progress uses `remember` (`navigation/TrainIqNav.kt:197-202`) and recreation returns the overlay to the first destination (`TrainIqNav.kt:220-225`).
- external sources: none required.
- expected target-state behavior: the current guided-tour step survives recreation.
- concrete recommended fix: use bounded `rememberSaveable` state or persist tour progress.
- regression risk: low.
- minimal verification: recreate on every step and assert identical step and destination.
- owner suggestion: Android/navigation.

### TAB-2026-07-12-012

- finding_id: `TAB-2026-07-12-012`
- priority: `P3`
- area: `release`
- status: `open`
- current evidence: release minification is enabled (`app/build.gradle.kts:97-104`), but `app/proguard-rules.pro:23-30` keeps entire Room/local/database/data-model packages.
- external sources: none required for the local rule scope.
- expected target-state behavior: only reflection/serialization-required members are kept, backed by minified runtime smoke.
- concrete recommended fix: inspect R8 usage/mapping, replace blanket keeps with serializer-specific rules or annotations, and retain only proven reflection boundaries.
- regression risk: medium; aggressive narrowing can break import/export or JSON parsing.
- minimal verification: `assembleRelease`, mapping/usage inspection, then minified install/launch/import-export/AI JSON smoke.
- owner suggestion: Android/release engineering.

## Positive Evidence

- Navigation uses type-safe serialized routes, top-level save/restore and `SavedStateHandle` result passing for barcode/scale scans.
- Major screens expose explicit observable UI-state; shimmer loading is used broadly.
- Room uses explicit migrations and transactions; no destructive migration fallback was found in the inspected paths.
- Health Connect checks provider status, handles per-metric permissions/tokens and has token-expiry recovery.
- Gemini/OpenAI request structured JSON schema output; API keys are migrated to AES-GCM Android Keystore storage.
- Telemetry defaults off and uses an attribute allowlist; Android backup is disabled.
- Release signing inputs remain external and `checkReleaseSigningReadiness` validates completeness and keystore existence.

## Verification

| Check | Result | Evidence |
|---|---|---|
| Static route/tab/backend trace | PASS | All matrix rows mapped to route, UI-state and data boundary |
| `:app:testDebugUnitTest` | PASS | Gradle combined gate, 2026-07-12 |
| `:app:lintDebug` | PASS | Gradle combined gate, 2026-07-12 |
| `:app:compileDebugAndroidTestKotlin` | PASS | Gradle combined gate, 2026-07-12 |
| `:app:assembleDebug` | PASS | Isolated rerun after transient generated-file lock |
| `:app:checkReleaseSigningReadiness` | PASS | Signing configuration complete |
| `:macrobenchmark:compileProfileableJavaWithJavac` | PASS | Gradle combined gate |
| Initial combined assemble run | FAIL, infrastructure/transient | Windows denied hashing a generated D8 global-synthetics file; isolated rerun passed unchanged |
| Emulator install | PASS | `Medium_Phone_2 (AVD) - API 16` |
| Cold launch | PASS | `Status: ok`, `TotalTime: 6749 ms`, `WaitTime: 6815 ms` |
| Crash buffer after launch | PASS | empty |
| Home UI hierarchy | PASS | rendered TrainIQ Home and six navigation destinations |
| Full populated tab interaction | NOT RUN | emulator profile was setup-empty; destructive data seeding was outside this read-only audit |
| Manual TalkBack/Switch Access | NOT RUN | requires human release signoff |
| Physical-device performance | NOT RUN | no physical device was attached |

## Recommended Execution Order

1. Fix Nutrition and Coach draft restoration with focused recreation tests.
2. Restore CI so current local gates and migration/release checks become enforceable.
3. Add retry actions to Nutrition, Coach and Settings terminal errors.
4. Make workout AI enrichment durable and idempotent while keeping local completion immediate.
5. Close owner release decisions, then capture accessibility and physical-device performance evidence.
6. Incrementally improve domain boundaries, coverage measurement, baseline-profile freshness and R8 rules.

## Assumptions and Limits

- This was an audit implementation, not an app-code fix pass; no application source was intentionally changed.
- Existing modified files in Home, navigation, UI tests and QA/progress docs are user-owned work and remain untouched.
- Findings are based on the current worktree, including its uncommitted source state, and should be revalidated after those changes are committed or reverted by their owner.
