package com.trainiq.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AdaptiveRouteViewportInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun expandedRouteIsCenteredWithinTheSpaceAfterTheNavigationRail() {
        var densityScale = 1f
        var availableLeftPx = Float.NaN
        var availableWidthPx = Float.NaN
        var viewportLeftPx = Float.NaN
        var viewportWidthPx = Float.NaN
        var routeLeftPx = Float.NaN
        var routeWidthPx = Float.NaN
        var routeRightPx = Float.NaN

        compose.setContent {
            densityScale = LocalDensity.current.density
            Box(
                modifier = Modifier
                    .requiredSize(width = 1348.dp, height = 800.dp)
                    .onGloballyPositioned {
                        availableLeftPx = it.positionInRoot().x
                        availableWidthPx = it.size.width.toFloat()
                    },
            ) {
                AdaptiveRouteViewport(
                    widthClass = TrainIqWindowWidthClass.Expanded,
                    modifier = Modifier.onGloballyPositioned {
                        viewportLeftPx = it.positionInRoot().x
                        viewportWidthPx = it.size.width.toFloat()
                    },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned {
                                routeLeftPx = it.positionInRoot().x
                                routeWidthPx = it.size.width.toFloat()
                                routeRightPx = it.positionInRoot().x + it.size.width
                            },
                    )
                }
            }
        }

        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(1348f, availableWidthPx / densityScale, 0.5f)
            assertEquals(availableLeftPx / densityScale, viewportLeftPx / densityScale, 0.5f)
            assertEquals(1348f, viewportWidthPx / densityScale, 0.5f)
            assertEquals(114f, (routeLeftPx - availableLeftPx) / densityScale, 0.5f)
            assertEquals(1120f, routeWidthPx / densityScale, 0.5f)
            assertEquals(114f, (availableLeftPx + availableWidthPx - routeRightPx) / densityScale, 0.5f)
        }
    }
}
