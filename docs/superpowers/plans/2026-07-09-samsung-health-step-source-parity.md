# Samsung Health Step Source Parity Implementation Plan

Date: 2026-07-09  
Status: automated candidate complete; physical Samsung acceptance pending

## Goal

Make TrainIQ display Samsung Health All steps whenever the consented Samsung Health Data SDK can return `DataType.StepsType.TOTAL`, while retaining a deduplication-safe Health Connect fallback and preserving existing app data.

## Implemented

- [x] Recognize Samsung Health's official `com.sec.android.app.shealth` package through one shared constant.
- [x] Keep raw Samsung `StepsRecord` sums diagnostic-only; they cannot select the displayed value.
- [x] Make every successful direct Samsung TOTAL read authoritative, including a valid zero.
- [x] Treat nullable Samsung aggregate buckets as zero and reject non-numeric values explicitly.
- [x] Keep legacy Gson mapping direct-first, expire unknown-date scalars on sync, and require no appdata removal.
- [x] Add `stepsLocalDate` ownership so yesterday's scalar total cannot survive a date rollover.
- [x] Use one local-day range per full/incremental sync for Health Connect and Samsung reads.
- [x] Preserve same-day cache only for transient read failures; do not reuse direct cache after explicit permission loss.
- [x] Distinguish change-token failures from actual aggregate failures.
- [x] Keep modern display zero from falling back to legacy raw-record sums and render that zero on Home.
- [x] Preserve unrelated cached metrics when resolving step failures.
- [x] Mark a temporarily reused direct Samsung cache value as stale in Settings/clipboard diagnostics.
- [x] Keep fresh direct values fresh when Health Connect aggregation fails, while marking reused display cache stale.
- [x] Treat successful zero as measured data without presenting missing permission/error as zero.
- [x] Stop persisting raw step-record details and purge legacy raw cache rows.
- [x] Persist replacement/removal semantics for expired tokens and block revoked-metric token re-entry.

## Regression coverage

- [x] RED/PASS: official Samsung package classification.
- [x] RED/PASS: lower direct Samsung TOTAL beats higher Health Connect values.
- [x] RED/PASS: successful direct zero survives snapshot merge, persistence mapping and Home rendering.
- [x] RED/PASS: legacy Gson cache recomputes direct-first.
- [x] RED/PASS: raw Samsung records remain diagnostics in selection, mapping and user-facing copy.
- [x] RED/PASS: nullable/non-numeric Samsung aggregate values.
- [x] RED/PASS: aggregate error propagation and change-token isolation.
- [x] RED/PASS: same-day preservation, permission loss and cross-day invalidation.
- [x] PASS: complete `HealthConnectPermissionPolicyTest` and related mapper/Home/Settings/use-case tests.

## Verification

- [x] `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin` — PASS.
- [x] `--no-configuration-cache :app:assembleSamsungHealthParityDebug` — PASS; `samsung-health-data-api-1.1.0.aar` ready.
- [x] Emulator update install and cold launch — PASS; resumed `MainActivity`, live process, empty crash buffer.
- [ ] Physical Samsung: Sync now, grant Samsung access, refresh TrainIQ, compare All steps, copy Settings diagnosis.

The physical checkbox is intentionally open because Samsung Health Data SDK is unsupported on emulators and no physical Samsung was connected during this run.

The untracked user handoff remains preserved as historical input. Its older higher-Health-Connect/lower-direct rule is superseded by this plan's direct-Samsung-authoritative policy.
