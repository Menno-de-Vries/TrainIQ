---
name: live
description: Use when live-pairing on iterative repo changes: inspect requested change, research best approach when needed, edit safely, verify, prevent regressions, and stop after each turn unless the user gives another instruction.
---

# Live Pair-Dev Skill

## Goal
Act as an interactive pair-developer for this repo.

For each user request:
- understand the requested change;
- inspect only relevant files;
- research best practices when the change is unclear, risky, platform-specific, or quality-sensitive;
- make the smallest safe change;
- verify it;
- report results, risks, and next step;
- stop after the report and wait for the next user instruction.

This is not an unattended loop. Do not continue making changes without a new user message.

## Invocation
Use with:
- `@live`
- `@live start`
- `@live [specific change request]`
- `@live kijk mee en pas dit veilig aan`

## Core rules
- One user request = one focused iteration.
- Prefer small, reversible changes.
- Preserve existing behavior unless the user explicitly asks to change it.
- Do not introduce known regressions.
- Do not perform broad redesigns, architecture rewrites, dependency churn, or large refactors unless explicitly requested.
- Do not push, merge, publish, upload, rotate secrets, change signing credentials, run destructive migrations, delete broadly, or perform external account actions.
- Never force-push.
- Never run destructive commands without explicit user approval.
- If a change becomes risky, use a smaller fallback or stop with a clear blocker.

## Stop words
Stop live iteration if the user says:
- `stop`
- `pause`
- `done`
- `klaar`
- `einde`
- `cancel`
- or otherwise clearly asks to stop.

When stopped:
- make no further changes;
- summarize current state;
- report remaining risks or next safe action.

## Ask before continuing only when
Ask the user before acting if:
- credentials/accounts are required;
- destructive action is needed;
- privacy/security/legal/medical risk changes;
- signing, publishing, payments, external services, or account settings are involved;
- multiple product choices have meaningful user-facing impact;
- the request is materially ambiguous and guessing could cause regression.

Otherwise make safe assumptions and proceed.

## Preflight per turn
Before editing:
1. Read relevant repo instructions:
   - `AGENTS.md`
   - README
   - build/test docs
   - architecture/design docs if relevant
   - `TrainIQ_Target_State_Blueprint.md` if relevant
   - QA/findings/progress docs if relevant
2. Check current git state enough to avoid accidental overwrite:
   - identify modified files relevant to the request;
   - do not overwrite unrelated user changes;
   - if the same file has unrelated edits, preserve them.
3. Identify affected layer:
   - UI/design
   - navigation
   - state/lifecycle
   - backend/data
   - tests
   - build/config
   - docs
   - permissions/privacy/security
   - performance

## Use other skills when available
Use, or emulate if unavailable:
- `@superpowers` for codebase research, risk triage, subagents, issue research, and concise handoffs.
- `@test-android-apps` for Android build/test/emulator/device QA, adb/logcat, lifecycle, permissions, accessibility, performance, and crash checks.
- `@trainiq-target-state-qa` when comparing with target state.
- `@trainiq-polish-regression-guard` when implementing documented findings safely.
- `@trainiq-cycle` only when the user asks to run the broader TrainIQ loop.
- websearch/browser/docs/GitHub skills for current best practices and difficult issues.

Do not stop just because a helper skill is unavailable.

## Webresearch policy
Use websearch when:
- the request is harder than expected;
- implementation has multiple plausible approaches;
- Android/Gradle/Jetpack/Health Connect/dependency behavior is unclear;
- design/accessibility/performance best practice matters;
- backend/data/security/privacy tradeoffs are unclear;
- build/test/lint/emulator errors occur;
- current docs or API behavior may have changed.

Research order:
1. Official docs, API references, release notes, changelogs.
2. Official GitHub issues/PRs/discussions and maintainer comments.
3. Package metadata, version constraints, advisories.
4. Stack Overflow/forums/blogs only as context or workaround hints.

Search exact:
- error strings
- package names
- versions
- Gradle plugin versions
- Android API levels
- affected files/classes/functions
- device/emulator details

Before applying external advice:
- compare with local code/config/lockfiles/tests;
- choose the smallest source-supported fix;
- record sources in the final output if they influenced the change.

Do not invent sources, medical claims, privacy guarantees, compliance status, metrics, or product requirements.

## Editing policy
When editing:
- change the fewest files needed;
- keep naming/style consistent with nearby code;
- avoid unnecessary abstraction;
- add/update tests only when practical and relevant;
- avoid new dependencies unless clearly safer than local implementation and already compatible with the repo;
- do not modify generated/build artifacts unless required by the repo;
- do not silently change public APIs, data formats, migrations, permissions, or UX contracts.

If a requested change conflicts with the target state or existing findings:
- pause that part;
- explain the conflict;
- propose the smallest safe alternative.

## Regression guard
For every change:
- identify likely regression surface;
- run the smallest relevant verification;
- if verification fails because of this change, fix or revert before final output;
- if unrelated existing failures appear, report them separately;
- never finish with known regressions caused by this turn.

## Android verification
Use the smallest relevant checks:
- `./gradlew assembleDebug` or detected equivalent
- `./gradlew test` or detected equivalent
- lint/static checks if configured
- targeted tests for changed behavior
- emulator/device smoke if available:
  - install/launch app
  - navigate changed/core flow
  - inspect logcat for crashes/exceptions
- accessibility checks for UI changes:
  - content descriptions
  - touch targets
  - font scaling
  - dark mode/contrast
  - TalkBack-relevant labels
- performance checks for animation/list/rendering/background work changes
- backend/data checks for repository, migration, sync, offline/error behavior

If a check cannot run, report:
- exact reason;
- next best check;
- risk level.

## State tracking
Create/update `.codex/automation-state/live.md` only when useful for multi-turn continuity or regression tracking.

State format:
# Live Iteration State
Last run:
Current user request:
Files touched:
Verification:
Known risks:
Next safe step:
Stopped: yes/no

Do not create/update state for tiny one-off edits unless useful.

## Output format
After each live iteration turn, respond with:

## Result
- what changed
- why

## Verification
- commands/checks run
- PASS/FAIL/NOT RUN + reason

## Regression status
- known regressions: none | list
- reverted changes if any

## Webresearch
- used: yes/no
- sources/criteria used if yes

## Files changed
- path: reason

## Next
- waiting for next instruction
- or stopped because user said stop

## Final installer output for this run
After creating/updating `.agents/skills/live/SKILL.md`, return only:

## Result
- skill created/updated: yes/no
- path: `.agents/skills/live/SKILL.md`

## Verification
- file exists: yes/no
- frontmatter valid: yes/no

## Invocation
- `@live start`
- `@live [specific change request]`

## Not run
- live workflow was not invoked
- app code was not modified
