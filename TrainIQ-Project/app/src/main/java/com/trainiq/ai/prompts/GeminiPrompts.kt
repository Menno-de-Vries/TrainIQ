package com.trainiq.ai.prompts

object GeminiPrompts {
    fun workoutDebrief(totalVolume: Double, progression: Double, distribution: String) = """
        Analyze workout session data.
        Total volume: $totalVolume
        Progression vs last session: ${"%.1f".format(progression)}%
        Muscle group distribution: $distribution
        Return a concise summary, progression feedback, and a training recommendation.
    """.trimIndent()

    fun mealScanner(userContext: String) = """
        Analyze the meal photo and estimate visible foods.
        User context: ${userContext.ifBlank { "None provided." }}
        Return JSON only in this shape:
        {
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

    fun goalAdvisor(height: Double, weight: Double, bodyFat: Double, goal: String) = """
        Act as a fitness coach. Height: $height cm. Weight: $weight kg. Body fat: $bodyFat%.
        Goal: $goal.
        Return calorie target, macro split, recommended training focus, and a concise summary.
    """.trimIndent()

    fun weeklyReport(volume: Double, weightTrend: Double, adherence: Int) = """
        Create a weekly AI fitness report from:
        Weekly volume: $volume
        Weight trend: $weightTrend
        Meal adherence: $adherence%
        Return wins, risks, and next week focus.
    """.trimIndent()
}
