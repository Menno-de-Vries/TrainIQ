# Barcode, meal categories and six UX improvements

Date: 2026-09-08. Base: `main` at `52e3aaa2cbb04fd9cd744b3f0ca5a6f165041b06`.
Task branch: `codex/barcode-meal-ux`.

## Scope

- Removed Settings > Health Connect > Samsung access button and its exclusive permission/request-resolution call chain. Kept the shared Samsung step reader, permission inspection, readiness diagnostics and Health Connect consumers. Searches across app sources found no remaining callers of the removed request methods. No permission declarations, data formats, database schemas or dependencies changed.
- Camera initialization, analyzer installation, preview attachment and initialization-future failures now use the existing visible camera fallback. Configure the analyzer before lifecycle binding; unbind before clearing it; ignore late callbacks after disposal. Refresh camera permission on resume from Android settings. Accept numeric retail barcode values only.
- Preserve the three manifest-discovered ML Kit registrar constructors in R8 builds. This fixes the startup crash without disabling optimization or upgrading dependencies.
- Reuse the nutrition scanner/product editor from all four meal menus (the existing UI labels are Ochtend, Middag, Avond and Snacks). Keep the meal target in saved UI state until recognition; require product review and existing meal-save confirmation. Cancellation does not open a new editor or persist anything.
- Preserve transport failures separately from unknown/missing product data. Keep bounded HTTP timeouts/response size, cancellation and latest-request protection. Display lookup progress/failure and retry inside the product editor.
- Restart an in-flight food lookup when its editor is restored after process loss, retaining the meal category. A restoration regression failed before this restart and passes after it.

Exactly two additional improvements per tab:

| Tab | Improvement 1 | Improvement 2 |
| --- | --- | --- |
| Training | Clear the exercise search and filters in one action | Preserve overview scroll position when returning from routine details |
| Voeding | Clear saved-product/ingredient searches in one action | Explain unavailable AI-photo actions using shared setup guidance |
| Coach | Direct navigation from missing-profile/advice states to Goals | Separate saved scroll positions for Week, Goals and Advice |

## Reproduction and limits

The user reported a crash roughly one second after opening Barcode, before preview, with camera permission already granted for AI photos. The original debug scanner opened on the local emulator and passed the real-camera AI-to-barcode switching test. The **minified profileable build reproduced the crash** through Voeding > Ochtend > Barcode scannen using the original CameraScannerScreen and original ProGuard rules. Logcat recorded a main-thread NullPointerException in BarcodeScanning.getClient. Earlier ComponentDiscovery errors recorded NoSuchMethodException for the no-argument constructors of CommonComponentRegistrar, VisionCommonRegistrar and BarcodeRegistrar. R8 usage.txt confirmed all three constructors were removed.

`BarcodeStartupSmokeTest` failed against that original scanner (no scanner screen after the crash). With the three constructor keep rules and repaired scanner, it passes with an active CameraService client and a retained scanner UI, then returns without adding anything. This is functional validation in the existing profileable test module, not an emulator performance claim. The physical phone was not available; the reproduced optimized-build failure matches the reported timing and route.

The original lookup service failed `transportFailureMustRemainDistinguishableFromUnknownBarcode`: IOException was collapsed into null and presented as missing product data. The corrected service passes this regression check and keeps HTTP 404 distinct from transport failure.

Baseline tooling issues encountered:

- Initial Hilt compilation could not find `TrainIqApplication_ComponentTreeDeps`. Regenerating Hilt dependencies and compilation locally with `--rerun-tasks` resolved it; no tracked build configuration changed.
- The existing camera permission test revoked its own runtime permission while instrumentation was running. Android killed the test process. Replaced this with a denied-permission UI fixture and a real granted-camera lifecycle test; system permission transitions are checked separately outside instrumentation.
- During test development, a Snack selector was corrected to the existing label Snacks, and the restoration test was aligned with the pinned JUnit4 Compose restoration API. These were test defects, not app failures.
- The new profileable smoke test initially assumed onboarding actions were above the fold. It now scrolls through existing onboarding on compact screens; this was a test navigation defect.

## Local verification

Environment: Windows, Android Studio bundled JBR 25.0.2 (Java 17 compilation target), installed Android SDK, API 36 Android 16 AVD `TrainIQ_Agent_API36_20260806`, isolated serial `emulator-5580`. Original 1080x2400/density420 and compact 720x1280/density320/font1.3 (360x640 dp). No physical/user device was modified. No SDK downloads, licenses or dependency upgrades were introduced. Existing PR CI is inspected separately from local evidence.

Commands run from `TrainIQ-Project`, with command-scoped `JAVA_HOME`, `ANDROID_HOME` and `ANDROID_SERIAL`:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
.\gradlew.bat :app:assembleProfileable :macrobenchmark:compileProfileableJavaWithJavac --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=<selected classes below>' --console=plain
.\gradlew.bat :macrobenchmark:connectedProfileableAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.macrobenchmark.BarcodeStartupSmokeTest' --console=plain
```

Selected instrumented classes:

- `features.nutrition.BarcodeMealFlowInstrumentedTest`: all meal targets, success/confirmation, cancel/back, unknown product, transport error/retry, and restoration while scanner is open; real nutrition UI and typed navigation with camera/transport/storage fakes.
- `features.nutrition.CameraPermissionScannerInstrumentedTest`: denied UI/back and real CameraX startup/switching with permission already granted.
- `features.nutrition.BarcodeRecognitionInstrumentedTest`: bundled ML Kit reads a deterministic EAN-13 bitmap without a network request.
- `navigation.ScannerSavedStateHandleInstrumentedTest`: typed-navigation result delivery/clearing.
- `features.nutrition.NutritionAiResultStateRestorationInstrumentedTest` and `ScannerRecoveryInstrumentedTest`: adjacent AI/editor recovery and late-result protection.
- `features.coach.CoachProfileStateRestorationInstrumentedTest`: direct setup actions plus existing draft/report recovery.
- `features.workout.WorkoutAiRoutineGenerationStateRestorationInstrumentedTest`: manual/AI routine draft recovery.

Local results:

- PASS: 890 JVM tests, zero failures/errors.
- PASS: lintDebug, zero errors; 65 existing warnings (same count before changes).
- PASS: assembleDebug, assembleProfileable and profileable test compilation.
- PASS: 28 distinct targeted debug instrumented tests across the selected runs. Final barcode run: 13/13; earlier compact Coach/meal run: 15/15; adjacent navigation, AI and workout tests passed in the earlier 23-test run. One invalid Snacks selector in that earlier run was corrected and rerun successfully. No aggregate single-run claim is made.
- PASS: final minified BarcodeStartupSmokeTest, active camera plus stable barcode screen and Back. The original scanner failed this regression with a captured fatal exception.
- PASS: manual minified permission flow: deny the system camera request, open Android app settings, grant camera access, return to the same scanner. Preview and active CameraService client recover without a crash. Screenshot and UI tree captured locally.
- PASS: manual compact/large-font screenshots and UI-tree inspection of Health Connect, Coach setup action, Training search clearing, Nutrition search clearing and shared AI guidance. The clear actions restored the full/empty lists; the Coach action opened Goals. Health Connect refresh, explanation, system request and denial/return preserved the access-required state without the Samsung button.
- PASS: public read-only lookup of EAN 3017620422003 returned Nutella with all required nutrition fields. Camera startup, real bundled ML Kit decoding and the four category confirmation flows are separately exercised at their respective layers; fake transport/storage in the UI tests avoids real data writes.

No physical-device performance or hardware-wide compatibility claim is made. Generated XML, screenshots and logs remain untracked under `.codex/device-qa/barcode-meal-ux/` or Gradle build outputs. The existing agent-owned AVD was started visibly; no AVD was created or wiped.

CameraX behavior reference: [CameraController API](https://developer.android.com/reference/androidx/camera/view/CameraController) and [LifecycleCameraController API](https://developer.android.com/reference/androidx/camera/view/LifecycleCameraController), checked 2026-09-08 against the project's CameraX 1.4.2 API usage. No dependency upgrade was made.

R8 reference: [Use R8 in full mode](https://developer.android.com/topic/performance/app-optimization/full-mode) and [reflection keep-rule examples](https://developer.android.com/topic/performance/app-optimization/keep-rule-examples), checked 2026-09-08. Full mode does not implicitly preserve no-argument constructors for reflectively instantiated classes; the fix names only the three discovered registrars and their constructors.

## Publication

One PR to the verified base `main`; report the final SHA and its automatic CI status at handoff. No merge, auto-merge, release, deployment or branch/worktree cleanup is authorized by this task.
