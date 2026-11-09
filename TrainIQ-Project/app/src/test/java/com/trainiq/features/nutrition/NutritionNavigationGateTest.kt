package com.trainiq.features.nutrition

import org.junit.Assert.*
import org.junit.Test

class NutritionNavigationGateTest {
    @Test fun busyBackAndTopNavigationWaitButCurrentTabIsNotDeparture() {
        val gate = NutritionNavigationGate()
        var busy = true
        val lease = gate.register("actual-entry", "Nutrition") { busy }
        var navigations = 0
        var notices = 0
        gate.update(lease, AiNavigationState(), {}, { notices++ })
        assertFalse(gate.request("back") { navigations++ })
        assertFalse(gate.request("Train") { navigations++ })
        assertTrue(gate.request("Nutrition") { navigations++ })
        assertEquals(1, navigations)
        assertEquals(2, notices)
        busy = false
        assertTrue(gate.request("Train") { navigations++ })
        assertEquals(2, navigations)
    }

    @Test fun reviewDepartureRequiresExplicitChoiceAndOldDisposalCannotClearNewOwner() {
        val gate = NutritionNavigationGate()
        val old = gate.register("Nutrition entry", "Nutrition")
        val current = gate.register("MealDetail entry", "MealDetail")
        var discarded = 0
        var navigations = 0
        gate.update(current, AiNavigationState(hasReview = true), { discarded++ }, {})
        gate.unregister(old)
        assertFalse(gate.request("back") { navigations++ })
        assertTrue(gate.confirmLeave.value)
        gate.stay()
        assertEquals(0, discarded)
        assertEquals(0, navigations)
        assertFalse(gate.request("Nutrition") { navigations++ })
        assertTrue(gate.leave())
        assertFalse(gate.leave())
        assertEquals(1, discarded)
        assertEquals(1, navigations)
    }

    @Test fun staleOwnerAndBusyTransitionCannotDiscardOrRunOldAction() {
        val gate = NutritionNavigationGate()
        val old = gate.register("detail", "MealDetail")
        var navigations = 0
        gate.update(old, AiNavigationState(hasReview = true), {}, {})
        gate.request("Nutrition") { navigations++ }
        val fresh = gate.register("detail", "MealDetail")
        gate.update(fresh, AiNavigationState(busy = true), {}, {})
        gate.unregister(old)
        assertFalse(gate.leave())
        assertFalse(gate.request("back") { navigations++ })
        assertEquals(0, navigations)
        gate.update(fresh, AiNavigationState(), {}, {})
        assertTrue(gate.request("back") { navigations++ })
    }
}
