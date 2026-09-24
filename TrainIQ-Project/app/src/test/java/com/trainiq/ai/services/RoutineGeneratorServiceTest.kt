package com.trainiq.ai.services

import com.trainiq.domain.model.AiFallbackContext
import com.trainiq.domain.model.RoutineGenerationOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Test

class RoutineGeneratorServiceTest {

    @Test
    fun fallbackRespectsEquipmentExclusionsSplitAndActualTimeBudget() {
        val routine = fallbackGeneratedRoutine(
            goal = "Kracht", targetFocus = "Onderlichaam", daysPerWeek = 2,
            equipment = "Lichaamsgewicht", experienceLevel = "beginner",
            sessionDurationMinutes = 30, includeDeload = false,
            excludedExercises = listOf("Plank"),
        )

        assertEquals(2, routine.days.size)
        routine.days.forEach { day ->
            assertTrue(day.exercises.isNotEmpty())
            assertTrue(day.exercises.all { it.equipment == "Lichaamsgewicht" })
            assertTrue(day.exercises.none { it.exerciseName == "Plank" })
            assertTrue(day.exercises.all { it.muscleGroup in setOf("Benen", "Hamstrings", "Bilspieren") })
            assertTrue(day.estimatedDurationMinutes <= 30)
        }
    }

    @Test
    fun generatedRoutineContractRejectsWrongDaysEquipmentExclusionsAndTime() {
        val fallback = fallbackGeneratedRoutine("Kracht", "Full body", 3, "Dumbbells", "beginner", 45, false)
        val valid = fallback.copy(source = GeneratedRoutineSource.OPENAI)
        val options = RoutineGenerationOptions(excludedExercises = listOf("Onbekende oefening"))

        assertTrue(valid.respectsRequest(3, "Dumbbells", 45, options, emptyList()))
        assertTrue(!valid.respectsRequest(4, "Dumbbells", 45, options, emptyList()))
        val barbell = valid.copy(days = valid.days.map { day -> day.copy(exercises = day.exercises.mapIndexed { index, ex ->
            if (index == 0) ex.copy(equipment = "Halterstang") else ex
        }) })
        assertTrue(!barbell.respectsRequest(3, "Dumbbells", 45, options, emptyList()))
        val excluded = valid.copy(days = valid.days.map { day -> day.copy(exercises = day.exercises + GeneratedExercise("Onbekende oefening", "Core", "Lichaamsgewicht", 3, "30s", 45)) })
        assertTrue(!excluded.respectsRequest(3, "Dumbbells", 45, options, emptyList()))
        assertTrue(!valid.respectsRequest(3, "Dumbbells", 5, options, emptyList()))
    }

    @Test
    fun generatedRoutineContractRejectsExerciseIdThatNamesAnotherExercise() {
        val routine = fallbackGeneratedRoutine("Kracht", "Full body", 2, "Halterstang", "beginner", 45, false)
        val first = routine.days.first().exercises.first()
        val withId = routine.copy(days = routine.days.mapIndexed { dayIndex, day ->
            if (dayIndex == 0) day.copy(exercises = listOf(first.copy(existingExerciseId = 7L)) + day.exercises.drop(1)) else day
        })
        val known = listOf("7: Andere oefening | Benen | ${first.equipment}")

        assertTrue(!withId.respectsRequest(2, "Halterstang", 45, RoutineGenerationOptions(), known))
        assertTrue(withId.respectsRequest(2, "Halterstang", 45, RoutineGenerationOptions(),
            listOf("7: ${first.exerciseName} | ${first.muscleGroup} | ${first.equipment}")))
    }

    @Test
    fun generatedRoutineContractRequiresEveryRequestedExerciseAndMusclePriority() {
        val routine = fallbackGeneratedRoutine("Kracht", "Full body", 2, "Dumbbells", "beginner", 45, false)
        val chosen = routine.days.first().exercises.first()

        assertTrue(routine.respectsRequest(2, "Dumbbells", 45,
            RoutineGenerationOptions(preferredExercises = listOf(chosen.exerciseName), priorityMuscleGroups = listOf(chosen.muscleGroup)), emptyList()))
        assertTrue(!routine.respectsRequest(2, "Dumbbells", 45,
            RoutineGenerationOptions(preferredExercises = listOf("Ontbrekende oefening")), emptyList()))
        assertTrue(!routine.respectsRequest(2, "Dumbbells", 45,
            RoutineGenerationOptions(priorityMuscleGroups = listOf("Onbekende spiergroep")), emptyList()))
    }

    @Test
    fun fallbackCanUsePreferredExerciseFromExistingLibrary() {
        val existing = listOf("88: Custom Row | Back | Dumbbells")
        val routine = fallbackGeneratedRoutine(
            goal = "Kracht", targetFocus = "Full body", daysPerWeek = 2,
            equipment = "Dumbbells", experienceLevel = "beginner", sessionDurationMinutes = 45,
            includeDeload = false, preferredExercises = listOf("Custom Row"), existingExercises = existing,
        )

        assertTrue(routine.days.any { day -> day.exercises.any { it.exerciseName == "Custom Row" && it.equipment == "Dumbbells" } })
        assertTrue(routine.respectsRequest(2, "Dumbbells", 45,
            RoutineGenerationOptions(preferredExercises = listOf("Custom Row")), existing))
    }

    @Test
    fun routineGeneratorService_usesProviderRouterBoundary() {
        val source = java.io.File(
            "src/main/java/com/trainiq/ai/services/RoutineGeneratorService.kt",
        ).readText()

        assertTrue(source.contains("aiJsonGenerator.generateJson"))
        assertTrue(source.contains("responseJsonSchema = AiJsonSchemas.routineGenerator"))
        assertTrue(source.contains("AiRouteRequest"))
    }

    @Test
    fun parseGeneratedRoutine_withFullJson_mapsRoutineMetadataAndCoachingCue() {
        val fallback = fallbackGeneratedRoutine(
            goal = "Lean bulk",
            targetFocus = "Upper/Lower",
            daysPerWeek = 4,
            equipment = "Barbell",
            experienceLevel = "intermediate",
            sessionDurationMinutes = 60,
            includeDeload = true,
        )
        val json = """
            {
              "routineName": "Upper kracht",
              "routineDescription": "Zware focus op bovenlichaam met beheerste progressie.",
              "periodizationNote": "Golfbelasting met een geplande deload.",
              "estimatedDurationMinutes": 75,
              "days": [
                {
                  "dayName": "Upper A",
                  "exercises": [
                    {
                      "exerciseName": "Bench Press",
                      "muscleGroup": "Borst",
                      "equipment": "Halterstang",
                      "targetSets": 4,
                      "repRange": "4-6",
                      "restSeconds": 150,
                      "coachingCue": "Houd het stangpad stabiel."
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val routine = parseGeneratedRoutine(json, fallback)

        assertEquals("Upper kracht", routine.routineName)
        assertEquals("Zware focus op bovenlichaam met beheerste progressie.", routine.routineDescription)
        assertEquals("Golfbelasting met een geplande deload.", routine.periodizationNote)
        assertEquals(estimatedRoutineDurationMinutes(routine.days.first().exercises), routine.estimatedDurationMinutes)
        assertEquals(1, routine.days.size)
        assertEquals("Upper A", routine.days.first().dayName)
        assertEquals(estimatedRoutineDurationMinutes(routine.days.first().exercises), routine.days.first().estimatedDurationMinutes)
        assertEquals("Bench Press", routine.days.first().exercises.first().exerciseName)
        assertEquals("Houd het stangpad stabiel.", routine.days.first().exercises.first().coachingCue)
    }

    @Test
    fun parseGeneratedRoutine_withDutchCoachingTermsAndEnglishExerciseNames_usesGeminiResult() {
        val fallback = fallbackGeneratedRoutine(
            goal = "Spiermassa",
            targetFocus = "Upper/lower",
            daysPerWeek = 4,
            equipment = "Halterstang",
            experienceLevel = "intermediate",
            sessionDurationMinutes = 60,
            includeDeload = true,
        )
        val json = """
            {
              "routineName": "Upper lower krachtblok",
              "routineDescription": "Vier dagen per week met progressieve overload en rustige deload-opbouw.",
              "periodizationNote": "Verhoog alleen als techniek en herstel stabiel blijven.",
              "estimatedDurationMinutes": 60,
              "days": [
                {
                  "dayName": "Bovenlichaam A",
                  "estimatedDurationMinutes": 60,
                  "exercises": [
                    {
                      "exerciseName": "Bench Press",
                      "muscleGroup": "Borst",
                      "equipment": "Halterstang",
                      "targetSets": 4,
                      "repRange": "6-8",
                      "restSeconds": 120,
                      "coachingCue": "Houd spanning op je bovenrug en druk gecontroleerd uit."
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val routine = parseGeneratedRoutine(json, fallback)

        assertEquals(GeneratedRoutineSource.GEMINI_2_5_FLASH, routine.source)
        assertEquals("Upper lower krachtblok", routine.routineName)
        assertEquals("Bench Press", routine.days.first().exercises.first().exerciseName)
    }

    @Test
    fun parseGeneratedRoutine_withPartialJson_appliesFallbackDefaultsToMissingFields() {
        val fallback = fallbackGeneratedRoutine(
            goal = "Cut",
            targetFocus = "Full body",
            daysPerWeek = 3,
            equipment = "Dumbbells",
            experienceLevel = "beginner",
            sessionDurationMinutes = 45,
            includeDeload = false,
        )
        val json = """
            {
              "routineName": "Eenvoudig plan",
              "days": [
                {
                  "dayName": "Dag 1",
                  "exercises": [
                    {
                      "exerciseName": "Squat"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val routine = parseGeneratedRoutine(json, fallback)

        assertEquals("Eenvoudig plan", routine.routineName)
        assertEquals("", routine.routineDescription)
        assertEquals("", routine.periodizationNote)
        assertEquals(estimatedRoutineDurationMinutes(routine.days.first().exercises), routine.estimatedDurationMinutes)
        assertEquals(1, routine.days.size)
        assertEquals("Squat", routine.days.first().exercises.first().exerciseName)
        assertEquals("Algemeen", routine.days.first().exercises.first().muscleGroup)
        assertEquals("", routine.days.first().exercises.first().coachingCue)
    }

    @Test
    fun parseGeneratedRoutine_withExistingExerciseId_keepsLibraryReference() {
        val fallback = fallbackGeneratedRoutine(
            goal = "Spiermassa",
            targetFocus = "Full body",
            daysPerWeek = 3,
            equipment = "Barbell",
            experienceLevel = "intermediate",
            sessionDurationMinutes = 60,
            includeDeload = false,
        )
        val json = """
            {
              "routineName": "Volledig lichaam kracht",
              "days": [
                {
                  "dayName": "Dag 1",
                  "exercises": [
                    {
                      "exerciseName": "Bench Press",
                      "muscleGroup": "Borst",
                      "equipment": "Halterstang",
                      "targetSets": 3,
                      "repRange": "6-10",
                      "restSeconds": 120,
                      "existingExerciseId": 7
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val routine = parseGeneratedRoutine(json, fallback)

        assertEquals(7L, routine.days.first().exercises.first().existingExerciseId)
    }

    @Test
    fun parseGeneratedRoutine_capsDaysAndExercisesBeforePreview() {
        val fallback = fallbackGeneratedRoutine(
            goal = "Spiermassa",
            targetFocus = "Full body",
            daysPerWeek = 3,
            equipment = "Barbell",
            experienceLevel = "intermediate",
            sessionDurationMinutes = 60,
            includeDeload = false,
        )
        val exercises = (1..15).joinToString(",") { index ->
            """
                {
                  "exerciseName": "Oefening $index",
                  "muscleGroup": "Borst",
                  "equipment": "Halterstang",
                  "targetSets": 4,
                  "repRange": "6-10",
                  "restSeconds": 120,
                  "coachingCue": "Houd de uitvoering gecontroleerd."
                }
            """.trimIndent()
        }
        val days = (1..10).joinToString(",") { index ->
            """
                {
                  "dayName": "Trainingsdag $index",
                  "estimatedDurationMinutes": 60,
                  "exercises": [$exercises]
                }
            """.trimIndent()
        }
        val json = """
            {
              "routineName": "Krachtblok met beheerste progressie",
              "routineDescription": "Nederlandse routine voor spiermassa met rustige opbouw.",
              "periodizationNote": "Verhoog alleen als herstel en techniek stabiel blijven.",
              "estimatedDurationMinutes": 60,
              "days": [$days]
            }
        """.trimIndent()

        val routine = parseGeneratedRoutine(json, fallback)

        assertEquals(GeneratedRoutineSource.GEMINI_2_5_FLASH, routine.source)
        assertEquals(7, routine.days.size)
        assertEquals(12, routine.days.first().exercises.size)
        assertEquals("Trainingsdag 7", routine.days.last().dayName)
        assertEquals("Oefening 12", routine.days.first().exercises.last().exerciseName)
    }

    @Test
    fun parseGeneratedRoutine_withMalformedJson_returnsDeterministicFallback() {
        val fallback = fallbackGeneratedRoutine(
            goal = "Recomp",
            targetFocus = "Push/Pull",
            daysPerWeek = 5,
            equipment = "Mixed",
            experienceLevel = "advanced",
            sessionDurationMinutes = 90,
            includeDeload = true,
        )

        val routine = parseGeneratedRoutine("{ not valid json", fallback)

        assertEquals(fallback, routine)
        assertNotNull(routine.days.firstOrNull())
    }

    @Test
    fun parseGeneratedRoutine_withEnglishJson_returnsDutchFallback() {
        val fallback = fallbackGeneratedRoutine(
            goal = "Spiermassa",
            targetFocus = "Upper/lower",
            daysPerWeek = 3,
            equipment = "Halterstang",
            experienceLevel = "intermediate",
            sessionDurationMinutes = 60,
            includeDeload = false,
        )
        val json = """
            {
              "routineName": "Strength Routine",
              "routineDescription": "Build muscle with progressive overload.",
              "periodizationNote": "Add weight weekly when recovery is good.",
              "days": [
                {
                  "dayName": "Upper Day",
                  "exercises": [
                    {
                      "exerciseName": "Bench Press",
                      "muscleGroup": "Chest",
                      "equipment": "Barbell",
                      "targetSets": 3,
                      "repRange": "8-10",
                      "restSeconds": 120,
                      "coachingCue": "Keep shoulders down."
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val routine = parseGeneratedRoutine(json, fallback)

        assertEquals(fallback, routine)
    }

    @Test
    fun fallbackGeneratedRoutine_usesSessionDurationAndIncludeDeloadFlag() {
        val routine = fallbackGeneratedRoutine(
            goal = "Strength",
            targetFocus = "Lower body",
            daysPerWeek = 2,
            equipment = "Barbell",
            experienceLevel = "advanced",
            sessionDurationMinutes = 120,
            includeDeload = true,
        )

        assertEquals("Gevorderde onderlichaam routine", routine.routineName)
        assertTrue(routine.estimatedDurationMinutes <= 90)
        assertEquals(estimatedRoutineDurationMinutes(routine.days.first().exercises), routine.days.first().estimatedDurationMinutes)
        assertEquals("Gevorderd blok: golvende belasting op RPE 7-9 met elke vierde week een deload.", routine.periodizationNote)
        assertEquals(2, routine.days.size)
        assertTrue(routine.days.first().exercises.all { it.muscleGroup in setOf("Benen", "Hamstrings", "Bilspieren") })
    }

    @Test
    fun fallbackGeneratedRoutine_keepsTypedLocalReason() {
        val routine = fallbackGeneratedRoutine(
            goal = "Kracht",
            targetFocus = "Full body",
            daysPerWeek = 3,
            equipment = "Barbell",
            experienceLevel = "intermediate",
            sessionDurationMinutes = 60,
            includeDeload = false,
            fallbackContext = AiFallbackContext.NO_DECRYPTABLE_KEY,
        )

        assertEquals(AiFallbackContext.NO_DECRYPTABLE_KEY, routine.fallbackContext)
    }

    @Test
    fun fallbackGeneratedRoutine_supportsBeginnerIntermediateAndAdvancedLevels() {
        val beginner = fallbackGeneratedRoutine("Muscle", "Full body", 3, "Dumbbell", "beginner", 45, false)
        val intermediate = fallbackGeneratedRoutine("Recomp", "Upper/Lower", 4, "Barbell", "intermediate", 60, true)
        val advanced = fallbackGeneratedRoutine("Strength", "Push/Pull", 5, "Mixed", "advanced", 90, true)

        assertEquals("8-12", beginner.days.first().exercises.first().repRange)
        assertEquals("6-10", intermediate.days.first().exercises.first().repRange)
        assertEquals("4-6", advanced.days.first().exercises.first().repRange)
        assertTrue(beginner.estimatedDurationMinutes in 1..45)
        assertTrue(intermediate.estimatedDurationMinutes in 1..60)
        assertTrue(advanced.estimatedDurationMinutes in 1..90)
    }

    @Test
    fun fallbackGeneratedRoutine_usesDutchExperienceLabels() {
        val routine = fallbackGeneratedRoutine(
            goal = "spiermassa opbouwen",
            targetFocus = "onderlichaam",
            daysPerWeek = 3,
            equipment = "halterstang",
            experienceLevel = "intermediate",
            sessionDurationMinutes = 60,
            includeDeload = false,
        )

        assertEquals("Gemiddelde onderlichaam routine", routine.routineName)
    }

    @Test
    fun fallbackGeneratedRoutine_normalizesSingularDumbbellEquipment() {
        val routine = fallbackGeneratedRoutine(
            goal = "kracht",
            targetFocus = "onderlichaam",
            daysPerWeek = 2,
            equipment = "Dumbbell",
            experienceLevel = "beginner",
            sessionDurationMinutes = 45,
            includeDeload = false,
        )

        assertEquals("Dumbbells", routine.days.first().exercises.first().equipment)
    }

    @Test
    fun fallbackGeneratedRoutine_doesNotExposeEnglishProfileFocusInTitleOrDescription() {
        val routine = fallbackGeneratedRoutine(
            goal = "90 kg worden",
            targetFocus = "strength training with progressive overload, complemented by cardiovascular exercise.",
            daysPerWeek = 3,
            equipment = "barbell",
            experienceLevel = "intermediate",
            sessionDurationMinutes = 60,
            includeDeload = true,
        )

        assertEquals("Gemiddelde kracht routine", routine.routineName)
        assertEquals("Lokale routine voor 90 kg worden met 3 sessies per week van ongeveer ${routine.estimatedDurationMinutes} minuten.", routine.routineDescription)
    }
}
