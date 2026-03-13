# AGENTS.md instructions for d:\GitHub\TrainIQ

## Skills
A skill is a set of local instructions to follow that is stored in a `SKILL.md` file. Below is the list of skills that can be used. Each entry includes a name, description, and file path so you can open the source for full instructions when using a specific skill.

### Available skills
- trainiq-core-architecture: Enforce MVVM + Clean + UDF architecture in the TrainIQ project. Use when creating new features, ViewModels, or refactoring existing code to ensure consistency in state management and navigation. (file: D:/GitHub/TrainIQ/.gemini/skills/trainiq-core-architecture/SKILL.md)
- trainiq-gemini-reasoning: Expert implementation of Gemini 2.5 Flash in TrainIQ. Use when designing prompts, implementing hybrid reasoning (Thinking Budget), or building structured AI response systems. (file: D:/GitHub/TrainIQ/.gemini/skills/trainiq-gemini-reasoning/SKILL.md)
- trainiq-health-sync: Specialized knowledge for Samsung Health / Health Connect integration in TrainIQ. Use when implementing data sync, permission flows, or data mapping from health records to domain models. (file: D:/GitHub/TrainIQ/.gemini/skills/trainiq-health-sync/SKILL.md)
- trainiq-strength-science: Strength training formulas and progression rules for TrainIQ. Use when implementing or reviewing 1RM calculations, RPE/RIR logic, set typing, plate math, rest timers, or workout volume tracking. (file: D:/GitHub/TrainIQ/.gemini/skills/trainiq-strength-science/SKILL.md)
- trainiq-nutrition-vision: Vision-based nutrition logging guidance for TrainIQ. Use when implementing food recognition, supplement detection, nutrition prompt design, AI-to-macro mapping, or confirm-before-save meal flows. (file: D:/GitHub/TrainIQ/.gemini/skills/trainiq-nutrition-vision/SKILL.md)
- trainiq-ui-motion: Material 3 motion and interaction guidance for TrainIQ. Use when building Compose animations, haptic feedback, dynamic color flows, loading states, or predictive navigation transitions. (file: D:/GitHub/TrainIQ/.gemini/skills/trainiq-ui-motion/SKILL.md)
- trainiq-testing-standard: Test conventions for TrainIQ business logic and integrations. Use when adding or reviewing ViewModel tests, mapper and use-case coverage, AI parsing tests, Health Connect mocks, or migration validation. (file: D:/GitHub/TrainIQ/.gemini/skills/trainiq-testing-standard/SKILL.md)
- openai-docs: Use when the user asks how to build with OpenAI products or APIs and needs up-to-date official documentation with citations, help choosing the latest model for a use case, or explicit GPT-5.4 upgrade and prompt-upgrade guidance; prioritize OpenAI docs MCP tools, use bundled references only as helper context, and restrict any fallback browsing to official OpenAI domains. (file: C:/Users/menno/.codex/skills/.system/openai-docs/SKILL.md)
- skill-creator: Guide for creating effective skills. This skill should be used when users want to create a new skill (or update an existing skill) that extends Codex's capabilities with specialized knowledge, workflows, or tool integrations. (file: C:/Users/menno/.codex/skills/.system/skill-creator/SKILL.md)
- skill-installer: Install Codex skills into $CODEX_HOME/skills from a curated list or a GitHub repo path. Use when a user asks to list installable skills, install a curated skill, or install a skill from another repo (including private repos). (file: C:/Users/menno/.codex/skills/.system/skill-installer/SKILL.md)

### How to use skills
- Discovery: The list above is the skills available in this session (name + description + file path). Skill bodies live on disk at the listed paths.
- Trigger rules: If the user names a skill (with `$SkillName` or plain text) OR the task clearly matches a skill's description shown above, you must use that skill for that turn. Multiple mentions mean use them all. Do not carry skills across turns unless re-mentioned.
- Missing/blocked: If a named skill isn't in the list or the path can't be read, say so briefly and continue with the best fallback.
- How to use a skill (progressive disclosure):
  1. After deciding to use a skill, open its `SKILL.md`. Read only enough to follow the workflow.
  2. When `SKILL.md` references relative paths (e.g., `scripts/foo.py`), resolve them relative to the skill directory listed above first, and only consider other paths if needed.
  3. If `SKILL.md` points to extra folders such as `references/`, load only the specific files needed for the request; don't bulk-load everything.
  4. If `scripts/` exist, prefer running or patching them instead of retyping large code blocks.
  5. If `assets/` or templates exist, reuse them instead of recreating from scratch.
- Coordination and sequencing:
  - If multiple skills apply, choose the minimal set that covers the request and state the order you'll use them.
  - Announce which skill(s) you're using and why (one short line). If you skip an obvious skill, say why.
- Context hygiene:
  - Keep context small: summarize long sections instead of pasting them; only load extra files when needed.
  - Avoid deep reference-chasing: prefer opening only files directly linked from `SKILL.md` unless you're blocked.
  - When variants exist (frameworks, providers, domains), pick only the relevant reference file(s) and note that choice.
- Safety and fallback: If a skill can't be applied cleanly (missing files, unclear instructions), state the issue, pick the next-best approach, and continue.
