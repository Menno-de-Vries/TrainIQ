# TrainIQ Owner-Approved Defer Template - 2026-05-27

Use this template only when a `NOT RUN` row is intentionally deferred instead of tested before release.

Do not mark a `NOT RUN` row as owner-approved without filling every field below.

## Defer record

- QA row / gate:
- Owner:
- Date:
- Decision: `DEFER`
- Release impact:
- User risk:
- Reason testing is not completed now:
- Mitigation before release:
- Follow-up owner:
- Follow-up due date:
- Evidence reviewed before defer:
- Explicitly accepted residual risk:

## Required owner statement

Owner statement:

```text
I approve deferring this QA gate for the stated release, with the residual risk and follow-up plan documented above.
```

Owner name:

## Allowed defer examples

- Physical-device macrobenchmark cannot run because no approved physical device is available before the current internal build, and release is not performance-signoff gated.
- Real-key privacy signoff is deferred because only mock/fake-key encrypted storage tests are allowed in this QA environment, and release is blocked until security owner signs off separately.
- Health Connect mutation matrix is deferred because the current test profile cannot safely mutate user/device permissions, and an owner-approved safe test profile is scheduled.

## Not allowed

- Deferring because a source/unit test passed when the QA row requires runtime evidence.
- Deferring without owner, date, release impact and residual risk.
- Deferring P0/P1/P2 bugs that are reproducible and unfixed.
- Marking `PASS` when the actual result is `NOT RUN`.
