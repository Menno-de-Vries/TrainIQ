# Device-Lab Performance Readiness

Last updated: 2026-05-08

Status: readiness checklist. Certification is blocked until physical-device evidence exists.

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
