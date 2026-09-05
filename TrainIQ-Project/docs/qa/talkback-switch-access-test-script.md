# TalkBack and Switch Access Test Script

> Release scope update (2026-09-06): [itch.io release policy](../release/itch-release-policy.md) supersedes the owner-approval and mandatory certification release gates below. LEGAL-001, PERF-001, A11Y-001, and AI-001 are retired for this personal itch.io project. Older BLOCKED/OPEN statements are historical or refer to optional certification/future Play submission, not current itch.io delivery. Preserve actual test results and technical findings; do not claim missing evidence passed.

Last updated: 2026-05-08

Status: manual test script. Results must be filled by a human tester.

## Evidence Header

- Tester:
- Date:
- Device model:
- Android version:
- App version/build:
- Theme: light/dark
- Font scale:
- Input mode: TalkBack / Switch Access
- Screen recording path:
- Screenshots path:

## TalkBack Flow

| Screen/flow | Steps | Pass/fail criteria | Result | Notes/evidence |
|---|---|---|---|---|
| Home first run | Launch app, swipe through all elements | Header, setup CTA, Health Connect CTA, and nav items are labeled and ordered | NOT_RUN | Manual required |
| Bottom navigation | Move across Start, Training, Voeding, Coach, Meer | Each destination is labeled; selected state is understandable | NOT_RUN | Manual required |
| Training list | Open Training, navigate routine controls | Create routine, AI routine, routine cards, menus are reachable | NOT_RUN | Manual required |
| Active workout | Start/open workout, edit set values, complete set, open set menu | Set controls have labels, values, increment/decrement actions are understandable | NOT_RUN | Manual required |
| Finish workout | Trigger finish confirmation | Dialog title/body/actions are announced, safe default focus | NOT_RUN | Manual required |
| Nutrition Today | Open Nutrition, navigate meals and add buttons | Meal sections and add actions are labeled | NOT_RUN | Manual required |
| Nutrition scanner | Open scan, handle camera permission, capture, result/fallback | Permission denied and missing-key states are understandable | NOT_RUN | Manual required |
| Nutrition forms | Open Recepten and Producten, navigate text fields | Focus order follows form order; IME does not hide fields | NOT_RUN | Manual required |
| Coach | Navigate goal advice form and reports | Inputs, AI buttons, loading/error/fallback states are understandable | NOT_RUN | Manual required |
| Progress | Add/edit/delete measurement | Text fields, save, delete confirmation, and errors are labeled | NOT_RUN | Manual required |
| Health Connect rationale | Open from Settings/Home, continue to system prompt/settings | Rationale is clear before system permission UI | NOT_RUN | Manual required |
| Settings AI/privacy | Toggle telemetry, AI, save/delete key | Disclosure text, masked key field, destructive confirmation are clear | NOT_RUN | Manual required |
| Settings local data deletion | Trigger clear data dialog without confirming in production data | Consequences are read and confirmation/cancel are reachable | NOT_RUN | Manual required |

## Switch Access Flow

| Screen/flow | Steps | Pass/fail criteria | Result | Notes/evidence |
|---|---|---|---|---|
| Top-level navigation | Scan through all bottom nav destinations | Each destination can be selected without trapping focus | NOT_RUN | Manual required |
| Long forms | Scan through Coach/Profile/Nutrition forms | Fields and actions reachable in order; no impossible scroll trap | NOT_RUN | Manual required |
| Active workout controls | Scan set controls, menus, finish action | Increment/decrement/edit/delete reachable; no accidental destructive action | NOT_RUN | Manual required |
| Modal sheets/dialogs | Open routine preview, nutrition sheet, delete confirmations | Focus remains inside modal; dismiss and primary action reachable | NOT_RUN | Manual required |
| Permission flows | Camera and Health Connect permission routes | Denied and settings actions reachable | NOT_RUN | Manual required |

## Final Signoff

- [ ] TalkBack pass completed.
- [ ] Switch Access pass completed.
- [ ] Large font pass completed.
- [ ] Dark theme pass completed.
- [ ] Evidence archived.
- [ ] Issues filed for every failure.

## Owner Handoff Control

Status: `OPEN`

Owner role: accessibility owner + manual QA tester

Decision required: execute this script and decide whether each flow passes, fails, or has an approved exception.

Allowed options:

- Pass all flows and approve `docs/qa/human-assistive-tech-qa-signoff.md`.
- Fail one or more flows and block release.
- Approve documented exceptions with release-owner acceptance.

Required evidence:

- Tester notes for each row.
- Screen recording or screenshots for critical flows.
- Device model, Android version, app build identifier, theme, font scale, TalkBack settings, and Switch Access settings.

Exact completion criteria:

- `docs/qa/human-assistive-tech-qa-signoff.md` is completed and signed.
- `A11Y-001` in `docs/release/owner-action-tracker.md` is `APPROVED`.

Release impact if not completed: release remains `BLOCKED`; do not claim accessibility certification.
