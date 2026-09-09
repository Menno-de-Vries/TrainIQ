package com.trainiq.features.nutrition

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.trainiq.domain.model.FoodProviderMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodProviderSelector(selected: FoodProviderMode, select: (FoodProviderMode) -> Unit) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    Column {
        Text("Voedingsbron", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FoodProviderMode.entries.forEach { mode ->
                FilterChip(selected = selected == mode, onClick = { select(mode) }, label = { Text(mode.label) })
            }
        }
        TextButton(onClick = {
            uriHandler.openUri(if (selected == FoodProviderMode.FATSECRET) "https://www.fatsecret.com" else "https://world.openfoodfacts.org/data")
        }) {
            Text(if (selected == FoodProviderMode.FATSECRET) "Powered by fatsecret" else "Open Food Facts · ODbL", style = MaterialTheme.typography.labelSmall)
        }
    }
}
