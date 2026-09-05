# TrainIQ Agent Contract

TrainIQ turns passive health data into proactive coaching: `Health Connect -> Gemini 2.5 Flash -> Material 3 -> Personal action`. Build toward an invisible coach with multi-metric sync, recovery/training insights, multimodal meal/label/posture scanning, and Gemini Nano where local feedback is suitable. Optimize for long-term code health.

## Priorities and evidence

Platform instructions remain authoritative. Within this repository use: `Safety and authority -> explicit user intent -> architecture/product truth -> UX quality -> verification -> speed`.

Use evidence in this order: `repository code/config -> TrainIQ ADR or targeted guide -> current official primary source`. Keep stable rules here; verify volatile SDK, library, model, policy, and tool behavior against pinned versions and owning-vendor documentation. Browse only when requested or when current guidance can materially change the solution; record sources for lasting decisions and stop when evidence is sufficient. Preserve pinned behavior when guidance differs unless changing it is in scope.

The app is under `TrainIQ-Project/`. Before narrow work, read relevant sections of `TrainIQ_Target_State_Blueprint.md`, `docs/TrainIQ_Architecture_Decisions.md`, and affected guides. Read `docs/agent-guides/local-testing.md` fully before behavior, test, UI/UX, platform, persistence, remote-boundary, performance, verification-tooling, PR-evidence, or release work. For artifacts, signing, Play, privacy, or release, include `TrainIQ-Project/docs/play-privacy-release-evidence.md` and `TrainIQ-Project/docs/release/final-release-risk-register.md`; the itch.io scope in `TrainIQ-Project/docs/release/itch-release-policy.md` governs release checks. LEGAL-001, PERF-001, A11Y-001, and AI-001 are retired; never require those owner approvals or recurring exemptions for itch.io delivery. Read governing documents fully for broad/cross-feature, migration, security, AI-boundary, privacy, or release work. Do not reread unchanged context in one task.

## Autonomous execution

Default loop: `Inspect -> Decide -> Implement -> Verify -> Commit -> Report`.

- Inspect status, branch, history, affected architecture, tests, and existing patterns before writing. Define acceptance criteria and risk level; decide material choices once from repository evidence.
- Implement the smallest complete reversible change. Own in-scope diagnosis and fixes; retry a failed check only after new evidence or a changed hypothesis, implementation, or test condition.
- Ask only for unavailable authority or unresolved material risk involving data loss, privacy/security, cost, downtime, stability, or genuinely ambiguous acceptance criteria. Do not pause for safe details the repository answers.
- When the user says `autonoom`, own the scoped outcome end to end: research, planning, implementation, scalable tests, bounded recovery, focused local commits, and evidence-backed completion.
- Autonomous mode never grants permission to push, open/merge a PR, release, deploy, sign, change secrets, grant/revoke permissions, mutate remote data, or perform destructive actions; each requires an explicit user request naming it.
- Preserve user work and scope. Never stash, discard, overwrite, rename, or include unrelated changes. Delegate only when requested and the work is independent.

## Architecture and routing

Use Kotlin, Compose, Hilt, Room, Health Connect, CameraX, and Gemini with `MVVM + Clean Architecture + UDF` (`Data -> Domain -> UI`).

- Put feature UI/ViewModels in `app/src/main/java/com/trainiq/features/`, rules/use cases in `domain/`, persistence/external access in `data/`, shared app code in `core/`, and routes in `navigation/`.
- UI reads ViewModel state only; each screen exposes one `uiState: StateFlow<T>` as sealed `Loading`, `Success`, or `Error`. Keep mapping and business logic out of composables.
- Use Hilt: repositories `@Singleton`, ViewModel-owned dependencies `@ViewModelScoped`. Use Navigation 2.8+ typed `kotlinx.serialization` routes; never string routes.
- Mirror packages in `app/src/test/` or `app/src/androidTest/`; keep Room schemas in `app/schemas/` and performance code in `macrobenchmark/`.
- Never edit generated `**/build/`, `.gradle/`, `TrainIQ-Project/dist/`, IDE output, or generated source/resource content.

## UI/UX definition of done

- Follow the blueprint and reuse Material 3/design-system primitives. Use `MaterialTheme.colorScheme`, `MaterialTheme.typography`, Dynamic Color on Android 12+, concise actionable copy, calm hierarchy, and progressive disclosure.
- Model every relevant loading/shimmer, empty, success, partial-data, offline, permission-denied, recoverable-error, and retry state. Preserve state through navigation, rotation, resize, background/foreground, lock/unlock, and process recreation where required.
- Target Adaptive Optimized behavior for affected critical flows: compact phones, tablets/foldables, resizable/multi-window layouts, functional parity, dark mode, font scale, keyboard/external input, and no clipped or unreachable actions.
- Provide semantics, logical focus/traversal, sufficient contrast and touch targets, meaningful labels, and reduced-motion-safe behavior. Screenshots never replace semantics/UI-tree or TalkBack/Switch Access evidence when relevant.
- Use previews/fixtures for meaningful states and selective screenshot tests when deterministic infrastructure exists; cover only combinations with unique feedback and never silently update references.
- Prefer subtle `AnimatedContent`, purposeful haptics, shimmer over generic spinners, and shared transitions for Home -> Active Workout, Workout List -> Detail, and Meal Scan -> Result when they improve continuity.
- Keep Compose inputs stable, state ownership explicit, recomposition/animation bounded, and critical journeys represented in Baseline Profiles. Visually inspect every affected screen at the smallest representative device matrix.

## Health, AI, and data invariants

- Health Connect: always check `HealthConnectClient.getSdkStatus()`, handle provider missing/update, explain value before the system prompt, request least privilege, and support denial, repeated cancellation, partial/revoked access, background-read availability, paging, and Manage Access. Use a `ChangesToken` per record type and fetch all pages of inserts, updates, and deletions since successful sync for steps, heart rate, sleep, active calories, and workout sessions.
- Gemini: default to `gemini-2.5-flash` with the Senior Strength Coach persona and no unsupported medical claims. Require schema-backed JSON using `responseMimeType = "application/json"`; never regex free text into JSON or expose chain-of-thought. Use `thinkingBudget = 0` for fast scan/classification and `500-1000` for coaching, recovery, reports, debriefs, and recommendations.
- Keep AI opt-in and context-minimal with bounded timeout/cancellation/retry, safe errors, and deterministic local fallback where practical. Never hardcode, log, or commit keys; retain Keystore-backed storage and fail closed on migration errors.
- Room is authoritative for app-owned data; legacy JSON is import/export/backup only. Before persisted fields inspect `Entities.kt`, `DomainModels.kt`, `Mappers.kt`, DAOs, repositories, use cases, schemas, and navigation. Prefer `AutoMigration`; otherwise use verified SQL migrations. Preserve transactions, idempotent imports, and rollback on invalid data.

## Local-only scalable quality

- Track test source, deterministic fixtures, schemas, and essential reproducibility assets; run every build/test/evidence gate locally. Never add or use GitHub Actions, hosted runners, cloud test/device services, or remote build caches without a separate exact request. GitHub status is not test evidence; generated reports, binaries, logs, captures, traces, emulator data, and caches stay untracked.
- Select tests from changed surfaces plus transitive risk, always at the lowest reliable layer: domain/mappers/use cases -> unit; ViewModel/reducer/Flow -> deterministic state/component; Compose -> state/semantics/selective visual; navigation/lifecycle/permissions -> targeted instrumented; Room/repository/migration -> contract/transaction/migration; Health Connect/CameraX -> fakes plus safe device; Gemini/remote/backend -> schema/timeout/error/fallback/privacy; cross-cutting/release/performance -> applicable full local matrix.
- Every behavior proves happy path, material boundary, and failure/recovery without equivalent cross-layer assertions. Add broader tests only when cheaper layers cannot prove the risk; update/remove obsolete tests and fixtures with removed behavior. Never weaken, skip, or delete a valid test to pass a gate; treat flakes as defects.
- Widen once per invalidated scope: `edit -> focused proof | pre-commit -> affected layers | authorized PR -> local baseline + affected gates | release -> applicable full matrix + owner/device evidence`. Reuse passing evidence until relevant code/config/dependency/fixture/schema/environment inputs change; do not rerun unchanged gates or use routine `clean`.
- In `autonoom` mode classify surfaces/risk, add maintainable tests, run all required gates locally, diagnose failures, provision at most one safe emulator, and record exact evidence without asking choices this contract/guide answers.

## Devices and canonical local gates

- Use the configured `:app:connectedDebugAndroidTest` route for instrumentation. Discover SDK tools from `local.properties`, `ANDROID_SDK_ROOT`/`ANDROID_HOME`, then standard paths; list targets, select one compatible isolated serial, wait for `sys.boot_completed`, and scope every `adb` call.
- Never commandeer, reset, wipe, reconfigure, or change permissions on an unknown physical/user device. If occupied/uncertain, start another installed compatible AVD; create one uniquely named agent-owned AVD only when necessary and within capacity. Never silently download/accept licenses, delete AVD data, or expand the matrix. Stop only emulators the agent started and report created AVDs.
- Capture only required screenshot/UI-tree/lifecycle/crash evidence. Use `scripts/collect-health-connect-runtime-evidence.ps1` only on an approved safe profile/device; use physical hardware for performance claims.
- Canonical local gates from `TrainIQ-Project/`: `:app:assembleDebug`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:connectedDebugAndroidTest`, `:app:generateDebugRoomMigrationChainVerificationMarker`, `:app:assembleProfileable`, `:macrobenchmark:assembleAndroidTest`, `:macrobenchmark:connectedProfileableAndroidTest`, `:app:checkReleaseSigningReadiness`. Exact commands and evidence rules live in the testing guide.

## Git, safety, and completion

- Start clean-`main` write work on `codex/<slug>`; continue an existing task branch. With unrelated changes use an isolated worktree without moving them; stop if changes overlap.
- Add dependencies or change services, permissions, config, schemas, or formats only after understanding purpose, migration, impact, and safer alternatives. Never expose secrets, health data, photos, telemetry, credentials, or signing material.
- Stage exact task paths; never `git add -A`. Review scoped staged diff and risk, then create complete revertible Conventional Commits (`feat|fix|test|docs|refactor|perf|build|chore|style|revert`).
- Never force-push, rewrite shared history, bypass protections, destructively clean, or delete data, branches, tags, releases, AVDs, or unmerged work without separate exact authorization and verified targets.
- Fail fast, fix in-scope root causes, and report unrelated failures without changing them. Do not repeat unchanged checks, add CI, or introduce unrelated cleanup.
- Report decisions, assumptions, exact verification and results, residual risks, blockers, deviations, and changed paths. PR evidence also covers purpose, scope, linked issues, and applicable migration/privacy/security/AI/tooling/artifact/device/visual evidence. Stop when acceptance criteria and required gates pass.

`Long-term code health > short-term speed.`
