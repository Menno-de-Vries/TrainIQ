# Training Setup Entry Design

Date: 2026-05-10

## Goal

Make the existing Training setup flow reachable from the current Training overview without changing the underlying routine builder, workout start, or persistence behavior.

The immediate QA blocker is that a newly created empty active routine tells the user to open the routine below and add a training day/exercise, but the visible active-routine card does not expose a clear entry into the existing detail builder. The app already has a routine detail mode with `Sessies`, `Eerste oefening toevoegen`, set editing, and workout start behavior. This change should expose that existing path.

## Scope

In scope:

- Add a clear `Routine inrichten` action to the active routine card when the routine has no startable workout day.
- Route that action to the existing routine detail mode by setting the selected routine id.
- Keep the existing start action unchanged when a routine already has a startable day.
- Keep the existing routine list `Details`, `Actief maken`, and `Start` actions working.
- Add source-level regression coverage for the empty-active-routine setup entry and existing start behavior.

Out of scope:

- New routine-builder architecture.
- New persistence paths.
- New generated sample data or debug-only fixtures.
- Redesigning all Training cards, history, or exercise library.
- Changing active workout completion/debrief behavior directly.

## User Experience

When a user creates an empty routine:

1. The active routine card still shows the routine name and explanatory empty-state copy.
2. Instead of only telling the user to open the routine below, the card shows a primary action: `Routine inrichten`.
3. Tapping `Routine inrichten` opens the existing routine detail screen for that routine.
4. In detail mode, the existing `Sessies` tab and `Eerste oefening toevoegen` path remain the setup surface.
5. Once the routine has a startable day, the active routine card shows the existing start button and no longer needs the setup action.

## Architecture

`WorkoutScreen` already owns `selectedRoutineId` and renders detail mode when it is set. The change should reuse that state:

- Extend `ActiveRoutineCard` with an `onOpenDetails: (Long) -> Unit` callback.
- In `WorkoutScreen`, pass `{ selectedRoutineId = it }`.
- Inside `ActiveRoutineCard`, call `onOpenDetails(activeRoutine.id)` only when `firstStartableDay()` is null.
- Preserve `onStartWorkout(startableDay.id)` when `firstStartableDay()` is non-null.

This keeps navigation local to the existing Training screen and avoids duplicating routine setup logic.

## Data Flow

No new data model or repository behavior is required.

Current flow remains:

- `WorkoutViewModel.uiState`
- `WorkoutScreen`
- `ActiveRoutineCard`
- `selectedRoutineId`
- existing `RoutineCard(detailMode = true)`

The only new data passed to a child component is the routine id for the setup-entry callback.

## Error Handling

No new error path is introduced. If the routine disappears before the callback resolves, the existing selected-routine reconciliation in `WorkoutScreen` clears invalid selections through `resolveSelectedRoutineId`.

The existing message card, empty-state copy, and detail-mode behavior remain unchanged except for making the action explicit.

## Testing

Add or extend source-level tests around `WorkoutScreen.kt` behavior guards:

- Empty active routine copy/action includes `Routine inrichten`.
- Existing startable routine still uses `activeRoutineStartLabel(...)` / `onStartWorkout` behavior.
- The existing `Details` entry on routine list cards remains present.

Run:

- `./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --console=plain --no-configuration-cache`
- `./gradlew.bat :app:assembleDebug --console=plain --no-configuration-cache`

If feasible after implementation, rerun physical-device Training setup QA and confirm an empty routine can reach the detail builder.

## Acceptance Criteria

- A newly created empty active routine exposes a visible setup action from the active-routine card.
- Tapping that action opens the existing routine detail mode.
- Start behavior for routines with a startable day remains unchanged.
- Existing routine list details flow remains unchanged.
- No app code outside the Training feature is changed unless required by tests.
