# TrainIQ Target-State Backlog

Updated date: 2026-05-10

## Now

### VNEXT-001

- item_id: VNEXT-001
- title: Guided first-run onboarding
- type: feature
- phase: Now
- priority: P1
- user value: Users can set goals, consent choices, training context, AI mode, and reminder preferences before the app starts giving advice.
- dev notes: Model onboarding state separately from profile fields so skipped steps can be resumed from Settings.
- affected modules/files: `features/home`, `features/settings`, `domain/model/DomainModels.kt`, `domain/usecase/UseCases.kt`, `data/repository/TrainIqRepository.kt`
- acceptance criteria:
  - User can finish onboarding with Health Connect disabled and AI disabled.
  - Goal, experience, schedule, equipment, consent, AI mode, and reminder preferences are persisted.
  - Skipped steps appear as actionable Settings/Home items.
  - Rotation/app switch does not lose entered form state.
- verification: Unit tests for onboarding state reducer and persistence; compact UI screenshots at 360x640/360x800 font scale 1.3/1.5; TalkBack pass.
- risks: Scope creep; privacy copy must match final AI/telemetry mode.
- dependencies: AI boundary decision for production copy; Health Connect consent copy.
- decision needed: yes

### VNEXT-002

- item_id: VNEXT-002
- title: Home next-best-action contract
- type: feature
- phase: Now
- priority: P1
- user value: The user sees one clear action and understands why it matters.
- dev notes: Add a deterministic local action selector before any AI summary. AI may rewrite or enrich only after local action exists.
- affected modules/files: `features/home/HomeScreen.kt`, `domain/usecase/UseCases.kt`, `domain/model/DomainModels.kt`
- acceptance criteria:
  - Home exposes one primary action, reason, data-quality label, and fallback action.
  - Missing/stale/denied Health Connect metrics are explicitly labeled.
  - Startup first draw does not wait on AI or Health Connect full sync.
- verification: Unit tests for action selection; emulator launch smoke; no startup timeout; Home UI dump for missing/partial data states.
- risks: Bad prioritization can reduce trust.
- dependencies: Data-quality model and Health Connect status.
- decision needed: no

### VNEXT-003

- item_id: VNEXT-003
- title: Finish targeted Room writes for hot paths
- type: data
- phase: Now
- priority: P0
- user value: User data does not resurrect, disappear, or slow down as history grows.
- dev notes: Continue one flow at a time; keep JSON import for legacy/import/export only.
- affected modules/files: `core/database/TrainIqDao.kt`, `data/repository/RoomTrainIqRuntimeStore.kt`, `data/repository/TrainIqRepository.kt`, `app/src/test/java/com/trainiq/architecture`
- acceptance criteria:
  - Active workout finish/edit/undo/session delete, routine edit/delete, generated-routine save, workout day/exercise add/remove, routine-level exercise add, meal save/delete, recipe save/delete, measurement add/delete, profile writes, and startup exercise-library seeding avoid full-state JSON mirror import.
  - Each migrated path has process-restart correctness coverage.
  - Delete/discard flows cannot resurrect stale rows.
- verification: Architecture guard tests; repository/process-restart tests; `:app:test`; connected migration tests where needed.
- current evidence: normal app-source callers of `runtimeStore.update(...)` have been removed as of 2026-05-11, and the public `RoomTrainIqRuntimeStore.update(transform)` API has also been removed. Focused architecture/seeder/transaction tests pass for the final exercise-library seeding migration and runtime API removal; earlier targeted restart coverage covers the main user mutation paths. The remaining full-state import surface is explicit migration/readiness infrastructure plus private one-time legacy seed support.
- risks: High regression risk if changed broadly.
- dependencies: Existing Room schema and migration test harness.
- decision needed: no

### VNEXT-004

- item_id: VNEXT-004
- title: Health Connect runtime evidence matrix
- type: testing
- phase: Now
- priority: P1
- user value: Users understand and control health-data access across real permission states.
- dev notes: Use official Health Connect guidance for transparency, clarity, and manage-access paths.
- affected modules/files: `data/datasource/HealthConnectDataSource.kt`, `core/health`, `features/home`, `features/settings`, QA docs
- acceptance criteria:
  - Provider missing/update, no permission, partial permission, revoke while open, and background-read unavailable/granted states are tested.
  - Partial permissions sync granted metrics without clearing unrelated caches.
  - Settings exposes direct manage-access path.
- verification: Device/emulator script with UI dumps and logcat; unit tests for permission-state mapping.
- current evidence: 2026-05-10 partial SM-S931B evidence confirms launchable Settings/Health Connect status copy, app-owned rationale screen rendering, Health Connect controller package visibility, and empty crash buffers; focused `*HealthConnect*` unit tests and manifest/build/lint checks pass. 2026-05-11 added a repeatable non-mutating collector at `TrainIQ-Project/scripts/collect-health-connect-runtime-evidence.ps1`; latest output at `TrainIQ-Project/.codex/device-qa/2026-05-11-health-connect-scripted-baseline-debug-v4/` confirms cold main launch, rationale launch, system manage-access launch, all requested health permissions ungranted, and an empty crash slice. Provider-missing, partial-grant, revoke-while-open, and background-read runtime states still need safe-profile evidence.
- risks: Provider availability varies by device/API level.
- dependencies: Emulator/device with Health Connect support.
- decision needed: no

### VNEXT-005

- item_id: VNEXT-005
- title: Release owner gates
- type: release
- phase: Now
- priority: P0
- user value: Release claims match implementation, privacy posture, and Play requirements.
- dev notes: Keep release blocked until owners close Data Safety, privacy policy, AI boundary, background Health Connect, accessibility, performance, signing, and versioning.
- affected modules/files: `TrainIQ-Project/docs/release`, `TrainIQ-Project/docs/architecture`, `.github/workflows/android.yml`
- acceptance criteria:
  - Owner-action tracker has dated decisions or explicit exceptions.
  - Data Safety/privacy docs match final AI, telemetry, Health Connect, export, and logging behavior.
  - Versioning strategy is approved before upload.
- verification: Release-doc review; `:app:checkReleaseSigningReadiness`; signed-release workflow dry run where possible.
- risks: High compliance risk if docs diverge from implementation.
- dependencies: Product/legal/security/release owner decisions.
- decision needed: yes

## Next

### VNEXT-006

- item_id: VNEXT-006
- title: Recovery/readiness model
- type: feature
- phase: Next
- priority: P1
- user value: Users know when to train hard, adjust, or recover without overclaiming medical certainty.
- dev notes: Start deterministic and conservative; AI can summarize but not be the only source of advice.
- affected modules/files: `domain/model/DomainModels.kt`, `domain/usecase/UseCases.kt`, `features/coach`, `features/home`, `features/progress`
- acceptance criteria:
  - Readiness output includes recommendation, confidence/data quality, inputs used, missing inputs, and fallback action.
  - Stale or denied metrics are not treated as zero.
  - Copy avoids diagnosis, treatment, or clinical claims.
- verification: Unit tests for data-quality combinations; AI fallback tests; UX review for safety copy.
- risks: Medical overclaiming if wording is too assertive.
- dependencies: Product/legal review of coaching boundaries.
- decision needed: yes

### VNEXT-007

- item_id: VNEXT-007
- title: Goal and adherence model
- type: backend
- phase: Next
- priority: P1
- user value: Coaching adapts to what the user is trying to achieve and whether they are following the plan.
- dev notes: Store goal lifecycle and adherence events locally; keep recommendations explainable.
- affected modules/files: `domain/model/DomainModels.kt`, `core/database/Entities.kt`, `data/mapper/Mappers.kt`, `features/home`, `features/coach`, `features/progress`
- acceptance criteria:
  - Goals have status, start date, target horizon, selected metrics, and review cadence.
  - Missed/complete plan events are recorded without requiring AI.
  - Home and Coach can explain goal progress using local data.
- verification: Mapper/entity/usecase tests; process-restart persistence tests.
- risks: Too many goal types can dilute the product.
- dependencies: Onboarding model.
- decision needed: yes

### VNEXT-008

- item_id: VNEXT-008
- title: Opt-in reminders
- type: feature
- phase: Next
- priority: P2
- user value: Users get timely nudges for training, weekly reports, meal logging, progress check-ins, recovery, and Health Connect issues.
- dev notes: Quiet defaults; explicit opt-in; respect battery and notification policy.
- affected modules/files: `core/health`, `features/settings`, `domain/model/DomainModels.kt`, WorkManager/notifications
- acceptance criteria:
  - Each reminder type can be enabled/disabled independently.
  - No reminder uploads health data.
  - Reminders degrade gracefully if notification permission is denied.
- verification: Unit tests for schedule policy; notification permission manual QA; battery/background smoke.
- risks: Annoying reminders hurt trust.
- dependencies: Product decision on reminder set and copy.
- decision needed: yes

### VNEXT-009

- item_id: VNEXT-009
- title: Progress narrative and export
- type: feature
- phase: Next
- priority: P2
- user value: Users can understand long-term trends and take their data with them.
- dev notes: Keep export local and explicit; do not include secrets.
- affected modules/files: `features/progress`, `domain/model/DomainModels.kt`, `data/repository`, export utilities
- acceptance criteria:
  - Weekly/monthly comparison includes trend, data quality, and plain-language summary.
  - Export scope is previewed before generation.
  - Export excludes API keys, telemetry tokens, and internal diagnostics unless explicitly selected.
- verification: Unit tests for export content; file inspection; TalkBack and contrast check for progress summaries.
- risks: Privacy risk if export includes sensitive data unexpectedly.
- dependencies: Export scope decision.
- decision needed: yes

## Later

### VNEXT-010

- item_id: VNEXT-010
- title: Barcode product lookup
- type: feature
- phase: Later
- priority: P3
- user value: Users can add packaged food faster when lookup data is available.
- dev notes: Current barcode path can remain manual capture until a data-source policy is chosen.
- affected modules/files: `features/nutrition`, `data/repository`, remote/data-source layer
- acceptance criteria:
  - Lookup has offline, not-found, ambiguous result, and manual fallback states.
  - Product data source and freshness are shown.
  - User can correct imported nutrition values.
- verification: Fake data-source tests; scanner UI smoke; privacy review.
- risks: Data quality and licensing risk.
- dependencies: Product/data-source decision.
- decision needed: yes

### VNEXT-011

- item_id: VNEXT-011
- title: Production AI gateway or final BYOK policy
- type: security
- phase: Later
- priority: P1
- user value: AI features are trustworthy, cost-controlled, and privacy-clear.
- dev notes: If gateway is chosen, add backend auth, quota, abuse controls, logging policy, and Data Safety update. If BYOK remains, document it as the supported production mode.
- affected modules/files: `data/remote/GeminiApi.kt`, `ai/services`, `docs/architecture`, `docs/release`
- acceptance criteria:
  - One AI mode is signed off for release.
  - API keys are never sent in URLs or logs.
  - User-facing copy explains third-party AI data flow.
- verification: Security review; logcat redaction check; release-doc review.
- risks: High privacy/billing/security impact.
- dependencies: Product/security/legal decision.
- decision needed: yes

### VNEXT-012

- item_id: VNEXT-012
- title: Wear OS or companion surfaces
- type: feature
- phase: Later
- priority: P3
- user value: Workout/rest-timer interactions may become faster during training.
- dev notes: Do not start until core Android app release gates are closed.
- affected modules/files: New module if approved.
- acceptance criteria:
  - Companion scope is limited to high-frequency training actions.
  - Phone app remains fully functional without companion.
  - Privacy and battery impact are reviewed.
- verification: Separate platform QA plan.
- risks: Maintenance cost.
- dependencies: Product validation.
- decision needed: yes
