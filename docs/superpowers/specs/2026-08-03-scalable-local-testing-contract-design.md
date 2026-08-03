# Scalable Local Testing Contract Design

**Date:** 2026-08-03
**Status:** Approved design, pending written-spec review

## Goal

Extend TrainIQ's agent contract with a compact test-growth system that scales with functionality, UI/UX, data and remote boundaries, Android platform integration, and release risk. Keep all execution local while retaining test source and reproducibility assets in Git.

## Current evidence

- The repository currently tracks 61 JVM unit tests, 9 instrumented tests, and 1 macrobenchmark source.
- No GitHub Actions or other tracked remote test workflow exists.
- Existing commands cover unit tests, lint, debug assembly, connected tests, macrobenchmarks, and release-signing readiness, but `AGENTS.md` does not map change types to widening local gates.

## Chosen structure

Use two layers:

1. `AGENTS.md` receives the enforceable local-only policy, test-growth rules, compact change-impact matrix, and autonomous gate-selection behavior.
2. `docs/agent-guides/local-testing.md` becomes the detailed source for test categories, triggers, commands, artifacts, failure handling, emulator use, and evidence reporting.

This keeps the root contract fast to read while giving agents one targeted guide when testing is relevant. No script, dependency, Gradle configuration, emulator image, or CI workflow is added in this change.

## Local-only policy

- Track test source, deterministic fixtures, schemas, and the minimum reproducibility assets required to run tests.
- Run builds, tests, lint, instrumented flows, screenshots, accessibility checks, migration verification, and benchmarks only on the user's local machine or explicitly approved local device.
- Never add GitHub Actions, hosted runners, cloud device farms, remote test services, or remote build caches unless the user separately requests that exact remote capability.
- Never treat GitHub check status as test evidence. Before an authorized PR or merge, report exact local commands, commit under test, environment/device where relevant, and PASS/FAIL/NOT RUN results.
- Do not commit generated reports, APK/AAB files, logs, screenshots captured as evidence, traces, emulator data, caches, or other test output. Small intentional screenshot references may be tracked only when deterministic screenshot infrastructure is explicitly adopted and each reference supplies unique regression value.

## Scalable test-growth model

Every behavior change adds or updates the lowest-cost test that reliably proves its unique risks. Coverage grows with product behavior and boundary complexity, not with line count or a fixed percentage.

| Changed surface | Required local proof |
|---|---|
| Documentation or agent policy | Content, referenced paths/links, scoped diff, `git diff --check` |
| Pure domain rule, mapper, formatter, validation, use case | Focused JVM unit test, then affected module unit suite |
| ViewModel, reducer, flow, UI state | Deterministic state/flow/component tests plus affected unit suite |
| Compose layout, copy, accessibility, adaptive UX | State fixtures/previews, semantics assertions where useful, selective visual/runtime inspection, compile and lint |
| Navigation, lifecycle, restoration, permission orchestration | Local logic/component tests plus targeted instrumented flow |
| Room entity, DAO, repository, transaction, import/export, migration | Mapper/unit tests, repository/transaction contracts, migration-chain or instrumented database proof |
| Health Connect or CameraX boundary | Deterministic fakes/contracts plus targeted safe-profile device validation for platform behavior |
| Gemini, `data/remote`, telemetry, or future backend boundary | Request/response/schema contracts, timeout/cancellation, retry, privacy minimization, error and deterministic fallback tests; no live production mutation |
| Cross-cutting architecture, shared persistence, build/test tooling | All affected lower layers plus full local baseline and applicable connected/security gates |
| Release or performance-critical journey | Release/profileable checks, critical-flow smoke evidence, macrobenchmark; performance claims only from approved physical hardware |

Each behavior proves its happy path, material boundary case, and failure/recovery path. Do not duplicate equivalent assertions across layers. Add a broader device or end-to-end test only when a cheaper layer cannot prove the risk. When behavior is removed, remove or update obsolete tests and fixtures in the same change.

## Execution widening

Agents select gates from both changed paths and transitive risk:

```text
Edit loop -> focused test
Pre-commit -> every affected local layer
Authorized PR -> debug assemble + module tests + lint + affected platform/data/UI gates
Release -> complete applicable local matrix + owner/device evidence
```

Passing evidence remains valid until relevant source, configuration, dependency, fixture, schema, or environment inputs change. Do not rerun unchanged gates. A failed gate is retried only after evidence-driven diagnosis or changed inputs.

## Autonomous behavior

When the user says `autonoom`, the agent:

1. identifies changed surfaces and transitive boundaries;
2. assigns a risk level and derives the focused and completion gates;
3. adds or updates maintainable tests as part of the implementation;
4. provisions one safe local emulator when platform proof is necessary and no suitable target is free;
5. diagnoses failures at their owning layer and widens coverage only when evidence justifies it;
6. records exact local evidence for the commit or authorized PR;
7. asks no testing-preference questions that repository evidence and this matrix answer.

Autonomous work does not authorize remote test execution, push, PR, merge, release, signing, external-service mutation, permission changes, destructive cleanup, or downloads outside existing authority.

## Efficiency and sustainability

- Prefer unit/component/contract tests over device tests; keep critical UI/release smoke suites small and stable.
- Use existing Gradle up-to-date checks, configuration cache, and local build cache when compatible. Do not run `clean` unless diagnosing cache correctness.
- Reuse one agent-owned emulator and installed images; do not create device matrices or shards without demonstrated need and capacity.
- Avoid sleep-based synchronization, shared mutable test state, live network dependence, combinatorial screenshot matrices, and tests coupled to implementation details.
- Use reusable builders, fakes, controlled clocks/dispatchers, stable test data, and explicit contracts so new features extend existing test vocabulary.
- Fail fast at the cheapest layer. A high-level failure should produce enough local evidence to reproduce it at a lower layer where practical.
- Generated evidence stays local and is reported by path/result, not committed. Never delete user-owned evidence or caches as routine cleanup.

## Acceptance criteria

- `AGENTS.md` explicitly distinguishes tracked test source from local-only execution.
- `AGENTS.md` contains a compact change-impact and widening policy without materially increasing its current 86-line size.
- The local testing guide covers every matrix row, exact current commands, evidence rules, failure handling, and autonomous selection.
- Functional, UI/UX, data/remote, platform, migration, performance, and release changes each have proportional local gates.
- No GitHub workflow, remote runner/service, new dependency, Gradle test task, script, or generated artifact is introduced.
- Existing TrainIQ architecture, safety, privacy, emulator, Git, and release rules remain intact.
- All referenced repository paths and commands are verified; documentation and `git diff --check` checks pass before commit.

## Research basis

Primary guidance checked 2026-08-03:

- [Android testing strategies](https://developer.android.com/training/testing/fundamentals/strategies)
- [Android command-line testing](https://developer.android.com/studio/test/command-line)
- [Gradle Managed Devices](https://developer.android.com/studio/test/managed-devices)
- [Compose screenshot testing](https://developer.android.com/training/testing/ui-tests/screenshot)
- [Gradle build cache](https://docs.gradle.org/current/userguide/build_cache.html)
- [Gradle configuration cache](https://docs.gradle.org/current/userguide/configuration_cache_enabling.html)
