---
name: trainiq-polish-regression-guard
description: Use when safely implementing TrainIQ QA findings and target-state improvements without regressions.
---

# TrainIQ Polish Regression Guard

Implement a bounded user-requested improvement or evidence-backed finding. AGENTS.md and the local testing guide govern architecture, authority and quality; do not duplicate their checklists.

- Read the relevant blueprint/finding sections, owning source and tests, current diff and build configuration. Preserve existing user work. Do not reread the whole backlog/README for every subtask.
- Choose the smallest complete reversible batch and acceptance criteria. Reuse the primary checkout/caches; create another worktree only under AGENTS.md's exceptions.
- Prioritize crashes/data loss/privacy/core flow, then correctness/lifecycle, then requested UX. Record unrelated issues without expanding the batch.
- Reproduce bugs with meaningful tests and cover changed happy path, material boundary and recovery. Baseline once when needed to distinguish an existing failure; focused checks during edits, affected integration gates before completion. Reuse unchanged passing evidence; never weaken a valid test.
- Use current primary documentation when local evidence cannot settle a version-dependent question, following higher-priority browsing requirements. No automatic web search for an obvious local assertion/compile error.
- Apply the appropriate independent review from AGENTS.md, consolidate findings, fix and recheck affected consequences. Do not repeat a full audit after each fix.
- Update one existing finding/progress entry or a single requested QA report with source, results, paths and limits. No invented alignment percentage, mandatory state file or boilerplate report tree.

No new dependency/service/schema/signing/permission or broad redesign without understanding its impact and authorization. Remote actions require the user's applicable explicit authority; this skill adds none and does not revoke authority already given. Finish when the selected batch and its applicable gates pass; report concrete blockers, not hypothetical owner gates.
