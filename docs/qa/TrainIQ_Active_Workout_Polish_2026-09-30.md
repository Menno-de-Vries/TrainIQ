# Active Workout polish — 2026-09-30

## Scope and decisions

Baseline: `54a56effb51b87546a8f640345fc93f438272ae3`, primary branch `codex/active-workout-rpe-food`. User requested a local visual refinement of existing Active Workout cards, inputs and sticky controls. A1 audited read-only, A2 implemented, and A3 performs runtime QA sequentially; each worker explicitly uses `gpt-6.1-sol` with `low` effort.

This batch supports blueprint workout UX and the Active Workout portion of QA-2026-05-09-011. The broader routine-generation finding remains partially done. ViewModel, persistence, workout state transitions, routes, permissions and dependencies are unchanged.

- `WorkoutScreen.kt`: existing Material typography, color scheme, shapes and spacing replace oversized/competing emphasis. Current/correction rows use a subtle primary tint and outline; completed rows retain a primary check on a neutral surface; upcoming rows use a quieter surface with the same shape.
- Active metric cells place their supporting label and main value inside one compact container, with a 56 dp minimum and natural growth for larger text. The complete input remains tappable, and filtering, validation, IME traversal and fallback values are preserved. Visual set-type badges shrink independently of their existing 48 dp interaction area.
- Previous/target/plan context uses wrapping secondary text. Exercise titles, set labels and values have distinct visual weight. Completed-row actions share the header when space permits and move below it when required by width/font scale.
- Set logging and finish actions use the existing Material medium shape and a minimum 48 dp height. The bottom surface, padding, status and controls form one composition, retaining navigation-bar insets and adapting its rest status when horizontal space is insufficient.
- `ActiveWorkoutSetActionsInstrumentedTest.kt`: a synthetic first-set fixture checks metric geometry, tap targets, IME focus traversal, persistence of 7 reps / 27.5 kg / 75 s, and finish-action bounds.

## Local verification

`JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`; Android SDK: `%LOCALAPPDATA%\Android\Sdk`. Commands run from `TrainIQ-Project/`, without remote runners or remote build caches.

- PASS: `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain --no-parallel --max-workers=2`.
- JVM report: 1,013 tests, zero failures/errors. Lint: zero errors, 66 existing warnings.
- PASS: final `:app:lintDebug --console=plain --no-parallel --max-workers=2` after the instrumentation-test corrections; zero errors, 66 warnings. Log: `final-lint.log`. `git diff --check` and the exact staged diff check passed.
- The first added test did not compile because `performTextReplacement` returns Unit; its subsequent IME action was split into a separate interaction, and the integrated gate passed. No app behavior was changed for this correction.
- The initial compact runtime suite passed seven of eight cases. The recreation case tapped an exercise toggle while the restoration Snackbar covered it; before/after runtime screenshots established the intercepted tap. Waiting only after the tap did not help. The test now waits for the Snackbar to disappear before tapping and then awaits the asynchronously persisted expansion. App state logic remains unchanged. Explicit planned-set removal/re-add coverage was also added.
- PASS: `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.features.workout.ActiveWorkoutSetActionsInstrumentedTest --console=plain --no-parallel --max-workers=2`, on existing agent-owned API 36 AVD `TrainIQ_Agent_Migration_20260911`, serial `emulator-5582`.
- Final runtime matrix: 360 × 640 dp / light / font 1.0: 8/8 passed; 360 × 640 dp / dark / font 1.5: 8/8 passed; 411 × 891 dp / light / font 1.3: 8/8 passed. No failures/errors/skips in those final runs. Unique logs are `runtime-normal-final.log`, `runtime-dark150.log` and `runtime-large130.log`; earlier diagnostic failures remain recorded separately.
- Checks cover first/current/completed/next sets, planned-set removal/re-add, extra sets, long titles, field filtering/editing and IME traversal, corrections and deletion, double-tap persistence protection, collapse/notes/recreation, rest adjustment/skip, two-exercise scrolling and background/resume, and one-time finish persistence. The synthetic metric test proves aligned heights/baselines, no adjacent overlap, compact heights and minimum 48 dp targets.
- Runtime screenshots show coherent set states, wrapping previous/target context, an unobstructed logging action after scrolling, adaptive completed-header controls, and finish/rest controls above system navigation and the IME. A compact first-set card can require scrolling to its log action; that reachability is explicitly tested. Snackbar overlays are transient and are awaited when a test targets an underlying control.

The final crash buffer was empty. A3 restored the emulator's original size/density/font/night settings and stopped only this task's emulator `5582`; the existing AVD was retained. Fresh runtime captures use `after-normal-final-*`, `after-dark150-*` and `after-large130-*` names. The large matrix XML is preserved as `results-large130.xml`; normal/dark XML reports were overwritten by subsequent Gradle runs, while their unique logs retain the exact counts.

Generated logs and before/after captures stay untracked in `.codex/device-qa/active-workout-polish/`; baseline captures are from the exact baseline above. Source, tests and this reproducibility record are tracked.

## Limits

Tablet/landscape, physical-device performance, live Health Connect/AI, and TalkBack/Switch Access were not run in this focused phone matrix. Those boundaries were not changed. Global blueprint alignment is not reassessed. No release, signing, upload, remote mutation or push is part of this polish batch.

The next safe extension is physical-device/TalkBack acceptance for the existing wider accessibility finding; it does not require additional feature or architecture work in this completed polish scope.
