---
name: trainiq-target-state-qa
description: Use when QA auditing TrainIQ Android app backend, data, design, features, Android quality, tests, privacy, and release readiness against TrainIQ_Target_State_Blueprint.md.
---

# TrainIQ Target State QA

Use this skill for a full read-only QA audit of the TrainIQ Android app against `TrainIQ_Target_State_Blueprint.md`.

## Goal

Assess how closely the current app matches the target-state blueprint across backend, data, design, features, Android quality, tests, privacy, security, performance, and release readiness. Produce evidence-backed findings and, when useful, improve the blueprint so expectations are clearer, more testable, and less ambiguous.

## Inputs

Read and compare:

- `TrainIQ_Target_State_Blueprint.md`
- Existing QA, findings, progress, polish, audit, release, and roadmap docs
- Android app source
- Gradle/build files and lock/config files
- Tests
- Documentation, including `AGENTS.md` and `README`
- CI configuration

Research first. Inspect repo structure, modules, architecture, build system, app entry points, screens, navigation, data layer, repositories, API/local persistence, permissions, health integrations, tests, docs, and the current blueprint. Detect the stack: Kotlin/Java, Compose/XML, Room, Retrofit/Ktor, Health Connect, WorkManager, Firebase/Supabase/custom backend, and related Android libraries.

Prefer updating existing QA/findings files over creating duplicates. If no suitable findings file exists, create `docs/TrainIQ_QA_Findings_To_Improve.md`.

## Allowed Changes

- Improve `TrainIQ_Target_State_Blueprint.md` only to preserve product intent while making target state clearer, more testable, less ambiguous, and aligned with evidence.
- Create or update a findings file.
- Create or update this repo-local skill and required fallback skills.
- No app-code changes during QA-only runs.

## Forbidden Actions

- Do not change app code in a QA-only run.
- Do not push, publish, upload, delete broadly, rotate secrets, change signing credentials, or perform destructive external actions.
- Do not invent sources, citations, medical claims, privacy claims, compliance claims, metrics, or product requirements.
- Do not ask the user except for credentials, paid accounts, destructive actions, legal/privacy risk, or major product choices.

## Autonomous Webresearch Rules

Use websearch when a finding, failure, API behavior, dependency issue, emulator issue, Android permission/lifecycle behavior, Health Connect behavior, backend pattern, privacy/security risk, accessibility concern, performance issue, or possible fix is unclear or harder than expected.

Rules:

- Start with official or primary sources: Android docs, Jetpack docs, Gradle docs, Health Connect docs, Material docs, dependency docs, release notes, changelogs, GitHub issues/PRs, and maintainer comments.
- Use Stack Overflow, blogs, and forums only as context or workaround hints.
- Search exact error strings, package names, versions, Gradle plugin versions, Android API levels, device/emulator details, and affected file names.
- Compare advice with local code, lockfiles, configs, and versions before recommending anything.
- Record used sources in the findings file with title/source/date accessed.
- Do not invent sources, citations, medical claims, privacy claims, compliance claims, metrics, or product requirements.

## Android QA Checks

Run the smallest relevant checks and record exact PASS, FAIL, or NOT RUN reasons:

- Detected equivalent of `./gradlew test`
- Detected equivalent of `./gradlew assembleDebug`
- Lint/static checks if configured
- Emulator/device smoke only if available
- adb/logcat crash scan only if the app can be launched

Inspect Android-specific quality areas:

- Lifecycle and state restoration
- Runtime permissions and Health Connect permission flow
- Navigation and deep/back behavior
- Offline, loading, empty, and error states
- Crash risk and logcat evidence where launchable
- Accessibility labels, TalkBack semantics, touch targets, font scaling, color contrast, and dark mode
- Performance risks, baseline profiles, startup, recomposition, background work, WorkManager, and sync behavior
- Release readiness, R8/ProGuard, signing safety, versioning, CI, and Play-policy blockers

If a check cannot run, record the exact reason and the next best check.

## Blueprint Rules

- Preserve product intent.
- Convert vague goals into verifiable acceptance criteria.
- Add missing target-state expectations only when supported by app direction, docs, implementation evidence, or reliable research.
- Mark uncertain product choices as `Decision needed`.
- Do not invent medical, health, privacy, backend, or product claims.

## Findings Schema

Every finding must include:

- `finding_id`
- `priority`: `P0` | `P1` | `P2` | `P3`
- `area`: `backend` | `data` | `feature` | `UI` | `UX` | `accessibility` | `Android lifecycle` | `performance` | `privacy` | `security` | `tests` | `release`
- Current evidence with file references
- External sources used, if any
- Expected target-state behavior
- Concrete recommended fix
- Regression risk
- Minimal verification command/check
- `status`: `open` | `blocked` | `needs-decision`
- Owner suggestion if inferable

## Parallel QA Workstreams

Use read-only subagents where useful and permitted:

1. Backend/data/design: architecture, data flow, persistence, sync, API boundaries, privacy/security, error handling, domain logic.
2. Android app QA: lifecycle, permissions, navigation, state restoration, crash risk, offline/error/loading states, test coverage.
3. UI/UX/accessibility: target-state fit, Material patterns, spacing, empty/loading/error states, dark mode, font scaling, TalkBack labels, touch targets.
4. Test/release readiness: Gradle config, unit/instrumentation tests, emulator viability, adb/logcat, R8/ProGuard, versioning, Play-policy blockers.

Worker handoff format:

- task:
- scope:
- status: done | blocked | risky
- findings:
- changed files: none for QA unless docs only
- verification:
- risks:
- handoff_needed:

## Verification

Before final output, verify:

- Skill file has valid frontmatter and is discoverable in `.agents/skills`.
- Findings file exists and uses the requested schema.
- Any blueprint changes preserve product intent and are evidence-backed.
- Gradle/build/test/lint/emulator checks are recorded as PASS, FAIL, or NOT RUN with reasons.
- No app-code files were changed.

## Final Output Format

Use this structure:

```markdown
## Result
- QA scope completed
- Bootstrap/skill status
- Blueprint changes
- Findings file path
- Highest-risk findings

## Webresearch
- issues researched
- sources used
- fixes/recommendations supported by sources
- unresolved unknowns

## Verification
- command/check: PASS | FAIL | NOT RUN + reason

## Target-state assessment
- current estimated alignment: 0-100%
- strongest areas
- weakest areas
- top 5 fixes toward target state

## Files changed
- path: reason

## Risks/blockers
- concrete only
```

## Stop Conditions

Stop when:

- QA scope is completed.
- Required repo files are missing.
- Required credentials, device, or tooling are unavailable.
- Further progress requires a product, medical, privacy, or legal decision.
