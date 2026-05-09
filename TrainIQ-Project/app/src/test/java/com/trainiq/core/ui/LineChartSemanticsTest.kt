package com.trainiq.core.ui

import com.trainiq.domain.model.ChartPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class LineChartSemanticsTest {
    @Test
    fun lineChartContentDescription_whenNoPoints_reportsNoData() {
        assertEquals(
            "Grafiek zonder data.",
            lineChartContentDescription(emptyList()),
        )
    }

    @Test
    fun lineChartContentDescription_summarizesLatestRangeAndTrend() {
        val points = listOf(
            ChartPoint("Week 1", 80.0),
            ChartPoint("Week 2", 92.5),
            ChartPoint("Week 3", 100.0),
        )

        assertEquals(
            "Volume met 3 datapunten. Laatste: Week 3 100 kg. Bereik: 80 tot 100 kg. Trend: stijgend.",
            lineChartContentDescription(points, chartName = "Volume", valueSuffix = "kg"),
        )
    }

    @Test
    fun lineChartContentDescription_reportsFallingTrend() {
        val points = listOf(
            ChartPoint("Start", 71.0),
            ChartPoint("Vandaag", 70.0),
        )

        assertEquals(
            "Gewicht met 2 datapunten. Laatste: Vandaag 70 kg. Bereik: 70 tot 71 kg. Trend: dalend.",
            lineChartContentDescription(points, chartName = "Gewicht", valueSuffix = "kg"),
        )
    }
}
