package com.trainiq.features.sleep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trainiq.MainViewModel
import com.trainiq.core.theme.TrainIqTheme
import dagger.hilt.android.AndroidEntryPoint

/** Private notification entry point; reuses the same screen as the typed Settings route. */
@AndroidEntryPoint
class SleepRoutineActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val theme by viewModel.themeMode.collectAsStateWithLifecycle()
            TrainIqTheme(themeMode = theme) { SleepRoutineRoute(onBack = { finish() }) }
        }
    }
}
