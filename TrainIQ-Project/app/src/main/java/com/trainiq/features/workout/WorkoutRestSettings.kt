package com.trainiq.features.workout

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.trainiq.core.workout.RestChannelId
import com.trainiq.core.workout.WorkoutRestNotificationStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkoutRestSettingsSheet(status: WorkoutRestNotificationStatus?, soundEnabled: Boolean,
    exactAllowed: Boolean, onSoundChanged: (Boolean) -> Unit, onRefresh: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onRefresh() }
    val settings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { onRefresh() }
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Rustmelding", style = MaterialTheme.typography.headlineSmall)
            Text(status?.label(soundEnabled) ?: "Meldingsstatus laden")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Kort geluid bij afgelopen rust", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = soundEnabled, onCheckedChange = onSoundChanged,
                    modifier = Modifier.semantics { contentDescription = "Geluid bij afgelopen rust" })
            }
            Text("Je kanaalgeluid, systeemvolume en Niet storen blijven bepalend.", style = MaterialTheme.typography.bodyMedium)
            Text(if (exactAllowed) "Nauwkeurige planning toegestaan. In slaapstand kan Android de melding vertragen."
                else "Globale planning: Android kan de melding vertragen, vooral in slaapstand.", style = MaterialTheme.typography.bodyMedium)
            if (status?.appAllowed == false) Button(onClick = {
                if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else settings.launch(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            }, modifier = Modifier.fillMaxWidth()) { Text("Meldingen toestaan") }
            OutlinedButton(onClick = { settings.launch(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).putExtra(Settings.EXTRA_CHANNEL_ID, RestChannelId)) },
                modifier = Modifier.fillMaxWidth()) { Text("Geluid en meldingen beheren") }
            if (!exactAllowed && Build.VERSION.SDK_INT >= 31) TextButton(onClick = {
                settings.launch(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
            }, modifier = Modifier.fillMaxWidth()) { Text("Nauwkeurige planning beheren") }
        }
    }
}
