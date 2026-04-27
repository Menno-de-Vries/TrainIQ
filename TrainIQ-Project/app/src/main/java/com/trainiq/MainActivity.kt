package com.trainiq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trainiq.core.diagnostics.DiagnosticsTracker
import com.trainiq.core.diagnostics.PerformanceSessionMonitor
import com.trainiq.core.theme.TrainIqTheme
import com.trainiq.navigation.TrainIqApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var performanceSessionMonitor: PerformanceSessionMonitor
    @Inject lateinit var diagnosticsTracker: DiagnosticsTracker

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            TrainIqTheme(themeMode = themeMode) {
                TrainIqApp(diagnosticsTracker = diagnosticsTracker)
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
