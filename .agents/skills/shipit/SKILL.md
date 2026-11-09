---
name: shipit
description: Use when the user wants to commit and push current git changes with a clear summary and description based on the full backlog of changed files, docs, findings, progress, and code changes.
---

# Shipit: commit and push current changes

Explicit invocation authorizes committing and pushing the current intended task branch, not merging, releasing, changing remote settings or including unrelated work.

1. Inspect status, staged/unstaged/untracked diff and recent history. Read only docs/config relevant to actual changes. Identify intended paths; preserve unrelated work.
2. Check for secrets/signing material, generated artifacts, conflicts, accidental large files and task-related failures. Follow AGENTS.md's branch policy; do not switch an occupied checkout or push a protected/default branch without the applicable explicit authority and protections.
3. Reuse valid checks from this task. Run only missing/invalidated gates from the local testing guide; docs/skills-only work needs content/frontmatter/link validation, not an Android build. Invocation is not a reason to repeat all tests or run a generic Gradle test task.
4. Stage exact task paths, review the staged diff, create a concise Conventional Commit and push normally (set upstream if needed). No force/amend, branch deletion or broad staging.
5. After any authorized PR creation, request its chat attachment and retain the URL; bound ancillary app-RPC waits. Report unconfirmed attachment separately without confusing it with GitHub publication.

Report commit, branch, push result, meaningful checks and blockers. No mandatory long commit body, full-backlog narration or boilerplate sections. With no changes, report that fact and stop. A missing new push request is not authorization inferred from an earlier completed task.
