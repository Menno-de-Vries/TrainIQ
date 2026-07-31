# TrainIQ Agent Contract v2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace `AGENTS.md` with a shorter, decision-oriented TrainIQ contract that strengthens autonomous work, UI/UX quality, scalable testing, Android emulator handling, and durable source use without losing existing project knowledge.

**Architecture:** Keep stable invariants and decision boundaries in the root contract. Route detailed or volatile procedures to repository evidence and current official sources. Use risk-based gates so routine work stays fast while platform, privacy, UI, migration, and release changes receive proportionate validation.

**Tech Stack:** Markdown, Git, TrainIQ Android/Kotlin/Compose/Hilt/Room/Health Connect/Gemini repository conventions.

## Global Constraints

- Preserve every TrainIQ requirement listed under “Knowledge preservation” in `docs/superpowers/specs/2026-07-31-agent-contract-v2-design.md` or replace it with a stricter equivalent.
- Keep `AGENTS.md` around 80–90 readable lines where possible; prioritize clarity over a mechanical line limit.
- Autonomous mode is activated by the user saying `autonoom`, owns the scoped outcome end to end, and never expands remote or destructive authority.
- Prefer Gradle Managed Devices for configured automated instrumentation; use isolated serial-scoped manual AVD handling for interactive QA.
- Target Adaptive Optimized behavior for affected critical UI flows and use the lowest reliable test layer.
- Add no dependency, Gradle configuration, emulator, SDK image, CI workflow, or generated artifact in this documentation-only implementation.
- Modify and stage only `AGENTS.md` and this plan/spec documentation.

---

### Task 1: Implement the compact execution contract

**Files:**
- Modify: `AGENTS.md`
- Reference: `docs/superpowers/specs/2026-07-31-agent-contract-v2-design.md`

**Interfaces:**
- Consumes: current root agent contract, approved v2 design, existing repository paths and Gradle tasks.
- Produces: one root contract governing future TrainIQ agent work.

- [ ] **Step 1: Record the pre-change contract baseline**

Run:

```powershell
(Get-Content AGENTS.md).Count
git status --short --branch
```

Expected: approximately 110 lines and no unrelated worktree changes.

- [ ] **Step 2: Rewrite `AGENTS.md` as a decision-oriented contract**

The document must contain these compact sections and rules:

```text
Mission and precedence
Scope and source routing
Autonomous execution (default loop plus explicit autonomous mode)
Architecture and product invariants
UI/UX definition of done
Android device and emulator strategy
Scalable testing and verification matrix
Safety, Git, publication, and completion boundaries
```

Include exact project requirements for type-safe navigation, sealed `uiState`, Hilt scopes, Room authority/migrations, per-record Health Connect `ChangesToken`, Gemini 2.5 Flash structured JSON and thinking budgets, Material 3/adaptive/accessibility behavior, Gradle commands, release gates, generated paths, exact staging, local commits, and separately authorized push/PR/merge/release actions.

- [ ] **Step 3: Verify knowledge coverage and compaction**

Run a PowerShell content check for the required anchors:

```text
autonoom, Inspect -> Decide -> Implement -> Verify -> Commit -> Report,
MaterialTheme.colorScheme, Adaptive Optimized, semantics, Gradle Managed Device,
ChangesToken, gemini-2.5-flash, responseMimeType, thinkingBudget,
AutoMigration, testDebugUnitTest, connectedDebugAndroidTest,
checkReleaseSigningReadiness, git diff --check, explicit user request
```

Also verify every referenced local path exists and compare the new line count with the baseline.

- [ ] **Step 4: Run documentation verification**

Run:

```powershell
git diff --check
git diff -- AGENTS.md
git status --short --branch
```

Expected: no whitespace errors, no unrelated changes, and a scoped readable diff. Android build/test tasks are `NOT RUN` because only Markdown policy changes.

- [ ] **Step 5: Commit the implementation**

Stage exact paths and commit:

```powershell
git add -- AGENTS.md docs/superpowers/plans/2026-07-31-agent-contract-v2.md
git diff --cached --check
git commit -m "docs: strengthen autonomous TrainIQ agent contract"
```

### Task 2: Publish, review, and merge

**Files:**
- No additional repository files.

**Interfaces:**
- Consumes: clean `codex/merge-agent-contract` with the verified contract commits.
- Produces: pushed branch, GitHub PR targeting `main`, and a normal protected merge when checks permit.

- [ ] **Step 1: Run publication safety checks**

Inspect status, complete branch diff against `origin/main`, changed-file sizes, conflict markers, suspicious secret filenames/content, generated artifacts, and commit history. Stop on unexplained risk or failure.

- [ ] **Step 2: Push without rewriting history**

Run `git push -u origin codex/merge-agent-contract`. Never use force options.

- [ ] **Step 3: Create the pull request**

Create a PR to `main` describing purpose, scope, exact documentation verification, Android checks not run, and the absence of runtime risk.

- [ ] **Step 4: Inspect checks and mergeability**

Read PR status/checks. Do not bypass required checks or protections. If checks are pending, wait; if failing, diagnose only in-scope failures.

- [ ] **Step 5: Merge normally and verify remote state**

Merge the PR with the repository-supported normal method, then verify the PR is merged and `origin/main` contains the merge result. Do not delete the branch unless separately requested.
