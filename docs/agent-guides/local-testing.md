# TrainIQ Local Testing Guide

> Release scope update (2026-09-06): [itch.io release policy](../../TrainIQ-Project/docs/release/itch-release-policy.md) supersedes the owner-approval and mandatory certification release gates below. LEGAL-001, PERF-001, A11Y-001, and AI-001 are retired for this personal itch.io project. Older BLOCKED/OPEN statements are historical or refer to optional certification/future Play submission, not current itch.io delivery. Preserve actual test results and technical findings; do not claim missing evidence passed.

Use this guide for any change to behavior, tests, UI/UX, navigation, Android boundaries, persistence, remote/AI boundaries, performance, verification tooling, PR evidence, or release readiness.

## Local-only contract

- Track test source, deterministic fixtures, Room schemas, and the minimum reproducibility assets needed to run tests.
- Run builds, tests, lint, instrumented flows, screenshots, accessibility checks, migration verification, and benchmarks only on the user's local machine or an explicitly approved local device.
- Do not add or use GitHub Actions, hosted runners, cloud test/device services, or remote build caches without a separate user request naming that remote capability.
- GitHub status is never test evidence. An authorized PR or merge reports the exact local commands, commit under test, environment/device where relevant, and `PASS`, `FAIL`, or `NOT RUN` with reason.
- Do not commit generated reports, APK/AAB files, logs, captured evidence screenshots, traces, emulator data, caches, or other test output. Track screenshot references only after deterministic screenshot infrastructure is explicitly adopted and each image supplies unique regression value.

## Selection algorithm

1. Identify changed files, behavior, and transitive consumers/boundaries.
2. List unique regression risks and assign each to the lowest reliable test layer.
3. Prove the happy path, material boundary case, and failure/recovery behavior.
4. Add a broader test only when a cheaper layer cannot prove a risk.
5. Run focused checks while editing, every affected layer before commit, the local baseline plus affected gates before an authorized PR, and the applicable full matrix for release.

Coverage grows with product behavior and boundary complexity—not line count, fixed percentages, or duplicated assertions. When behavior is removed, remove or update obsolete tests and fixtures in the same change.

## Change-impact matrix

| Changed surface | Cheapest required proof | Widen when |
|---|---|---|
| Docs or agent policy | Content, paths/links, scoped diff, `git diff --check` | Referenced commands/config or release truth changes |
| Domain rule, mapper, formatter, validation, use case | Focused JVM unit test, then affected module unit suite | Multiple repositories/features consume the contract |
| ViewModel, reducer, Flow, UI state | Deterministic state/Flow/component tests and affected unit suite | Lifecycle, navigation, restoration, or Android APIs participate |
| Compose layout, copy, accessibility, adaptive UX | State fixtures/previews, useful semantics assertions, selective visual/runtime inspection, compile and lint | Interaction, configuration change, focus, or platform behavior is at risk |
| Navigation, lifecycle, restoration, permission orchestration | Local logic/component tests plus targeted instrumented flow | Critical journey or multi-window/device behavior changes |
| Room entity, DAO, repository, transaction, import/export, migration | Mapper/unit tests, repository/transaction contracts, migration-chain or instrumented database proof | Schema/runtime compatibility or rollback behavior changes |
| Health Connect or CameraX boundary | Deterministic fakes/contracts plus targeted safe-profile device validation | Provider, permission, paging, lifecycle, or real hardware behavior changes |
| Gemini, `data/remote`, telemetry, future backend boundary | Request/response/schema contracts; timeout, cancellation, retry, privacy, error, and fallback tests | Transport, authentication, quota, persistence, or production boundary changes |
| Shared architecture, persistence, build/test tooling | All affected lower layers plus local baseline and applicable connected/security gates | Change crosses modules or alters how evidence is produced |
| Release or performance-critical journey | Release/profileable checks, critical-flow smoke evidence, macrobenchmark | Release scope or an approved performance threshold/device matrix requires it |

Live production mutation is never a test strategy. Use fakes, fixtures, local contracts, approved test profiles, or explicit non-production environments within granted authority.

## Test-layer quality

- Unit/component: pure, fast, isolated; use existing JUnit/Turbine patterns, reusable builders, deterministic fakes, controlled clocks, and controlled dispatchers.
- Repository/data: assert public contracts, transactions, idempotency, rollback, import/export compatibility, and migration edges rather than implementation calls.
- UI: separate state/behavior assertions from visual assertions. Keep previews representative and screenshot cases minimal; never generate every state × theme × font × window combination.
- Instrumented: reserve for Android framework, lifecycle, navigation, permissions, Room runtime, Health Connect, CameraX, or semantics behavior the host cannot prove.
- Smoke/release: keep a small stable set of critical user journeys. Do not turn every feature test into end-to-end coverage.
- Performance: measure repeatable critical journeys; emulator results may diagnose but never support real-world performance claims.

Avoid sleeps, live-network dependence, shared mutable test state, order dependence, cross-layer duplication, oversized fixtures, and assertions coupled to private implementation. Never weaken, skip, or delete a valid test merely to make a gate pass.

## Local commands

Run from `TrainIQ-Project/`; on Unix replace `gradlew.bat` with `./gradlew`.

Focused JVM test:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.package.TestClass.testName" --console=plain
```

Local baseline:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
```

Connected Android tests are the project's configured instrumentation route:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
```

Inspect the generated HTML report under `app/build/reports/androidTests/connected/debug/` and machine-readable results under `app/build/outputs/androidTest-results/connected/debug/`; keep both untracked.

Room migration marker after connected migration coverage:

```powershell
.\gradlew.bat :app:generateDebugRoomMigrationChainVerificationMarker --console=plain
```

Profileable build and macrobenchmark package:

```powershell
.\gradlew.bat :app:assembleProfileable :macrobenchmark:assembleAndroidTest --console=plain --no-daemon
```

Physical-device macrobenchmark:

```powershell
.\gradlew.bat :macrobenchmark:connectedProfileableAndroidTest --console=plain --no-daemon
```

Release signing readiness:

```powershell
.\gradlew.bat :app:checkReleaseSigningReadiness --console=plain
```

For Health Connect baseline evidence, use `scripts/collect-health-connect-runtime-evidence.ps1` only on an approved safe profile/device and write output below the ignored local `.codex/device-qa/` area. Never grant or revoke permissions merely to make a test pass.

If Java or SDK discovery fails, resolve Android Studio's bundled JBR and the SDK through `local.properties`, `ANDROID_SDK_ROOT`/`ANDROID_HOME`, then standard OS locations. Set them for the command session; do not write machine-specific paths into tracked files.

## Widening and evidence validity

```text
Edit loop -> focused proof
Pre-commit -> every affected local layer
Authorized PR -> local baseline + affected UI/data/platform/security gates
Release -> applicable full matrix + owner/device evidence
```

A passing result remains valid until relevant source, configuration, dependency, fixture, schema, test, build variant, or environment/device inputs change. After a change, rerun only invalidated gates. Never repeat a failure without inspecting evidence and changing the diagnosis, implementation, test, or environment.

Record:

- commit/tree tested;
- exact command and working directory;
- local JDK/SDK, build variant, and device/Android version when relevant;
- `PASS`, `FAIL`, or `NOT RUN` plus material warnings;
- local report/evidence path without committing generated output;
- residual risk and required human/device signoff.

## Autonomous mode

When the user says `autonoom`, the agent identifies changed surfaces and transitive boundaries, assigns risk, derives focused/completion gates, adds maintainable tests, runs them locally, provisions at most one safe emulator when required, diagnoses failures at their owning layer, and records evidence without asking questions this guide answers.

Autonomous mode does not authorize remote execution, downloads, push, PR, merge, release, signing, external-service mutation, permission changes, destructive cleanup, or deletion of user-owned evidence/caches.

## Efficiency and sustainability

- Fail fast at the cheapest layer; reproduce high-level failures lower when practical.
- Reuse Gradle up-to-date checks, configuration cache, and local build cache when compatible. Do not run `clean` unless diagnosing cache correctness.
- Reuse installed images and one agent-owned emulator. Add device profiles, matrices, or shards only for a demonstrated unique risk and available capacity.
- Batch compatible Gradle tasks into one invocation at commit/PR boundaries; do not repeatedly rerun unchanged gates during editing.
- Extend shared fixtures/builders/contracts rather than copying setup. Keep data minimal, synthetic, non-sensitive, and readable.
- Treat flaky tests as defects at their owning layer; do not normalize retries or quarantine without an explicit remediation record.
- Keep local evidence only as long as useful. Never remove user-owned artifacts or global caches as routine cleanup.
