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
        if (selected != FoodProviderMode.FATSECRET) TextButton(onClick = {
            uriHandler.openUri("https://world.openfoodfacts.org/data")
        }) {
            Text("Open Food Facts · ODbL", style = MaterialTheme.typography.labelSmall)
        }
        if (selected != FoodProviderMode.OPEN_FOOD_FACTS) TextButton(onClick = {
            uriHandler.openUri("https://www.fatsecret.com")
        }) {
            Text("Powered by fatsecret", style = MaterialTheme.typography.labelSmall)
        }
    }
}
