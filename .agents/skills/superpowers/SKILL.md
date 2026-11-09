---
name: superpowers
description: "Use for codebase research, subagent orchestration, autonomous issue research, risk triage, concise handoffs, and verification-first execution."
---

# Superpowers

Use AGENTS.md's efficient execution rules for research, delegation and review. Do not automatically start the broader improvement cycle.

- Inspect the affected source/tests and choose the smallest complete change.
- When delegation is authorized, use independent ownership and one concise handoff: goal, paths, model/effort, acceptance, result/evidence and remaining issues. No separate handoff file unless needed for recovery.
- Reuse capable agents after allocator failures; avoid repeated unchanged spawn attempts. Shared files/index/device operations need coordination; independent work can run in parallel.
- Combine spec and quality review per cohesive batch. Whole-change review is for requested or materially cross-feature work; after fixes review their diff and consequences, not the entire repository again.
- Keep one task summary and one evidence index for long work. Reference logs rather than repeatedly copying them into reports. No automatic skill bootstrap, global scoring or workflow-engine implementation.
- Resolve concrete material findings, verify at the owning layer and stop at acceptance. Keep self-review and independent review distinct.
