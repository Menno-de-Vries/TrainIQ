# A3 independent review — FIX-03 / FIX-04 / FIX-06 / FIX-08

Read-only review of current integrated source on `codex/full-audit-2026-09-30`, base `d32922e`. A3 changed this report only. No Gradle, device, credentials or live services. Passing runtime/build results remain the central runner's responsibility.

## Result

No confirmed blocking implementation defect found in these four packages. Static review supports advancing to integrated verification. The targeted coverage recommendation below was addressed by root during review; it was not a demonstrated behavior failure.

## FIX-03: caller deadline preservation

- `AiSupport.kt:282-283` and `:349-350` check the current coroutine's activity before converting `TimeoutCancellationException`. A canceled parent throws its cancellation cause before throttle/metadata mapping; an owned inner timeout returns to an active surrounding context and retains the existing typed provider-timeout behavior.
- New `AiSupportTest` covers Gemini outer deadline and OpenAI outer deadline after a rate-limit response, distinguishing cancellation from ordinary failure/fallback and proving no cooldown is recorded. Existing `AiServicesTest:267-301` retains owned Gemini timeout/generic cancellation tests; existing `AiSupportTest:143-166` retains owned OpenAI timeout and successful subsequent request.
- Test fixtures use controlled virtual time and no network. The fallback counter checks the consumer exception category; it does not directly test every router/feature consumer. Router still explicitly rethrows `CancellationException`, consistent with this helper fix.
- [Kotlin ensureActive documentation](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/ensure-active.html), checked 2026-09-30, confirms canceled-context cause propagation. Current vendor documentation describes a newer library; repository pin remains coroutines 1.9.0 and was not changed.

## FIX-04: durable key deletion

- `DurableKeyRemoval.kt:4-5` checks the Boolean result once and emits a safe error. Both Android stores call it around synchronous removal/commit. It does not log, decrypt, expose or rotate keys.
- `GeminiKeyMigration.clearEncryptedKey`, `OpenAiKeyStore.clearEncryptedKey` and `AiUsageGate` propagate the failure. `SettingsSection.kt:353-371` performs success updates/messages after the clear call. `launchAction` at `:551-552` uses `launchUserAction` (`core/ui/UserAction.kt:20-33`), which catches ordinary storage exceptions and emits the existing safe Settings failure message, while cancellation propagates. Therefore false commit does not reach a success message and does not create an uncaught ViewModel coroutine failure.
- `clearAllData` at `SettingsSection.kt:493-508` separately uses `runUserActionCatching` and emits a safe failure. This multi-store deletion is not atomic: earlier local data or the first provider key may already have been removed when a later key store fails. Current reporting must preserve that limitation rather than claim rollback.
- New tests prove true/false/throwing commits and both provider facade propagation. They use a synthetic commit, not an actual Android disk failure. Static tracing proves current Settings ordering, but the tests do not instantiate the actual Settings ViewModel/AiUsageGate clear operation. This is a coverage limit, not a presently observed code failure.

## FIX-06: completion reading and lifecycle

- `WorkoutScreen.kt:5022-5050` honors the platform recommendation, disables timed navigation for indefinite timeout, and restarts fresh reading time at RESUMED through `repeatOnLifecycle`. Loading/error never start the timer. Explicit stay uses a semantic button near the header; preview key events pause before normal child handling, and touch/scroll pause behavior is retained.
- New production Compose tests use semantic click, real key dispatch, a synthetic accessibility manager and a controlled LifecycleRegistry; existing tests retain ordinary 12s navigation, loading/error/retry and large-font action bounds. No screenshots substitute for those behavior tests.
- Library pins remain Compose BOM 2024.12.01 and lifecycle 2.8.7. No unsupported API/dependency change was introduced. Official Android reference pages for AccessibilityManager/repeatOnLifecycle exceeded this browser tool's content limit; no successful fetch or additional vendor claim is asserted. Central compile/runtime verification must establish pinned compatibility.
- Residual limits: human assistive-technology behavior is not certified, and layout reachability on the requested runtime matrix remains root-owned.

### REVIEW-A-01 — Finite accessibility extension lacks a dedicated regression case

- priority: P3; category: tests; status: addressed, central execution pending
- evidence: `WorkoutScreen.kt:5040-5043` derives/rounds a recommended reading interval; the new manager fixture exercises only `Long.MAX_VALUE`, while existing ordinary countdown tests exercise 12s.
- proposed smallest proof: provide an AccessibilityManager returning 30,500 ms, establish foreground success, assert no navigation after 12s/30s and one navigation after the rounded 31s; no sleeps or real accessibility service necessary. Keep no-interaction default tests unchanged.
- significance: covers the newly added finite recommendation branch and rounding contract. No evidence currently shows that branch fails, so do not label this as a user-visible defect or release blocker.
- root response: added a 30,500-ms recommendation fixture asserting no return at 30s and return by 32s. Static inspection confirms the new branch coverage; execution remains the central runner's responsibility.

## FIX-08: onboarding copy

- Health Connect text describes optional linking without claiming continuous live updates. AI copy retains off-by-default, user-requested actions, provider-key prerequisite and deferred workout debrief disclosure, then explains manual logging/local insights plainly.
- No permission, provider routing, consent persistence or billing behavior changed. Existing onboarding completion/disclosure tests remain relevant. Compact visual readability is central-runner/root evidence, not this static review.

## Verification and handoff

- Scoped read/diff review and `git diff --check`: PASS.
- Build/unit/lint/connected/runtime: NOT RUN by A3; pending central integration proof.
- Required fixes from this review: none confirmed.
- Finite-timeout test recommendation: root addressed in its completion test file; A3 did not edit it.
- Changed path: `docs/qa/2026-09-30/review-A.md` only.
