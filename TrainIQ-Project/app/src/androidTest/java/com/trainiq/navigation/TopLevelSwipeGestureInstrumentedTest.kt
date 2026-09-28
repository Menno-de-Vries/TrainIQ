package com.trainiq.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TopLevelSwipeGestureInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun shortAndVerticalMovesDoNotSwitchButAdjacentSwipesAndFlicksDo() {
        val directions = mutableListOf<Int>()
        compose.setContent {
            Box(Modifier.size(320.dp, 240.dp).topLevelTabSwipeNavigation(2, 5) { directions += it }.testTag("surface"))
        }
        compose.onNodeWithTag("surface").performTouchInput {
            swipe(Offset(width * .55f, height * .5f), Offset(width * .48f, height * .5f))
            swipe(Offset(width * .65f, height * .2f), Offset(width * .4f, height * .8f))
            swipe(Offset(width * .8f, height * .5f), Offset(width * .2f, height * .5f))
            swipe(Offset(width * .2f, height * .5f), Offset(width * .8f, height * .5f), durationMillis = 80)
        }
        compose.runOnIdle { assertEquals(listOf(1, -1), directions) }
    }

    @Test fun systemEdgeAndHorizontalChildKeepTheirGestures() {
        val directions = mutableListOf<Int>()
        compose.setContent {
            Column(Modifier.size(320.dp, 240.dp).topLevelTabSwipeNavigation(2, 5) { directions += it }.testTag("surface")) {
                Box(Modifier.fillMaxWidth().height(100.dp).horizontalScroll(rememberScrollState()).testTag("horizontal")) {
                    Box(Modifier.size(800.dp, 90.dp))
                }
            }
        }
        compose.onNodeWithTag("surface").performTouchInput {
            swipe(Offset(width * .02f, height * .8f), Offset(width * .8f, height * .8f))
        }
        compose.onNodeWithTag("horizontal").performTouchInput {
            swipe(Offset(width * .8f, height * .5f), Offset(width * .2f, height * .5f))
        }
        compose.runOnIdle { assertEquals(emptyList<Int>(), directions) }
    }

    @Test fun tabSwipeDoesNotTriggerChildClick() {
        val directions = mutableListOf<Int>()
        var clicks = 0
        compose.setContent {
            Box(Modifier.size(320.dp, 240.dp).topLevelTabSwipeNavigation(2, 5) { directions += it }.testTag("surface")) {
                Box(Modifier.fillMaxSize().clickable { clicks++ }.testTag("button"))
            }
        }
        compose.onNodeWithTag("surface").performTouchInput {
            swipe(Offset(width * .8f, height * .5f), Offset(width * .2f, height * .5f))
        }
        compose.runOnIdle {
            assertEquals(listOf(1), directions)
            assertEquals(0, clicks)
        }
    }

    @Test fun verticalChildScrollKeepsOwnership() {
        val directions = mutableListOf<Int>()
        compose.setContent {
            Column(Modifier.size(320.dp, 240.dp).topLevelTabSwipeNavigation(2, 5) { directions += it }
                .verticalScroll(rememberScrollState()).testTag("vertical")) {
                Box(Modifier.size(320.dp, 900.dp))
            }
        }
        compose.onNodeWithTag("vertical").performTouchInput {
            swipe(Offset(width * .65f, height * .2f), Offset(width * .4f, height * .8f))
        }
        compose.runOnIdle { assertEquals(emptyList<Int>(), directions) }
    }
}
