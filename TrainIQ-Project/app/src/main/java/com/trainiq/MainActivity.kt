package com.trainiq

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trainiq.core.diagnostics.DiagnosticsTracker
import com.trainiq.core.diagnostics.PerformanceSessionMonitor
import com.trainiq.core.diagnostics.TelemetryExporter
import com.trainiq.core.health.HealthConnectBackgroundSyncScheduler
import com.trainiq.core.reminders.TrainIqReminderScheduler
import com.trainiq.core.theme.TrainIqTheme
import com.trainiq.navigation.TrainIqWindowWidthClass
import com.trainiq.navigation.TrainIqApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var performanceSessionMonitor: PerformanceSessionMonitor
    @Inject lateinit var diagnosticsTracker: DiagnosticsTracker
    @Inject lateinit var healthConnectBackgroundSyncScheduler: HealthConnectBackgroundSyncScheduler
    @Inject lateinit var reminderScheduler: TrainIqReminderScheduler
    @Inject lateinit var telemetryExporter: TelemetryExporter

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val telemetryOptIn by viewModel.telemetryOptIn.collectAsStateWithLifecycle()
            val onboardingState by viewModel.onboardingState.collectAsStateWithLifecycle()
            val windowSizeClass = calculateWindowSizeClass(this)
            LaunchedEffect(telemetryOptIn) {
                telemetryExporter.setUserOptIn(telemetryOptIn)
            }
            TrainIqTheme(themeMode = themeMode) {
                when (val onboardingState = onboardingState) {
                    MainOnboardingState.Loading -> TrainIqStartupGate()
                    is MainOnboardingState.Ready -> TrainIqApp(
                        diagnosticsTracker = diagnosticsTracker,
                        windowWidthClass = windowSizeClass.widthSizeClass.toTrainIqWidthClass(),
                        onboardingPreferences = onboardingState.preferences,
                        markGuidedTourCompleted = viewModel::markGuidedTourCompleted,
                        markGuidedTourSkipped = viewModel::markGuidedTourSkipped,
                    )
                }
            }
        }
        window.decorView.postDelayed({
            performanceSessionMonitor.start(this)
            lifecycleScope.launch(Dispatchers.IO) {
                healthConnectBackgroundSyncScheduler.scheduleIfBackgroundReadAvailable()
                reminderScheduler.syncScheduleWithPreferences()
            }
        }, StartupDiagnosticsDelayMillis)
    }

    override fun onResume() {
        super.onResume()
        performanceSessionMonitor.setEnabled(true)
    }

    override fun onPause() {
        performanceSessionMonitor.setEnabled(false)
        super.onPause()
    }

    override fun onStop() {
        lifecycleScope.launch(Dispatchers.IO) {
            telemetryExporter.flush()
        }
        super.onStop()
    }

    override fun onDestroy() {
        performanceSessionMonitor.stop()
        super.onDestroy()
    }
}

@Composable
private fun TrainIqStartupGate() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "TrainIQ laden",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

private fun WindowWidthSizeClass.toTrainIqWidthClass(): TrainIqWindowWidthClass = when (this) {
    WindowWidthSizeClass.Compact -> TrainIqWindowWidthClass.Compact
    WindowWidthSizeClass.Medium -> TrainIqWindowWidthClass.Medium
    WindowWidthSizeClass.Expanded -> TrainIqWindowWidthClass.Expanded
    else -> TrainIqWindowWidthClass.Compact
}

private const val StartupDiagnosticsDelayMillis = 8_000L
