# Scroll focus and APK validation follow-up

## Scope and cause

Base: PR #35 merge `0d8442b5433ef6151d0eeee963e8c5ff8e667d36`. The user authorized fixing the APK blocker, pushing the correction, and retrying delivery. The failed delivery remains recorded separately; its pinned source, blocked lock and failure evidence are preserved.

The release preparation first passed 919 JVM and 169 Android tests plus lint. A subsequent migration-marker invocation reran the Android suite and failed `ExercisePlanValidationInstrumentedTest.editRejectsMalformedValuesThenSavesCorrectedDraft`: the error text was not displayed after semantic scrolling. A focused pass alone did not resolve that failure.

The dialog's focused input schedules delayed bring-into-view requests. Semantic scrolling does not dismiss that focus. More importantly, a real drag also failed to clear focus: the shared `clearFocusOnScrollOrDrag` observer waited for touch slop in the Main pointer pass, where a child scrollable can consume movement first and cancel that observation. The strengthened real-dialog test failed its focus assertion twice on the original implementation, including after settling the keyboard.

The observer now examines movement in the Initial pass, clears focus once beyond the configured touch slop, and consumes no events. A child tap retains focus; a child scroll still moves its content. No layout, validation, persistence, route, permission, dependency, signing configuration or domain rule changes.

The dialog regression now performs a real drag, asserts focus dismissal, scrolls to the message and waits for actual Android layout visibility. It retains disabled-save, visible-error, no-submission and corrected-save assertions. A separate shared-helper regression verifies button delivery, retained focus for taps, focus dismissal during consumed scrolling and actual scroll progress.

## Diagnosis and evidence

Local evidence: `.codex/device-qa/exercise-plan-fix/` (ignored). Android Studio JBR; installed SDK; existing agent-owned `TrainIQ_Agent_Nutrition_20260912`, API 36, `emulator-5580`, 720x1280/320 dpi, font scale 1.3. No downloads, wipes or physical-device interaction.

- A frozen Compose-clock experiment stalled during scroll synchronization and was abandoned. It is not failure reproduction evidence.
- The semantic-scroll diagnostic passed and retained input focus; before/after bounds did not change. It did not capture the original intermittent interleaving.
- `focused-corrected.log` and `keyboard-settled.log`: original helper failed the real-drag focus assertion.
- `initial-pass-green.log`: corrected helper passed all four exercise-plan and tap-outside tests, with no skips, in 1m49s.
- `full-validation.log` / `full-validation.xml`: debug build, all 919 JVM tests and lint PASS (68 warnings, zero errors). Full Android run: all 169 existing cases PASS, including all three exercise-plan cases; the newly added helper fixture failed because its empty scroll column had no width. This failed run is retained, not relabelled as a pass. Only the fixture was corrected with `fillMaxWidth`; production code was unchanged.
- `final-focused-marker.log` / `.xml`: the corrected helper fixture, tap-outside test, three exercise-plan cases and all 13 migration cases PASS (18 tests, zero failures/errors/skips), in 1m38s. `generateReleaseRoomMigrationChainVerificationMarker` and `lintDebug` PASS. The generated release marker records the real migration run; it is not copied from another checkout.

Commands from `TrainIQ-Project/`, with command-scoped JBR/SDK and `ANDROID_SERIAL=emulator-5580`:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain --no-daemon --max-workers=2
.\gradlew.bat :app:generateReleaseRoomMigrationChainVerificationMarker :app:lintDebug '-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.core.ui.ScrollFocusInstrumentedTest,com.trainiq.core.ui.TapOutsideFocusInstrumentedTest,com.trainiq.features.workout.ExercisePlanValidationInstrumentedTest,com.trainiq.core.database.TrainIqDatabaseMigrationTest' --console=plain --no-daemon --max-workers=2
```

The second command uses the configured connected route for actual migration coverage plus affected regressions. Passing unchanged cases from the complete run are reused; there is no claim of a single all-green 170-case invocation. All 170 distinct Android cases have passing evidence for the final source/fixtures. No valid assertion was removed or skipped. `git diff --check` passes.

These checks ran on the working tree based on `0d8442b`, before the corrective commit, so embedded debug Git metadata can report a dirty tree. The deliverable must instead be built and validated from the corrected, clean pinned remote main after an authorized merge. No release APK, ZIP, upload or email is claimed by this audit.

The shared helper affects multiple feature screens, so final validation includes the complete local JVM/Android suites and lint. Release delivery remains a separate clean-main, signature, package, installation and private-link verification process; a passing debug test is not release artifact validation.

## References

- [Compose pointer-event passes and consumption](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures): Initial dispatch reaches ancestors before children; consumption does not automatically stop dispatch.
- [Compose test synchronization](https://developer.android.com/develop/ui/compose/testing/synchronization): Android measure/layout is outside the Compose test clock; condition-based waiting is appropriate for external completion.

The pinned Compose version is retained. No new synchronization API or dependency is introduced. Physical performance, live health/AI providers and human accessibility certification are not claimed.
