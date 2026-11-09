package com.trainiq.domain.repository

import com.trainiq.domain.model.UserProfile

internal const val MissingProfileForAiRoutineMessage = "Vul eerst je profiel in voordat je een AI-routine maakt."

class MissingProfileForAiRoutineException : IllegalStateException(
    MissingProfileForAiRoutineMessage,
)

internal fun requireRoutineGenerationProfile(profile: UserProfile?): UserProfile =
    profile ?: throw MissingProfileForAiRoutineException()
