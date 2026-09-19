package com.trainiq.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.trainiq.core.theme.TrainIqTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TapOutsideFocusInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun childButtonReceivesFirstTapWithoutParentDismissingInputMidGesture() {
        var additions = 0
        compose.setContent {
            TrainIqTheme {
                Column(Modifier.fillMaxSize().clearFocusOnTapOutside()) {
                    var value by remember { mutableStateOf("100") }
                    OutlinedTextField(value, { value = it }, Modifier.testTag("grams"))
                    Button(onClick = { additions++ }, modifier = Modifier.testTag("add")) { Text("Toevoegen") }
                    Box(Modifier.fillMaxWidth().height(100.dp).testTag("background"))
                }
            }
        }
        compose.onNodeWithTag("grams").performClick().assertIsFocused()
        compose.onNodeWithTag("add").performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, additions) }
        // The button owns this gesture. Dismissing IME here can move it before pointer-up.
        compose.onNodeWithTag("grams").assertIsFocused()
        compose.onNodeWithTag("add").performTouchInput { click() }
        compose.runOnIdle { assertEquals(2, additions) }
        compose.onNodeWithTag("background").performTouchInput { click() }
        compose.onNodeWithTag("grams").assertIsNotFocused()
    }
}
