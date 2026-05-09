---
name: trainiq-polish-regression-guard
description: Use when safely implementing TrainIQ QA findings and target-state improvements without regressions.
---

# TrainIQ Polish Regression Guard

Use this skill when implementing TrainIQ improvements from `TrainIQ_Target_State_Blueprint.md`, `docs/TrainIQ_QA_Findings_To_Improve.md`, or the current findings file.

## Goal

Move the Android health app measurably closer to the target state without known regressions. Apply only small, evidence-backed changes linked to open findings or blueprint acceptance criteria.

## Inputs

Read before editing:

- `TrainIQ_Target_State_Blueprint.md`
- `docs/TrainIQ_QA_Findings_To_Improve.md` or the existing findings file
- `docs/TrainIQ_Target_State_Progress.md` when present
- Existing tests
- CI config
- Android app code
- `AGENTS.md`
- `README.md`
- Recent diffs and current worktree status

Research first:

1. Read blueprint, findings, progress, AGENTS, README, recent diffs, relevant source files, tests, and build config.
2. Map open findings to target-state acceptance criteria.
3. Identify the smallest safe implementation batch.
4. Run baseline verification before changes where feasible.

## Allowed Changes

- App code, tests, and docs directly linked to an open finding or target-state acceptance criterion.
- Findings/progress docs required to record status, evidence, and remaining risk.
- Repo-local fallback skills when missing.

## Forbidden Actions

- Do not push, publish, upload, delete broadly, rotate secrets, change signing credentials, publish builds, run destructive migrations, or perform external account actions.
- Do not perform broad redesigns, dependency churn, architecture rewrites, external service changes, signing changes, secret rotation, or destructive migrations.
- Do not invent sources, citations, medical claims, privacy claims, compliance claims, metrics, or product requirements.
- Do not leave known regressions.
- Ask only for credentials, paid accounts, destructive actions, legal/privacy risk, or major product choices.

## Implementation Priorities

1. P0: crash, data loss, privacy/security, broken core flow.
2. P1: target-state feature gaps, backend/data correctness, permission/lifecycle problems.
3. P2: UX/accessibility/polish that is safe and verifiable.
4. P3: cosmetic polish only after higher priorities pass.

Execution rules:

- Prefer a worktree for implementation.
- Apply only changes directly linked to blueprint or findings.
- Preserve existing UX patterns unless the target state or finding requires change.
- Use the smallest reversible change.
- If a finding conflicts with the blueprint, mark it `needs-decision` and continue with safe findings.
- If any change causes a regression, fix immediately or revert before final output.
- Keep batches small and verifiable.

## Autonomous Webresearch Rules

Use websearch when an issue is harder than expected, ambiguous, blocked by tooling, or likely affected by current Android, Gradle, dependency, or platform behavior.

Websearch triggers:

- Build, test, lint, or Gradle failure
- Emulator, device, adb, or logcat issue
- Android lifecycle, permission, or background behavior uncertainty
- Health Connect or health-data API uncertainty
- Dependency or API change
- Backend, data, sync, security, or privacy implementation uncertainty
- Accessibility or Material behavior uncertainty
- Performance, jank, memory, or battery concern
- Multiple possible fixes with unclear tradeoff

Rules:

- Start with official or primary sources: Android docs, Jetpack docs, Gradle docs, Health Connect docs, Material docs, dependency docs, release notes, changelogs, GitHub issues/PRs, and maintainer comments.
- Use Stack Overflow, blogs, and forums only as context or workaround hints.
- Search exact errors, versions, package names, API levels, plugin versions, and affected file names.
- Compare found advice with local code, lockfiles, configs, and tests.
- Apply the smallest source-supported fix within scope.
- Record sources and reasoning in the findings/progress file.

## Regression Guard Rules

- Run baseline verification before changes where feasible.
- Add or update targeted tests for changed behavior.
- Prefer test-first for behavior changes.
- Keep code changes local to the finding.
- Re-run targeted checks after every meaningful change.
- Run broader checks before completion.
- If a regression appears, fix it immediately or revert the related change.
- Do not mark a finding done without verification evidence.

## Android Verification

Required checks:

- `./gradlew assembleDebug` or detected equivalent
- `./gradlew test` or detected equivalent
- Lint/static checks if configured
- Targeted tests for changed behavior

Emulator/device smoke if available:

- Install and launch the app
- Navigate changed/core flows
- Check logcat for crashes/exceptions

Accessibility checks where UI changed:

- Touch targets
- Content descriptions
- Font scaling
- Dark mode/contrast

Backend/data checks where data changed:

- Repository/use-case tests
- Migration, sync, offline, and error behavior

If a check cannot run, record `NOT RUN` with the exact reason and the next best check.

## Findings And Progress Updates

Update the findings file with:

- `status`: `done` | `partially-done` | `blocked` | `needs-decision`
- Files changed
- Verification evidence
- External sources used, if any
- Remaining risk

Create or update `docs/TrainIQ_Target_State_Progress.md` with:

- Updated date
- Target-state alignment score: 0-100%
- Completed findings
- Partially completed findings
- Remaining P0/P1/P2/P3 findings
- Webresearch performed
- Regression checks run
- Known blockers
- Next safest actions

## Final Output Format

Use this structure:

```markdown
## Result
- Bootstrap/skill status
- implemented improvements
- findings closed/updated
- target-state progress

## Webresearch
- issues researched
- sources used
- fixes chosen
- rejected alternatives

## Verification
- baseline checks
- after-change checks
- PASS/FAIL/NOT RUN + reason

## Regression status
- known regressions: none | list
- reverted changes, if any
- remaining risks

## Files changed
- path: reason

## Target-state progress
- previous alignment if available
- current estimated alignment: 0-100%
- delta
- next highest-impact safe step
```

## Stop Conditions

Stop when:

- Selected safe batch is completed and verified.
- No open safe findings remain.
- The next fix requires a product, medical, privacy, or legal decision.
- Required credentials, device, or tooling are unavailable.
- Repeated verification failure cannot be safely resolved.
- A regression cannot be fixed safely, so the change is reverted.
