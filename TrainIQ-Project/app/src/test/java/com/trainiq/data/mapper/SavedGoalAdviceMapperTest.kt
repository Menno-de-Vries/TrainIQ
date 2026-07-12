package com.trainiq.data.mapper

import com.trainiq.core.database.SavedGoalAdviceEntity
import com.trainiq.domain.model.GoalAdviceSource
import org.junit.Assert.assertEquals
import org.junit.Test

class SavedGoalAdviceMapperTest {

    @Test
    fun savedGoalAdviceEntity_toDomain_preservesDisplayedAdviceAndSnapshotMetadata() {
        val entity = SavedGoalAdviceEntity(
            id = 1L,
            profileFingerprint = "profile-v1",
            savedAt = 1_725_000_000_000L,
            bmr = 1_820,
            maintenanceCalories = 2_750,
            activityMultiplier = 1.55,
            calorieTarget = 2_450,
            proteinTarget = 180,
            carbsTarget = 260,
            fatTarget = 75,
            trainingFocus = "Progressieve overload",
            summary = "Sterke basis.",
            calorieAdvice = "Kies een klein tekort.",
            macroAdvice = "Houd eiwit hoog.",
            activityExplanation = "Gemiddeld actief.",
            attentionPointsJson = "[\"Slaap\",\"Stappen\"]",
            advice = "Train vier keer per week.",
            dataQuality = "Profiel compleet.",
            source = GoalAdviceSource.GEMINI_2_5_FLASH.name,
            rawResponse = "{\"summary\":\"Sterke basis.\"}",
        )

        val savedAdvice = entity.toDomain()

        assertEquals(entity.profileFingerprint, savedAdvice.profileFingerprint)
        assertEquals(entity.savedAt, savedAdvice.savedAt)
        assertEquals(entity.summary, savedAdvice.advice.summary)
        assertEquals(listOf("Slaap", "Stappen"), savedAdvice.advice.attentionPoints)
        assertEquals(GoalAdviceSource.GEMINI_2_5_FLASH, savedAdvice.advice.source)
        assertEquals(entity.rawResponse, savedAdvice.advice.rawResponse)
    }
}
