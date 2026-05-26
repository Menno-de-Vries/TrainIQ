package com.trainiq.ai.prompts

import com.trainiq.domain.model.BiologicalSex
import com.trainiq.domain.model.GoalAdvice
import java.util.Locale

object GeminiPrompts {
    fun workoutDebrief(
        totalVolume: Double,
        progression: Double?,
        comparisonSummary: String = if (progression == null) {
            "Nog geen eerdere vergelijkbare training gevonden."
        } else {
            "Vergelijking beschikbaar."
        },
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
        - Totaalvolume: ${totalVolume}kg
        - Vergelijking: $comparisonSummary
        - Volumeprogressie tegenover vorige vergelijkbare training: ${progression?.let { "${String.format(Locale.US, "%.1f", it)}%" } ?: "niet beschikbaar"}
        - Verdeling spiergroepen: $distribution
        - Gemiddelde RPE: $avgRpe / 10
        - Belangrijkste lifts: $topExercises
        - Trainingsfrequentie per week: $weeklyFrequency dagen/week

        Regels:
        - Gebruik geen Engels, behalve oefeningnamen als die in de invoer Engels zijn
        - summary is 1 korte zin
        - progressionFeedback is 1 korte zin
        - recommendation is een concrete Nederlandse actie: verhogen, gelijk houden, verlagen of eerst data verbeteren
        - wins bevat 2 tot 4 korte hoogtepunten
        - risks bevat 0 tot 3 korte aandachtspunten; benoem ontbrekende RPE of beperkte historie netjes als dat relevant is
        - recoveryAdvice is 1 korte herstelzin
        - Als gemiddelde RPE > 8,5 en progressie > 5%, waarschuw kort over herstel
        - Als gemiddelde RPE < 6 en volume stijgt, adviseer iets hogere intensiteit
        - nextSessionFocus moet specifiek zijn: oefening plus doelgewicht of herhalingen
        - Adviseer geen agressieve gewichtsstappen bij RPE 9-10 of grote volumestijging
        - nextLoadTarget moet concreet zijn, bijvoorbeeld "Bench Press: 82,5 kg x 6-8 voor 3 werksets"
        - Verzin geen PR's, volume of vorige-sessie vergelijkingen als de data ontbreekt
        - Als vergelijking niet beschikbaar is, schrijf exact dat er nog geen eerdere vergelijkbare training is gevonden

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
        Gebruikerscontext: ${userContext.ifBlank { "Niet opgegeven." }}
        Regels voor context:
        - Als de gebruikerscontext een gewicht noemt, behandel dat gewicht als waarheid.
        - Herbereken of overschrijf opgegeven gewichten niet.
        - Als de gebruikerscontext meerdere losse componenten noemt, behoud precies die componentgrenzen en namen.
        - Gebruik de foto alleen om ontbrekende hoeveelheden en macro's aan te vullen.
        - Maak verschillende contextcomponenten nooit hetzelfde item; markeer onzekerheid met confidence "low" en notes.
        - Schat alleen hoeveelheden waar de gebruiker geen gewicht voor noemt.
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

    fun bodyMeasurementPhoto(userContext: String) = """
        Je bent een nauwkeurige data-assistent voor TrainIQ.
        Lees een foto of screenshot van een smart-weegschaal of smart-scale app uit.
        Gebruikerscontext: ${userContext.ifBlank { "Niet opgegeven." }}

        Regels:
        - Antwoord altijd als JSON only, zonder markdown.
        - Als de gebruikerscontext gewicht, vetpercentage of spiermassa noemt, behandel die waarde als waarheid.
        - Herbereken of overschrijf opgegeven contextwaarden niet.
        - Gebruik kg voor gewicht en spiermassa.
        - bodyFat is een percentage, bijvoorbeeld 18.4.
        - muscleMass is skeletspiermassa of spiermassa in kg. Als alleen percentage zichtbaar is, schat niet; geef dan 0.
        - Als een waarde niet betrouwbaar zichtbaar is, geef 0 en confidence "low".
        - notes is een korte Nederlandse toelichting.

        Return JSON only:
        {
          "weight": 82.4,
          "bodyFat": 18.2,
          "muscleMass": 38.5,
          "unit": "kg",
          "confidence": "high|medium|low",
          "notes": "string"
        }
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
        Lengte: $height cm. Gewicht: $weight kg. Vetpercentage: $bodyFat%. Leeftijd: $age. Biologische sekse: ${sex.name}.
        Activiteitsniveau: $activityLevel.
        Doel: $goal.
        Vaste lokale berekening:
        - BMR (Mifflin-St Jeor): ${baseline.bmr} kcal
        - Onderhoud: ${baseline.maintenanceCalories} kcal
        - Activiteitsfactor: ${String.format(Locale.US, "%.3f", baseline.activityMultiplier)}
        - Doelcalorieën: ${baseline.calorieTarget} kcal
        - Eiwit: ${baseline.proteinTarget} g
        - Koolhydraten: ${baseline.carbsTarget} g
        - Vet: ${baseline.fatTarget} g
        Wijzig deze calorie- en macrocijfers niet. Gebruik ze als vaste output en leg kort uit waarom ze bij het doel passen.
        Leg activiteit eenvoudig uit: onderhoud = BMR x activiteitsfactor. Noem onzekerheid zonder extra data te verzinnen.
        Gebruik geen Engels. Vertaal activiteitsniveau en doel natuurlijk naar het Nederlands als de invoer Engels is.
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
        Weekvolume: $volume
        Gewichtstrend: $weightTrend
        Voedingsconsistentie: $adherence%
        Return JSON only:
        {
          "summary": "string",
          "wins": ["string"],
          "risks": ["string"],
          "nextWeekFocus": "string",
          "rationaleBullets": ["korte Nederlandse onderbouwing zonder interne redeneerstappen"]
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

        Gebruikersprofiel:
        - Doel: $goal
        - Trainingssplit: $targetFocus
        - Dagen per week: $daysPerWeek
        - Beschikbaar materiaal: $equipment
        - Ervaringsniveau: $experienceLevel
        - Sessieduur: ongeveer $sessionDurationMinutes minuten
        - Deload-richtlijn opnemen: $includeDeload

        Periodiseringsregels per niveau:
        - Beginner: lineaire progressie, 3x8-12, bij voorkeur volledig lichaam
        - Gemiddeld: boven/onder of push/pull/legs, wekelijkse progressieve overbelasting met kleine stappen
        - Gevorderd: golvende periodisering, RPE-gestuurde belasting (RPE 7-9), deload rond elke vierde week

        Aantal oefeningen per sessie:
        - 30 min -> 3-4 oefeningen
        - 45 min -> 4-5 oefeningen
        - 60 min -> 5-6 oefeningen
        - 90 min -> 6-8 oefeningen

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
