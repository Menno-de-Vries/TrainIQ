# Device-Lab Performance Readiness

> Release scope update (2026-09-06): [itch.io release policy](../release/itch-release-policy.md) supersedes the owner-approval and mandatory certification release gates below. LEGAL-001, PERF-001, A11Y-001, and AI-001 are retired for this personal itch.io project. Older BLOCKED/OPEN statements are historical or refer to optional certification/future Play submission, not current itch.io delivery. Preserve actual test results and technical findings; do not claim missing evidence passed.

Last updated: 2026-05-11

Status: partial physical-device evidence exists. Certification remains blocked until thresholds, device matrix, broader flow memory evidence, and owner approval exist.

## Build Commands

```powershell
.\gradlew.bat :app:assembleProfileable :macrobenchmark:assembleAndroidTest --console=plain --no-daemon
.\gradlew.bat :macrobenchmark:connectedProfileableAndroidTest --console=plain --no-daemon
```

Optional release build, owner-controlled signing only:

```powershell
.\gradlew.bat :app:assembleRelease --console=plain --no-daemon
```

## Required Evidence Before Certification

- Device model and Android version for each run.
- Build variant and commit/build identifier.
- Macrobenchmark output and generated reports.
- Logcat scan for crash/ANR/repeated severe app errors.
- Frame timing or jank evidence for startup, tab switching, settings scroll, and active workout logging.
- Memory snapshot before and after repeated navigation/workout/nutrition flows.
- Notes on thermal state, battery saver, and network condition.

## Local Readiness Status

| Area | Local status | Blocker |
|---|---|---|
| Macrobenchmark module | Present | Physical device run required |
| Baseline profile file | Present | Regeneration evidence required per release train |
| Debug build/test/lint | Available locally | Does not certify release performance |
| Emulator smoke | Available locally | Emulator is not device-lab certification |
| Numeric thresholds | Missing | PRODUCT_CONFIRMATION_REQUIRED |

## Physical-Device Evidence

- 2026-05-11 SM-S931B profileable macrobenchmark: `docs/qa/performance-evidence-2026-05-11-sm-s931b-profileable.md`
- PASS: `:app:assembleProfileable :macrobenchmark:assembleAndroidTest`
- PASS: `:macrobenchmark:connectedProfileableAndroidTest` with 3 tests, 0 failures, 0 errors.
- Recorded metrics include cold startup `timeToInitialDisplayMs` min / median / max of 202.812761 / 245.662812 / 337.020729 ms, plus top-level navigation and Settings scroll frame timing.
- Follow-up profileable launch/memory/crash evidence captured `Status: ok`, `WaitTime: 161`, after-navigation `TOTAL PSS: 108947`, `TOTAL RSS: 219432`, `TOTAL SWAP PSS: 1267`, and empty logcat/dropbox crash/ANR slices in `.codex/device-qa/2026-05-11-profileable-memory-crash/`.
- Active-workout logging was first attempted and blocked by missing profileable app state, then rerun with a profileable-only deterministic seed. The targeted `activeWorkoutLoggingFrames` run passed on SM-S931B with 1 test, 0 failures, 0 errors; metrics recorded `frameDurationCpuMs` P50 / P90 / P95 / P99 of 1.687865 / 2.4953752 / 2.960833 / 3.22850592 ms and `frameOverrunMs` P50 / P90 / P95 / P99 of -5.288078 / -4.1804796 / -3.9324458 / -3.733878 ms.
- Certification remains blocked because thresholds are still `PRODUCT_CONFIRMATION_REQUIRED`, the device matrix is not owner-approved, and the run does not cover nutrition scanner, AI fallback/retry, broader repeated-flow memory deltas, or release-owner signoff.

## Release Guardrail

Do not mark performance as certified until `docs/qa/performance-threshold-decision-record.md` has approved thresholds and this file links to physical-device evidence.

## Closure Control

Status: `OPEN`

Owner role: Android owner + release owner

Decision required: certify profileable/release performance on the approved physical device matrix.

Allowed options:

- Approve device-lab evidence.
- Require fixes and rerun.
- Block release because thresholds/devices/evidence are missing.

Required evidence:

- Approved thresholds from `docs/qa/performance-threshold-decision-record.md`.
- Completed physical-device run artifacts.
- Macrobenchmark reports, screenshots/logs where relevant, and crash/ANR scan.

Exact completion criteria:

- All required flows have recorded evidence.
- All required metrics meet thresholds or have approved release exceptions.
- `PERF-001` in `docs/release/owner-action-tracker.md` is `APPROVED`.

Release impact if not completed: release remains `BLOCKED`; do not claim physical-device performance certification.

Signoff:

- Owner:
- Decision:
- Date:
- Status: `OPEN | IN_REVIEW | APPROVED | BLOCKED`
