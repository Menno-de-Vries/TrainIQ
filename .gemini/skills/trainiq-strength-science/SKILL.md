---
name: trainiq-strength-science
description: Expert guidance for strength training logic, exercise physiology calculations (1RM, RPE), and progression algorithms. Use when implementing workout logging, volume tracking, or AI-driven training adjustments.
---

# Skill: TrainIQ Strength Science 🏋️‍♂️

Expert guidance for implementing strength training logic, exercise physiology calculations, and progression algorithms within the TrainIQ ecosystem.

## Core Principles
- **Accuracy First:** Strength calculations (1RM, Volume, RPE) must be scientifically grounded.
- **Progressive Overload:** Always prioritize logic that tracks and suggests incremental increases in intensity or volume.
- **Standardization:** Use consistent units (kg/lbs) and naming conventions for exercises across the app.

## Domain Logic & Formulas

### 1. One-Rep Max (1RM) Calculations
When calculating estimated 1RM, use the **Brzycki Formula** for reps ≤ 10:
`1RM = weight / (1.0278 - (0.0278 * reps))`
For reps > 10, use the **Epley Formula**:
`1RM = weight * (1 + 0.0333 * reps)`

### 2. Training Intensity (RPE & RIR)
- **RPE (Rate of Perceived Exertion):** Scale of 1-10.
- **RIR (Reps in Reserve):** `10 - RPE = RIR`.
- Implementation: When a user enters RPE 8, the system must log 2 RIR. Suggest weight increases only when RIR is ≥ 2 consistently.

### 3. Volume Load
`Volume Load = Sets * Reps * Weight`.
When comparing sessions, volume load must be normalized against body weight where relevant (e.g., for squats vs. isolation moves).

## Database & Model Standards
- **Exercise Naming:** Follow a "Muscle Group - Exercise - Variation" pattern (e.g., "Chest - Bench Press - Incline Dumbbell").
- **Rest Timers:** Default to 120s for compound movements (>3 muscles) and 60s for isolation movements.
- **Set Types:** Differentiate between `WARMUP`, `WORKING`, `TOP_SET`, and `BACKOFF_SET` in the `Entities.kt`.

## UI/UX Requirements
- **Plate Calculator:** Always provide a helper to translate weight to 20kg/10kg/5kg/2.5kg/1.25kg plates.
- **Visual Cues:** Use color coding for intensity (Green: RPE <7, Yellow: RPE 8-9, Red: RPE 10).
- **Rest State:** The UI must prioritize the countdown timer during the rest period between sets.

## Verification Checklist
- [ ] Does the 1RM calculation handle edge cases (e.g., 0 reps)?
- [ ] Is the volume load correctly aggregated in the `ProgressScreen`?
- [ ] Does the AI "Coach" use these specific formulas when suggesting next week's weights?
