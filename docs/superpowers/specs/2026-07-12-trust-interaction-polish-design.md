# TrainIQ Trust and Interaction Polish Design

## Goal

Improve daily Android responsiveness and public-release trust without a redesign, dependency change, Room migration, or broad architecture rewrite.

## Approved behavior

- Top-level destinations are changed only through the visible bottom navigation or navigation rail. Remove the hidden full-screen horizontal swipe gesture and its haptic side effect.
- Rename deterministic Home guidance from `aiInsight` to `coachInsight`. The Home card is labeled `Coach-inzicht` and `Lokale analyse`; no provider call or AI behavior changes.
- Replace the misleading `fatigueIndex` with `weeklyLoadRatio: Double?`. It compares the latest recorded positive training week with the average of up to three preceding positive training weeks. It is `null` until both sides exist.
- Trend shows `Nog geen vergelijking` when the ratio is unavailable. When available it shows `x.xx×` with copy explaining the comparison. Coach never calls the ratio fatigue or claims it includes RPE.
- Trend uses the existing `reloadableObservation` pattern. A fatal observation error exposes an accessible `Opnieuw proberen` action and retry resubscribes to the source.

## Architecture and error flow

- Keep all changes inside existing domain, repository, ViewModel, and Compose boundaries.
- `ProgressOverview.weeklyLoadRatio` is UI-ready derived domain state; no persistence is added.
- `ProgressViewModel` owns a reload counter and maps `Result<ProgressOverview>?` to `Loading`, `Success`, or `Error`.
- Existing local logging, Health Connect, AI routing, navigation state restoration, and screen layouts remain unchanged.

## Verification

- Add only targeted tests that fail on the old behavior: visible-only top-level navigation, local insight labeling, nullable/real weekly load comparison, and Trend failure/retry wiring.
- Run focused tests after each change, then `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:lintDebug`, and `:app:compileDebugAndroidTestKotlin`.
- Install the debug APK on the existing emulator, tap all six destinations, exercise vertical/diagonal swipes, and confirm the app remains on the selected tab with an empty crash buffer.

## Explicit exclusions

- GitHub Actions restoration, Health Connect single-flight, workout draft debouncing, timer refactoring, new recovery features, adaptive layout migration, and color-system changes belong to later focused batches.
