# Accessibility Manual QA Plan

Last updated: 2026-05-08

Status: manual QA preparation. This does not certify accessibility completion.

## Scope

Required manual flows from `TrainIQ_Target_State_Blueprint.md`:

- Home first-run and missing-data states.
- Active workout and workout finish confirmation.
- Nutrition scanner permission, capture, result, denied-camera, and fallback states.
- Health Connect rationale, missing provider, partial permission, denied permission, and settings links.
- AI routine generation and generated routine preview.
- Settings privacy, telemetry, AI key, Health Connect, destructive local data actions.
- Progress measurement entry and deletion.

## Local Code Evidence Reviewed

- Semantics/content descriptions appear in navigation and dense workout controls: `TrainIqNav.kt`, `WorkoutScreen.kt`.
- IME bring-into-view helpers are used in forms: `bringIntoViewOnFocus`, `imePadding`.
- Destructive actions use confirmation dialogs in Settings and Nutrition.
- Emulator smoke QA artifacts exist under `D:\GitHub\TrainIQ\qa-cycle-runtime\resume-final`.

## Setup

- Device: physical Android phone preferred; emulator acceptable only for rehearsal.
- Android versions: Android 12, 13, 14, 15, and current target emulator/device where available.
- Enable large font: Settings -> Display -> Font size, test at default and at least one large setting.
- Enable dark theme and light theme.
- Enable TalkBack.
- Enable Switch Access with at least one switch method.
- Capture evidence: screenshots, screen recording, tester notes, Android version, device model, app build variant, commit/build identifier.

## Pass Criteria

- Every actionable element has a meaningful spoken label.
- Focus order follows visual/logical order.
- Buttons, toggles, text fields, chips, and destructive actions are reachable without touch exploration only.
- Text remains readable at large font and does not overlap critical controls.
- Error, empty, loading, offline, denied-permission, and partial-data states are announced or visibly clear.
- Keyboard and IME do not move users away from the active field.
- Destructive actions require confirmation and clearly state consequences.

## Known Risks To Watch

- Dense active workout controls can be hard to navigate with TalkBack/Switch Access.
- Nutrition recipe/product forms contain many fields and may need tester confirmation for focus order.
- Generated routine preview is long; verify sticky actions and sheet dismissal with assistive tech.
- Health Connect system UI behavior varies by Android version and provider availability.

## Result Recording

Use `docs/qa/talkback-switch-access-test-script.md` for step-by-step evidence capture and pass/fail notes.

