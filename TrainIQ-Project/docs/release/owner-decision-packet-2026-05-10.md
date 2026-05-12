# Owner Decision Packet - 2026-05-12

Release status: `BLOCKED`

Filename note: this file keeps its original `2026-05-10` path so existing release references remain stable; the packet content was refreshed on 2026-05-12.

Purpose: give product, legal/privacy, security, accessibility, Android, and release owners a single handoff page for the non-local decisions still blocking TrainIQ app-ready status. This document does not approve any gate; it lists the evidence required to approve them.

## Current Local Engineering Evidence

- Debug build/test/lint/android-test compile passed after the current implementation batch.
- Targeted connected persistence test passed on SM-S931B with 26 tests, 0 failures, 0 errors, and 0 skipped.
- Profileable performance evidence is recorded in `docs/qa/performance-evidence-2026-05-11-sm-s931b-profileable.md`, including startup/navigation/active-workout macrobenchmarks, deterministic active-workout logging, profileable launch, memory capture, and empty crash/ANR slices.
- Compact/font-scale runtime evidence now covers Start, Training, Voeding, Coach, Instellingen, Voortgang, seeded active workout, Settings destructive dialogs, Health Connect rationale, scanner permission gates, and scanner permission-gate rotation at 360x640/font scale 1.5 with `NAF=0` and empty crash buffers.
- Health Connect no-permission Settings/rationale evidence is archived under `TrainIQ-Project/.codex/device-qa/2026-05-10-health-connect-*` and the repeatable scripted baseline is archived under `TrainIQ-Project/.codex/device-qa/2026-05-11-health-connect-scripted-baseline-debug-v4/`.
- Health Connect runtime matrix is recorded in `docs/qa/health-connect-runtime-matrix-2026-05-10.md`.
- Local release-gate command passed: `./gradlew.bat :app:checkReleaseSigningReadiness :macrobenchmark:compileProfileableJavaWithJavac --console=plain --no-configuration-cache`.
- Signing-readiness output still says release signing is not configured, so local release artifacts are unsigned.
- Completion audit is current in `docs/TrainIQ_App_Ready_Completion_Audit.md` and concludes TrainIQ remains not ready-to-use until owner/manual/safe-device gates are closed.

## Decisions Required

| Gate | Owner(s) | Decision | Required evidence | Current status | Source docs |
| --- | --- | --- | --- | --- | --- |
| `LEGAL-001` | Product, legal/privacy, release | Approve Data Safety, privacy policy, Health Connect declarations, and production data-sharing claims. | Completed Play worksheet, published privacy policy URL, final production build config/dependency scan, legal/privacy signoff. | `OPEN` | `docs/release/data-safety-decision-gates.md`, `docs/release/play-console-data-safety-worksheet.md`, `docs/release/privacy-policy-draft.md` |
| `PERF-001` | Product, Android, release | Set numeric startup/navigation/workout/scanner/settings thresholds and approve device matrix. | Numeric thresholds, physical-device profileable/release macrobenchmark results, crash/ANR log scan, completed performance evidence template. | `OPEN` | `docs/qa/performance-threshold-decision-record.md`, `docs/qa/device-lab-performance-readiness.md`, `docs/qa/performance-evidence-template.md` |
| `A11Y-001` | Accessibility, manual QA, release | Complete TalkBack, Switch Access, large-font, and dark-mode signoff. | Completed flow table, recordings/screenshots, tester notes, device/build/font/theme evidence, accessibility owner approval. | `OPEN` | `docs/qa/talkback-switch-access-test-script.md`, `docs/qa/human-assistive-tech-qa-signoff.md`, `docs/qa/accessibility-certification-boundary.md` |
| `AI-001` | Product, backend, security, legal/privacy | Choose production AI mode: BYOK, gateway, OAuth/account-mediated, hybrid, or AI scoped out. | Signed AI mode decision, backend/security design if applicable, updated privacy policy/Data Safety docs, Android evidence if client behavior changes. | `OPEN` | `docs/architecture/production-ai-boundary-decision-gate.md`, `docs/security/byok-vs-production-gateway-risk-register.md`, `docs/security/production-ai-boundary-checklist.md` |
| Health Connect runtime matrix | Android, release, privacy if scope changes | Approve safe test profile/device and run provider/permission edge states. | Provider missing/update, no permission, partial grant, revoke while open, background-read unavailable/granted evidence. | `PARTIAL` | `docs/qa/health-connect-runtime-matrix-2026-05-10.md` |
| Release signing/versioning | Release owner | Configure release signing/versioning or approve a dated exception for local unsigned artifacts. | Signing ownership, keystore handling policy, versioning strategy, release artifact provenance. | `OPEN` | `docs/release/owner-action-tracker.md`, `docs/release/final-release-risk-register.md` |

## Inputs Needed Before Codex Can Continue Release Evidence

- Safe Health Connect permission profile/device for provider-missing, partial-grant, revoke-while-open, and background-read cases.
- Approved real-camera/barcode test setup or explicit permission to use a connected physical device for scanner capture evidence.
- Accessibility tester/device/build details for TalkBack and Switch Access signoff.
- Performance owner thresholds and approved device matrix for certification runs.
- Approved Gemini credentials/network-use scope if live AI debrief evidence is required.
- Release owner signing/versioning decision or written exception for unsigned local artifacts.

## Approval Rules

- Do not mark `APPROVED` without owner name, date, evidence path, and downstream docs updated.
- Do not use debug launch timing as release performance certification.
- Do not use local fallback AI behavior as production AI boundary approval.
- Do not grant or revoke real Health Connect permissions on a personal device for release evidence; use a disposable test profile/device.
- Do not claim Play/Data Safety readiness until the final production AI, telemetry, Health Connect, privacy, and signing choices are consistent.

## Minimum Closure Sequence

1. Product/release owner confirms release scope: AI included or scoped out, Health Connect background read included or removed/scoped out, target device matrix, and signing/versioning plan.
2. Legal/privacy owner completes Data Safety and privacy policy updates for the final scope.
3. Android owner runs Health Connect edge-state matrix on a safe profile/device.
4. Manual QA/accessibility owner completes TalkBack/Switch Access/large-font/dark-mode signoff.
5. Android/performance owner runs device-lab profileable/release performance evidence against approved numeric thresholds.
6. Release owner updates `docs/release/owner-action-tracker.md` and `docs/release/final-release-risk-register.md`.

## Current Completion Decision

TrainIQ should remain `not ready-to-use` for release purposes until the gates above are closed or explicitly scoped out with dated owner approval.
