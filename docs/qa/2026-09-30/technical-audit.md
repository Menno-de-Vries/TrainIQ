# Technical audit — 2026-09-30

## Scope and provenance

Task A1, wave 1: read-only technical inspection of the shared primary checkout, base `d32922e`, task branch `codex/full-audit-2026-09-30`. App code, Gradle, devices, credentials and remote data were not changed or exercised. This file is the only A1 write. Priority denotes engineering impact; missing Play/owner certification is not a current itch.io gate.

Read the repository contract, blueprint, ADRs, full local-testing guide, privacy evidence pack, release risk notes and itch.io policy. Consulted relevant security/AI decision records, existing July full audit, September twenty-finding batches and September 24 refinement evidence to distinguish historical repaired findings. Repo-local target-state QA skill informed the finding schema; the parent-defined wave restriction overrides its build/runtime suggestions.

## Inventory and coverage

| Surface | Implementation truth and coverage | Limit |
| --- | --- | --- |
| Backend/accounts/auth | No app account or cloud persistence architecture is implemented. Android uses direct Gemini/OpenAI BYOK; a real Node `food-gateway/` implements the optional FatSecret barcode boundary. Inspected gateway server, adapter, fixtures and README. | No deployed endpoint or credentials tested. Local server intentionally has no production user auth/rate limiting; README requires separate authorized deployment. Do not invent an AI backend or require one for itch.io. |
| Persistence | Room v19, auto migrations 16→17→18 plus explicit chain; local JSON is import/export/legacy migration. Inspected DAO graph, runtime transaction methods, mapper inputs, coordinator create/edit calls and import sink. Full imports clear mirror tables transactionally, unlike the historical stale-row finding. | No migration/runtime gates in wave 1; no assertion that all schema edges passed today. |
| Health sync | SDK/provider, least metric permissions, paging, per-metric tokens, token expiry, partial caches and Samsung direct steps are present. Background worker has feature/permission gating and cancellation propagation. Inspected read-sync-save and multi-caller paths. | Provider/device permission scenarios and Samsung SDK parity require safe runtime evidence. |
| Background/lifecycle | WorkManager Health Connect, reminders, versioned workout debrief; persisted sleep alarms/countdown with exact/inexact recovery. Inspected workers, schedulers, receiver, sleep rules and data-clear orchestration. | No physical alarm/OEM execution or accessibility claim. |
| AI | Structured response schemas, explicit Gemini 2.5 Flash, OpenAI pinned discovery policy, image bounds, per-feature timeout/throttle, cross-provider fallback setting, deterministic fallback, safe error mapping. Inspected router/retry, provider transport and debrief generation safeguards. | No paid/live calls or accuracy/model-access claim. |
| Security/privacy | AES/GCM Android Keystore BYOK; fail-closed migration; no request-body HTTP logging; disabled-by-default telemetry; backup/device-transfer exclusions; manifest exported activity boundaries. Inspected key stores, gate, backup rules and telemetry privacy boundary. | This is a focused code audit, not exhaustive threat modeling or certification. |
| Performance/code health | Critical mutation methods use targeted Room writes; full app observation still eagerly maps all tables into snapshots. Delayed exercise seeding keeps initial rendering independent. Macrobenchmark/profile infrastructure is present. | No new timing/jank claim. Eager mapping is architectural risk, not a measured regression finding. |

## Executable findings

### TECH-01 — Concurrent Health Connect sync can publish older cache/token state

- priority: P2
- area: data
- status: open
- evidence: `HealthConnectDataSource.kt:114` starts `getStatus` without serialization; `:213` reads persisted token/cache, external sync suspends, then `:169-184` rereads and replaces the granted metrics with the earlier result. Home (`HomeScreen.kt:172`), Settings (`SettingsSection.kt:408`) and background worker (`HealthConnectBackgroundSyncWorker.kt:69-75`) can run against the same singleton. Per-ViewModel refresh gates do not coordinate these callers.
- trigger: A reads token T0 and is slow; B reads T0, observes newer changes and saves T2/C2; A finishes later and replaces those same metrics with T1/C1. A later sync can recover by replay, but UI/cache freshness regresses and duplicate provider work occurs. An in-flight sync can also save after `ClearAppDataUseCase` clears private preferences (`UseCases.kt:543`).
- target: one serialized read-sync-save operation per data source; clear/reset must invalidate or serialize in-flight persistence so deletion cannot be repopulated by an older request.
- fix: introduce a bounded shared sync coordinator/mutex spanning the complete operation, with a reset generation or coordinated clear operation; preserve cancellation while waiting/acquired and keep unrelated metrics intact. Do not merely lock the final DataStore edit.
- criteria/tests: controlled two-call interleaving cannot commit older state after newer state; canceled waiter never invokes provider; exception/cancellation releases lock; clear during suspended sync blocks old persistence. Existing metric-token/partial-cache tests remain valid. Cheapest proof is a deterministic extracted coordinator component plus existing Health Connect contracts; device provider validation is a later affected gate.
- risk: medium; lock must not introduce deadlock through clear or nested sync, or hold UI thread. Owner: data/platform implementation stream.

### TECH-02 — Deleted meal/recipe edits silently recreate rows

- priority: P2
- area: data
- status: open
- evidence: coordinator `TrainIqRepository.kt:1118-1165` uses supplied ID for edit and 0 for creation. `RoomTrainIqRuntimeStore.kt:750-760` accepts every positive meal ID then DAO upserts it. `:796-817` likewise accepts positive recipe ID without checking current recipe. By contrast `:771-776` explicitly rejects nonexistent positive food IDs, matching repaired NEXT20-12. The meal/recipe case is not addressed by that food fix.
- trigger: retain an edit request, delete its row, submit the request. It creates a new parent and children using the deleted ID rather than rejecting the edit. Data clear/import can also invalidate retained editors.
- target/fix: distinguish creation from update at the transaction boundary; require the positive-ID parent to exist before allocating child IDs/writing. Return an actionable error and preserve unrelated data. Keep null/0 creates and valid edits working.
- criteria/tests: actual Room tests for create, valid edit, delete→stale edit failure with unchanged table counts/content and reopen proof; run through both meal save overloads and recipe save. Test valid timestamp/createdAt preservation. Lowest reliable layer: targeted Room instrumentation (`TargetedRoomPersistenceInstrumentedTest`).
- risk: low; no schema needed. Existence guards do not solve recycled-ID identity after an unrelated new row reuses the same ID; do not claim that broader guarantee. Owner: persistence stream.

### TECH-03 — Outer coroutine deadline is mislabeled as provider timeout

- priority: P2
- area: Android lifecycle
- status: open
- evidence: `AiSupport.kt:282` and `:348` catch every `TimeoutCancellationException` before general `CancellationException` and convert it into ordinary AI failure. They cannot currently distinguish their own timeout from a surrounding caller deadline. Router then classifies the failure and may update verification/diagnostic/fallback state (`AiProviders.kt:542-569`). Existing cancellation coverage throws generic cancellation, not an outer deadline.
- trigger: outer `withTimeout` shorter than provider budget cancels a suspended provider block. Wrapper translates caller cancellation to AI timeout, permitting ordinary failure/fallback handling inside the canceled call chain.
- target/fix: caller cancellation/deadline propagates; only owned provider timeout maps to user-safe AI failure. Check current coroutine activity before timeout conversion, or use an owned timeout-result boundary.
- criteria/tests: deterministic outer-timeout cases for both wrappers preserve `TimeoutCancellationException` and do not record provider throttle/failure or invoke fallback; owned timeout remains mapped; generic cancellation still propagates; success/rate-limit behavior unchanged. Cheapest proof: coroutine JVM tests in existing AI service/retry suite.
- risk: low. Owner: AI stream.

### TECH-04 — API-key deletion reports success despite failed durable commit

- priority: P2
- area: security
- status: open
- evidence: `AndroidKeystoreGeminiKeyStore.kt:46-51` and `AndroidKeystoreOpenAiKeyStore.kt:46-49` discard `SharedPreferences.Editor.commit()` Boolean; `clearKey(): Unit` then completes normally. Save already checks commit success and readback. Settings/clear-all can report completion although failed disk persistence permits key recovery after process restart.
- target/fix: fail closed on failed encrypted-key deletion; surface storage error through the existing reversible user action path instead of claiming deletion. Do not delete Keystore aliases or rotate keys as unrelated cleanup.
- criteria/tests: inject or extract a narrow durable-clear operation; false commit fails, true succeeds, exception remains failure. Settings success must occur only on durable success. No credentials/live API required.
- risk: low/medium; clear-all is multi-store and not atomic, so accurately describe partial deletion when failure occurs. Owner: security/data stream.

## Rejected and historical candidates

- FatSecret v2 barcode endpoint correctly returns detailed nutrition, rather than only food ID. Checked owning-vendor documentation on 2026-09-30: [Foods: Find Id For Barcode v2](https://platform.fatsecret.com/docs/v2/food.find_id_for_barcode). No endpoint-change finding.
- Historical full-import stale-row problem is repaired: `RoomJsonImportSink` now always calls `clearMirrorTables` in its transaction.
- Food-in-recipe deletion is protected by Room RESTRICT; stale observation can affect error wording but does not silently cascade ingredients. No P1 data-loss finding.
- Versioned workout debrief checks protect recycled session IDs and cancellation precedes deletion; no duplicate of repaired debrief finding.
- Eager broad snapshot mapping remains a scaling consideration. No new benchmark evidence supports assigning a new performance regression priority.
- No new gateway/authentication/owner-approval requirement for current optional BYOK or local gateway; deployment remains outside this task.

## Wave-1 verification

- Repository/code/doc inspection: PASS (file/line evidence and current history checked).
- App source edits: none in this wave.
- Gradle/build/unit/lint/instrumentation/migration/performance: NOT RUN, parent wave-1 restriction.
- Device/Health Connect/live AI/gateway requests: NOT RUN, read-only wave and no authorized live credentials/cost.
- Sources: repository primary evidence; one official FatSecret documentation check rejected an uncertain candidate.
- Scope completion: technical inventory complete at static audit depth. Runtime, migrations, AI accuracy and physical performance remain explicitly unverified. No percentage alignment estimate is invented from static inspection.
