# B2 — Progress draft safety and return navigation

Status: implementation ready for central verification after root's `BASELINE FROZEN`. B2 performed no Gradle/device operation. No passing test claim is made before C's execution.

## Selected behavior

FIX-05 preserves the typed Progress route and expanded rail. Add a labeled return action in Loading, Error and Success. Coach → Progress → return restores Coach; scanner still returns to Progress first. Compact navigation highlights Coach while on Progress, matching the September body-progress ownership contract. Existing five-destination compact navigation remains.

FIX-07 preserves saveable raw-string drafts and validation. ViewModel owns import request revision, pending state and cancellation. Capture a request token before copying the selected URI; user edit/save, replacement import, camera handoff, explicit cancel or destination exit invalidates that token. A stale copied image must be released before provider admission. `LatestScanRequest` retains provider-cancellation and temporary-image cleanup; a non-cooperative late provider must not publish into a changed draft. Show pending status and a semantic cancel action; fields remain manually editable.

## Planned proof and file ownership

- Progress-focused deferred-result unit tests: unchanged success once; edit/save invalidation; copy-before-admission cancellation; replacement request wins; failure clears pending and permits retry. Use synthetic paths/results and controlled dispatchers, never live provider calls.
- Existing `AdaptiveNavigationPolicyTest`: assert Progress selects Coach while rail and five compact destinations remain intact.
- Existing `CoachHealthNavigationInstrumentedTest`: explicit semantic return plus preserved system Back and scanner-parent continuity as relevant.
- New focused Progress Compose test: pending/cancel affordances, manual edit events, retained draft after recreation, return available in Loading/Error/Success. Do not duplicate Room persistence tests.

Exclusive production files: `TrainIQ-Project/app/src/main/java/com/trainiq/features/progress/ProgressScreen.kt` and `TrainIQ-Project/app/src/main/java/com/trainiq/navigation/TrainIqNav.kt`. Direct corresponding Progress/Coach/navigation tests and this document are B2-owned. Root owns screenshots/devices; C owns Gradle. No dependency, schema, provider, permissions, release or remote mutation belongs to this package.

## Risks and verification

Medium async-input/lifecycle risk for import; low navigation risk. The route's composition lifetime must not retain a callback into a disposed form. Safe cancellation must preserve manual fields and cleanup imported images. Existing public ProgressScreen defaults/test fixtures should remain compatible.

## Implemented changes and evidence

`ProgressPhotoImportSession` is the small ViewModel-owned admission helper in the existing Progress file. It begins before URI copying, exposes pending state through `ProgressUiState.Success`, cancels on edits/save/picker/camera/explicit exit, and rejects stale copied paths without cancelling a newer request. Existing `LatestScanRequest` still releases files after provider completion/cancellation. The screen cancels via `DisposableEffect` with the latest callback when disposed, including recreation, and preserves saveable manual strings. It remains possible to edit/save manually during analysis.

The explicit return action is available before every state branch. From an existing Coach parent it pops that parent; rail/tour entry instead uses existing saved top-level Coach navigation. Android system Back stays unchanged. Compact Progress now selects Coach.

Tests were written before production implementation; no red Gradle execution was dispatched because C owns the single frozen integration run. Added five deferred/admission unit tests in `ProgressPhotoImportSessionTest`, a ViewModel save-vs-analysis test in `ProgressSaveTest`, and three Compose tests in `ProgressPhotoDraftInstrumentedTest` for manual edit/restoration, explicit cancellation/retry and all-state semantic return. Existing navigation policy and real Coach navigation assertions were updated. An existing source-bound import test now retains its import-path assertion with the new token argument; no valid behavior assertion was removed.

`git diff --check` on the exclusive package passed (line-ending advisories only). Build/unit/lint/connected evidence is pending C/root. Exact focused Android targets: `com.trainiq.features.progress.ProgressPhotoDraftInstrumentedTest`, `com.trainiq.features.progress.ProgressDeleteConfirmationInstrumentedTest`, `com.trainiq.features.coach.CoachHealthNavigationInstrumentedTest`. JVM targets: `com.trainiq.features.progress.*`, `com.trainiq.navigation.AdaptiveNavigationPolicyTest`. Broader integrated gates remain root/C-owned.

No schema, dependency, provider/model, key/privacy boundary, permission, signing or remote service change. Root must inspect compact/dark/large-font return/busy/cancel reachability; emulator tests do not establish real-photo recognition quality or physical assistive-technology certification.

First integrated compile failed because the wildcard UI test import selected experimental `StateRestorationTester`, while this fixture uses JUnit4 `ComposeContentTestRule`. After root confirmed the gate process had exited, B corrected only the explicit `androidx.compose.ui.test.junit4.StateRestorationTester` import, matching existing `BarcodeMealFlowInstrumentedTest`. No assertion or test was removed. C's exact failure/next-run evidence is recorded in `verification.md`; the repaired snapshot remains unverified until that rerun.

The first full connected run failed `explicitCancelRetainsManualValuesAndNewPhotoCanStart` at its final displayed assertion, after the second request begins. Its first cancel, disappearance and retained `82` assertions passed. The focus/inset/anchor transition remains a hypothesis, not a proven production defect. After root's gate-stop signal, B retained the failing assertion and added only a pending-state assertion plus root/cancel/weight bounds, IME and root-semantics logging for one bounded focused diagnostic. No production change or reachability weakening was made.

The diagnostic initially did not compile because `createComposeRule` exposes no `activity` property. After the diagnostic gate exited, the insets lookup was corrected to the current Compose `LocalView` root, retaining all diagnostic bounds/semantics/insets and the original failing assertion. No runtime test executed in that compile-failed attempt.

The bounded diagnostic then passed once but showed an unstable native window transition: IME was visible with bottom inset 598px; root bounds changed from `(0,0)-(720,1280)` to `(0,-348)-(720,932)` between reads, and the Lazy viewport ended at y334 while the earlier cancel bounds were y586–682. This proves the focused component host was panning asynchronously. Production MainActivity declares `adjustResize`; this generic component activity does not establish a production layout defect. A single diagnostic pass was not treated as closure.

After root confirmed that diagnostic stopped, the test condition was corrected to mimic a subsequent picker result without retained editing focus: an actual visible list drag uses the existing production focus-clear gesture, then a bounded IME-hidden/unpanned-root wait precedes the second request. Original pending, removal, retained-value and displayed assertions remain; the second cancel now additionally requires an actual touch, another disappearance and retained `82`. Temporary diagnostic logging was removed after recording its evidence. No production accessibility/layout change or arbitrary sleep was introduced. Focused rerun remains pending C.

Final integration acceptance supersedes earlier pending statuses: fresh992JVM and full202Android pass including Progress photo/draft/recreation and Coach navigation classes. Root installed-app checks at compact360x640/expanded720x800, dark/light/font1.3/1.5 prove labeled return, correct compactCoach/expandedProgress selection, draft retention through configuration changes, enabled synthetic measurement save and physical return. Real-photo/assistive-technology limits remain. See verification.md.
