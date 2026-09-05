# TrainIQ Architecture Decisions

> Release scope update (2026-09-06): [itch.io release policy](../TrainIQ-Project/docs/release/itch-release-policy.md) supersedes the owner-approval and mandatory certification release gates below. LEGAL-001, PERF-001, A11Y-001, and AI-001 are retired for this personal itch.io project. Older BLOCKED/OPEN statements are historical or refer to optional certification/future Play submission, not current itch.io delivery. Preserve actual test results and technical findings; do not claim missing evidence passed.

Updated date: 2026-05-10

This file records target-state architecture decisions and open decision gates. It is not a substitute for implementation evidence.

## ADR-001: Local-First Room Authority

- status: accepted, partially implemented
- decision: Room is the primary source of truth for app-owned data. Legacy JSON remains for import/export/backup compatibility, not normal runtime mutation authority.
- rationale: Local-first behavior supports offline use, process restart, privacy control, and deterministic testing.
- consequences:
  - Normal user mutations need targeted DAO transactions or explicit full-replacement semantics.
  - Delete/discard paths require process-restart tests.
  - Migration tests must cover dirty legacy data and foreign-key checks.
- evidence:
  - `TrainIQ_Target_State_Blueprint.md`
  - `docs/TrainIQ_QA_Findings_To_Improve.md`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/core/database`
  - `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository`

## ADR-002: Health Connect Consent and Sync

- status: accepted, runtime evidence incomplete
- decision: Health Connect is optional and partial-permission capable. The app must support granted metrics without treating denied metrics as zero or clearing unrelated caches.
- rationale: Official Health Connect guidance emphasizes user control, transparency, and permission management from app settings.
- consequences:
  - UX copy must distinguish full, partial, denied, stale, failed, and unavailable states.
  - Sync state and changes tokens are per metric/record type.
  - Background reads require explicit feature availability, permission, user value, and release justification.
- sources:
  - Android Developers, Health Connect UI guidelines, accessed 2026-05-10: https://developer.android.com/health-and-fitness/guides/health-connect/design/permissions-and-data
  - Android Developers, Health Connect permissions and data access, accessed 2026-05-10: https://developer.android.com/health-and-fitness/guides/health-connect/design/permissions
  - Android Developers, Health Connect synchronize data, accessed 2026-05-10: https://developer.android.com/health-and-fitness/health-connect/sync-data

## ADR-003: Bounded AI With Local Fallback

- status: accepted, production boundary undecided
- decision: AI features use structured JSON outputs, feature-specific thinking budgets, bounded timeouts, retry/backoff, cancellation propagation, user-safe errors, and deterministic fallback where possible.
- rationale: AI should enhance coaching without becoming the only way to save, inspect, or act on user data.
- consequences:
  - Production AI mode remains a decision gate: BYOK/direct-client, backend gateway, OAuth-mediated access, hybrid, or AI scoped out.
  - User-facing copy and Data Safety docs must match the selected mode.
  - AI output must not expose chain-of-thought or unsupported medical claims.
- sources:
  - Google AI for Developers, Gemini structured output, accessed 2026-05-10: https://ai.google.dev/gemini-api/docs/structured-output
  - Google AI for Developers, Gemini thinking, accessed 2026-05-10: https://ai.google.dev/gemini-api/docs/thinking
  - Google Cloud, API key best practices, accessed 2026-05-10: https://cloud.google.com/docs/authentication/api-keys-best-practices

## ADR-004: Adaptive Material 3 UX

- status: accepted, evidence incomplete
- decision: TrainIQ uses Material 3 and adaptive navigation/layout behavior rather than a custom visual framework.
- rationale: Android quality guidance favors standard platform patterns, adaptive layouts, accessibility, and predictable navigation.
- consequences:
  - Compact, medium, expanded, foldable, large-font, dark-mode, dynamic-color, gesture-nav, and 3-button-nav states require QA evidence.
  - Dense dialogs and sheets need sticky/reachable actions.
  - Custom controls and charts need semantics and touch-target evidence.
- sources:
  - Android Developers, Core app quality guidelines, accessed 2026-05-10: https://developer.android.com/docs/quality-guidelines/core-app-quality
  - Android Developers, Build adaptive navigation, accessed 2026-05-10: https://developer.android.com/develop/ui/compose/layouts/adaptive/build-adaptive-navigation
  - Android Developers, Use window size classes, accessed 2026-05-10: https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes
  - W3C, WCAG 2.2, accessed 2026-05-10: https://www.w3.org/TR/WCAG22/

## ADR-005: Release Readiness Is Gated

- status: accepted, blocked
- decision: Release readiness requires more than passing debug builds. Accessibility signoff, Health Connect runtime evidence, physical-device performance evidence, release owner decisions, migration-marker policy, signing/versioning, and privacy/Data Safety parity are gates.
- rationale: Health data, AI, and background permissions create user trust and release-policy risk.
- consequences:
  - CI should enforce what it can and docs must record owner decisions for what CI cannot prove.
  - Profileable/release physical-device data is required before performance claims.
  - Minified/profileable or release-like smoke should run before upload.
- sources:
  - Android Developers, Core app quality guidelines, accessed 2026-05-10: https://developer.android.com/docs/quality-guidelines/core-app-quality
  - Android Developers, Design for Safety, accessed 2026-05-10: https://developer.android.com/quality/privacy-and-security
  - OWASP, Mobile Application Security Verification Standard, accessed 2026-05-10: https://mas.owasp.org/MASVS/
