# TrainIQ end-to-end refinement — local evidence and evaluation plan

## Scope and baseline

- Primary checkout started clean on `main` at `a14e041a09e3db376f211146829fd8066c276283`. A read-only fetch on 2026-09-24 confirmed `origin/main` still points to that commit. Work is on `codex/end-to-end-refinement`.
- Before edits, `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug` passed locally in 3m19s. There was no confirmed reference meal that established whether a reported 1800 kcal scan was wrong.
- Changed surfaces: top-level navigation, active workout card, meal scan normalization/review, OpenAI model policy, routine generation and preview. No Room schema, permissions, remote service, signing, or release change was intended.

## Deterministic acceptance

- A top-level swipe changes only to an adjacent visible destination; taps and swipes use the same typed navigation and saved destination state. Edge gestures, detail routes, IME, and modal flows remain outside the swipe handler.
- Collapsing an exercise hides its set editor while preserving logged sets, active status, drafts, rest controls, and saved collapse state.
- Meal weights explicitly given in g/kg remain fixed; ml is not assumed to equal g. A partial ingredient list retains other predicted items. Unknown nutrition is never silently represented as zero. Nutrition values are normalized from a declared per-portion or per-100-g basis once. The add action blocks a sum above an explicit meal total until the editable amounts or context are corrected. Serving/package and raw/cooked scope that cannot be converted safely receives an explicit review note.
- GPT-6 Luna is the first pinned OpenAI candidate. Meal scan and routine generation send explicit `reasoning.effort=medium`; access failure has a bounded one-model retry. Provider, consent, and Gemini thinking behavior are unchanged.
- Generated routines respect days, available material, exclusions, every requested exercise and muscle priority, existing exercise identity, and a computed time budget. The local fallback can use compatible exercises already in the library; an incompatible preference fails visibly instead of returning an unsuitable routine. Preview shows every exercise and permits targeted replacement/removal before saving.

## Photo accuracy evaluation, pending suitable reference data

No weighed reference photos or reliable label-to-portion cases were present in the repository, and no paid live OpenAI or Gemini request was run for this change. Unit and contract tests establish parsing and scaling behavior; they cannot establish real-world photo accuracy or a quality gain from GPT-6 Luna.

For a future authorized comparison, gather at least 12 consented meals with a measured edible portion and source-backed nutrition (package label or weighed recipe), including mixed dishes, sauces, multiple ingredients, raw/cooked ambiguity, volume-only context, and partial context. Keep photos and health details local and out of Git. For each case:

1. Freeze the exact photo bytes, user context, meal type, schema, and provider settings. Record whether values are for a serving, 100 g, or the whole meal.
2. Evaluate old prompt with `gpt-5.6-luna`, new prompt with `gpt-5.6-luna`, and new prompt with `gpt-6-luna` on the same cases, if the account has access and the user approves a cost cap. Make the first response the primary result; repeat each combination three times only to measure variation, without choosing the lowest calorie answer.
3. Record response success, latency, tokens/cost, explicit and estimated quantities, ingredient identity, kcal and macro absolute/relative error, severe mismatches, and whether the review UI exposed them. Keep model failures separate from nutrition errors.
4. Inspect paired per-case results before claiming improvement. A result with fewer kcal is not inherently better. Any label or weighed-value conflict requires manual review rather than automatic adjustment.

Official capability references checked during implementation: [GPT-6 Luna](https://developers.openai.com/api/docs/models/gpt-6-luna), [reasoning](https://developers.openai.com/api/docs/guides/reasoning), and [Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs). Model-list presence is only an access hint; this task did not verify a live GPT-6 Luna response from the user's account.

The bounded fallback IDs were also checked against the official [GPT-5.6 Luna](https://developers.openai.com/api/docs/models/gpt-5.6-luna) and [GPT-5.4 Mini](https://developers.openai.com/api/docs/models/gpt-5.4-mini) capability pages. Both list image input, Responses, Structured Outputs, and medium reasoning. GPT-6 Luna and GPT-5.6 Luna already default to medium; GPT-5.4 Mini defaults to none, so explicit medium raises reasoning work only when that last fallback handles a meal scan or routine request.

## Local verification record

- From `TrainIQ-Project/`, `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest` passed on the combined navigation, workout, meal, model, and routine changes; the device task ran **172/172 tests** on the isolated API 36 emulator.
- One preceding full run on the same code finished with 171/172: the unchanged `SleepAlarmRegressionTest.unconfirmedRoutineKeepsThreeTenMinuteSuccessors` compared wall-clock times to an exact lower bound and observed 599,959 ms instead of 600,000 ms. A focused rerun passed, then the full 172/172 rerun passed. No sleep implementation or test was changed; the 41 ms emulator-clock discrepancy remains a test stability risk.
- Final visual-only Nutrition layout change was verified with `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest` filtered to four affected Nutrition instrumented classes: **8/8 tests passed**. The full 172-test suite was not rerun after this two-column field layout change.
- The final routine preference/identity contract change passed focused `RoutineGeneratorServiceTest` unit tests, then `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest` filtered to the generated routine preview and state-restoration classes: **6/6 instrumented tests passed**. The full 172-test suite was not repeated after this contract change; the affected routine paths and full unit suite were rerun.
- Visual/interactions: compact 360 × 640 dp at font scale 1.3 in dark mode; active workout collapsed/expanded and numeric IME states, full routine preview including a scrolled exercise, synthetic AI meal review, Start and Voeding screens. The AI review revealed an overcrowded three-field macro row; the final two-column version was captured and inspected. Medium 600 × 960 dp dark navigation rail and a Nutrition → Progress horizontal swipe were checked manually. Compact light mode was inspected during navigation. No clipped critical action was observed in these captures.
- Captures live only in ignored `.codex/device-qa/end-to-end/`; they are not committed. The emulator had a pre-existing `720x1280` size override, `320` dpi override, font scale `1.3`, and night mode `yes`; those exact values were restored after the manual checks. No physical device was touched.
- No Room schema, permission, dependency, signing, or release artifact changed. Room migration and release/performance gates were therefore not run. No real meal-photo accuracy or live model access claim is made.
