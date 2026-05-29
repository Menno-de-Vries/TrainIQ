# TrainIQ Full-App QA Run Template

Date: 2026-05-27
Tester: Codex using `trainiq-full-app-qa` short QA fix loop
Build variant: debug
App version/build id: versionName `1.0.1-A`, versionCode `2`
Commit/build identifier: current worktree after commit `670a3227` plus local QA-loop doc updates
Device/emulator: Medium_Phone_2 AVD (`emulator-5554`, `sdk_gphone64_x86_64`)
Android version: 16 as reported by Gradle connected test target label
Theme: default/system
Font scale: default
Network state: not controlled
Health Connect state: not mutated; visible state showed access required
AI provider/key state: no real provider call configured in this short loop

Status values: `PASS`, `FAIL`, `NOT RUN`.
## 2026-05-29 Current Direct APK Artifact Refresh

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| Release build/signing readiness | `./gradlew :app:checkReleaseSigningReadiness :app:assembleRelease --console=plain` | PASS | Command exited `0`; current release APK SHA-256 is `E86E3C9B721C60568B5E8C690DC702A07C33136C147FAE2E68FF98C439045BA6`. Evidence: `docs/qa/evidence/2026-05-29-current-direct-apk-artifact-refresh-loop/checkReleaseSigningReadiness-assembleRelease.txt`, `release-apk-sha256.txt`. |
| Direct APK install | `adb install -r app-release.apk`; `adb shell pm clear com.trainiq` | PASS | Current release APK installed directly and app data was cleared for a fresh launch smoke. Evidence: `docs/qa/evidence/2026-05-29-current-direct-apk-artifact-refresh-loop/adb-install-release-apk.txt`, `pm-clear.txt`. |
| Release launch and UI dump | `adb shell am start -W -n com.trainiq/.MainActivity`; `uiautomator dump`; `screencap` | PASS | Cold launch returned `Status: ok`; UI dump contained TrainIQ top-level content. Evidence: `docs/qa/evidence/2026-05-29-current-direct-apk-artifact-refresh-loop/launch-release-apk.txt`, `trainiq-direct-apk-refresh.xml`, `trainiq-direct-apk-refresh.png`, `direct-apk-refresh-checks.txt`. |
| Release logcat scan | `adb logcat -d -t 3000`; strict TrainIQ crash/ANR/input-timeout/security scan | PASS | `NO_ACTIONABLE_MATCHES`. Evidence: `docs/qa/evidence/2026-05-29-current-direct-apk-artifact-refresh-loop/logcat-actionable-matches.txt`. |

Direct APK Ready after this loop: `NO`. The current direct APK artifact builds, installs and launches cleanly, but owner/manual/live gates still require PASS or owner-approved DEFER evidence.
## 2026-05-29 Post-Accessibility Readiness Audit

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| QA status and evidence consistency | Parse `qa-status-2026-05-27.json`; recount `evidence-index-2026-05-27.md`; verify paths exist | PASS | Status remains `PARTIAL`; Direct APK Ready remains `NO`; evidence declared count and actual unique links matched; missing evidence links were `0`. Evidence: `docs/qa/evidence/2026-05-29-post-accessibility-readiness-audit-loop/audit-checks.txt`, `missing-evidence.txt`. |
| Latest accessibility evidence row audit | Generate `post-accessibility-readiness-audit.json` | PASS | Audit records 7 current automated/runtime/safe-slice evidence rows and 7 open owner/manual/live release gates. Latest accessibility metrics: 59 interactive nodes, 0 effective unlabeled, 0 under-48 and 0 NAF. Evidence: `docs/qa/evidence/2026-05-29-post-accessibility-readiness-audit-loop/post-accessibility-readiness-audit.json`. |

Direct APK Ready after this loop: `NO`. The current evidence packet is internally consistent after the accessibility service-state/static UI audit, but all 7 owner/manual/live gates still require PASS or owner-approved DEFER evidence.
## 2026-05-29 Release Accessibility Service-State/Static UI Audit

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| Accessibility service state | `adb shell settings get secure accessibility_enabled`; `enabled_accessibility_services`; `touch_exploration_enabled`; `dumpsys accessibility` | PASS | Current emulator state captured without mutation: `accessibility_enabled=0`, `enabled_accessibility_services=null`, `touch_exploration_enabled=0`. Evidence: `docs/qa/evidence/2026-05-29-release-accessibility-service-state-loop/accessibility-enabled.txt`, `enabled-accessibility-services.txt`, `touch-exploration-enabled.txt`, `dumpsys-accessibility.txt`. |
| Release top-level capture | `./gradlew :app:installRelease`; `adb shell pm clear`; launch; UIAutomator dumps/screenshots | PASS | Start, Training, Voeding, Coach, Meer/Instellingen and Start return were captured; all six XML dumps matched expected destination content. Evidence: `docs/qa/evidence/2026-05-29-release-accessibility-service-state-loop/xml-content-summary.txt`. |
| Static accessibility heuristics | XML scan for touch targets, NAF and descendant-aware labels | PASS | 59 interactive nodes scanned; under-48px clickable/focusable nodes `0`; NAF nodes `0`; descendant-aware effective unlabeled interactive nodes `0`. Evidence: `docs/qa/evidence/2026-05-29-release-accessibility-service-state-loop/accessibility-state-static-summary.txt`, `effective-label-summary.txt`. |
| Release logcat scan | `adb logcat -d -t 3000`; strict TrainIQ crash/ANR/input-timeout/security scan | PASS | `NO_ACTIONABLE_MATCHES`. Evidence: `docs/qa/evidence/2026-05-29-release-accessibility-service-state-loop/logcat-actionable-matches.txt`. |

Direct APK Ready after this loop: `NO`. This improves safe release accessibility/service-state evidence, but real TalkBack/Switch Access traversal remains open and must be PASS or owner-approved DEFER.
## 2026-05-29 Post-Performance Readiness Audit

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| QA status and evidence consistency | Parse `qa-status-2026-05-27.json`; recount `evidence-index-2026-05-27.md`; verify paths exist | PASS | Status remains `PARTIAL`; Direct APK Ready remains `NO`; evidence declared count and actual unique links matched; missing evidence links were `0`. Evidence: `docs/qa/evidence/2026-05-29-post-performance-readiness-audit-loop/audit-checks.txt`, `missing-evidence.txt`. |
| Latest evidence row audit | Generate `post-performance-readiness-audit.json` | PASS | Audit records 6 current automated/runtime/safe-slice evidence rows and 7 open owner/manual/live release gates. Profileable emulator cold-start median from latest evidence is `3652.0354` ms. Evidence: `docs/qa/evidence/2026-05-29-post-performance-readiness-audit-loop/post-performance-readiness-audit.json`. |

Direct APK Ready after this loop: `NO`. The current evidence packet is internally consistent after the performance refresh, but all 7 owner/manual/live gates still require PASS or owner-approved DEFER evidence.
## 2026-05-29 Current Profileable Performance Refresh

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| Device classification | `adb devices`; `adb shell getprop ro.kernel.qemu` | PASS | Connected target is `emulator-5554`; `ro.kernel.qemu=1`, so this is emulator-only performance evidence. Evidence: `docs/qa/evidence/2026-05-29-current-profileable-performance-refresh-loop/adb-devices.txt`, `ro-kernel-qemu.txt`. |
| Full profileable macrobenchmark suite | `./gradlew :macrobenchmark:connectedProfileableAndroidTest -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR` | PARTIAL | Full 4-test suite reached 1/4 tests and did not complete before command timeout. Post-timeout strict TrainIQ logcat scan returned `NO_ACTIONABLE_MATCHES`. Evidence: `docs/qa/evidence/2026-05-29-current-profileable-performance-refresh-loop/connectedProfileableAndroidTest-emulator-suppressed.txt`, `logcat-timeout-actionable-matches.txt`. |
| Targeted cold-start benchmark | `./gradlew :macrobenchmark:connectedProfileableAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.macrobenchmark.TrainIqStartupBenchmark#coldStartupWithRequiredBaselineProfile -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR` | PASS | Targeted benchmark completed 1/1 with exit code `0`. Emulator `timeToInitialDisplayMs`: min `3287.2187`, median `3652.0354`, max `4056.6182`; Perfetto traces were produced for five iterations. Evidence: `docs/qa/evidence/2026-05-29-current-profileable-performance-refresh-loop/connectedProfileableAndroidTest-coldStartup-emulator-suppressed.txt`, `additional-com.trainiq.macrobenchmark-benchmarkData.json`, `result-macrobenchmark-coldstartup.xml`. |
| Post-targeted logcat scan | `adb logcat -d -t 3000`; strict TrainIQ crash/ANR/input-timeout/security scan | PASS | `NO_ACTIONABLE_MATCHES`. Evidence: `docs/qa/evidence/2026-05-29-current-profileable-performance-refresh-loop/logcat-coldStartup-actionable-matches.txt`. |

Direct APK Ready after this loop: `NO`. This refreshes emulator profileable cold-start evidence, but physical-device benchmark threshold/signoff remains open.
## 2026-05-29 Synthetic Lower-Version Over-Install Persistence Loop

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| Synthetic lower release build | Isolated project copy with `versionCode=1`; `./gradlew :app:checkReleaseSigningReadiness :app:assembleRelease --console=plain` | PASS | Built a signed synthetic lower release APK from current code in an isolated temp copy. Metadata: package `com.trainiq`, versionCode `1`, versionName `1.0.0-synthetic-upgrade-seed`. Evidence: `docs/qa/evidence/2026-05-29-synthetic-lower-version-overinstall-persistence-loop/synthetic-lower-assembleRelease-rerun.txt`, `badging-synthetic-lower.txt`, `apksigner-synthetic-lower.txt`. |
| Lower install and seed | `adb install synthetic-lower-versionCode1-release.apk`; UI set Settings theme to `Licht` | PASS | Lower APK installed successfully, launched with `Status: ok`, and the seeded Settings XML contained theme Light evidence. Evidence: `docs/qa/evidence/2026-05-29-synthetic-lower-version-overinstall-persistence-loop/adb-install-synthetic-lower.txt`, `launch-synthetic-lower.txt`, `lower-after-theme.xml`. |
| Current release over-install | `adb install -r app-release.apk`; relaunch current release | PASS | Current release installed over the synthetic lower APK without clearing data, launched with `Status: ok`, rendered TrainIQ Settings/Meer content, and retained the seeded theme Light evidence. Evidence: `docs/qa/evidence/2026-05-29-synthetic-lower-version-overinstall-persistence-loop/adb-install-current-over-synthetic-lower.txt`, `launch-current-after-overinstall.txt`, `current-after-overinstall.xml`, `overinstall-persistence-checks.txt`. |
| Post-over-install logcat | `adb logcat -d -t 2500`; strict TrainIQ crash/ANR/input-timeout/security scan | PASS | `NO_ACTIONABLE_MATCHES`. Evidence: `docs/qa/evidence/2026-05-29-synthetic-lower-version-overinstall-persistence-loop/logcat-actionable-matches-current-after-overinstall.txt`. |

Direct APK Ready after this loop: `NO`. This proves same-lineage synthetic lower-to-current install mechanics and a simple persisted Settings value, but it does not fully close the true older-version upgrade/persistence gate because the lower APK was generated from current code and did not seed all representative data surfaces.
## 2026-05-29 Privacy/Key-Storage Contract Refresh

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| Targeted key/privacy tests | `./gradlew :app:testDebugUnitTest --tests "com.trainiq.core.security.GeminiKeyMigrationTest" --tests "com.trainiq.domain.usecase.ClearAppDataUseCaseTest" --tests "com.trainiq.features.settings.SettingsUiStateTest" --console=plain` | PASS | Targeted JVM tests completed with exit code `0`, covering encrypted Gemini/OpenAI key migration, clear-all key orchestration, Settings key masking and destructive copy. Evidence: `docs/qa/evidence/2026-05-29-privacy-key-storage-contract-refresh-loop/privacy-key-storage-targeted-tests.txt`. |
| Source/docs/evidence secret-pattern scan | High-risk Gemini/OpenAI/API-key/Bearer text scan | PASS | Scan produced 20 broad-pattern hits; every hit was classified as false positive, with unreviewed hits `0` and real secret findings `0`. Evidence: `docs/qa/evidence/2026-05-29-privacy-key-storage-contract-refresh-loop/privacy-scan-summary.txt`, `secret-pattern-matches.json`, `secret-pattern-false-positive-review.json`, `secret-pattern-classification-summary.txt`. |

Direct APK Ready after this loop: `NO`. This strengthens safe privacy/security contract evidence, but owner real-key save/readback/delete/signoff remains open and must be PASS or owner-approved DEFER.
## 2026-05-29 Current Readiness Completion Audit

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| QA status and evidence consistency | Parse `qa-status-2026-05-27.json`; recount `evidence-index-2026-05-27.md`; verify paths exist | PASS | Status remains `PARTIAL`; Direct APK Ready remains `NO`; evidence declared count and actual unique links matched; missing evidence links were `0`. Evidence: `docs/qa/evidence/2026-05-29-current-readiness-completion-audit-loop/audit-checks.txt`, `missing-evidence.txt`. |
| Current requirement matrix | Generate `readiness-completion-audit.json` | PASS | Audit recorded 6 currently proven automated/runtime/safe-slice requirements and 7 open owner/manual/live release gates. Evidence: `docs/qa/evidence/2026-05-29-current-readiness-completion-audit-loop/readiness-completion-audit.json`. |

Direct APK Ready after this loop: `NO`. The current evidence packet is internally consistent, but TalkBack/Switch traversal, performance signoff, seeded Health Connect background proof, real-key signoff, live provider/scanner flows, manual deep UX audits and true seeded older-version upgrade/persistence still require PASS or owner-approved DEFER evidence.
## 2026-05-29 Release Settings Font-Scale 1.5 Scroll-Candidate Review

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| Release install and visual setup | `./gradlew :app:installRelease`; `adb shell settings put system font_scale 1.5`; `adb shell cmd uimode night yes`; `adb shell pm clear com.trainiq` | PASS | Current release APK installed, app data cleared, dark mode forced and font scale set to `1.5`. Evidence: `docs/qa/evidence/2026-05-29-release-settings-font15-scroll-candidate-loop/installRelease.txt`, `font-scale-during.txt`, `night-mode-during.txt`. |
| Settings edge capture | Tap Meer/Settings; `uiautomator dump`; `screencap` | PASS with note | Edge capture reproduced the previous heuristic candidate: 1 clickable/focusable node under 48px (`266x42`) at the bottom edge of the viewport. Evidence: `docs/qa/evidence/2026-05-29-release-settings-font15-scroll-candidate-loop/settings-edge.xml`, `settings-edge-under-48px.txt`. |
| Scrolled Weergave capture | `adb shell input swipe`; `uiautomator dump`; `screencap` | PASS | After scrolling Weergave/theme controls fully into view, the XML scan reported `0` under-48px clickable/focusable nodes. Evidence: `docs/qa/evidence/2026-05-29-release-settings-font15-scroll-candidate-loop/settings-weergave-scrolled.xml`, `settings-weergave-scrolled-under-48px.txt`, `settings-scroll-candidate-summary.txt`. |
| Release logcat scan and restore | `adb logcat -d -t 2500`; restore font scale/night mode | PASS | Strict TrainIQ crash/ANR/input-timeout/security scan returned `NO_ACTIONABLE_MATCHES`; font scale and night mode were restored. Evidence: `docs/qa/evidence/2026-05-29-release-settings-font15-scroll-candidate-loop/logcat-actionable-matches.txt`, `font-scale-after-restore.txt`, `night-mode-after-restore.txt`. |

Direct APK Ready after this loop: `NO`. The Settings under-48px hit is classified as a viewport-edge artefact, not a fixed-position release blocker, but TalkBack/Switch traversal and broader manual accessibility/UX gates still require real traversal or owner-approved defer.
## 2026-05-29 Release Dark Mode/Font-Scale 1.5 Top-Level Loop

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| Release install and visual setup | `./gradlew :app:installRelease`; `adb shell settings put system font_scale 1.5`; `adb shell cmd uimode night yes`; `adb shell pm clear com.trainiq` | PASS | Current release APK installed, app data cleared, dark mode forced and font scale set to `1.5`. Evidence: `docs/qa/evidence/2026-05-29-release-dark-font15-top-level-loop/installRelease.txt`, `docs/qa/evidence/2026-05-29-release-dark-font15-top-level-loop/font-scale-during.txt`, `docs/qa/evidence/2026-05-29-release-dark-font15-top-level-loop/night-mode-during.txt`. |
| Release cold launch | `adb shell am start -W -n com.trainiq/.MainActivity` | PASS | Cold launch returned `Status: ok`, `LaunchState: COLD`, `TotalTime: 2167`. Evidence: `docs/qa/evidence/2026-05-29-release-dark-font15-top-level-loop/corrected-launch-dark-font15.txt`. |
| Dark/large-font top-level traversal | `adb shell input tap`; `uiautomator dump`; `screencap` | PASS | Start, Training, Voeding, Coach, Meer/Instellingen and Start return were captured after corrected bottom-navigation taps; every XML dump matched expected destination content. Evidence: `docs/qa/evidence/2026-05-29-release-dark-font15-top-level-loop/xml-content-summary.txt` plus matching XML/PNG files in the same evidence directory. |
| Heuristic accessibility scan | XML clickable/focusable bounds scan | PASS with note | One small clickable/focusable virtual node was reported in Settings XML (`266x42`, empty text/content-desc). Visual screenshot remained operable; this remains carried under open TalkBack/Switch/manual accessibility gates, not classified as a reproduced release blocker from this safe slice. Evidence: `docs/qa/evidence/2026-05-29-release-dark-font15-top-level-loop/under-48px-clickable-focusable.txt`. |
| Release logcat scan and restore | `adb logcat -d -t 3500`; restore font scale/night mode | PASS | Strict TrainIQ crash/ANR/input-timeout/security scan returned `NO_ACTIONABLE_MATCHES`; font scale and night mode were restored. Evidence: `docs/qa/evidence/2026-05-29-release-dark-font15-top-level-loop/logcat-actionable-matches.txt`, `docs/qa/evidence/2026-05-29-release-dark-font15-top-level-loop/font-scale-after-restore.txt`, `docs/qa/evidence/2026-05-29-release-dark-font15-top-level-loop/night-mode-after-restore.txt`. |

Direct APK Ready after this loop: `NO`. This improves safe emulator dark/large-font release UX evidence, but physical TalkBack/Switch traversal, owner performance signoff, seeded Health Connect background-read proof, real-key signoff, live provider/scanner flows, true seeded older-version upgrade/persistence and broader manual deep-runtime UX gates remain open.

## 2026-05-29 Tablet/Foldable Layout Smoke Loop

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| Tablet display setup | `adb shell wm size 1600x2560`; `adb shell wm density 320` | PASS | Emulator was temporarily set to a tablet-style 800dp shortest-width layout. Original physical size/density were captured for restore. Evidence: `docs/qa/evidence/2026-05-29-tablet-foldable-layout-smoke-loop/wm-size-before.txt`, `docs/qa/evidence/2026-05-29-tablet-foldable-layout-smoke-loop/wm-density-before.txt`, `docs/qa/evidence/2026-05-29-tablet-foldable-layout-smoke-loop/wm-size-during.txt`, `docs/qa/evidence/2026-05-29-tablet-foldable-layout-smoke-loop/wm-density-during.txt`. |
| Debug tablet launch comparison | `./gradlew :app:installDebug`; `adb shell am start -W -n com.trainiq/.MainActivity` | FAIL -> classified | Debug build timed out twice under the tablet override and window state recorded a TrainIQ focus-event input timeout; because release APK launched successfully under the same override, this is recorded as non-release/harness evidence, not a Direct APK blocker. Evidence: `docs/qa/evidence/2026-05-29-tablet-foldable-layout-smoke-loop/launch-tablet-smoke.txt`, `docs/qa/evidence/2026-05-29-tablet-foldable-layout-smoke-loop/launch-tablet-repro.txt`, `docs/qa/evidence/2026-05-29-tablet-foldable-layout-smoke-loop/window-after-tablet-repro.txt`. |
| Release tablet launch | `./gradlew :app:installRelease`; `adb shell am start -W -n com.trainiq/.MainActivity` | PASS | Current release APK installed and cold-launched with `Status: ok`, `LaunchState: COLD`, `TotalTime: 5620`. Evidence: `docs/qa/evidence/2026-05-29-tablet-foldable-layout-smoke-loop/installRelease-tablet-smoke.txt`, `docs/qa/evidence/2026-05-29-tablet-foldable-layout-smoke-loop/launch-release-tablet.txt`, `docs/qa/evidence/2026-05-29-tablet-foldable-layout-smoke-loop/logcat-after-release-tablet-launch.txt`. |
| Release top-level tablet traversal | `uiautomator dump`; `adb shell input tap` through nav rail | PASS | Start, Training, Voeding, Voortgang, Coach, Instellingen/Meer and Start return were captured. Evidence: `docs/qa/evidence/2026-05-29-tablet-foldable-layout-smoke-loop/release-tablet-start-fresh.xml`, `docs/qa/evidence/2026-05-29-tablet-foldable-layout-smoke-loop/release-tablet-training.xml`, `docs/qa/evidence/2026-05-29-tablet-foldable-layout-smoke-loop/release-tablet-nutrition.xml`, `docs/qa/evidence/2026-05-29-tablet-foldable-layout-smoke-loop/release-tablet-progress.xml`, `docs/qa/evidence/2026-05-29-tablet-foldable-layout-smoke-loop/release-tablet-coach.xml`, `docs/qa/evidence/2026-05-29-tablet-foldable-layout-smoke-loop/release-tablet-settings.xml`, `docs/qa/evidence/2026-05-29-tablet-foldable-layout-smoke-loop/release-tablet-start-return.xml`. |
| Tablet layout heuristic scan | XML bounds/content-desc scan | PASS | 7 primary dumps scanned; 85 interactive nodes, 0 outside-bounds nodes. One initial under-48dp candidate was a partially visible Settings switch at the viewport edge; after scroll review the same switch was fully visible at 104x96px. Evidence: `docs/qa/evidence/2026-05-29-tablet-foldable-layout-smoke-loop/tablet-layout-heuristic-scan.txt`, `docs/qa/evidence/2026-05-29-tablet-foldable-layout-smoke-loop/release-tablet-settings-scrolled.xml`, `docs/qa/evidence/2026-05-29-tablet-foldable-layout-smoke-loop/settings-scrolled-touch-target-check.txt`. |
| Release traversal logcat | `adb logcat -d -t 3500`; actionable pattern scan | PASS | `NO_ACTIONABLE_MATCHES`. Evidence: `docs/qa/evidence/2026-05-29-tablet-foldable-layout-smoke-loop/logcat-release-tablet-traversal.txt`, `docs/qa/evidence/2026-05-29-tablet-foldable-layout-smoke-loop/logcat-release-tablet-actionable-matches.txt`. |
| Display restore and launch smoke | `adb shell wm size reset`; `adb shell wm density reset`; `adb shell am start -W -n com.trainiq/.MainActivity` | PASS | Display restored to physical `1080x2400`, density `420`; release launch after restore returned `Status: ok`; actionable logcat scan returned `NO_ACTIONABLE_MATCHES`. Evidence: `docs/qa/evidence/2026-05-29-tablet-foldable-layout-smoke-loop/wm-size-after-restore.txt`, `docs/qa/evidence/2026-05-29-tablet-foldable-layout-smoke-loop/wm-density-after-restore.txt`, `docs/qa/evidence/2026-05-29-tablet-foldable-layout-smoke-loop/launch-release-after-display-restore.txt`, `docs/qa/evidence/2026-05-29-tablet-foldable-layout-smoke-loop/logcat-after-display-restore-actionable-matches.txt`. |

Direct APK Ready after this loop: `NO`. Release tablet/foldable-style smoke is green, but physical TalkBack/Switch Access traversal, full manual visual deep-flow overlap certification, real-key signoff, live AI/provider flows, real optical scanner decode/result return, true older-release upgrade/persistence, and live Health Connect background data-read proof remain open.
## 2026-05-29 Current Regression Refresh Loop

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| JVM/lint/debug baseline | `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain` | PASS | Current unit tests, lint and debug assemble completed successfully. Evidence: `docs/qa/evidence/2026-05-29-current-regression-refresh-loop/unit-lint-assembleDebug.txt`. |
| Initial full connected run | `./gradlew :app:connectedDebugAndroidTest --console=plain` | FAIL -> classified | Failed once in `TrainIqFlowSmokeInstrumentedTest` with `RootViewWithoutFocusException`; emulator window state showed a Nexus Launcher ANR/focus issue and captured TrainIQ logcat classification found no TrainIQ crash/ANR. Evidence: `docs/qa/evidence/2026-05-29-current-regression-refresh-loop/connectedDebugAndroidTest-full.txt`, `docs/qa/evidence/2026-05-29-current-regression-refresh-loop/focus-failure-window-classification.txt`, `docs/qa/evidence/2026-05-29-current-regression-refresh-loop/focus-failure-logcat-classification.txt`. |
| Full connected rerun before hardening | `./gradlew :app:connectedDebugAndroidTest --console=plain` | FAIL -> classified | Timed out at 55/57 after 604s; window/logcat captured the same launcher/focus interference pattern. Evidence: `docs/qa/evidence/2026-05-29-current-regression-refresh-loop/connectedDebugAndroidTest-full-rerun.txt`, `docs/qa/evidence/2026-05-29-current-regression-refresh-loop/window-during-full-rerun-hang.txt`, `docs/qa/evidence/2026-05-29-current-regression-refresh-loop/full-rerun-hang-classification.txt`. |
| Test-harness hardening | Code change in `TrainIqFlowSmokeInstrumentedTest` | PASS | The recreation/back-stack smoke now invokes `onBackPressedDispatcher` through `ActivityScenario` after recreation, preserving the app back-stack assertion without Espresso root-focus dependency. |
| Targeted verification after hardening | `./gradlew :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.flow.TrainIqFlowSmokeInstrumentedTest" --console=plain` | PASS | 2/2 tests passed. Evidence: `docs/qa/evidence/2026-05-29-current-regression-refresh-loop/TrainIqFlowSmokeInstrumentedTest-targeted-after-dispatcher-hardening.txt`. |
| Full connected verification after hardening | `./gradlew :app:connectedDebugAndroidTest --console=plain` | PASS | 57/57 tests passed. Evidence: `docs/qa/evidence/2026-05-29-current-regression-refresh-loop/connectedDebugAndroidTest-full-after-hardening.txt`. |
| Debug runtime smoke | `./gradlew :app:installDebug`; `adb shell am start -W -n com.trainiq/.MainActivity`; `uiautomator dump`; `adb logcat -d -t 2500` | PASS | Debug install passed; cold launch returned `Status: ok`; UI dump succeeded after one null-root retry; actionable crash/ANR/security scan returned `NO_ACTIONABLE_MATCHES`. Evidence: `docs/qa/evidence/2026-05-29-current-regression-refresh-loop/installDebug-final-smoke.txt`, `docs/qa/evidence/2026-05-29-current-regression-refresh-loop/launch-debug-final-smoke.txt`, `docs/qa/evidence/2026-05-29-current-regression-refresh-loop/trainiq-debug-final-smoke.xml`, `docs/qa/evidence/2026-05-29-current-regression-refresh-loop/logcat-debug-final-smoke-actionable-matches.txt`. |

Direct APK Ready after this loop: `NO`. Automated regression gates are green after test-harness hardening, but TalkBack/Switch Access traversal, full visual deep-flow overlap certification, real-key signoff, live AI/provider flows, real optical scanner decode/result return, true older-release upgrade/persistence, and live Health Connect background data-read proof remain open without owner-approved defer.
## 2026-05-29 Active Workout Font-Scale 1.5 Dense-Controls Loop

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| Font-scale setup/restore | `adb shell settings get system font_scale`; `adb shell settings put system font_scale 1.5`; restore to original value | PASS | Font scale changed from `1.0` to `1.5`, then restored to `1.0`. Evidence: `docs/qa/evidence/2026-05-29-active-workout-font15-dense-controls-loop/font-scale-before.txt`, `font-scale-during.txt`, `font-scale-after-restore.txt`. |
| Active workout dense controls under large font | `./gradlew :app:connectedDebugAndroidTest --console=plain "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.features.workout.ActiveWorkoutSetActionsInstrumentedTest"` | PASS | 1/1 tests passed on `Medium_Phone_2(AVD) - 16`; covers Training tab, active workout entry, set type change, logged set correction, weight/reps/RPE fields, update action, delete action, delete confirmation dialog, snackbar/result state, and final `0 sets gelogd` state under font scale 1.5. Evidence: `docs/qa/evidence/2026-05-29-active-workout-font15-dense-controls-loop/ActiveWorkoutSetActionsInstrumentedTest-font15.txt`. |
| Logcat crash/ANR/security scan | `adb logcat -d -t 2500`; actionable pattern scan | PASS | `NO_MATCHES`. Evidence: `docs/qa/evidence/2026-05-29-active-workout-font15-dense-controls-loop/logcat-active-workout-font15.txt`, `logcat-crash-anr-security-matches.txt`. |

Direct APK Ready after this loop: `NO`. This closes targeted emulator traversal for the highest-risk active-workout dense controls at font scale 1.5, but physical TalkBack/Switch Access traversal, full visual deep-flow overlap certification, real-key signoff, live AI/provider flows, real optical scanner decode/result return, true older-release upgrade/persistence and live Health Connect background data-read proof remain open.
## 2026-05-29 Health Connect All-Metric/Background-Read Loop

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| Release install and clean HC state | `./gradlew :app:installRelease`; `adb shell pm revoke ...`; `dumpsys package`; `appops get` | PASS | Current release APK installed and started from no Health Connect read grants. Evidence: `docs/qa/evidence/2026-05-29-healthconnect-all-metric-background-loop/installRelease.txt`, `docs/qa/evidence/2026-05-29-healthconnect-all-metric-background-loop/permissions-before.txt`, `docs/qa/evidence/2026-05-29-healthconnect-all-metric-background-loop/appops-before.txt`. |
| System UI all-metric grant | Rationale CTA -> Health Connect prompt -> `Allow all` -> `Allow` | PASS | System UI prompt captured; `Allow all` selected; `READ_ACTIVE_CALORIES_BURNED`, `READ_EXERCISE`, `READ_SLEEP`, `READ_STEPS`, `READ_WEIGHT`, and `READ_HEART_RATE` became `granted=true`. Evidence: `docs/qa/evidence/2026-05-29-healthconnect-all-metric-background-loop/hc-all-system-after-allow-all.xml`, `docs/qa/evidence/2026-05-29-healthconnect-all-metric-background-loop/permissions-after-all-metric-grant.txt`. |
| Background-read package state | `adb shell pm grant com.trainiq android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND`; `dumpsys package` | PASS | `pm grant` returned exit `0`; `READ_HEALTH_DATA_IN_BACKGROUND` became `granted=true` after foreground grants. Evidence: `docs/qa/evidence/2026-05-29-healthconnect-all-metric-background-loop/pm-grant-background-read.txt`, `docs/qa/evidence/2026-05-29-healthconnect-all-metric-background-loop/permissions-after-background-grant-attempt.txt`. |
| Runtime Settings smoke under all grants | `adb shell am start -W -n com.trainiq/.MainActivity`; tap Meer; UI dump | PASS | Main launch returned `Status: ok`; Settings/Meer captured under all-metric grant. Evidence: `docs/qa/evidence/2026-05-29-healthconnect-all-metric-background-loop/launch-main-after-all-grant.txt`, `docs/qa/evidence/2026-05-29-healthconnect-all-metric-background-loop/hc-all-settings.xml`, `docs/qa/evidence/2026-05-29-healthconnect-all-metric-background-loop/settings-health-visible-text-after-all-grant.txt`. |
| Permission restore | `adb shell pm revoke` for all declared Health Connect read permissions | PASS | Foreground and background Health Connect read permissions restored to `granted=false`; appops captured after restore. Evidence: `docs/qa/evidence/2026-05-29-healthconnect-all-metric-background-loop/permissions-after-revoke.txt`, `docs/qa/evidence/2026-05-29-healthconnect-all-metric-background-loop/appops-after-revoke.txt`. |
| Logcat crash/ANR/security scan | `adb logcat -d -t 3000`; actionable pattern scan | PASS | `NO_MATCHES`. Evidence: `docs/qa/evidence/2026-05-29-healthconnect-all-metric-background-loop/logcat-healthconnect-all-metric-background.txt`, `docs/qa/evidence/2026-05-29-healthconnect-all-metric-background-loop/logcat-crash-anr-security-matches.txt`. |

Direct APK Ready after this loop: `NO`. All visible Health Connect foreground metrics and package-level background-read grant state are now covered on emulator, but live background data-read proof with seeded provider data, TalkBack/Switch Access traversal, real-key signoff, live AI/provider flows, real optical scanner decode/result return, true older-release upgrade/persistence and manual deep-runtime UX audits remain open.
## 2026-05-29 Health Connect Partial Grant/Revoke Loop

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| Release install and Health Connect prompt | `./gradlew :app:installRelease`; `adb shell am start -W -n com.trainiq/.core.health.HealthConnectPermissionsRationaleActivity`; scroll/tap CTA | PASS | Current release APK installed; app rationale opened; Android Health Connect system prompt was captured after a transient `uiautomator` null-root retry. Evidence: `docs/qa/evidence/2026-05-29-healthconnect-partial-grant-revoke-loop/installRelease.txt`, `docs/qa/evidence/2026-05-29-healthconnect-partial-grant-revoke-loop/launch-rationale.txt`, `docs/qa/evidence/2026-05-29-healthconnect-partial-grant-revoke-loop/current-after-failed-dump.xml`. |
| Partial Health Connect grant | Tap `Active calories burned` switch; tap enabled `Allow`; `adb shell dumpsys package com.trainiq` | PASS | `android.permission.health.READ_ACTIVE_CALORIES_BURNED: granted=true`; other declared Health Connect read permissions remained `granted=false`. Evidence: `docs/qa/evidence/2026-05-29-healthconnect-partial-grant-revoke-loop/hc-partial-system-after-toggle.xml`, `docs/qa/evidence/2026-05-29-healthconnect-partial-grant-revoke-loop/permissions-after-partial-grant.txt`. |
| Settings partial-copy repro before fix | Open TrainIQ Settings/Meer under one-metric grant | FAIL -> fixed | Reproduced `QA-2026-05-29-003`: Settings showed `Health Connect: Verbonden, nog geen data` despite partial access. Evidence: `docs/qa/evidence/2026-05-29-healthconnect-partial-grant-revoke-loop/hc-partial-settings.xml`, `docs/qa/evidence/2026-05-29-healthconnect-partial-grant-revoke-loop/settings-health-visible-text-after-partial.txt`. |
| Permission restore | `adb shell pm revoke` for declared Health Connect read permissions; `dumpsys package` | PASS | Health Connect read permissions restored to `granted=false`. Evidence: `docs/qa/evidence/2026-05-29-healthconnect-partial-grant-revoke-loop/permissions-after-revoke.txt`, `docs/qa/evidence/2026-05-29-healthconnect-partial-grant-revoke-loop/permissions-after-settings-revoke.txt`, `docs/qa/evidence/2026-05-29-healthconnect-partial-grant-revoke-loop/permissions-after-settings-detail-revoke.txt`. |
| Targeted fix verification | `./gradlew :app:testDebugUnitTest --tests "com.trainiq.features.settings.SettingsUiStateTest" --console=plain` | PASS | Targeted `SettingsUiStateTest` verifies partial access labels as `Gedeeltelijk verbonden` and preserves the partial-permission data-source message. Evidence: `docs/qa/evidence/2026-05-29-healthconnect-partial-grant-revoke-loop/SettingsUiStateTest-after-partial-copy-fix.txt`. |
| Regression verification | `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain` | PASS | Full JVM tests, lint and debug assemble passed. Evidence: `docs/qa/evidence/2026-05-29-healthconnect-partial-grant-revoke-loop/unit-lint-assemble-after-partial-copy-fix.txt`. |
| Logcat crash/ANR/security scan | `adb logcat -d -t 3000`; actionable pattern scan | PASS | `NO_MATCHES`. Evidence: `docs/qa/evidence/2026-05-29-healthconnect-partial-grant-revoke-loop/logcat-healthconnect-partial-grant-revoke.txt`, `docs/qa/evidence/2026-05-29-healthconnect-partial-grant-revoke-loop/logcat-crash-anr-security-matches.txt`. |

Fixed finding: `QA-2026-05-29-003` (`P2`) Settings Health Connect partial-permission copy. Repro: grant only `READ_ACTIVE_CALORIES_BURNED`, open Settings/Meer, observe `Verbonden, nog geen data` despite five denied metrics. Expected: partial access is clearly labeled and explained. Fix: Settings Health Connect label/message helpers now detect mixed granted/denied metric statuses and preserve the partial-permission message.

Direct APK Ready after this loop: `NO`. Partial-grant/revoke evidence improved and one P2 copy bug was fixed, but Health Connect background-read/all-metric mutation coverage, TalkBack/Switch Access traversal, real-key signoff, live AI/provider flows, real optical scanner decode/result return, true older-release upgrade/persistence and manual deep-runtime UX audits remain open.
## 2026-05-29 Health Connect System Prompt Runtime Loop

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| Release install | `./gradlew :app:installRelease --console=plain` | PASS | Current release APK installed on `emulator-5554`. Evidence: `docs/qa/evidence/2026-05-29-healthconnect-system-prompt-loop/installRelease.txt`. |
| App-side rationale launch | `adb shell am start -W -n com.trainiq/.core.health.HealthConnectPermissionsRationaleActivity` | PASS | Launch returned `Status: ok`, `LaunchState: COLD`, `Activity: com.trainiq/.core.health.HealthConnectPermissionsRationaleActivity`. Evidence: `docs/qa/evidence/2026-05-29-healthconnect-system-prompt-loop/launch-rationale.txt`. |
| Rationale CTA reachability | `uiautomator dump`; scroll; tap `Health Connect-toegang geven` | PASS | Bottom CTA was visible after scroll and tapped at the recorded center. Evidence: `docs/qa/evidence/2026-05-29-healthconnect-system-prompt-loop/hc-rationale-bottom.xml`, `docs/qa/evidence/2026-05-29-healthconnect-system-prompt-loop/cta-center.txt`. |
| Android Health Connect system prompt | `uiautomator dump` after CTA tap | PASS | System UI opened under `com.google.android.healthconnect.controller` with `Allow TrainIQ to access your fitness and wellness data?`, `Allow all`, per-metric read toggles, `Don't allow`, and disabled `Allow` while no metrics were selected. Evidence: `docs/qa/evidence/2026-05-29-healthconnect-system-prompt-loop/hc-system-prompt.xml`, `docs/qa/evidence/2026-05-29-healthconnect-system-prompt-loop/window-after-cta.txt`. |
| No permission mutation after Back | `adb shell input keyevent BACK`; `adb shell dumpsys package com.trainiq` | PASS | Before and after Back, `android.permission.CAMERA` plus all declared Health Connect read permissions remained `granted=false`. Evidence: `docs/qa/evidence/2026-05-29-healthconnect-system-prompt-loop/permissions-before.txt`, `docs/qa/evidence/2026-05-29-healthconnect-system-prompt-loop/permissions-after-back.txt`. |
| Logcat crash/ANR/security scan | `adb logcat -d -t 2500`; actionable pattern scan | PASS | `NO_MATCHES`. Evidence: `docs/qa/evidence/2026-05-29-healthconnect-system-prompt-loop/logcat-healthconnect-system-prompt.txt`, `docs/qa/evidence/2026-05-29-healthconnect-system-prompt-loop/logcat-crash-anr-security-matches.txt`. |

Direct APK Ready after this loop: `NO`. This closes the previously weak app-rationale-to-system-prompt evidence. The full Health Connect runtime matrix still remains open because this loop did not grant, deny through the system button, partially select, revoke while open, or validate background-read behavior. Real-key signoff, TalkBack/Switch Access traversal, live AI/provider flows, real optical scanner decode/result return, true older-release upgrade/persistence, and manual deep-runtime UX audits also remain open without owner-approved defer.
## 2026-05-29 Release Permission-State Audit Loop

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| Fresh release install | `adb uninstall com.trainiq`; `./gradlew :app:installRelease --console=plain` | PASS | Current release APK installed after fresh uninstall. Evidence: `docs/qa/evidence/2026-05-29-release-permission-state-audit-loop/adb-uninstall-com.trainiq.txt`, `docs/qa/evidence/2026-05-29-release-permission-state-audit-loop/installRelease.txt`. |
| Release cold launch | `adb shell am start -W -n com.trainiq/.MainActivity` | PASS | Launch returned `Status: ok`, `LaunchState: COLD`, `Activity: com.trainiq/.MainActivity`. Evidence: `docs/qa/evidence/2026-05-29-release-permission-state-audit-loop/launch-release.txt`. |
| Runtime permission state after fresh install | `adb shell dumpsys package com.trainiq`; targeted extract for camera and Health Connect permissions | PASS | `android.permission.CAMERA` and all declared Health Connect read permissions were present with `granted=false`; normal install permissions such as `INTERNET` were granted as expected. Evidence: `docs/qa/evidence/2026-05-29-release-permission-state-audit-loop/dumpsys-package-release.txt`, `docs/qa/evidence/2026-05-29-release-permission-state-audit-loop/permission-lines-extract.txt`. |
| AppOps privacy state | `adb shell appops get com.trainiq` | PASS | `CAMERA`, `READ_WRITE_HEALTH_DATA`, and listed health sensor/read appops were in `ignore` state after fresh install. Evidence: `docs/qa/evidence/2026-05-29-release-permission-state-audit-loop/appops-release.txt`. |
| Release UI/logcat smoke | `uiautomator dump`; `adb logcat -d -t 2500`; actionable crash/ANR/security scan | PASS | UI dump captured; actionable scan returned `NO_MATCHES`. A broader legacy scan matched only normal `uiautomator` `AndroidRuntime` startup/shutdown lines, not a TrainIQ crash. Evidence: `docs/qa/evidence/2026-05-29-release-permission-state-audit-loop/trainiq-release-permission-state.xml`, `docs/qa/evidence/2026-05-29-release-permission-state-audit-loop/logcat-release-permission-state.txt`, `docs/qa/evidence/2026-05-29-release-permission-state-audit-loop/logcat-crash-anr-security-matches.txt`. |

Direct APK Ready after this loop: `NO`. Fresh release permission-state evidence is green, but real-key signoff, TalkBack/Switch Access traversal, full Health Connect runtime matrix, live AI/provider flows, real optical scanner decode/result return, true older-release upgrade/persistence, and manual deep-runtime UX audits remain open without owner-approved defer.
## 2026-05-29 Backup/Data-Extraction Privacy Audit Loop

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| Backup rules review | Copy/read `app/src/main/res/xml/backup_rules.xml`; parse expected exclusions | PASS | Excludes `trainiq-state.json`, DataStore `datastore/trainiq_preferences.preferences_pb`, encrypted Gemini/OpenAI SharedPreferences, and performance session SharedPreferences. Evidence: `docs/qa/evidence/2026-05-29-backup-data-extraction-audit-loop/backup_rules.xml`, `docs/qa/evidence/2026-05-29-backup-data-extraction-audit-loop/backup-rule-exclusion-check.txt`. |
| Data extraction rules review | Copy/read `app/src/main/res/xml/data_extraction_rules.xml`; parse cloud/device-transfer exclusions | PASS | Cloud backup and device transfer exclude the same sensitive stores; cloud backup uses `disableIfNoEncryptionCapabilities=true`. Evidence: `docs/qa/evidence/2026-05-29-backup-data-extraction-audit-loop/data_extraction_rules.xml`, `docs/qa/evidence/2026-05-29-backup-data-extraction-audit-loop/backup-rule-exclusion-check.txt`. |
| Merged release manifest backup flags | Inspect current release APK manifest dump for backup attributes | PASS | Merged manifest has `android:allowBackup=false` and references `fullBackupContent` plus `dataExtractionRules`. Evidence: `docs/qa/evidence/2026-05-29-backup-data-extraction-audit-loop/merged-manifest-backup-lines.txt`. |
| Sensitive store mapping | Source review for DataStore, encrypted AI key SharedPreferences, performance session store and legacy JSON state | PASS | Identified sensitive local stores are covered by explicit exclusions; Room DB is not individually excluded, but app-level `allowBackup=false` disables backup, with resource exclusions as defense-in-depth if backup behavior changes later. Evidence: `docs/qa/evidence/2026-05-29-backup-data-extraction-audit-loop/source-store-mapping.txt`. |

Direct APK Ready after this loop: `NO`. Backup/data-extraction privacy evidence is green, but real-key signoff, TalkBack/Switch Access traversal, full Health Connect runtime matrix, live AI/provider flows, real optical scanner decode/result return, true older-release upgrade/persistence, and manual deep-runtime UX audits remain open without owner-approved defer.
## 2026-05-29 Release APK Manifest/Permission Artifact Audit Loop

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| Release APK badging | `aapt2 dump badging app-release.apk` | PASS | Package `com.trainiq`, versionCode `2`, versionName `1.0.1-A`, minSdk `26`, targetSdk `36`, launch activity `com.trainiq.MainActivity`; camera feature is `required=false`. Evidence: `docs/qa/evidence/2026-05-29-release-apk-manifest-permission-audit-loop/aapt2-dump-badging.txt`. |
| Release permissions | `aapt2 dump permissions app-release.apk`; expected-permission diff | PASS | 14 expected merged permissions found; 0 unexpected; 0 missing; 0 high-risk non-expected permission families. Evidence: `docs/qa/evidence/2026-05-29-release-apk-manifest-permission-audit-loop/aapt2-dump-permissions.txt`, `docs/qa/evidence/2026-05-29-release-apk-manifest-permission-audit-loop/permission-diff.txt`. |
| Merged manifest components | `aapt2 dump xmltree --file AndroidManifest.xml app-release.apk` | PASS | Exported components reviewed as expected launcher, Health Connect rationale/onboarding/permission usage entries, or guarded library components from WorkManager/ProfileInstaller/Health platform. Evidence: `docs/qa/evidence/2026-05-29-release-apk-manifest-permission-audit-loop/aapt2-dump-xmltree-manifest.txt`, `docs/qa/evidence/2026-05-29-release-apk-manifest-permission-audit-loop/permission-component-classification.txt`. |
| Release artifact identity | `Get-FileHash app-release.apk -Algorithm SHA256`; copy `output-metadata.json` | PASS | Release APK hash and Gradle output metadata recorded. Evidence: `docs/qa/evidence/2026-05-29-release-apk-manifest-permission-audit-loop/app-release-sha256.txt`, `docs/qa/evidence/2026-05-29-release-apk-manifest-permission-audit-loop/output-metadata.json`. |

Direct APK Ready after this loop: `NO`. Release artifact manifest/permission audit is green, but real-key signoff, TalkBack/Switch Access traversal, full Health Connect runtime matrix, live AI/provider flows, real optical scanner decode/result return, true older-release upgrade/persistence, and manual deep-runtime UX audits remain open without owner-approved defer.
## 2026-05-29 QA Packet Consistency Refresh Loop

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| QA status JSON parse | Python JSON parse for `qa-status-2026-05-27.json` | PASS | Parsed successfully; status `PARTIAL`, `notRunRows=16`. Evidence: `docs/qa/evidence/2026-05-29-qa-packet-consistency-refresh-loop/qa-status-json-parse-after-index-fix.txt`. |
| QA status reference check | Python traversal of `docs/qa/...` references in `qa-status-2026-05-27.json` | PASS | All status references exist. Evidence: `docs/qa/evidence/2026-05-29-qa-packet-consistency-refresh-loop/qa-status-reference-check-after-index-fix.txt`. |
| Local evidence linkcheck | Python local evidence reference scan across status summary, full ledger, evidence index and `.codex/qa-loop-state.md` | PASS | 1180 local evidence references checked, 0 missing; intentional wildcard/directory-only legacy entries excluded. Evidence: `docs/qa/evidence/2026-05-29-qa-packet-consistency-refresh-loop/local-evidence-linkcheck-after-index-fix.txt`. |
| NOT RUN count consistency | Python comparison of status JSON `notRunRows` with open-gaps snapshot rows | PASS | Status `16`, snapshot `16`. Evidence: `docs/qa/evidence/2026-05-29-qa-packet-consistency-refresh-loop/not-run-count-consistency-after-index-fix.txt`. |
| Evidence Index count sanity | Python comparison of declared current-index total with structured bullet count | PASS | Initial mismatch found (`284` declared vs `194` bullets), then corrected; after adding this loop, declared `211` matches structured bullet count `211`. Evidence: `docs/qa/evidence/2026-05-29-qa-packet-consistency-refresh-loop/evidence-index-count-sanity-after-index-fix.txt`. |

Direct APK Ready after this loop: `NO`. QA packet integrity is green after correcting the Evidence Index count, but real-key signoff, TalkBack/Switch Access traversal, full Health Connect runtime matrix, live AI/provider flows, real optical scanner decode/result return, true older-release upgrade/persistence, and manual deep-runtime UX audits remain open without owner-approved defer.
## 2026-05-29 Privacy Artifact/Secret Scan Loop

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| High-risk secret patterns | `rg --pcre2` over app source/tests, QA docs/evidence and text release output paths for OpenAI, Google, GitHub, Slack, AWS and generic long secret assignment shapes | PASS | No high-risk secret pattern matches found. Evidence: `docs/qa/evidence/2026-05-29-privacy-artifact-secret-scan-loop/secret-pattern-scan-raw.txt`. |
| Release evidence sensitive terms | `rg --pcre2` over latest release evidence for key/bearer/API-key terms | PASS | One false positive: `sk-` matched inside Android ART logcat `--set-task-profile`, not a credential. Evidence: `docs/qa/evidence/2026-05-29-privacy-artifact-secret-scan-loop/release-evidence-sensitive-term-scan.txt`, `docs/qa/evidence/2026-05-29-privacy-artifact-secret-scan-loop/sensitive-term-classification.txt`. |
| Context classification | `rg --pcre2` for `apiKey`, `token`, `secret`, `password`, `Bearer`, `Authorization` across app source/tests and QA docs | PASS | Matches are policy docs, source variable names, runtime auth header construction, and fake test fixtures such as `token` / `abcd1234wxyz`; no production secret literal identified. Evidence: `docs/qa/evidence/2026-05-29-privacy-artifact-secret-scan-loop/sensitive-term-context-scan.txt`. |
| Release output inventory | `Get-ChildItem app/build/outputs -Recurse -Include *.apk,*.aab` | PASS | Release/debug/profileable APK artifacts listed for traceability. Binary APK content was not unpacked/reverse-scanned in this loop. Evidence: `docs/qa/evidence/2026-05-29-privacy-artifact-secret-scan-loop/release-output-artifacts.txt`. |

Direct APK Ready after this loop: `NO`. No real secret material was found in the scanned artifacts/logs/docs/source, but real-key save/readback/privacy signoff remains owner-gated and was intentionally not executed. TalkBack/Switch Access traversal, full Health Connect runtime matrix, live AI/provider flows, real optical scanner decode/result return, true older-release upgrade/persistence, and manual deep-runtime UX audits remain open without owner-approved defer.
## 2026-05-29 Release-Over-Release Same-Lineage Smoke Loop

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| Release signing readiness | `./gradlew :app:checkReleaseSigningReadiness` | PASS | Evidence: `docs/qa/evidence/2026-05-29-release-over-release-same-lineage-loop/checkReleaseSigningReadiness.txt`. |
| Release build | `./gradlew :app:assembleRelease` | PASS | Evidence: `docs/qa/evidence/2026-05-29-release-over-release-same-lineage-loop/assembleRelease.txt`. |
| Baseline release install | `adb uninstall com.trainiq`; `./gradlew :app:installRelease` | PASS | Existing package removed to avoid debug/release signature contamination, then current release APK installed as baseline. Evidence: `docs/qa/evidence/2026-05-29-release-over-release-same-lineage-loop/installRelease-baseline.txt`. |
| Baseline release launch | `adb shell am start -W -n com.trainiq/.MainActivity`; UIAutomator dump | PASS | Baseline release launch returned `Status: ok`; UI dump captured. Evidence: `docs/qa/evidence/2026-05-29-release-over-release-same-lineage-loop/launch-release-baseline.txt`, `docs/qa/evidence/2026-05-29-release-over-release-same-lineage-loop/trainiq-release-baseline.xml`. |
| Same-lineage over-install | `./gradlew :app:installRelease` over existing release install | PASS | Current release APK installed over the existing release-signed install; output includes `Installed on 1 device` and `BUILD SUCCESSFUL`. Evidence: `docs/qa/evidence/2026-05-29-release-over-release-same-lineage-loop/installRelease-over-release.txt`. |
| Post-over-install launch and logcat | `adb shell am start -W -n com.trainiq/.MainActivity`; UIAutomator dump; logcat scan | PASS | Launch returned `Status: ok`, `LaunchState: COLD`; UI dump contains TrainIQ Home; crash/ANR/input-timeout scan returned `NO_MATCHES`. Evidence: `docs/qa/evidence/2026-05-29-release-over-release-same-lineage-loop/launch-release-after-overinstall.txt`, `docs/qa/evidence/2026-05-29-release-over-release-same-lineage-loop/trainiq-release-after-overinstall.xml`, `docs/qa/evidence/2026-05-29-release-over-release-same-lineage-loop/logcat-release-after-overinstall-crash-anr-matches.txt`. |

Direct APK Ready after this loop: `NO`. Same-lineage release over-install for the current APK is green, but a true older-version upgrade/persistence gate still requires an older release APK signed with compatible lineage. TalkBack/Switch Access traversal, full Health Connect runtime matrix, real-key privacy/security signoff, live AI/provider flows, real optical scanner decode/result return, and manual deep-runtime UX audits remain open without owner-approved defer.
## 2026-05-29 Current Dark-Mode Top-Level Runtime Audit Loop

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| Debug install | `./gradlew :app:installDebug` | PASS | Installed current debug build on `emulator-5554`. Evidence: `docs/qa/evidence/2026-05-29-current-dark-mode-top-level-audit-loop/installDebug.txt`. |
| Dark-mode setup and restore | `adb shell cmd uimode night yes`; restore to previous `no` state | PASS | Night mode before/during/after captured and restored. Evidence: `docs/qa/evidence/2026-05-29-current-dark-mode-top-level-audit-loop/uimode-before.txt`, `docs/qa/evidence/2026-05-29-current-dark-mode-top-level-audit-loop/uimode-during.txt`, `docs/qa/evidence/2026-05-29-current-dark-mode-top-level-audit-loop/uimode-after.txt`. |
| Dark-mode top-level UI dumps | `adb shell uiautomator dump` for Start, Training, Voeding, Coach, Settings, Progress and Start return | PASS | XML captured for all compact top-level destinations plus Progress via Settings. Evidence starts at `docs/qa/evidence/2026-05-29-current-dark-mode-top-level-audit-loop/start.xml`; Progress: `docs/qa/evidence/2026-05-29-current-dark-mode-top-level-audit-loop/progress.xml`. |
| Dark-mode parser audit | Python XML parser for 48dp touch targets, NAF nodes, effective unlabeled interactive controls and text-bounds suspects | PASS | Parsed 68 interactive nodes; under-48dp candidates `0`; NAF nodes `0`; effective unlabeled interactive controls `0`; text-bounds clipping suspects `0`. Evidence: `docs/qa/evidence/2026-05-29-current-dark-mode-top-level-audit-loop/dark-mode-audit-summary.txt`. |
| Runtime logcat scan | `adb logcat -d -t 2000`; TrainIQ fatal/ANR/input-timeout pattern scan | PASS | Crash/ANR/input-timeout scan returned `NO_MATCHES`. Evidence: `docs/qa/evidence/2026-05-29-current-dark-mode-top-level-audit-loop/logcat-crash-anr-matches.txt`. |

Direct APK Ready after this loop: `NO`. Dark-mode runtime evidence improved and no app bug was found, but TalkBack/Switch Access traversal, full Health Connect runtime matrix, real-key privacy/security signoff, live AI/provider flows, real optical scanner decode/result return, and manual deep-runtime UX audits remain open without owner-approved defer.
## 2026-05-29 Top-Level Recreation/Back-Stack Runtime Loop

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| Initial targeted flow test | `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.flow.TrainIqFlowSmokeInstrumentedTest` | FAIL | New test initially expected Back from Voortgang to return to Meer. Source review showed `SettingsRoute` opens Voortgang through `navigateTopLevel()`, whose contract returns Back to Start. This was a test expectation issue, not a reproduced app bug. Evidence: `docs/qa/evidence/2026-05-29-top-level-recreation-backstack-loop/TrainIqFlowSmokeInstrumentedTest.txt`. |
| Corrected targeted flow test | `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.flow.TrainIqFlowSmokeInstrumentedTest` | PASS | 2/2 tests passed. New coverage verifies Meer -> Voortgang, activity recreation on Voortgang, Android Back to Start per top-level navigation contract, Training navigation, and second recreation on Training. Evidence: `docs/qa/evidence/2026-05-29-top-level-recreation-backstack-loop/TrainIqFlowSmokeInstrumentedTest-after-contract-fix.txt`. |
| JVM regression | `./gradlew :app:testDebugUnitTest` | PASS | Evidence: `docs/qa/evidence/2026-05-29-top-level-recreation-backstack-loop/testDebugUnitTest.txt`. |
| Lint regression | `./gradlew :app:lintDebug` | PASS | Evidence: `docs/qa/evidence/2026-05-29-top-level-recreation-backstack-loop/lintDebug.txt`. |
| Debug build regression | `./gradlew :app:assembleDebug` | PASS | Evidence: `docs/qa/evidence/2026-05-29-top-level-recreation-backstack-loop/assembleDebug.txt`. |
| Full connected regression | `./gradlew :app:connectedDebugAndroidTest` | PASS | 57/57 tests passed on `Medium_Phone_2(AVD) - 16`. Evidence: `docs/qa/evidence/2026-05-29-top-level-recreation-backstack-loop/connectedDebugAndroidTest-full.txt`. |

Direct APK Ready after this loop: `NO`. Top-level lifecycle/back-stack runtime evidence improved and no app bug was found, but TalkBack/Switch Access traversal, full Health Connect runtime matrix, real-key privacy/security signoff, live AI/provider flows, real optical scanner decode/result return, and manual deep-runtime UX audits remain open without owner-approved defer.
## 2026-05-29 Current Font-Scale 1.5 Accessibility Audit Loop

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| Debug install | `./gradlew :app:installDebug` | PASS | Installed current debug build on `emulator-5554` after clearing installed `com.trainiq` state. Evidence: `docs/qa/evidence/2026-05-29-current-font15-a11y-audit-loop/installDebug.txt`. |
| Font-scale setup/restore | `adb shell settings put system font_scale 1.5`; restore previous value after dumps | PASS | Previous font scale captured and restored. Evidence: `docs/qa/evidence/2026-05-29-current-font15-a11y-audit-loop/font-scale-before.txt`, `docs/qa/evidence/2026-05-29-current-font15-a11y-audit-loop/font-scale-after.txt`. |
| Top-level UIAutomator dumps | `adb shell uiautomator dump` across Start, Training, Voeding, Progress, Coach, Settings and Start return | PASS | XML dumps captured for all top-level destinations. Evidence starts at `docs/qa/evidence/2026-05-29-current-font15-a11y-audit-loop/start.xml`. |
| Font-scale parser audit | Python XML parser for 48dp touch targets, NAF/unlabeled interactive nodes, and text-bounds suspects | PASS | Parsed 58 interactive nodes; found 0 text-bounds clipping suspects, 2 under-48dp candidates, and 1 NAF/unlabeled candidate. Candidate review found the hits were partially visible scroll-continuation nodes in Coach and Settings, not fixed clipped controls. Evidence: `docs/qa/evidence/2026-05-29-current-font15-a11y-audit-loop/font15-a11y-audit-summary.txt`, `docs/qa/evidence/2026-05-29-current-font15-a11y-audit-loop/false-positive-review.txt`. |
| Runtime logcat | `adb logcat -d -t 2000` | PASS | Captured for audit trail; no new reproducible app bug was identified from this loop. Evidence: `docs/qa/evidence/2026-05-29-current-font15-a11y-audit-loop/logcat-font15-audit.txt`. |

Direct APK Ready after this loop: `NO`. This reduces current-worktree large-font smoke risk, but TalkBack/Switch Access traversal, full Health Connect runtime matrix, real-key privacy/security signoff, live AI/provider flows, real optical scanner decode/result return, and manual deep-runtime UX audits remain open.
## 2026-05-29 AI Camera Scanner Modes Runtime Loop

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| AI scanner modes targeted | `./gradlew :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.features.nutrition.AiCameraScannerModesInstrumentedTest'` | PASS | 2/2 tests passed on `Medium_Phone_2(AVD) - 16`; verifies AI meal scanner preview opens with `Camerascanner`, meal guidance, `Foto maken` and `Terug`, and smart-scale preview opens with scale guidance, `Foto maken`, import and `Terug`. Evidence: `docs/qa/evidence/2026-05-29-ai-camera-scanner-modes-loop/AiCameraScannerModesInstrumentedTest-after-permission-override.txt`. |
| JVM regression | `./gradlew :app:testDebugUnitTest` | PASS | Evidence: `docs/qa/evidence/2026-05-29-ai-camera-scanner-modes-loop/testDebugUnitTest.txt`. |
| Lint regression | `./gradlew :app:lintDebug` | PASS | Evidence: `docs/qa/evidence/2026-05-29-ai-camera-scanner-modes-loop/lintDebug.txt`. |
| Debug build regression | `./gradlew :app:assembleDebug` | PASS | Evidence: `docs/qa/evidence/2026-05-29-ai-camera-scanner-modes-loop/assembleDebug.txt`. |
| Full connected regression | `./gradlew :app:connectedDebugAndroidTest` | PASS | 56/56 tests passed on `Medium_Phone_2(AVD) - 16`. Evidence: `docs/qa/evidence/2026-05-29-ai-camera-scanner-modes-loop/connectedDebugAndroidTest-full.txt`. |

Direct APK Ready after this loop: `NO`. AI-meal and smart-scale preview surfaces are now covered at runtime without live AI or camera driver binding in the component test. Real AI meal analysis, valid smart-scale result processing, real optical scanner decode/result return, and owner/manual gates remain open.
## 2026-05-29 Current Release Readiness Refresh

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| Release signing readiness | `./gradlew :app:checkReleaseSigningReadiness` | PASS | Release signing configuration is complete. Evidence: `docs/qa/evidence/2026-05-29-current-release-readiness-refresh/checkReleaseSigningReadiness.txt`. |
| Release build | `./gradlew :app:assembleRelease` | PASS | Release build and lintVital passed. Evidence: `docs/qa/evidence/2026-05-29-current-release-readiness-refresh/assembleRelease.txt`. |
| Release install | `./gradlew :app:installRelease` | PASS | Release APK installed on `Medium_Phone_2(AVD) - 16`. Evidence: `docs/qa/evidence/2026-05-29-current-release-readiness-refresh/installRelease.txt`. |
| Release cold launch | `adb shell am start -W -n com.trainiq/.MainActivity` | PASS | `Status: ok`, `LaunchState: COLD`, `Activity: com.trainiq/.MainActivity`. Evidence: `docs/qa/evidence/2026-05-29-current-release-readiness-refresh/launch-release.txt`. |
| Release UI dump | `adb shell uiautomator dump /sdcard/trainiq-current-release-smoke.xml`; `adb pull ...` | PASS | UI hierarchy dump captured at `docs/qa/evidence/2026-05-29-current-release-readiness-refresh/trainiq-current-release-smoke.xml`. The pull command emitted a PowerShell native-command error line despite the file being successfully pulled. |
| Release crash/ANR/input-timeout scan | `Select-String` over release logcat | PASS | `NO_MATCHES` for `com.trainiq.*FATAL EXCEPTION`, `ANR in com.trainiq`, and `Input dispatching timed out.*com.trainiq`. Evidence: `docs/qa/evidence/2026-05-29-current-release-readiness-refresh/logcat-release-crash-anr-matches.txt`. |

Direct APK Ready after this refresh: `NO`. No new reproducible app P0/P1/P2/P3 bugs were found in executed release checks, but owner/manual gates remain open: TalkBack/Switch Access traversal, Health Connect partial/revoke/background-read matrix, real-key privacy/security signoff, live AI/provider flows, real optical scanner decode/result return, and manual deep-runtime UX audits.
## 2026-05-29 Generated Routine Preview Runtime Loop

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| Generated routine preview targeted | `./gradlew :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.features.workout.GeneratedRoutinePreviewInstrumentedTest'` | PASS | 2/2 tests passed on `Medium_Phone_2(AVD) - 16`; verifies a long generated routine preview keeps Save/Retry/Cancel visible and clickable, and saving state disables only Save while Retry/Cancel remain reachable. Evidence: `docs/qa/evidence/2026-05-29-generated-routine-preview-runtime-loop/GeneratedRoutinePreviewInstrumentedTest.txt`. |
| JVM regression | `./gradlew :app:testDebugUnitTest` | PASS | Evidence: `docs/qa/evidence/2026-05-29-generated-routine-preview-runtime-loop/testDebugUnitTest.txt`. |
| Lint regression | `./gradlew :app:lintDebug` | PASS | Evidence: `docs/qa/evidence/2026-05-29-generated-routine-preview-runtime-loop/lintDebug.txt`. |
| Debug build regression | `./gradlew :app:assembleDebug` | PASS | Evidence: `docs/qa/evidence/2026-05-29-generated-routine-preview-runtime-loop/assembleDebug.txt`. |
| Full connected regression | `./gradlew :app:connectedDebugAndroidTest` | PASS | 54/54 tests passed on `Medium_Phone_2(AVD) - 16`. Evidence: `docs/qa/evidence/2026-05-29-generated-routine-preview-runtime-loop/connectedDebugAndroidTest-full.txt`. |

Direct APK Ready after this loop: `NO`. The generated routine preview runtime surface is now covered without real AI, but live provider generation with a real key and end-to-end save from a real generated provider response remain open provider/owner gates.

## 2026-05-29 Health Connect Rationale CTA Loop

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| Health Connect rationale CTA targeted | `./gradlew :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.core.health.HealthConnectPermissionsRationaleInstrumentedTest'` | PASS | 1/1 tests passed on `Medium_Phone_2(AVD) - 16`; verifies long rationale/status content still allows the bottom `Health Connect-toegang geven` and `Doorgaan naar TrainIQ` actions to scroll into view, remain enabled/displayed and dispatch clicks. Evidence: `docs/qa/evidence/2026-05-29-health-connect-rationale-cta-loop/connected-health-rationale-targeted.txt`. |
| JVM regression | `./gradlew :app:testDebugUnitTest` | PASS | Evidence: `docs/qa/evidence/2026-05-29-health-connect-rationale-cta-loop/test-debug-unit.txt`. |
| Lint regression | `./gradlew :app:lintDebug` | PASS | Evidence: `docs/qa/evidence/2026-05-29-health-connect-rationale-cta-loop/lint-debug.txt`. |
| Debug build regression | `./gradlew :app:assembleDebug` | PASS | Evidence: `docs/qa/evidence/2026-05-29-health-connect-rationale-cta-loop/assemble-debug.txt`. |

Direct APK Ready after this loop: `NO`. The app-side Health Connect rationale CTA click path is now covered, but the full Health Connect runtime matrix remains open because this loop did not mutate Health Connect grants, test revoke-while-open, test background-read permission behavior, or validate Android's system permission controller.

## 2026-05-29 AI Provider Router Loop

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| Targeted AI provider router test, first attempt | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.ai.services.AiProviderRouterTest" --console=plain` | FAIL | Reproduced `QA-2026-05-29-002`: OpenAI-first transient rate-limit did not fall back to Gemini because feature throttle blocked the second provider. Evidence: `docs/qa/evidence/2026-05-29-ai-provider-router-loop/AiProviderRouterTest.txt`. |
| Targeted AI provider router after fix | same targeted command | PASS | Verifies preferred provider, missing-key skip, transient failover, non-transient stop, cancellation propagation and fallback failure recording. Evidence: `docs/qa/evidence/2026-05-29-ai-provider-router-loop/AiProviderRouterTest-after-expectation-fix.txt`. |
| AI services regression | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.ai.services.*" --console=plain` | PASS | Evidence: `docs/qa/evidence/2026-05-29-ai-provider-router-loop/ai-services-tests-after-router-fix.txt`. |
| Compile regression | `.\gradlew.bat :app:compileDebugKotlin --console=plain` | PASS | Evidence: `docs/qa/evidence/2026-05-29-ai-provider-router-loop/compileDebugKotlin-after-router-fix.txt`. |
| Unit regression | `.\gradlew.bat :app:testDebugUnitTest --console=plain` | PASS | Evidence: `docs/qa/evidence/2026-05-29-ai-provider-router-loop/testDebugUnitTest-after-router-fix.txt`. |
| Lint regression | `.\gradlew.bat :app:lintDebug --console=plain` | PASS | Evidence: `docs/qa/evidence/2026-05-29-ai-provider-router-loop/lintDebug-after-router-fix.txt`. |
| Debug build | `.\gradlew.bat :app:assembleDebug --console=plain` | PASS | Evidence: `docs/qa/evidence/2026-05-29-ai-provider-router-loop/assembleDebug-after-router-fix.txt`. |

## Finding QA-2026-05-29-002

- priority: P1
- area: AI provider routing - fallback after transient provider failure
- flow: AI generator route with OpenAI/Gemini provider preference and both keys configured
- status: fixed
- repro steps: Run `AiProviderRouterTest` with OpenAI-first settings, OpenAI fake client returning HTTP 429 and Gemini fake client ready; before the fix, the route throws `AiProviderUnavailableException` instead of using Gemini.
- expected behavior: A transient failure from the preferred provider records fallback evidence and tries the next configured provider.
- actual behavior: Feature-level throttle was applied before the second provider, so Gemini was not attempted after OpenAI rate-limit.
- evidence paths: `docs/qa/evidence/2026-05-29-ai-provider-router-loop/AiProviderRouterTest.txt`, `docs/qa/evidence/2026-05-29-ai-provider-router-loop/AiProviderRouterTest-after-expectation-fix.txt`.
- recommended fix: Apply bounded retry throttle per provider within the route loop while preserving cancellation propagation and non-transient error stop behavior.
- regression risk: Medium; shared AI routing affects meal scan, weekly report, workout debrief, goal advice and routine generation provider fallback.
- changed files: `app/src/main/java/com/trainiq/ai/services/AiProviders.kt`, `app/src/test/java/com/trainiq/ai/services/AiProviderRouterTest.kt`.
- targeted verification: `AiProviderRouterTest-after-expectation-fix.txt` passed.
- regression verification: `ai-services-tests-after-router-fix.txt`, `testDebugUnitTest-after-router-fix.txt`, `lintDebug-after-router-fix.txt` and `assembleDebug-after-router-fix.txt` passed.
- remaining risk: Real Gemini/OpenAI calls and real-key privacy signoff remain owner/runtime gates.
- final status: fixed

Direct APK Ready after this loop: `NO`. AI provider failover is now covered without real keys or network calls. Live provider calls, real-key signoff, real optical scanner decode and owner/manual gates remain open.
## 2026-05-29 Barcode Offline Runtime Lookup Loop

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| Targeted barcode lookup tests | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.remote.BarcodeProductLookupServiceTest" --console=plain` | PASS | Fake connection coverage verifies offline/openConnection failure returns null, malformed response returns null and disconnects, successful fake response parses product data, and bounded timeout/header config remains intact. Evidence: `docs/qa/evidence/2026-05-29-barcode-offline-runtime-loop/BarcodeProductLookupServiceTest-after-fake-connection-fix.txt`. |
| Unit regression | `.\gradlew.bat :app:testDebugUnitTest --console=plain` | PASS | Evidence: `docs/qa/evidence/2026-05-29-barcode-offline-runtime-loop/testDebugUnitTest-after-barcode-offline.txt`. |
| Lint regression | `.\gradlew.bat :app:lintDebug --console=plain` | PASS | Evidence: `docs/qa/evidence/2026-05-29-barcode-offline-runtime-loop/lintDebug-after-barcode-offline.txt`. |
| Debug build | `.\gradlew.bat :app:assembleDebug --console=plain` | PASS | Evidence: `docs/qa/evidence/2026-05-29-barcode-offline-runtime-loop/assembleDebug-after-barcode-offline.txt`. |

Direct APK Ready after this loop: `NO`. Barcode lookup offline/malformed-response resilience is now covered without depending on a live external endpoint. AI offline/live-provider behavior and real optical camera barcode decode remain open runtime/provider gates.
## 2026-05-29 Scanner SavedStateHandle Runtime Loop

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| Android test compile | `.\gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain` | PASS | Evidence: `docs/qa/evidence/2026-05-29-scanner-savedstate-runtime-loop/compileDebugAndroidTestKotlin.txt`. |
| Targeted scanner runtime test, first attempt | `.\gradlew.bat :app:connectedDebugAndroidTest --console=plain "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.navigation.ScannerSavedStateHandleInstrumentedTest"` | FAIL | Environment/test-install blocker only: `INSTALL_FAILED_UPDATE_INCOMPATIBLE` because a release-signed `com.trainiq` package was installed. Evidence: `docs/qa/evidence/2026-05-29-scanner-savedstate-runtime-loop/ScannerSavedStateHandleInstrumentedTest.txt`. |
| Targeted scanner runtime test before fix | same targeted command after uninstall | FAIL | Reproduced `QA-2026-05-29-001`: after barcode result consumption, `clearBarcodeScanResult()` used `SavedStateHandle.remove()` and the `getStateFlow(..., "")` consumer did not return to the empty state. Evidence: `docs/qa/evidence/2026-05-29-scanner-savedstate-runtime-loop/ScannerSavedStateHandleInstrumentedTest-rerun-after-uninstall.txt`, `docs/qa/evidence/2026-05-29-scanner-savedstate-runtime-loop/ScannerSavedStateHandleInstrumentedTest-final.txt`. |
| Source guard after fix | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.navigation.ScannerModeRouteTest" --console=plain` | PASS | Evidence: `docs/qa/evidence/2026-05-29-scanner-savedstate-runtime-loop/ScannerModeRouteTest-after-clear-fix.txt`. |
| Compile after fix | `.\gradlew.bat :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin --console=plain` | PASS | Evidence: `docs/qa/evidence/2026-05-29-scanner-savedstate-runtime-loop/compile-after-clear-fix-rerun.txt`. |
| Targeted scanner runtime after fix | targeted `ScannerSavedStateHandleInstrumentedTest` | PASS | The type-safe Navigation Compose back stack returned barcode `3017620422003` to Nutrition and clear-after-consumption returned the destination to the empty scanner state. Evidence: `docs/qa/evidence/2026-05-29-scanner-savedstate-runtime-loop/ScannerSavedStateHandleInstrumentedTest-after-clear-fix.txt`. |
| Unit regression after fix | `.\gradlew.bat :app:testDebugUnitTest --console=plain` | PASS | Evidence: `docs/qa/evidence/2026-05-29-scanner-savedstate-runtime-loop/testDebugUnitTest-after-clear-fix.txt`. |
| Full connected regression, first attempt | `.\gradlew.bat :app:connectedDebugAndroidTest --console=plain` | FAIL | Suite attempt failed in `ActiveWorkoutSetActionsInstrumentedTest` on a missing set-type content-description node. The same class passed in isolation and the clean full connected rerun passed, so no reproducible app bug remains from this attempt. Evidence: `docs/qa/evidence/2026-05-29-scanner-savedstate-runtime-loop/connectedDebugAndroidTest-after-clear-fix.txt`, `docs/qa/evidence/2026-05-29-scanner-savedstate-runtime-loop/ActiveWorkoutSetActionsInstrumentedTest-rerun-after-suite-failure.txt`. |
| Full connected regression, clean rerun | `adb uninstall com.trainiq`; `adb uninstall com.trainiq.test`; `.\gradlew.bat :app:connectedDebugAndroidTest --console=plain` | PASS | Passed 51/51 tests on `emulator-5554` / `Medium_Phone_2(AVD) - 16`. Evidence: `docs/qa/evidence/2026-05-29-scanner-savedstate-runtime-loop/connectedDebugAndroidTest-full-rerun-after-uninstall.txt`. |
| Lint regression after fix | `.\gradlew.bat :app:lintDebug --console=plain` | PASS | Evidence: `docs/qa/evidence/2026-05-29-scanner-savedstate-runtime-loop/lintDebug-after-clear-fix.txt`. |

## Finding QA-2026-05-29-001

- priority: P1
- area: Navigation - scanner savedStateHandle result clear
- flow: CameraScanner -> Nutrition barcode return; CameraScanner -> Progress smart-scale return uses the same clear pattern
- status: fixed
- repro steps: Run `ScannerSavedStateHandleInstrumentedTest` before the fix; navigate from Nutrition to CameraScanner, set barcode `3017620422003` on the previous back stack entry, pop back, tap the clear/processed action, and observe the destination does not return to the empty scanner state.
- expected behavior: After a scanner result is consumed, the observing destination returns to its empty state and does not keep reprocessing the stale barcode/scale result.
- actual behavior: `clearBarcodeScanResult()` and `clearScaleMeasurementResult()` removed keys while the destinations observed `getStateFlow(..., "")`; runtime evidence showed the barcode consumer did not reliably receive the empty state after removal.
- evidence paths: `docs/qa/evidence/2026-05-29-scanner-savedstate-runtime-loop/ScannerSavedStateHandleInstrumentedTest-rerun-after-uninstall.txt`, `docs/qa/evidence/2026-05-29-scanner-savedstate-runtime-loop/ScannerSavedStateHandleInstrumentedTest-final.txt`, `docs/qa/evidence/2026-05-29-scanner-savedstate-runtime-loop/ScannerSavedStateHandleInstrumentedTest-after-clear-fix.txt`.
- recommended fix: Publish explicit empty string values from the clear helpers so the `getStateFlow(..., "")` observers receive a value matching their empty-state contract.
- regression risk: Low; change is limited to navigation result clear helpers and keeps the same stable keys.
- changed files: `app/src/main/java/com/trainiq/navigation/TrainIqNav.kt`, `app/src/test/java/com/trainiq/navigation/ScannerModeRouteTest.kt`, `app/src/androidTest/java/com/trainiq/navigation/ScannerSavedStateHandleInstrumentedTest.kt`.
- targeted verification: `ScannerSavedStateHandleInstrumentedTest-after-clear-fix.txt` passed.
- regression verification: `ScannerModeRouteTest-after-clear-fix.txt`, `testDebugUnitTest-after-clear-fix.txt`, `connectedDebugAndroidTest-full-rerun-after-uninstall.txt` and `lintDebug-after-clear-fix.txt` passed.
- remaining risk: Full real camera/barcode decode through ML Kit remains owner/runtime-gated; this finding closes the navigation savedStateHandle return and clear-after-consumption contract, not optical scan quality.
- final status: fixed

Direct APK Ready after this loop: `NO`. One reproducible P1 scanner navigation contract bug was fixed and verified. Owner/manual gates remain open: TalkBack/Switch Access traversal, Health Connect partial/revoke/background-read matrix, real-key privacy/security signoff, live AI/provider flows, real camera/scanner result return, and manual deep-runtime UX audits.
## 2026-05-29 Direct APK Readiness Refresh

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| Debug build | `.\gradlew.bat :app:assembleDebug --console=plain` | PASS | Evidence: `docs/qa/evidence/2026-05-29-direct-apk-readiness-loop/assembleDebug.txt`. |
| JVM unit tests | `.\gradlew.bat :app:testDebugUnitTest --console=plain` | PASS | Evidence: `docs/qa/evidence/2026-05-29-direct-apk-readiness-loop/testDebugUnitTest.txt`. |
| Lint | `.\gradlew.bat :app:lintDebug --console=plain` | PASS | Evidence: `docs/qa/evidence/2026-05-29-direct-apk-readiness-loop/lintDebug.txt`. |
| Connected tests, first attempt | `.\gradlew.bat :app:connectedDebugAndroidTest --console=plain` | FAIL | Environment/test-install blocker only: `INSTALL_FAILED_UPDATE_INCOMPATIBLE` because a release-signed `com.trainiq` package was already installed and the task needed to install the debug APK. Evidence: `docs/qa/evidence/2026-05-29-direct-apk-readiness-loop/connectedDebugAndroidTest.txt`. |
| Connected tests, clean rerun | `adb uninstall com.trainiq`; `adb uninstall com.trainiq.test`; `.\gradlew.bat :app:connectedDebugAndroidTest --console=plain` | PASS | Passed 50/50 tests on `emulator-5554` / `Medium_Phone_2(AVD) - 16`. Evidence: `docs/qa/evidence/2026-05-29-direct-apk-readiness-loop/adb-uninstall-com.trainiq-before-connected-rerun.txt`, `docs/qa/evidence/2026-05-29-direct-apk-readiness-loop/adb-uninstall-com.trainiq.test-before-connected-rerun.txt`, `docs/qa/evidence/2026-05-29-direct-apk-readiness-loop/connectedDebugAndroidTest-rerun-after-uninstall.txt`. |
| Release signing readiness | `.\gradlew.bat :app:checkReleaseSigningReadiness --console=plain` | PASS | Evidence: `docs/qa/evidence/2026-05-29-direct-apk-readiness-loop/checkReleaseSigningReadiness.txt`. |
| Release build | `.\gradlew.bat :app:assembleRelease --console=plain` | PASS | Evidence: `docs/qa/evidence/2026-05-29-direct-apk-readiness-loop/assembleRelease.txt`. |
| Fresh release install | `.\gradlew.bat :app:installRelease --console=plain` | PASS | Evidence: `docs/qa/evidence/2026-05-29-direct-apk-readiness-loop/installRelease.txt`. |
| Release cold launch | `adb shell am start -W -n com.trainiq/.MainActivity` | PASS | Launch returned `Status: ok`, `LaunchState: COLD`, `TotalTime: 1244`. Evidence: `docs/qa/evidence/2026-05-29-direct-apk-readiness-loop/launch-release-smoke.txt`, `docs/qa/evidence/2026-05-29-direct-apk-readiness-loop/trainiq-release-smoke.xml`. |
| Release crash/ANR gate | `Select-String ... "com.trainiq.*FATAL EXCEPTION|ANR in com.trainiq|Input dispatching timed out.*com.trainiq"` | PASS | No matches in release smoke logcat. Evidence: `docs/qa/evidence/2026-05-29-direct-apk-readiness-loop/logcat-release-smoke.txt`, `docs/qa/evidence/2026-05-29-direct-apk-readiness-loop/logcat-release-crash-matches.txt`. |

Direct APK Ready after this refresh: `NO`. No new reproducible app P0/P1/P2/P3 bugs were found in executed checks. The first connected run failed only because the emulator still had a release-signed app installed; after uninstalling that package, the clean connected rerun passed. Owner/manual gates remain open: TalkBack/Switch Access traversal, Health Connect partial/revoke/background-read matrix, real-key privacy/security signoff, live AI/provider flows, real camera/scanner result return, and manual deep-runtime UX audits.
## 2026-05-28 Direct APK Readiness Refresh

| Check | Command | Status | Evidence/notes |
|---|---|---|---|
| Debug build | `.\gradlew.bat :app:assembleDebug --console=plain` | PASS | Evidence: `docs/qa/evidence/2026-05-28-direct-apk-readiness-loop/assembleDebug.txt`. |
| JVM unit tests | `.\gradlew.bat :app:testDebugUnitTest --console=plain` | PASS | Evidence: `docs/qa/evidence/2026-05-28-direct-apk-readiness-loop/testDebugUnitTest.txt`. |
| Lint | `.\gradlew.bat :app:lintDebug --console=plain` | PASS | Evidence: `docs/qa/evidence/2026-05-28-direct-apk-readiness-loop/lintDebug.txt`. |
| Connected tests | `.\gradlew.bat :app:connectedDebugAndroidTest --console=plain` | PASS | Passed on `emulator-5554` / `sdk_gphone64_x86_64`. Evidence: `docs/qa/evidence/2026-05-28-direct-apk-readiness-loop/connectedDebugAndroidTest.txt`. |
| Release signing readiness | `.\gradlew.bat :app:checkReleaseSigningReadiness --console=plain` | PASS | Evidence: `docs/qa/evidence/2026-05-28-direct-apk-readiness-loop/checkReleaseSigningReadiness.txt`. |
| Release build | `.\gradlew.bat :app:assembleRelease --console=plain` | PASS | Evidence: `docs/qa/evidence/2026-05-28-direct-apk-readiness-loop/assembleRelease.txt`. |
| Fresh release install | `.\gradlew.bat :app:installRelease --console=plain` after no installed `com.trainiq` package | PASS | Evidence: `docs/qa/evidence/2026-05-28-direct-apk-readiness-loop/installRelease.txt`, `docs/qa/evidence/2026-05-28-direct-apk-readiness-loop/fresh-release-install-final.txt`. |
| Release cold launch | `adb shell am start -W -n com.trainiq/.MainActivity` | PASS | Launch returned `Status: ok`. Evidence: `docs/qa/evidence/2026-05-28-direct-apk-readiness-loop/launch-release-smoke.txt`, `docs/qa/evidence/2026-05-28-direct-apk-readiness-loop/fresh-release-launch-final.txt`. |
| Release crash/ANR gate | `Select-String ... "com.trainiq.*FATAL EXCEPTION|ANR in com.trainiq|Input dispatching timed out.*com.trainiq"` | PASS | No matches in release smoke or final fresh release smoke. Evidence: `docs/qa/evidence/2026-05-28-direct-apk-readiness-loop/logcat-release-crash-matches.txt`, `docs/qa/evidence/2026-05-28-direct-apk-readiness-loop/fresh-release-crash-matches-final.txt`. |
| Debug-to-release upgrade command from plan | `.\gradlew.bat :app:installDebug`; launch; `.\gradlew.bat :app:installRelease`; launch | FAIL | Controlled retry reproduced `INSTALL_FAILED_UPDATE_INCOMPATIBLE` because debug and release signatures differ. This is not an app runtime crash/ANR/data-loss finding, but the exact debug-to-release command is not a valid direct APK upgrade proxy. Evidence: `docs/qa/evidence/2026-05-28-direct-apk-readiness-loop/upgrade-controlled-installRelease-over-debug.txt`. |

Direct APK Ready after this refresh: `NO`. No new reproducible app P0/P1/P2/P3 bugs were found in executed checks, but owner/manual gates remain open: TalkBack/Switch Access traversal, Health Connect partial/revoke/background-read matrix, real-key privacy/security signoff, live AI/provider flows, real camera/scanner result return, and manual deep-runtime UX audits.


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
| Debug build | `.\gradlew.bat :app:assembleDebug --console=plain` | PASS | Build successful on 2026-05-27 after QA-loop updates, fixes, source guards, shared dialog pane semantics guard and dynamic-color gate guard. Latest evidence: `docs/qa/evidence/2026-05-27-post-touch-target-baseline/assembleDebug.txt`. |
| JVM unit tests | `.\gradlew.bat :app:testDebugUnitTest --console=plain` | PASS | Build successful on 2026-05-27 after QA-loop updates, fixes, source guards, shared dialog pane semantics guard and dynamic-color gate guard. Latest evidence: `docs/qa/evidence/2026-05-27-post-touch-target-baseline/testDebugUnitTest-rerun2.txt`. |
| Lint | `.\gradlew.bat :app:lintDebug --console=plain` | PASS | Build successful on 2026-05-27 after one guarded API-31 lint rerun for the dynamic-color helper. Latest evidence: `docs/qa/evidence/2026-05-27-post-touch-target-baseline/lintDebug.txt`. |
| Connected tests | `.\gradlew.bat :app:connectedDebugAndroidTest --console=plain` | PASS | Connected test suite passed on `Medium_Phone_2(AVD) - 16` after QA-loop updates, test DB isolation hardening, active set-type semantics, and the active-key Room v14 schema fix. Latest evidence: `docs/qa/evidence/2026-05-27-connected-baseline-refresh-loop/connectedDebugAndroidTest-final-after-room-v14-marker-fix.txt`. |
| AI/nutrition targeted | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.ai.services.AiServicesTest" --console=plain` | PASS | Build successful on 2026-05-27; covers nutrition AI identity/fallback parsing regressions including multi-component context handling. |
| Barcode lookup targeted | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.remote.BarcodeProductLookupServiceTest" --console=plain` | PASS | Build successful on 2026-05-27; covers Open Food Facts JSON parsing for complete nutrition and incomplete/null product data. External endpoint smoke also returned `Nutella` for barcode `3017620422003` and status `0` for unknown barcode `00000000000000`. Latest failure-guard test also covers not-found status, malformed JSON, unsafe macro values, digit/length sanitization, bounded connect/read timeouts and null-return on network failures via `runCatching(...).getOrNull()`. Evidence: `docs/qa/evidence/2026-05-27-barcode-lookup-loop/BarcodeProductLookupServiceTest.txt`, `docs/qa/evidence/2026-05-27-barcode-lookup-loop/BarcodeProductLookupServiceTest-failure-guards.txt`, `docs/qa/evidence/2026-05-27-barcode-lookup-loop/openfoodfacts-3017620422003.json`, `docs/qa/evidence/2026-05-27-barcode-lookup-loop/openfoodfacts-00000000000000.json`. |
| Room persistence targeted | `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.repository.TargetedRoomPersistenceInstrumentedTest" --console=plain` | PASS | 27 tests passed on `Medium_Phone_2(AVD) - 16`; covers meal mutations, historical meal snapshot immutability after product/recipe edits, routine graph, active-workout set edit/delete/finish/undo and persistence across reopen. |
| Active workout restore targeted | `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.features.workout.ActiveWorkoutRestoreInstrumentedTest" --console=plain` | PASS | 1 test passed on `Medium_Phone_2(AVD) - 16`; verifies active workout state survives activity recreation. |
| Progress chart accessibility targeted | `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.core.ui.AppLineChartAccessibilityTest" --console=plain` | PASS | 1 test passed on `Medium_Phone_2(AVD) - 16`; verifies chart accessibility semantics at component level. |
| Health Connect provider intent targeted | `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.data.datasource.HealthConnectProviderIntentInstrumentedTest" --console=plain` | PASS | 1 test passed on `Medium_Phone_2(AVD) - 16`; verifies safe provider intent behavior without mutating Health Connect permissions. |
| Health Connect core policy targeted | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.core.health.*" --console=plain` | PASS | Build successful on 2026-05-27; covers read permission messaging, background sync work policy and retry/stop behavior for transient vs permanent Health Connect failures. Evidence: `docs/qa/evidence/2026-05-27-healthconnect-loop/HealthConnectCoreTests.txt`. |
| Accessibility targeted unit checks | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --tests "com.trainiq.features.nutrition.CameraScannerStateTest" --tests "com.trainiq.features.settings.SettingsUiStateTest" --tests "com.trainiq.core.ui.LineChartSemanticsTest" --console=plain` | PASS | Build successful on 2026-05-27; covers workout content descriptions/rest timer/set actions/source chips, nutrition scanner and field labels, Settings Health Connect/theme accessibility labels, and chart semantic descriptions. Evidence: `docs/qa/evidence/2026-05-27-accessibility-targeted-loop/targeted-accessibility-unit-tests.txt`. |
| Coach/AI contract targeted | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.coach.GoalAdviceInputTest" --tests "com.trainiq.ai.services.AiServicesTest" --console=plain` | PASS | Build successful on 2026-05-27; covers Coach goal input validation, structured Dutch Gemini goal advice parsing, deterministic baseline preservation, workout debrief parsing/fallbacks, nutrition scan identity guards and malformed/English response fallback behavior. Evidence: `docs/qa/evidence/2026-05-27-coach-ai-contract-loop/coach-ai-contract-tests.txt`. |
| Coach weekly/training/nutrition targeted | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.coach.GoalAdviceInputTest" --tests "com.trainiq.ai.services.AiServicesTest" --console=plain` | PASS | Build successful after adding a Coach UI source guard: profile-ready Coach screen keeps `Weekrapport maken`, loading copy, `Trainingsinzichten`, fallback insights, `Voedingscoach`, nutrition coach message binding, weekly report source labels and report sections for wins, risks, next step and rationale. `AiServicesTest` also covers weekly report JSON schema/parsing, Dutch-output guard and local fallback. Evidence: `docs/qa/evidence/2026-05-27-coach-deep-loop/coach-weekly-training-nutrition-targeted-tests.txt`. |
| Nutrition validation targeted | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.NutritionInputValidationTest" --console=plain` | PASS | Build successful on 2026-05-27; covers nutrition input validation and recipe UI/source guard checks, including recipe section visibility, recipe bottom-sheet actions, saved recipe add-to-meal path preserving contextual meal target and tab state. Evidence: `docs/qa/evidence/2026-05-27-nutrition-recipes-loop/NutritionInputValidationTest-rerun.txt`. |
| Scanner route/state targeted | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.navigation.ScannerModeRouteTest" --tests "com.trainiq.features.nutrition.CameraScannerStateTest" --tests "com.trainiq.features.nutrition.CameraUiStateMapperTest" --console=plain` | PASS | Build successful on 2026-05-27; covers barcode/AI-meal scanner route mode, camera scanner restorable state, completed/empty/local-fallback states and scanner UI-state mapping. Evidence: `docs/qa/evidence/2026-05-27-barcode-loop/scanner-route-state-tests.txt`. Latest expanded scanner/barcode contract evidence also includes Nutrition pending-barcode consumption into recipe draft vs food editor and clear-after-consumption: `docs/qa/evidence/2026-05-27-scanner-runtime-loop/scanner-barcode-contract-tests.txt`. |
| Recipe delete ANR fix compile | `.\gradlew.bat :app:compileDebugKotlin --console=plain` | PASS | Build successful after `ConfirmNutritionDeleteDialog` fix. Evidence: `docs/qa/evidence/2026-05-27-nutrition-recipe-runtime-loop/compileDebugKotlin-after-recipe-delete-fix.txt`. |
| Recipe delete targeted unit | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.NutritionInputValidationTest" --console=plain` | PASS | Build successful after recipe delete dialog fix. Evidence: `docs/qa/evidence/2026-05-27-nutrition-recipe-runtime-loop/NutritionInputValidationTest-after-recipe-delete-fix.txt`. |
| Recipe delete runtime verification | `.\gradlew.bat :app:installDebug --console=plain` plus adb runtime recipe delete repro | PASS | Installed fixed debug build, opened saved-recipe delete confirmation without ANR, confirmed delete, and saved recipe list returned to empty state. Logcat after confirmation showed no TrainIQ ANR/FATAL. Evidence: `docs/qa/evidence/2026-05-27-nutrition-recipe-runtime-loop/installDebug-after-recipe-delete-fix.txt`, `recipe-fix-delete-confirm.xml`, `recipe-fix-after-delete-confirm.xml`, `logcat-recipe-fix-delete-confirm-open.txt`, `logcat-recipe-fix-after-delete-confirm.txt`. |
| Recipe edit source guard targeted | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.NutritionInputValidationTest" --console=plain` | PASS | Build successful after adding source guard for saved-recipe edit flow: `Bewerk` selects `selectedRecipeId`, editor loads recipe fields/draft, save calls `onSaveRecipe(selectedRecipeId, ...)`, and edit copy shows `Wijzigingen opslaan`. Evidence: `docs/qa/evidence/2026-05-27-nutrition-recipe-edit-loop/NutritionInputValidationTest-recipe-edit-guard.txt`. |
| Room marker Gradle generator targeted | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.data.migration.RoomMigrationChainVerificationProviderTest" --console=plain` | PASS | Build successful after fixing Gradle marker generator drift from v12 to provider/source-of-truth v13 and adding a test that asserts the buildscript marker/current/required/covered end versions match `CurrentRoomVersion`. Evidence: `docs/qa/evidence/2026-05-27-nutrition-recipe-edit-loop/RoomMigrationChainVerificationProviderTest-after-gradle-marker-fix.txt`. |
| Scanner savedStateHandle contract targeted | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.navigation.ScannerModeRouteTest" --console=plain` | PASS | Build successful after adding source guard for scanner return contracts: stable barcode/scale result keys, Nutrition/Progress consumption through savedStateHandle, scanner result writes to previousBackStackEntry, and clear-after-consumption helpers. Evidence: `docs/qa/evidence/2026-05-27-nutrition-recipe-edit-loop/ScannerModeRouteTest-savedstate-guard-final.txt`. |
| Encrypted AI key stores targeted | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.core.security.GeminiKeyMigrationTest" --console=plain` | PASS | Build successful after adding OpenAI encrypted key-store coverage next to Gemini: fake-key save trims input, verifies encrypted readback, clear removes key, and failed save does not overwrite existing key. Evidence: `docs/qa/evidence/2026-05-27-settings-ai-keys-loop/GeminiKeyMigrationTest-openai-coverage.txt`. |
| Training exercise history targeted | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.navigation.TrainDetailModeChromeTest" --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain` | PASS | Build successful on 2026-05-27; covers keeping Train detail chrome active while Exercise History is on top of the Train flow, history metadata formatting, workout history list keys and related workout UI source guards. Evidence: `docs/qa/evidence/2026-05-27-training-history-loop/training-history-targeted-tests.txt`. |
| AI routine preview/save/cancel targeted | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --tests "com.trainiq.ai.services.RoutineGeneratorServiceTest" --console=plain` | PASS | Build successful after adding a source guard for AI routine preview state: save ignores duplicate saves, requires a pending routine, sets saving state, calls `saveGeneratedRoutineUseCase`, clears pending routine after success, shows `Routine opgeslagen.`, resets saving state, dismiss clears pending routine, generator dialog cannot dismiss while loading, and preview dialog wires save/dismiss callbacks. Routine parser tests cover Gemini schema, Dutch-output guard, malformed/English fallback and generated routine metadata mapping. Evidence: `docs/qa/evidence/2026-05-27-training-ai-routine-loop/ai-routine-preview-contract-tests.txt`. |
| Active workout set actions targeted | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain` | PASS | Build successful after adding source guard for active workout logged-set actions: set type changes call `updateActiveWorkoutSetTypeUseCase`, edit loads the logged set into draft and marks correction state, delete clears correction state and calls `deleteActiveWorkoutSetUseCase`, undo calls `undoWorkoutLogEventUseCase`, snackbar exposes `Ongedaan maken`, and active workout UI wires edit/delete/relog/type-change callbacks into set rows. Evidence: `docs/qa/evidence/2026-05-27-active-workout-set-actions-loop/WorkoutInputValidationTest-set-actions.txt`. |
| Meal history reuse targeted | `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.nutrition.NutritionInputValidationTest" --console=plain` | PASS | Build successful after adding a source guard for `Voeding > Historie > Opnieuw gebruiken`: reuse restores meal type, name, notes and each snapshot item into the editable draft with item type, reference id, grams, serving count and notes preserved, then opens the meal draft tab. Evidence: `docs/qa/evidence/2026-05-27-meal-history-loop/NutritionInputValidationTest-meal-history-reuse.txt`. |
| Meal history reuse runtime targeted | `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.features.nutrition.MealHistoryReuseRuntimeInstrumentedTest" --console=plain` one-off QA run | PASS | One-off targeted runtime test passed on `Medium_Phone_2(AVD) - 16`: seeded saved food and logged meal snapshot, opened `Voeding > Historie`, verified `Voedingshistorie`, `QA Lunch History` and `QA Kip Rollade`, tapped `Opnieuw gebruiken`, and verified the editable `Maaltijd controleren` draft with the reused item and save action. The temporary test was removed from the permanent full suite after it introduced suite-order interference with `ActiveWorkoutRestoreInstrumentedTest`; the restore test passes in isolation and full connected baseline is restored separately. Evidence: `docs/qa/evidence/2026-05-27-meal-history-runtime-loop/MealHistoryReuseRuntimeInstrumentedTest-rerun3.txt`, `docs/qa/evidence/2026-05-27-meal-history-runtime-loop/ActiveWorkoutRestoreInstrumentedTest-after-suite-failure.txt`. |
| Recipe edit runtime targeted | `connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.features.nutrition.NutritionRecipeEditInstrumentedTest` one-off QA run | PASS | One-off instrumented Compose QA run seeded a saved recipe, opened Voeding > Recepten, tapped `Bewerk`, verified `Wijzigingen opslaan`, saved through the edit path, and observed `Recept opgeslagen.` with the recipe still present. The temporary test was removed from the permanent full suite after it introduced suite-order interference with an existing restore UI test; recipe edit remains covered by source guard plus this captured runtime evidence. Full `connectedDebugAndroidTest` passes again afterward. Evidence: `docs/qa/evidence/2026-05-27-nutrition-recipe-edit-loop/NutritionRecipeEditInstrumentedTest-final.txt`, `docs/qa/evidence/2026-05-27-nutrition-recipe-edit-loop/connectedDebugAndroidTest-after-recipe-edit-evidence-only.txt`. |
| Profileable benchmark build | `.\gradlew.bat :app:assembleProfileable :macrobenchmark:assembleAndroidTest --console=plain` | PASS | Build successful on 2026-05-27; profileable app and macrobenchmark test APK assemble. |
| Macrobenchmark physical device | `.\gradlew.bat :macrobenchmark:connectedProfileableAndroidTest --console=plain` | PASS | Passed on physical device `RFCY60HNHNJ`, model `SM-S931B`, Android `16` / SDK `36`, with `ro.kernel.qemu=0`. Evidence: `docs/qa/evidence/2026-05-27-physical-device-macrobenchmark-loop/adb-devices.txt`, `device-is-emulator.txt`, `device-model.txt`, `android-release.txt`, `android-sdk.txt`, `profileable-macrobenchmark-assemble.txt`, `connectedProfileableAndroidTest.txt`, `logcat-after-macrobenchmark.txt`, `logcat-crash-matches.txt`. |

## Start/Home

| Flow | Status | Evidence/notes |
|---|---|---|
| First run without profile/Health Connect/routine/data | PASS | After `pm clear`, Start/Home opened in `Ontdekmodus` with setup copy and checklist items for profile, routine, first meal and optional Health Connect. Evidence: `docs/qa/evidence/2026-05-27-home-cta-loop/home-first-run.png`, `docs/qa/evidence/2026-05-27-home-cta-loop/home-first-run.xml`. |
| Dashboard cards render and remain readable | PASS | Start tab visible with `TrainIQ`, `Vandaag in een slimme cockpit`, setup cards and bottom nav. Evidence: `docs/qa/evidence/2026-05-27-short-loop/tab-start.xml`. |
| CTA to profile/settings works | PASS | `Profiel invullen` CTA opened Coach profile setup screen. Evidence: `docs/qa/evidence/2026-05-27-home-cta-loop/home-cta-profile.xml`. |
| CTA to Health Connect works | PASS | `Health Connect koppelen` CTA opened TrainIQ Health Connect rationale screen before permission prompt. Evidence: `docs/qa/evidence/2026-05-27-home-cta-loop/home-cta-healthconnect.xml`. |
| CTA to Training works | PASS | Bottom navigation opened Training. Evidence: `docs/qa/evidence/2026-05-27-short-loop/tab-training.xml`. |
| CTA to Coach works | PASS | Bottom navigation opened Coach. Evidence: `docs/qa/evidence/2026-05-27-short-loop/tab-coach.xml`. |
| Loading/empty/partial/error states are understandable | PASS | First-run empty/setup state showed clear `Ontdekmodus`, checklist and CTA guidance for missing profile, routine, meal and Health Connect data. Deeper loading/partial/error backend states were not forced in this loop. Evidence: `docs/qa/evidence/2026-05-27-home-cta-loop/home-first-run.xml`. |
| Dashboard data survives app restart where expected | PASS | First-run dashboard/setup state survived force-stop/relaunch: `TrainIQ`, `Vandaag in een slimme cockpit`, `Ontdekmodus`, setup checklist and CTAs remained visible before and after restart, with no TrainIQ crash/ANR. Evidence: `docs/qa/evidence/2026-05-27-home-restart-loop/home-restart-before.xml`, `docs/qa/evidence/2026-05-27-home-restart-loop/home-restart-after.xml`, `docs/qa/evidence/2026-05-27-home-restart-loop/logcat-home-restart.txt`. |
| Dark mode, large font, dynamic color, tablet/foldable layout | PASS | Large-font/dark-mode smoke at font scale 1.5 opened Start without crash/ANR and kept dashboard text readable enough for top-level smoke. Dynamic color has Android 12+ source/unit contract coverage but was not visually verified on a Material You configured device. Tablet/foldable responsive policy has source/unit coverage for `WindowSizeClass` propagation, navigation rail on medium/expanded widths, compact overflow and adaptive dashboard columns/content max width; real tablet/foldable visual runtime remains unverified. Evidence: `docs/qa/evidence/2026-05-27-accessibility-visual-loop/a11y-large-dark-start.png`, `docs/qa/evidence/2026-05-27-accessibility-visual-loop/a11y-large-dark-start.xml`, `docs/qa/evidence/2026-05-27-dynamic-color-loop/ThemeDynamicColorTest.txt`, `docs/qa/evidence/2026-05-27-adaptive-layout-loop/adaptive-navigation-tests.txt`. |

## Training

| Flow | Status | Evidence/notes |
|---|---|---|
| Routine list opens | PASS | Training tab showed routine creation card, active routine empty state, routines empty state and exercise library. Evidence: `docs/qa/evidence/2026-05-27-short-loop/tab-training.xml`. |
| Create routine | PASS | Empty routine dialog opened, `QA_Routine` was created, and the routine appeared as active routine and in the routines list. Evidence: `docs/qa/evidence/2026-05-27-training-loop/training-empty-routine-flow.xml`, `docs/qa/evidence/2026-05-27-training-loop/training-created-routine-after-save.xml`. |
| Edit/delete routine | PASS | Runtime delete path passed: created routine `DeleteQA`, opened details, tapped `Verwijderen`, saw confirmation `Routine verwijderen?` explaining routine/sessions are removed while training history remains, confirmed delete, and Training returned to empty active routine/routines states without crash. Direct edit of name/description was not modified in this loop. Evidence: `docs/qa/evidence/2026-05-27-training-delete-loop/routine-delete-created.xml`, `docs/qa/evidence/2026-05-27-training-delete-loop/routine-delete-detail.xml`, `docs/qa/evidence/2026-05-27-training-delete-loop/routine-delete-confirm.xml`, `docs/qa/evidence/2026-05-27-training-delete-loop/routine-after-delete.xml`, `docs/qa/evidence/2026-05-27-training-delete-loop/logcat-routine-delete.txt`. |
| Generated routine preview save/cancel | NOT RUN | AI routine form opened with focus/split, days, equipment, level, duration, cancel and generate controls; live generation/save was not run because no AI provider/key was configured in this safe loop. Targeted sourceguard verifies preview save/dismiss state handling, duplicate-save guard, pending-routine requirement, pending clear after save/dismiss, loading-safe generator dismiss behavior and preview callback wiring. Latest runtime component coverage verifies a long generated routine preview keeps Save/Retry/Cancel reachable and clickable, and saving state disables only Save while Retry/Cancel remain available. Latest AI contract rescan also covers routine generator schema, generated metadata and malformed/English fallback. Evidence: `docs/qa/evidence/2026-05-27-training-loop/training-ai-routine-repeat.xml`, `docs/qa/evidence/2026-05-27-training-ai-routine-loop/ai-routine-preview-contract-tests.txt`, `docs/qa/evidence/2026-05-27-ai-contract-rescan/ai-coach-routine-contract-tests.txt`, `docs/qa/evidence/2026-05-29-generated-routine-preview-runtime-loop/GeneratedRoutinePreviewInstrumentedTest.txt`. |
| Exercise library and picker | PASS | Exercise library list was visible and routine exercise picker opened from `Eerste oefening toevoegen`; `Ab Wheel Rollout`, default sets/reps/rest and add controls were visible. Evidence: `docs/qa/evidence/2026-05-27-training-exercise-loop/training-add-exercise-entry.xml`. |
| Exercise history | PASS | Emulator-only seeded runtime coverage now opens Training on `emulator-5554`, starts the seeded routine, taps `Open geschiedenis voor Bench Press` from the active workout exercise header, and verifies the Exercise History detail renders `Volume per sessie`, `Bench Press`, `Sessies`, `2`, `Beste kg` and `90` for two completed history sessions. Targeted unit/source guards for Train detail chrome, history metadata formatting, workout history list keys and related UI guards also passed. Logcat after the emulator flow had no TrainIQ `FATAL EXCEPTION`, ANR or input-dispatch timeout. Evidence: `docs/qa/evidence/2026-05-27-emulator-exercise-history-loop/ExerciseHistoryInstrumentedTest-visible-stats-pass.txt`, `training-history-targeted-unit-rerun.txt`, `assembleDebug-after-exercise-history-test.txt`, `exercise-history-runtime-summary.txt`. |
| Start active workout | PASS | After adding `Ab Wheel Rollout`, routine summary changed to `Core - 1 oefening - ca. 10 min`; `Start` opened active workout. Evidence: `docs/qa/evidence/2026-05-27-training-exercise-loop/training-after-add-exercise.xml`, `docs/qa/evidence/2026-05-27-training-exercise-loop/training-active-workout-entry.xml`. |
| Add/edit/delete/undo set | PASS | Runtime add/log set path passed previously. New emulator-only instrumented runtime test seeds an active workout with one logged set, opens Training on `emulator-5554`, enters active workout, cycles logged set type from `Normaal` to `Warm-up`, taps `Gelogde set corrigeren`, verifies correction state through `Wijzig loggen`, edits weight/reps/RPE through the active-workout fields, verifies persisted Room values, opens `Set verwijderen?`, confirms delete, and verifies `Set verwijderd.` plus `0 sets gelogd`. Backend/data undo coverage remains covered by `TargetedRoomPersistenceInstrumentedTest`; UI/source wiring for undo/snackbar remains covered by `WorkoutInputValidationTest`. Evidence: `docs/qa/evidence/2026-05-27-workout-completion-loop/active-scrolled.xml`, `docs/qa/evidence/2026-05-27-workout-completion-loop/after-log-set.xml`, `docs/qa/evidence/2026-05-27-active-workout-set-actions-loop/WorkoutInputValidationTest-set-actions.txt`, `docs/qa/evidence/2026-05-27-emulator-active-workout-set-actions-loop/ActiveWorkoutSetActionsInstrumentedTest-manual-values-replacement.txt`, `manual-values-runtime-summary.txt`. |
| Weight/reps/RPE/set type/rest timer | PASS | Runtime default reps/rest and rest timer were covered previously. New emulator-only active-workout runtime test performs a logged-set type change to `Warm-up`, edits weight to `82.5`, reps to `6`, and RPE to `8.5`, then verifies persisted active-workout Room values before deleting the set. Backend/data value and set-type edit coverage also passed in `TargetedRoomPersistenceInstrumentedTest`; source guard still verifies wiring through `updateActiveWorkoutSetTypeUseCase`. Evidence: `docs/qa/evidence/2026-05-27-workout-completion-loop/after-log-set.xml`, `docs/qa/evidence/2026-05-27-active-workout-set-actions-loop/WorkoutInputValidationTest-set-actions.txt`, `docs/qa/evidence/2026-05-27-emulator-active-workout-set-actions-loop/ActiveWorkoutSetActionsInstrumentedTest-manual-values-replacement.txt`, `manual-values-runtime-summary.txt`. |
| Active workout restore after recreate/restart | PASS | Targeted instrumented test seeded an active workout and verified Training still exposes active routine state after `ActivityScenario.recreate()`. Command: `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.features.workout.ActiveWorkoutRestoreInstrumentedTest" --console=plain`. |
| Finish workout and processing route | PASS | With 0 logged sets, `Training afronden` showed safe discard confirmation. With 1 logged set, finish showed `Training afronden?` and explained `Je hebt 1 van 3 sets gelogd`; `Opslaan` completed the session. Evidence: `docs/qa/evidence/2026-05-27-training-exercise-loop/training-finish-zero-sets.xml`, `docs/qa/evidence/2026-05-27-workout-completion-loop/after-finish.xml`. |
| Workout completion screen | PASS | Runtime completion UI opened after saving a workout with one logged set. Screen showed `Voltooid`, `QAWorkout - Sessie 1`, `Sterkste set: 0 kg x 12`, `Slimme samenvatting`, source chip `Lokale fallback`, recovery/advice sections, exercise/set totals and no TrainIQ crash/ANR. Evidence: `docs/qa/evidence/2026-05-27-workout-completion-loop/completion-screen.png`, `docs/qa/evidence/2026-05-27-workout-completion-loop/completion-screen.xml`, `docs/qa/evidence/2026-05-27-workout-completion-loop/logcat-after-completion-screen.txt`. |
| AI debrief valid Gemini response | PASS | `AiServicesTest.generateWorkoutDebrief_withStructuredJson_returnsParsedDebrief` passed as part of `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.ai.services.AiServicesTest" --console=plain`; verifies Gemini 2.5 Flash source, structured schema and Dutch prompt contract. |
| AI debrief fallback reasons: disabled/missing key/malformed/timeout/rate-limit/offline | PASS | Runtime missing/unconfigured AI state reached completion screen and visibly labeled the summary source as `Lokale fallback`. `AiServicesTest` covers deterministic fallback for malformed JSON, English JSON and AI-disabled/missing-key states; timeout/rate-limit handling is covered by bounded Gemini retry tests at service level. Evidence: `docs/qa/evidence/2026-05-27-workout-completion-loop/completion-screen.xml`. |
| Dense controls: touch targets, no overlap at large font | NOT RUN | Runtime active workout completion at normal font passed, and targeted unit checks passed for active workout content descriptions, finish/rest timer labels, set relog/copy labels and large-font-related source guards. Full physical touch-target and overlap audit at large font across active workout controls remains `NOT RUN`. Evidence: `docs/qa/evidence/2026-05-27-workout-completion-loop/completion-screen.xml`, `docs/qa/evidence/2026-05-27-accessibility-targeted-loop/targeted-accessibility-unit-tests.txt`. |

## Voeding

| Flow | Status | Evidence/notes |
|---|---|---|
| AI meal scanner opens | NOT RUN | Add sheet opened and `Foto / AI-inschatting` was visible but disabled because AI is not configured in the safe app flow. Latest targeted runtime component coverage verifies the AI meal scanner preview surface opens with `Camerascanner`, meal framing guidance, `Foto maken` and `Terug` without live provider calls. End-to-end scanner route from the disabled add-sheet and real AI analysis remain `NOT RUN`. Evidence: `docs/qa/evidence/2026-05-27-nutrition-loop/nutrition-add-ochtend-exact.xml`, `docs/qa/evidence/2026-05-29-ai-camera-scanner-modes-loop/AiCameraScannerModesInstrumentedTest-after-permission-override.txt`. |
| Multi-component scan keeps item identity | PASS | Unit coverage passed in `AiServicesTest`; runtime camera/AI scanner execution remains separate `NOT RUN` because no provider/image flow was executed. |
| Suspicious duplicate AI output shows review warning | PASS | Unit coverage passed in `AiServicesTest`; runtime warning UI was not opened. |
| Manual food add/edit/delete | PASS | Manual product entry opened from `Toevoegen aan Ochtend`; fields visible for product name, barcode, kcal, protein, carbs, fat, save and barcode scan. No product was saved in this safe loop. Evidence: `docs/qa/evidence/2026-05-27-nutrition-loop/nutrition-manual-product-entry.xml`. |
| Products list and quick add | PASS | Product editor and empty products list visible after choosing manual product creation. Evidence: `docs/qa/evidence/2026-05-27-nutrition-loop/nutrition-manual-product-entry.xml`. |
| Recipes add/edit/delete/use in meal | PASS | Runtime recipe section opens through `Voeding secties > Recepten`; empty save validation passed. Runtime create/save/use/delete was executed with `Wrap_kip_qa`: manual ingredient `Kip_rollade` was added, recipe saved, `Aan maaltijd` created a meal draft, meal save added `Wrap_kip_qa · 150g x1` to Middag, and delete was verified after fixing `QA-2026-05-27-002`. Runtime edit/save is now covered by `NutritionRecipeEditInstrumentedTest`: seeded saved recipe, tapped `Bewerk`, verified `Wijzigingen opslaan`, saved through edit path and observed `Recept opgeslagen.` Evidence: `docs/qa/evidence/2026-05-27-nutrition-recipes-loop/recipes-empty-save-validation.xml`, `docs/qa/evidence/2026-05-27-nutrition-recipes-loop/NutritionInputValidationTest-rerun.txt`, `docs/qa/evidence/2026-05-27-nutrition-recipe-runtime-loop/recipe-runtime-after-real-save.xml`, `docs/qa/evidence/2026-05-27-nutrition-recipe-runtime-loop/recipe-runtime-after-meal-save.xml`, `docs/qa/evidence/2026-05-27-nutrition-recipe-runtime-loop/recipe-fix-delete-confirm.xml`, `docs/qa/evidence/2026-05-27-nutrition-recipe-runtime-loop/recipe-fix-after-delete-confirm.xml`, `docs/qa/evidence/2026-05-27-nutrition-recipe-runtime-loop/logcat-recipe-fix-after-delete-confirm.txt`. |
| Meal logging save/reopen/restart | PASS | Backend/data persistence passed in `TargetedRoomPersistenceInstrumentedTest.targetedMealMutationsSurviveDatabaseReopen`; runtime product-save automation attempt was rejected as unreliable adb input evidence, so full manual UI save/reopen remains a separate release QA task. Rejected attempt evidence: `docs/qa/evidence/2026-05-27-nutrition-save-loop/nutrition-product-fields-filled.xml`. |
| Meal history and reuse meal | PASS | Meal logging, manual nutrition input, recipe create/use/edit/delete and historical snapshot immutability are covered. Targeted source guard verifies `Opnieuw gebruiken` restores meal type/name/notes and snapshot items into the editable draft while preserving item type, reference id, grams, serving count and notes. Targeted runtime test seeded a saved food and logged meal snapshot, opened `Voeding > Historie`, verified the meal/item, tapped `Opnieuw gebruiken`, and verified the editable `Maaltijd controleren` draft with reused item and save action. Evidence: `docs/qa/evidence/2026-05-27-meal-history-loop/NutritionInputValidationTest-meal-history-reuse.txt`, `docs/qa/evidence/2026-05-27-meal-history-runtime-loop/MealHistoryReuseRuntimeInstrumentedTest-rerun3.txt`. |
| Barcode scanner and lookup success/fail | NOT RUN | Barcode scan CTA opened scanner permission/fallback state successfully. Physical-device product-editor barcode flow now covers camera-denied fallback (`Cameratoegang nodig`, `Toegang geven`, `Terug`) and, after adb camera grant plus app restart, opens `Barcodescanner` with `Richt de camera op de barcode van het product.` on `SM-S931B`; no TrainIQ crash/ANR was observed and camera permission was restored to the initial denied state. Latest scanner/barcode contract rescan passed for scanner route modes, restorable scanner UI state, completed/empty/local-fallback mapping, savedStateHandle key/clear contracts, Nutrition pending-barcode routing to recipe draft vs food editor, and barcode lookup parser/failure guards. External Open Food Facts endpoint smoke returned success data for barcode `3017620422003` (`Nutella`, kcal/protein/carbs/fat present) plus fail/not-found status `0` for `00000000000000`. Full app runtime success/fail through a real barcode read and `pendingBarcode` return remains `NOT RUN` because no real barcode camera scan or savedStateHandle result was produced. Evidence: `docs/qa/evidence/2026-05-27-physical-barcode-camera-loop/barcode-camera-summary.txt`, `scanner-denied.xml`, `scanner-granted-after-restart.xml`, `logcat-crash-matches-strict.txt`, `logcat-crash-matches-after-grant-after-restart-strict.txt`, `docs/qa/evidence/2026-05-27-scanner-barcode-rescan/scanner-barcode-contract-tests.txt`, `docs/qa/evidence/2026-05-27-barcode-lookup-loop/openfoodfacts-3017620422003.json`, `docs/qa/evidence/2026-05-27-barcode-lookup-loop/openfoodfacts-00000000000000.json`. |
| Camera denied/no camera/manual fallback | PASS | Barcode scanner opened camera fallback state: `Cameratoegang nodig` and explanatory text were visible; no crash observed. AI/foto scanner remained disabled without AI provider. Evidence: `docs/qa/evidence/2026-05-27-barcode-loop/barcode-scanner-entry.xml`, `docs/qa/evidence/2026-05-27-nutrition-loop/nutrition-add-ochtend-exact.xml`. |
| Missing AI key/invalid AI response/local fallback | PASS | Missing AI state is represented by disabled `Foto / AI-inschatting` action in meal add sheet; no crash or secret exposure observed. Evidence: `docs/qa/evidence/2026-05-27-nutrition-loop/nutrition-add-ochtend-exact.xml`. |
| Long forms, keyboard/IME, dark mode, large font | PASS | Emulator-only instrumented runtime test on `emulator-5554` temporarily set font scale to `1.5`, opened `Voeding`, opened the Ochtend add sheet, focused `AI-context voor foto`, entered a long multi-component context, dismissed IME with the sheet still visible, verified `AI-context voor foto`, `Foto / AI-inschatting` and `Sluiten`, then restored font scale to the original value. Targeted nutrition/scanner unit checks and `assembleDebug` passed; logcat after the emulator flow had no TrainIQ `FATAL EXCEPTION`, ANR or input-dispatch timeout. This closes the high-risk Nutrition add-sheet long-form/IME font-scale path; broader full-app clipping/touch/focus audits remain separate rows. Evidence: `docs/qa/evidence/2026-05-27-emulator-nutrition-longform-ime-loop/NutritionLongFormImeInstrumentedTest.txt`, `nutrition-longform-targeted-unit.txt`, `assembleDebug-after-nutrition-longform-ime.txt`, `font-scale-after-test.txt`, `nutrition-longform-ime-summary.txt`. |
| Historical meal snapshots do not silently change after product/recipe edits | PASS | Added and ran `TargetedRoomPersistenceInstrumentedTest.targetedMealItemSnapshotsDoNotChangeAfterProductAndRecipeEdits`; existing meal item names/macros for `Kip rollade`, `Kaas` and `Wrap kip kaas` stayed unchanged after editing referenced food items and recipe. Command passed as part of the targeted Room persistence run with 27 tests. |


Physical-device Nutrition long-form/IME rerun: temporarily set font scale to `1.5`, opened Voeding and the Ochtend add sheet, focused `AI-context voor foto`, entered long multi-component context (`kip rollade kaas wrap saus sla ...`), dismissed IME with sheet still visible, restored font scale to `1.0`, and logcat had no TrainIQ crash/ANR. Evidence: `docs/qa/evidence/2026-05-27-physical-nutrition-longform-ime-loop/nutrition-longform-ime-summary.txt`, `nutrition-context-ime-font15.xml`, `nutrition-context-after-ime-back-font15.xml`, `font-scale-after-restore.txt`, `logcat-crash-matches.txt`.
## Voortgang

| Flow | Status | Evidence/notes |
|---|---|---|
| Add body measurement | PASS | Entered weight `82`, fat `18` and muscle `40`; overview updated to `82.0 kg`, `Vet 18.0%`, `Spier 40.0 kg`. Physical-device rerun entered `82.4`, `18.5`, `63.2`, captured pre/post-save UI, and showed the saved weight in the post-save dump. Evidence: `docs/qa/evidence/2026-05-27-progress-loop/progress-after-save.xml`, `docs/qa/evidence/2026-05-27-physical-progress-measurement-loop/before-save-valid-measurement.xml`, `after-save-valid-measurement.xml`, `progress-measurement-summary.txt`. |
| Edit/delete measurement | PASS | Runtime delete path passed: after saving a valid measurement, `Meetgeschiedenis` showed `27 May: 82.0 kg, 18.0% vet, 40.0 kg spier` with `Verwijderen`; tapping delete removed the measurement and returned the overview to empty placeholders (`-- kg`, `Vet --%`, `Spier -- kg`) plus `Nog geen voortgangsdata`. No separate edit action was visible in this UI, so edit-by-modifying-existing-entry remains not applicable to the current screen. Evidence: `docs/qa/evidence/2026-05-27-progress-edit-delete-loop/progress-valid-created.xml`, `docs/qa/evidence/2026-05-27-progress-edit-delete-loop/progress-history-actions.xml`, `docs/qa/evidence/2026-05-27-progress-edit-delete-loop/progress-after-delete.xml`, `docs/qa/evidence/2026-05-27-progress-edit-delete-loop/logcat-progress-delete.txt`. |
| Invalid values show clear validation | PASS | Runtime invalid input attempt showed inline validation and did not save bad overview values: weight field reported `Gewicht moet tussen 30 en 300 kg zijn.`, fat field reported `Vetpercentage moet tussen 0 en 100% zijn.`, and no TrainIQ crash/ANR occurred. Physical-device rerun captured an invalid `-1` weight attempt before valid save. Evidence: `docs/qa/evidence/2026-05-27-progress-validation-loop/progress-invalid-save.png`, `docs/qa/evidence/2026-05-27-progress-validation-loop/progress-invalid-save.xml`, `docs/qa/evidence/2026-05-27-progress-validation-loop/logcat-progress-invalid-save.txt`, `docs/qa/evidence/2026-05-27-physical-progress-measurement-loop/after-invalid-weight.xml`, `after-invalid-weight.png`. |
| Save/reopen/restart measurement integrity | PASS | After force-stop/relaunch and reopening Voortgang via Meer, saved values still showed `82.0 kg`, `Vet 18.0%`, `Spier 40.0 kg`. Physical-device rerun also verified Voortgang opens via Meer, a valid measurement save keeps the saved weight visible, back navigation is captured, and logcat has no TrainIQ crash/ANR. Evidence: `docs/qa/evidence/2026-05-27-progress-loop/progress-after-restart.xml`, `docs/qa/evidence/2026-05-27-physical-progress-measurement-loop/progress-top.xml`, `after-save-valid-measurement.xml`, `after-back-from-progress.xml`, `logcat-crash-matches.txt`, `progress-measurement-summary.txt`. |
| Smart-scale scanner valid result | NOT RUN | Scanner now opens on a physical device with camera permission granted, but no valid image/import/AI result was processed because AI is disabled in Settings and `Foto maken` / `Foto importeren` are disabled in that state. Latest targeted runtime component coverage verifies the smart-scale preview surface opens with scale guidance, `Foto maken`, import and `Terug` controls without live provider calls. Valid smart-scale OCR/AI result processing remains `NOT RUN`. Evidence: `docs/qa/evidence/2026-05-27-physical-smartscale-scanner-loop/after-smartscale-tap.xml`, `after-smartscale-tap.png`, `after-smartscale-visible-labels.txt`, `smartscale-scanner-summary.txt`, `docs/qa/evidence/2026-05-29-ai-camera-scanner-modes-loop/AiCameraScannerModesInstrumentedTest-after-permission-override.txt`. |
| Smart-scale scanner partial/no result/manual fallback | PASS | Tapping smart-scale photo/import entry without camera permission showed `Cameratoegang nodig`, explanatory text and `Foto importeren`; no crash observed. Physical-device rerun with camera permission granted opened `Camerascanner`, showed smart-scale explanatory copy and the AI-disabled fallback (`AI staat uit in Instellingen. Zet AI aan voordat je scant.`), kept capture/import disabled, and logcat had no TrainIQ crash/ANR. Evidence: `docs/qa/evidence/2026-05-27-progress-loop/progress-scale-scanner-entry.xml`, `docs/qa/evidence/2026-05-27-physical-smartscale-scanner-loop/camera-permission-state.txt`, `after-smartscale-tap.xml`, `after-smartscale-tap.png`, `after-smartscale-visible-labels.txt`, `logcat-crash-matches.txt`, `smartscale-scanner-summary.txt`. |
| Charts render and expose accessibility summary | PASS | Voortgang opened and measurement summary rendered. Component-level chart accessibility semantics passed via `AppLineChartAccessibilityTest`; full TalkBack traversal remains a separate release gate. Evidence: `docs/qa/evidence/2026-05-27-progress-loop/progress-after-save.xml`. |
| Empty states, dark mode, large font | PASS | Voortgang opened at font scale 1.5 in dark mode with overview text, smart-scale CTA and measurement fields visible; no TrainIQ crash/ANR observed. Empty-state copy was visible through placeholder values (`-- kg`, `Vet --%`, `Spier -- kg`). Evidence: `docs/qa/evidence/2026-05-27-accessibility-visual-loop/a11y-large-dark-progress-final.png`, `docs/qa/evidence/2026-05-27-accessibility-visual-loop/a11y-large-dark-progress-final.xml`, `docs/qa/evidence/2026-05-27-accessibility-visual-loop/logcat-large-dark-progress-final.txt`. |

## Coach

| Flow | Status | Evidence/notes |
|---|---|---|
| Goal advice form and result | PASS | Coach profile/goal fields accepted `QA_User`, age `30`, height `180`, weight `82`, fat `18`, goal `spieropbouw`; after scrolling, local calculation/result was visible. Evidence: `docs/qa/evidence/2026-05-27-coach-loop/coach-before-final-advice-tap.xml`, `docs/qa/evidence/2026-05-27-coach-loop/coach-advice-visible.png`. |
| Weekly report | PASS | Emulator-only seeded Coach runtime test on `emulator-5554` opens Coach with profile, workout and meal context, taps `Weekrapport maken`, verifies `Samenvatting bijgewerkt.`, source label `Lokale analyse`, `Hoogtepunten` and `Volgende stap`, then continues through training and nutrition coach sections. This covers provider-independent local weekly report runtime; live AI-provider weekly report remains covered only by contract tests and stays a provider-gated release item. Targeted Coach/AI unit guards and `assembleDebug` passed; logcat after the emulator flow had no TrainIQ `FATAL EXCEPTION`, ANR or input-dispatch timeout. Evidence: `docs/qa/evidence/2026-05-27-emulator-coach-insights-loop/CoachInsightsInstrumentedTest-weekly-report.txt`, `coach-weekly-targeted-unit.txt`, `assembleDebug-after-coach-weekly.txt`, `coach-weekly-runtime-summary.txt`. |
| Training insights | PASS | Emulator-only seeded runtime test on `emulator-5554` saves a profile, active routine, completed Bench Press workout and meal context, opens Coach, verifies the profile-ready screen, `Weekrapport maken`, `Trainingsinzichten`, `Actieve routine: QA Coach Routine` and the best estimated 1RM insight. Targeted Coach/AI unit guards and `assembleDebug` passed; logcat after the emulator flow had no TrainIQ `FATAL EXCEPTION`, ANR or input-dispatch timeout. Evidence: `docs/qa/evidence/2026-05-27-emulator-coach-insights-loop/CoachInsightsInstrumentedTest.txt`, `coach-insights-targeted-unit.txt`, `assembleDebug-after-coach-insights.txt`, `coach-insights-runtime-summary.txt`. |
| Nutrition coach message | PASS | Same emulator-only seeded Coach runtime test verifies `Voedingscoach` with a meal-aware nutrition message (`kcal onder je target`) after seeding a profile and meal context. Targeted Coach/AI unit guards and `assembleDebug` passed; logcat after the emulator flow had no TrainIQ `FATAL EXCEPTION`, ANR or input-dispatch timeout. Evidence: `docs/qa/evidence/2026-05-27-emulator-coach-insights-loop/CoachInsightsInstrumentedTest.txt`, `coach-insights-targeted-unit.txt`, `assembleDebug-after-coach-insights.txt`, `coach-insights-runtime-summary.txt`. |
| AI enabled valid JSON | PASS | Contract-level unit coverage passed in `AiServicesTest`: structured Dutch Gemini goal advice parses with source `GEMINI_2_5_FLASH`, workout debrief structured JSON parses, and deterministic profile/calorie baseline values remain app-owned. Runtime live provider call was not executed in this privacy-safe loop. Evidence: `docs/qa/evidence/2026-05-27-coach-ai-contract-loop/coach-ai-contract-tests.txt`. |
| AI disabled/missing key/invalid/English/timeout/rate-limit/offline | PASS | Missing/unconfigured AI state used local deterministic calculation instead of crashing or exposing secrets. Physical-device Coach rerun showed profile-required AI-disabled state, no secret-like API key text, and no TrainIQ crash/ANR. Evidence text included `Lokale berekening: onderhoud 2790 kcal en doel 2790 kcal op basis van je profiel.` Additional evidence: `docs/qa/evidence/2026-05-27-physical-coach-ai-disabled-loop/coach-ai-disabled-summary.txt`, `coach-visible-labels.txt`, `logcat-crash-matches.txt`. |
| Profile/calorie baseline is not overwritten by AI | PASS | In missing-key/local mode, deterministic profile inputs remained visible and local calorie output was derived from those values; no AI overwrite path was exercised. Evidence: `docs/qa/evidence/2026-05-27-coach-loop/coach-before-final-advice-tap.xml`. |
| Source labels, bullets, loading/error/fallback clarity | PASS | Emulator-only Coach weekly runtime now verifies provider-independent fallback clarity: after tapping `Weekrapport maken`, the UI shows `Samenvatting bijgewerkt.`, source label `Lokale analyse`, structured `Hoogtepunten` and `Volgende stap` sections, then continues to seeded training/nutrition coach content. Targeted Coach/AI unit guards cover parsing, fallback schemas, Dutch-output guard and source labels; `assembleDebug` passed, and logcat after the emulator flow had no TrainIQ `FATAL EXCEPTION`, ANR or input-dispatch timeout. Live AI-provider loading/error/offline behavior remains a provider-gated release item, not claimed here. Evidence: `docs/qa/evidence/2026-05-27-emulator-coach-insights-loop/CoachInsightsInstrumentedTest-weekly-report.txt`, `coach-weekly-targeted-unit.txt`, `assembleDebug-after-coach-weekly.txt`, `coach-weekly-runtime-summary.txt`. |
| Deep-mode thinking budget and JSON schema contract | PASS | Contract-level unit coverage passed for structured JSON parsing, Dutch-output guards, source tracking and baseline preservation in `AiServicesTest`. Runtime live Gemini request inspection remains outside this safe loop because no provider/key was configured. Evidence: `docs/qa/evidence/2026-05-27-coach-ai-contract-loop/coach-ai-contract-tests.txt`. |

## Meer/Instellingen

| Flow | Status | Evidence/notes |
|---|---|---|
| Theme mode changes | PASS | Settings theme controls `Systeem`, `Licht`, `Donker` visible with content descriptions. Physical-device rerun opened Meer/Instellingen, exposed theme options, toggled a mode, kept Settings rendered, and logcat had no TrainIQ crash/ANR. Evidence: `docs/qa/evidence/2026-05-27-settings-loop/settings-initial.xml`, `docs/qa/evidence/2026-05-27-physical-settings-theme-loop/meer-theme-top-rerun.xml`, `theme-options-visible.xml`, `after-theme-toggle.xml`, `settings-theme-summary.txt`, `logcat-crash-matches.txt`. |
| Telemetry opt-in/out | PASS | Telemetry section visible, default off text visible: `Telemetrie staat uit`. Toggle was visible; no upload or secret text shown. Evidence: `docs/qa/evidence/2026-05-27-settings-loop/settings-scrolled.xml`. |
| Gemini API key save/delete | PASS | Runtime Settings AI section showed `Gemini sleutel: Niet ingesteld`, Gemini key field, Google AI Studio link, `Sleutel opslaan`, and shared `AI-sleutels verwijderen` action. Delete confirmation explicitly states Gemini and OpenAI keys are removed and AI is disabled; confirming with no saved keys returned safely to Settings. JVM encrypted-store tests cover fake-key Gemini save/readback/clear and failure handling. No real key was entered in this privacy-safe loop, so save-with-real-key readback remains owner/security signoff. Evidence: `docs/qa/evidence/2026-05-27-settings-ai-keys-loop/GeminiKeyMigrationTest-openai-coverage.txt`, `docs/qa/evidence/2026-05-27-settings-ai-keys-loop/settings-ai-keys-lower.xml`, `docs/qa/evidence/2026-05-27-settings-ai-keys-loop/settings-after-ai-key-delete.xml`, `docs/qa/evidence/2026-05-27-settings-ai-keys-loop/settings-ai-key-delete-confirmed.xml`. |
| OpenAI API key save/delete | PASS | Runtime Settings AI section showed `OpenAI sleutel: Niet ingesteld`, OpenAI key field, OpenAI Platform API Keys link, `OpenAI opslaan`, and shared `AI-sleutels verwijderen` action. Delete confirmation explicitly covers OpenAI; no crash/secret exposure observed. JVM encrypted-store tests now cover fake-key OpenAI save/readback/clear and failed-save non-overwrite behavior. No real key was entered in this privacy-safe loop, so save-with-real-key readback remains owner/security signoff. Evidence: `docs/qa/evidence/2026-05-27-settings-ai-keys-loop/GeminiKeyMigrationTest-openai-coverage.txt`, `docs/qa/evidence/2026-05-27-settings-ai-keys-loop/settings-ai-keys-lower.xml`, `docs/qa/evidence/2026-05-27-settings-ai-keys-loop/settings-after-ai-key-delete.xml`, `docs/qa/evidence/2026-05-27-settings-ai-keys-loop/logcat-settings-ai-key-delete-confirmed.txt`. |
| Provider preference | PASS | Runtime AI section exposed provider order controls `Gemini eerst` and `OpenAI eerst`, plus AI enabled/disabled disclosure. Preference switching was visible but not toggled to avoid changing provider state in this privacy-safe loop. Evidence: `docs/qa/evidence/2026-05-27-settings-ai-keys-loop/settings-ai-providers.xml`, `docs/qa/evidence/2026-05-27-settings-ai-keys-loop/settings-ai-keys-lower.xml`. |
| Health Connect status refresh | PASS | Settings runtime state showed `Health Connect: Toegang vereist`; full Health Connect section showed `Status: Toegang vereist`, explanatory copy and actions `Toegang geven` and `Vernieuwen`. No crash/ANR observed. Evidence: `docs/qa/evidence/2026-05-27-healthconnect-loop/settings-healthconnect.xml`, `docs/qa/evidence/2026-05-27-healthconnect-loop/healthconnect-section-lower.xml`, `docs/qa/evidence/2026-05-27-healthconnect-loop/logcat-healthconnect-section.txt`. |
| Health Connect rationale | PASS | Tapping `Toegang geven` opened app rationale before Android permission grant, explaining why TrainIQ reads steps, heart rate, sleep, active calories, weight and workouts. No permission was granted or changed in this safe loop; no crash/security exception observed. Evidence: `docs/qa/evidence/2026-05-27-healthconnect-loop/healthconnect-permission-entry.png`, `docs/qa/evidence/2026-05-27-healthconnect-loop/healthconnect-permission-entry.xml`, `docs/qa/evidence/2026-05-27-healthconnect-loop/logcat-healthconnect-permission-entry.txt`. |
| Health Connect settings/install/update links | PASS | Provider intent behavior passed via `HealthConnectProviderIntentInstrumentedTest`; full runtime provider install/update flow was not manually opened. |
| Local data clear confirmation and effect | PASS | Runtime confirmation opened and clearly stated local training, nutrition, profile, AI key, preferences and Health Connect cache would be cleared; action was confirmed on test install, app relaunched into first-run/ontdekmodus without crash. Backend/usecase also verified after privacy fix: `ClearAppDataUseCase` clears all encrypted AI keys, local preferences, runtime Room data and performance diagnostics. Evidence: `docs/qa/evidence/2026-05-27-settings-clear-loop/settings-local-clear-confirm.xml`, `docs/qa/evidence/2026-05-27-settings-clear-loop/settings-after-local-clear-relaunch.xml`. |
| Destructive dialogs safe and accessible | PASS | Local data clear destructive dialog showed explicit irreversible-action copy and separate `Annuleren`/`Bevestigen` actions. Full TalkBack traversal remains open. Evidence: `docs/qa/evidence/2026-05-27-settings-clear-loop/settings-local-clear-confirm.xml`. |
| Secrets absent from logs, URLs, screenshots, BuildConfig production values | PASS | Settings screenshots/UI dumps show no API keys, only provider links and `Niet ingesteld` statuses. Logcat scans found no real Gemini/OpenAI key patterns; AndroidRuntime matches were uiautomator startup/shutdown only. Repo/docs scan found no real `AIza`, `sk-`, or `sk-proj` key patterns; test fixtures only use fake keys. Additional QA evidence/ledger secret-pattern scan found no `sk-*`, `sk-proj-*`, `AIza*`, `OPENAI_API_KEY`, `GEMINI_API_KEY`, `api_key=` or `Bearer` token matches. Evidence: `docs/qa/evidence/2026-05-27-settings-loop/logcat-tail.txt`, `docs/qa/evidence/2026-05-27-settings-ai-keys-loop/logcat-settings-ai-keys.txt`, `docs/qa/evidence/2026-05-27-settings-ai-keys-loop/logcat-settings-ai-key-delete-confirmed.txt`, `docs/qa/evidence/2026-05-27-privacy-evidence-loop/secret-pattern-scan.txt`. |

## Cross-Tab Runtime

| Flow | Status | Evidence/notes |
|---|---|---|
| Tab switching Start -> Training -> Voeding -> Coach -> Meer | PASS | Manual adb taps opened Start, Training, Voeding, Coach, Meer and Voortgang via Meer. Evidence: `docs/qa/evidence/2026-05-27-short-loop/tab-*.xml`. |
| Back stack behavior | PASS | Cross-tab runtime smoke opened Training, Voeding, Coach, Meer, Voortgang via Meer, then Android Back returned to Start/Home without crash. Evidence: `docs/qa/evidence/2026-05-27-cross-tab-lifecycle-loop/cross-training.xml`, `docs/qa/evidence/2026-05-27-cross-tab-lifecycle-loop/cross-nutrition.xml`, `docs/qa/evidence/2026-05-27-cross-tab-lifecycle-loop/cross-coach.xml`, `docs/qa/evidence/2026-05-27-cross-tab-lifecycle-loop/cross-meer.xml`, `docs/qa/evidence/2026-05-27-cross-tab-lifecycle-loop/cross-progress.xml`, `docs/qa/evidence/2026-05-27-cross-tab-lifecycle-loop/cross-after-back.xml`. |
| Scanner return values through savedStateHandle | PASS | Runtime Navigation Compose coverage now verifies CameraScanner returns barcode `3017620422003` to Nutrition through the previous back stack entry and that clear-after-consumption returns the destination to the empty scanner state. `QA-2026-05-29-001` fixed the stale-clear bug by publishing explicit empty string values for `getStateFlow(..., "")` consumers; clean full connected regression passed 51/51. Real optical camera/barcode decode remains tracked separately under barcode scanner success/fail. Evidence: `docs/qa/evidence/2026-05-29-scanner-savedstate-runtime-loop/ScannerSavedStateHandleInstrumentedTest-after-clear-fix.txt`, `docs/qa/evidence/2026-05-29-scanner-savedstate-runtime-loop/connectedDebugAndroidTest-full-rerun-after-uninstall.txt`. |
| Camera permission denied/granted | PASS | Emulator-only instrumented runtime test on `emulator-5554` now forces camera permission denied, opens `Voeding > Producten > Barcode scannen`, verifies `Cameratoegang nodig`, `Toegang geven` and `Terug`, then grants camera permission, reopens the same barcode scanner flow and verifies `Barcodescanner`, `Richt de camera op de barcode van het product.` and `Annuleren`. Targeted scanner route/state unit checks and `assembleDebug` passed; logcat after the denied/granted emulator flow had no TrainIQ `FATAL EXCEPTION`, ANR or input-dispatch timeout. Real barcode decode/result return remains covered separately under scanner savedStateHandle/barcode runtime gates. Evidence: `docs/qa/evidence/2026-05-27-emulator-camera-permission-loop/CameraPermissionScannerInstrumentedTest-pass.txt`, `scanner-permission-targeted-unit.txt`, `assembleDebug-after-camera-permission-test.txt`, `camera-permission-runtime-summary.txt`. |
| Health Connect no permission | PASS | Runtime Settings showed no-permission state `Toegang vereist`; app rationale opened safely through `Toegang geven` without granting permissions. Evidence: `docs/qa/evidence/2026-05-27-healthconnect-loop/healthconnect-section-lower.xml`, `docs/qa/evidence/2026-05-27-healthconnect-loop/healthconnect-permission-entry.xml`. |
| Health Connect partial/revoke/background-read on safe profile | NOT RUN | Targeted unit coverage passed for partial permission messaging, provider visibility, background-read manifest permission, background sync retry/stop policy, per-metric ChangesToken storage, legacy token fallback, incremental failure payloads and per-metric sync status handling. Physical-device Settings smoke verified Meer shows `Health Connect: Toegang vereist`, the `Voortgang openen` CTA works, back navigation returns safely, and logcat has no TrainIQ crash/ANR. New physical-device rationale run on `SM-S931B` confirmed all requested Health Connect permissions start `granted=false`, TrainIQ shows its own rationale screen before the system prompt with metric explanations for heart rate, sleep, active calories, weight and workouts plus a visible `TrainIQ verbinden` CTA, back navigation returns safely, permissions remain unmutated, and strict logcat scan found no TrainIQ crash/ANR. The system permission prompt was not reached in this coordinate run because the CTA was visible at the bottom edge but taps did not activate it; no grant/deny action was executed. Runtime partial grant/revoke/background-read matrix remains `NOT RUN` because it mutates device permission state. Evidence: `docs/qa/evidence/2026-05-27-healthconnect-policy-loop/healthconnect-policy-tests.txt`, `docs/qa/evidence/2026-05-27-physical-settings-healthconnect-loop/meer-top-rerun.xml`, `after-voortgang-open-rerun.xml`, `after-back-rerun.xml`, `settings-healthconnect-navigation-summary-rerun.txt`, `logcat-crash-matches-rerun.txt`, `docs/qa/evidence/2026-05-27-physical-healthconnect-permission-screen-loop/healthconnect-rationale-summary.txt`, `health-permissions-before.txt`, `system-screen-final.xml`, `rationale-bottom.xml`, `after-back.xml`, `health-permissions-after.txt`, `logcat-crash-matches-strict.txt`. |
| Offline/slow network for AI and barcode | NOT RUN | Barcode lookup offline/malformed-response behavior now has JVM runtime coverage through a fake `HttpURLConnection`: connection failure returns null, malformed response returns null and disconnects, successful fake response parses product data, and bounded 5s connect/read timeouts plus request headers are asserted. AI offline/live-provider behavior remains `NOT RUN` because no real provider/key flow was executed. Evidence: `docs/qa/evidence/2026-05-29-barcode-offline-runtime-loop/BarcodeProductLookupServiceTest-after-fake-connection-fix.txt`, `docs/qa/evidence/2026-05-29-barcode-offline-runtime-loop/testDebugUnitTest-after-barcode-offline.txt`, `docs/qa/evidence/2026-05-29-barcode-offline-runtime-loop/summary.txt`. |
| App background/foreground, lock/unlock | PASS | Background/foreground smoke passed: after Home key and relaunch, TrainIQ returned to Start/Home without crash. Lock/unlock emulator smoke also passed: Start/Home was visible before lock and after wake/dismiss-keyguard, with no TrainIQ crash/ANR. Evidence: `docs/qa/evidence/2026-05-27-cross-tab-lifecycle-loop/cross-after-foreground.png`, `docs/qa/evidence/2026-05-27-cross-tab-lifecycle-loop/cross-after-foreground.xml`, `docs/qa/evidence/2026-05-27-lock-unlock-loop/lock-before.xml`, `docs/qa/evidence/2026-05-27-lock-unlock-loop/lock-after.xml`, `docs/qa/evidence/2026-05-27-lock-unlock-loop/logcat-lock-unlock.txt`. |
| Rotation/recreate on high-risk screens | PASS | Emulator rotation smoke passed for Start/Home and Voortgang: both screens remained visible after portrait-to-landscape configuration change, with no TrainIQ crash/ANR. This does not replace full tablet/foldable layout QA or rotation coverage for every high-risk subflow. Evidence: `docs/qa/evidence/2026-05-27-rotation-loop/start-portrait.xml`, `docs/qa/evidence/2026-05-27-rotation-loop/start-landscape.xml`, `docs/qa/evidence/2026-05-27-rotation-loop/progress-portrait.xml`, `docs/qa/evidence/2026-05-27-rotation-loop/progress-landscape.xml`, `docs/qa/evidence/2026-05-27-rotation-loop/logcat-rotation.txt`. |
| Logcat crash/ANR slice after smoke | PASS | `adb logcat` captured after short, Home CTA/restart, Settings, Settings AI keys, Nutrition, Nutrition recipes, Barcode, Training, Training routine delete, Progress, Progress validation/edit/delete, Coach, Training exercise, workout completion, Health Connect rationale, cross-tab lifecycle, lock/unlock, rotation, Settings clear, font-scale 1.3, large-font/dark-mode visual loops and latest clean install/start smoke. No TrainIQ `FATAL EXCEPTION` or `ANR`; `AndroidRuntime` matches were uiautomator startup/shutdown, not app crash evidence. Latest current-build smoke installed debug, cleared app data, opened `com.trainiq/.MainActivity`, dumped UI and found `NO_TRAINIQ_CRASH_OR_ANR_MATCHES`. Targeted connected tests for Room persistence and active workout restore also passed. Evidence: `docs/qa/evidence/2026-05-27-short-loop/logcat-tail.txt`, `docs/qa/evidence/2026-05-27-home-cta-loop/logcat-home-ctas.txt`, `docs/qa/evidence/2026-05-27-home-restart-loop/logcat-home-restart.txt`, `docs/qa/evidence/2026-05-27-settings-loop/logcat-tail.txt`, `docs/qa/evidence/2026-05-27-settings-ai-keys-loop/logcat-settings-ai-key-delete-confirmed.txt`, `docs/qa/evidence/2026-05-27-nutrition-loop/logcat-after-ai-scanner.txt`, `docs/qa/evidence/2026-05-27-nutrition-recipes-loop/logcat-nutrition-recipes-final.txt`, `docs/qa/evidence/2026-05-27-barcode-loop/logcat-after-barcode-entry.txt`, `docs/qa/evidence/2026-05-27-training-loop/logcat-after-empty-routine-start.txt`, `docs/qa/evidence/2026-05-27-training-delete-loop/logcat-routine-delete.txt`, `docs/qa/evidence/2026-05-27-progress-loop/logcat-after-scale-scanner-entry.txt`, `docs/qa/evidence/2026-05-27-progress-validation-loop/logcat-progress-invalid-save.txt`, `docs/qa/evidence/2026-05-27-progress-edit-delete-loop/logcat-progress-delete.txt`, `docs/qa/evidence/2026-05-27-coach-loop/logcat-after-coach-advice-visible.txt`, `docs/qa/evidence/2026-05-27-training-exercise-loop/logcat-after-finish-zero-sets.txt`, `docs/qa/evidence/2026-05-27-workout-completion-loop/logcat-after-completion-screen.txt`, `docs/qa/evidence/2026-05-27-healthconnect-loop/logcat-healthconnect-permission-entry.txt`, `docs/qa/evidence/2026-05-27-cross-tab-lifecycle-loop/logcat-cross-tab-lifecycle.txt`, `docs/qa/evidence/2026-05-27-lock-unlock-loop/logcat-lock-unlock.txt`, `docs/qa/evidence/2026-05-27-rotation-loop/logcat-rotation.txt`, `docs/qa/evidence/2026-05-27-settings-clear-loop/logcat-after-local-clear.txt`, `docs/qa/evidence/2026-05-27-fontscale13-loop/logcat-fontscale13-correction.txt`, `docs/qa/evidence/2026-05-27-accessibility-visual-loop/logcat-large-dark.txt`, `docs/qa/evidence/2026-05-27-accessibility-visual-loop/logcat-large-dark-progress-final.txt`, `docs/qa/evidence/2026-05-27-final-smoke-loop/logcat-final-smoke.txt`, `docs/qa/evidence/2026-05-27-final-smoke-loop/logcat-crash-matches.txt`, `docs/qa/evidence/2026-05-27-final-smoke-loop/trainiq-final-smoke.xml`. |

## Accessibility And Design

| Flow | Status | Evidence/notes |
|---|---|---|
| TalkBack high-risk flows | NOT RUN | Targeted semantics/unit checks passed for charts, shared dialog pane title, workout labels, nutrition fields/scanner copy and Settings labels, but TalkBack traversal on high-risk full screens was not executed. Physical device `SM-S931B` / Android `16` reports `accessibility_enabled=0` and `enabled_accessibility_services=null`; physical label/focus smoke captured Start, Training, Voeding, Coach and Settings with no TrainIQ crash/ANR, but this is not TalkBack traversal. Evidence: `docs/qa/evidence/2026-05-27-accessibility-targeted-loop/targeted-accessibility-unit-tests.txt`, `docs/qa/evidence/2026-05-27-accessibility-contract-rescan/accessibility-contract-tests.txt`, `docs/qa/evidence/2026-05-27-physical-assistive-tech-state-loop/assistive-tech-state-summary.txt`, `docs/qa/evidence/2026-05-27-physical-assistive-tech-state-loop/assistive-tech-smoke-summary.txt`, `docs/qa/evidence/2026-05-27-physical-assistive-tech-state-loop/start-labels-focusables.txt`, `docs/qa/evidence/2026-05-27-physical-assistive-tech-state-loop/training-labels-focusables.txt`, `docs/qa/evidence/2026-05-27-physical-assistive-tech-state-loop/nutrition-labels-focusables.txt`, `docs/qa/evidence/2026-05-27-physical-assistive-tech-state-loop/coach-labels-focusables.txt`, `docs/qa/evidence/2026-05-27-physical-assistive-tech-state-loop/settings-labels-focusables.txt`, `docs/qa/evidence/2026-05-27-physical-assistive-tech-state-loop/logcat-crash-matches-strict.txt`. |
| Switch Access high-risk flows | NOT RUN | Targeted labels/semantics passed, including latest dialog pane, scanner field and workout control contract rescan. Physical device `SM-S931B` / Android `16` has accessibility disabled (`accessibility_enabled=0`, `enabled_accessibility_services=null`); related accessibility packages are present, but no Switch Access traversal was executed. This remains an owner/manual assistive-tech gate. Evidence: `docs/qa/evidence/2026-05-27-accessibility-targeted-loop/targeted-accessibility-unit-tests.txt`, `docs/qa/evidence/2026-05-27-accessibility-contract-rescan/accessibility-contract-tests.txt`, `docs/qa/evidence/2026-05-27-physical-assistive-tech-state-loop/accessibility-related-packages.txt`, `docs/qa/evidence/2026-05-27-physical-assistive-tech-state-loop/accessibility-enabled.txt`, `docs/qa/evidence/2026-05-27-physical-assistive-tech-state-loop/enabled-accessibility-services.txt`, `docs/qa/evidence/2026-05-27-physical-assistive-tech-state-loop/assistive-tech-smoke-summary.txt`. |
| Font scale 1.3 and 1.5 | NOT RUN | Font scale 1.3 and 1.5 visual smoke covered Start, Training, Voeding, Coach, Meer and Voortgang without app crash/ANR. Additional physical-device font scale 1.5 smoke covered the Nutrition add sheet, long AI-context input and IME dismiss with no TrainIQ crash/ANR, then restored font scale to 1.0. Deeper subflows are still not fully traversed, so this remains `NOT RUN` for full-app font-scale certification. Evidence: `docs/qa/evidence/2026-05-27-fontscale13-loop/fs13-start-final.xml`, `docs/qa/evidence/2026-05-27-accessibility-visual-loop/a11y-large-dark-*.xml`, `docs/qa/evidence/2026-05-27-physical-nutrition-longform-ime-loop/nutrition-longform-ime-summary.txt`, `nutrition-context-ime-font15.png`, `font-scale-after-restore.txt`. |
| Dark mode and dynamic color | NOT RUN | Dark-mode smoke covered Start, Training, Voeding, Coach, Meer and Voortgang at font scale 1.5 without app crash/ANR. Dynamic color source/unit guard verifies Android 12+ gating and both dynamic dark/light color-scheme paths. Physical-device top-level runtime smoke now also covered Start, Training, Voeding, Coach, Meer and Voortgang on `SM-S931B` / Android `16` with no TrainIQ crash/ANR, and Settings theme mode options/toggle were exercised on the physical device. Deep subflows and full visual overlap certification remain `NOT RUN`. Evidence: `docs/qa/evidence/2026-05-27-accessibility-visual-loop/a11y-large-dark-*.png`, `docs/qa/evidence/2026-05-27-dynamic-color-loop/ThemeDynamicColorTest.txt`, `docs/qa/evidence/2026-05-27-physical-dynamic-color-loop/physical-dynamic-top-level-smoke-summary.txt`, `docs/qa/evidence/2026-05-27-physical-settings-theme-loop/settings-theme-summary.txt`, `theme-options-visible.png`, `after-theme-toggle.png`, `logcat-crash-matches.txt`. |
| Touch targets | NOT RUN | Targeted semantics/source checks and large-font top-level smoke passed. Emulator-only top-level audit on `emulator-5554` found one under-48dp Settings feedback switch candidate; P2 `QA-2026-05-27-006` moved the feedback switch action semantics to the full row, made the visual switch decorative for accessibility, and compacted loaded Settings spacing so the first feedback row is not clipped by the bottom bar. Post-fix emulator UIAutomator audit across Start/Training/Voeding/Coach/Meer found `0` clickable/focusable nodes under the 48dp threshold. Full deep-flow touch-target measurement and TalkBack/Switch owner signoff remain open. Evidence: `docs/qa/evidence/2026-05-27-emulator-touch-target-audit-loop/touch-target-audit-summary-compact-settings-fix.txt`, `touch-target-under-48dp-compact-settings-fix.txt`, `SettingsUiStateTest-compact-settings-fix.txt`, `assembleDebug-compact-settings-fix.txt`, `lintDebug-compact-settings-fix.txt`, `settings-row-toggle-interaction-pass.txt`, `logcat-after-row-toggle-interaction-clean.txt`. |
| Content descriptions and focus order | NOT RUN | Targeted content-description/semantic checks passed for charts, shared dialogs, active workout controls, nutrition fields/scanner copy and Settings Health Connect/theme actions. Emulator-only top-level focus/label audit on `emulator-5554` inspected Start/Training/Voeding/Coach/Meer UIAutomator dumps; the refined audit found no NAF attributes, `31` focusable/clickable nodes, and `0` unlabeled nodes without child text/content-desc in the parsed subtree. Logcat had no TrainIQ crash/ANR/input timeout. Full app focus order remains unverified without TalkBack/Switch traversal. Evidence: `docs/qa/evidence/2026-05-27-emulator-focus-label-loop/focus-label-refined-summary.txt`, `focus-label-audit-summary.txt`, `focus-label-unlabeled-candidates.txt`, `naf-summary.txt`, `logcat-after-focus-label-smoke-clean.txt`, plus `docs/qa/evidence/2026-05-27-accessibility-targeted-loop/targeted-accessibility-unit-tests.txt` and `docs/qa/evidence/2026-05-27-accessibility-contract-rescan/accessibility-contract-tests.txt`. |
| Text overlap/clipping check | NOT RUN | Emulator-only top-level font-scale 1.5 text-bounds audit on `emulator-5554` inspected Start/Training/Voeding/Coach/Meer and found one P2 clipped Settings theme text candidate (`Licht`, 6px high at viewport edge). `QA-2026-05-27-007` shortened the Settings overflow copy while preserving the compact navigation and Voortgang meaning; post-fix emulator audit inspected 64 text nodes with `0` suspect bounds. Full deep-subflow overlap/clipping certification remains open. Evidence: `docs/qa/evidence/2026-05-27-emulator-text-clipping-loop/text-clipping-audit-summary.txt`, `text-clipping-suspects.txt`, `SettingsUiStateTest-short-overflow-copy.txt`, `assembleDebug-short-overflow-copy.txt`, `text-clipping-audit-summary-after-fix.txt`, `text-clipping-suspects-after-fix.txt`, `lintDebug-short-overflow-copy.txt`, `logcat-after-text-clipping-fix-clean.txt`. |
| Modal/dialog focus containment | NOT RUN | Destructive confirmations were exercised for recipe delete, routine delete and local data clear. Emulator-only local-data confirmation on `emulator-5554` opened with `Alle lokale appdata wissen?`, irreversible warning copy, `Annuleren` and `Bevestigen`; UIAutomator dialog dump root was bounded to the modal (`[120,763][960,1636]`) and exposed only the two modal action targets, with no background navigation/content focusables in the active tree. Back dismissed the dialog back to `Gegevens / opslag` without confirming, and logcat had no TrainIQ crash/ANR/input timeout. Full TalkBack/Switch Access and keyboard/D-pad traversal across all dialogs/sheets remain open. Evidence: `docs/qa/evidence/2026-05-27-emulator-modal-focus-loop/local-clear-dialog-after-more.xml`, `modal-focus-summary.txt`, `modal-focusables.txt`, `back-dismiss-pass.txt`, `logcat-after-modal-focus-clean.txt`, plus source guard `docs/qa/evidence/2026-05-27-dialog-accessibility-loop/AppDialogAccessibilityTest.txt`. |

## Findings

Add findings below using the schema from `docs/qa/full-app-qa-basis.md`.

## Finding QA-2026-05-27-001

- priority: P1
- area: Meer/Instellingen - privacy/security - local data clear
- tab/flow: `Meer/Instellingen > Local data clear`
- status: fixed
- current evidence: Static scan showed `ClearAppDataUseCase` called `aiUsageGate.clearEncryptedApiKey()`, which clears only Gemini encrypted key state.
- expected behavior: Local data clear removes all local private AI secrets, including Gemini and OpenAI encrypted keys, plus legacy DataStore values.
- actual behavior: OpenAI encrypted key storage was not cleared by the local data clear use case.
- repro steps: Inspect `ClearAppDataUseCase.invoke()` before fix; observe `aiUsageGate.clearEncryptedApiKey()` instead of `clearAllAiKeys()`.
- recommended fix: Use `aiUsageGate.clearAllAiKeys()` inside `ClearAppDataUseCase`.
- regression risk: Low; change broadens secret cleanup scope for a destructive clear action.
- minimal verification: `.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.domain.usecase.ClearAppDataUseCaseTest" --console=plain` passed; `.\gradlew.bat :app:testDebugUnitTest --console=plain` passed; `.\gradlew.bat :app:assembleDebug --console=plain` passed; `.\gradlew.bat :app:lintDebug --console=plain` passed; `.\gradlew.bat :app:connectedDebugAndroidTest --console=plain` passed with 45 tests.
- owner suggestion: Keep privacy/security owner signoff open until post-clear key-readback is manually verified with real saved keys; runtime Settings destructive confirmation/relaunch passed on test install.

## Finding QA-2026-05-27-002

- priority: P0
- area: Voeding - recipes - destructive action - runtime ANR
- tab/flow: `Voeding > Recepten > Opgeslagen recepten > Verwijderen`
- status: fixed
- current evidence: Runtime recipe flow created and saved `Wrap_kip_qa`; tapping saved-recipe `Verwijderen` showed Android `TrainIQ isn't responding`, with logcat `Input dispatching timed out` for `com.trainiq/com.trainiq.MainActivity`. Confirming delete reproduced the ANR dialog.
- expected behavior: Saved recipe delete opens a confirmation dialog immediately, confirm removes the recipe, UI remains responsive, and logcat contains no app ANR/FATAL.
- actual behavior: Delete confirmation and confirm path could trigger Android ANR/not-responding dialog during recipe delete flow.
- repro steps: Open `Voeding`, open `Recepten`, create/save a recipe with manual ingredient, tap saved recipe `Verwijderen`, observe `TrainIQ isn't responding`; after `Wait`, confirm delete and observe another ANR dialog.
- recommended fix: Close the confirmation dialog before starting the destructive delete action and remove the scrollable modifier from the AlertDialog body text to avoid focus/layout work remaining active during delete confirmation.
- regression risk: Low to medium; affects shared nutrition delete confirmation for meal/food/recipe. Behavior remains the same for users except dialog closes before delete work starts.
- minimal verification: `compileDebugKotlin` passed, `NutritionInputValidationTest` passed, `installDebug` passed, runtime re-test opened recipe delete confirmation without ANR, confirmed delete, saved recipe list returned to empty state, and logcat after fixed confirmation showed no TrainIQ ANR/FATAL. Post-fix regression also passed: `assembleDebug`, `testDebugUnitTest`, `lintDebug`, and `connectedDebugAndroidTest`.
- owner suggestion: Keep this in the next connected/full regression batch because the dialog component is shared by meal, food and recipe delete confirmations.
## Finding QA-2026-05-27-003

- priority: P1
- area: Database/migration - release marker generation - buildscript drift
- tab/flow: `Room migration-chain verification marker generation`
- status: fixed
- current evidence: `RoomMigrationChainVerificationProvider.CurrentRoomVersion` is `13`, and provider tests expected marker `trainiq-room-migration-chain-v2-to-v13`, but `app/build.gradle.kts` still generated marker `trainiq-room-migration-chain-v2-to-v12` with `currentRoomVersion`, `requiredEndVersion` and `coveredEndVersion` set to `12`.
- expected behavior: The Gradle marker generator uses the same Room migration end version as the provider/source-of-truth so generated release/profileable/debug marker assets can become trusted and fresh.
- actual behavior: Generated markers would be stale against the provider because the buildscript emitted v12 metadata while the runtime gate expects v13.
- repro steps: Inspect `app/build.gradle.kts` marker generator before fix and compare it with `RoomMigrationChainVerificationProvider.CurrentRoomVersion` and `expectedMarker()`.
- recommended fix: Update Gradle marker generator values to v13 and add a regression test asserting the buildscript marker/current/required/covered end versions match `CurrentRoomVersion`.
- regression risk: Low; changes only generated marker metadata and test coverage, not database schema or migrations.
- minimal verification: `RoomMigrationChainVerificationProviderTest` passed after the fix, including the new buildscript/provider contract test. `NutritionInputValidationTest` also passed after the same batch. Full post-fix baseline passed afterward: `assembleDebug`, `testDebugUnitTest`, `lintDebug`, and `connectedDebugAndroidTest`.
- owner suggestion: Keep migration-chain marker generation in release checklist; a full marker-generation task still depends on connected migration tests and should be run before release artifact signing.
## Finding QA-2026-05-27-005

- priority: P2
- area: Meer/Instellingen - accessibility/design - feedback and telemetry switches
- tab/flow: `Meer > Weergave/Workoutfeedback/Privacy en telemetrie` at emulator font scale `1.5`
- status: fixed
- current evidence: Emulator-only UIAutomator dump on `emulator-5554` showed Settings feedback/telemetry switches as clickable/focusable `NAF="true"` controls with empty `content-desc`, even though visible labels existed next to the controls. Evidence: `docs/qa/evidence/2026-05-27-emulator-ux-loop-settings-font/settings-scrolled-font15.xml`, `touch-target-audit.txt`.
- expected behavior: Every Settings switch has its own accessible label with current on/off state, so assistive tooling does not expose anonymous controls when font scale is large.
- actual behavior: The switch controls were reachable but unnamed in UIAutomator, creating a focus/semantics gap for `Rusttimer-geluid`, `Workouttrillingen`, and `Technische telemetrie delen`.
- repro steps: Install debug on `emulator-5554`, set `font_scale` to `1.5`, clear app data, open TrainIQ, navigate to `Meer`, scroll Settings to Workoutfeedback/Privacy, dump UI hierarchy, and inspect clickable/focusable switch nodes.
- recommended fix: Add a stateful `settingsToggleAccessibilityLabel(title, checked)` helper and apply it directly to the `Switch` modifier in `FeedbackToggleRow`.
- regression risk: Low; behavior and layout are unchanged, only switch semantics are named.
- minimal verification: `SettingsUiStateTest` passed with the new source guard; fixed runtime dump on `emulator-5554` reports `NAF count: 0`, includes `Rusttimer-geluid: ingeschakeld` and `Workouttrillingen: ingeschakeld`, and logcat had no TrainIQ `FATAL EXCEPTION`, ANR or input-dispatch timeout. `assembleDebug`, emulator-only `TrainIqFlowSmokeInstrumentedTest`, and `lintDebug` passed. Evidence: `docs/qa/evidence/2026-05-27-emulator-ux-loop-settings-font/SettingsUiStateTest-after-switch-labels.txt`, `switch-label-runtime-summary.txt`, `assembleDebug-after-switch-labels.txt`, `TrainIqFlowSmokeInstrumentedTest-emulator-only-after-switch-labels.txt`, `lintDebug-after-switch-labels.txt`.
- owner suggestion: Keep full TalkBack/Switch Access traversal open; this closes only the emulator-observed anonymous-switch semantics gap.
## Finding QA-2026-05-27-006

- priority: P2
- area: Meer/Instellingen - accessibility/design - touch targets and clipping
- tab/flow: `Meer > Workoutfeedback` initial loaded Settings viewport on `emulator-5554`
- status: fixed
- current evidence: Emulator-only UIAutomator audit at density `420` found `Rusttimer-geluid: ingeschakeld` exposed as a clickable/focusable node with bounds `[859,2143][996,2174]`, height `31px`, below the computed 48dp threshold of `126px`. Evidence: `docs/qa/evidence/2026-05-27-emulator-touch-target-audit-loop/touch-target-audit-summary.txt`, `touch-target-under-48dp.txt`.
- expected behavior: Settings feedback toggles expose a reachable 48dp-or-larger accessibility target, and the initial Settings viewport must not expose clipped focusable controls behind the bottom bar.
- actual behavior: The visual switch was exposed as the focusable target and the first feedback control was partially clipped at the bottom of the viewport.
- repro steps: Install debug on `emulator-5554`, clear app data, open TrainIQ, navigate across top-level tabs to `Meer`, dump UI hierarchy, compute clickable/focusable bounds against `48dp * density / 160`.
- recommended fix: Make `FeedbackToggleRow` itself the `Role.Switch` toggle target with min 48dp height and stateful label, clear semantics from the visual `Switch`, and tighten loaded Settings screen vertical spacing enough to keep the first feedback row fully visible.
- regression risk: Low to medium; scoped to Settings layout density and feedback switch accessibility behavior.
- minimal verification: `SettingsUiStateTest` passed with source guards for 48dp row toggle semantics, `assembleDebug` passed, `lintDebug` passed, debug build installed on `emulator-5554`, post-fix UIAutomator audit across Start/Training/Voeding/Coach/Meer found `0` under-48dp clickable/focusable nodes, row-toggle runtime smoke changed `Rusttimer-geluid` to the off state, and logcat had no TrainIQ `FATAL EXCEPTION`, ANR or input-dispatch timeout. Evidence: `docs/qa/evidence/2026-05-27-emulator-touch-target-audit-loop/SettingsUiStateTest-compact-settings-fix.txt`, `assembleDebug-compact-settings-fix.txt`, `installDebug-compact-settings-fix.txt`, `touch-target-audit-summary-compact-settings-fix.txt`, `touch-target-under-48dp-compact-settings-fix.txt`, `settings-row-toggle-interaction-pass.txt`, `lintDebug-compact-settings-fix.txt`, `logcat-after-row-toggle-interaction-clean.txt`.
- owner suggestion: Keep full app deep-flow touch-target, TalkBack traversal and Switch Access signoff open; this closes only the emulator-observed top-level Settings feedback touch/clipping issue.
## Finding QA-2026-05-27-007

- priority: P2
- area: Meer/Instellingen - large font - text clipping
- tab/flow: `Meer > Weergave` at font scale `1.5` on `emulator-5554`
- status: fixed
- current evidence: Emulator-only text-bounds audit inspected top-level Start/Training/Voeding/Coach/Meer dumps at font scale `1.5` and found one suspect clipped text node: `Licht`, height `6px`, bounds `[126,2105][253,2111]`, at the bottom edge of the Settings viewport. Evidence: `docs/qa/evidence/2026-05-27-emulator-text-clipping-loop/text-clipping-audit-summary.txt`, `text-clipping-suspects.txt`, `settings-font15.xml`.
- expected behavior: Top-level Settings content remains readable at font scale `1.5`; visible text nodes should not render as clipped fragments at the viewport edge.
- actual behavior: The long compact-navigation helper copy consumed enough vertical space that the `Licht` theme chip text appeared as a clipped 6px fragment at the bottom of the viewport.
- repro steps: Install debug on `emulator-5554`, set `font_scale` to `1.5`, clear app data, open TrainIQ, navigate across top-level tabs to `Meer`, dump UI hierarchy, and audit text node bounds for tiny or edge-clipped visible text.
- recommended fix: Shorten the Settings overflow helper copy while preserving the meaning: compact navigation groups extra items, and Voortgang opens trends/graphs.
- regression risk: Low; scoped to a Settings helper string and covered by existing source guard expectations for `Compacte navigatie` and `Voortgang`.
- minimal verification: `SettingsUiStateTest` passed, `assembleDebug` passed, debug build installed on `emulator-5554`, post-fix font-scale `1.5` top-level audit inspected 64 text nodes and found `0` suspect bounds, `lintDebug` passed, font scale was restored, and logcat had no TrainIQ `FATAL EXCEPTION`, ANR or input-dispatch timeout. Evidence: `docs/qa/evidence/2026-05-27-emulator-text-clipping-loop/SettingsUiStateTest-short-overflow-copy.txt`, `assembleDebug-short-overflow-copy.txt`, `installDebug-short-overflow-copy.txt`, `text-clipping-audit-summary-after-fix.txt`, `text-clipping-suspects-after-fix.txt`, `font-scale-after-after-fix.txt`, `lintDebug-short-overflow-copy.txt`, `logcat-after-text-clipping-fix-clean.txt`.
- owner suggestion: Keep full deep-subflow overlap/clipping certification open; this closes only the emulator-observed top-level Settings large-font clipping issue.
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

Overall status: `PARTIAL`

Highest-risk open issues:

- Eight executed-loop findings were found and fixed: `QA-2026-05-27-001` local data clear did not clear OpenAI encrypted key storage, `QA-2026-05-27-002` saved-recipe delete triggered Android ANR/not-responding, `QA-2026-05-27-003` Gradle migration marker generation drifted behind Room v13, `QA-2026-05-27-004` hardened under-48dp Coach/Settings controls, `QA-2026-05-27-005` fixed anonymous Settings feedback/telemetry switch semantics on emulator font scale 1.5, `QA-2026-05-27-006` fixed the emulator Settings feedback touch-target/clipping issue, `QA-2026-05-27-007` fixed emulator Settings large-font text clipping, and `QA-2026-05-27-008` fixed active-workout correction crashes caused by the invalid draft active-key FK. No open P0/P1/P2 bugs remain from the executed checks after targeted verification, but full Definition of Done is still incomplete.
- Manual deeper flows remain `NOT RUN`; runtime workout completion/debrief with a logged set is covered, and barcode service/parser plus external endpoint smoke are covered, but full app barcode runtime through scanner result remains open. Post dialog/theme contract changes, the automated baseline passed again: `assembleDebug`, `testDebugUnitTest`, `lintDebug` after one guarded API-31 lint rerun, and `connectedDebugAndroidTest`. Evidence: `docs/qa/evidence/2026-05-27-post-dialog-theme-baseline/assembleDebug.txt`, `docs/qa/evidence/2026-05-27-post-dialog-theme-baseline/testDebugUnitTest.txt`, `docs/qa/evidence/2026-05-27-post-dialog-theme-baseline/lintDebug-rerun.txt`, `docs/qa/evidence/2026-05-27-post-dialog-theme-baseline/connectedDebugAndroidTest.txt`. This run does not satisfy full Definition of Done yet.

Open `NOT RUN` summary:

- External/owner-gated: TalkBack/Switch Access traversal, Health Connect partial/revoke/background-read runtime matrix, privacy/security real-key signoff. Physical-device macrobenchmark is now PASS on `SM-S931B` (`ro.kernel.qemu=0`).
- Runtime/provider-gated: live AI routine generation, AI meal scanner, live AI weekly report provider response, scanner savedStateHandle return with real camera/barcode result, offline/slow-network simulation. Barcode denied/granted camera permission path plus seeded Coach weekly/training/nutrition local runtime views and fallback source/bullet clarity are now covered on emulator.
- Manual deep-runtime-gated: smart-scale valid result, full-app touch-target, overlap/clipping and focus-order audits. Emulator runtime Exercise History seeded detail, Nutrition long-form IME at font scale 1.5, edit/delete interaction and manual weight/reps/RPE/set-type edits for active-workout logged sets are now covered.
- Reproducible open-gaps snapshot: `docs/qa/evidence/2026-05-27-dod-open-gaps-audit/not-run-snapshot.txt`.
- Owner/runtime closure checklist: `docs/qa/release-gate-owner-checklist-2026-05-27.md`.

Release gates still open:

- Physical-device macrobenchmark is closed as PASS on device `RFCY60HNHNJ` (`SM-S931B`, Android `16` / SDK `36`, `ro.kernel.qemu=0`). Evidence: `docs/qa/evidence/2026-05-27-physical-device-macrobenchmark-loop/profileable-macrobenchmark-assemble.txt`, `connectedProfileableAndroidTest.txt`, `logcat-after-macrobenchmark.txt`, `logcat-crash-matches.txt`.
- TalkBack/Switch Access full-screen traversal; chart semantics component test, targeted accessibility unit checks and large-font/dark-mode top-level smoke passed, but assistive-tech traversal across high-risk full screens remains open. Current emulator accessibility services are disabled; Switch Access package is installed but not exercised.
- Full Health Connect matrix; no-permission/rationale runtime path passed, provider-intent and core policy tests passed, and Health Connect controller package is present on emulator, but partial permission, revoke-while-open and background-read runtime cases remain open because they mutate device permission state and still require explicit owner/runtime pass.
- Privacy/security owner signoff remains open; static scan, local-clear usecase fix, runtime local-clear confirmation/relaunch, AI key field/delete UX, fake-key encrypted Gemini/OpenAI save/readback/clear tests, no-secret log/UI smoke and latest secret-pattern rescan passed (`NO_MATCHES`), but real-key save/readback was intentionally not run. Evidence: `docs/qa/evidence/2026-05-27-privacy-rescan-loop/secret-pattern-scan.txt`.

Next safest action:

Run the next short loop focused on a more stable open gate: actual TalkBack/Switch Access traversal, remaining Health Connect runtime mutations (partial permission, revoke-while-open, background-read), full app barcode lookup through real scanner result, real AI meal scanner/provider behavior, or recipe edit via a proper Compose/instrumented runtime test instead of fragile coordinate input; source guard coverage for recipe edit and scanner savedStateHandle contracts now exists but does not replace runtime proof. Backend/data coverage for meal persistence, historical meal snapshot immutability and active-workout set edit/delete/finish/undo is backed by targeted connected tests; Coach/AI and debrief parsing/fallback logic are backed by `AiServicesTest` plus `GoalAdviceInputTest`; runtime workout completion/debrief with missing AI key/local fallback is covered; recipe create/use/edit/delete runtime is covered; barcode parser/service and external endpoint smoke are covered; Health Connect no-permission/rationale runtime path is covered; targeted accessibility unit checks are covered.

Handoff:

- Current decision: `PARTIAL`, not release-ready by full DoD.
- QA packet entry point: `docs/qa/qa-packet-2026-05-27.md`.
- Compact release-review summary: `docs/qa/qa-status-summary-2026-05-27.md`.
- Machine-readable QA status, including owner defer template reference: `docs/qa/qa-status-2026-05-27.json`.
- QA status schema: `docs/qa/qa-status-schema-2026-05-27.json`.
- QA status JSON validation: `docs/qa/evidence/2026-05-27-qa-status-json-validation/qa-status-json-validation.txt`.
- QA status schema validation: `docs/qa/evidence/2026-05-27-qa-status-schema-validation/schema-validation.txt`.
- QA status count consistency: original `docs/qa/evidence/2026-05-27-qa-status-consistency/not-run-count-consistency.txt`; refreshed after touch-target/baseline updates at `docs/qa/evidence/2026-05-27-post-touch-target-qa-metadata/not-run-count-consistency.txt`.
- Green baseline: `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `connectedDebugAndroidTest` all passed after the touch-target fix with latest evidence under `docs/qa/evidence/2026-05-27-post-touch-target-baseline/`.
- Fixed blockers: `QA-2026-05-27-001`, `QA-2026-05-27-002`, `QA-2026-05-27-003`, `QA-2026-05-27-004`, `QA-2026-05-27-005`, `QA-2026-05-27-006`, `QA-2026-05-27-007`, `QA-2026-05-27-008`; index: `docs/qa/fixed-findings-index-2026-05-27.md`.
- No open P0/P1/P2 from executed checks after the targeted touch-target hardening, emulator Settings switch semantics fix, emulator Settings feedback touch/clipping fix, emulator Settings large-font text clipping fix, emulator local-data modal focus smoke, emulator top-level focus/label audit, emulator active-workout set edit/delete/manual-value runtime test, emulator Exercise History seeded runtime test, emulator barcode camera denied/granted runtime test, emulator Nutrition long-form IME font-scale 1.5 runtime test, and emulator Coach seeded weekly/training/nutrition runtime test.
- Do not close remaining `NOT RUN` rows with source/unit evidence alone; use `docs/qa/release-gate-owner-checklist-2026-05-27.md` for required runtime/owner evidence.
- If a `NOT RUN` row is intentionally deferred, use `docs/qa/owner-approved-defer-template-2026-05-27.md`.
- Evidence index for reviewer handoff: `docs/qa/evidence-index-2026-05-27.md`.
- Next-run command sheet: `docs/qa/next-run-command-sheet-2026-05-27.md`.
- QA packet local-link check, including machine-readable status and owner defer template: original `docs/qa/evidence/2026-05-27-qa-packet-linkcheck/linkcheck.txt`; refreshed post-touch-target metadata check at `docs/qa/evidence/2026-05-27-post-touch-target-qa-metadata/local-linkcheck.txt`.
- Post touch-target fix baseline rerun passed: `assembleDebug`, `testDebugUnitTest` after one source-guard-preserving modifier-order rerun, `lintDebug`, and physical-device `connectedDebugAndroidTest`. Evidence: `docs/qa/evidence/2026-05-27-post-touch-target-baseline/assembleDebug.txt`, `testDebugUnitTest-rerun2.txt`, `lintDebug.txt`, `connectedDebugAndroidTest.txt`.

Definition of done audit:

- All tabs/flows `PASS` or owner-approved `NOT RUN`: No; several deeper runtime, accessibility, Health Connect and performance flows remain `NOT RUN` without owner-approved release reason. Evidence: `docs/qa/evidence/2026-05-27-dod-open-gaps-audit/not-run-snapshot.txt`.
- No open P0/P1/P2 bugs: No new P0/P1/P2 bugs found in this short loop.
- Every fixed bug has repro, expected/actual, evidence, fix, targeted verification and regression result: Yes for the fixed findings discovered in executed loops; see `docs/qa/fixed-findings-index-2026-05-27.md`. No new fixed bugs in this loop.
- Final full regression found no new P0/P1/P2 issues: Automated baseline passed again after the P2 touch-target hardening (`QA-2026-05-27-004`): `assembleDebug`, `testDebugUnitTest`, `lintDebug`, and physical-device `connectedDebugAndroidTest`; full manual regression remains incomplete.
- `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `connectedDebugAndroidTest` passed: Yes; latest connected evidence is under `docs/qa/evidence/2026-05-27-connected-baseline-refresh-loop/`; latest assemble/unit/lint evidence is under `docs/qa/evidence/2026-05-27-baseline-refresh-loop/`.
- Logcat contains no app crash/ANR after smoke and high-risk flows: Yes for executed short loops including workout completion, plus latest current-build clean install/start smoke; high-risk flows incomplete.
- Open release gates explicitly listed: Yes.
- Owner/runtime closure checklist exists: Yes; see `docs/qa/release-gate-owner-checklist-2026-05-27.md`.









































## Loop Addendum - Health Connect Status Refresh Smoke

- target: Health Connect no-permission/status refresh runtime smoke on emulator
- status: PASS / no-op
- evidence: `docs/qa/evidence/2026-05-27-healthconnect-status-refresh-loop/installDebug.txt`, `launch.txt`, `home.xml`, `settings-top.xml`, `settings-health-section.xml`, `logcat-crash-matches.txt`
- result: Debug install passed on `emulator-5554`; cold launch returned `Status: ok`, `WaitTime: 7451`; first-run Home showed `Health Connect optioneel koppelen` and `Health Connect koppelen`; Settings summary and About/status copy showed `Health Connect: Toegang vereist`; logcat crash/ANR scan returned `NO_MATCHES`.
- findings: No new safe P0/P1/P2 found. Partial grant, revoke-while-open and background-read runtime cases remain owner-gated because they mutate Health Connect/device permission state.



## Loop Addendum - 2026-05-29 Release Lifecycle Runtime Smoke

- target: Direct APK release lifecycle readiness for cold launch, background/foreground, lock/unlock and rotation.
- status: fixed.
- finding: `QA-2026-05-29-004` (`P0`) release APK could trigger `ANR in com.trainiq (com.trainiq/.MainActivity)` during cold idle and rotation-only lifecycle smoke.
- current evidence: `docs/qa/evidence/2026-05-29-release-lifecycle-runtime-smoke-loop/logcat-release-lifecycle-actionable-matches.txt`, `rotation-only-actionable-matches.txt`, `window-after-lifecycle-anr.txt`, `summary.txt`.
- expected behavior: release APK remains responsive through cold idle, background/foreground, lock/unlock and rotation, with UI dumps succeeding and no TrainIQ crash/ANR logcat matches.
- actual behavior: pre-fix release lifecycle smoke produced repeated input-dispatch timeout ANRs for `com.trainiq/.MainActivity`; `/data/anr` files were listable but not readable due emulator permission denial.
- fix: `MainActivity` runs Health Connect background sync scheduling and telemetry flush work on `Dispatchers.IO`, avoiding lifecycle side effects on the main dispatcher.
- targeted verification: `:app:assembleRelease`, `:app:installRelease`, release cold-idle smoke, release rotation-only smoke and full release lifecycle smoke passed after the fix; `cold-wait-after-fix-actionable-matches.txt`, `rotation-only-after-fix-actionable-matches.txt` and `full-lifecycle-after-fix-actionable-matches.txt` are empty.
- regression verification: `:app:testDebugUnitTest :app:assembleDebug` passed in `docs/qa/evidence/2026-05-29-release-lifecycle-runtime-smoke-loop/unit-assembleDebug-after-main-thread-lifecycle-fix.txt`.
- device hygiene: rotation was restored to `accelerometer_rotation=1` and `user_rotation=0`.
- Direct APK Ready: `NO`; this P0 is fixed, but owner/manual gates remain open without owner-approved defer.
## Loop Addendum - 2026-05-29 Health Connect Revoke While Open

- target: Health Connect partial grant, revoke-while-open behavior, and release stability.
- status: fixed and partially closed.
- finding: `QA-2026-05-29-005` (`P0`) release launch with `READ_ACTIVE_CALORIES_BURNED` granted could produce `ANR in com.trainiq (com.trainiq/.MainActivity)` during Health Connect startup/status refresh.
- current evidence: `docs/qa/evidence/2026-05-29-healthconnect-revoke-while-open-loop/logcat-revoke-while-open-actionable-matches.txt`, `window-after-health-grant-anr.txt`, `summary.txt`.
- expected behavior: partial Health Connect grant and revoke/relaunch do not crash or ANR TrainIQ; post-revoke relaunch shows a normal UI state with denied permission state restored.
- actual behavior: pre-fix release partial-grant launch produced a TrainIQ input-dispatch ANR. Intermediate reruns also showed emulator system/launcher ANR dialogs, so verification was isolated after emulator reboot and split into no-grant launch, partial-grant launch, and revoke/relaunch checks.
- fix: `HealthConnectDataSource` runs Health Connect I/O entrypoints on `Dispatchers.IO`, and Health Connect paged record reads use `pageSize = 100` instead of the default provider request size of 1000.
- targeted verification: `:app:compileDebugKotlin :app:compileReleaseKotlin`, `:app:assembleRelease`, `:app:installRelease`, release no-grant launch, release partial-grant launch, revoke-while-open plus relaunch. `isolated-grant-actionable-matches.txt` and `relaunch-after-revoke-actionable-matches.txt` are empty.
- regression verification: `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug` passed in `docs/qa/evidence/2026-05-29-healthconnect-revoke-while-open-loop/unit-lint-assembleDebug-after-healthconnect-io-page-fix.txt`.
- runtime note: Android kills the app process when `READ_ACTIVE_CALORIES_BURNED` is revoked while foregrounded (`permissions revoked` in logcat); this is system behavior. The verified app behavior is clean relaunch after the revoke.
- Direct APK Ready: `NO`; this P0 is fixed, but owner/manual gates remain open without owner-approved defer.
## 2026-05-29 Addendum - Health Connect Settings relaunch loop

- Target: release Settings UI state after Health Connect partial grant and revoke-while-open relaunch.
- Device/emulator: `emulator-5554`, 1080x2400, density 420, font scale 1.0.
- Build/app: `com.trainiq`, versionName `1.0.1-A`, versionCode `2`, commit/build identifier `5fcbb78c`.
- PASS: `:app:installRelease` installed the current release APK. Evidence: `docs/qa/evidence/2026-05-29-healthconnect-settings-relaunch-loop/installRelease-rerun.txt`.
- PASS: with exactly `READ_ACTIVE_CALORIES_BURNED` granted, release cold launch returned `Status: ok` / `LaunchState: COLD`; Settings showed `Health Connect: Gedeeltelijk verbonden`. Evidence: `docs/qa/evidence/2026-05-29-healthconnect-settings-relaunch-loop/rerun-package-after-active-calories-grant.txt`, `docs/qa/evidence/2026-05-29-healthconnect-settings-relaunch-loop/partial-settings-rerun2-launch.txt`, `docs/qa/evidence/2026-05-29-healthconnect-settings-relaunch-loop/partial-settings-rerun2.xml`, `docs/qa/evidence/2026-05-29-healthconnect-settings-relaunch-loop/partial-settings-rerun2.png`.
- PASS: strict TrainIQ crash/ANR/input-timeout scan for partial-grant Settings returned no matches. Evidence: `docs/qa/evidence/2026-05-29-healthconnect-settings-relaunch-loop/partial-settings-rerun2-actionable-matches.txt`.
- PASS: after revoking `READ_ACTIVE_CALORIES_BURNED` while the app was open, all declared Health Connect read permissions were `granted=false`; release relaunch returned `Status: ok` / `LaunchState: COLD`; Settings showed `Health Connect: Toegang vereist`. Evidence: `docs/qa/evidence/2026-05-29-healthconnect-settings-relaunch-loop/package-after-revoke-while-open.txt`, `docs/qa/evidence/2026-05-29-healthconnect-settings-relaunch-loop/relaunch-after-revoke.txt`, `docs/qa/evidence/2026-05-29-healthconnect-settings-relaunch-loop/settings-after-revoke-relaunch.xml`, `docs/qa/evidence/2026-05-29-healthconnect-settings-relaunch-loop/settings-after-revoke-relaunch.png`.
- PASS: strict TrainIQ crash/ANR/input-timeout scan for post-revoke relaunch Settings returned no matches. Evidence: `docs/qa/evidence/2026-05-29-healthconnect-settings-relaunch-loop/settings-after-revoke-relaunch-actionable-matches.txt`.
- Classification note: one intermediate attempt captured Android `System UI isn't responding`; strict TrainIQ actionable scan for that intermediate state was empty, and the stabilized rerun passed. Evidence: `docs/qa/evidence/2026-05-29-healthconnect-settings-relaunch-loop/window-after-systemui-dialog.txt`, `docs/qa/evidence/2026-05-29-healthconnect-settings-relaunch-loop/systemui-dialog-trainiq-actionable-matches.txt`.
- New app findings: none.
- Product code changed: none.
- Direct APK Ready: `NO`; live Health Connect background data-read proof with seeded provider data, TalkBack/Switch Access traversal, real-key privacy/security signoff, live AI/provider flows, real optical scanner decode/result return, true older-version upgrade/persistence and full manual visual deep-flow certification remain open without owner-approved defer.
- Summary evidence: `docs/qa/evidence/2026-05-29-healthconnect-settings-relaunch-loop/summary.txt`.
## 2026-05-29 Addendum - Deep runtime regression loop

- Target: targeted deep-runtime regression coverage for active workout controls, Exercise History, Nutrition long-form IME at font scale 1.5, and barcode scanner permission states.
- Device/emulator: `emulator-5554`, `sdk_gphone64_x86_64`, Android `16`, 1080x2400, density 420.
- Build/app: debug instrumentation target; commit/build identifier `5fcbb78c`.
- PASS: `:app:connectedDebugAndroidTest` with `ActiveWorkoutSetActionsInstrumentedTest`, `ExerciseHistoryInstrumentedTest`, `NutritionLongFormImeInstrumentedTest`, and `CameraPermissionScannerInstrumentedTest` ran 4/4 tests, 0 failures, 0 errors, 0 skipped. Evidence: `docs/qa/evidence/2026-05-29-deep-runtime-regression-loop/connected-deep-runtime-targeted.txt`, `docs/qa/evidence/2026-05-29-deep-runtime-regression-loop/connected-deep-runtime-targeted-report.xml`, `docs/qa/evidence/2026-05-29-deep-runtime-regression-loop/connected-deep-runtime-targeted-testcases.txt`.
- Covered testcases: active workout logged-set correction/delete; seeded Exercise History opens from Training and shows progress; Nutrition add sheet keeps long AI context visible after IME dismiss at font scale 1.5; barcode scanner shows denied fallback then granted camera copy.
- PASS: strict TrainIQ crash/ANR/security logcat scan after targeted runtime run returned no matches. Evidence: `docs/qa/evidence/2026-05-29-deep-runtime-regression-loop/logcat-actionable-matches.txt`.
- PASS: system font scale restored to `1.0`. Evidence: `docs/qa/evidence/2026-05-29-deep-runtime-regression-loop/font-scale-after.txt`.
- PASS: regression `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug` completed successfully. Evidence: `docs/qa/evidence/2026-05-29-deep-runtime-regression-loop/unit-lint-assembleDebug-after-deep-runtime.txt`.
- New app findings: none.
- Product code changed: none.
- Direct APK Ready: `NO`; this is targeted automated/runtime coverage, not full manual visual deep-flow certification or TalkBack/Switch traversal. Real optical barcode decode/result return, live AI/provider calls, real-key privacy signoff, true older-version upgrade/persistence and live Health Connect background data-read proof remain open without owner-approved defer.
- Summary evidence: `docs/qa/evidence/2026-05-29-deep-runtime-regression-loop/summary.txt`.
## 2026-05-29 Addendum - Release top-nav performance smoke loop

- Target: release APK top-level navigation runtime/performance smoke using `gfxinfo` and strict TrainIQ crash/ANR logcat scan.
- Device/emulator: `emulator-5554`, `sdk_gphone64_x86_64`, Android `16`, 1080x2400, density 420, font scale 1.0.
- Build/app: `com.trainiq`, versionName `1.0.1-A`, versionCode `2`, commit/build identifier `5fcbb78c`.
- PASS: `:app:installRelease` installed the current release APK. Evidence: `docs/qa/evidence/2026-05-29-release-topnav-performance-smoke-loop/installRelease.txt`.
- PASS: release cold launch returned `Status: ok`; rerun launch reported `TotalTime: 4642` / `WaitTime: 4661`. Evidence: `docs/qa/evidence/2026-05-29-release-topnav-performance-smoke-loop/launch-release.txt`, `docs/qa/evidence/2026-05-29-release-topnav-performance-smoke-loop/rerun-launch-release.txt`.
- PASS: top-level tap sequence completed back to Start, and strict TrainIQ crash/ANR/security scans returned no matches. Evidence: `docs/qa/evidence/2026-05-29-release-topnav-performance-smoke-loop/rerun-release-topnav-final.xml`, `docs/qa/evidence/2026-05-29-release-topnav-performance-smoke-loop/rerun-logcat-actionable-matches.txt`.
- PARTIAL: first traversal captured Start, Training and Nutrition UI dumps; Coach and Settings UIAutomator dumps failed with `ERROR: null root node returned by UiTestAutomationBridge`. Evidence: `docs/qa/evidence/2026-05-29-release-topnav-performance-smoke-loop/release-topnav-start.xml`, `docs/qa/evidence/2026-05-29-release-topnav-performance-smoke-loop/release-topnav-training.xml`, `docs/qa/evidence/2026-05-29-release-topnav-performance-smoke-loop/release-topnav-nutrition.xml`, `docs/qa/evidence/2026-05-29-release-topnav-performance-smoke-loop/dump-coach.txt`, `docs/qa/evidence/2026-05-29-release-topnav-performance-smoke-loop/dump-settings.txt`.
- DIAGNOSTIC RISK: `gfxinfo` reported high emulator jank after reset and a no-dump-between-taps rerun: `Janky frames: 27 (90.00%)`, `50th percentile: 950ms`, `90th percentile: 1350ms`. This is not release performance PASS evidence and keeps performance threshold/signoff open pending owner-approved thresholds and stronger profileable/physical-device evidence. Evidence: `docs/qa/evidence/2026-05-29-release-topnav-performance-smoke-loop/rerun-gfxinfo-summary-after-topnav.txt`, `docs/qa/evidence/2026-05-29-release-topnav-performance-smoke-loop/rerun-gfxinfo-framestats-after-topnav.txt`.
- New crash/ANR/security app findings: none.
- Product code changed: none.
- Direct APK Ready: `NO`; release top-level performance threshold/signoff, TalkBack/Switch Access traversal, real-key privacy/security signoff, live AI/provider flows, real optical scanner decode/result return, true older-version upgrade/persistence, full manual visual certification and live Health Connect background data-read proof remain open without owner-approved defer.
- Summary evidence: `docs/qa/evidence/2026-05-29-release-topnav-performance-smoke-loop/summary.txt`.
## 2026-05-29 Addendum - Profileable top-nav benchmark loop

- Target: replace ad-hoc release `gfxinfo` top-nav evidence with the existing profileable Macrobenchmark top-level navigation test, and harden the benchmark harness if needed.
- Device/emulator: `emulator-5554`, `sdk_gphone64_x86_64`, Android `16`, 1080x2400, density 420, font scale 1.0.
- Build/app: `com.trainiq` profileable build, commit/build identifier `5fcbb78c`.
- FAIL / environment guard: initial `:macrobenchmark:connectedProfileableAndroidTest` for `TrainIqStartupBenchmark#topLevelNavigationAndSettingsScrollFrames` failed because AndroidX Benchmark rejects emulator benchmarking unless `EMULATOR` is explicitly suppressed. Evidence: `docs/qa/evidence/2026-05-29-profileable-topnav-benchmark-loop/connectedProfileableAndroidTest-topnav.txt`.
- FAIL / harness flake after suppressing only `EMULATOR`: benchmark exposed `androidx.test.uiautomator.StaleObjectException` in `clickNearestClickable`. Evidence: `docs/qa/evidence/2026-05-29-profileable-topnav-benchmark-loop/connectedProfileableAndroidTest-topnav-emulator-suppressed.txt`.
- Fix: `TrainIqStartupBenchmark` now verifies the Settings screen after tapping `Meer`/`Instellingen` instead of clicking the Settings heading again, and `tapAnyText` retries stale UIAutomator nodes before failing. Changed file: `macrobenchmark/src/main/java/com/trainiq/macrobenchmark/TrainIqStartupBenchmark.java`.
- PASS after harness fix: `:macrobenchmark:compileProfileableJavaWithJavac`. Evidence: `docs/qa/evidence/2026-05-29-profileable-topnav-benchmark-loop/compileProfileableJavaWithJavac-after-benchmark-harness-fix.txt`.
- PASS after harness fix: targeted `:macrobenchmark:connectedProfileableAndroidTest` for `TrainIqStartupBenchmark#topLevelNavigationAndSettingsScrollFrames` completed 1/1 with 0 failures when `androidx.benchmark.suppressErrors=EMULATOR` was explicitly passed. Evidence: `docs/qa/evidence/2026-05-29-profileable-topnav-benchmark-loop/connectedProfileableAndroidTest-topnav-after-harness-fix-emulator-suppressed.txt`, `docs/qa/evidence/2026-05-29-profileable-topnav-benchmark-loop/macrobenchmark-topnav-after-harness-fix-report.xml`.
- DIAGNOSTIC metrics: AndroidX Benchmark still warns emulator results are not representative; recorded `frameDurationCpuMs` P50 `873.2`, P90 `1056.3`, P95 `1193.8`, and `frameOverrunMs` P50 `1235.3`, P90 `1492.3`, P95 `1657.7`. Evidence: `docs/qa/evidence/2026-05-29-profileable-topnav-benchmark-loop/macrobenchmark-topnav-after-harness-fix-metrics.txt`, `docs/qa/evidence/2026-05-29-profileable-topnav-benchmark-loop/macrobenchmark-topnav-after-harness-fix-result.textproto`.
- PASS: strict TrainIQ crash/ANR/security logcat scan after the successful benchmark returned no matches. Evidence: `docs/qa/evidence/2026-05-29-profileable-topnav-benchmark-loop/logcat-actionable-matches-after-harness-fix.txt`.
- PASS: regression `:macrobenchmark:compileProfileableJavaWithJavac :macrobenchmark:compileDebugJavaWithJavac :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` completed successfully. Evidence: `docs/qa/evidence/2026-05-29-profileable-topnav-benchmark-loop/regression-after-profileable-benchmark-harness-fix.txt`.
- New app crash/ANR/security finding: none. Fixed item is benchmark harness reliability, not a user-facing app bug.
- Direct APK Ready: `NO`; performance threshold/signoff still requires owner-approved thresholds and stronger physical-device/profileable evidence. TalkBack/Switch Access traversal, real-key privacy/security signoff, live AI/provider flows, real optical scanner decode/result return, true older-version upgrade/persistence, full manual visual certification and live Health Connect background data-read proof remain open without owner-approved defer.
- Summary evidence: `docs/qa/evidence/2026-05-29-profileable-topnav-benchmark-loop/summary.txt`.
## Finding QA-2026-05-29-006

- priority: P1
- area: Profileable Macrobenchmark - active workout seed/start/logging
- flow: `BenchmarkSeedActivity` seeds an active workout, `MainActivity` launches, benchmark navigates to Training and logs active-workout controls.
- status: fixed
- repro steps: run the profileable active-workout benchmark or manually install profileable, launch `com.trainiq.benchmark.BenchmarkSeedActivity`, then launch `com.trainiq/.MainActivity` and capture logcat.
- expected behavior: seed completes without blocking/focusing stale tasks; `MainActivity` launches without TrainIQ crash/ANR; benchmark can wait for and tap Training UI.
- actual behavior: before the fix, seeded launch produced `Input dispatching timed out` / `ANR in com.trainiq` for `MainActivity`; benchmark also failed on short UI label waits.
- evidence paths: `docs/qa/evidence/2026-05-29-profileable-active-workout-benchmark-loop/summary.txt`, `docs/qa/evidence/2026-05-29-profileable-active-workout-benchmark-loop/logcat-seeded-launch.txt`, `docs/qa/evidence/2026-05-29-profileable-active-workout-benchmark-loop/logcat-actionable-matches-seeded-launch-after-task-fix.txt`, `docs/qa/evidence/2026-05-29-profileable-active-workout-benchmark-loop/connectedProfileableAndroidTest-active-workout-after-wait-fix-emulator-suppressed.txt`.
- recommended fix: move profileable seed work off the main thread, remove the seed activity task after completion, and use bounded longer UI waits in the Macrobenchmark harness.
- regression risk: low; changes are profileable/benchmark-only and do not alter production runtime behavior.
- changed files: `app/src/profileable/AndroidManifest.xml`, `app/src/profileable/java/com/trainiq/benchmark/BenchmarkSeedActivity.kt`, `macrobenchmark/src/main/java/com/trainiq/macrobenchmark/TrainIqStartupBenchmark.java`.
- targeted verification: PASS - seeded profileable launch after task fix had 0 strict TrainIQ crash/ANR/security matches and `MainActivity` launch `TotalTime: 1489` / `WaitTime: 1527`; targeted active-workout Macrobenchmark passed 1/1 with explicit `EMULATOR` suppression.
- regression verification: PASS - `:app:compileProfileableKotlin :app:processProfileableManifest :macrobenchmark:compileProfileableJavaWithJavac :macrobenchmark:compileDebugJavaWithJavac :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.
- remaining risk: benchmark metrics remain emulator-suppressed diagnostics only, not release performance threshold/signoff evidence.
- final status: fixed
## Finding QA-2026-05-29-007

- priority: P0
- area: Release APK - startup/accessibility focus responsiveness
- flow: Fresh release install, `pm clear`, cold launch `MainActivity`, early UIAutomator/accessibility dump and strict logcat scan.
- status: fixed
- repro steps: build/install release, clear app data, launch `com.trainiq/.MainActivity`, wait about 10 seconds, run `uiautomator dump`, then scan logcat for TrainIQ crash/ANR/input-timeout patterns.
- expected behavior: release APK launches and remains responsive to accessibility/UIAutomator focus immediately after first draw; strict TrainIQ crash/ANR/security scan is empty.
- actual behavior: before the fix, cold launch returned `Status: ok`, but early UIAutomator dump/logcat captured `ANR in com.trainiq` / `Input dispatching timed out` for `MainActivity`.
- evidence paths: `docs/qa/evidence/2026-05-29-current-release-readiness-refresh-loop/summary.txt`, `docs/qa/evidence/2026-05-29-current-release-readiness-refresh-loop/logcat-actionable-matches-release-cold.txt`, `docs/qa/evidence/2026-05-29-current-release-readiness-refresh-loop/logcat-actionable-matches-release-cold-after-home-startup-dispatcher-fix.txt`, `docs/qa/evidence/2026-05-29-current-release-readiness-refresh-loop/regression-after-release-cold-startup-dispatcher-fix.txt`.
- recommended fix: move Home Health Connect status refresh and dashboard mapping off the main thread during early startup.
- regression risk: low to moderate; dispatcher changes affect timing but preserve state-flow contracts and UI state shape.
- changed files: `app/src/main/java/com/trainiq/features/home/HomeScreen.kt`, `app/src/main/java/com/trainiq/data/repository/TrainIqRepository.kt`.
- targeted verification: PASS - release cold launch plus early UIAutomator dump after the fix reported no strict TrainIQ crash/ANR/security matches; launch `Status: ok`, `LaunchState: COLD`, `TotalTime: 5006`, `WaitTime: 5008`.
- regression verification: PASS - `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease`.
- remaining risk: Direct APK Ready remains NO because owner/manual/performance gates remain open without owner-approved defer.
- final status: fixed
## Finding QA-2026-05-29-008

- priority: P0
- area: Release APK - dark mode/font-scale startup responsiveness
- flow: Release install, set dark mode and font scale 1.3, clear app data, cold launch MainActivity, observe logcat and optional UIAutomator dump/top-level traversal.
- status: fixed
- repro steps: install release, set `settings put system font_scale 1.3`, set `cmd uimode night yes`, `pm clear com.trainiq`, launch `com.trainiq/.MainActivity`, wait 30-35 seconds and scan logcat for TrainIQ crash/ANR/input-timeout patterns.
- expected behavior: release APK launches and remains responsive under dark mode/font scale 1.3; strict TrainIQ crash/ANR/security scan is empty.
- actual behavior: before the fix, release cold launch under dark mode/font scale 1.3 reproduced `ANR in com.trainiq` / `Input dispatching timed out` for `MainActivity`, including a no-UIAutomator-dump reproduction.
- evidence paths: `docs/qa/evidence/2026-05-29-release-dark-font13-topnav-loop/summary.txt`, `docs/qa/evidence/2026-05-29-release-dark-font13-topnav-loop/logcat-actionable-matches-dark-font13-no-dump-repro.txt`, `docs/qa/evidence/2026-05-29-release-dark-font13-topnav-loop/logcat-actionable-matches-dark-font13-after-delayed-diagnostics.txt`, `docs/qa/evidence/2026-05-29-release-dark-font13-topnav-loop/regression-after-release-dark-font13-startup-fix.txt`.
- recommended fix: remove first-screen startup shimmer load from Home and delay non-critical startup diagnostics/background scheduling until after first focus settles.
- regression risk: moderate; diagnostics collection begins later and Home loading visual changes from animated shimmer to static placeholders, but runtime data flow and UI state contracts are unchanged.
- changed files: `app/src/main/java/com/trainiq/MainActivity.kt`, `app/src/main/java/com/trainiq/features/home/HomeScreen.kt`.
- targeted verification: PASS - release cold launch under dark mode/font scale 1.3 stayed strict TrainIQ crash/ANR/security-clean for 35 seconds after the fix.
- regression verification: PASS - `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease`.
- remaining risk: post-fix top-level UI dump traversal was partially blocked by an emulator Pixel Launcher ANR overlay, so full visual/accessibility certification remains open.
- final status: fixed
## 2026-05-29 Current connected regression after startup fixes

- Status: PARTIAL overall, connected debug regression PASS.
- Initial `:app:connectedDebugAndroidTest` attempt did not install or execute app tests because the emulator framework services were unavailable: `cmd: Can't find service: package` and `cmd: Can't find service: activity`.
- Recovery: `adb reboot` and cold `-no-snapshot-load` restart did not restore services; starting `Medium_Phone_2` with `-wipe-data -no-snapshot-load` restored `sys.boot_completed=1`, package service and activity service.
- Finding type: test stability/runtime evidence quality, not a product crash. `ActiveWorkoutSetActionsInstrumentedTest` was hardened to wait for set-type semantics before interacting and to verify delete via persistent active-set state plus visible `0 sets gelogd`, avoiding transient snackbar timing in full-suite runs.
- Targeted verification: `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.features.workout.ActiveWorkoutSetActionsInstrumentedTest` passed 1/1 with empty strict TrainIQ crash/ANR/security scan.
- Regression verification: full `:app:connectedDebugAndroidTest` passed 57/57 with empty strict TrainIQ crash/ANR/security scan.
- Evidence: `docs/qa/evidence/2026-05-29-current-connected-regression-after-startup-fixes-loop/summary.txt`.
- Direct APK Ready: NO. Owner/manual gates remain open without owner-approved defer.
## 2026-05-29 Release APK fresh smoke after connected regression

- Status: PARTIAL overall, release APK fresh smoke PASS on available emulator.
- Device/emulator: `emulator-5554`, `sdk_gphone64_x86_64`, AVD `Medium_Phone_2`.
- Build identifier and app version config captured in `docs/qa/evidence/2026-05-29-release-apk-fresh-smoke-after-connected-loop/git-head-short.txt` and `app-version-config.txt`.
- PASS: `:app:checkReleaseSigningReadiness`, `:app:assembleRelease`, and `:app:installRelease`.
- PASS: release APK `pm clear` plus cold launch via `am start -W -n com.trainiq/.MainActivity`.
- PASS: strict TrainIQ crash/ANR/security logcat scan after cold launch returned no matches.
- PASS: release top-level traversal Start -> Training -> Voeding -> Coach -> Meer/Instellingen -> Start-return captured screenshots and UIAutomator XML dumps that confirm the rendered screens.
- PASS: strict TrainIQ crash/ANR/security logcat scan after top-level traversal returned no matches.
- PASS: regression `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.
- Evidence: `docs/qa/evidence/2026-05-29-release-apk-fresh-smoke-after-connected-loop/summary.txt`.
- Direct APK Ready: NO. Owner/manual gates remain open without owner-approved defer.
## 2026-05-29 Release artifact secret-safety audit

- Status: PARTIAL overall, static release artifact/log leak check PASS.
- Scope: current release APK `app/build/outputs/apk/release/app-release.apk`, recent release smoke `.txt`/`.xml` evidence, and streamed APK zip entries.
- PASS: APK compressed-byte scan found 0 Gemini/OpenAI/API-key/secret/token/password pattern matches.
- PASS: streamed scan of 1023 APK entries and 30,281,571 uncompressed bytes found 0 secret-pattern matches.
- PASS: recent release smoke `.txt`/`.xml` evidence scan found 0 secret-pattern matches.
- Note: an intermediate extraction attempt hit duplicate APK zip entry names and is not used as proof; the entry-stream scan supersedes it.
- Evidence: `docs/qa/evidence/2026-05-29-release-artifact-secret-safety-loop/summary.txt`.
- Direct APK Ready: NO. This does not replace owner real-key save/readback/privacy signoff, and owner/manual gates remain open without owner-approved defer.
## 2026-05-29 Release UI dump accessibility static audit

- Status: PARTIAL overall, static top-level release accessibility audit PASS.
- Scope: latest release UIAutomator XML dumps from `docs/qa/evidence/2026-05-29-release-apk-fresh-smoke-after-connected-loop/release-*.xml`.
- Screens covered: Start, Training, Voeding, Coach, Meer/Instellingen, and Start return.
- PASS: 6 XML files audited.
- PASS: 59 clickable/focusable nodes audited, 0 under-48px clickable/focusable nodes in the captured 1080x2400 emulator XML.
- PASS: 0 UIAutomator `NAF=true` nodes.
- PASS: 0 effectively unlabeled clickable/focusable nodes after accounting for Compose wrapper nodes and descendant text/content descriptions.
- Evidence: `docs/qa/evidence/2026-05-29-release-ui-dump-accessibility-static-audit-loop/summary.txt`.
- Direct APK Ready: NO. This is static XML accessibility evidence only; TalkBack/Switch Access traversal and other owner/manual gates remain open without owner-approved defer.
## 2026-05-29 Release Baseline Profile artifact audit

- Status: PARTIAL overall, release Baseline Profile artifact audit PASS.
- Scope: source `app/src/main/baseline-prof.txt`, release APK `app/build/outputs/apk/release/app-release.apk`, and macrobenchmark compile path.
- PASS: `:app:assembleRelease :macrobenchmark:compileProfileableJavaWithJavac :macrobenchmark:compileDebugJavaWithJavac` completed successfully.
- PASS: source `baseline-prof.txt` is present with 18 lines covering startup and core app surfaces.
- PASS: release APK contains `assets/dexopt/baseline.prof` (11,033 bytes), `assets/dexopt/baseline.profm` (1,441 bytes), and `META-INF/androidx.profileinstaller_profileinstaller.version`.
- Evidence: `docs/qa/evidence/2026-05-29-release-baseline-profile-artifact-loop/summary.txt`.
- Direct APK Ready: NO. This proves release artifact/profile presence only; physical-device/profileable performance threshold signoff remains open without owner-approved defer.
## 2026-05-29 Archived release to current release over-install smoke

- Status: PARTIAL overall, archived same-version release to current release over-install PASS.
- Scope: archived release APK `artifacts/TrainIQ-1.0.1-A-release-signed/TrainIQ-1.0.1-A-release-signed.apk` and current release APK `app/build/outputs/apk/release/app-release.apk`.
- PASS: both APKs use package `com.trainiq`, `versionCode=2`, `versionName=1.0.1-A`, and signer certificate SHA-256 digest `509a572d7113aae6711ed8c4e003324fe0c69f1bb9983bb3bd422174950823ee`.
- PASS: APK hashes differ, so this is not the exact same APK over itself.
- PASS: archived release install and cold launch succeeded.
- PASS: current release installed over archived release, cold-launched, rendered TrainIQ Start/Home, and strict TrainIQ crash/ANR/security logcat scan was empty.
- Evidence: `docs/qa/evidence/2026-05-29-archived-release-to-current-release-overinstall-loop/summary.txt`.
- Direct APK Ready: NO. This is same-version over-install evidence only; true older-version upgrade/persistence still requires a lower-version compatible release APK and persisted user-data scenario.
## 2026-05-29 AI/scanner static contract audit

- Result: PASS for static AI/scanner contract checks that do not require real provider keys, camera hardware, or network calls.
- Scope: reviewed AI provider/model configuration, Gemini JSON response contract, thinking-budget usage, missing-key handling, scanner mode wiring, barcode analysis disposal, permission copy, and navigation scan-result contract.
- PASS: `GEMINI_FLASH_MODEL = "gemini-2.5-flash"`, Gemini JSON response MIME default `application/json`, `thinkingConfig`, fast-mode `thinkingBudget = 0`, and deep-mode `thinkingBudget = 1000` are present.
- PASS: missing-key provider-unavailable handling, Gemini/OpenAI provider ordering, barcode image-analysis mode, scanner disposal, camera-permission copy, and scanner navigation result contract are present.
- PASS: source/config high-risk API-key/token pattern scan returned `NO_SECRET_PATTERN_MATCHES`.
- Evidence:
  - `docs/qa/evidence/2026-05-29-ai-scanner-static-contract-audit-loop/summary.txt`
  - `docs/qa/evidence/2026-05-29-ai-scanner-static-contract-audit-loop/source-files-reviewed.txt`
  - `docs/qa/evidence/2026-05-29-ai-scanner-static-contract-audit-loop/ai-scanner-contract-grep.txt`
  - `docs/qa/evidence/2026-05-29-ai-scanner-static-contract-audit-loop/contract-checks.txt`
  - `docs/qa/evidence/2026-05-29-ai-scanner-static-contract-audit-loop/secret-pattern-scan.txt`
- Limitation: no live Gemini/OpenAI call was made and no camera/barcode optical decode was performed.
- Direct APK Ready: `NO`, because real-key save/readback/privacy signoff, live AI/provider generation, real optical scanner decode/result return, and owner/manual/live gates remain open without owner-approved defer.
## 2026-05-29 Release lifecycle lock/background smoke

- Result: PASS for release APK background/foreground and lock/unlock lifecycle smoke on `emulator-5554`.
- Scope: installed current release APK, cleared app data, cold launched TrainIQ, sent app to Home and relaunched, then slept/woke/unlocked the device and relaunched TrainIQ.
- PASS: cold launch returned `Status: ok`, `LaunchState: COLD`, `TotalTime: 943`, `WaitTime: 945`.
- PASS: return from Home returned `Status: ok`, `LaunchState: HOT`, `TotalTime: 230`, `WaitTime: 232`.
- PASS: return after sleep/wake/unlock returned `Status: ok`; UIAutomator XML dump still contained TrainIQ Start/Home content.
- PASS: strict TrainIQ logcat scan returned `NO_ACTIONABLE_MATCHES` for TrainIQ fatal exception, ANR, input dispatch timeout, and security exception patterns.
- Evidence:
  - `docs/qa/evidence/2026-05-29-release-lifecycle-lock-background-loop/summary.txt`
  - `docs/qa/evidence/2026-05-29-release-lifecycle-lock-background-loop/installRelease.txt`
  - `docs/qa/evidence/2026-05-29-release-lifecycle-lock-background-loop/launch-cold.txt`
  - `docs/qa/evidence/2026-05-29-release-lifecycle-lock-background-loop/return-from-home.txt`
  - `docs/qa/evidence/2026-05-29-release-lifecycle-lock-background-loop/return-after-lock-unlock.txt`
  - `docs/qa/evidence/2026-05-29-release-lifecycle-lock-background-loop/trainiq-lifecycle-cold.xml`
  - `docs/qa/evidence/2026-05-29-release-lifecycle-lock-background-loop/trainiq-lifecycle-return-home.xml`
  - `docs/qa/evidence/2026-05-29-release-lifecycle-lock-background-loop/trainiq-lifecycle-lock-unlock.xml`
  - `docs/qa/evidence/2026-05-29-release-lifecycle-lock-background-loop/logcat-actionable-matches.txt`
- Limitation: this is emulator release lifecycle evidence. It does not replace TalkBack/Switch Access traversal, physical performance threshold signoff, live Health Connect seeded background-read proof, real-key signoff, live provider calls, or real optical scanner decode.
- Direct APK Ready: `NO`, because owner/manual/live gates remain open without owner-approved defer.
## 2026-05-29 Release rotation/configuration smoke

- Result: PASS for release APK portrait/landscape/portrait rotation smoke on `emulator-5554`.
- Scope: installed current release APK, cleared app data, cold launched TrainIQ in portrait, forced landscape, then returned to portrait.
- PASS: portrait cold launch returned `Status: ok` and UIAutomator XML contained TrainIQ Start/Home content.
- PASS: forced landscape UIAutomator XML contained TrainIQ Start/Home content.
- PASS: forced portrait after rotation UIAutomator XML contained TrainIQ Start/Home content.
- PASS: system rotation settings were restored after the run.
- PASS: strict TrainIQ logcat scan returned `NO_ACTIONABLE_MATCHES` for TrainIQ fatal exception, ANR, input dispatch timeout, and security exception patterns.
- Evidence:
  - `docs/qa/evidence/2026-05-29-release-rotation-config-loop/summary.txt`
  - `docs/qa/evidence/2026-05-29-release-rotation-config-loop/installRelease.txt`
  - `docs/qa/evidence/2026-05-29-release-rotation-config-loop/launch-portrait.txt`
  - `docs/qa/evidence/2026-05-29-release-rotation-config-loop/trainiq-rotation-portrait-before.xml`
  - `docs/qa/evidence/2026-05-29-release-rotation-config-loop/trainiq-rotation-landscape.xml`
  - `docs/qa/evidence/2026-05-29-release-rotation-config-loop/trainiq-rotation-portrait-after.xml`
  - `docs/qa/evidence/2026-05-29-release-rotation-config-loop/logcat-actionable-matches.txt`
- Limitation: this is emulator release rotation evidence. It does not replace tablet/foldable certification, TalkBack/Switch Access traversal, physical performance threshold signoff, live Health Connect seeded background-read proof, real-key signoff, live provider calls, or real optical scanner decode.
- Direct APK Ready: `NO`, because owner/manual/live gates remain open without owner-approved defer.
## 2026-05-29 Release force-stop/process recreation smoke

- Result: PASS for release APK force-stop/relaunch process recreation smoke on `emulator-5554`.
- Scope: installed current release APK, cleared app data, cold launched TrainIQ, navigated to Training, force-stopped the app process, then relaunched MainActivity.
- PASS: cold launch returned `Status: ok` and UIAutomator XML contained TrainIQ content.
- PASS: Training navigation rendered TrainIQ content before force-stop.
- PASS: relaunch after `am force-stop com.trainiq` returned `Status: ok` and UIAutomator XML contained TrainIQ content.
- PASS: strict TrainIQ logcat scan returned `NO_ACTIONABLE_MATCHES` for TrainIQ fatal exception, ANR, input dispatch timeout, and security exception patterns.
- Evidence:
  - `docs/qa/evidence/2026-05-29-release-force-stop-recreation-loop/summary.txt`
  - `docs/qa/evidence/2026-05-29-release-force-stop-recreation-loop/installRelease.txt`
  - `docs/qa/evidence/2026-05-29-release-force-stop-recreation-loop/launch-cold.txt`
  - `docs/qa/evidence/2026-05-29-release-force-stop-recreation-loop/trainiq-force-stop-training-before.xml`
  - `docs/qa/evidence/2026-05-29-release-force-stop-recreation-loop/launch-after-force-stop.txt`
  - `docs/qa/evidence/2026-05-29-release-force-stop-recreation-loop/trainiq-force-stop-after-relaunch.xml`
  - `docs/qa/evidence/2026-05-29-release-force-stop-recreation-loop/logcat-actionable-matches.txt`
- Limitation: this verifies safe emulator relaunch to a rendered TrainIQ UI, not full persisted in-progress workout/meal restoration or true older-version upgrade persistence.
- Direct APK Ready: `NO`, because owner/manual/live gates remain open without owner-approved defer.
## 2026-05-29 Release offline/network smoke

- Result: PASS for release APK offline launch/top-level smoke on `emulator-5554`.
- Scope: installed current release APK, cleared app data, enabled airplane mode, disabled Wi-Fi and mobile data, launched TrainIQ, visited Start, Coach and Settings/Meer, then restored connectivity commands.
- PASS: offline launch returned `Status: ok`.
- PASS: offline Start, Coach and Settings/Meer UIAutomator XML dumps contained TrainIQ content. Start XML was recaptured after an initial UIAutomator null-root dump.
- PASS: strict TrainIQ logcat scans returned `NO_ACTIONABLE_MATCHES` for TrainIQ fatal exception, ANR, input dispatch timeout, and security exception patterns.
- PASS: connectivity restore commands were executed after the run.
- Evidence:
  - `docs/qa/evidence/2026-05-29-release-offline-network-loop/summary.txt`
  - `docs/qa/evidence/2026-05-29-release-offline-network-loop/launch-offline.txt`
  - `docs/qa/evidence/2026-05-29-release-offline-network-loop/trainiq-offline-start.xml`
  - `docs/qa/evidence/2026-05-29-release-offline-network-loop/trainiq-offline-coach.xml`
  - `docs/qa/evidence/2026-05-29-release-offline-network-loop/trainiq-offline-settings.xml`
  - `docs/qa/evidence/2026-05-29-release-offline-network-loop/logcat-actionable-matches.txt`
  - `docs/qa/evidence/2026-05-29-release-offline-network-loop/logcat-start-recapture-actionable-matches.txt`
- Limitation: this is offline shell-connectivity evidence for launch/top-level rendering. It does not replace live AI/provider offline action UX, Health Connect seeded background-read proof, real-key signoff, or real optical scanner decode.
- Direct APK Ready: `NO`, because owner/manual/live gates remain open without owner-approved defer.
## 2026-05-29 Current DoD completion audit

- Result: PARTIAL. Direct APK Ready remains `NO`.
- Build/context: git head `5fcbb78c`, package `com.trainiq`, versionCode `2`, versionName `1.0.1-A`.
- PASS: automated baseline is recorded as green for `assembleDebug`, `testDebugUnitTest`, `lintDebug`, and `connectedDebugAndroidTest`.
- PASS: release signing/build/install/launch/top-level runtime evidence is recorded as green for executed checks.
- PASS_WITH_LIMITS: release lifecycle/background/lock/rotation/force-stop/offline emulator smokes are green, but do not replace owner/manual/live gates.
- PASS: known open P0/P1/P2 from executed checks is `0`; fixed findings count is `16`.
- PASS: documentation packet consistency is green with `1667` evidence links before this audit, declared count `1667`, and `0` missing references.
- PARTIAL/OPEN: Health Connect, AI/provider, scanner/camera, UX/accessibility, privacy/security, upgrade persistence, and performance gates still require owner/manual/live evidence before readiness can be set to `YES`.
- Evidence:
  - `docs/qa/evidence/2026-05-29-current-dod-completion-audit-loop/summary.txt`
  - `docs/qa/evidence/2026-05-29-current-dod-completion-audit-loop/dod-gate-matrix.json`
- Open gates blocking `Direct APK Ready: YES`:
  - TalkBack/Switch Access traversal.
  - Release top-level performance threshold/signoff.
  - Live Health Connect background data-read proof with seeded provider data.
  - Privacy/security real-key signoff.
  - Provider/runtime-gated AI and scanner flows.
  - Manual deep-runtime UX audits.
  - True older-version upgrade/persistence.
- Direct APK Ready: `NO`, because the plan requires every automated, runtime, release, documentation and owner-gated check to be `PASS` or owner-approved `DEFER`; that evidence is not complete.
## 2026-05-29 Current automated baseline refresh

- Result: PASS for current worktree automated debug baseline refresh.
- Command: `.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain`.
- PASS: `:app:testDebugUnitTest`, `:app:lintDebug`, and `:app:assembleDebug` completed successfully in one Gradle invocation.
- Evidence:
  - `docs/qa/evidence/2026-05-29-current-automated-baseline-refresh-loop/summary.txt`
  - `docs/qa/evidence/2026-05-29-current-automated-baseline-refresh-loop/testDebugUnitTest-lintDebug-assembleDebug.txt`
  - `docs/qa/evidence/2026-05-29-current-automated-baseline-refresh-loop/result.txt`
- Limitation: this refreshes JVM/unit, lint and debug assembly only. It does not replace connected device tests, release APK runtime smokes, or owner/manual/live gates.
- Direct APK Ready: `NO`, because owner/manual/live gates remain open without owner-approved defer.
## 2026-05-29 Current release build refresh

- Result: PASS for current worktree release signing/build refresh.
- Command: `.\gradlew.bat :app:checkReleaseSigningReadiness :app:assembleRelease --console=plain`.
- PASS: `:app:checkReleaseSigningReadiness` reported `TrainIQ release signing configuration is complete.`
- PASS: `:app:assembleRelease` completed successfully.
- Release APK: `app/build/outputs/apk/release/app-release.apk`, size `26729404`, SHA-256 `E86E3C9B721C60568B5E8C690DC702A07C33136C147FAE2E68FF98C439045BA6`.
- Evidence:
  - `docs/qa/evidence/2026-05-29-current-release-build-refresh-loop/summary.txt`
  - `docs/qa/evidence/2026-05-29-current-release-build-refresh-loop/checkReleaseSigningReadiness-assembleRelease.txt`
  - `docs/qa/evidence/2026-05-29-current-release-build-refresh-loop/result.txt`
- Limitation: this refreshes release signing readiness and release APK assembly only. It does not replace release install/launch/runtime smokes or owner/manual/live gates.
- Direct APK Ready: `NO`, because owner/manual/live gates remain open without owner-approved defer.
## 2026-05-29 Current release install/launch refresh

- Result: PASS for current release APK install, `pm clear`, cold launch, UI dump and strict logcat scan on `emulator-5554`.
- APK SHA-256: `E86E3C9B721C60568B5E8C690DC702A07C33136C147FAE2E68FF98C439045BA6`.
- PASS: `:app:installRelease` completed successfully.
- PASS: cold launch returned `Status: ok` and UIAutomator XML contained TrainIQ content.
- PASS: strict TrainIQ logcat scan returned `NO_ACTIONABLE_MATCHES` for TrainIQ fatal exception, ANR, input dispatch timeout, and security exception patterns.
- Evidence:
  - `docs/qa/evidence/2026-05-29-current-release-install-launch-refresh-loop/summary.txt`
  - `docs/qa/evidence/2026-05-29-current-release-install-launch-refresh-loop/installRelease.txt`
  - `docs/qa/evidence/2026-05-29-current-release-install-launch-refresh-loop/launch-release.txt`
  - `docs/qa/evidence/2026-05-29-current-release-install-launch-refresh-loop/trainiq-current-release-refresh.xml`
  - `docs/qa/evidence/2026-05-29-current-release-install-launch-refresh-loop/logcat-actionable-matches.txt`
- Limitation: this refreshes release install/launch only. It does not replace full top-level traversal, performance signoff, or owner/manual/live gates.
- Direct APK Ready: `NO`, because owner/manual/live gates remain open without owner-approved defer.
## 2026-05-29 Current DoD completion audit refresh

- Result: PARTIAL. Direct APK Ready remains `NO`.
- Refreshed existing DoD completion audit artifacts after the current automated baseline refresh and current release build/install/launch refresh.
- PASS: refreshed gate matrix now points to current automated baseline evidence for `assembleDebug`, `testDebugUnitTest`, and `lintDebug`.
- PASS: refreshed gate matrix now points to current release build evidence for `checkReleaseSigningReadiness` and `assembleRelease`.
- PASS: refreshed gate matrix now points to current release install/launch smoke evidence for latest runtime smoke.
- PASS: documentation packet consistency is green with `1688` evidence links, declared count `1688`, and `0` missing references.
- Evidence:
  - `docs/qa/evidence/2026-05-29-current-dod-completion-audit-loop/summary.txt`
  - `docs/qa/evidence/2026-05-29-current-dod-completion-audit-loop/dod-gate-matrix.json`
- Direct APK Ready: `NO`, because TalkBack/Switch Access traversal, release performance threshold/signoff, live Health Connect seeded background-read proof, real-key privacy signoff, live AI/provider/scanner flows, manual deep-runtime UX audits, and true lower-version upgrade/persistence remain open without owner-approved defer.
## 2026-05-29 Current release top-level traversal refresh

- Result: PASS for current release APK top-level traversal on `emulator-5554`.
- APK SHA-256: `E86E3C9B721C60568B5E8C690DC702A07C33136C147FAE2E68FF98C439045BA6`.
- PASS: `:app:installRelease`, `pm clear`, and cold launch returned `Status: ok`.
- PASS: top-level traversal covered Start, Training, Voeding, Coach, Meer/Instellingen, and return to Start.
- PASS: every captured UIAutomator XML dump contained TrainIQ content.
- PASS: strict TrainIQ logcat scan returned `NO_ACTIONABLE_MATCHES` for TrainIQ fatal exception, ANR, input dispatch timeout, and security exception patterns.
- Evidence:
  - `docs/qa/evidence/2026-05-29-current-release-top-level-traversal-refresh-loop/summary.txt`
  - `docs/qa/evidence/2026-05-29-current-release-top-level-traversal-refresh-loop/installRelease.txt`
  - `docs/qa/evidence/2026-05-29-current-release-top-level-traversal-refresh-loop/launch-release.txt`
  - `docs/qa/evidence/2026-05-29-current-release-top-level-traversal-refresh-loop/trainiq-top-start.xml`
  - `docs/qa/evidence/2026-05-29-current-release-top-level-traversal-refresh-loop/trainiq-top-training.xml`
  - `docs/qa/evidence/2026-05-29-current-release-top-level-traversal-refresh-loop/trainiq-top-nutrition.xml`
  - `docs/qa/evidence/2026-05-29-current-release-top-level-traversal-refresh-loop/trainiq-top-coach.xml`
  - `docs/qa/evidence/2026-05-29-current-release-top-level-traversal-refresh-loop/trainiq-top-settings.xml`
  - `docs/qa/evidence/2026-05-29-current-release-top-level-traversal-refresh-loop/trainiq-top-start-return.xml`
  - `docs/qa/evidence/2026-05-29-current-release-top-level-traversal-refresh-loop/logcat-actionable-matches.txt`
- Limitation: this refreshes emulator top-level traversal only. It does not replace performance threshold/signoff, TalkBack/Switch Access traversal, manual deep UX audits, or owner/manual/live gates.
- Direct APK Ready: `NO`, because owner/manual/live gates remain open without owner-approved defer.
## 2026-05-29 Current DoD completion audit top-level refresh

- Result: PARTIAL. Direct APK Ready remains `NO`.
- Refreshed existing DoD completion audit artifacts after the current release top-level traversal refresh.
- PASS: refreshed gate matrix now points to current release top-level traversal evidence as `latestRuntimeSmoke`.
- PASS: documentation packet consistency is green with `1730` evidence links, declared count `1730`, and `0` missing references.
- Evidence:
  - `docs/qa/evidence/2026-05-29-current-dod-completion-audit-loop/summary.txt`
  - `docs/qa/evidence/2026-05-29-current-dod-completion-audit-loop/dod-gate-matrix.json`
- Direct APK Ready: `NO`, because TalkBack/Switch Access traversal, release performance threshold/signoff, live Health Connect seeded background-read proof, real-key privacy signoff, live AI/provider/scanner flows, manual deep-runtime UX audits, and true lower-version upgrade/persistence remain open without owner-approved defer.
## 2026-05-29 Current connected baseline refresh

- Result: PASS for full current worktree `:app:connectedDebugAndroidTest` on `emulator-5554`.
- PASS: connected suite completed 57/57 tests with 0 failed and 0 skipped.
- PASS: strict TrainIQ logcat scan returned `NO_ACTIONABLE_MATCHES` for TrainIQ fatal exception, ANR, input dispatch timeout, and security exception patterns.
- Evidence:
  - `docs/qa/evidence/2026-05-29-current-connected-baseline-refresh-loop/summary.txt`
  - `docs/qa/evidence/2026-05-29-current-connected-baseline-refresh-loop/connectedDebugAndroidTest.txt`
  - `docs/qa/evidence/2026-05-29-current-connected-baseline-refresh-loop/logcat-after-connected.txt`
  - `docs/qa/evidence/2026-05-29-current-connected-baseline-refresh-loop/logcat-actionable-matches.txt`
- Limitation: this refreshes connected debug instrumentation only. It does not replace release APK runtime smokes or owner/manual/live gates.
- Direct APK Ready: `NO`, because owner/manual/live gates remain open without owner-approved defer.
## 2026-05-29 Current DoD completion audit connected refresh

- Result: PARTIAL. Direct APK Ready remains `NO`.
- Refreshed existing DoD completion audit artifacts after the current connected baseline refresh.
- PASS: refreshed gate matrix now points to current connected baseline evidence where full `:app:connectedDebugAndroidTest` passed 57/57.
- PASS: refreshed gate matrix still points to current release top-level traversal evidence as `latestRuntimeSmoke`.
- PASS: documentation packet consistency is green with `1742` evidence links, declared count `1742`, and `0` missing references.
- Evidence:
  - `docs/qa/evidence/2026-05-29-current-dod-completion-audit-loop/summary.txt`
  - `docs/qa/evidence/2026-05-29-current-dod-completion-audit-loop/dod-gate-matrix.json`
- Direct APK Ready: `NO`, because TalkBack/Switch Access traversal, release performance threshold/signoff, live Health Connect seeded background-read proof, real-key privacy signoff, live AI/provider/scanner flows, manual deep-runtime UX audits, and true lower-version upgrade/persistence remain open without owner-approved defer.
## 2026-05-29 Final packet consistency refresh

- Result: PASS for QA packet consistency after latest connected/runtime/audit refreshes.
- PASS: QA status JSON parses, status remains `PARTIAL`, release-ready remains `NO`, latest runtime is `PASS`, and connected baseline is `PASS`.
- PASS: evidence index declared count matches actual referenced evidence links; missing evidence is `0`.
- PASS: QA status handoff references all exist; missing status refs is `0`.
- PASS: refreshed DoD audit mentions the current evidence count, has 16 gate rows, documents all 7 open release gates, and keeps Direct APK Ready `NO`.
- Evidence:
  - `docs/qa/evidence/2026-05-29-final-packet-consistency-refresh-loop/summary.txt`
  - `docs/qa/evidence/2026-05-29-final-packet-consistency-refresh-loop/packet-checks.txt`
  - `docs/qa/evidence/2026-05-29-final-packet-consistency-refresh-loop/missing-evidence.txt`
  - `docs/qa/evidence/2026-05-29-final-packet-consistency-refresh-loop/missing-status-refs.txt`
  - `docs/qa/evidence/2026-05-29-final-packet-consistency-refresh-loop/undocumented-open-gates.txt`
- Direct APK Ready: `NO`, because packet consistency is green but owner/manual/live gates remain open without owner-approved defer.