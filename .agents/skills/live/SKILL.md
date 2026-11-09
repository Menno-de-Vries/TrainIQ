---
name: live
description: Use when live-pairing on iterative repo changes: inspect requested change, research best approach when needed, edit safely, verify, prevent regressions, and stop after each turn unless the user gives another instruction.
---

# Live Pair Development

Handle the user's current request as one focused iteration; do not start an unattended cycle unless requested. Follow AGENTS.md's efficient execution, architecture, safety and local verification rules.

- Inspect relevant instructions/source/tests and git status once; preserve unrelated work.
- State goal, scope and acceptance briefly. Use a separate design only for material unresolved choices.
- Implement the smallest complete reversible change. Research current primary sources when needed; do not automatically research every local test/compile failure.
- Use the risk-selected checks from docs/agent-guides/local-testing.md and test-android-apps. Reuse prior passing evidence until its inputs change. Documentation/skill edits require content/frontmatter/link checks, not Gradle/emulator runs.
- Apply an appropriate independent review for meaningful risk or explicit requests; combine spec/quality and scope rechecks to fixes. No automatic full-app audit or generic test task.
- Use one concise state note only for useful multi-turn continuity. Do not require duplicated reports, global scores or boilerplate final sections.
- Report outcome, checks and concrete limitations, then stop this iteration. Honor explicit pause/cancel; user-authorized autonomous scope continues until complete.

Ask only for missing material information/authority. Existing explicit authorization persists; do not ask again merely because signing, remote actions or a service is mentioned. This skill grants no extra remote/destructive authority. Never force-push, erase user work or expand scope silently. Related helper skills are optional routing aids, not reasons to bootstrap a catalog or stop when a safe supported workflow exists.
