# Play Console Owner Checklist

Last updated: 2026-05-08

This checklist must be completed by the app owner or release manager with Play Console access.

## Play Console Health Apps Declaration

- [ ] Confirm package name and signing identity.
- [ ] Confirm every Health Connect data type requested in `AndroidManifest.xml`.
- [ ] Confirm user-facing purpose for steps, heart rate, sleep, active calories, weight, and exercise sessions.
- [ ] Confirm background read justification for `READ_HEALTH_DATA_IN_BACKGROUND`.
- [ ] Confirm screenshots or screen recording show rationale before Android permission prompt.
- [ ] Confirm unsupported-provider and denied-permission flows are documented.
- [ ] OWNER_CONFIRMATION_REQUIRED: submit or update Health Apps declaration.

## Data Safety Form

- [ ] Use `docs/release/play-console-data-safety-worksheet.md` as the input worksheet.
- [ ] Confirm whether data is collected, shared, processed ephemerally, or local-only under Play definitions.
- [ ] Confirm telemetry production endpoint and processor before answering diagnostics/performance sharing questions.
- [ ] Confirm Gemini requests and BYOK key handling under third-party sharing definitions.
- [ ] Confirm meal photo handling and retention.
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
- [ ] Archive screenshots, screen recordings, tester notes, Android versions, and device models.
- [ ] Confirm no production secrets are committed.
- [ ] Confirm production signing and Play upload are performed by owner only.

