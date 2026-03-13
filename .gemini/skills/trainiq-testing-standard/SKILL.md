---
name: trainiq-testing-standard
description: Standardized procedures for validating business logic, AI responses (Golden Files), and health data sync. Use when writing unit tests for ViewModels, UseCases, or Mappers.
---

# Skill: TrainIQ Testing Standard 🧪

Standardized procedures for validating business logic, AI responses, and health data synchronization in the TrainIQ project.

## Core Principles
- **Predictable Outcomes:** Tests must be deterministic. Use `TestDispatcher` for all Coroutine testing.
- **Behavioral Focus:** Test *what* the system does (User Stories), not just *how* it's implemented.
- **High Coverage for Edge Cases:** Prioritize testing for null data, network failures, and permission denials.

## Testing Layers

### 1. ViewModels (UDF Testing)
- Use **Turbine** to test `StateFlow` emissions.
- Pattern: `viewModel.uiState.test { ... }`.
- Ensure the initial state is always `Loading` or a sensible default.

### 2. UseCases & Mappers
- Mappers must have 100% coverage for all domain/DTO transformations.
- UseCases should be tested with mocked repositories (using `mockk` or fakes).

### 3. AI & Health Connect
- **Health Connect:** Mock the `HealthConnectClient` to simulate different SDK statuses (Missing, Needs Update, Allowed).
- **Gemini:** Test the JSON parsing logic with "Golden Files" (pre-recorded successful AI responses) and "Malformed JSON" to ensure app stability.

## Tools & Configuration
- **Libraries:** MockK, Turbine, Kotest (Assertions), Coroutines-Test.
- **Naming:** `[Function]_[Scenario]_[ExpectedOutcome]` (e.g., `calculate1RM_withZeroReps_returnsZero`).

## Verification Checklist
- [ ] Are all new ViewModels accompanied by a `*Test.kt` file?
- [ ] Does `npm run test` (or the Gradle equivalent) pass before any PR?
- [ ] Are Room migrations tested with the `MigrationTestHelper`?
