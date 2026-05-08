# Human Assistive-Tech QA Signoff

Last updated: 2026-05-08

Status: blank signoff template. Do not mark complete without human QA evidence.

## Tester Evidence

- Tester:
- Role:
- Date:
- Device model:
- Android version:
- App build identifier:
- Theme:
- Font scale:
- TalkBack version/settings:
- Switch Access configuration:
- Screen recording folder:
- Screenshot folder:

## Flow Results

| Flow | TalkBack result | Switch Access result | Evidence link/path | Issues filed |
|---|---|---|---|---|
| Home first run/missing data | NOT_RUN | NOT_RUN | | |
| Bottom navigation | NOT_RUN | NOT_RUN | | |
| Active workout | NOT_RUN | NOT_RUN | | |
| Workout finish confirmation | NOT_RUN | NOT_RUN | | |
| Nutrition scanner permission/result/fallback | NOT_RUN | NOT_RUN | | |
| Nutrition forms | NOT_RUN | NOT_RUN | | |
| Health Connect rationale and settings | NOT_RUN | NOT_RUN | | |
| AI routine generation and preview | NOT_RUN | NOT_RUN | | |
| Settings privacy/AI/destructive actions | NOT_RUN | NOT_RUN | | |
| Progress measurements | NOT_RUN | NOT_RUN | | |

## Signoff

- [ ] All critical TalkBack flows passed or have approved release exceptions.
- [ ] All critical Switch Access flows passed or have approved release exceptions.
- [ ] Large font pass completed.
- [ ] Dark mode pass completed.
- [ ] Evidence archived.
- [ ] Accessibility owner approved release.

Accessibility owner:

Date:

Notes:

Status: `OPEN | IN_REVIEW | APPROVED | BLOCKED`

## Closure Control

Owner role:

- Accessibility owner
- Manual QA tester
- Release owner

Decision required:

- Confirm whether TalkBack, Switch Access, large font, and dark mode flows pass for release.

Allowed options:

- Approve accessibility QA for release.
- Approve with documented exceptions.
- Block release and file issues for failed flows.

Required evidence:

- Completed result table above.
- Screen recordings or screenshots.
- Tester notes.
- Device model, Android version, app build identifier, theme, font scale, and assistive tech settings.

Exact completion criteria:

- Every critical flow is `PASS` or has an approved exception.
- Evidence paths are filled.
- Issues are filed for every failure.
- `A11Y-001` in `docs/release/owner-action-tracker.md` is `APPROVED`.

Downstream docs that must be updated:

- `docs/qa/accessibility-certification-boundary.md`
- `docs/release/final-release-risk-register.md`
- `docs/release/owner-action-tracker.md`

Release impact if not completed:

- Accessibility certification remains blocked.
- Release status remains `BLOCKED`.
