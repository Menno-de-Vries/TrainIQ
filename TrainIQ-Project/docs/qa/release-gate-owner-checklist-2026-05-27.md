# TrainIQ Release Gate Owner Checklist - 2026-05-27

Status: `OPEN`

Purpose: close the remaining `PARTIAL` QA status only with evidence that cannot be produced safely in the current emulator-only/no-real-key/no-live-provider run.

Source ledger:
- `docs/qa/full-app-qa-run-2026-05-27.md`
- `docs/qa/evidence/2026-05-27-dod-open-gaps-audit/not-run-snapshot.txt`

## 1. Physical-device macrobenchmark

Owner:

Required environment:
- Real physical Android device, not emulator.
- App and macrobenchmark profileable artifacts build successfully.

Command:
```powershell
.\gradlew.bat :macrobenchmark:connectedProfileableAndroidTest --console=plain
```

Required evidence:
- `adb devices`
- `adb shell getprop ro.kernel.qemu` showing non-emulator target.
- Macrobenchmark result output.
- Perfetto or benchmark report artifact if generated.

Pass criteria:
- Startup and measured benchmark tests pass on physical device.
- No app crash/ANR in post-run logcat.

Owner decision:
- `PASS | DEFER | BLOCK`

## 2. TalkBack and Switch Access traversal

Owner:

Required environment:
- Accessibility service enabled for TalkBack.
- Switch Access enabled or owner-approved equivalent traversal setup.

High-risk flows:
- Start first-run setup CTAs.
- Training routine creation, active workout, set logging, finish.
- Nutrition meal add, recipe use/edit/delete, barcode fallback.
- Progress measurement add/invalid save, smart-scale fallback.
- Coach profile/advice and AI fallback surfaces.
- Settings destructive dialogs, Health Connect rationale, AI key fields.

Required evidence:
- Accessibility settings state before traversal.
- Screenshots or UI dumps from each high-risk flow.
- Logcat after traversal.
- Notes for focus order, labels, traps, modal containment and unreachable controls.

Pass criteria:
- No unreachable primary action.
- No focus trap in dialogs/sheets.
- Critical controls have understandable labels.
- No app crash/ANR.

Owner decision:
- `PASS | DEFER | BLOCK`

## 3. Health Connect runtime matrix

Owner:

Required environment:
- Safe test profile/device where Health Connect permissions may be granted, partially granted, revoked and retested.

Runtime cases:
- No permission/rationale path.
- Partial permission grant.
- Revoke while app is open.
- Background-read availability and denial.
- Provider missing/update-required behavior where available.

Required evidence:
- Permission state before and after each case.
- Settings/Home Health Connect UI dump for each state.
- Logcat after each mutation.
- Notes on whether cached metrics remain clear and non-misleading.

Pass criteria:
- Permission mutations do not crash.
- Partial grants show granted and denied metrics correctly.
- Revoke stops background sync/retry loops.
- Background-read denial does not pretend full sync.

Owner decision:
- `PASS | DEFER | BLOCK`

## 4. Privacy/security real-key signoff

Owner:

Required environment:
- Approved throwaway Gemini/OpenAI keys.
- Safe test device/profile.

Runtime cases:
- Save Gemini key.
- Save OpenAI key.
- Relaunch and verify masked/readback behavior.
- Delete each key.
- Local data clear after real-key save.
- Confirm no key appears in UI dumps, logcat or QA artifacts.

Required evidence:
- Redacted screenshots/UI dumps.
- Logcat scan result.
- Secret-pattern scan after the run.
- Owner note confirming throwaway keys were invalidated or disposed.

Pass criteria:
- Keys are masked in UI.
- Keys persist only in encrypted storage.
- Delete and local data clear remove keys.
- No raw key in logs, screenshots, XML dumps or repo artifacts.

Owner decision:
- `PASS | DEFER | BLOCK`

## 5. Provider/runtime-gated AI and scanner flows

Owner:

Runtime cases:
- Live AI routine generation preview/save/cancel.
- AI meal scanner with representative image/context.
- Coach weekly report/training insight/nutrition message with seeded data or live provider.
- Barcode scan through camera or approved savedStateHandle injection.
- Smart-scale valid result.
- Granted camera path.
- Offline/slow-network simulation for AI and barcode.

Required evidence:
- UI dumps/screenshots before and after each flow.
- Provider/fallback source labels.
- Saved/reopened data checks where relevant.
- Logcat after each flow.

Pass criteria:
- Generated data is reviewable before save.
- AI does not overwrite user profile or calorie baseline silently.
- Scanner return values route to the correct target.
- Offline/slow failures produce clear fallback/error state.
- No app crash/ANR.

Owner decision:
- `PASS | DEFER | BLOCK`

## 6. Manual deep-runtime UX audit

Owner:

Runtime cases:
- Exercise History with seeded empty and populated state.
- Active-workout set edit/delete/undo.
- Manual weight/RPE/set-type edits.
- Nutrition long forms with keyboard/IME.
- Large-font touch-target, overlap and clipping audit.
- Dynamic color visual check on Material You configured device.
- Tablet/foldable visual check.

Required evidence:
- Screenshots/UI dumps per flow.
- Logcat after flow group.
- Notes on touch target, clipping and focus issues.

Pass criteria:
- Primary actions remain visible/reachable.
- Saved edits persist and can be reopened.
- Large font and form input do not hide required actions.
- No app crash/ANR.

Owner decision:
- `PASS | DEFER | BLOCK`

## Final owner signoff

Overall release decision:
- `PASS | DEFER | BLOCK`

Required final evidence:
- Updated `docs/qa/full-app-qa-run-2026-05-27.md`
- Closed or owner-approved `NOT RUN` rows.
- Latest full regression:
```powershell
.\gradlew.bat :app:assembleDebug --console=plain
.\gradlew.bat :app:testDebugUnitTest --console=plain
.\gradlew.bat :app:lintDebug --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
```
