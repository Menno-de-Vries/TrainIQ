---
name: shipit
description: Use when the user wants to commit and push current git changes with a clear summary and description based on the full backlog of changed files, docs, findings, progress, and code changes.
---

# Shipit: commit and push current git changes

## Goal
Review the complete current git backlog, create a precise commit message, commit the changes, and push the current branch.

## Trigger
Run this skill when explicitly invoked as:
- `@shipit`
- `@shipit commit and push`
- `@shipit ship current changes`

Explicit invocation is permission to commit and push the current branch, within the safety rules below.

## Safety rules
- Never force-push.
- Never amend previous commits unless explicitly requested.
- Never delete branches or tags.
- Never publish releases.
- Never rotate secrets, change signing credentials, or modify remote settings.
- Do not push if secrets, private keys, tokens, passwords, `.env` secrets, signing files, or suspicious credentials appear in the diff.
- If on `main` or `master`, commit is allowed, but push only if the user explicitly invoked `@shipit` and the branch already has a normal upstream. If uncertain, stop before push and explain.
- If there are merge conflicts, unresolved rebase state, detached HEAD, or failing required checks that cannot be explained, stop before commit or push.

## Inputs to inspect
- `git status --short --branch`
- `git diff --stat`
- `git diff`
- `git diff --staged`
- `git log --oneline -5`
- relevant docs such as:
  - `TrainIQ_Target_State_Blueprint.md`
  - `docs/TrainIQ_QA_Findings_To_Improve.md`
  - `docs/TrainIQ_Target_State_Progress.md`
  - `docs/TrainIQ_vNext_Research.md`
  - `docs/TrainIQ_Target_State_Backlog.md`
  - `.codex/automation-state/trainiq-cycle.md`
- package/build/test config if changed
- AGENTS.md or repo instructions

## Process
1. Inspect all changed, staged, unstaged, and untracked files.
2. Group changes by intent:
   - feature
   - fix
   - polish
   - QA/docs
   - tests
   - refactor
   - build/config
   - workflow/skills
3. Detect risk:
   - secrets
   - generated/build artifacts
   - accidental large files
   - unrelated noisy changes
   - broken conflict markers
   - debug-only logs or temporary files
4. Stage only appropriate project changes.
5. Run lightweight verification when feasible:
   - use repo-defined checks if obvious;
   - for Android projects prefer `./gradlew test` and/or `./gradlew assembleDebug` when not too expensive;
   - if checks are unavailable or too expensive, explain.
6. Create a commit message based on the full backlog.

## Commit message format
Use Conventional Commit style unless repo history clearly uses another style.

Format:
`type(scope): concise summary`

Body:
- 2-6 bullets describing the real change groups.
- Mention important docs/state/backlog updates.
- Mention verification run and result.
- Mention skipped checks with reason.

Good types:
- `feat`
- `fix`
- `polish`
- `docs`
- `test`
- `refactor`
- `build`
- `chore`

Scope examples:
- `trainiq`
- `android`
- `qa`
- `skills`
- `target-state`
- `backend`
- `ui`

## Commit and push
- If no changes exist, do nothing and report `No changes to commit`.
- If changes are valid:
  - `git add` relevant files
  - `git commit`
  - push current branch:
    - if upstream exists: `git push`
    - if no upstream and remote `origin` exists: `git push -u origin HEAD`
- Never use `--force` or `--force-with-lease`.

## Final output
Return:

## Result
- committed: yes/no
- pushed: yes/no
- branch:
- commit:

## Summary
- commit title
- commit body

## Verification
- commands run
- PASS/FAIL/NOT RUN + reason

## Risk scan
- secrets checked
- generated/noisy files checked
- blockers

## Files included
- grouped file list
