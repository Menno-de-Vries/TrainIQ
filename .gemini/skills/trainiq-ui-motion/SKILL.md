---
name: trainiq-ui-motion
description: Expert guidance for the TrainIQ "Alive" interface using Material 3, advanced Compose animations, and haptic feedback. Use when creating new screens, transitions, or micro-interactions.
---

# Skill: TrainIQ UI Motion & Aesthetics ✨

Expert guidance for creating the "Alive" interface using Material 3, advanced Compose animations, and platform-native feedback.

## Core Principles
- **Fluidity:** Every state transition must be animated. Avoid abrupt "popping" of UI elements.
- **Contextual Meaning:** Use motion to explain where data is coming from (e.g., a card expanding into a detail view).
- **Haptic Harmony:** Use tactile feedback sparingly but effectively to reinforce success or warning states.

## Material 3 Standards
- **Dynamic Color:** Always utilize `MaterialTheme.colorScheme` to support user-defined themes (Android 12+).
- **Surfaces:** Use `TonalElevation` to distinguish between different layers of information.
- **Typography:** Strictly adhere to the scale defined in `Theme.kt` (Display, Headline, Title, Body, Label).

## Animation Patterns
- **Shared Elements:** Use `SharedTransitionLayout` (Compose 1.7+) for transitions between `HomeScreen` and `WorkoutScreen`.
- **Shimmer Effects:** Implement custom shimmers for all loading states. Do not use generic circular progress indicators for main content.
- **Predictive Back:** Ensure all navigation flows support Android's predictive back gesture.

## Micro-interactions
- **Haptics:** 
    - `HapticFeedbackType.LongPress` for editing sets.
    - `HapticFeedbackType.TextHandleMove` for scroll snapping in timers.
- **AnimatedVisibility:** Use `expandVertically` + `fadeIn` for adding new sets to a workout.

## Verification Checklist
- [ ] Does the app look consistent in both Light and Dark modes?
- [ ] Is the "Looming" effect (subtle scale animation) applied to the primary CTA buttons?
- [ ] Do animations run at a smooth 60/120 FPS on target devices?
