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
                        statusMessage = "TrainIQ heeft alle vijf Health Connect-signalen samen nodig om belasting, herstel, energiebalans en voortgang eerlijk te combineren."
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
                                        "TrainIQ verbinden",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        "Health Connect beheert toestemmingen op een centrale plek. TrainIQ vraagt alleen leestoegang om het dashboard te synchroniseren en coaching beter te onderbouwen.",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            statusMessage = null
                                            permissionLauncher.launch(HealthConnectReadPermissions)
                                        },
                                    ) {
                                        Text("Health Connect-toegang geven")
                                    }
                                    OutlinedButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = ::openTrainIq,
                                    ) {
                                        Text("Doorgaan naar TrainIQ")
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
        title = "Stappen",
        description = "Stappen tonen bewegingsvolume en consistentie, zodat TrainIQ een actieve week kan onderscheiden van alleen korte zware trainingsmomenten.",
    ),
    HeartRate(
        title = "Hartslag",
        description = "Hartslag geeft een signaal voor intensiteit en herstel, zodat adviezen minder hoeven te gokken naar stress, conditie en vermoeidheid.",
    ),
    Sleep(
        title = "Slaap",
        description = "Slaap helpt TrainIQ inschatten hoe hersteld je bent en maakt readiness, deload-signalen en sessieadvies concreter.",
    ),
    Calories(
        title = "Verbrande calorieen",
        description = "Verbrande calorieen geven context voor energieverbruik en helpen training en voeding realistischer naast elkaar te zetten.",
    ),
    Weight(
        title = "Gewicht",
        description = "Gewichtstrends geven voortgang context. TrainIQ gebruikt ze om prestaties en voeding te verbinden aan echte lichaamsverandering.",
    ),
}
