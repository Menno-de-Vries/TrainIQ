# Finding Discovery Report

Scope: D:\GitHub\TrainIQ\TrainIQ-Project\app.

Worklist: artifacts/02_discovery/rank_input.csv and deep_review_input.csv contain 123 app-scope source-like rows.

Reportable candidates discovered:
- CAND-001: Legacy Gemini plaintext key may remain after explicit encrypted key save/clear paths.
- CAND-002: Health Connect READ_WEIGHT overcollects health data outside the declared TrainIQ Health Connect metric standard.
- CAND-003: Reminder notification body text can expose meal/workout behavior on notification surfaces.

Suppressed candidates:
- Exported Health Connect rationale/onboarding components: rationale-only UI, no sensitive extras/data read on launch; aliases are platform/provider permission protected where required.
- Background Health Connect sync abuse: WorkManager-internal path; scheduling is gated by feature and permission, Health Connect enforces reads.
- Import JSON DoS: user-facing read cap and import limits; lower-level parser has no uncapped external path.
- Import data loss: destructive restore runs inside Room transaction after preview validation.
- SQL injection: Room bound queries and hardcoded migration SQL only.
- AI key outbound/logging: keys are headers, logging level NONE, no hardcoded secrets found.
- Telemetry export: disabled by default, opt-in required, allowed events/attributes only, token-like values redacted.
