# TrainIQ Agent Contract

This file is the leading engineering contract for TrainIQ. Optimize for long-term code health: scalable, maintainable, performant, privacy-conscious, and AI-native.

## Product and roadmap

TrainIQ turns passive health data into proactive, personal coaching:

```text
Health Connect -> Gemini 2.5 Flash -> Material 3 UI -> Personal action
```

- Foundation: MVVM with Hilt, Room entities/repositories, and a Health Connect data source.
- Modernization and precision: type-safe navigation, adaptive Material 3, incremental Health Connect sync, multi-metric support, Gemini 2.5 Flash, and bounded thinking budgets.
- Invisible coach: proactive sleep/recovery/training insights, multimodal meal/label/posture scanning, and Gemini Nano for suitable local feedback.

## Scope and routing

The Android project lives in `TrainIQ-Project/` and uses Kotlin, Jetpack Compose, Hilt, Room, Health Connect, CameraX, and Gemini. Preserve `MVVM + Clean Architecture + Unidirectional Data Flow` (`Data -> Domain -> UI`).

- Put screens and feature ViewModels in `app/src/main/java/com/trainiq/features/`, business rules and use cases in `domain/`, persistence and external data access in `data/`, shared app code in `core/`, and routes in `navigation/`.
- UI reads ViewModel state only. Each screen exposes one `uiState: StateFlow<T>` modeled as a sealed `Loading`, `Success`, or `Error` state. Keep mapping and business logic out of composables.
- Use Hilt; repositories are `@Singleton` and ViewModel-owned dependencies are `@ViewModelScoped`.
- Use Navigation 2.8+ type-safe `kotlinx.serialization` routes; never add string-based routes.
- Mirror production packages under `app/src/test/` or `app/src/androidTest/`; keep Room schemas in `app/schemas/` and performance tests in `macrobenchmark/`.
- Never edit generated `**/build/`, `.gradle/`, `TrainIQ-Project/dist/`, IDE output, or generated source/resource content.

Before changing visible UI, product/health/AI copy, interaction, accessibility, responsive behavior, or navigation, read `TrainIQ_Target_State_Blueprint.md` and `docs/TrainIQ_Architecture_Decisions.md` fully; they govern product truth, states, layouts, inputs, and visual verification.

Before changing Room, migrations, Health Connect, Gemini transport or keys, telemetry, sensitive data, permissions, manifests, or security boundaries, read `docs/TrainIQ_Architecture_Decisions.md` plus the relevant documents under `TrainIQ-Project/docs/security/` fully. Preserve Room as the app-owned source of truth and legacy JSON only as an import/export/backup bridge.

Before versioning, artifacts, APK/AAB, signing, Play Console, privacy declarations, or release work, read `TrainIQ-Project/docs/play-privacy-release-evidence.md`, `TrainIQ-Project/docs/release/final-release-risk-register.md`, and the relevant release checklist fully. Open owner gates remain blockers; never imply release readiness without their recorded evidence.

## Product implementation rules

### Material 3 and adaptive UX

- Use `MaterialTheme.colorScheme` and `MaterialTheme.typography`, with Dynamic Color on Android 12+.
- Prefer shimmer loading states, subtle `AnimatedContent` transitions, and haptics for important actions.
- Support compact phones, tablets, and foldables through window-size-aware layouts; verify dark mode, font scale, accessibility semantics, and touch targets.
- Use shared transitions where appropriate for Home -> Active Workout, Workout List -> Workout Detail, and Meal Scan -> Result.
- Avoid unnecessary Compose recompositions and maintain Baseline Profiles for critical journeys.

### Health Connect

- Always check `HealthConnectClient.getSdkStatus()` and handle provider-missing/update states safely.
- Explain value and requested signals in the app-owned rationale/permission manager before opening the system permission prompt.
- Request only necessary permissions and handle denial, partial grants, revocation, unavailable background reads, and paging.
- Sync incrementally with a `ChangesToken` per metric/record type and fetch only changes since the last successful sync.
- Supported core metrics are steps, heart rate, sleep, active calories, and workout sessions.

### Gemini

- Default to `gemini-2.5-flash` and preserve the Senior Strength Coach persona without unsupported medical claims.
- Require structured JSON using `responseMimeType = "application/json"` and a response schema; never recover JSON from free text with regex and never expose chain-of-thought.
- Fast scan/classification flows use `thinkingBudget = 0`; coach advice, weekly reports, recovery analysis, debriefs, and training recommendations use a bounded `500-1000` token budget.
- Keep AI opt-in, minimize sent context, use bounded timeouts/cancellation/retries, show safe errors, and provide deterministic local fallback where practical.
- Never hardcode, log, or commit API keys; retain Android Keystore-backed storage and fail closed on key migration errors.

### Room and tests

- Before adding or changing persisted fields, inspect `Entities.kt`, `DomainModels.kt`, `Mappers.kt`, DAOs, repositories, use cases, existing schemas, and navigation impact.
- Prefer `AutoMigration`; use explicit SQL migrations when required. Preserve transactions, idempotent import planning, and rollback on invalid data.
- Add or update tests that prove changed behavior, especially mappers, use cases, repository/transaction logic, migrations, permissions, and UI-state reducers. Prefer JUnit, Turbine, and MockK where they fit existing patterns.

## Commands

Run Gradle from `TrainIQ-Project/` (use `gradlew` on Unix):

```powershell
.\gradlew.bat :app:assembleDebug --console=plain
.\gradlew.bat :app:testDebugUnitTest --console=plain
.\gradlew.bat :app:lintDebug --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
```

- Use the smallest relevant task during development; the local baseline is debug assemble, unit tests, and lint.
- Device-dependent work uses `connectedDebugAndroidTest`; Health Connect evidence uses `scripts/collect-health-connect-runtime-evidence.ps1` only on an approved safe profile/device.
- Performance validation uses `:app:assembleProfileable`, `:macrobenchmark:assembleAndroidTest`, and `:macrobenchmark:connectedProfileableAndroidTest`; trust performance numbers only from a physical device.
- Release work also runs `:app:checkReleaseSigningReadiness` and the affected release/profileable gates. Never insert signing secrets into commands, source, `BuildConfig`, logs, or evidence.

## Autonomy and safety

Unless the user narrows the task, inspect first, make material choices once, implement the smallest reversible change, test it, and create focused local commits. Infer safe details from repository evidence; ask only for unavailable authority or unresolved choices involving data loss, privacy/security, cost, downtime, or stability. Browse only when current primary guidance can change the approach, stop when sufficient, delegate only when requested and independent, batch related work, and do not reread unchanged context.

Preserve user work and scope. Do not stash, discard, overwrite, rename, or include unrelated changes. Never expose secrets, personal health data, photos, telemetry, or credentials. Add dependencies or alter unfamiliar services, permissions, configuration, schemas, or data formats only after understanding purpose, impact, migration needs, and safer alternatives.

Pushes, pull requests, merges, releases, deployments, signing, secret changes, permission grants/revocations, and remote data mutations require an explicit user request naming that action. Never force-push, rewrite shared history, hard-reset, destructively clean, bypass protection, or delete data, branches, tags, releases, or unmerged work without separate exact authorization and verified targets.

## Git workflow

- Before writing, inspect status, branch, relevant history, and the architecture surfaces named above.
- Start clean-`main` write work on `codex/<slug>`; continue an existing task branch. With unrelated user changes, use an isolated worktree without moving them; if changes overlap, stop for direction.
- Keep changes small and consistent with existing naming. Do not duplicate entities, models, repositories, use cases, or routes.
- Stage exact task paths, never blind `git add -A`; review the scoped diff and run `git diff --check` before committing.
- Commit complete, revertible units with Conventional Commits: `feat`, `fix`, `test`, `docs`, `refactor`, `perf`, `build`, `chore`, `style`, or `revert`.
- Remove an agent-owned branch or worktree only after confirmed merge and clean state.

## Verification and completion

- During work, run the smallest relevant check. Before a local commit, run focused tests and `git diff --check`.
- Before an authorized push or PR, run debug assemble, unit tests, lint, and every affected device, migration, Health Connect, security, or performance gate.
- Release, shared persistence/migration, verification tooling, or explicitly requested full validation requires all applicable local, connected, profileable, signing-readiness, privacy, and release gates—not one synthetic command.
- Documentation-only work uses scoped content, path/link, and diff checks. Fail fast, fix in-scope root causes, report unrelated failures without changing them, and do not repeat unchanged checks or add CI workflows unless explicitly requested.
- PR evidence states what and why, scope, exact commands/results, risks, linked issues, and applicable migration, privacy, security, AI-context, tooling, artifact, device, and visual evidence.
- Report only material decisions, assumptions, blockers, deviations, and verification. Stop when acceptance criteria and required checks pass; do not add unrelated cleanup or polish.

```text
Long-term code health > short-term speed
```
