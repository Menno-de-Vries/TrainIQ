---
name: trainiq-nutrition-vision
description: Specialized knowledge for food scanning, label analysis, and macro-tracking using Gemini 2.5 Flash in TrainIQ. Use when implementing meal recognition, nutrition data extraction, or supplement logging.
---

# Skill: TrainIQ Nutrition Vision 🥗

Specialized knowledge for implementing food scanning, label analysis, and macro-tracking logic using Gemini 2.5 Flash.

## Core Principles
- **Accuracy over Speed:** Use high-resolution image analysis and specific prompting to avoid "hallucinated" calories.
- **Atomic Logging:** Always break down multi-ingredient meals into their base components where possible.
- **User Verification:** Never log nutrition data without a "Confirm/Edit" step in the UI.

## Gemini Implementation (Vision)
- **Prompting:** Use the `Coach` persona but switch to "Nutritional Scientist" sub-persona. 
- **JSON Schema:** Always request a structured response that maps to `NutritionStorageModels.kt`.
- **Unit Conversion:** Standardize all output to grams (g) and kilocalories (kcal).

## Data Mapping & Storage
- **Mapping:** Use `Mappers.kt` to convert AI DTOs into `MealEntry` domain models.
- **Contextual Knowledge:** Use the user's weight and goal (from `UserPreferencesRepository`) to calculate "Remaining Macros" dynamically.
- **Supplement Logic:** Identify common supplements (Whey, Creatine, Pre-workout) and log them under a specific `SUPPLEMENT` category to keep them separate from whole foods.

## UI/UX Requirements
- **Visual Feedback:** Show bounding boxes or labels on detected food items in the camera preview if possible.
- **Manual Override:** The macro-tracker must always allow manual entry of protein, carbs, and fats.
- **Dynamic Goals:** Adjust daily targets based on the day's training volume (from `trainiq-strength-science`).

## Verification Checklist
- [ ] Does the AI correctly identify common trigger words (e.g., "low fat", "high protein") on labels?
- [ ] Is the JSON response handled safely in `AiServices.kt`?
- [ ] Does the UI update instantly after a meal is confirmed (UDF pattern)?
