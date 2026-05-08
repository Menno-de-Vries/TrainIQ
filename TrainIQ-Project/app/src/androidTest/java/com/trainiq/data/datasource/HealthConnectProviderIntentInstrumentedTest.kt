package com.trainiq.data.datasource

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthConnectProviderIntentInstrumentedTest {
    @Test
    fun providerInstallIntentUsesHealthConnectOnboardingPlayStoreOverlay() {
        val intent = healthConnectProviderInstallIntent("com.trainiq")

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("com.android.vending", intent.`package`)
        assertEquals("com.trainiq", intent.getStringExtra("callerId"))
        assertEquals(true, intent.getBooleanExtra("overlay", false))
        assertTrue(intent.dataString.orEmpty().contains("com.google.android.apps.healthdata"))
        assertTrue(intent.dataString.orEmpty().contains("healthconnect%3A%2F%2Fonboarding"))
    }
}
