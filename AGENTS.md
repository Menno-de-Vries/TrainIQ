# TrainIQ Agent Contract

TrainIQ turns passive health data into proactive coaching: `Health Connect -> Gemini 2.5 Flash -> Material 3 -> Personal action`. Build toward an invisible coach with multi-metric sync, recovery/training insights, multimodal meal/label/posture scanning, and Gemini Nano where local feedback is suitable. Optimize for long-term code health.

## Priorities and evidence

Platform instructions remain authoritative. Within this repository use: `Safety and authority -> explicit user intent -> architecture/product truth -> UX quality -> verification -> speed`.

Use evidence in this order: `repository code/config -> TrainIQ ADR or targeted guide -> current official primary source`. Keep stable rules here; verify volatile SDK, library, model, policy, and tool behavior against pinned versions and owning-vendor documentation. Browse only when requested or when current guidance can materially change the solution; record sources for lasting decisions and stop when evidence is sufficient. Preserve pinned behavior when guidance differs unless changing it is in scope.

The app is under `TrainIQ-Project/`. Before narrow work, read relevant sections of `TrainIQ_Target_State_Blueprint.md`, `docs/TrainIQ_Architecture_Decisions.md`, and affected QA/security/release guides. For artifacts, signing, Play, privacy, or release, include `TrainIQ-Project/docs/play-privacy-release-evidence.md` and `TrainIQ-Project/docs/release/final-release-risk-register.md`; open owner gates remain blockers. Read governing documents fully for broad UI, cross-feature, migration, security, AI-boundary, privacy, or release work. Do not reread unchanged context in one task.

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

## Devices and scalable tests

Put each test at the lowest layer with enough fidelity: pure rules/mappers/use cases in unit tests; ViewModels/reducers/flows in deterministic state tests; Room/migrations/transactions in contract/integration tests; Android/Health Connect/permissions/lifecycle/navigation in targeted instrumented tests; only critical journeys in UI/release smoke tests; performance in macrobenchmarks.

Every behavior change proves its happy path, relevant boundary, and failure/recovery behavior. Use existing JUnit/Turbine patterns plus reusable fixtures/builders, controlled clocks/dispatchers, and deterministic fakes; use MockK only where justified. Avoid sleeps, shared mutable state, duplicated cross-layer assertions, implementation coupling, and oversized end-to-end suites. Never weaken, skip, or delete a valid test to pass a gate; diagnose flakes at their owning layer.

For repeatable instrumented automation prefer a project-configured Gradle Managed Device; do not add that configuration during unrelated work. For interactive QA discover SDK tools from `local.properties`, `ANDROID_SDK_ROOT`/`ANDROID_HOME`, then standard OS paths; Android Studio is a fallback, not a runtime dependency.

- List devices/AVDs, select an isolated compatible target, wait for `sys.boot_completed`, and scope every `adb` call to its serial. Never commandeer, reset, wipe, reconfigure, or change permissions on an unknown physical device or user AVD.
- If a target is occupied/uncertain, start another compatible AVD; if none exists, autonomous mode may create one uniquely named agent-owned AVD only from an installed image with sufficient capacity. Never silently download SDK assets, accept licenses, wipe/delete an AVD, or expand the device matrix.
- Capture appropriate screenshot, UI-tree, lifecycle, and crash-log evidence. Run at most one agent-created emulator unless acceptance criteria require a matrix; stop only emulators the agent started and report any created AVD.
- For Health Connect evidence, use `scripts/collect-health-connect-runtime-evidence.ps1` only on an approved safe profile/device.

## Commands and risk-based gates

Run from `TrainIQ-Project/`: `./gradlew` on Unix or `gradlew.bat` on Windows.

```text
:app:assembleDebug | :app:testDebugUnitTest | :app:lintDebug
:app:connectedDebugAndroidTest
:app:assembleProfileable | :macrobenchmark:assembleAndroidTest | :macrobenchmark:connectedProfileableAndroidTest
:app:checkReleaseSigningReadiness
```

During work run the smallest affected check. Before a local commit run focused tests and `git diff --check`. Before an authorized push/PR run debug assemble, unit tests, lint, and every affected device/migration/security/performance gate. Use physical hardware for trustworthy performance numbers. Release/shared-migration/tooling changes require all applicable connected, profileable, signing, privacy, and release gates. Documentation-only work uses scoped content/path/link/diff checks.

## Git, safety, and completion

- Start clean-`main` write work on `codex/<slug>`; continue an existing task branch. With unrelated changes use an isolated worktree without moving them; stop if changes overlap.
- Add dependencies or change services, permissions, config, schemas, or formats only after understanding purpose, migration, impact, and safer alternatives. Never expose secrets, health data, photos, telemetry, credentials, or signing material.
- Stage exact task paths—never `git add -A`—review scoped staged diff and risk, then create complete revertible Conventional Commits (`feat|fix|test|docs|refactor|perf|build|chore|style|revert`).
- Never force-push, rewrite shared history, bypass protections, destructively clean, or delete data, branches, tags, releases, AVDs, or unmerged work without separate exact authorization and verified targets.
- Fail fast, fix in-scope root causes, and report unrelated failures without changing them. Do not repeat unchanged checks, add CI, or introduce unrelated cleanup.
- Report decisions, assumptions, exact verification and results, residual risks, blockers, deviations, and changed paths. PR evidence also covers purpose, scope, linked issues, and applicable migration/privacy/security/AI/tooling/artifact/device/visual evidence. Stop when acceptance criteria and required gates pass.

`Long-term code health > short-term speed.`
