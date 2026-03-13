---
name: trainiq-core-architecture
description: Enforce MVVM + Clean + UDF architecture in the TrainIQ project. Use when creating new features, ViewModels, or refactoring existing code to ensure consistency in state management and navigation.
---

# TrainIQ Core Architecture Expert

This skill guides the implementation of high-quality Android code based on the TrainIQ project standards.

## Core Mandates

### 1. MVVM + Clean Architecture
- **Data Layer:** Contains Repositories and DataSources. No business logic.
- **Domain Layer:** Pure Kotlin. Contains UseCases and DomainModels. No Android dependencies.
- **UI Layer:** ViewModels and Compose Screens.

### 2. Unidirectional Data Flow (UDF)
- Every ViewModel must expose a single `uiState: StateFlow<T>`.
- Use a sealed interface for state:
  ```kotlin
  sealed interface HomeUiState {
      data object Loading : HomeUiState
      data class Success(val data: HomeData) : HomeUiState
      data class Error(val message: String) : HomeUiState
  }
  ```
- Events (e.g., navigation, show snackbar) must be handled via a `SharedFlow` or a similar one-time event mechanism.

### 3. Type-Safe Navigation
- All navigation must use `kotlinx.serialization` based routes.
- Routes are defined as `@Serializable` objects or classes.
- Avoid string-based route constants.

### 4. Dependency Injection (Hilt)
- Use `@Inject` for all dependencies.
- Ensure proper scoping: `@Singleton` for repositories, `@ViewModelScoped` for ViewModels.

## Prohibited Patterns
- ❌ Do NOT pass `Context` into ViewModels.
- ❌ Do NOT use `MutableState` directly in the UI if it belongs in the ViewModel.
- ❌ Do NOT call DAOs directly from ViewModels.
- ❌ Do NOT use hardcoded hex colors in Compose; use `MaterialTheme.colorScheme`.

## Workflow
1. **Define Domain Model:** Check `DomainModels.kt` first.
2. **Implement UseCase:** Ensure business logic is decoupled.
3. **Update UI State:** Create the sealed interface for the new feature.
4. **Build UI:** Use standard Material 3 components (`Scaffold`, `TopAppBar`).
