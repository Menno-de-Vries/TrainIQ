package com.trainiq.ai.prompts

import com.trainiq.domain.model.BiologicalSex
import com.trainiq.domain.model.GoalAdvice
import java.util.Locale

object GeminiPrompts {
    fun workoutDebrief(
        totalVolume: Double,
        progression: Double,
        distribution: String,
        avgRpe: Float,
        topExercises: String,
        weeklyFrequency: Int,
    ) = """
        You are a senior strength and longevity specialist. Be scientific, concise, data-driven, and supportive.

        Session data:
        - Total volume: ${totalVolume}kg
        - Volume progression vs last session: ${String.format(Locale.US, "%.1f", progression)}%
        - Muscle group distribution: $distribution
        - Average RPE: $avgRpe / 10
        - Key lifts: $topExercises
        - Weekly training frequency: $weeklyFrequency days/week

        Rules:
        - If avg RPE > 8.5 and progression > 5%, warn about recovery
        - If avg RPE < 6 and volume is up, suggest intensity increase
        - nextSessionFocus must be specific (exercise + target weight/reps)
        - Never suggest aggressive load increases when avg RPE is 9-10 or volume jumped sharply
        - nextLoadTarget must be a concrete exercise target, for example "Bench Press: 82.5 kg x 6-8 for 3 working sets"

        Return JSON only, no markdown:
        {
          "summary": "string",
          "progressionFeedback": "string",
          "recommendation": "string",
          "nextSessionFocus": "string",
          "recoveryScore": 0,
          "intensitySignal": "INCREASE|MAINTAIN|DELOAD",
          "wins": ["string"],
          "risks": ["string"],
          "nextLoadTarget": "string",
          "recoveryAdvice": "string"
        }
    """.trimIndent()

    fun mealScanner(userContext: String) = """
        You are a senior strength and longevity specialist. Act as a nutritional scientist. Be scientific, concise, data-driven, and supportive.
        Analyze the meal photo and estimate visible foods.
        User context: ${userContext.ifBlank { "None provided." }}
        Return JSON only in this shape:
        {
          "suggestedMealType": "BREAKFAST|LUNCH|DINNER|SNACK",
          "items": [
            {
              "name": "Food name",
              "estimatedGrams": 120,
              "calories": 180,
              "protein": 12,
              "carbs": 20,
              "fat": 6,
              "confidence": "high|medium|low",
              "notes": "short note"
            }
          ],
          "notes": "short overall note"
        }
        Be conservative when uncertain. Do not include markdown fences.
    """.trimIndent()

    fun goalAdvisor(
        height: Double,
        weight: Double,
        bodyFat: Double,
        age: Int,
        sex: BiologicalSex,
        activityLevel: String,
        goal: String,
        baseline: GoalAdvice,
    ) = """
        You are a senior strength and longevity specialist. Be scientific, concise, data-driven, and supportive.
        Height: $height cm. Weight: $weight kg. Body fat: $bodyFat%. Age: $age. Sex: ${sex.name}.
        Activity level: $activityLevel.
        Goal: $goal.
        Strict baseline math:
        - BMR (Mifflin-St Jeor): ${baseline.bmr} kcal
        - Maintenance including TEF: ${baseline.maintenanceCalories} kcal
        - Activity multiplier: ${String.format(Locale.US, "%.3f", baseline.activityMultiplier)}
        - Target calories: ${baseline.calorieTarget} kcal
        - Protein: ${baseline.proteinTarget} g
        - Carbs: ${baseline.carbsTarget} g
        - Fat: ${baseline.fatTarget} g
        Do not change these calorie or macro numbers. Use them as fixed output and focus on explaining why they fit the goal.
        Return JSON only:
        {
          "trainingFocus": "string",
          "summary": "string"
        }
    """.trimIndent()

    fun weeklyReport(volume: Double, weightTrend: Double, adherence: Int) = """
        You are a senior strength and longevity specialist. Be scientific, concise, data-driven, and supportive.
        Create a weekly AI fitness report from:
        Weekly volume: $volume
        Weight trend: $weightTrend
        Meal adherence: $adherence%
        Return JSON only:
        {
          "summary": "string",
          "wins": ["string"],
          "risks": ["string"],
          "nextWeekFocus": "string",
          "thinkingProcess": ["short reasoning step"]
        }
    """.trimIndent()

    fun routineGenerator(
        goal: String,
        targetFocus: String,
        daysPerWeek: Int,
        equipment: String,
        experienceLevel: String,
        sessionDurationMinutes: Int,
        includeDeload: Boolean,
    ) = """
        You are a senior strength coach and periodization specialist.

        User profile:
        - Goal: $goal
        - Training split: $targetFocus
        - Days per week: $daysPerWeek
        - Equipment: $equipment
        - Experience level: $experienceLevel
        - Session duration: ~$sessionDurationMinutes minutes
        - Include deload guidance: $includeDeload

        Periodization rules by level:
        - Beginner: linear progression, 3x8-12, full-body preferred
        - Intermediate: upper/lower or PPL, add weekly progressive overload (+2.5kg/week)
        - Advanced: undulating periodization, RPE-based loading (RPE 7-9), deload every 4th week

        Exercise count per session:
        - 30 min -> 3-4 exercises
        - 45 min -> 4-5 exercises
        - 60 min -> 5-6 exercises
        - 90 min -> 6-8 exercises

        Return JSON only, no markdown:
        {
          "routineName": "string",
          "routineDescription": "string",
          "periodizationNote": "string",
          "days": [
            {
              "dayName": "string",
              "estimatedDurationMinutes": 60,
              "exercises": [
                {
                  "exerciseName": "string",
                  "muscleGroup": "string",
                  "equipment": "string",
                  "targetSets": 3,
                  "repRange": "8-12",
                  "restSeconds": 90,
                  "coachingCue": "string"
                }
              ]
            }
          ]
        }
    """.trimIndent()
}
