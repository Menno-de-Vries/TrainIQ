---
name: trainiq-cycle
description: Use when running the full TrainIQ Android health app improvement cycle: choose vNext research, QA audit, polish implementation, verify-only, or stop based on blueprint, findings, progress, and regression risk.
---

# TrainIQ Cycle

Use this skill to run the full TrainIQ Android health app improvement cycle with a short command such as `@trainiq-cycle run next safe step`.

## Cycle Goal

Make the repository self-managing for TrainIQ target-state improvement through this loop:

1. vNext research improves the blueprint and backlog.
2. QA audits app, backend, data, design, features, Android quality, tests, privacy, security, performance, and release readiness against the blueprint.
3. Polish implements safe findings without regressions.
4. Progress and state files decide the next safest action.
5. Stop when target state is reached, blocked, or no safe work remains.

## Inputs

Read before deciding:

- `TrainIQ_Target_State_Blueprint.md`
- `docs/TrainIQ_QA_Findings_To_Improve.md`
- `docs/TrainIQ_Target_State_Progress.md`
- `docs/TrainIQ_vNext_Research.md`
- `docs/TrainIQ_Target_State_Backlog.md`
- `docs/TrainIQ_Architecture_Decisions.md` when present
- `.codex/automation-state/trainiq-cycle.md` when present
- `AGENTS.md`
- `README` files
- Android app source
- Gradle/build files
- Tests
- CI config
- Recent diffs and current worktree status

## Decision Rules

Decide exactly one next action:

- `vnext_research`: choose when blueprint/backlog is stale, vague, missing backend/design direction, or new higher-level product ideas are needed.
- `qa_audit`: choose when a blueprint exists but findings are missing/stale, or the app has changed materially since the last QA.
- `polish`: choose when open P0/P1/P2 findings exist and one small implementation batch is safe.
- `verify_only`: choose when recent changes need regression checks or when no code should change but verification evidence is stale.
- `stop`: choose when no safe qualifying work remains or stop conditions are met.

Make at most one focused improvement batch per run. Do not ask routine follow-up questions.

## Skill Bootstrap Rules

Create or update these repo-local skills if missing or stale:

- `.agents/skills/trainiq-cycle/SKILL.md`
- `.agents/skills/trainiq-target-state-qa/SKILL.md`
- `.agents/skills/trainiq-polish-regression-guard/SKILL.md`
- `.agents/skills/trainiq-vnext-research/SKILL.md`
- `.agents/skills/test-android-apps/SKILL.md`
- `.agents/skills/superpowers/SKILL.md`

Each `SKILL.md` must:

- Include valid frontmatter with `name` and `description`.
- Front-load trigger words in `description`.
- Contain stable reusable instructions, not one-off chat context.
- Define inputs, allowed changes, forbidden actions, webresearch rules, verification, output format, and stop rules.
- Avoid external dependencies unless already used by this repo.

Validate discoverability by listing `.agents/skills` and checking frontmatter.

## State File Handling

Create or update `.codex/automation-state/trainiq-cycle.md` with this schema:

```markdown
# Automation State: trainiq-cycle

Last run:
Mode: cycle | qa | polish | vnext | verify_only | stop
Selected next action:
Current target-state alignment:
Last useful change:
Consecutive no-op runs:
Consecutive blocked runs:
Open findings:
- P0:
- P1:
- P2:
- P3:
Next safest action:
Stop if:
Blockers:
Verification summary:
```

Use the state file to avoid repeating no-op or blocked work. Preserve useful history where present, but keep the file concise.

## Allowed Actions

- Update repo-local skill files.
- Update `.codex/automation-state/trainiq-cycle.md`.
- Update progress/state docs needed by the selected action.
- For `vnext_research`: update blueprint/backlog/research docs only.
- For `qa_audit`: update blueprint clarity and findings docs only.
- For `polish`: change app code/tests/docs only for documented findings or target-state improvements.
- For `verify_only`: run verification and update state/progress only.
- For `stop`: update state and report why stopping is correct.

## Forbidden Actions

- Do not push, merge, publish, upload, rotate secrets, change signing credentials, delete broadly, run destructive migrations, or perform external account actions.
- Do not leave known regressions.
- Do not invent sources, medical claims, privacy claims, compliance claims, metrics, or product requirements.
- Do not perform broad redesigns, dependency churn, architecture rewrites, or external service changes in a cycle run.
- Ask only for credentials, paid accounts, destructive actions, legal/privacy risk, or major product choices.

Prefer worktrees for code-changing implementation. Keep changes small, reversible, and verifiable.

## Autonomous Webresearch Rules

Use websearch whenever an issue, fix, architecture choice, API behavior, dependency behavior, Android behavior, Health Connect behavior, privacy/security risk, design/accessibility pattern, or implementation path is unclear or harder than expected.

Rules:

- Start with official docs, release notes, changelogs, GitHub issues/PRs, and maintainer comments.
- Search exact errors, versions, package names, plugin versions, API levels, and affected files.
- Use Stack Overflow, blogs, and forums only as context.
- Compare external advice with local code, config, lockfiles, and tests.
- Record sources with title, source, URL, and date accessed in the relevant doc.
- Do not invent sources, medical claims, privacy claims, compliance claims, metrics, or product requirements.

## Selected-Action Execution

### vnext_research

- Invoke/use `trainiq-vnext-research`.
- No app-code changes.
- Improve `TrainIQ_Target_State_Blueprint.md`, `docs/TrainIQ_vNext_Research.md`, `docs/TrainIQ_Target_State_Backlog.md`, and `docs/TrainIQ_Architecture_Decisions.md` only when supported.

### qa_audit

- Invoke/use `trainiq-target-state-qa`.
- No app-code changes.
- Improve blueprint clarity and findings docs.
- Prefer updating existing findings over creating duplicates.

### polish

- Invoke/use `trainiq-polish-regression-guard`.
- Code changes are allowed only for documented findings or target-state improvements.
- Run baseline and after-change checks where feasible.
- Fix or revert regressions before final output.

### verify_only

- Run the smallest relevant verification:
  - `./gradlew assembleDebug` or detected equivalent.
  - `./gradlew test` or detected equivalent.
  - Lint/static checks if configured.
  - Targeted emulator/logcat/accessibility checks if available and relevant.
- Update progress/state.
- Make no feature changes.

### stop

- Make no code changes.
- Report why stopping is correct.
- Update state.

## Verification

Before final output:

- Validate required skill files exist and are discoverable.
- Run verification required by the selected action.
- If verification cannot run, record exact reason and next best check.
- Confirm no app code changed unless the selected action was `polish`.
- Confirm state file was updated.

## Stop Conditions

Stop when any applies:

- Target-state alignment is at least 95%.
- No open P0/P1/P2 findings remain.
- Two consecutive no-op runs.
- Two consecutive blocked runs.
- Required credentials, device, or tooling are unavailable.
- Next action requires a product, medical, privacy, or legal decision.
- Verification repeatedly fails for the same unresolved external cause.
- No safe qualifying work remains.

## Final Output Format

Use this structure:

```markdown
## Result
- Bootstrap/skill status
- selected next action
- action completed

## Invocation
- `@trainiq-cycle run next safe step`
- `@trainiq-target-state-qa audit current app`
- `@trainiq-polish-regression-guard implement next safe findings`
- `@trainiq-vnext-research improve blueprint/backlog`

## Verification
- skill files validated
- commands/checks run
- PASS/FAIL/NOT RUN + reason

## State
- state file updated
- progress file updated if applicable
- current estimated target-state alignment
- next safest action

## Files changed
- path: reason

## Risks/blockers
- concrete only
```
