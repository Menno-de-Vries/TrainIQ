# TrainIQ vNext Research

Date accessed: 2026-05-10

## Executive Summary

TrainIQ already has the technical base for a serious local-first Android health coach: Compose Material 3, Hilt, Room, Health Connect, CameraX, Gemini 2.5 Flash, type-safe navigation, AI JSON schemas, WorkManager sync, diagnostics, release docs, and a substantial QA trail.

The vNext opportunity is not another set of disconnected tracker features. The product should converge around a small number of complete journeys:

- Guided onboarding into goals, consent, privacy, AI mode, training availability, and reminders.
- Home as a true next-best-action cockpit with data-quality context.
- Recovery/readiness as a first-class, conservative coaching surface.
- Local-first data reliability with targeted Room writes and process-restart proof.
- Release quality gates that treat accessibility, Health Connect runtime states, performance, privacy, and AI boundary decisions as blockers.

No medical, privacy, security, or compliance claim in this document should be read as certified status. Any such area remains a release-owner decision until signed off.

## Current App Baseline

Current implementation evidence shows:

- Product surfaces: Home, Training, Nutrition, Progress, Coach, Settings, camera scanner, Health Connect rationale, active workout, routine generation, meal analysis, goal advice, weekly report, workout debrief.
- Stack: Kotlin, Compose Material 3, Hilt, Room v12, DataStore, Retrofit/Gson, WorkManager, Health Connect, CameraX, ML Kit barcode, Gemini 2.5 Flash, Navigation Compose typed routes, Macrobenchmark module.
- Strengths: training depth, explicit AI JSON contracts, Keystore-backed Gemini key path, Health Connect per-metric sync state, Room migration tests, release signing readiness checks, accessibility improvements for charts and dense workout controls.
- Open risks: targeted Room persistence migration is incomplete, manual accessibility signoff is blocked, Health Connect runtime matrix is incomplete, physical-device performance evidence is missing, release owner gates are open, AI production boundary is undecided.

Local evidence reviewed:

- `TrainIQ_Target_State_Blueprint.md`
- `docs/TrainIQ_QA_Findings_To_Improve.md`
- `docs/TrainIQ_Target_State_Progress.md`
- `TrainIQ-Project/README.md`
- `TrainIQ-Project/app/build.gradle.kts`
- `.github/workflows/android.yml`
- `TrainIQ-Project/app/src/main/java/com/trainiq/domain/model/DomainModels.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/domain/usecase/UseCases.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/data/repository/TrainIqRepository.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/navigation/TrainIqNav.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/features`

## Sources Reviewed

Official and primary sources:

- Android Developers, Core app quality guidelines, accessed 2026-05-10: https://developer.android.com/docs/quality-guidelines/core-app-quality
- Android Developers, Health Connect UI guidelines, accessed 2026-05-10: https://developer.android.com/health-and-fitness/guides/health-connect/design/permissions-and-data
- Android Developers, Health Connect permissions and data access, accessed 2026-05-10: https://developer.android.com/health-and-fitness/guides/health-connect/design/permissions
- Android Developers, Health Connect get started, accessed 2026-05-10: https://developer.android.com/health-and-fitness/health-connect/get-started
- Android Developers, Health Connect synchronize data, accessed 2026-05-10: https://developer.android.com/health-and-fitness/health-connect/sync-data
- Android Developers, Build adaptive navigation, accessed 2026-05-10: https://developer.android.com/develop/ui/compose/layouts/adaptive/build-adaptive-navigation
- Android Developers, Use window size classes, accessed 2026-05-10: https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes
- Android Developers, Design for Safety, accessed 2026-05-10: https://developer.android.com/quality/privacy-and-security
- OWASP, Mobile Application Security Verification Standard, accessed 2026-05-10: https://mas.owasp.org/MASVS/
- W3C, WCAG 2.2, accessed 2026-05-10: https://www.w3.org/TR/WCAG22/
- Google AI for Developers, Gemini structured output, accessed 2026-05-10: https://ai.google.dev/gemini-api/docs/structured-output
- Google AI for Developers, Gemini thinking, accessed 2026-05-10: https://ai.google.dev/gemini-api/docs/thinking
- Google Cloud, API key best practices, accessed 2026-05-10: https://cloud.google.com/docs/authentication/api-keys-best-practices

Context sources:

- Public competitor/product patterns were considered only generically: onboarding, goals, readiness, weekly summaries, reminders, trends, exports, and consent education are common health/fitness app patterns. No protected competitor design or proprietary feature was copied.

## Product Opportunities

### Onboarding Backbone

TrainIQ needs a first-run flow that captures the minimum useful coaching context:

- Goal: strength, recomposition, fat loss, endurance support, general health, or custom.
- Training context: experience level, available days, equipment, session length, limitations, and preferred units.
- Health data consent: Health Connect value, requested metrics, partial-permission behavior, and manage-access path.
- AI mode: disabled, BYOK/direct-client, or future production gateway mode after owner decision.
- Reminder preferences: workout plan, weekly report, meal logging, progress check-in, Health Connect issue, and recovery prompt.
- Privacy expectations: local-first storage, export/delete controls, telemetry opt-in status, and limits of AI advice.

Acceptance criteria:

- A user can complete onboarding without enabling Health Connect or AI.
- Every skipped capability has a clear later entry point in Settings.
- Onboarding state survives rotation, app switch, and process recreation where feasible.
- The flow has compact and large-font evidence at 360x640 and 360x800.

### Home as Next-Best-Action Cockpit

Home should select one primary action and explain why:

- Train today, recover, log meal, connect Health Connect, review weekly report, complete profile, resume active workout, or inspect trend.
- It must include data-quality labels such as fresh, stale, denied, missing, or estimated.
- It must show fallback action when AI, Health Connect, camera, or network is unavailable.

Acceptance criteria:

- Home never blocks first draw on Health Connect full sync or AI calls.
- Home shows one primary action, one reason, and one fallback.
- Missing or denied health metrics are not displayed as zero unless zero is a measured value.

### Recovery and Readiness

Recovery should become a first-class product model, but stay conservative:

- Inputs can include sleep, steps, active calories, workouts, heart rate where available, soreness/manual check-in if added, and recent training load.
- Output should be a data-quality-aware recommendation, not a diagnosis or clinical claim.
- AI may summarize, but deterministic local rules must provide a safe fallback.

Decision needed:

- Whether to add manual soreness/energy check-ins, and how strongly coaching may alter training plans.

### Progress Narrative and Exports

Progress should move beyond charts into narrative history:

- Weekly/monthly comparisons.
- Strength milestones and estimated PRs.
- Body trend confidence and data-quality labels.
- Recovery/adherence trend.
- User-facing export/import/delete controls.

Acceptance criteria:

- Export scope is explicit before the user starts.
- Export excludes secrets and redacts telemetry identifiers.
- Charts have semantic summaries and contrast evidence in light, dark, and dynamic color modes.

## Design/UX Opportunities

- Keep Material 3 as the baseline; improve hierarchy and adaptive layouts rather than redesigning the visual language.
- Use `NavigationSuiteScaffold` or equivalent adaptive behavior for navigation rail/bar transitions where it fits current dependencies and design.
- Health Connect permission UX must follow consistency, transparency, and clarity: value first, clear data types, direct manage-access path, and explicit partial-permission states.
- Dense workout, nutrition, scanner, generated routine, and progress chart surfaces need large-font evidence, not only source-level checks.
- Manual TalkBack and Switch Access release evidence remains mandatory for critical flows.

Acceptance criteria:

- Critical tap targets are at least 48dp unless a documented platform exception applies.
- Dialog/sheet primary actions remain reachable at 360x640, 360x800, font scale 1.3 and 1.5.
- Dynamic color contrast is verified for text, charts, disabled controls, error text, and accent containers.

## Backend/Data Opportunities

- Finish the migration from full-state JSON mirror mutation paths to targeted DAO transactions.
- Add process-restart tests for every migrated write/delete path.
- Split the large concrete repository/coordinator into smaller concrete services after hot-path persistence is stable.
- Keep Health Connect DataStore cache only while payloads remain small and cleanup/debug requirements are simple; move cache/token metadata to Room when payloads or migration semantics grow.
- Keep AI result contracts typed and schema-backed.

Acceptance criteria:

- Normal app mutations do not call full-state JSON import paths.
- Delete/discard flows cannot resurrect after app restart.
- Migration-marker generation is a CI/release gate or explicitly documented as diagnostic-only.

## Android Quality Opportunities

- Treat release readiness as gated: debug compile/test success is not enough.
- Run Health Connect runtime matrix: provider missing/update, no permissions, partial permissions, revoke while open, background read unavailable/granted.
- Define performance thresholds before tuning: startup, top-level navigation, active workout logging, scanner launch, settings scroll.
- Run profileable/release macrobenchmarks on physical devices.
- Smoke minified/profileable or release-like builds before release.

Acceptance criteria:

- `am start -W` has no timeout on QA emulator unless a dated owner exception explains why.
- Physical-device profileable evidence exists before release performance claims.
- Crash buffer is empty after launch and critical navigation smoke.

## Proposed vNext Target State

TrainIQ vNext should be defined as:

```text
Local-first health data + explicit consent + reliable training/nutrition logs
    -> deterministic readiness and data-quality model
    -> bounded Gemini 2.5 Flash summaries where enabled
    -> one calm next-best action
```

Confirmed target:

- Local-first data authority with Room.
- User-controlled Health Connect, AI, telemetry, export, and delete paths.
- Bounded AI with structured JSON and fallback.
- Material 3 adaptive UI with accessibility evidence.
- Release readiness blocked by owner decisions and device evidence.

Speculative proposals:

- Manual readiness check-ins.
- Barcode product lookup.
- Production AI gateway.
- Cloud backup or account sync.
- Wear OS companion.
- Social sharing.

These remain `Decision needed` until a product owner chooses scope.

## Phased Roadmap

### Now

- Close P0/P1 release and data risks.
- Add onboarding target state and backlog.
- Make Home next-best-action behavior explicit.
- Finish Health Connect partial-permission runtime evidence.
- Continue targeted Room writes one flow at a time.

### Next

- Build goal/adherence model.
- Add recovery/readiness model with conservative local fallback.
- Add opt-in reminders.
- Add progress narrative and export scope.
- Split concrete repositories/services once persistence hot paths are stable.

### Later

- Adaptive plan changes and deload suggestions.
- Barcode product lookup if data-source policy is decided.
- Cloud backup/account sync if privacy/security model is approved.
- Gemini Nano/local multimodal feedback where platform support and product value are clear.
- Wear OS companion if training/rest-timer use cases justify the maintenance cost.

## Rejected Ideas and Why

- Clinical diagnosis, injury treatment, or disease management: rejected because no medical/legal basis is established.
- Always-on background health reads by default: rejected without explicit user value, Play justification, and privacy approval.
- Broad backend/cloud sync before local persistence is stable: rejected because it compounds data correctness and privacy risk.
- Social comparison features: rejected for vNext because they dilute the private coaching promise and add moderation/privacy scope.
- Copying competitor designs: rejected; only generic product patterns are used.

## Open Decisions

- Production AI mode: BYOK/direct-client, backend gateway, OAuth-mediated access, hybrid, or AI scoped out.
- Health Connect background-read permission: keep with signed justification or remove before release.
- Telemetry: disabled local diagnostics only, or explicit opt-in upload with endpoint/auth/retention policy.
- Barcode: manual capture only, or product lookup with source/offline/not-found policy.
- Recovery check-ins: whether to add subjective soreness/energy/readiness inputs.
- Export/import scope: JSON backup, CSV summaries, PDF report, or all of the above.
- Versioning: first Play upload remains `1.0`/`1` or uses an internal/pre-release track scheme.

## Risks

- Scope risk: too many partial features can weaken the core “what should I do next?” promise.
- Trust risk: AI and health coaching need conservative language, data-quality labels, and no unsupported medical claims.
- Data risk: targeted Room writes now cover known repository mutation hot paths, but incomplete per-mutation process-restart coverage can still miss stale-row resurrection cases.
- Accessibility risk: manual assistive-tech signoff is still blocked.
- Release risk: owner gates and physical-device evidence remain required before production claims.
