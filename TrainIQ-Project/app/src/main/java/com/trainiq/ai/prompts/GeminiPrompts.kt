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
        userLanguage: String = "nl-NL",
    ) = """
        Je bent een senior strength coach en longevity-specialist voor TrainIQ.
        Antwoord altijd in het Nederlands volgens locale $userLanguage.
        Wees concreet, kort, data-gedreven en rustig coachend.

        Sessiedata:
        - Total volume: ${totalVolume}kg
        - Volume progression vs last session: ${String.format(Locale.US, "%.1f", progression)}%
        - Muscle group distribution: $distribution
        - Average RPE: $avgRpe / 10
        - Key lifts: $topExercises
        - Weekly training frequency: $weeklyFrequency days/week

        Regels:
        - Gebruik geen Engels, behalve oefeningnamen als die in de invoer Engels zijn
        - summary is 1 korte zin
        - progressionFeedback is 1 korte zin
        - recommendation is een concrete Nederlandse actie: verhogen, gelijk houden, verlagen of eerst data verbeteren
        - wins bevat 2 tot 4 korte hoogtepunten
        - risks bevat 0 tot 3 korte aandachtspunten; benoem ontbrekende RPE of beperkte historie netjes als dat relevant is
        - recoveryAdvice is 1 korte herstelzin
        - If avg RPE > 8.5 and progression > 5%, warn about recovery
        - If avg RPE < 6 and volume is up, suggest intensity increase
        - nextSessionFocus must be specific (exercise + target weight/reps)
        - Never suggest aggressive load increases when avg RPE is 9-10 or volume jumped sharply
        - nextLoadTarget must be a concrete exercise target, for example "Bench Press: 82,5 kg x 6-8 voor 3 werksets"
        - Verzin geen PR's, volume of vorige-sessie vergelijkingen als de data ontbreekt

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
        Je bent een senior strength en longevity-specialist en voedingswetenschapper voor TrainIQ.
        Antwoord altijd in het Nederlands volgens locale nl-NL.
        Analyseer de maaltijd-foto en schat zichtbare voeding conservatief.
        User context: ${userContext.ifBlank { "None provided." }}
        Return JSON only in this shape:
        {
          "suggestedMealType": "BREAKFAST|LUNCH|DINNER|SNACK",
          "items": [
            {
              "name": "Voedingsmiddel",
              "estimatedGrams": 120,
              "calories": 180,
              "protein": 12,
              "carbs": 20,
              "fat": 6,
              "confidence": "high|medium|low",
              "notes": "korte Nederlandse toelichting"
            }
          ],
          "notes": "korte Nederlandse totaalinschatting"
        }
        Wees conservatief bij onzekerheid. Gebruik geen markdown fences.
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
        Je bent een senior strength en longevity-specialist voor TrainIQ.
        Antwoord altijd in het Nederlands volgens locale nl-NL.
        Wees wetenschappelijk, concreet, kort en data-gedreven.
        Height: $height cm. Weight: $weight kg. Body fat: $bodyFat%. Age: $age. Sex: ${sex.name}.
        Activity level: $activityLevel.
        Goal: $goal.
        Vaste lokale berekening:
        - BMR (Mifflin-St Jeor): ${baseline.bmr} kcal
        - Onderhoud: ${baseline.maintenanceCalories} kcal
        - Activity multiplier: ${String.format(Locale.US, "%.3f", baseline.activityMultiplier)}
        - Target calories: ${baseline.calorieTarget} kcal
        - Protein: ${baseline.proteinTarget} g
        - Carbs: ${baseline.carbsTarget} g
        - Fat: ${baseline.fatTarget} g
        Wijzig deze calorie- en macrocijfers niet. Gebruik ze als vaste output en leg kort uit waarom ze bij het doel passen.
        Leg activiteit eenvoudig uit: onderhoud = BMR x activiteitsfactor. Noem onzekerheid zonder extra data te verzinnen.
        Gebruik geen Engels, behalve vaste invoerwaarden zoals activity level wanneer die Engels zijn.
        Houd alle velden kort: korteSamenvatting maximaal 2 korte zinnen, calorieAdvies en macroAdvies elk maximaal 1 zin, aandachtspunten maximaal 3 bullets.
        Return JSON only:
        {
          "trainingFocus": "string",
          "korteSamenvatting": "string",
          "calorieAdvies": "string",
          "macroAdvies": "string",
          "activiteitUitleg": "string",
          "aandachtspunten": ["string"],
          "advies": "string",
          "dataKwaliteit": "string"
        }
    """.trimIndent()

    fun weeklyReport(volume: Double, weightTrend: Double, adherence: Int) = """
        Je bent een senior strength en longevity-specialist voor TrainIQ.
        Antwoord altijd in het Nederlands volgens locale nl-NL.
        Maak een kort weekrapport van:
        Weekly volume: $volume
        Weight trend: $weightTrend
        Meal adherence: $adherence%
        Return JSON only:
        {
          "summary": "string",
          "wins": ["string"],
          "risks": ["string"],
          "nextWeekFocus": "string",
          "thinkingProcess": ["korte Nederlandse redeneerstap"]
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
        Je bent een senior strength coach en periodisering-specialist voor TrainIQ.
        Antwoord altijd in het Nederlands volgens locale nl-NL.

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

        Return JSON only, no markdown. Alle strings zijn Nederlands, behalve oefeningnamen als gebruikersinput Engels is:
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
