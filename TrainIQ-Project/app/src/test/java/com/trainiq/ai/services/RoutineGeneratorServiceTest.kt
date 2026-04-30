package com.trainiq.ai.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RoutineGeneratorServiceTest {

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
        assertEquals(75, routine.estimatedDurationMinutes)
        assertEquals(1, routine.days.size)
        assertEquals("Upper A", routine.days.first().dayName)
        assertEquals(75, routine.days.first().estimatedDurationMinutes)
        assertEquals("Bench Press", routine.days.first().exercises.first().exerciseName)
        assertEquals("Houd het stangpad stabiel.", routine.days.first().exercises.first().coachingCue)
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
              "routineName": "Simple Plan",
              "days": [
                {
                  "dayName": "Day 1",
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

        assertEquals("Simple Plan", routine.routineName)
        assertEquals("", routine.routineDescription)
        assertEquals("", routine.periodizationNote)
        assertEquals(fallback.estimatedDurationMinutes, routine.estimatedDurationMinutes)
        assertEquals(1, routine.days.size)
        assertEquals("Squat", routine.days.first().exercises.first().exerciseName)
        assertEquals("General", routine.days.first().exercises.first().muscleGroup)
        assertEquals("", routine.days.first().exercises.first().coachingCue)
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

        assertEquals("Gevorderde Lower body routine", routine.routineName)
        assertEquals(90, routine.estimatedDurationMinutes)
        assertEquals("Gevorderd blok: golvende belasting op RPE 7-9 met elke vierde week een deload.", routine.periodizationNote)
        assertEquals(2, routine.days.size)
        assertEquals("Bench Press", routine.days.first().exercises[1].exerciseName)
    }

    @Test
    fun fallbackGeneratedRoutine_supportsBeginnerIntermediateAndAdvancedLevels() {
        val beginner = fallbackGeneratedRoutine("Muscle", "Full body", 3, "Dumbbell", "beginner", 45, false)
        val intermediate = fallbackGeneratedRoutine("Recomp", "Upper/Lower", 4, "Barbell", "intermediate", 60, true)
        val advanced = fallbackGeneratedRoutine("Strength", "Push/Pull", 5, "Mixed", "advanced", 90, true)

        assertEquals("8-12", beginner.days.first().exercises.first().repRange)
        assertEquals("6-10", intermediate.days.first().exercises.first().repRange)
        assertEquals("4-6", advanced.days.first().exercises.first().repRange)
        assertEquals(45, beginner.estimatedDurationMinutes)
        assertEquals(60, intermediate.estimatedDurationMinutes)
        assertEquals(90, advanced.estimatedDurationMinutes)
    }
}
