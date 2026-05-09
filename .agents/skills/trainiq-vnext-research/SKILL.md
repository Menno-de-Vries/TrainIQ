---
name: trainiq-vnext-research
description: Use when researching and improving TrainIQ target state, design direction, backend/data architecture, Android quality, and higher-level feature roadmap.
---

# TrainIQ vNext Research

Use this skill when researching and improving the TrainIQ target state, product/design direction, backend/data architecture, Android quality, and higher-level roadmap.

## Goal

Create a stronger, developer-friendly vNext target state for the TrainIQ Android health app with realistic new features, better architecture clarity, and concrete acceptance criteria, without inventing unsupported medical, privacy, compliance, or product claims.

## Inputs

Research and compare:

- `TrainIQ_Target_State_Blueprint.md`
- Current Android app source
- Existing QA, findings, progress, release, architecture, and roadmap docs
- `AGENTS.md`
- `README` files
- Architecture docs
- CI configuration and tests
- Official docs and reliable product, design, backend, privacy, security, and Android platform research

## Research Scope

Cover these areas:

1. Product and feature opportunities:
   - Current app capabilities
   - Missing high-value health/training flows
   - Onboarding, goals, personalization, insights, reminders, history, progress, recovery, coaching, exports
   - User safety, disclaimers, and privacy expectations
2. Android UX/design:
   - Material 3 patterns
   - Navigation, hierarchy, empty/loading/error states
   - Accessibility, TalkBack, font scaling, dark mode, contrast, touch targets
   - Charts, forms, permission flows, and health-data consent
3. Backend/data architecture:
   - Domain model
   - Repositories and use cases
   - Local-first/offline behavior
   - Sync boundaries
   - Health-data storage and privacy
   - Error handling
   - Migrations
   - Testability
4. Android platform quality:
   - Lifecycle, process death, and configuration changes
   - Background work
   - Permissions
   - Health Connect or relevant health APIs
   - Performance, battery, memory, startup
   - Release readiness
5. Competitive/context research:
   - Public patterns from comparable fitness, health, and training apps
   - Do not copy protected designs or proprietary features
   - Extract generic product patterns only

## Source Quality Rules

Use autonomous webresearch whenever an issue, fix, architecture choice, API behavior, dependency behavior, Android behavior, Health Connect behavior, privacy/security risk, design/accessibility pattern, or implementation path is unclear.

Rules:

- Use official or primary sources first:
  - Android docs
  - Jetpack docs
  - Health Connect docs
  - Material Design docs
  - WCAG/accessibility guidance
  - OWASP Mobile security guidance where relevant
  - Backend/framework docs used by this repo
- Use reputable secondary sources for product and design context.
- Use competitor/public app information only for generic pattern inspiration.
- Cite each important external source in the research doc with title, source, URL, and date accessed.
- Mark uncertain claims as assumptions or `Decision needed`.
- Do not invent sources, metrics, medical recommendations, clinical claims, privacy guarantees, or compliance status.

## Allowed Changes

- Update `TrainIQ_Target_State_Blueprint.md` only if changes are clearly supported and improve developer execution.
- Create or update:
  - `docs/TrainIQ_vNext_Research.md`
  - `docs/TrainIQ_Target_State_Backlog.md`
  - `docs/TrainIQ_Architecture_Decisions.md` if useful
- Create or update this repo-local skill and required fallback skills.
- Do not modify app code.

## Forbidden Actions

- Do not change app code.
- Do not push, publish, upload, delete broadly, rotate secrets, change signing credentials, or perform destructive external actions.
- Do not invent sources, citations, metrics, medical claims, privacy claims, security claims, compliance status, or product requirements.
- Do not ask the user except for credentials, paid accounts, destructive actions, legal/privacy risk, or major product choices.

## Blueprint Improvement Rules

Make target-state content developer-friendly:

- Modules and components
- Data contracts
- Screen and flow acceptance criteria
- State handling
- Backend/local data responsibilities
- Privacy/security expectations
- Analytics/logging boundaries where relevant
- Testing strategy
- Release criteria

Add new features only as phased proposals:

- `Now` | `Next` | `Later`
- User value
- Implementation notes
- Backend/data impact
- Privacy/security impact
- Regression risk
- Verification

Separate confirmed target state from speculative ideas. Mark large product choices as `Decision needed`.

## Backlog Schema

Every backlog item must include:

- `item_id`
- `title`
- `type`: `feature` | `design` | `backend` | `data` | `privacy` | `security` | `testing` | `release` | `performance`
- `phase`: `Now` | `Next` | `Later`
- `priority`: `P0` | `P1` | `P2` | `P3`
- `user value`
- `dev notes`
- `affected modules/files`
- `acceptance criteria`
- `verification`
- `risks`
- `dependencies`
- `decision needed`: `yes` | `no`

## Research Doc Schema

Use this structure:

- Executive summary
- Current app baseline
- Sources reviewed
- Product opportunities
- Design/UX opportunities
- Backend/data opportunities
- Android quality opportunities
- Proposed vNext target state
- Phased roadmap
- Rejected ideas and why
- Open decisions
- Risks

## Parallel Research Workstreams

Use read-only subagents when useful and permitted:

1. Product strategy:
   - Feature opportunities, user journeys, prioritization, scope risks.
2. Design/UX:
   - Visual system, navigation, flows, states, accessibility, polish opportunities.
3. Backend/data architecture:
   - Domain model, storage, sync, APIs, privacy/security, testability.
4. Android platform/release:
   - Lifecycle, permissions, Health Connect, performance, testing, release quality.
5. Feasibility/risk:
   - Effort, dependency risk, regression risk, phased implementation plan.

Worker handoff format:

- task:
- scope:
- status: done | blocked | risky
- findings:
- sources:
- recommendations:
- risks:
- handoff_needed:

## Verification

Before final output, verify:

- Skill file has valid frontmatter and is discoverable in `.agents/skills`.
- Every blueprint/backlog requirement is clear, specific, feasible, and verifiable.
- Research-backed claims cite sources.
- No medical, privacy, security, or compliance claims are invented.
- Proposed features have acceptance criteria and verification.
- App code was not changed.
- Repo tests/build are run only if needed to understand feasibility; if not run, record why.

## Final Output Format

Use this structure:

```markdown
## Result
- Bootstrap/skill status
- research completed
- blueprint updated: yes/no
- docs created/updated
- top vNext recommendations

## Sources
- official/primary sources used
- secondary/context sources used

## Proposed target-state upgrade
- design
- backend/data
- Android quality
- new features

## Dev-friendly roadmap
- Now
- Next
- Later

## Verification
- requirement-quality check
- source-support check
- build/test checks if run

## Files changed
- path: reason

## Risks/open decisions
- concrete only
```

## Stop Conditions

Stop when:

- Research scope is completed.
- Blueprint/backlog is improved.
- No useful source-backed upgrade remains.
- Next step requires a product, medical, privacy, or legal decision.
- Required source or tooling is unavailable.
