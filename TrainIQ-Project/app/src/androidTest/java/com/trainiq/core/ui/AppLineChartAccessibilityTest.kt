package com.trainiq.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.trainiq.domain.model.ChartPoint
import org.junit.Rule
import org.junit.Test

class AppLineChartAccessibilityTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun appLineChart_exposesSemanticSummary() {
        val points = listOf(
            ChartPoint("Ma", 10.0),
            ChartPoint("Di", 12.0),
        )

        compose.setContent {
            MaterialTheme {
                AppLineChart(points = points, modifier = Modifier)
            }
        }

        compose
            .onNodeWithContentDescription(
                "Grafiek met 2 datapunten. Laatste: Di 12. Bereik: 10 tot 12. Trend: stijgend.",
            )
            .assertExists()
    }
}
