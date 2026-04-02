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
              "routineName": "Upper Power",
              "routineDescription": "Heavy upper-body focus",
              "periodizationNote": "Wave loading with a planned deload.",
              "estimatedDurationMinutes": 75,
              "days": [
                {
                  "dayName": "Upper A",
                  "exercises": [
                    {
                      "exerciseName": "Bench Press",
                      "muscleGroup": "Chest",
                      "equipment": "Barbell",
                      "targetSets": 4,
                      "repRange": "4-6",
                      "restSeconds": 150,
                      "coachingCue": "Keep the bar path steady."
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val routine = parseGeneratedRoutine(json, fallback)

        assertEquals("Upper Power", routine.routineName)
        assertEquals("Heavy upper-body focus", routine.routineDescription)
        assertEquals("Wave loading with a planned deload.", routine.periodizationNote)
        assertEquals(75, routine.estimatedDurationMinutes)
        assertEquals(1, routine.days.size)
        assertEquals("Upper A", routine.days.first().dayName)
        assertEquals(75, routine.days.first().estimatedDurationMinutes)
        assertEquals("Bench Press", routine.days.first().exercises.first().exerciseName)
        assertEquals("Keep the bar path steady.", routine.days.first().exercises.first().coachingCue)
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

        assertEquals("Advanced Lower body Routine", routine.routineName)
        assertEquals(90, routine.estimatedDurationMinutes)
        assertEquals("Advanced block: undulating loading at RPE 7-9 with a deload every 4th week.", routine.periodizationNote)
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
