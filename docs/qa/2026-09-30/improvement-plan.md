# Integrated audit and improvement plan — 2026-09-30

Base: `d32922e642ca38a348d978716f81723396c6762e`; clean primary checkout, branch `codex/full-audit-2026-09-30`. User authorizes research, local implementation/tests and local commits, explicitly three workers `gpt-6.1-sol` / `low`; no nested delegation or remote delivery. Root owns integration and device. Reports: [technical audit](technical-audit.md), [product/flow matrix and comparator research](product-audit.md), [baseline/test coverage](baseline-tests.md). Static research is accepted; runtime claims require new evidence.

## Selected package and acceptance

| ID / priority | Evidence and problem | Desired behavior / benefit | Scope and dependencies | Acceptance and cheapest regression proof | Risk |
|---|---|---|---|---|---|
| FIX-01 / P2 | TECH-01: parallel HC callers read-sync-save out of order; clear can be repopulated | Serialize full sync and coordinate private-preference reset; cancellation releases ownership | HC datasource, shared preferences coordination; existing formats unchanged | Controlled concurrency proves ordering, canceled waiters and failure recovery; reset cannot retain old sync writes; existing partial/token contracts; safe provider status smoke | Medium async/platform |
| FIX-02 / P2 | TECH-02: positive meal/recipe edit IDs can recreate deleted parents | Reject missing update target transactionally with actionable error; valid create/edit unchanged | Runtime store and actual Room contracts; no schema migration | Both meal overloads and recipe stale save fail without parent/child writes; valid edits/date preservation; DB reopen | Low persistence |
| FIX-03 / P2 | TECH-03: AI wrappers map outer deadline to ordinary failure | Caller cancellation propagates; owned provider timeout still safely maps | AI retry helpers and deterministic JVM tests | Both outer deadline tests, no fallback/throttle; owned timeout/rate-limit/success remain passing | Low cancellation |
| FIX-04 / P2 | TECH-04: failed key-clear durable commit ignored | Failed deletion reports storage failure; success only after durable commit | Both encrypted stores, narrow durable helper and Settings/AI gate tests | False commit and exception fail; true succeeds; provider-key clear failure does not produce success | Low security/storage |
| FIX-05 / P2 | B1-01: body progress lacks parent-return; compact selection implies Settings ownership | Visible labeled return and compact Coach selection, preserving route state | Progress + typed navigation; owner shared with FIX-07 | Return in Loading/Error/Success and real Coach path; scanner returns Progress; updated nav policy; compact semantics | Low navigation |
| FIX-06 / P2 | B1-02: intentional 12s completion exits during non-touch reading/background | Preserve ordinary auto-return; early explicit stay action, keyboard/accessibility and lifecycle pause | Workout completion only; existing countdown tests preserved | No-interaction countdown; stay by semantic/keyboard action; background/accessibility pauses; loading/error retain; explicit exits work | Medium lifecycle/UI |
| FIX-07 / P2 | B1-03: delayed scale-photo result replaces newer manual draft | Pending/cancel feedback and revision-aware delivery; new edits/save remain authoritative | Progress ViewModel/screen, same owner as FIX-05, latest-image cleanup preserved | Deferred success/changed draft/cancel/failure/new request tests; targeted Compose edit/recreation proof | Medium async input |
| FIX-08 / P3 | B1-04: architecture jargon and live-refresh promise in onboarding | Plain Dutch capability/value and offline fallback copy; material consent remains | Onboarding copy, existing disclosure regression | Optional HC/AI completion and delayed-debrief disclosure intact; compact runtime readability | Low copy |

No change to dependencies, provider models, schema, permissions, remote configuration, signing or distribution. Existence guards in FIX-02 do not claim protection against recycled row IDs; broader identity redesign is deferred pending separate evidence and migration design.

### Integration correction before acceptance

Independent B3 traced the complete UI command chain and found that new meals reserve a positive timestamp ID for retry idempotence. The initial FIX-02 assumption that positive ID always means edit was wrong: it would reject first saves. A4 must explicitly distinguish create intent from edit intent while preserving that stable key and transaction guard. Existing first-add/submit/meal integration tests are required alongside stale-editor tests. Safe deleted-target errors must also reach the UI as actionable recreate guidance, rather than generic retry; never expose arbitrary provider/storage exception messages. This is correction of a new regression, not an accepted baseline defect.

B4 corrects the new Progress test's `StateRestorationTester` import to the existing junit4 API. The first integrated gate's failed Android-test compilation is not a passing verification. C3 reruns only after both owning repairs and a frozen source handoff. Expanded A4 file ownership is the directly affected nutrition create/edit command chain and matching tests; B4 owns its Progress test only. No schema/provider/dependency expansion.

## Central queue / exclusive ownership

Every worker keeps the user-selected model/effort (`gpt-6.1-sol`, `low`). Handoffs include changed files, proof, uncertainty and blockers. No concurrent Gradle or device operations.

| Task / wave | Owner, context, start condition | Exclusive write area | Result / acceptance |
|---|---|---|---|
| A1 / 1 | A; contract, broad data/AI/privacy guides; clean baseline | technical-audit.md | Accepted static inventory and four concrete findings |
| B1 / 1 | B; contract/product guides/primary competitors; clean baseline | product-audit.md | Accepted all 14 routes, state/flow matrix, four findings and bounded comparison |
| C1 / 1 | C; local testing/build inventory; clean baseline | baseline-tests.md; ignored baseline evidence | Local JVM/build/lint/compile + Node baseline; root captures fresh runtime; no source edits until baseline inputs frozen |
| A2 / 2 | A; FIX-01/02 and accepted A1; baseline gate finishes | HC datasource + narrow coordinator, UserPreferencesRepository, runtime store; directly corresponding new/focused JVM/Room tests; implementation-A.md | Both packages reviewed, deterministically proved and integrated; no nested delegation/Gradle without root dispatch |
| B2 / 2 | B; FIX-05/07 and accepted B1; baseline gate finishes | ProgressScreen.kt, TrainIqNav.kt, directly corresponding Progress/navigation/Coach tests; implementation-B.md | Draft preservation and navigation complete, compact semantics evidence required |
| C2 / 2 | C; FIX-03 and baseline; C1 finishes | AiSupport.kt, directly corresponding AI retry tests; implementation-C.md | Outer cancellation regressions and existing retries pass; C remains central Gradle owner |
| R2 / 2 | Root; FIX-04/06/08; baseline gate finishes | encrypted stores + new durable helper/tests, WorkoutScreen.kt/completion tests, OnboardingScreen.kt, integration/report/state docs | Storage failure, accessible countdown, plain copy; no shared agent-owned source writes |
| A3/B3 / 3 | Reuse A/B; after integrated changes | review-A.md / review-B.md only | Independent cross-package review; defects repaired by owning stream |
| C3 / 3 | Reuse C; integrated snapshot after review | verification.md; ignored generated evidence | Full debug/unit/lint and canonical full connected gate, Room marker, traceable results and baseline comparison |

## Verification and remaining scope

Before implementation freeze baseline build/unit/lint/Android-test compile, synthetic gateway tests and root fresh UI-tree/screenshots on one installed agent AVD. Final full connected suite covers existing flow roles/configurations and changed paths; it does not mean every provider/hardware combination was exercised. Root visually inspects compact 360×640 dp, font 1.3/1.5 and dark/light plus expanded geometry for changed screens. Capture crash/lifecycle evidence and use meaningful Compose semantics; never infer manual TalkBack/Switch Access certification from screenshots.

Run `:app:generateDebugRoomMigrationChainVerificationMarker` after connected chain proof. Profileable/benchmark packaging is useful broad audit verification, but no physical-device performance claim or release/signing operation is authorized/needed. No live AI request, account mutation, paid provider, credential use, physical user-device reconfiguration, HC permission mutation or production data action.

Rejected/deferred opportunities and authoritative comparator links are in product-audit.md. Live HC/Samsung provider mutation, real camera/audio fidelity, live AI access/accuracy, physical benchmarks and human assistive-technology coverage must be reported as NOT RUN when unavailable. These do not stop independent implementation; final delivery must not claim the original exhaustive runtime request fully certified if those necessary inputs are absent.

## Full-suite integration evidence expansion

The first full Android run found four failures at compact font 1.3. Directly affected test owners investigated exact traces and preserved the original assertions. Root expanded ownership to the existing exercise-history regression: lazy stats/rank must be scrolled into composition before waits. Completion fixture now establishes real Keyboard input mode and verifies focus before its key dispatch. B keeps Progress retry/cancel assertions and clears component-host native IME pan through actual drag and settled-inset waiting. A keeps single physical meal taps and exactly-once assertions, but waits for the native IME transition after adding before targeting save. No timeout inflation, extra taps, skipped tests, reference updates or production changes were used to make these four tests pass. Original failures and all changed-hypothesis attempts remain in the verification ledger. Final full execution is required; focused passes alone are insufficient.

### FIX-06 visual acceptance follow-up

Root and independent B inspected the generated font1.5 action screenshot: Volume actually displayed `100 …` inside a fully visible narrow card; Oefeningen wrapped mid-word. CompletionStats now uses the existing completion-action compact/large-font breakpoint to stack full-width cards, retaining three columns at sufficient width. Shared StatusMetric is unchanged. A new GetTextLayoutResult regression proves complete metric text without width/height overflow or ellipsis; this production change invalidates the prior build/JVM/lint snapshot, requiring a fresh integrated gate. No new feature, schema or dependency scope.

### Final verification and visual follow-up queue

All reused workers retain explicitly configured gpt-6.1-sol / low, no nested delegation.

| Task | Owner / start condition / dependency | Exclusive area | Result and acceptance |
|---|---|---|---|
| A-final-pointer-diagnostic | A; cold full200/201 failure, gates stopped | BarcodeMealFlowInstrumentedTest only | Primitive before-tap snapshot plus failure-only native coordinates/tree/PNG, unchanged single physical taps; no production conclusion before failure frame |
| C-barcode-class | C; A source-stable handoff | Gradle/device/verification.md/ignored evidence | Whole class16/16pass; no failure frame, full-context defect still open |
| B-final-metrics-review | B; existing font1.5 screenshot and source, no device | review-B.md only | Confirmed actual unit ellipsis; smallest existing-breakpoint fix accepted in source |
| R-final-metrics | Root; C released device/source | CompletionStats and its existing Android class, root reports | Stacked constrained metrics and no-ellipsis rendered-text proof; no global metric change |
| C-final-layout-and-full | C; root stable source and B review | Gradle/device/verification.md/ignored evidence | Fresh992/debug/lint/compile pass; repair new fixture selector/index without weakening assertions; completion class must pass before canonical full202 |
| R-final-runtime-and-commits | Root; full gate acceptance and released device | Manual device inspection, scoped exact-path commits, final docs | Small/expanded dark/light changed flows, explicit limitations, reviewable local commits |
| C-final-provenance-packages | C; root clean committed HEAD | Gradle/ignored final sidecar only | Final debug/profileable/benchmark packages and Settings provenance test with exact clean Git SHA; no remote/signing/physical performance claim |
