package com.trainiq.features.nutrition

import com.trainiq.domain.repository.UnavailableMealItemException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class MealSaveTest {
    @Test fun storageFailureKeepsDraftAndSuccessfulRetryClosesIt() = runTest {
        var draftOpen = true
        var finished = 0
        val messages = mutableListOf<String>()
        performMealSave({ throw IOException("private disk detail") }, { draftOpen = false }, messages::add, { finished++ })
        assertTrue(draftOpen)
        assertEquals(1, finished)
        assertTrue(messages.single().contains("Probeer opnieuw"))
        assertFalse(messages.single().contains("verwijderd"))
        assertFalse(messages.single().contains("private"))
        performMealSave({}, { draftOpen = false }, messages::add, { finished++ })
        assertFalse(draftOpen)
        assertEquals(2, finished)
        assertEquals("Maaltijd opgeslagen.", messages.last())
    }

    @Test fun missingReferenceOffersDraftRepairOnlyForTypedFailure() = runTest {
        var saved = false
        var text = ""
        performMealSave({ throw UnavailableMealItemException() }, { saved = true }, { text = it }, {})
        assertFalse(saved)
        assertTrue(text.contains("Verwijder het item uit je concept"))
        performMealSave({ throw com.trainiq.domain.repository.InvalidMealItemException() }, { saved = true }, { text = it }, {})
        assertFalse(saved)
        assertTrue(text.contains("Controleer de producten"))
        assertFalse(text.contains("verwijderd"))
    }

    @Test fun cancellationReleasesPendingGuardWithoutFailureOrSuccessFeedback() = runTest {
        var finished = false
        var called = false
        try {
            performMealSave({ throw CancellationException("closed") }, { called = true }, { called = true }, { finished = true })
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
        assertTrue(finished)
        assertFalse(called)
    }
}
