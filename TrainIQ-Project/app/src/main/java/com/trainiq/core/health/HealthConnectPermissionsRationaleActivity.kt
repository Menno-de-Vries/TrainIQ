package com.trainiq.core.health

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.health.connect.client.PermissionController
import com.trainiq.MainActivity
import com.trainiq.core.theme.TrainIqTheme
import com.trainiq.core.theme.spacing
import com.trainiq.core.ui.MessageCard

class HealthConnectPermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TrainIqTheme {
                var statusMessage by rememberSaveable { mutableStateOf<String?>(null) }
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = PermissionController.createRequestPermissionResultContract(),
                ) { grantedPermissions ->
                    if (grantedPermissions.containsAll(HealthConnectReadPermissions)) {
                        openTrainIq()
                    } else {
                        statusMessage = "TrainIQ needs all five Health Connect signals together so the AI coach can combine workload, recovery, energy balance, and progress trends reliably."
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(MaterialTheme.spacing.large),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                        ) {
                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(MaterialTheme.spacing.large),
                                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                                ) {
                                    Text(
                                        "Why TrainIQ needs Health Connect",
                                        style = MaterialTheme.typography.headlineSmall,
                                    )
                                    Text(
                                        "TrainIQ reads five signals to coach like a senior strength and longevity specialist. Each permission explains a different part of your physiology, so the app can avoid generic advice and keep the dashboard honest.",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }

                            if (statusMessage != null) {
                                MessageCard(
                                    message = statusMessage.orEmpty(),
                                    onDismiss = { statusMessage = null },
                                )
                            }

                            PermissionReason.entries.forEach { reason ->
                                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier.padding(MaterialTheme.spacing.medium),
                                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
                                    ) {
                                        Text(
                                            text = reason.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = reason.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                }
                            }

                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(MaterialTheme.spacing.large),
                                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                                ) {
                                    Text(
                                        "Connect TrainIQ",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        "Health Connect keeps the permission switch in one private place. TrainIQ only asks for read access to power coaching, sync the dashboard, and write better AI summaries.",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            statusMessage = null
                                            permissionLauncher.launch(HealthConnectReadPermissions)
                                        },
                                    ) {
                                        Text("Grant Health Connect access")
                                    }
                                    OutlinedButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = ::openTrainIq,
                                    ) {
                                        Text("Continue to TrainIQ")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun openTrainIq() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }
}

private enum class PermissionReason(val title: String, val description: String) {
    Steps(
        title = "Steps",
        description = "Steps show movement volume and consistency, which lets TrainIQ separate a truly active week from a week where you only trained hard for short windows.",
    ),
    HeartRate(
        title = "Heart rate",
        description = "Heart rate gives the coach an intensity and recovery signal, so recommendations can reflect stress, conditioning, and fatigue instead of guessing.",
    ),
    Sleep(
        title = "Sleep",
        description = "Sleep tells TrainIQ how recovered you are. It keeps readiness, deload signals, and next-session advice grounded in actual recovery quality.",
    ),
    Calories(
        title = "Calories burned",
        description = "Calories burned provide energy-expenditure context, which helps the AI explain whether training output and nutrition intake are aligned.",
    ),
    Weight(
        title = "Weight",
        description = "Weight trends anchor progress. TrainIQ uses them to connect performance and nutrition patterns to real body-direction changes over time.",
    ),
}
