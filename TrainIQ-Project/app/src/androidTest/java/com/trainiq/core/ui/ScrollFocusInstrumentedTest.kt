package com.trainiq.core.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import com.trainiq.core.theme.TrainIqTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ScrollFocusInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun childTapPreservesFocusAndConsumedScrollClearsItWithoutBlockingScroll() {
        var clicks = 0
        lateinit var scroll: ScrollState
        compose.setContent {
            TrainIqTheme {
                Column(Modifier.fillMaxSize().clearFocusOnScrollOrDrag()) {
                    var value by remember { mutableStateOf("100") }
                    OutlinedTextField(value, { value = it }, Modifier.testTag("input"))
                    Button(onClick = { clicks++ }, modifier = Modifier.testTag("button")) { Text("Toevoegen") }
                    scroll = rememberScrollState()
                    Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(scroll).testTag("scroll")) {
                        Spacer(Modifier.height(1200.dp))
                    }
                }
            }
        }
        compose.onNodeWithTag("input").performClick().assertIsFocused()
        Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("button").performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, clicks) }
        compose.onNodeWithTag("input").assertIsFocused()
        compose.onNodeWithTag("scroll").performTouchInput { swipeUp() }
        compose.onNodeWithTag("input").assertIsNotFocused()
        compose.runOnIdle { assertTrue("The child must still receive the scroll gesture", scroll.value > 0) }
    }
}
