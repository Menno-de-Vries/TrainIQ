---
name: trainiq-cycle
description: Use when running the full TrainIQ Android health app improvement cycle: choose vNext research, QA audit, polish implementation, verify-only, or stop based on blueprint, findings, progress, and regression risk.
---

# TrainIQ Cycle

Run this broader router only when the user requests the improvement cycle; ordinary fixes do not activate a whole-app audit.

Choose one next action using current status/diff and only relevant sections of the blueprint, findings, progress, backlog/research and architecture decisions:

- vnext_research: unclear or stale product/architecture direction; use trainiq-vnext-research.
- qa_audit: a requested broad assessment or material missing evidence; use trainiq-target-state-qa.
- polish: a bounded safe user request/open finding; use trainiq-polish-regression-guard.
- verify_only: evidence invalidated by relevant inputs; use test-android-apps.
- stop: no safe in-scope action remains or the selected acceptance is met.

Perform one focused batch. Follow AGENTS.md for risk, reviews, test reuse, primary checkout and authority. Read other skills only for the selected action; do not bootstrap/regenerate the entire skill catalog as a side effect.

For repeated unattended runs, update one concise existing state record with last useful change, open blocker, source/evidence and next action to prevent repeated no-ops. One-off tasks need no separate automation-state file. Update findings/progress only when changed; do not invent a global percentage or use an unmeasured percentage as a stop gate.

Stop at completed scope, repeated unchanged external blockage or missing material authority; continue independent safe work. Report unavailable checks accurately. No automatic remote actions, CI, new services, secrets changes, destructive operations or scope expansion; explicit user authority and AGENTS.md govern exceptions. A short result/evidence/remaining-risk summary suffices; no mandatory multi-section output.
