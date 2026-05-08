# Accessibility Certification Boundary

Last updated: 2026-05-08

Status: certification boundary. Accessibility certification is blocked until human assistive-technology QA is completed.

## What Local Automation/Inspection Can Check

- Presence of Compose semantics/content descriptions in source.
- Use of Material components with built-in accessibility semantics.
- Basic emulator UI dumps for visible text and focusable controls.
- Build/lint/test regressions.
- Text input IME behavior via emulator smoke.

## What Requires Human Testing

- TalkBack spoken labels and hints.
- TalkBack focus order and rotor/action behavior.
- Switch Access reachability and scan order.
- Large font readability and actual overlap perception.
- Dark mode contrast perception.
- System permission UI behavior for camera and Health Connect.
- Long modal sheet behavior with screen readers.
- Destructive-action comprehension.

## Required Critical Flows

| Flow | Pass/fail criteria | Evidence required |
|---|---|---|
| Home first run/missing data | All CTAs and status cards are announced in logical order | Screen recording, tester notes |
| Bottom navigation | Every destination announced with selected state | Screen recording |
| Active workout | Set controls, menus, timers, haptics/sound controls, and finish action are reachable | Screen recording, tester notes |
| Nutrition scanner | Camera permission, capture, failure, and result/fallback states are understandable | Screen recording, screenshots |
| Nutrition forms | Recipe/product/meal fields navigate in logical order and remain visible | Tester notes, screenshots |
| Health Connect rationale | Rationale appears before system permission prompt; settings links reachable | Screen recording |
| AI routine generation | Prompt inputs, loading/error/fallback, preview sheet, save/cancel reachable | Screen recording |
| Settings destructive actions | Key deletion, profile reset, local data clear dialogs are clear and cancelable | Screen recording |
| Progress measurements | Input, save, delete, and validation states are understandable | Tester notes |

## Evidence Header Required For Signoff

- Tester name.
- Date.
- Device model.
- Android version.
- App build identifier.
- Theme.
- Font scale.
- Assistive tech mode: TalkBack or Switch Access.
- Screen recording path.
- Screenshot path.
- Pass/fail notes.

## Certification Statement

Accessibility certification is `NOT_COMPLETE` until a human tester completes `docs/qa/talkback-switch-access-test-script.md` and archives the evidence listed above.

