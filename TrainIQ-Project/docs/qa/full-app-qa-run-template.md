# TrainIQ Full-App QA Run Template

Date:
Tester:
Build variant:
App version/build id:
Commit/build identifier:
Device/emulator:
Android version:
Theme:
Font scale:
Network state:
Health Connect state:
AI provider/key state:

Status values: `PASS`, `FAIL`, `NOT RUN`.

## Short QA Fix Loop

Use this run file as the loop ledger:

1. `QA short pass`: open Start, Training, Voeding, Voortgang, Coach and Meer; open key subsections; test visible screen, primary CTA, one safe save/edit flow, empty/error state, back/navigation and logcat crashcheck.
2. `Findings`: record every reproducible bug below and store screenshots, UI dumps or logcat under `docs/qa/evidence/`.
3. `Small fix batch`: fix only reproducible findings, grouped by area. Avoid broad refactors unless required for the bug.
4. `Targeted verification`: record the smallest proof that the fix works.
5. `Regression pass`: rerun connected smoke and relevant targeted tests; reopen the same flow. New bug means a new finding and another loop.

Definition of done: all tabs/flows are `PASS` or owner-approved `NOT RUN`; no open P0/P1/P2 bugs remain; every fixed bug has repro, expected/actual, evidence, fix, targeted verification and regression result; final regression finds no new P0/P1/P2 issues; baseline Gradle checks pass; logcat has no app crash/ANR; open release gates are explicitly listed.

## Automated Baseline

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| Debug build | `.\gradlew.bat :app:assembleDebug --console=plain` | NOT RUN | |
| JVM unit tests | `.\gradlew.bat :app:testDebugUnitTest --console=plain` | NOT RUN | |
| Lint | `.\gradlew.bat :app:lintDebug --console=plain` | NOT RUN | |
| Connected tests | `.\gradlew.bat :app:connectedDebugAndroidTest --console=plain` | NOT RUN | |
| AI/nutrition targeted | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.ai.services.AiServicesTest" --console=plain` | NOT RUN | |
| Profileable benchmark build | `.\gradlew.bat :app:assembleProfileable :macrobenchmark:assembleAndroidTest --console=plain` | NOT RUN | |
| Macrobenchmark physical device | `.\gradlew.bat :macrobenchmark:connectedProfileableAndroidTest --console=plain` | NOT RUN | |

## Start/Home

| Flow | Status | Evidence/notes |
|---|---|---|
| First run without profile/Health Connect/routine/data | NOT RUN | |
| Dashboard cards render and remain readable | NOT RUN | |
| CTA to profile/settings works | NOT RUN | |
| CTA to Health Connect works | NOT RUN | |
| CTA to Training works | NOT RUN | |
| CTA to Coach works | NOT RUN | |
| Loading/empty/partial/error states are understandable | NOT RUN | |
| Dashboard data survives app restart where expected | NOT RUN | |
| Dark mode, large font, dynamic color, tablet/foldable layout | NOT RUN | |

## Training

| Flow | Status | Evidence/notes |
|---|---|---|
| Routine list opens | NOT RUN | |
| Create routine | NOT RUN | |
| Edit/delete routine | NOT RUN | |
| Generated routine preview save/cancel | NOT RUN | |
| Exercise library and picker | NOT RUN | |
| Exercise history | NOT RUN | |
| Start active workout | NOT RUN | |
| Add/edit/delete/undo set | NOT RUN | |
| Weight/reps/RPE/set type/rest timer | NOT RUN | |
| Active workout restore after recreate/restart | NOT RUN | |
| Finish workout and processing route | NOT RUN | |
| Workout completion screen | NOT RUN | |
| AI debrief valid Gemini response | NOT RUN | |
| AI debrief fallback reasons: disabled/missing key/malformed/timeout/rate-limit/offline | NOT RUN | |
| Dense controls: touch targets, no overlap at large font | NOT RUN | |

## Voeding

| Flow | Status | Evidence/notes |
|---|---|---|
| AI meal scanner opens | NOT RUN | |
| Multi-component scan keeps item identity | NOT RUN | |
| Suspicious duplicate AI output shows review warning | NOT RUN | |
| Manual food add/edit/delete | NOT RUN | |
| Products list and quick add | NOT RUN | |
| Recipes add/edit/delete/use in meal | NOT RUN | |
| Meal logging save/reopen/restart | NOT RUN | |
| Meal history and reuse meal | NOT RUN | |
| Barcode scanner and lookup success/fail | NOT RUN | |
| Camera denied/no camera/manual fallback | NOT RUN | |
| Missing AI key/invalid AI response/local fallback | NOT RUN | |
| Long forms, keyboard/IME, dark mode, large font | NOT RUN | |
| Historical meal snapshots do not silently change after product/recipe edits | NOT RUN | |

## Voortgang

| Flow | Status | Evidence/notes |
|---|---|---|
| Add body measurement | NOT RUN | |
| Edit/delete measurement | NOT RUN | |
| Invalid values show clear validation | NOT RUN | |
| Save/reopen/restart measurement integrity | NOT RUN | |
| Smart-scale scanner valid result | NOT RUN | |
| Smart-scale scanner partial/no result/manual fallback | NOT RUN | |
| Charts render and expose accessibility summary | NOT RUN | |
| Empty states, dark mode, large font | NOT RUN | |

## Coach

| Flow | Status | Evidence/notes |
|---|---|---|
| Goal advice form and result | NOT RUN | |
| Weekly report | NOT RUN | |
| Training insights | NOT RUN | |
| Nutrition coach message | NOT RUN | |
| AI enabled valid JSON | NOT RUN | |
| AI disabled/missing key/invalid/English/timeout/rate-limit/offline | NOT RUN | |
| Profile/calorie baseline is not overwritten by AI | NOT RUN | |
| Source labels, bullets, loading/error/fallback clarity | NOT RUN | |
| Deep-mode thinking budget and JSON schema contract | NOT RUN | |

## Meer/Instellingen

| Flow | Status | Evidence/notes |
|---|---|---|
| Theme mode changes | NOT RUN | |
| Telemetry opt-in/out | NOT RUN | |
| Gemini API key save/delete | NOT RUN | |
| OpenAI API key save/delete | NOT RUN | |
| Provider preference | NOT RUN | |
| Health Connect status refresh | NOT RUN | |
| Health Connect rationale | NOT RUN | |
| Health Connect settings/install/update links | NOT RUN | |
| Local data clear confirmation and effect | NOT RUN | |
| Destructive dialogs safe and accessible | NOT RUN | |
| Secrets absent from logs, URLs, screenshots, BuildConfig production values | NOT RUN | |

## Cross-Tab Runtime

| Flow | Status | Evidence/notes |
|---|---|---|
| Tab switching Start -> Training -> Voeding -> Voortgang -> Coach -> Meer | NOT RUN | |
| Back stack behavior | NOT RUN | |
| Scanner return values through savedStateHandle | NOT RUN | |
| Camera permission denied/granted | NOT RUN | |
| Health Connect no permission | NOT RUN | |
| Health Connect partial/revoke/background-read on safe profile | NOT RUN | |
| Offline/slow network for AI and barcode | NOT RUN | |
| App background/foreground, lock/unlock | NOT RUN | |
| Rotation/recreate on high-risk screens | NOT RUN | |
| Logcat crash/ANR slice after smoke | NOT RUN | |

## Accessibility And Design

| Flow | Status | Evidence/notes |
|---|---|---|
| TalkBack high-risk flows | NOT RUN | |
| Switch Access high-risk flows | NOT RUN | |
| Font scale 1.3 and 1.5 | NOT RUN | |
| Dark mode and dynamic color | NOT RUN | |
| Touch targets | NOT RUN | |
| Content descriptions and focus order | NOT RUN | |
| Text overlap/clipping check | NOT RUN | |
| Modal/dialog focus containment | NOT RUN | |

## Findings

Add findings below using the schema from `docs/qa/full-app-qa-basis.md`.

## Finding QA-YYYY-MM-DD-###

- priority:
- area:
- tab/flow:
- status:
- current evidence:
- expected behavior:
- actual behavior:
- repro steps:
- recommended fix:
- regression risk:
- minimal verification:
- owner suggestion:

## Final QA Decision

Overall status: `PASS | FAIL | BLOCKED | PARTIAL`

Highest-risk open issues:

Release gates still open:

Next safest action:

Definition of done audit:

- All tabs/flows `PASS` or owner-approved `NOT RUN`:
- No open P0/P1/P2 bugs:
- Every fixed bug has repro, expected/actual, evidence, fix, targeted verification and regression result:
- Final full regression found no new P0/P1/P2 issues:
- `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `connectedDebugAndroidTest` passed:
- Logcat contains no app crash/ANR after smoke and high-risk flows:
- Open release gates explicitly listed:
