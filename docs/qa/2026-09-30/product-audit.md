# TrainIQ product and flow audit — 2026-09-30

## Evidence and scope

Wave B1 is a **code-inspected product/UX audit and current primary-source research**, not a completed visual or accessibility certification. Baseline inspected: `d32922e642ca38a348d978716f81723396c6762e`, primary checkout `C:/My-PC-Files/GitHub/TrainIQ`. Android is the only implemented client. This worker changed only this report; no app code, device, permission, network account, remote data, build or Git mutation was performed.

`AGENTS.md`, the target blueprint, ADRs and local-testing guide govern the assessment. Targeted guides consulted: coach/hydration/food providers, food-first add/workout, nutrition scanner/energy, nutrition suite recovery, input/meal/alarm flows, barcode/save/sleep, sleep-alarm reliability, release enum navigation. Current source supersedes historical guide references: e.g. Home currently routes setup to Coach, and body progress belongs to Coach; historical startup/jank and release approval statements are not fresh findings. The September 6 release policy retires the named owner approval gates for itch.io; none are reintroduced here.

Evidence labels:

- **CODE**: concrete current declarations, branches, UI affordances or tests inspected. Proves implementation intent, not rendering or successful execution.
- **DOCUMENTED**: repository guide or official comparator documentation; no claim of observed app operation.
- **RUNTIME NOT YET**: this wave has no fresh screenshots, UI tree, device traversal, TalkBack/Switch Access or measured performance evidence. Root owns runtime and another worker owns Gradle.
- **INFERENCE**: predicted user consequence derived from CODE; marked in each finding. No live service response was assumed.

The product-design audit skill describes screenshot-first flow capture. Its runtime evidence requirement cannot be fulfilled by this code-only worker assignment; this report deliberately distinguishes source findings and proposed checks from a captured-flow audit. No old screenshots or historical test passes are presented as fresh proof.

## Product model and users

One local device owner uses training, nutrition, coaching, measurements and optional sleep reminders. No coach/admin/team account role or cloud account journey appears in the typed client graph. Meaningful user configurations are first-run/skipped setup, local-only/no keys, opted-in AI with ready provider, Health Connect full/partial/denied/unavailable, camera granted/denied/unavailable, and existing local history. These are capability states, not authorization roles.

The app supports a practical local-first loop: set profile → plan routine → start/log/finish workout → review local summary → use nutrition diary and body history → request AI enrichment when enabled. Optional Health Connect adds passive signals. Sleep acknowledgement is a reminder/confirmation routine, **not measured sleep**. Manual hydration and checkbox-controlled meal hydration remain distinct product contracts; this audit proposes no density inference.

## Typed destination and coverage matrix

Every declared destination in `navigation/TrainIqNav.kt:117-157` is inventoried below. Every row is CODE / RUNTIME NOT YET. Existing test classes listed are evidence targets, not fresh PASS results.

| Destination | Entry/user goal and internal surfaces | Loading, empty, error/offline/permission contract inspected | Back/cancel and restoration inspected | Proposed decisive check / existing target |
|---|---|---|---|---|
| `Onboarding` | First-run or Settings reopen; Welcome → Goal/training → Health Connect → AI/privacy → Reminders; skip all and guided tour | Sealed Loading/Success/Error; save/completion failure; optional HC/AI choices | In-content previous step; reducer draft persistence; finish removes onboarding | Fresh install, local-only completion, rejected save retains draft, reopen/back; `OnboardingTourInstrumentedTest`, `OnboardingAiPrivacyDisclosureInstrumentedTest` |
| `Home` | Dashboard, profile/routine setup, next workout, coach insight, health refresh | Local placeholder; missing profile guidance; stale/permission/provider/no data step text; refresh recovery | Top-level saved navigation; next workout uses typed day | No profile and existing profile; partial permission not zero; fresh vs yesterday cache; `FeatureRecoveryInstrumentedTest`, `TrainIqFlowSmokeInstrumentedTest` |
| `Train` | Routines, exercise library, history; create/edit routine/day/set; generate and review routine | Shared observation Loading/Error retry; empty routine/library guidance; bounded generation fallback/retry | Routine detail intercepts Back; saveable selected routine/tab/editor inputs; top nav hidden during detail | Empty → create day/exercise → start; invalid editor; AI disabled/error; cancel generated preview; `PlanDraftRestorationInstrumentedTest`, `GeneratedRoutinePreviewInstrumentedTest`, `ExerciseEditorRecoveryInstrumentedTest` |
| `ActiveWorkout(dayId)` | Resume or resolve old workout conflict; edit/log/remove sets, rest timer, swap exercise, finish/discard | Loading/read error; empty workout; logger validation; conflict dialog; finish failure | Active workout persisted; route Back returns without discard; explicit discard/finish; history child | Rapid log, failed save retry, IME dismissal, rotation/app switch/process reopen, finish twice; `ActiveWorkoutSetActionsInstrumentedTest`, `ActiveWorkoutRestoreInstrumentedTest` |
| `WorkoutProcessing(dayId)` | Legacy finish processing/retry | Processing and error retry; current finish flow can navigate straight to saved completion | Explicit back-to-training callback pops; completion stack logic | Failure retains saved context; Home-started vs Train-started stack. Check reachability before deleting legacy route |
| `WorkoutCompletion(sessionId)` | Saved summary, local/remote source, wins/risks/advice, stats; Home/Training actions | Loading and error remain; retry; local fallback status; success countdown | System Back goes Training; saved 12-second auto-return; touch/scroll cancel; accessible-focus gap below | Success full countdown, error retry, focus/read pause, app switch; `WorkoutCompletionRecoveryInstrumentedTest` |
| `ExerciseHistory(exerciseId)` | Exercise session history, best set/volume chart | Loading/error retry, missing history | Explicit Back to parent; return during active logging | Empty/populated chart semantics, return preserves draft; `ExerciseHistoryInstrumentedTest` |
| `Nutrition` | Today, Products, Recipes, History; internal meal editor/add source chooser; manual hydration | Sealed Loading/Success/Error; empty section actions; scan/import busy; provider lookup pending/not found/error/manual path; submit guard | Saveable tab/editor/drafts; contextual scan destination in SavedStateHandle; modal cancel; focused meal edit Back | First manual meal/product/recipe, offline barcode, cancellation, historical date, duplicate tap; `NutritionFirstAddInstrumentedTest`, `NutritionSubmitDispatchInstrumentedTest`, `HydrationUiTest` |
| `MealDetail(mealType)` | Category detail with individual entries; edit/delete; scanner to same category | Shared nutrition observation; empty category; pending save/delete and confirmation | Explicit route Back; edits/cancel clear focus; scan result/cancel parent state | Each enum category, edit grams/date/servings, confirm/cancel delete, recreation; `MealDetailPersistenceTest`, minified artifact enum smoke |
| `CameraScanner(contextHint,mode,destination,mealType)` | Barcode lookup OR AI meal OR AI smart-scale; camera permission/preview/capture/result/retry/manual | Preview, Processing, Completed, CompletedScale, Empty, NoConfig, LocalFallback, Error; denied/revoked/no camera and bind failure | Back/modal exit share cancellation; scan lifetime guard; completed scale populates parent; saved parent intent | Each mode denial→manual, cancel processing, latest scan wins, rotation/return, no key/offline; `CameraPermissionScannerInstrumentedTest`, `ScannerRecoveryInstrumentedTest`, `AiCameraScannerModesInstrumentedTest`, `ScannerSavedStateHandleInstrumentedTest` |
| `Coach` | Week report, Goals profile/advice; links to Sleep and Body progress | Sealed Loading/Error retry; absent profile points to Goals; generating flags/shimmer; deterministic advice/report fallback | Saveable selected section and profile draft ViewModel ownership; parent routes | No profile → Goals; invalid field; report generation and provider failure; `CoachInsightsInstrumentedTest`, `CoachProfileStateRestorationInstrumentedTest` |
| `Progress` | Body registration, Strength, History; camera/import weight photo, validated save, delete confirmation/charts | Sealed Loading/Error retry; no data guidance; isSaving prevents duplicate; imported analysis lacks busy state | Saveable inputs/tabs/delete; scanner handoff; **no visible Back callback** | Coach→Progress→visible return; delayed import vs edited draft; save failure retains values; `CoachHealthNavigationInstrumentedTest`, `ProgressDeleteConfirmationInstrumentedTest` |
| `SleepPreparation` | Coach/Settings and private alarm activity; pending confirmation first, routine enable/time, system access settings | Sealed Loading/Error retry; busy/error; notification/exact/fullscreen/sound unavailable guidance | Explicit Back; confirmation/next trigger Room authority; lockscreen private activity separate from graph | Native confirmation, no permission fallback, change same time idempotent, app switch/reopen; `SleepObservationRecoveryInstrumentedTest`, `SleepRoutineInstrumentedTest`, `SleepNotificationInstrumentedTest` |
| `Settings` | Overview, onboarding, theme, feedback/reminders, telemetry, providers/keys, HC, local data import/export/reset | Loading/error wrapper; provider status; partial access; import preview/pending/error; destructive confirmation | Saveable confirmation/technical disclosure; secret input deliberately volatile and cleared on dispose | Key field masks; settings actions keyboard traversal; denied/revoked HC refresh; cancel import/delete, scope clarity; `OpenAiSettingsRouteInstrumentedTest`, HC rationale/provider tests |

Graph framing: compact bottom navigation is Home/Train/Nutrition/Coach/Settings; Progress remains in the top-level destination list and expanded rail/guided tour, but is filtered from compact navigation. Coach pushes Progress; compact selection currently maps Progress to Settings/Meer. Medium/expanded layouts use shared viewport max-width and rail policies. Global top-level swipe is conditionally disabled for IME/detail/tour. Guided tour step/index is saveable. Source presence is not proof of current compact labels, complete swipe arbitration, tablet parity or touch bounds; root should capture the actual smallest profile.

## Actionable findings

### B1-01 — P2: Body progress has no visible return action

**CODE:** `TrainIqNav.kt:819-840` constructs `ProgressRoute` without `onBack`; Coach pushes it at `:845`. `ProgressRoute` and `ProgressScreen` signatures have no Back callback. Header at `ProgressScreen.kt:446` shows only title/subtitle. Progress is declared a top-level item; compact navigation remains but highlights Settings/Meer through compactSelectedNavigationRouteClass (:569-570). Expanded rail includes Progress. This is a parent-ownership inconsistency, not an absence of all navigation.

**INFERENCE:** the owner can use Android system Back or top-level navigation but lacks a visible touch/keyboard/assistive return to the originating Coach context. On compact width the selected Meer indicator also contradicts the documented Coach ownership. Other nested flows such as Sleep, MealDetail and ExerciseHistory have explicit Back.

**Small change:** thread a required `onBack` through Nav → ProgressRoute → ProgressScreen; add standard labeled 48dp action near the title. Preserve inputs and system Back behavior. Map compact Progress selection to Coach to match ownership; do not add another bottom tab.

**Acceptance / proof:** explicit return goes to prior Coach state; scanner returns to Progress first; Back from loading/error/success works; keyboard Enter and meaningful semantic label; compact 360×640/1.3 and dark/1.5 actions visible. Extend existing real `CoachHealthNavigationInstrumentedTest`; check `TrainIqFlowSmokeInstrumentedTest` for no stack regressions.

**Risk:** low, reversible navigation/Compose change. Candidate files: `features/progress/ProgressScreen.kt`, `navigation/TrainIqNav.kt`, `app/src/androidTest/java/com/trainiq/features/coach/CoachHealthNavigationInstrumentedTest.kt` (under TrainIQ-Project app source roots).

### B1-02 — P2: Timed completion exit cannot be paused reliably by non-touch readers

**CODE:** `WorkoutScreen.kt:5016-5034` starts a 12-second auto-return for Success. Only pointer presses and actual list scrolling cancel it (`:5029-5033`, `:5046-5054`). CompletionActions is the last lazy item after summary/stats/exercises. The existing test `countdownStartsOnlyAfterSummaryIsLoaded` explicitly expects navigation; `touchingSummaryCancelsAutomaticReturn` only proves touch. `docs/qa/TrainIQ_Twenty_New_Findings_2026-09-06.md` TWENTY-15 preserves the successful countdown policy.

**INFERENCE:** keyboard focus or TalkBack reading at rest does not necessarily cause pointer/scroll events and may lose the summary before the user understands it. The coroutine is composition-scoped, so background pause also needs runtime verification.

**Small change:** preserve intentional auto-return, expose an early semantic “Op scherm blijven” action in success header and make non-touch interaction pause it. Use existing Compose/accessibility primitives and lifecycle ownership rather than a custom global gesture system. Consider accessibility-manager recommended timeouts only if evidence shows that still satisfies current policy; no blanket removal proposed.

**Acceptance / proof:** unchanged no-interaction success countdown and loading/error retention; keyboard action pauses without scrolling; accessible action pauses and remains after recreation; returning from background does not silently skip a read opportunity; explicit Home/Training and system Back still clean the stack. Extend `WorkoutCompletionRecoveryInstrumentedTest` and root's active TalkBack/keyboard smoke.

**Risk:** medium lifecycle/accessibility behavior, no persisted data change. Candidate files: `features/workout/WorkoutScreen.kt`, matching `WorkoutCompletionRecoveryInstrumentedTest.kt`. Root should decide exact interaction ownership once.

### B1-03 — P2: Imported scale analysis can replace a newer manual draft

**CODE:** `ProgressUiState.Success` has save state but no analysis state (`ProgressScreen.kt:116-124`). `analyzeScalePhoto` launches `LatestScanRequest` and publishes callback (`:248-266`). Screen callback (`:372-383`) unconditionally writes all three draft fields. The manual fields, save and import buttons remain usable during analysis. LatestScanRequest rejects earlier scans, not user edits to the current draft.

**INFERENCE:** select a photo → edit weight while slow analysis runs → returned analysis overwrites the newer manual values. A save before the response also permits later fill of an apparently new form. No fresh delayed-result UI reproduction is claimed.

**Small change:** represent pending analysis in ViewModel state, show meaningful busy/cancel status, and publish only if the draft revision still matches the request. Preserve newer edits with an explanatory message. Disabling every field is a less useful alternative; do not automatically save recognized values.

**Acceptance / proof:** delayed success fills an unchanged draft once; manual edits/save after request cannot be overwritten; replacement scan wins; cancellation/failure clears busy and preserves values; import temporary file cleanup stays intact. Deferred-response ViewModel/component test first, then focused Progress Compose recreation/edit/import test. Compile, affected JVM, lint and targeted instrumentation.

**Risk:** medium async UI state/draft ownership; no Room format change. Candidate files: `features/progress/ProgressScreen.kt`, existing progress unit tests or focused new analysis-state test, narrowly named progress instrumentation. Shares file ownership with B1-01 and must be implemented sequentially.

### B1-04 — P3: First-run capability copy uses implementation terminology

**CODE:** `OnboardingStep.HEALTH_CONNECT.subtitle` says “Health Connect-first” and “live ververst zodra Home opent” (`OnboardingScreen.kt:79`); AI privacy step says “JSON-contracten en lokale fallback” (`:594`). These describe architecture, not decisions a user needs to make. Existing AI disclosure correctly explains optional providers and delayed background workout processing; preserve that disclosure.

**Small change:** use concise Dutch value and fallback copy, e.g. “Koppel je stappen, slaap en beweging wanneer jij dat wilt” and “Je kunt handmatig blijven loggen als AI uitstaat of tijdelijk niet beschikbaar is.” Do not promise real-time availability or alter consent flags.

**Acceptance / proof:** privacy/provider/background disclosure retained; no change to optional completion; no new technical setup terminology; existing `OnboardingAiPrivacyDisclosureInstrumentedTest` still proves material disclosure; focused compact screenshot. Copy-only regression does not need another implementation-mirroring unit test.

**Risk:** low. Candidate files: `features/onboarding/OnboardingScreen.kt`, adjust existing disclosure expectation only if its exact text changes while retaining semantic claim.

## External comparison research, accessed 2026-09-30

No comparator app was installed, purchased, operated or observed. The following are **DOCUMENTED vendor capabilities**, not proof of actual UX quality, feature equivalence, adoption or recommendations to spend money. Research stops after three representative apps; broad cloning would conflict with TrainIQ's local-first invisible-coach target.

| Comparator / primary source | Documented pattern | Useful bounded implication for TrainIQ |
|---|---|---|
| [Hevy feature list](https://www.hevyapp.com/features/) and [workout tracking guide](https://www.hevyapp.com/features/track-workouts/) | Routines, history, automatic rest timers and logging tools | TrainIQ already has these basic execution surfaces. Prioritize reliable logger/history/return continuity; social feed, platform breadth and plate calculator are not evidence-backed priorities |
| [Cronometer photo logging help](https://support.cronometer.com/hc/en-us/articles/39013533811092-Mobile-Photo-Logging) and [official photo logging explainer](https://cronometer.com/blog/photo-logging/) | Photo detects candidate foods for diary or recipes; official guidance distinguishes branded-food barcode use from photo lookup | TrainIQ's editable pre-save rows and explicit scan intent are appropriate. Keep pending analysis clear and edits authoritative; do not market photo estimates as verified measurements |
| [Oura readiness contributors](https://support.ouraring.com/hc/en-us/articles/360057791533-Readiness-Contributors) | Readiness uses individual contributors and longer-term context, not a lone raw activity count | Validate TrainIQ's data-quality/stale/missing labels with partial inputs. Do not import Oura thresholds or imply clinical validity without TrainIQ data/proof |

The search also returned third-party/reddit material; none supports accepted findings. Vendor content supports bounded comparison only. Findings B1-01 through B1-04 are grounded in repository code, independent of comparator marketing.

## Existing server and external boundaries

`TrainIQ-Project/food-gateway/` is an existing Node local adapter (`server.mjs`, `fatsecret.mjs`, synthetic tests), documented in its README. It binds loopback `127.0.0.1:8787`; Android's HTTPS gateway URL defaults empty. OAuth tokens/credentials are server-only; barcode is the request scope. Durable nutrient rights are an explicit fail-closed gate; there is no authorized secure production deployment/auth/quota boundary in this audit. Source existence does not mean a live backend is configured or tested. Manual entry and existing Open Food Facts lookup remain important recovery paths. No purchase, credentials, entitlements, service deployment or dependency change is proposed.

## Rejected opportunities and uncertainty

| Opportunity | Decision / reason |
|---|---|
| Remove completion auto-return | Rejected: explicit repository regression preserves 12-second Success policy; fix accessible pause instead |
| Add sixth Progress tab or move body history back to Settings | Rejected: compact-nav and September routing contract already chose Coach ownership |
| Replace all colors/components with visual redesign | Rejected: no fresh screenshot evidence; current Material/theme primitives exist. Inspect actual contrast/reachability before changes |
| Add social feed, cloud accounts, smartwatch/iOS clients | Rejected: comparator features do not authorize new remote/platform scope |
| Add readiness check-in, posture scanner or medical recovery score | Deferred: blueprint marks check-ins as decision-needed; no validated product requirement/data boundary in current graph |
| Expand micronutrient/database/gateway providers | Rejected: live license/auth/quota authority and useful outcome not established; preserve existing fail-closed and manual fallback |
| Treat imported missing body metrics as zero or make optional stored metrics nullable now | Deferred: current model requires all three values; schema/domain/import contracts must be inspected by data worker before a persisted change. Audit flags friction but does not prescribe migration |
| Automatically rewrite scan grams into fluid density | Rejected: violates explicit checkbox-controlled one-to-one product contract |
| Claim alarm audio reliability or startup performance from old emulator evidence | Rejected: fresh runtime and physical hardware evidence are distinct; this wave has neither |
| Introduce full screenshot Cartesian matrix | Rejected: choose unique smallest/large-font/dark scenarios; semantics and lifecycle tests carry behavior |

## Root verification handoff

Suggested bounded runtime sequence (synthetic local data, agent-owned approved emulator):

1. Fresh optional onboarding → skip HC/AI → Home → guided tour → revisit setup. Capture disclosure and actual next action; avoid entering real secrets.
2. Coach → body progress → verify B1-01 absence and explicit return after fix; Weight input/validation → failed save retained; scan denial/manual → return; history delete cancel.
3. Nutrition Today → manual product/recipe/meal → historical/category detail → edit grams/servings → cancel → barcode no camera/not found/manual → return same category; hydration checkbox/manual ml remain independent.
4. Training empty/create/day/exercise → active workout → edit/log/timer/history → back/resume → finish → saved completion; touch, keyboard and screen-reader non-scroll reading around 12 seconds; explicit return stack.
5. Settings/theme/provider/no key/HC partial and unsupported fixtures → export/import preview/delete cancellation; Sleep permission/status and confirmation. Native alarm delivery requires a separate safe-profile plan, not speculative setting changes.
6. Small compact 360×640 / 1.3 first, dark / 1.5 for unique reachability; expanded width for rail/content parity; IME/rotation/app switch/recreation when fields are at risk. Inspect UI tree/semantics alongside accepted screenshots. TalkBack/Switch Access assertions must say exactly what was traversed/heard.

For accepted changes, lowest-layer proofs precede compilation/lint and targeted `:app:connectedDebugAndroidTest`. Root/C own fresh baseline and device gates; this document does not repeat them. R8 typed-enum navigation needs the exact minified artifact only if release-like verification is in authorized scope; debug navigation does not prove it. No migration marker, signing or physical macrobenchmark is needed for a doc-only audit.

**Current check status:** CODE inspection and comparator primary research completed; `git diff --check` and path review are the only worker verification. Build/unit/lint/connected/runtime for this report: NOT RUN (owned by root/C; documentation-only worker). No finding is recorded as implemented or runtime-fixed here. Root should mark accepted/rejected/verified disposition in the integrated ledger after implementation and current-run evidence.
