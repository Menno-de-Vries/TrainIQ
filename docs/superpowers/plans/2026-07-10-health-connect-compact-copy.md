# Health Connect Compact Copy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Maak Health Connect op Home en in Instellingen rustig en scanbaar, terwijl alle Samsung/Health Connect-diagnose beschikbaar en functioneel blijft.

**Architecture:** Eén kleine gedeelde UI-copyhelper bepaalt of de getoonde stappen van Samsung Health of Health Connect komen. Home gebruikt pure helpers voor een primaire statusregel en update-regel en toont exact één Health Connect-kaart. Instellingen gebruikt dezelfde bronnaam, een compacte samenvatting en een `rememberSaveable`/`AnimatedVisibility` technische sectie binnen de bestaande kaart.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, JUnit 4, AndroidX Health Connect-bestaande domainmodellen.

## Global Constraints

- De stappenbron, Samsung raw fallback, synchronisatie, cache, permissies en diagnostische waarden veranderen niet.
- Home toont bij succes alleen titel, `N stappen · bron`, update-tijd, refreshactie en een kort tijdelijk refreshresultaat.
- Home toont nooit tegelijk de compacte sync-kaart en de algemene Health Connect-permissiekaart.
- Instellingen toont standaard alleen status, compacte stappensamenvatting, controletijd, noodzakelijke acties en `Technische details tonen`.
- `raw`, `aggregate`, `SDK`, `pariteit` en foutcodes blijven buiten standaard Home- en Settings-copy.
- Alle bestaande technische diagnose en diagnoseacties blijven beschikbaar na uitklappen.
- De technische sectie is standaard dicht en gebruikt `rememberSaveable`.
- Geen nieuwe dependency, navigatieroute, commit of push.
- Bewaar alle bestaande niet-gerelateerde worktreewijzigingen.

---

### Task 1: Gedeelde broncopy en compacte Home-kaart

**Files:**
- Create: `TrainIQ-Project/app/src/main/java/com/trainiq/core/health/HealthConnectUserCopy.kt`
- Create: `TrainIQ-Project/app/src/test/java/com/trainiq/core/health/HealthConnectUserCopyTest.kt`
- Modify: `TrainIQ-Project/app/src/main/java/com/trainiq/features/home/HomeScreen.kt:314-650`
- Modify: `TrainIQ-Project/app/src/test/java/com/trainiq/features/home/HomeDashboardRefreshTest.kt:210-375`

**Interfaces:**
- Produces: `internal fun healthConnectStepSourceLabel(status: HealthConnectStatus): String`
- Produces: `internal fun homeHealthCompactSummary(status: HealthConnectStatus): String`
- Produces: `internal fun homeHealthSyncSummary(status: HealthConnectStatus): String?`
- Produces: `internal fun showCompactHomeHealthCard(status: HealthConnectStatus): Boolean`

- [x] **Step 1: Schrijf falende tests voor gedeelde bronselectie**

```kotlin
class HealthConnectUserCopyTest {
    @Test
    fun displayedSamsungSelectionUsesSamsungHealthLabel() {
        val status = HealthConnectStatus(
            state = HealthConnectState.CONNECTED,
            metrics = HealthConnectMetrics(stepsToday = 84),
            message = "Verbonden",
            stepDiagnostic = HealthConnectStepDiagnostic(
                aggregateStepsToday = 8,
                samsungHealthStepsToday = 84,
                samsungHealthAggregateStepsToday = 8,
                samsungRawStepRecordSumToday = 84,
                queriedAt = 123L,
            ),
        )

        assertEquals("Samsung Health", healthConnectStepSourceLabel(status))
    }

    @Test
    fun generalAggregateUsesHealthConnectLabel() {
        val status = HealthConnectStatus(
            state = HealthConnectState.CONNECTED,
            metrics = HealthConnectMetrics(stepsToday = 84),
            message = "Verbonden",
        )

        assertEquals("Health Connect", healthConnectStepSourceLabel(status))
    }
}
```

- [x] **Step 2: Schrijf falende Home-copy- en dubbele-kaarttests**

```kotlin
@Test
fun compactHomeHealthCopyShowsOnlyValueSourceAndUpdateTime() {
    val status = HealthConnectStatus(
        state = HealthConnectState.CONNECTED,
        metrics = HealthConnectMetrics(stepsToday = 84),
        message = "Verbonden",
        lastSyncedAt = 123L,
        stepDataFreshness = HealthConnectStepDataFreshness.FRESH,
        stepDataUpdatedAt = 123L,
        stepDiagnostic = HealthConnectStepDiagnostic(
            aggregateStepsToday = 8,
            samsungHealthStepsToday = 84,
            samsungHealthAggregateStepsToday = 8,
            samsungRawStepRecordSumToday = 84,
            queriedAt = 123L,
        ),
    )

    assertEquals("84 stappen · Samsung Health", homeHealthCompactSummary(status))
    assertEquals("Bijgewerkt om ${formatHomeLastSync(123L)}", homeHealthSyncSummary(status))
    assertFalse(homeHealthCompactSummary(status).contains("raw", ignoreCase = true))
    assertFalse(homeHealthCompactSummary(status).contains("aggregate", ignoreCase = true))
    assertTrue(showCompactHomeHealthCard(status))
    assertFalse(
        showCompactHomeHealthCard(
            status.copy(stepDataFreshness = HealthConnectStepDataFreshness.PERMISSION_MISSING),
        ),
    )
}
```

- [x] **Step 3: Draai de gerichte tests en bevestig RED**

Run from `TrainIQ-Project`:

```powershell
./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.core.health.HealthConnectUserCopyTest" --tests "com.trainiq.features.home.HomeDashboardRefreshTest"
```

Expected: testcompile faalt omdat de nieuwe helpers nog niet bestaan.

- [x] **Step 4: Implementeer de gedeelde bronhelper**

```kotlin
package com.trainiq.core.health

import com.trainiq.domain.model.HealthConnectStatus

internal fun healthConnectStepSourceLabel(status: HealthConnectStatus): String {
    val diagnostic = status.stepDiagnostic ?: return "Health Connect"
    val displaysDirectSamsung = diagnostic.samsungHealthDirectStepsToday != null &&
        diagnostic.displaySteps == diagnostic.samsungHealthDirectStepsToday
    val displaysSamsungVisible = diagnostic.samsungHealthStepsToday != null &&
        diagnostic.displaySteps == diagnostic.samsungHealthStepsToday
    return if (displaysDirectSamsung || displaysSamsungVisible) "Samsung Health" else "Health Connect"
}
```

- [x] **Step 5: Implementeer compacte Home-helpers**

```kotlin
internal fun homeHealthCompactSummary(status: HealthConnectStatus): String = when (status.state) {
    HealthConnectState.PERMISSION_REQUIRED -> "Stappentoegang ontbreekt."
    HealthConnectState.PROVIDER_MISSING -> "Health Connect moet worden bijgewerkt."
    HealthConnectState.UNSUPPORTED -> "Health Connect wordt niet ondersteund."
    HealthConnectState.ERROR -> "Stappen konden niet worden bijgewerkt."
    HealthConnectState.CONNECTED,
    HealthConnectState.NO_DATA -> when (status.stepDataFreshness) {
        HealthConnectStepDataFreshness.FRESH -> status.stepsToday
            ?.let { "$it stappen · ${healthConnectStepSourceLabel(status)}" }
            ?: "Nog geen recente stappen."
        HealthConnectStepDataFreshness.STALE_CACHE -> status.stepsToday
            ?.let { "Laatst bekend: $it stappen · ${healthConnectStepSourceLabel(status)}" }
            ?: "Nog geen recente stappen."
        HealthConnectStepDataFreshness.PERMISSION_MISSING -> "Stappentoegang ontbreekt."
        HealthConnectStepDataFreshness.UNAVAILABLE -> "Health Connect is niet beschikbaar."
        HealthConnectStepDataFreshness.ERROR -> "Stappen konden niet worden bijgewerkt."
        HealthConnectStepDataFreshness.UNKNOWN -> "Stappenstatus wordt opgehaald."
    }
}

internal fun homeHealthSyncSummary(status: HealthConnectStatus): String? {
    val updatedAt = status.stepDataUpdatedAt ?: status.lastSyncedAt ?: return null
    return when (status.stepDataFreshness) {
        HealthConnectStepDataFreshness.FRESH -> "Bijgewerkt om ${formatHomeLastSync(updatedAt)}"
        HealthConnectStepDataFreshness.STALE_CACHE -> "Laatste update om ${formatHomeLastSync(updatedAt)}"
        else -> null
    }
}

internal fun showCompactHomeHealthCard(status: HealthConnectStatus): Boolean =
    status.state in setOf(HealthConnectState.CONNECTED, HealthConnectState.NO_DATA) &&
        status.stepDataFreshness != HealthConnectStepDataFreshness.PERMISSION_MISSING
```

- [x] **Step 6: Maak Home exact één Health Connect-kaart**

In de grid:

```kotlin
if (showCompactHomeHealthCard(healthConnectStatus)) {
    item(span = { GridItemSpan(gridColumns) }) {
        HealthConnectSyncCard(
            status = healthConnectStatus,
            isRefreshingHealth = uiState.isRefreshingHealth,
            refreshMessage = uiState.refreshMessage,
            onRefresh = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onRefreshHealth()
            },
        )
    }
} else {
    item(span = { GridItemSpan(gridColumns) }) {
        PermissionManagerCard(
            status = healthConnectStatus,
            onRequestPermission = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onRequestHealthPermission()
            },
            onOpenInstall = {
                val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata"))
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata"))
                if (!context.startActivityIfResolvable(marketIntent)) {
                    context.startActivityIfResolvable(webIntent)
                }
            },
            onOpenSettings = {
                val settingsIntent = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
                if (!context.startActivityIfResolvable(settingsIntent)) {
                    onRefreshHealth()
                }
            },
            onRefresh = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onRefreshHealth()
            },
        )
    }
}
```

Behoud de bestaande callbacks en haptics letterlijk; verplaats alleen de bestaande `PermissionManagerCard` in de `else`-tak. Vervang in `HealthConnectSyncCard` de huidige status/diagnoseblokken door:

```kotlin
Text(
    homeHealthCompactSummary(status),
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.trainIqColors.mutedText,
)
homeHealthSyncSummary(status)?.let { syncSummary ->
    Text(
        syncSummary,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.trainIqColors.mutedText,
    )
}
refreshMessage?.let { message ->
    Text(message, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
}
```

Verwijder de ongebruikte lange `homeHealthStatusSummary` en `homeHealthStepDiagnostic` helpers.

- [x] **Step 7: Draai de gerichte tests en bevestig GREEN**

Run de Step 3-opdracht. Expected: beide testclasses slagen.

### Task 2: Compacte Settings met ingeklapte technische diagnose

**Files:**
- Modify: `TrainIQ-Project/app/src/main/java/com/trainiq/features/settings/SettingsSection.kt:1-1425`
- Modify: `TrainIQ-Project/app/src/test/java/com/trainiq/features/settings/SettingsUiStateTest.kt:145-255`

**Interfaces:**
- Consumes: `healthConnectStepSourceLabel(status)` uit Task 1.
- Produces: `internal fun healthConnectStepsAvailabilityMessage(status: HealthConnectStatus): String?`
- Produces: `internal fun healthConnectLastCheckedMessage(lastSyncedAt: Long?, zoneId: ZoneId = ZoneId.systemDefault()): String?`
- Produces: `internal fun healthTechnicalDetailsToggleLabel(expanded: Boolean): String`
- Produces: `private fun HealthConnectTechnicalDetails(...)` composable.

- [x] **Step 1: Schrijf falende compacte Settings-tests**

```kotlin
@Test
fun healthConnectCompactSummaryUsesFriendlySourceAndStaleCopy() {
    val diagnostic = HealthConnectStepDiagnostic(
        aggregateStepsToday = 8,
        samsungHealthStepsToday = 84,
        samsungHealthAggregateStepsToday = 8,
        samsungRawStepRecordSumToday = 84,
        queriedAt = 123L,
    )
    val fresh = HealthConnectStatus(
        state = HealthConnectState.CONNECTED,
        metrics = HealthConnectMetrics(stepsToday = 84),
        message = "Verbonden",
        stepDataFreshness = HealthConnectStepDataFreshness.FRESH,
        stepDiagnostic = diagnostic,
    )

    assertEquals("84 stappen via Samsung Health", healthConnectStepsAvailabilityMessage(fresh))
    assertEquals(
        "Laatst bekend: 84 stappen via Samsung Health",
        healthConnectStepsAvailabilityMessage(fresh.copy(stepDataFreshness = HealthConnectStepDataFreshness.STALE_CACHE)),
    )
    assertEquals("Technische details tonen", healthTechnicalDetailsToggleLabel(expanded = false))
    assertEquals("Technische details verbergen", healthTechnicalDetailsToggleLabel(expanded = true))
}

@Test
fun healthConnectLastCheckedUsesCompactTime() {
    val timestamp = Instant.parse("2026-07-10T00:33:00Z").toEpochMilli()
    assertEquals(
        "Laatst gecontroleerd om 00:33",
        healthConnectLastCheckedMessage(timestamp, ZoneId.of("UTC")),
    )
}
```

- [x] **Step 2: Voeg een falende structuurtest voor progressive disclosure toe**

```kotlin
@Test
fun healthConnectTechnicalDiagnosticsAreCollapsedButPreserved() {
    val source = File("src/main/java/com/trainiq/features/settings/SettingsSection.kt").readText()
    val healthCard = source.substringAfter("SectionCard(title = \"Health Connect\")")
        .substringBefore("SectionCard(title = \"Gegevens / opslag\")")
    val details = source.substringAfter("private fun HealthConnectTechnicalDetails(")
        .substringBefore("internal fun healthStatusLabel")

    assertTrue(source.contains("var showHealthTechnicalDetails by rememberSaveable"))
    assertTrue(healthCard.contains("AnimatedVisibility"))
    assertTrue(healthCard.contains("healthTechnicalDetailsToggleLabel"))
    assertTrue(details.contains("stepValueDebugSummary"))
    assertTrue(details.contains("samsungHealthDirectStatus"))
    assertTrue(details.contains("parityGapSummary"))
    assertTrue(details.contains("samsungStepDebugClipboardText"))
    assertTrue(details.contains("workoutWindowSummary"))
}
```

- [x] **Step 3: Draai de Settings-test en bevestig RED**

```powershell
./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.settings.SettingsUiStateTest"
```

Expected: compile/assertion failure omdat compacte helpers en uitklapper ontbreken.

- [x] **Step 4: Implementeer compacte copyhelpers**

```kotlin
internal fun healthConnectStepsAvailabilityMessage(status: HealthConnectStatus): String? {
    val steps = status.stepsToday ?: return null
    val source = healthConnectStepSourceLabel(status)
    return when (status.stepDataFreshness) {
        HealthConnectStepDataFreshness.FRESH -> "$steps stappen via $source"
        HealthConnectStepDataFreshness.STALE_CACHE -> "Laatst bekend: $steps stappen via $source"
        else -> null
    }
}

internal fun healthConnectLastCheckedMessage(
    lastSyncedAt: Long?,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String? = lastSyncedAt?.let { timestamp ->
    val time = Instant.ofEpochMilli(timestamp)
        .atZone(zoneId)
        .format(DateTimeFormatter.ofPattern("HH:mm"))
    "Laatst gecontroleerd om $time"
}

internal fun healthTechnicalDetailsToggleLabel(expanded: Boolean): String =
    if (expanded) "Technische details verbergen" else "Technische details tonen"
```

Voeg de imports voor `AnimatedVisibility`, `healthConnectStepSourceLabel`, `HealthConnectStepDiagnosticFreshness`, `Instant`, `ZoneId` en `DateTimeFormatter` toe.

- [x] **Step 5: Extraheer de bestaande technische inhoud zonder verlies**

Voeg binnen `SettingsSection.kt` toe:

```kotlin
@Composable
private fun HealthConnectTechnicalDetails(
    healthStatus: HealthConnectStatus,
    onOpenHealthSettings: () -> Unit,
    onOpenSamsungHealth: () -> Unit,
    onCopyDiagnostic: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
        Text(
            "Achtergrondsync werkt alleen wanneer Android en Health Connect die toegang toestaan.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        healthStatus.stepDiagnostic?.let { stepDiagnostic ->
            Text("Bronnen vandaag: ${stepDiagnostic.sourceSummary}", style = MaterialTheme.typography.bodySmall)
            Text("Venster: ${stepDiagnostic.queryWindowSummary}. ${stepDiagnostic.aggregateAuthorityLabel}", style = MaterialTheme.typography.bodySmall)
            Text("Samsung-vergelijking: ${stepDiagnostic.samsungHealthComparisonSummary}", style = MaterialTheme.typography.bodySmall)
            Text("Stappenwaarden: ${stepDiagnostic.stepValueDebugSummary}", style = MaterialTheme.typography.bodySmall)
            Text("Samsung-bron timing: ${stepDiagnostic.samsungSourceRecencySummary()}", style = MaterialTheme.typography.bodySmall)
            Text("Samsung direct: ${stepDiagnostic.samsungHealthDirectStatus}", style = MaterialTheme.typography.bodySmall)
            Text("Pariteit: ${stepDiagnostic.parityGapSummary}", style = MaterialTheme.typography.bodySmall)
            Text("Health Connect zichtbaar: ${stepDiagnostic.healthConnectVisibleStepSummary}", style = MaterialTheme.typography.bodySmall)
            Text("Health Connect-prioriteit: ${stepDiagnostic.healthConnectStepPrioritySummary}", style = MaterialTheme.typography.bodySmall)
            if (stepDiagnostic.hasMultipleHealthConnectStepSources) {
                TextButton(onClick = onOpenHealthSettings) { Text("Prioriteiten openen") }
            }
            Text("Workout-overlap: ${stepDiagnostic.workoutWindowSummary}", style = MaterialTheme.typography.bodySmall)
            if (!stepDiagnostic.hasSamsungHealthSource ||
                stepDiagnostic.freshness() == HealthConnectStepDiagnosticFreshness.STALE
            ) {
                Text(stepDiagnostic.samsungHealthSyncGuidance(), style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = { onCopyDiagnostic(stepDiagnostic.samsungStepDebugClipboardText()) }) {
                Text("Diagnose kopiëren")
            }
            TextButton(onClick = onOpenSamsungHealth) { Text("Samsung Health openen") }
        }
        healthStatus.averageHeartRateBpm?.let { Text("Gemiddelde hartslag vandaag: $it bpm") }
        healthStatus.latestHeartRateBpm?.let { Text("Laatste hartslagmeting: $it bpm") }
        healthStatus.sleepMinutes.takeIf { it > 0 }?.let { minutes ->
            Text("Recente slaap: ${minutes / 60}u ${minutes % 60}m")
        }
    }
}
```

Bij implementatie behoudt elk verplaatst diagnoseveld zijn bestaande `onSurfaceVariant`/primary kleur en `settingsActionLabel`-semantiek; de code hierboven definieert de complete inhoud, terwijl de bestaande modifiers letterlijk worden meegenomen.

- [x] **Step 6: Bouw de standaard ingeklapte Settings-sectie**

Voeg bij de lokale screenstate toe:

```kotlin
var showHealthTechnicalDetails by rememberSaveable { mutableStateOf(false) }
```

De Health Connect-kaart krijgt deze volgorde:

```kotlin
Text("Status: ${healthStatusLabel(healthStatus)}")
healthConnectStepsAvailabilityMessage(healthStatus)?.let { Text(it) }
healthConnectLastCheckedMessage(healthStatus.lastSyncedAt)?.let { Text(it) }
if (healthStatus.state !in setOf(HealthConnectState.CONNECTED, HealthConnectState.NO_DATA) ||
    healthStatus.hasPartialHealthConnectAccess()
) {
    Text(healthConnectSettingsMessage(healthStatus))
}

val toggleLabel = healthTechnicalDetailsToggleLabel(showHealthTechnicalDetails)
TextButton(
    modifier = Modifier.settingsActionLabel(toggleLabel),
    onClick = { showHealthTechnicalDetails = !showHealthTechnicalDetails },
) { Text(toggleLabel) }
AnimatedVisibility(visible = showHealthTechnicalDetails) {
    HealthConnectTechnicalDetails(
        healthStatus = healthStatus,
        onOpenHealthSettings = onOpenHealthSettings,
        onOpenSamsungHealth = onOpenSamsungHealth,
        onCopyDiagnostic = { diagnostic ->
            clipboardManager.setText(AnnotatedString(diagnostic))
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Samsung stappen-diagnose gekopieerd.")
            }
        },
    )
}
```

- [x] **Step 7: Draai Settings-test en beide UI-testclasses GREEN**

```powershell
./gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.features.settings.SettingsUiStateTest" --tests "com.trainiq.features.home.HomeDashboardRefreshTest" --tests "com.trainiq.core.health.HealthConnectUserCopyTest"
```

Expected: alle drie testclasses slagen.

### Task 3: Documentatie en volledige Android-quality-gate

**Files:**
- Modify: `docs/TrainIQ_QA_Findings_To_Improve.md`
- Modify: `docs/TrainIQ_Target_State_Progress.md`

**Interfaces:**
- Consumes: definitieve Home/Settings-copy en testresultaten uit Tasks 1-2.
- Produces: traceerbare UX- en verificatie-evidence zonder Samsung-pariteitsstatus te overschrijven.

- [x] **Step 1: Documenteer de compacte UX-batch**

Voeg bovenaan beide documenten een 2026-07-10-notitie toe met:

```markdown
## 2026-07-10 Health Connect Progressive Disclosure Polish

- Home toont bij succes één compacte Health Connect-kaart met stappen, bron en update-tijd; de dubbele permissiekaart is alleen nog voor actie-/foutstatussen.
- Instellingen toont status, compacte broncopy, tijd en acties; volledige Samsung/Health Connect-diagnose blijft standaard ingeklapt en kopieerbaar.
- De stappenbronselectie en Samsung raw fallback zijn niet gewijzigd.
- Verificatie: gerichte Home/Settings-copytests en de volledige debug unit/assemble/lint/AndroidTest/Samsung-paritygate PASS.
- Runtime: een fysieke Samsung UI-smoke blijft de eindacceptatie; claim geen emulatorpass wanneer de beschikbare target opnieuw tijdens package-verificatie vastloopt.
```

- [x] **Step 2: Draai de volledige ondersteunde gate**

```powershell
./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:compileDebugAndroidTestKotlin :app:assembleSamsungHealthParityDebug --no-configuration-cache
```

Expected: `BUILD SUCCESSFUL`, Samsung SDK-readiness groen, geen test- of lintfouten.

- [x] **Step 3: Controleer diff en APK**

```powershell
git diff --check
Get-FileHash -Algorithm SHA256 app/build/outputs/apk/debug/app-debug.apk
```

Expected: diff-check exit 0 (line-endingwaarschuwingen toegestaan), APK bestaat en heeft een SHA-256.

- [x] **Step 4: Runtimebewijs**

Installeer en launch op een gezonde beschikbare adb-target. Controleer dat Home één Health Connect-kaart toont en dat `Technische details tonen/verbergen` in Instellingen werkt. Als de huidige emulator opnieuw tijdens package-verificatie een startup-ANR krijgt, rapporteer `NOT RUN/blocked by emulator state` en lever de APK voor de afgesproken fysieke Samsung-smoke; claim geen emulatorpass.
