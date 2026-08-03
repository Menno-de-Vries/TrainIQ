# Scalable Local Testing Contract Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a compact local-only, change-scaled testing contract to `AGENTS.md` and preserve its full operational detail in one targeted testing guide.

**Architecture:** `AGENTS.md` remains the short enforcement layer; `docs/agent-guides/local-testing.md` owns the detailed matrix, commands, evidence, and maintenance rules. No executable test infrastructure or remote automation changes.

**Tech Stack:** Markdown, TrainIQ Android/Kotlin/Compose/Hilt/Room/Health Connect/Gemini conventions, local Gradle/ADB tooling.

## Global Constraints

- Test source, deterministic fixtures, schemas, and essential reproducibility assets remain tracked in Git; all test execution remains local.
- Add no GitHub workflow, hosted runner, cloud test/device service, remote build cache, dependency, Gradle task/configuration, script, emulator image, or generated artifact.
- Preserve all current TrainIQ product, architecture, privacy, safety, emulator, Git, and release rules.
- Keep `AGENTS.md` close to its current 86-line size; move detail to the guide without duplicating it.
- Cover functional, ViewModel/state, UI/UX, navigation/lifecycle, Room/migration, Health Connect/CameraX, Gemini/remote, cross-cutting, release, and performance risks.

---

### Task 1: Create the local testing guide

**Files:**
- Create: `docs/agent-guides/local-testing.md`
- Reference: `docs/superpowers/specs/2026-08-03-scalable-local-testing-contract-design.md`

**Interfaces:**
- Consumes: current repository test layout, Gradle tasks, device rules, approved design.
- Produces: one detailed guide selected whenever production behavior, tests, verification, UI, platform, data, remote boundaries, performance, or release evidence changes.

- [ ] **Step 1: Define local-only ownership and artifacts**

State explicitly that test code/assets are tracked, execution and evidence generation are local, GitHub status is not proof, remote runners/services/caches require separate authorization, and generated APKs/reports/logs/screenshots/traces/emulator data stay untracked.

- [ ] **Step 2: Add the complete change-impact matrix**

Include each approved matrix row with its cheapest sufficient layer, widening condition, and unique-risk rule. Require happy path, material boundary, and failure/recovery proof without cross-layer duplication.

- [ ] **Step 3: Add exact local gates**

Document current Windows commands from `TrainIQ-Project/`:

```powershell
.\gradlew.bat :app:assembleDebug --console=plain
.\gradlew.bat :app:testDebugUnitTest --console=plain
.\gradlew.bat :app:lintDebug --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
.\gradlew.bat :app:assembleProfileable :macrobenchmark:assembleAndroidTest --console=plain --no-daemon
.\gradlew.bat :macrobenchmark:connectedProfileableAndroidTest --console=plain --no-daemon
.\gradlew.bat :app:checkReleaseSigningReadiness --console=plain
```

Route Health Connect evidence to `scripts/collect-health-connect-runtime-evidence.ps1` and retain physical-device requirements for trustworthy performance numbers.

- [ ] **Step 4: Define widening, autonomy, failure, and sustainability rules**

Specify `edit -> focused`, `pre-commit -> affected layers`, `authorized PR -> baseline + affected gates`, and `release -> applicable full matrix`. Add input invalidation, evidence reporting, autonomous risk classification, one-emulator limit, no routine `clean`, cache reuse, deterministic fixtures, and obsolete-test cleanup.

### Task 2: Compact the root contract

**Files:**
- Modify: `AGENTS.md`
- Reference: `docs/agent-guides/local-testing.md`

**Interfaces:**
- Consumes: detailed guide from Task 1.
- Produces: compact enforceable rules that route agents to the guide and prevent remote execution.

- [ ] **Step 1: Add trigger routing**

Require reading `docs/agent-guides/local-testing.md` before behavior, test, UI/UX, platform, persistence, remote-boundary, performance, verification-tooling, PR-evidence, or release work.

- [ ] **Step 2: Replace the current testing prose with a compact contract**

Retain device safety while adding:

```text
tracked test sources != remote execution
lowest sufficient layer + unique risk
change surface + transitive boundary determines gates
focused -> affected -> PR baseline -> release matrix
autonomous mode owns classification, tests, local execution, diagnosis, and evidence
no remote CI/services/cache without separate exact authorization
```

Keep exact commands in the guide, not duplicated in `AGENTS.md`; retain only the canonical task names needed for gate recognition.

- [ ] **Step 3: Preserve all non-testing knowledge**

Check that TrainIQ mission, priority/evidence order, autonomy authority, architecture/routing, UI/UX definition, Health Connect, Gemini, Room, emulator safety, Git, publication, release, and completion requirements remain represented.

### Task 3: Verify and commit

**Files:**
- Modify: `AGENTS.md`
- Create: `docs/agent-guides/local-testing.md`
- Existing: `docs/superpowers/specs/2026-08-03-scalable-local-testing-contract-design.md`
- Existing: `docs/superpowers/plans/2026-08-03-scalable-local-testing-contract.md`

**Interfaces:**
- Consumes: Tasks 1–2.
- Produces: verified documentation-only commit.

- [ ] **Step 1: Run content and repository checks**

Verify every matrix surface, local-only boundary, autonomous behavior, command, referenced path, and preserved TrainIQ anchor. Confirm no `.github` workflow or generated/test-output file was added.

- [ ] **Step 2: Check compactness and diffs**

Compare `AGENTS.md` line count with 86, inspect scoped diffs, validate Markdown links/paths, and run `git diff --check`. Android build/test tasks are `NOT RUN` because implementation changes only policy documentation.

- [ ] **Step 3: Commit exact paths**

```powershell
git add -- AGENTS.md docs/agent-guides/local-testing.md docs/superpowers/plans/2026-08-03-scalable-local-testing-contract.md
git diff --cached --check
git commit -m "docs: scale local testing contract by change risk"
```
