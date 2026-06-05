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
import androidx.compose.runtime.Composable
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
                    val message = healthConnectPermissionResultMessage(grantedPermissions)
                    if (message == null) {
                        openTrainIq()
                    } else {
                        statusMessage = message
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    HealthConnectPermissionsRationaleContent(
                        statusMessage = statusMessage,
                        onDismissStatus = { statusMessage = null },
                        onRequestPermission = {
                            statusMessage = null
                            permissionLauncher.launch(HealthConnectReadPermissions)
                        },
                        onContinue = ::openTrainIq,
                    )
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

@Composable
internal fun HealthConnectPermissionsRationaleContent(
    statusMessage: String?,
    onDismissStatus: () -> Unit,
    onRequestPermission: () -> Unit,
    onContinue: () -> Unit,
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
                        "Waarom TrainIQ Health Connect gebruikt",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        "TrainIQ leest vijf signalen om training, herstel en voeding beter te duiden. Elke toestemming verklaart een ander deel van je belasting en herstel, zodat het dashboard niet doet alsof ontbrekende data bekend is.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (statusMessage != null) {
                MessageCard(
                    message = statusMessage,
                    onDismiss = onDismissStatus,
                )
            }

            HealthConnectRationaleReasons.forEach { reason ->
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
                        "TrainIQ verbinden",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Health Connect beheert toestemmingen op een centrale plek. TrainIQ vraagt alleen leestoegang om het dashboard te synchroniseren en coaching beter te onderbouwen.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "Achtergrondsync wordt alleen ingepland als Android en Health Connect die aparte achtergrondtoegang beschikbaar maken en jij die toegang in Health Connect hebt toegestaan.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onRequestPermission,
                    ) {
                        Text("Health Connect-toegang geven")
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onContinue,
                    ) {
                        Text("Doorgaan naar TrainIQ")
                    }
                }
            }
        }
    }
}
