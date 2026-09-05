# Performance Threshold Decision Record

> Release scope update (2026-09-06): [itch.io release policy](../release/itch-release-policy.md) supersedes the owner-approval and mandatory certification release gates below. LEGAL-001, PERF-001, A11Y-001, and AI-001 are retired for this personal itch.io project. Older BLOCKED/OPEN statements are historical or refer to optional certification/future Play submission, not current itch.io delivery. Preserve actual test results and technical findings; do not claim missing evidence passed.

Last updated: 2026-05-08

Status: decision gate. Thresholds are not defined in the blueprint or repo, so every threshold below is `PRODUCT_CONFIRMATION_REQUIRED`.

## Blueprint Requirement

`TrainIQ_Target_State_Blueprint.md` requires profileable/release macrobenchmark evidence and explicit p50/p95 thresholds before release. It does not define numeric thresholds.

## Proposed Metrics

| Metric | Proposed threshold | Owner | Evidence required |
|---|---|---|---|
| Cold startup p50/p95 | PRODUCT_CONFIRMATION_REQUIRED | Product + Android owner | Macrobenchmark on physical release/profileable device matrix |
| Warm startup p50/p95 | PRODUCT_CONFIRMATION_REQUIRED | Product + Android owner | Macrobenchmark on physical release/profileable device matrix |
| Top-level navigation frame jank | PRODUCT_CONFIRMATION_REQUIRED | Product + Android owner | Macrobenchmark/JankStats/Perfetto |
| Settings scroll frame jank | PRODUCT_CONFIRMATION_REQUIRED | Product + Android owner | Macrobenchmark/JankStats/Perfetto |
| Active workout logging frame jank | PRODUCT_CONFIRMATION_REQUIRED | Product + Android owner | Macrobenchmark/JankStats/Perfetto |
| Memory growth after repeated key flows | PRODUCT_CONFIRMATION_REQUIRED | Android owner | `dumpsys meminfo`/Android Studio profiler before and after repeated flows |
| Battery/network sensitivity during AI failure/retry | PRODUCT_CONFIRMATION_REQUIRED | Product + Android owner | Poor-network/offline physical-device run |
| Crash-free smoke flow | PRODUCT_CONFIRMATION_REQUIRED | Release owner | Logcat/ANR scan on physical device matrix |

## Local Checks Available Now

- `:app:assembleDebug`
- `:app:testDebugUnitTest`
- `:app:lintDebug`
- `:app:compileDebugAndroidTestKotlin`
- `:macrobenchmark:assembleProfileable`
- Emulator install/launch/logcat smoke.

Local checks do not replace physical-device release/profileable certification.

## Physical Devices Required

Minimum recommended matrix:

- Low/mid Android phone on Android 12 or 13.
- Current mainstream Android phone on Android 14 or 15.
- Large phone on Android 14+.
- Tablet or foldable on Android 14+ if release scope includes larger screens.

Specific device models: `PRODUCT_CONFIRMATION_REQUIRED`.

## Decision Gate

Release cannot claim performance certification until:

1. Product/Android owner sets numeric thresholds.
2. Physical devices are selected.
3. Profileable or release macrobenchmarks run on those devices.
4. Evidence is archived in `docs/qa/performance-evidence-template.md` or attached release artifacts.

## Owner Handoff Control

Status: `OPEN`

Owner role:

- Product owner
- Android owner
- Release owner

Decision required:

- Set numeric thresholds for every metric listed above.
- Approve the physical device matrix.
- Decide whether any flow is release-blocking or release-scoped out.

Allowed options:

- Approve thresholds and run device-lab certification.
- Request revised metrics/device matrix.
- Block release until thresholds or devices are available.

Required evidence:

- Numeric thresholds replacing every `PRODUCT_CONFIRMATION_REQUIRED`.
- Device list with model and Android version.
- Macrobenchmark/profileable or release results.
- Logcat crash/ANR scan.
- Completed `docs/qa/performance-evidence-template.md`.

Exact completion criteria:

- Every threshold is numeric or explicitly scoped out by product/Android owner.
- Device-lab run passes or failures have approved release exceptions.
- `docs/qa/device-lab-performance-readiness.md` links to the evidence.
- `PERF-001` in `docs/release/owner-action-tracker.md` is `APPROVED`.

Downstream docs that must be updated:

- `docs/qa/device-lab-performance-readiness.md`
- `docs/qa/performance-evidence-template.md`
- `docs/release/final-release-risk-register.md`
- `docs/release/owner-action-tracker.md`

Release impact if not completed:

- Physical-device performance certification remains blocked.
- Release status remains `BLOCKED`.

Signoff:

- Owner:
- Decision:
- Date:
- Status: `OPEN | IN_REVIEW | APPROVED | BLOCKED`
