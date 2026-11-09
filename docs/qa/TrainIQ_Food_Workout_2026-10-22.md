# Food analysis and active workout review — 2026-10-22

Base: `2278301778d0f714691e6e1a93cc84cac4949f0f`, primary checkout,
`codex/active-workout-rpe-food`. The user requested analysis first and subsequently
authorized evidence-backed improvements even when their proposed cause was wrong.
Two independent agents investigated food analysis and workout UX; local integration
and verification are coordinated sequentially in the primary checkout.

## Findings and scope

- **Confirmed context defects:** the local quantity override parser rejects bullet
  and numbered list prefixes. Its name-keyed map overwrites repeated identical food
  names. The original context still reaches the AI prompt, so these defects explain
  ignored/wrong weights but do not establish the cause of the reported total failure.
  Preserve explicit per-item quantities and repeated entries without counting model
  predictions twice. Keep ambiguous names editable rather than guessing identity.
- **Confirmed image boundary:** both gallery copy and image preparation reject
  sources above 6 MiB before compression. Separate bounded source acceptance from
  the existing 1280 px / 1,500,000-byte JPEG upload limits. Keep decode sampling and
  actionable unreadable/oversized-photo recovery.
- **Confirmed response gap:** Gemini response mapping drops `finishReason`,
  `promptFeedback` and thought metadata. Blocked, truncated and empty responses
  collapse into generic parsing failures. Classify meal responses at the provider
  boundary, exclude thoughts and record only safe provider/category/status
  diagnostics. Preserve existing non-meal parsers and local coaching fallback.
- **Unproven causes:** network, account access, quota, provider latency and the
  exact response from the user's failing scan. The 20-second meal request budget
  is bounded; no evidence supports increasing it or changing the pinned models.
  Product enrichment already has an independent three-second budget and fallback.
- **Confirmed workout defects:** the finish label disappears during rest; running
  timer controls are duplicated in a list card and persistent bar; planned-rest
  buttons reuse running-timer accessibility labels. Keep one persistent timer,
  explicit `Training afronden` and distinct labels. Preserve timer deadlines,
  state restoration, set correction/deletion/undo, notes and finish persistence.

No schema, permission, dependency, AI model, account or service changes are needed.
AI/image boundary risk is medium; workout layout risk is medium. Shared theme
changes would widen risk without resolving these findings.

## Comparison of supplied concepts

These are comparisons of the user's concept image against repository behavior,
not usability-study results or claims of new rendered mockups.

| Workout | Strength | Tradeoff | Decision |
|---|---|---|---|
| A — Focus | Current exercise and large logging action are easy to identify | Hides workout order and makes superset/context switching harder | Borrow emphasis on current set; retain existing navigation |
| B — Overzicht | Planned/completed sets and exercise order remain visible | Dense rows need deliberate wrapping at large fonts | Use as foundation of the current adaptive list |
| C — Minimalistisch | Strong rest-state hierarchy | Giant timer displaces logging and planned sets on compact screens | Borrow calm hierarchy; use a compact persistent timer |

| Food AI | Strength | Tradeoff | Decision |
|---|---|---|---|
| 1 — Simpel & snel | Direct photo/context-to-result path | Must retain item review, quantities and errors | Preferred entry flow |
| 2 — Meer controle | Editable ingredients and explicit portions | Many controls can obscure the primary save action; sliders are imprecise for exact grams | Borrow editable ingredient review and precise quantity entry |
| 3 — AI-chat | Flexible follow-up corrections | More conversation/state/privacy scope and taps for routine logging | Defer; no chat feature added |
| 4 — Snelle log | Clear input and feedback | Four claimed processing stages would be misleading without real stage signals | Borrow concise cancellable loading, never invent progress |

Keep TrainIQ's existing Material 3 semantic colors, typography, shared cards and
dynamic light/dark themes. The supplied concepts' fixed green/black or white
palettes are visual references, not instructions to bypass the current theme.

## Acceptance and verification

- Context: bullet/numbered lists, decimal grams, individually weighted repeated
  bread slices, mixed food names, aggregated/repeated AI predictions and existing
  serving/raw/cooked/ml behavior.
- Images: camera and gallery share bounded source/upload policy; valid large source
  is sampled/compressed; unreadable and over-limit input recover; cancellation
  propagates without publishing a late result.
- Provider: successful JSON, refusal, incomplete/empty/malformed responses, no
  thought exposure, HTTP failure diagnostics, timeout/retry/cancellation and manual
  fallback remain covered by deterministic fixtures.
- Workout: one running timer group, planned/running semantics, visible 48 dp finish
  action during rest, adjustment/restart/skip, set logging and completion persistence.
- Local gates: focused JVM regression proof, debug build/full JVM suite/lint and
  affected connected tests on one isolated agent AVD, with compact light/dark and
  large-font visual/semantics inspection.

Results are recorded below after execution. Live AI/photo uploads, physical-camera
quality, manual TalkBack/Switch Access and physical-device performance are outside
the local deterministic proof and must not be reported as passed.

### Environment and development evidence

Windows PowerShell, Android Studio JBR (Java 21), local Android SDK, debug variant;
Gradle uses `--console=plain --max-workers=2`. Session-only environment:
`JAVA_HOME=C:/Program Files/Android/Android Studio/jbr`,
`ANDROID_HOME=C:/Users/menno/AppData/Local/Android/Sdk`,
`ANDROID_SERIAL=emulator-5580`. No machine-specific tracked configuration changed.

- Initial focused JVM proof selected `AiServicesTest`, `AiProviderRouterTest`,
  `RestTimerTest` and `WorkoutInputValidationTest`: 190 executed, 7 expected new
  regressions failed before production edits; the remaining 183 passed.
- Separate `GeminiModelClientTest` RED: both original cases failed on missing safe
  categories and thought exclusion. Log: `red-gemini-workout.log`.
- The first connected attempt ran zero tests because the existing
  `TrainIQ_Agent_Nutrition_20260912` app signature differed. Its installed app/data
  were preserved; only the emulator started by this task was stopped.
- Created exactly one new AVD, `TrainIQ_Agent_FoodWorkout_20261022`, from the already
  installed API 36 Google Play x86_64 image. `avdmanager` reported a missing
  `devices.xml`; the AVD was registered and booted to `sys.boot_completed=1`.
  No image downloads, license acceptance, wipe or physical-device mutation.
- First combined food proof: 151/154 focused JVM cases passed. Three existing
  coaching/request cases failed after initially applying strict Gemini validation
  to every feature. The implementation was narrowed to meal scans to preserve
  their original fallback contracts; no test assertion was weakened.
- On the new AVD at 360 × 640 dp, light/font 1.3, both image-preparation tests and
  actual gallery-copy boundary/cleanup passed. The new workout test failed at the
  expected missing visible finish label. Log: `food-green-workout-red.log`.
- Final additional RED: 5 executed, 3 failed on pooled conflicting product identity,
  oversized Gemini category and null response root. Log: `final-food-red.log`.
- First full JVM gate: 1,027 executed, 1 failed on the null-root fixture; the
  synthetic DTO fake's Kotlin non-null check occurred before client parsing.
  Replaced it with stronger real Retrofit/Gson conversion through a synthetic
  OkHttp interceptor. HTTP 200 `null` and HTTP 204 must produce the same safe
  invalid-response category. That transport test also failed before repair.
  Pinned Retrofit 2.11 throws `KotlinNullPointerException` for these bodies; the
  meal provider catches that transport failure without changing the API signature
  or other features' fallback policy. Logs: `final-light-gates.log` and
  `transport-red-ui-green.log`.
- The 20-case connected selection passed 18 and failed two workout cases. One
  exposed an accidentally omitted visible logged-set count, restored in the sticky
  status. The other was a false-positive assertion introduced by this task:
  `hasVisualOverflow` compared a 560 px paragraph allocation to a 294 px Text
  layout despite a complete visible label. Removing redundant text alignment did
  not change that measurement. The test now checks actual line left/right bounds,
  no ellipsis/height overflow, every character, label bounds within the button,
  a 48 dp target and button bounds within the viewport. Independent review accepted
  that replacement as more direct readability proof.
- Relogging after deletion also exposed an IME/platform synchronization gap. The
  test now waits for a displayed button, stable button/list/root geometry and a
  reachable tap center before a real click, retaining the Room persistence proof.
  No keyboard-specific production behavior or global inset policy was changed.
  Both repaired light/font-1.3 workout cases passed (`final-light-workout.log`).
  The earlier failing aggregates remain failures; individual passing results are
  reused only where the affected inputs stayed unchanged.
- First full dark/font-1.5 workout run passed 8/9. Relog's bounded visibility and
  geometry condition failed; a diagnostic-only isolated run subsequently passed,
  so that isolated pass is not presented as a proved production fix. The dark
  capture did show `Overslaan` wrapping onto a separate 48 dp row. Timer buttons
  now use the shared 4 dp compact content padding while retaining 48 dp minimum
  width/height, reducing avoidable wrapping without hiding controls during typing.
  The complete affected workout class is rerun after this layout change.
- That final production-code run passed 8/9 dark workout cases, including the
  previously intermittent relog case. Its full-session case reached resume but
  lacked the restore/feedback/stable-click guard at its last action and did not
  persist the fourth set. The test now clears known restore/Set-3 feedback and
  reuses the displayed/stable/reachable-action helper before the real click.
  Its unchanged four-set/notes/completion Room assertions then passed in the
  targeted dark run and again at expanded width. No production persistence or
  keyboard workaround was introduced for this harness correction.

All logs and captures remain untracked below
`.codex/device-qa/food-workout-20261022/`. The initial connected XML is retained
there under `initial-connected-results/`; generated current results remain under
`TrainIQ-Project/app/build/`. Aggregate failing development runs remain failures,
even when a subset passed.

## Changed surfaces

Production files under `TrainIQ-Project/app/src/main/java/com/trainiq/`:
`ai/services/AiServices.kt`, `ai/services/AiProviders.kt`,
`ai/prompts/AiPrompts.kt`, `data/model/GeminiDtos.kt`,
`features/nutrition/CameraScannerScreen.kt` and `features/workout/WorkoutScreen.kt`.
`RestTimer.kt`, `AppDesign.kt` and `Theme.kt` were inspected and preserved because
their timing and design-system contracts did not require changes.

Regression sources: `AiServicesTest`, `AiProviderRouterTest`,
`GeminiModelClientTest`, `ScannerPhotoImportInstrumentedTest`,
`AiImagePreparationInstrumentedTest`, `ActiveWorkoutSetActionsInstrumentedTest`
and the obsolete duplicate-widget source assertion in `WorkoutInputValidationTest`.
Analysis/status documentation is limited to this report and the existing findings
and target-state progress indexes.

## Final local results

Commands run from `TrainIQ-Project/` with the session environment above:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.features.workout.ActiveWorkoutSetActionsInstrumentedTest' --console=plain --max-workers=2
.\gradlew.bat :app:lintDebug :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.features.workout.ActiveWorkoutSetActionsInstrumentedTest#twoExerciseSessionKeepsNotesTimersAndSetsThroughResumeAndFinish' --console=plain --max-workers=2
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.features.workout.ActiveWorkoutSetActionsInstrumentedTest#runningRestKeepsFinishLabelAndOneTimerControlGroup,com.trainiq.features.workout.ActiveWorkoutSetActionsInstrumentedTest#twoExerciseSessionKeepsNotesTimersAndSetsThroughResumeAndFinish' --console=plain --max-workers=2
```

| Proof | Result | Evidence |
|---|---|---|
| Debug assembly and full JVM suite on final production code | PASS: 1,027 tests, no failures/errors/skips | `final-compact-dark.log`; JVM XML under `app/build/test-results/testDebugUnitTest/` |
| Final lint, including updated Android test harness | PASS: zero errors, 66 warnings | `final-dark-session.log`; `app/build/reports/lint-results-debug.xml` |
| Food/scanner Android contracts and restoration | PASS: all 11 selected food/scanner cases; no relevant production inputs changed afterward | Retained 20-case XML under `light-before-repair-results/`; the aggregate run remains 18/20 FAIL |
| Compact light, 360 × 640 dp / font 1.3 | PASS: 2/2 final finish/readability and correction/delete/relog cases, repeated after the final padding and shared-helper changes | `final-light-confirmation.log`, `final-light-confirmation-results/` |
| Compact dark, 360 × 640 dp / font 1.5 | PASS by scoped reuse: 8/9 final compact cases passed; the remaining full-session case passed after the test's missing resume/feedback/stable-click wait was repaired | `final-compact-dark.log` (aggregate FAIL), `final-dark-session.log` (1/1 PASS), retained XML in `compact-dark-results/` and `final-dark-session-results/` |
| Expanded light, 1,000 × 640 dp / font 1.0 | PASS: 2/2 critical workout cases, no failures/skips | `final-wide.log`, `final-wide-results/` |
| Independent code/test review and scoped whitespace check | PASS, no blocking findings | Three user-requested workers, root integration review; `git diff --check` |

Visual inspection covered compact light logging/IME/completed sets, dark large-font
rest controls and logging with the IME, and expanded inline timer controls plus
the labelled finish action. Captures are synthetic test data.
The final compact rest controls fit one row at font 1.5 while keeping 48 dp targets.
The two-exercise journey proves notes, four sets, timer actions, background and
navigation resume, and transactional completion. No failed aggregate is relabelled
as a passing full suite; passing unaffected cases are reused per the local guide.

NOT RUN: the user's original photo against a live configured AI provider,
physical-camera capture quality, human TalkBack/Switch Access and physical-device
performance. These limits leave the original live incident's exact cause unproved;
they do not invalidate the reproduced parser, image-boundary and response defects.
No schema changed and no release was requested; migration markers and release or
signing gates were outside this change's verification scope.

Completion: the reproduced defects and bounded workout improvements are verified
locally; the original live incident remains unconfirmed. Final snapshots under test
contain only the scoped code/test changes above on base `2278301`; subsequent edits
only finalize this evidence and its indexes. The primary checkout and existing
branch were reused, with no extra task worktree. The created
`TrainIQ_Agent_FoodWorkout_20261022` AVD is retained, stopped at the final compact
light/font-1.3 profile; the earlier existing AVD was also stopped after its signature
conflict. No user installation was removed, and no AVD was wiped or deleted.
The final emulator crash buffer was empty (`final-crash.txt`).

## Primary references

- [Gemini generateContent response contract](https://ai.google.dev/api/generate-content),
  checked 2026-10-22: prompt feedback and candidate finish reasons identify blocked
  and incomplete generation. Preserve the pinned Gemini 2.5 Flash request contract.
- [Android: loading large bitmaps efficiently](https://developer.android.com/topic/performance/graphics/load-bitmap),
  checked 2026-10-22: inspect dimensions before sampled decoding to bound memory.
- [Retrofit 2.11 suspend response handling](https://github.com/square/retrofit/blob/2.11.0/retrofit/src/main/java/retrofit2/HttpServiceMethod.java)
  and [null-body exception](https://github.com/square/retrofit/blob/2.11.0/retrofit/src/main/java/retrofit2/KotlinExtensions.kt),
  checked 2026-10-22: simply declaring a nullable DTO is insufficient for this
  pinned version's suspend-body path; keep the transport contract tested locally.
