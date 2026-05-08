# TrainIQ Finalization State

Date: 2026-05-08
Branch: `codex/finalize-target-state`

## Assumptions

- The Android app project is `TrainIQ-Project`; repository-level blueprint and AGENTS instructions are authoritative.
- Release signing must be configurable without committed secrets; unsigned local release builds remain acceptable when no signing inputs are present.
- Compatibility-first means preserving the existing repository behavior while moving its runtime state source to Room before deeper repository/use-case splitting.

## Current Phase Status

- Room runtime authority adapter: implemented via `RoomTrainIqRuntimeStore`.
- Legacy JSON store: no longer injected into `TrainIqRepository` or `SettingsViewModel`.
- Settings reset/clear: routed through `ResetProfileUseCase` and `ClearAppDataUseCase`.
- Adaptive route propagation: added to top-level feature routes with compact defaults.
- Release signing: Gradle/env configuration, readiness task, docs, and GitHub Actions workflow added.
- Repository interface bindings: split across focused Room repository implementations; legacy coordinator no longer implements domain repository interfaces.
- Use case orchestration: `BuildHomeDashboardUseCase` owns Health Connect/dashboard merge; `StartWorkoutSessionUseCase` owns start validation, progression drafts, and session start orchestration.
- Coordinator extraction: active workout session mutations, workout progression/readiness policy, and exercise library seeding extracted into focused, behavior-tested units.
- Android QA: active workout restore, camera denied/granted, settings reset/clear, expanded layout, rotation, launch, and logcat checks completed on emulator.
- Validation checkpoint: `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:connectedDebugAndroidTest`, `:app:assembleRelease`, and `:app:bundleRelease` passed after current changes.

## Open Target-State Work

- none known
