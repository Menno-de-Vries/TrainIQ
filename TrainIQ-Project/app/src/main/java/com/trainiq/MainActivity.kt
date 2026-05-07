package com.trainiq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trainiq.core.diagnostics.DiagnosticsTracker
import com.trainiq.core.diagnostics.PerformanceSessionMonitor
import com.trainiq.core.theme.TrainIqTheme
import com.trainiq.navigation.TrainIqWindowWidthClass
import com.trainiq.navigation.TrainIqApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var performanceSessionMonitor: PerformanceSessionMonitor
    @Inject lateinit var diagnosticsTracker: DiagnosticsTracker

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val windowSizeClass = calculateWindowSizeClass(this)
            TrainIqTheme(themeMode = themeMode) {
                TrainIqApp(
                    diagnosticsTracker = diagnosticsTracker,
                    windowWidthClass = windowSizeClass.widthSizeClass.toTrainIqWidthClass(),
                )
            }
        }
        window.decorView.post {
            performanceSessionMonitor.start(this)
        }
    }

    override fun onResume() {
        super.onResume()
        performanceSessionMonitor.setEnabled(true)
    }

    override fun onPause() {
        performanceSessionMonitor.setEnabled(false)
        super.onPause()
    }

    override fun onDestroy() {
        performanceSessionMonitor.stop()
        super.onDestroy()
    }
}

private fun WindowWidthSizeClass.toTrainIqWidthClass(): TrainIqWindowWidthClass = when (this) {
    WindowWidthSizeClass.Compact -> TrainIqWindowWidthClass.Compact
    WindowWidthSizeClass.Medium -> TrainIqWindowWidthClass.Medium
    WindowWidthSizeClass.Expanded -> TrainIqWindowWidthClass.Expanded
    else -> TrainIqWindowWidthClass.Compact
}
