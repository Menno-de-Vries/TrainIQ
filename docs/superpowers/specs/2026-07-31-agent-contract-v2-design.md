# TrainIQ Agent Contract v2 Design

**Date:** 2026-07-31
**Status:** Approved design, pending written-spec review

## Goal

Refine `AGENTS.md` into a shorter execution contract that produces more autonomous, consistent, and verifiable work without losing TrainIQ-specific product, architecture, privacy, AI, Health Connect, release, or Git knowledge.

Target balance: roughly 80% executable rules and 20% product context, in approximately 80–90 lines where readability permits.

## Contract structure

The contract will establish this priority order:

```text
Safety and authority -> User intent -> Architecture -> UX quality -> Verification -> Speed
```

It will then define:

1. TrainIQ product truth and repository routing.
2. A default autonomous execution loop.
3. A stronger mode activated when the user explicitly says `autonoom`.
4. Product-specific gates for Compose UI/UX, Health Connect, Gemini, Room, privacy, and releases.
5. Autonomous Android toolchain and emulator handling.
6. A scalable, risk-based testing strategy.
7. Durable source and web-research rules.
8. Focused Git, verification, and completion rules.

Repeated workflow, safety, and verification statements will be consolidated. Existing requirements may be rewritten but not silently removed.

## Autonomous execution

Default work follows:

```text
Inspect -> Decide -> Implement -> Verify -> Commit -> Report
```

The agent derives safe details from repository evidence, defines acceptance criteria and risk level before editing, makes material decisions once, keeps changes reversible and in scope, fixes in-scope failures, and avoids repeated approval requests. Failed checks are retried only after inspecting evidence and changing the diagnosis, implementation, or test conditions.

When the user says `autonoom`, the agent owns the scoped outcome end to end: research, planning, implementation, tests, diagnosis, bounded retries, focused local commits, and evidence-backed completion. It asks only when blocked by unavailable authority or a material unresolved risk involving data loss, privacy/security, cost, downtime, stability, or genuinely ambiguous acceptance criteria.

Autonomous mode never grants permission to push, open or merge a pull request, release, deploy, sign, change secrets, mutate remote data, grant/revoke permissions, or perform destructive operations. Those actions remain separately opt-in.

## UI/UX quality gate

Visible product work must preserve the TrainIQ blueprint and Material 3/adaptive architecture. The contract will require:

- explicit loading, empty, success, partial-data, offline, permission-denied, and recoverable error states where relevant;
- compact-phone, tablet, foldable, dark-mode, and font-scale behavior;
- accessibility semantics, focus order, touch targets, contrast, and reduced-motion-safe behavior;
- stable state restoration and navigation without duplicate routes;
- reusable design-system primitives before one-off styling;
- previews or fixtures for meaningful states and runtime visual verification for affected screens;
- calm hierarchy, concise actionable copy, progressive disclosure, and no unsupported health claims;
- performance-conscious Compose state, stable inputs, and bounded animation/recomposition.

Affected critical flows target Android Adaptive Optimized (Tier 2) behavior: functional parity across window sizes and configuration changes, layouts optimized for compact and expanded widths, and appropriate external-input support. Visual proof scales with risk: focused previews/screenshots for local changes; resizable/representative device evidence for interaction, adaptive, permission, camera, or accessibility work. A screenshot never substitutes for semantics/UI-tree inspection when accessibility or interaction is in scope.

## Android toolchain and emulator autonomy

When device validation is required, the agent discovers the SDK from `local.properties`, `ANDROID_SDK_ROOT`/`ANDROID_HOME`, or standard OS locations, then resolves `adb`, `emulator`, `avdmanager`, and Gradle tools directly. It discovers Android Studio only as a fallback or when an IDE-owned capability is actually required; routine build and QA must remain scriptable without opening the IDE.

For repeatable automated instrumented tests, prefer a project-configured Gradle Managed Device: Gradle owns provisioning, clean state, execution, reporting, and teardown. Do not add or change managed-device configuration during unrelated feature work; introduce it only as focused test-infrastructure work with verified system-image and resource impact.

Interactive, exploratory, Health Connect, camera, and manual accessibility work uses this fallback workflow:

```text
Discover tools -> List devices/AVDs -> Select isolated target -> Boot and await readiness -> Install/test -> Capture evidence -> Report ownership/state
```

- Use an explicitly addressed compatible running emulator when it is safe and not user-reserved; never send unscoped `adb` commands when multiple devices exist.
- Do not commandeer, reset, wipe, reconfigure, or change permissions on an unknown physical device or user-owned emulator.
- If the available emulator is occupied or its ownership is uncertain, start a separate compatible existing AVD. If none exists, autonomous mode may create one uniquely named agent-owned AVD only from an already installed system image and when local capacity is sufficient.
- Do not silently download SDK images or tools, accept licenses, or create a large new AVD when the required image is absent; report device QA as `NOT RUN` with the exact prerequisite.
- Use isolated launch settings, wait for `sys.boot_completed`, target the selected serial in every command, preserve user snapshots/data, and collect screenshot, UI-tree, lifecycle, and crash-log evidence appropriate to the change.
- Prefer a fresh agent-owned AVD when clean-state testing is required. Never wipe or delete an AVD or its data without separate exact authorization; report any agent-created AVD so the user can retain or remove it.
- Run at most one agent-created emulator per task unless the acceptance criteria genuinely require a device matrix; shut down only emulators started by the agent.

## Scalable testing strategy

Tests are placed at the lowest reliable layer:

- pure rules, calculations, mappers, validation, and use cases: unit tests;
- ViewModels, reducers, and flows: deterministic state/flow tests;
- Room repositories, migrations, transactions, and import/export: contract or integration tests;
- Android platform, Health Connect, permissions, lifecycle, and navigation boundaries: targeted instrumented tests;
- critical user journeys: a small stable UI smoke suite;
- startup and interaction performance: macrobenchmarks on physical hardware.

Every behavior change proves the happy path, relevant boundary cases, and failure/recovery behavior. Tests use reusable fixtures/builders, controlled clocks/dispatchers, and deterministic fakes. Avoid sleeps, shared mutable state, duplicated assertions across layers, oversized end-to-end suites, and tests coupled to implementation details.

Never weaken, skip, or delete a valid test merely to make a gate pass. Diagnose flaky behavior at its owning layer and keep expensive device coverage focused on boundaries that unit or integration tests cannot prove.

Use screenshot tests for high-value Compose components or screens when deterministic screenshot infrastructure exists. Choose cases that provide unique feedback across state, theme, font scale, or window size; do not generate their Cartesian product or silently update reference images.

Verification is risk-based: run the smallest affected checks during development, the complete affected gate before commit, and broader device/release gates only when their boundary is touched or explicitly requested.

## Durable sources and current guidance

Keep stable product invariants, authority boundaries, architecture constraints, and quality gates in `AGENTS.md`; derive volatile tool syntax, SDK behavior, library capability, model availability, and policy details from repository-pinned versions and current primary documentation.

Use this evidence order:

```text
Repository code/config -> TrainIQ ADRs and targeted guide -> Current official primary source
```

- Browse only when external guidance could materially change the solution, the user requests research, or a relevant fact is unstable. Prefer Android Developers, Google AI, and other owning vendors; record the source/date when it affects a lasting decision.
- Do not browse for stable facts already proven locally. Stop after sufficient authoritative evidence and do not introduce a new tool merely because documentation mentions it.
- Read the relevant source-of-truth sections before a narrow change; read the governing document fully for broad, cross-feature, security, migration, AI-boundary, or release work. Do not repeatedly reread unchanged material in the same task.
- If current official guidance conflicts with pinned project behavior, preserve compatibility, surface the discrepancy, and change the pin or architecture only within explicit scope and verification.

## Knowledge preservation

The revised contract retains the current requirements for:

- Kotlin/Compose/Hilt/Room project routing and UDF/UI-state architecture;
- type-safe navigation and Hilt scopes;
- Material 3, Dynamic Color, adaptive layouts, transitions, haptics, and Baseline Profiles;
- Health Connect availability, rationale-first permissions, partial/revoked states, paging, and per-record `ChangesToken` sync;
- Gemini 2.5 Flash, structured JSON/schema output, thinking budgets, bounded AI, opt-in context, local fallback, and Keystore safety;
- Room authority, migrations, transactions, safe legacy JSON import/export, and schema checks;
- generated-file exclusions, least privilege, user-work preservation, exact staging, Conventional Commits, remote-action authorization, and release owner gates;
- exact Gradle, connected-test, macrobenchmark, signing-readiness, and evidence commands.
- deterministic Android SDK/Studio discovery, serial-scoped device control, safe AVD reuse, and bounded agent-owned emulator creation.
- Gradle Managed Devices for reproducible automation when configured, selective screenshot tests, Adaptive Optimized critical flows, and current-primary-source checks for volatile guidance.

## Acceptance criteria

- `AGENTS.md` is measurably shorter and has less repetition than the current 110-line version.
- Autonomous mode has a clear trigger, responsibilities, stop conditions, and authority boundary.
- UI/UX requirements cover product states, adaptive layout, accessibility, visual verification, and Compose performance.
- Device-required work can discover the local Android toolchain and obtain an isolated emulator without disturbing user devices or silently expanding dependencies.
- The testing model is layered, deterministic, maintainable, and risk-scaled.
- The contract separates durable invariants from volatile guidance and defines when web research is required, unnecessary, or insufficient to override pinned project behavior.
- All current TrainIQ-specific requirements remain represented or are replaced by a stricter equivalent.
- Every referenced path and command exists or is verified against repository evidence.
- Documentation checks and `git diff --check` pass before the focused commit.

## Research basis

Primary guidance checked 2026-07-31:

- [Android testing strategies](https://developer.android.com/training/testing/fundamentals/strategies)
- [Gradle Managed Devices](https://developer.android.com/studio/test/managed-devices)
- [Compose screenshot testing](https://developer.android.com/training/testing/ui-tests/screenshot)
- [Adaptive app quality](https://developer.android.com/docs/quality-guidelines/adaptive-app-quality)
- [Android accessibility testing](https://developer.android.com/guide/topics/ui/accessibility/testing)
- [Health Connect synchronization](https://developer.android.com/health-and-fitness/health-connect/sync-data)
