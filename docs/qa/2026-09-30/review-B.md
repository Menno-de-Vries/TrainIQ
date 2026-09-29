# B3 independent review — 2026-09-30

Scope: read-only review of A2 FIX-01/02 and their interactions with client draft/import/reset flows. Reviewed the accepted improvement plan, implementation-A handoff, current production diff, new coordinator/Room tests, and relevant existing repository/feature callers. No source/test writes, Gradle/device runs or remote operations. This report owns only `review-B.md`. Runtime and central verification remain C/root-owned.

## Findings requiring owning-stream repair

### REVIEW-B-01 — P1: New manual meals cannot be created

`features/nutrition/NutritionScreen.kt:1357` gives a **new** meal a positive timestamp identity: `editingMealId ?: mealSaveId ?: System.currentTimeMillis().also { mealSaveId = it }`. It passes this ID through `NutritionViewModel.saveMeal` and `SaveMealUseCase`. `data/repository/TrainIqRepository.kt:1156-1165` preserves that ID in `LoggedMealStorage` before calling the resolver overload of runtime `saveMeal`.

The new A2 guard at `RoomTrainIqRuntimeStore.kt:751-752` treats every absent positive ID as a deleted edit. A new timestamp ID is absent by definition. Therefore an ordinary new meal submission now reaches failure rather than persistence. This is a direct source-path contradiction, not a hypothetical concurrency edge. Existing added Room tests create with zero and then edit the allocated ID, so they do not exercise this actual client creation contract.

Smallest complete repair must distinguish create intent from edit intent while retaining duplicate-submit/retry guarantees. One candidate is to send `null` for new entries under the existing pending-submit guard and let Room allocate the ID; remove obsolete reserved-ID state. Another is an explicit repository create command/idempotency contract. Do not weaken the stale-positive-edit guard or simply permit every missing positive ID.

Acceptance/proof: real Nutrition first manual meal and barcode-to-meal saves succeed; double-submit yields one parent; a failed save retains the full draft and can retry; a deleted **edit** cannot recreate either parent or children. Run the existing `NutritionFirstAddInstrumentedTest` / `BarcodeMealFlowInstrumentedTest` or equivalent real route alongside new Room stale-edit tests. Owner/root must settle the creation/idempotency design and broaden the affected nutrition proof accordingly.

### REVIEW-B-02 — P2: Deleted-parent guidance is lost before the user sees it

A2's guard messages ask the user to create a new meal/recipe, but both use ordinary `IllegalArgumentException`. `features/nutrition/MealSave.kt:26-28` catches that under generic Exception and tells the user to retry. Recipe `NutritionViewModel.saveRecipe` (`NutritionScreen.kt:464-465`) also emits generic “check input and retry” feedback. The same stale edit ID can never succeed by retrying.

Draft preservation is correct: neither failure calls `onSaved`, so meal draft reset and recipe editor reset do not run. However the accepted actionable-error requirement is not satisfied at the product boundary. Propagate a narrowly typed safe missing-edit failure and map it to explicit recreate guidance. Do not expose arbitrary throwable messages because generic storage errors intentionally hide private details.

Acceptance/proof: meal and recipe missing-target failures explain the removed parent and next action; success callback stays uncalled and pending guard reopens; generic disk failures keep existing safe retry copy; original draft values remain. Cheapest proof is existing meal-save helper/recipe ViewModel tests, followed by the affected real editor flow where useful. Owner/root must assign exact files before repair because NutritionScreen was outside A2's original exclusive scope.

## Accepted source behavior / no additional defect found

| Area | Current evidence | Review result and limit |
|---|---|---|
| HC full sync serialization | `UserPreferencesRepository` is `@Singleton`; its private coordinator owns one Mutex; `HealthConnectDataSource.getStatus` enters ownership before provider/status/preferences reads | Full read→commit serialized across normal Hilt callers. No manually constructed second preferences instance appears in production source. Pure tests do not prove Android/provider execution |
| HC reset ordering | `clearLocalPrivateData` uses the same coordinator; sync internal metric clear/save methods do not reacquire it | Lock order is HC coordinator → DataStore for both paths; no same-Mutex reentry/deadlock found. Reset waits already running provider work rather than canceling it |
| Cancellation/failure | `withLock` releases on exit; coordinator tests cover canceled waiter/owner and provider/reset failure recovery | Appropriate deterministic ownership proof, no sleep/retry masking. A fresh integration run remains necessary |
| New sync after reset | Settings `clearAllData` refreshes Health Connect after successful clear; clear does not revoke Android access | New post-reset sync may legitimately repopulate cache. A2 correctly limits its claim to an older active operation, not perpetual cache emptiness |
| Room meal guards | Both runtime overloads use the same transaction-local `persistMeal`; guard precedes ID/child writes; resolver builds items inside transaction | Stale target with valid ingredients cannot resurrect. Resolver may fail on an invalid/deleted ingredient before parent guard; that already has safe actionable guidance |
| Room recipe guard | Transaction/mutex cover parent existence and writes; existing date/createdAt preservation retained | No parent/child write on missing target; reopen tests meaningful. Row-ID recycling is explicitly deferred, not falsely claimed protected |
| Progress reset/import interaction | Navigating to Settings disposes Progress; ProgressScreen `DisposableEffect` invalidates photo session using latest cancellation callback; stale admission/provider publication guarded | Safe existing composition exit path. The audit does not invent a cross-screen reset hook or claim cancellation from a process outside the app |
| Pending Progress edits | ViewModel admission helper rejects stale copy without canceling new scan; manual edit/save invalidates; raw strings saveable | Coherent owner policy; B3 does not independently certify its own B2 implementation. Root/A review and fresh tests are separate |

## Coverage/metadata consistency

The product audit accurately labels all 14 typed routes as code-inspected/runtime-not-yet, distinguishes the existing local food gateway from live configured service, preserves the intentional completion auto-return policy, and records Progress as top-level/expanded rail with compact ownership inconsistency. These are baseline observations; implementation disposition belongs in the integrated plan/verification ledger. Do not convert the route inventory into a claim of full physical-camera/provider/audio/assistive/performance coverage.

New Room tests verify failure/no resurrection and survivors after reopen; they do not prove real client creation or display copy. The two findings above fill those specific acceptance gaps. No additional speculative architecture, dependency, schema or UI redesign is proposed.

## A4 repair disposition

A4's stable repair was independently inspected after the stopped compile gate. Both findings are **addressed in source; fresh execution pending**:

- REVIEW-B-01: UI constructs `MealSaveTarget.Edit(editingMealId)` or `Create(savedReservation)`; ViewModel → SaveMealUseCase → FocusedRepository → coordinator → transactional runtime preserve that explicit intent. Missing Edit still throws; reserved Create can insert/retry one parent. Legacy null/positive-ID callers retain create/edit semantics. Room reserved creation/retry/reopen regression and an actual UI first-add→Room test in `NutritionFirstAddInstrumentedTest` now cover the path the original guard tests missed.
- REVIEW-B-02: `MissingMealEditException` is mapped in `performMealSave`; `MissingRecipeEditException` is mapped through `recipeSaveFailureMessage`, used by the recipe ViewModel failure branch. Safe messages explain recreate recovery and draft preservation; generic errors retain their sanitized copy. Success callbacks remain uncalled on these failures. Focused `MealSaveTest` cases prove mapping/guard reopening; exact test execution remains pending C.

No additional blocking source finding arose from the corrected chain. The prior findings and failed integration attempt remain in history here rather than being erased. Reservations retain the pre-existing UI idempotency identity policy; no new protection against unrelated/recycled-ID collisions is claimed.

All 27 named connected classes in the product matrix were checked to exist in their matching Android-test package. `WorkoutProcessing` has no dedicated connected class; a JVM shimmer-policy assertion does not prove its runtime reachability/stack behavior. External dependencies remain NOT RUN unless root records fresh evidence: live HC/provider mutation, live AI/keys/accuracy, FatSecret credentials/retention rights/secure deployment/URL, physical camera/audio/OEM, physical benchmarks and complete human TalkBack/Switch Access. Current-run changed-screen screenshots belong in root's verification ledger, not this static review.

Review status: **source repair accepted pending central verification**. `git diff --check` for this documentation is the worker's only verification; all build/unit/lint/connected/runtime results must come from C/root's exact integrated snapshot.

## Final failure-repair review

Read-only review after the failed full run and bounded diagnostic confirms the final fixtures preserve their material assertions:

- Completion keyboard proof explicitly enters real `InputMode.Keyboard`, asserts that the stay action accepts focus and is focused, dispatches the original DirectionDown key, then retains the no-auto-return assertion after 13 seconds. It does not replace the key with a touch or a direct production callback. Focus itself is a permitted pause interaction; the test proves the combined supported keyboard path, not isolated key-handler coverage or full human screen-reader behavior.
- Exercise History scrolls the lazy statistics item into composition before retaining exact session count `2` and best weight `90`, then scrolls the rank item before retaining score presence/non-clickability. Original title wrapping/no-ellipsis and volume chart assertions remain. A wider timeout or a changed expected value was not substituted for reachability.
- Barcode manual add keeps the first single **physical** add tap with active keyboard, enabled/displayed checks, and draft-name evidence. It waits on actual native IME invisibility and zero bottom inset after the editor transition, then performs the original single physical Save tap. Final callback asserts one meal and the fixture save callback still asserts exactly one entry. The draft-name check alone would not establish editor closure, but final callback/entry proof remains decisive. There is no pre-add keyboard dismissal, repeated tap or ViewModel-only save replacement.
- Progress retains the successful first cancel/removal/manual `82`, then uses an actual list drag through the production focus-clear handler and a bounded native-IME-hidden/unpanned-root condition before simulating the next picker result. The original second-request pending/displayed assertions remain and now require a second physical cancel, disappearance, pending false and the same manual value. Recorded diagnostic bounds demonstrate the generic component host's asynchronous IME pan; production MainActivity already declares `adjustResize`, so no new production layout/accessibility guarantee is inferred.

No weakened behavior assertion or additional production change was found in these repairs. Conditions are bounded native state/focus/lazy-composition checks rather than arbitrary sleeps. Focused and full connected runs remain pending C; static approval does not close the retained original failures. Root's actual changed-screen visual/semantics observations and any external/device limits must be recorded separately before delivery.

### Keyboard fixture isolation closure

The later aborted full run exposed a shared platform input-mode leak; the initial keyboard-mode fixture did not restore it. The final fixture now explicitly establishes its owned Touch baseline through `Instrumentation.setInTouchMode(true)` and waits for the actual Compose manager to report Touch before recording that baseline. Inside `try`, it still requires successful Keyboard mode admission, successful focus request, actual focused semantics, the original DirectionDown event and zero Home returns after 13 seconds. `finally` restores the established fixture mode through Instrumentation and waits for actual mode restoration even if a keyboard/focus/assertion step fails. No existing ScrollFocus/TapOutsideFocus behavior assertion was edited.

This is scoped test-environment cleanup on the isolated agent device, not a production input-mode change or a claim to restore a user's mode before the fixture's deliberately established baseline. No source defect or weakened assertion was found in the cleanup. C's retained same-process order proof ran keyboard → ScrollFocus → TapOutsideFocus with all **3/3 PASS**, and post-run dumpsys showed Display0 TouchMode1. That proves the previously contaminated follow-on conditions for this bounded order; it does not replace the canonical full-suite rerun. The new full run is ongoing at this review, so full connected acceptance is still pending. All earlier failures/abort/compile-only attempts remain recorded in `verification.md`.

### Completion metric readability follow-up

Independent read-only review of the new `CompletionStats` change and `completionMetricsRemainCompleteAtLargeFontScale` found no blocking source issue. The two original captures distinguish the problem precisely: the large-font actions capture has fully visible metric cards but renders Volume as `100 …`, whereas `Oefeningen` wraps mid-word without cropping. The paused capture does not show the metric row and cannot establish its readability.

The fix is scoped to `CompletionStats`, leaving shared `StatusMetric` behavior intact. Its `BoxWithConstraints` uses exactly the existing actions predicate (`maxWidth < 280.dp`, or font scale at least 1.3 with width below 400.dp). Constrained layouts give each card the full available width in a Column; sufficient-width layouts retain three equal Row weights. The weighted modifier is created inside Row scope and passed unchanged to each sibling card, so parent data applies to the actual Row children. The local composable metrics lambda captures the immutable summary and modifier only; it adds no remembered owner, asynchronous side effect, or changing lambda-based state policy.

The new connected assertion retains the existing action geometry proof and adds independent text-layout proof at font scale 1.5. It scrolls the actual lazy content and requires displayed unmerged text, one text layout, one line, height and horizontal bounds fitting, and no ellipsis for both `Oefeningen` and `100 kg`. In this fixture, `100 kg` uniquely matches the metric: the strongest-set label is `Sterkste set: 100 kg` and the insight chip is `Volume 100 kg`. No former assertion was removed or weakened. This addresses the observed label break/unit loss on the compact configured device; it does not certify arbitrary device widths, maximum numeric volumes, or all font scales. Fresh central gates and a new visual capture are required because production source changed; prior passing snapshots do not close this follow-up. No Gradle/device action or source edit was performed by this reviewer.
