# Coach progress, hydration and food providers

Implemented on `codex/coach-hydration-food-providers`, based on `origin/main` at `910afc7` (PR #26). The initial unchanged baseline was `a5f5e33`; its debug build, JVM tests and lint passed before implementation.

## Behavior and data decisions

- Coach opens the existing typed Progress route as **Lichaam & voortgang**, including its existing measurement storage/history/scanner. Settings no longer exposes measurement registration. Compact navigation identifies Progress with Coach; existing adaptive rail routing remains compatible.
- Nutrition has independent manual ml entries with stable UUIDs, correction/deletion and day totals. Earlier entries remain accessible for correction. Manual entries never create food or macros.
- Explicit ml/cl/l metadata or user-confirmed ml is required. Grams are never treated as ml; historical meals receive zero hydration on migration. Each meal item holds its explicit per-serving volume. A grouped Room query produces at most one linked hydration row per meal, using the meal date and serving counts. Transactional replacement/deletion therefore cannot leave a separate stale or duplicated contribution.
- Room 17 → 18 is an additive AutoMigration: manual hydration table/index and a zero-default item volume column. JSON export/import includes both kinds of volume. Existing body measurement tables and records are unchanged.
- Automatic barcode lookup uses Open Food Facts first, then FatSecret on a valid miss or retryable primary failure. Manual selection calls only that provider. Invalid barcodes do not call a provider. Results carry provenance; values from different providers are never merged. No lookup cache was added.
- The server-only FatSecret adapter uses bounded transport, memory-only single-flight OAuth tokens and explicit localization scope. Android receives only normalized food data and uses a non-secret HTTPS gateway URL. See [gateway configuration and official sources](../../TrainIQ-Project/food-gateway/README.md).

## Verification

All commands run locally from `TrainIQ-Project`, with Android Studio JBR, the installed Android SDK and Gradle `--console=plain --max-workers=2`. No clean, dependency installation, hosted runner, permission grant or production request was used.

- Initial baseline: PASS, `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug` on unchanged `a5f5e33`.
- Server: PASS, all 7 tests, `node --test food-gateway/fatsecret.test.mjs food-gateway/server.test.mjs`.
- First complete Android feature pass: PASS, 34 connected tests including 13 migration tests, hydration persistence/manual UI, barcode flow and Coach restoration. `:app:generateDebugRoomMigrationChainVerificationMarker` produced v2-to-v18 with 13 migrations.
- Final combined-base debug build: PASS, `:app:assembleDebug`; JVM suite: PASS, all 908 tests, `:app:testDebugUnitTest`. Lint and Android test compilation: PASS, `:app:lintDebug :app:compileDebugAndroidTestKotlin` (65 lint warnings, no errors). An omitted test-only import in the added drink fixture was corrected before the passing compilation.
- Final connected run: PASS, all 35 tests, including the real Nutrition scan/editor → Room hydration total route. Exact command: `./gradlew.bat :app:generateDebugRoomMigrationChainVerificationMarker -Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.core.database.TrainIqDatabaseMigrationTest,com.trainiq.core.database.HydrationPersistenceTest,com.trainiq.features.nutrition.HydrationUiTest,com.trainiq.features.nutrition.BarcodeMealFlowInstrumentedTest,com.trainiq.features.coach.CoachProfileStateRestorationInstrumentedTest --console=plain --max-workers=2`, with `ANDROID_SERIAL=emulator-5580`.
- Final source scope matches the follow-up verification commit on top of `70f466f`; subsequent edits affect documentation only. Release signing and physical-device performance gates are NOT RUN because this task does not release an artifact or make performance claims.

The isolated existing AVD `TrainIQ_Agent_API36_20260806`, serial `emulator-5580`, runs Android 16/API 36. The task started that emulator without wiping or creating an AVD. Compact checks use 360 × 640 dp, dark theme and font scale 1.3. Actual app traversal confirmed Coach → body measurement save, removal of the Settings entry and manual 250 ml registration with unchanged calories. Semantics, screenshots and generated reports remain untracked under `.codex/device-qa/coach-hydration/` and `app/build/`.

An initial Coach test clicked an offscreen button at large font size; its scroll-before-click correction preserves the original assertion. A transient lint frontend error during concurrent source editing was resolved by a stable-source rerun without disabling any checks.

The final runtime inspection also resized the same isolated emulator to 1000 × 640 dp and switched to light theme. The 250 ml entry survived recreation, remained separate from the 0 kcal food total, and exposed correction/deletion. Progress and Settings were visually inspected at that width. The 35-test pass used the AVD's reset size of 540 × 960 dp; the earlier 34-test pass used 360 × 640 dp. The task restored its starting 720 × 1280 px override (320 dpi), inspected provider selection/attribution at 360 × 640 dp, and reran the new drink flow there: PASS, `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.trainiq.features.nutrition.BarcodeMealFlowInstrumentedTest#scannedDrinkReachesPersistentHydrationTotal`. Dark mode and font scale 1.3 match the starting setup. Compact Coach and Nutrition actions were verified with UI-tree bounds. This is emulator semantics/layout evidence, not a TalkBack or physical-device performance certification.

## Live-access limits and publication

Live FatSecret calls are NOT RUN: no account credentials/entitlements, verified nutrient-retention rights or authorized secure gateway endpoint are configured. Standard FatSecret terms do not by themselves permit indefinite nutrient snapshots; the local server fails closed until account-specific rights are verified. No purchases, secrets changes or deployments were made. The current per-100g app model accepts only explicit gram-based FatSecret servings; ml-only results produce an actionable unsupported-data error instead of a fabricated density.

Production authentication, HTTPS termination and quotas are outside the authorized local adapter. The remaining external setup is to supply an authorized gateway backed by appropriately entitled credentials; its non-secret URL can then be configured in the Android build.

Publication uses `[skip ci]`, matching PR #26 and the repository's local-only testing contract. GitHub checks are inspected on the published SHA, never reported as local test evidence. No merge or release is authorized.
