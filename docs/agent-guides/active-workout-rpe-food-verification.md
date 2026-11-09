# Active workout, reported effort and product verification

Scope: `codex/active-workout-rpe-food`, based on clean primary checkout `c69e24e` (2026-09-30). Three user-requested workers used `gpt-6.1-sol` / `low` with separate UI, strength AI and nutrition ownership; integration and all local gates ran sequentially in the primary checkout. No dependencies, AI providers, permissions or remote configurations changed. No push, PR, release or deployment.

## Decisions and behavior

- Active workout keeps TrainIQ's Material components and typed navigation. Session time is prominent beside the workout name; compact completed/planned rows replace repeated metric cards. Current/correcting input keeps labelled reps, weight and rest fields with IME traversal. Previous/target information sits beside active input and remains visible when an exercise is complete. Expand/collapse, set type, correction/delete/copy/relog, supersets, undo, replacement, discard and finish remain available. Direct add-set and 48 dp rest controls reduce detours. The persistent finish control retains its function and uses a visible label when idle.
- Workout notes use a saveable editing draft with save/cancel and save-before-back/finish. Failure keeps the draft. Targeted Room writes persist up to 2,000 characters; set logging/resume preserve notes, and finish transfers them transactionally to history. Notes do not enter AI requests. Room v20 uses `AutoMigration(19, 20)` to add empty-default notes to active/completed sessions. Export/import normalizes legacy absent fields; no existing effort fields are removed.
- Reported RPE is finite 1–10; zero remains the existing unknown sentinel. `WorkoutEffortSummary` centralizes the existing unweighted arithmetic mean over all completed sets with valid reported RPE. Incomplete and missing/invalid values do not dilute the mean. Every exercise with completed sets has explicit JSON set details and nullable `averageRpe` in the real debrief prompt; session RPE also excludes unknown values. Reported RIR is separate evidence, never a fabricated measured RPE. Objective weight/reps and planned effort cannot supply RPE. Existing new-set logging deliberately remains unknown because the product removed RPE input earlier.
- After parsing AI food identity and applying user amount overrides, one distinct product per meal is verified briefly. Duplicate entries reuse results and scale independently. Valid barcode uses existing provider-mode routing; brand/name/variant search uses Open Food Facts only when allowed by the selected mode. Name lookup requires one exact token match; ambiguity, low-confidence extraction, missing/invalid data, failures and timeouts retain the editable estimate. Success notes carry source/barcode and the Open Food Facts product URL.
- The whole enrichment has a three-second coroutine budget. Each OkHttp call has a 1,200 ms total deadline including response consumption, no redirects or automatic retry, a bounded response, and cancellation closes the call. Barcode automatic routing can attempt the existing secondary gateway. The cache holds at most 32 results/misses/failures for five minutes in memory. Providers receive only product identifiers, never photos, amounts, context, notes or health data. No new gateway is configured.
- Only gram inputs with a compatible non-beverage basis are enriched. Explicit ml, unknown units and Open Food Facts beverage bases retain the estimate: no gram-to-volume density is invented. Database/community values remain editable and the user is asked to check packaging. This is bounded product verification, not broad web research or a guarantee of current manufacturer data.

Primary API reference: [Open Food Facts API documentation](https://openfoodfacts.github.io/openfoodfacts-server/api/), checked for existing product v2 and search support. Pinned provider behavior remains authoritative.

## Local verification

Environment: Windows PowerShell, Android Studio JBR, installed SDK, debug variant. Reused existing agent AVD `TrainIQ_Agent_Nutrition_20260912` on isolated `emulator-5580`, Android 16/API 36, 360 × 640 dp, light/font scale 1.3. Only the emulator started by this task is eligible to be stopped. No download, wipe or physical/user-device mutation.

Commands run from `TrainIQ-Project/` with `JAVA_HOME=C:/Program Files/Android/Android Studio/jbr`, `ANDROID_HOME` set to the installed local SDK, `ANDROID_SERIAL=emulator-5580`, `--console=plain --max-workers=2`:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.package=com.trainiq.features.workout,com.trainiq.features.nutrition,com.trainiq.core.database,com.trainiq.data.repository' :app:generateDebugRoomMigrationChainVerificationMarker --console=plain --max-workers=2
```

Initial checks found and repaired positional-constructor compatibility after adding notes, a stale import version constant, obsolete UI source-contract assertions and an invalid-checksum product fixture. Their assertions were preserved or updated to the new visible layout. New tests cover effort boundaries/actual serialized request, product identity/amount/fallback/ambiguity/cache/privacy, loopback transport timeout/cancellation, legacy note import and notes-preserving mutations. Connected coverage includes a two-exercise session with keyboard input, notes, rest adjustment/skip, background/navigation resume and persisted finish, plus migration/FK checks.

Results (2026-09-30):

- Final `:app:assembleDebug`, `:app:testDebugUnitTest`, and `:app:lintDebug` passed (`final-large-mobile.log`). All 1,013 JVM tests passed, with zero failures/errors. Lint reported zero errors and 66 warnings. Kotlin and Android-test compilation passed.
- The initial affected-package Android run executed 141 tests: 139 passed, two failed. One existing nutrition test tapped Save while a transient product-added snackbar covered it; it now waits for that feedback to disappear before one real tap and still asserts exactly one persisted meal. The new full-workout test needed explicit fixture rest values and a lazy-list scroll after background resume. Follow-up workout/nutrition/migration selection passed 37 of 38; the remaining full-workout case then passed independently after the fixture/scroll repair. Passing unaffected cases were reused rather than rerunning the whole 141-test selection.
- All 15 database migration tests passed, including legacy versions and 19-to-20 note defaults/history preservation. `:app:generateDebugRoomMigrationChainVerificationMarker` passed (`dark-feedback-repair.log`); current marker covers v2-to-v20.
- Small mobile dark/font 1.5 initially passed 21 of 22 selected workout/migration tests. The remaining full-session test tapped during rest-skip feedback; waiting for both outstanding messages repaired it, and the full-session rerun passed. Larger mobile initially passed five of seven workout tests; both note-save cases tapped during restore feedback. Tests now await that message before one real Save tap.
- Final larger mobile (1080 × 2400 px at 420 dpi, approximately 411 × 914 dp, light/font 1.3) passed all seven active-workout tests (`final-large-mobile.log`). The full session asserts saved notes, two exercises, four persisted sets, keyboard traversal, rest adjustment/skip, background/navigation resume and completed history.
- Final small mobile (720 × 1280 px at 320 dpi, 360 × 640 dp, dark/font 1.5) passed all seven active-workout tests (`final-dark-small.log`) after the snackbar layout change. The final crash buffer was empty. Display configuration was restored to the starting light/font 1.3, 720 × 1280/320 dpi profile, and the emulator started by this task was stopped; no AVD was created or deleted.
- Visual inspection covered current compact light/dark inputs, completed rows, rest controls and the larger mobile viewport. Inspection found the undo label squeezing the message at large font sizes; the Material snackbar now places its action on a separate line at font scale >= 1.3. Runtime semantics assert correction/delete, collapse, long-name readability, note recreation, double-tap idempotence, finish confirmation and bottom action accessibility. Temporary feedback remains transient and above the bottom action bar.

Reproduction for the final viewport checks, using the environment above:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.features.workout.ActiveWorkoutSetActionsInstrumentedTest' --console=plain --max-workers=2
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.features.workout.ActiveWorkoutSetActionsInstrumentedTest' --console=plain --max-workers=2
```

Generated evidence stays untracked under `.codex/device-qa/active-workout-rpe-food/` and `TrainIQ-Project/app/build/`. Failed logs are retained alongside repaired runs; no failed aggregate run is represented as passing.

Live billable AI calls, human TalkBack/Switch Access, physical-camera and physical-device performance are not claimed. The actual AI request is inspected at a fake transport boundary with multiple exercises, avoiding account billing and health-data disclosure during tests.
