# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

All commands run from `TrainIQ-Project/`:

```bash
./gradlew build                  # Full build
./gradlew assembleDebug          # Debug APK
./gradlew installDebug           # Build and install on connected device/emulator
./gradlew test                   # Run unit tests
./gradlew testDebugUnitTest      # Run debug unit tests only
./gradlew clean                  # Clean build outputs
```

On Windows use `gradlew.bat` instead of `./gradlew`.

Unit tests for business logic go in `app/src/test/`. Use the **Turbine** library for testing StateFlow/Flow emissions.

## Architecture

**Core Loop:** Health Connect (data ingestion) → Gemini 2.5 Flash (reasoning) → Material 3 Compose UI (action)

The project uses **Clean Architecture / MVVM** with three main layers:

- **`data/`** — Infrastructure: `TrainIqRepository` (single class implementing all domain repository interfaces), `TrainIqLocalStore` (JSON file storage at `files/trainiq-state.json`), `HealthConnectDataSource`, Retrofit-based `GeminiApi`
- **`domain/`** — Business logic: repository interfaces, domain models (`DomainModels.kt`), 27+ use cases (thin wrappers over repository methods)
- **`features/`** — UI: one package per screen, each with a ViewModel exposing `uiState: StateFlow<T>` using sealed interfaces (`Loading`, `Success`, `Error`)

**Supporting packages:**
- `ai/services/` — Four Gemini services: `MealAnalysisService`, `WorkoutDebriefService`, `GoalAdvisorService`, `WeeklyReportService`
- `ai/prompts/GeminiPrompts.kt` — All system instructions; keep "Senior Strength Coach" persona
- `core/database/` — Room schema (defined at version 3, not yet active at runtime — JSON store is used instead)
- `core/di/` — Hilt modules (`AppModule`, `RepositoryModule`)
- `navigation/TrainIqNav.kt` — Type-safe routes via `kotlinx.serialization`

## Key Standards

**Before adding any field:** Read `Entities.kt` and `DomainModels.kt` first to understand existing structure.

**AI calls (Gemini 2.5 Flash):**
- Always use `response_mime_type: "application/json"` — never parse JSON from free-form text
- Meal scanning: disable thinking (speed priority)
- Goal advice & weekly reports: enable `ThinkingConfig` with 500–1000 token budget
- All services must have deterministic fallbacks when the API key is absent or the call fails
- `AiUsageGate` must be checked before making any AI call

**State management:** Every screen has a single `uiState: StateFlow<T>`. Use `AnimatedContent` for state transitions and haptic feedback on critical actions.

**Navigation:** Type-safe only — `@Serializable` data objects/classes for routes, no string-based routes.

**DI:** `@Singleton` for repositories, `@ViewModelScoped` for ViewModels.

**Room migrations:** Use `AutoMigration` where possible; manual SQL otherwise. Current DB version: 3.

**Prompt requests:** When the word "prompt" appears in a request, generate a Codex-style prompt based on the question rather than executing the actions directly.

---

## Android Best Practices

### Kotlin

- Prefer `data class` for models, `sealed interface` for UI state and results.
- Use `object` for singletons and companion objects only for factory methods or constants.
- Prefer `val` over `var`; immutability is the default.
- Use `copy()` on data classes instead of mutating state.
- Prefer `when` expressions (not statements) for exhaustive branching on sealed types.
- Use named arguments on function calls with 3+ parameters for readability.
- Avoid nullable types (`?`) unless null is a meaningful value; use `requireNotNull` / `checkNotNull` to assert invariants.
- Use `inline` functions for higher-order functions called in hot paths (e.g., Compose lambdas).
- Extension functions go in the same file as the class they extend if project-specific, or in a dedicated `util/` file if reusable.

### Coroutines & Flow

- Launch coroutines from `viewModelScope` in ViewModels; never from the repository layer.
- Repositories expose `Flow<T>`, never `suspend fun` that returns a snapshot when a stream is more appropriate.
- Use `stateIn(scope, SharingStarted.WhileSubscribed(5_000), initialValue)` to convert cold flows to hot StateFlows in ViewModels — the 5 s timeout survives config changes without leaking.
- Prefer `callbackFlow` to bridge callback-based APIs into Flow.
- Use `flowOn(Dispatchers.IO)` in the data layer; never hardcode dispatchers in ViewModels.
- Catch exceptions at the ViewModel boundary; never silently swallow them in repositories.

### Jetpack Compose

- Each composable should do one thing. Extract sub-composables when a function exceeds ~60 lines.
- Pass state down, events up (UDF). Composables never read from a ViewModel directly — only the screen-level composable does.
- Use `remember` and `derivedStateOf` to avoid unnecessary recompositions; profile with Layout Inspector.
- Prefer `LazyColumn`/`LazyRow` over `Column`/`Row` inside `ScrollState` for lists of unknown length.
- Use `Modifier` as the last non-lambda parameter and always pass it through to the root layout node.
- Avoid side effects inside composable bodies; use `LaunchedEffect`, `SideEffect`, or `DisposableEffect` appropriately.
- Use `stringResource`, `dimensionResource`, and `colorResource` — never hardcode strings or pixel values.
- Animate with `AnimatedContent`, `AnimatedVisibility`, or `animate*AsState`; avoid `Handler`/`postDelayed` for UI timing.
- Use `@Preview` with a `@PreviewParameter` for realistic data; keep previews in the same file as the composable.

### Material 3

- Always use `MaterialTheme.colorScheme.*` tokens — never hardcode colors.
- Support dynamic color (`dynamicLightColorScheme` / `dynamicDarkColorScheme`) on Android 12+; fall back to the static brand palette on older versions.
- Use `MaterialTheme.typography.*` tokens for all text styles.
- Use `Surface` as the root of screens to automatically apply background color and content color.
- Icons: prefer `Icons.Rounded.*` for a consistent visual weight throughout the app.

### ViewModel & State

- One ViewModel per screen (not per composable).
- Expose exactly one `uiState: StateFlow<ScreenState>` where `ScreenState` is a sealed interface.
- Side effects (navigation, snackbars, toasts) are emitted through a separate `SharedFlow<UiEvent>` — never embedded in `uiState`.
- Do not store `Context` in a ViewModel. Use `ApplicationContext` injected via Hilt `@ApplicationContext` if absolutely needed.

### Dependency Injection (Hilt)

- `@Singleton` — Repositories, DataStore, Database, Retrofit instances.
- `@ActivityRetainedScoped` — shared state that must survive config changes but not the full app lifetime.
- `@ViewModelScoped` — objects whose lifetime matches a single ViewModel.
- Inject interfaces, not concrete implementations, to keep layers testable.
- Provide fakes/test doubles via a `@TestInstallIn` module in the test source set.

### Room

- Define all entities in `Entities.kt`; update DB version and add a migration for every schema change.
- Use `@Transaction` for queries that read from multiple tables to avoid inconsistent reads.
- Return `Flow<T>` from DAO methods for observable queries; use `suspend fun` for one-shot writes.
- Never expose `Entity` classes outside the `data` layer — always map to domain models via `Mappers.kt`.

### Networking (Retrofit + OkHttp)

- All API calls are `suspend fun` in the Retrofit interface.
- Use an OkHttp `Interceptor` to attach the API key header — never hardcode it in call sites.
- Parse responses into sealed `Result<T>` types at the repository boundary; the domain layer never sees HTTP exceptions.
- Enable the OkHttp logging interceptor in debug builds only.

### Performance

- Use **Baseline Profiles** (`baseline-prof.txt`) to eliminate JIT lag on startup and critical paths.
- Prefer `rememberSaveable` over `remember` for state that should survive process death.
- Avoid allocations inside `drawWithContent`/`Canvas` lambdas; cache `Paint` and `Path` objects.
- Use `coil` with `AsyncImage` for all network/disk images; set explicit `size` to avoid over-fetching.

### Security & Privacy

- Store the Gemini API key in `local.properties` (git-ignored); read it via `BuildConfig` at compile time.
- Request only the Health Connect permissions that are actually used; re-check at runtime before each read.
- Use `EncryptedSharedPreferences` or DataStore with encryption for any PII stored on-device.

### Testing

- Unit test `Mappers.kt` and each `UseCase` class — these are the most critical correctness boundaries.
- Use **Turbine** (`app.cash.turbine:turbine`) to assert Flow emissions in sequence.
- Use `UnconfinedTestDispatcher` in `runTest` blocks to avoid timing issues with StateFlow collection.
- Prefer fakes over mocks for repository interfaces to keep tests readable and refactoring-safe.
