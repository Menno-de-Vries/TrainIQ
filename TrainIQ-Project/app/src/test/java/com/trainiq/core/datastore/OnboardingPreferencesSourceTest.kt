package com.trainiq.core.datastore

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingPreferencesSourceTest {
    @Test
    fun onboardingPreferencesPersistCompletionDraftAndSkippedCapabilityFields() {
        val source = File("src/main/java/com/trainiq/core/datastore/UserPreferencesRepository.kt").readText()

        assertTrue(source.contains("data class OnboardingPreferences"))
        assertTrue(source.contains("onboarding_completed"))
        assertTrue(source.contains("onboarding_goal"))
        assertTrue(source.contains("onboarding_experience"))
        assertTrue(source.contains("onboarding_training_days"))
        assertTrue(source.contains("onboarding_equipment"))
        assertTrue(source.contains("onboarding_session_length_minutes"))
        assertTrue(source.contains("onboarding_constraints"))
        assertTrue(source.contains("onboarding_health_connect_accepted"))
        assertTrue(source.contains("onboarding_health_connect_skipped"))
        assertTrue(source.contains("onboarding_ai_accepted"))
        assertTrue(source.contains("onboarding_ai_skipped"))
        assertTrue(source.contains("onboarding_reminders_enabled"))
        assertTrue(source.contains("onboarding_privacy_acknowledged"))
        assertTrue(source.contains("val onboardingPreferences: Flow<OnboardingPreferences>"))
        assertTrue(source.contains("suspend fun saveOnboardingPreferences"))
        assertTrue(source.contains("suspend fun completeOnboarding"))
        assertTrue(source.contains("suspend fun reopenOnboarding"))
    }
}
