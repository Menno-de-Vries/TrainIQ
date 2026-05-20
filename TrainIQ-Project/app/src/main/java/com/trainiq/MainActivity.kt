package com.trainiq

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trainiq.core.diagnostics.DiagnosticsTracker
import com.trainiq.core.diagnostics.PerformanceSessionMonitor
import com.trainiq.core.diagnostics.TelemetryExporter
import com.trainiq.core.health.HealthConnectBackgroundSyncScheduler
import com.trainiq.core.theme.TrainIqTheme
import com.trainiq.navigation.TrainIqWindowWidthClass
import com.trainiq.navigation.TrainIqApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var performanceSessionMonitor: PerformanceSessionMonitor
    @Inject lateinit var diagnosticsTracker: DiagnosticsTracker
    @Inject lateinit var healthConnectBackgroundSyncScheduler: HealthConnectBackgroundSyncScheduler
    @Inject lateinit var telemetryExporter: TelemetryExporter

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        hideNavigationBarUntilSwipe()
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val telemetryOptIn by viewModel.telemetryOptIn.collectAsStateWithLifecycle()
            val windowSizeClass = calculateWindowSizeClass(this)
            LaunchedEffect(telemetryOptIn) {
                telemetryExporter.setUserOptIn(telemetryOptIn)
            }
            TrainIqTheme(themeMode = themeMode) {
                TrainIqApp(
                    diagnosticsTracker = diagnosticsTracker,
                    windowWidthClass = windowSizeClass.widthSizeClass.toTrainIqWidthClass(),
                )
            }
        }
        window.decorView.post {
            performanceSessionMonitor.start(this)
            lifecycleScope.launch {
                healthConnectBackgroundSyncScheduler.scheduleIfBackgroundReadAvailable()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideNavigationBarUntilSwipe()
        performanceSessionMonitor.setEnabled(true)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideNavigationBarUntilSwipe()
    }

    override fun onPause() {
        performanceSessionMonitor.setEnabled(false)
        super.onPause()
    }

    override fun onStop() {
        telemetryExporter.flush()
        super.onStop()
    }

    override fun onDestroy() {
        performanceSessionMonitor.stop()
        super.onDestroy()
    }
}

private fun ComponentActivity.hideNavigationBarUntilSwipe() {
    WindowCompat.getInsetsController(window, window.decorView).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hide(WindowInsetsCompat.Type.navigationBars())
    }
}

private fun WindowWidthSizeClass.toTrainIqWidthClass(): TrainIqWindowWidthClass = when (this) {
    WindowWidthSizeClass.Compact -> TrainIqWindowWidthClass.Compact
    WindowWidthSizeClass.Medium -> TrainIqWindowWidthClass.Medium
    WindowWidthSizeClass.Expanded -> TrainIqWindowWidthClass.Expanded
    else -> TrainIqWindowWidthClass.Compact
}
