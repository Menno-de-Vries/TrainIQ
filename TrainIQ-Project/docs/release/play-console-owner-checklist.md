# Play Console Owner Checklist

> Release scope update (2026-09-06): [itch.io release policy](itch-release-policy.md) governs current delivery. The formal owner gates below are retired for itch.io; Play submission checklists are future reference material. Preserve accurate privacy and security descriptions.

Last updated: 2026-05-12

This checklist must be completed by the app owner or release manager with Play Console access.

Release status: `BLOCKED`

Use this checklist with the current owner handoff packet: `docs/release/owner-decision-packet-2026-05-10.md` (content refreshed on 2026-05-12). Do not submit Play Console declarations until the matching rows in `docs/release/owner-action-tracker.md` are approved with evidence.

## Play Console Health Apps Declaration

- [ ] Confirm package name and production signing identity.
- [ ] Confirm every Health Connect data type requested in `AndroidManifest.xml` and the final release scope.
- [ ] Confirm user-facing purpose for steps, heart rate, sleep, active calories, weight, and exercise sessions.
- [ ] Confirm whether `READ_HEALTH_DATA_IN_BACKGROUND` remains in production scope and document its user value and Play declaration justification.
- [ ] Confirm screenshots or screen recording show rationale before Android permission prompt.
- [ ] Confirm unsupported-provider and denied-permission flows are documented.
- [ ] Confirm Health Connect edge-state evidence is complete for provider missing/update, partial grant, revoke while open, and background-read available/unavailable, or document a dated release exception.
- [ ] OWNER_CONFIRMATION_REQUIRED: submit or update Health Apps declaration.

## Data Safety Form

- [ ] Use `docs/release/play-console-data-safety-worksheet.md` as the input worksheet.
- [ ] Confirm whether data is collected, shared, processed ephemerally, or local-only under Play definitions.
- [ ] Confirm telemetry production endpoint and processor before answering diagnostics/performance sharing questions.
- [ ] Confirm final production AI mode, Gemini request handling, and BYOK/gateway/account behavior under third-party sharing definitions.
- [ ] Confirm meal photo/barcode handling, retention, and whether images leave the device.
- [ ] Recheck answers after any telemetry, backend, analytics, crash reporting, account/auth, or AI-mode change.
- [ ] OWNER_CONFIRMATION_REQUIRED: submit Data Safety form.

## Privacy Policy

- [ ] Legal owner reviews `docs/release/privacy-policy-draft.md`.
- [ ] Add publisher name, contact, effective date, jurisdiction, and policy URL.
- [ ] Confirm Health Connect wording matches Play declaration.
- [ ] Confirm Gemini/BYOK wording matches production AI architecture.
- [ ] Confirm telemetry wording matches production build config.
- [ ] OWNER_CONFIRMATION_REQUIRED: publish policy URL and add it to Play Console.

## Release Readiness

- [ ] Run release/profileable build and macrobenchmark plan on physical devices.
- [ ] Complete TalkBack and Switch Access scripts under `docs/qa/`.
- [ ] Complete real camera/barcode and AI photo capture evidence on an approved safe camera test setup if scanner release scope includes those flows.
- [ ] Archive screenshots, screen recordings, tester notes, Android versions, and device models.
- [ ] Confirm performance thresholds and approved device matrix are recorded before certifying performance.
- [ ] Confirm no production secrets are committed.
- [ ] Confirm production signing and Play upload are performed by owner only.
- [ ] Confirm `docs/release/final-release-risk-register.md`, `docs/release/owner-action-tracker.md`, and `docs/TrainIQ_App_Ready_Completion_Audit.md` match the final release decision.
