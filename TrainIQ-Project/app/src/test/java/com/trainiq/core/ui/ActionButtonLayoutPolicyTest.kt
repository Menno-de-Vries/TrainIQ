package com.trainiq.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ActionButtonLayoutPolicyTest {
    @Test
    fun longDutchLabelsStackOnCompactWidths() {
        assertEquals(
            ActionButtonLayout.Stacked,
            actionButtonLayoutForWidth(
                widthDp = 320,
                labels = listOf("Vorige oefening", "Werkset opslaan en doorgaan"),
            ),
        )
    }

    @Test
    fun shortLabelsCanStayInlineOnCompactWidths() {
        assertEquals(
            ActionButtonLayout.Inline,
            actionButtonLayoutForWidth(
                widthDp = 320,
                labels = listOf("Vorige", "Opslaan"),
            ),
        )
    }

    @Test
    fun routineEditLabelsStackOnMediumPhoneWidths() {
        assertEquals(
            ActionButtonLayout.Stacked,
            actionButtonLayoutForWidth(
                widthDp = 390,
                labels = listOf("Start", "Routine aanpassen"),
            ),
        )
    }

    @Test
    fun longDutchLabelsCanStayInlineOnWideWidths() {
        assertEquals(
            ActionButtonLayout.Inline,
            actionButtonLayoutForWidth(
                widthDp = 600,
                labels = listOf("Vorige oefening", "Werkset opslaan en doorgaan"),
            ),
        )
    }
}
