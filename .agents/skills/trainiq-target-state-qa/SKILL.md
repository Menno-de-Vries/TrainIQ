---
name: trainiq-target-state-qa
description: Use when QA auditing TrainIQ Android app backend, data, design, features, Android quality, tests, privacy, and release readiness against TrainIQ_Target_State_Blueprint.md.
---

# TrainIQ Target State QA

Use for a requested broad blueprint audit. A targeted fix/review should inspect its affected surfaces rather than automatically auditing the full app.

Read AGENTS.md and the local testing guide once, then the relevant current blueprint/findings, source/config and tests. For a full audit cover applicable architecture/data, Android lifecycle/navigation/permissions, UI/accessibility, AI/privacy and release boundaries; do not reload all historical reports. Preserve intent and compare pinned behavior with primary documentation when current guidance matters.

QA-only work changes no app code. Update one existing findings file or one requested review report; change blueprint wording only when needed to clarify supported acceptance. Do not bootstrap other skills or calculate an unsupported global alignment score.

Each actionable finding needs severity, exact owning location, concrete trigger/evidence, impact, proposed repair and cheapest meaningful verification. Label hypotheses and unperformed checks; do not promote an unisolated observation to a proven defect. Use existing IDs where appropriate; no compulsory empty schema fields or duplicated progress narrative.

Select tests using the local testing guide and reuse valid results. Do not run generic test/build/emulator suites merely because the audit skill was invoked; broaden only for the assessed risks or requested full gate. Keep separate runs, source trees and fixture/real-owner evidence distinct. No hosted CI or unknown-device mutation.

When delegation is authorized, use a few non-overlapping read-only workstreams with concise handoffs. Consolidate into one finding list. A final whole-change review is followed by scoped fix review, not repeated discovery rounds. A source review does not claim live AI, physical audio/performance or human accessibility certification.

Remote mutations need explicit applicable user authority; this skill grants none. Do not reinstate retired owner gates. End with outcome, material findings, evidence/limits and file links; stop at the agreed audit scope.
