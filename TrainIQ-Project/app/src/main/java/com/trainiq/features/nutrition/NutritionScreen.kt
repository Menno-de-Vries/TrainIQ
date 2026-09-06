@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.trainiq.features.nutrition

import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.BundleCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.trainiq.ai.services.AiUsageGate
import com.trainiq.ai.services.hasAnyReadyProvider
import com.trainiq.ai.services.toAiUserMessage
import com.trainiq.core.datastore.AiPreferences
import com.trainiq.core.datastore.UserPreferencesRepository
import com.trainiq.core.theme.spacing
import com.trainiq.core.ui.MessageCard
import com.trainiq.core.ui.reloadableObservation
import com.trainiq.core.ui.ScreenHeader
import com.trainiq.core.ui.ShimmerCardPlaceholder
import com.trainiq.core.ui.AppCard
import com.trainiq.core.ui.AppChip
import com.trainiq.core.ui.AppLinearProgress
import com.trainiq.core.ui.TrainIqFormField
import com.trainiq.core.ui.TrainIqFormFieldContext
import com.trainiq.core.ui.clearFocusOnTapOutside
import com.trainiq.core.theme.trainIqColors
import com.trainiq.domain.model.FoodItem
import com.trainiq.domain.model.BarcodeProductLookupResult
import com.trainiq.domain.model.FoodSourceType
import com.trainiq.domain.model.EnergyBalanceSnapshot
import com.trainiq.domain.model.LoggedMeal
import com.trainiq.domain.model.LoggedMealItem
import com.trainiq.domain.model.LoggedMealItemType
import com.trainiq.domain.model.MealAnalysisResult
import com.trainiq.domain.model.MealScanItem
import com.trainiq.domain.model.MealType
import com.trainiq.domain.model.MealAnalysisSource
import com.trainiq.domain.model.NutritionOverview
import com.trainiq.domain.model.NutritionFacts
import com.trainiq.domain.model.Recipe
import com.trainiq.domain.model.rounded
import com.trainiq.domain.repository.MealEntryRequest
import com.trainiq.domain.repository.MealEntrySnapshot
import com.trainiq.domain.repository.MealEntryType
import com.trainiq.domain.usecase.AnalyzeMealUseCase
import com.trainiq.domain.usecase.ClearLastScanResultUseCase
import com.trainiq.domain.usecase.DeleteFoodUseCase
import com.trainiq.domain.usecase.DeleteMealUseCase
import com.trainiq.domain.usecase.DeleteRecipeUseCase
import com.trainiq.domain.usecase.LookupBarcodeProductUseCase
import com.trainiq.domain.usecase.ObserveNutritionUseCase
import com.trainiq.domain.usecase.SaveFoodItemUseCase
import com.trainiq.domain.usecase.SaveMealUseCase
import com.trainiq.domain.usecase.SaveRecipeUseCase
import com.trainiq.navigation.TrainIqWindowWidthClass
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.math.abs
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ScanTarget { FOOD_EDITOR, RECIPE_DRAFT }

enum class BarcodeLookupTarget { FOOD_EDITOR, RECIPE_DRAFT }

private val RecipeDraftSaver = Saver<SnapshotStateList<Pair<Long, Double>>, Bundle>(
    save = { draft ->
        Bundle().apply {
            putLongArray("ids", draft.map { it.first }.toLongArray())
            putDoubleArray("grams", draft.map { it.second }.toDoubleArray())
        }
    },
    restore = { bundle ->
        val ids = bundle.getLongArray("ids") ?: longArrayOf()
        val grams = bundle.getDoubleArray("grams") ?: doubleArrayOf()
        mutableStateListOf<Pair<Long, Double>>().apply {
            ids.indices.take(minOf(ids.size, grams.size)).forEach { index -> add(ids[index] to grams[index]) }
        }
    },
)

private val MealDraftSaver = Saver<SnapshotStateList<EditableMealEntryRequest>, Bundle>(
    save = { draft ->
        Bundle().apply {
            putParcelableArrayList("items", ArrayList(draft.map { entry ->
                Bundle().apply {
                    putString("type", entry.request.itemType.name)
                    putLong("referenceId", entry.request.referenceId)
                    putDouble("gramsUsed", entry.request.gramsUsed)
                    putInt("servingCount", entry.request.servingCount)
                    putString("notes", entry.request.notes)
                    putString("gramsText", entry.gramsText)
                    entry.request.snapshot?.let { snapshot ->
                        putBundle("snapshot", Bundle().apply {
                            putString("name", snapshot.name)
                            putDouble("calories", snapshot.calories)
                            putDouble("protein", snapshot.protein)
                            putDouble("carbs", snapshot.carbs)
                            putDouble("fat", snapshot.fat)
                        })
                    }
                }
            }))
        }
    },
    restore = { bundle ->
        mutableStateListOf<EditableMealEntryRequest>().apply {
            BundleCompat.getParcelableArrayList(bundle, "items", Bundle::class.java).orEmpty().forEach { item ->
                val snapshot = item.getBundle("snapshot")?.let {
                    MealEntrySnapshot(it.getString("name").orEmpty(), it.getDouble("calories"), it.getDouble("protein"), it.getDouble("carbs"), it.getDouble("fat"))
                }
                add(EditableMealEntryRequest(
                    request = MealEntryRequest(
                        itemType = MealEntryType.valueOf(item.getString("type") ?: MealEntryType.FOOD.name),
                        referenceId = item.getLong("referenceId"),
                        gramsUsed = item.getDouble("gramsUsed"),
                        servingCount = item.getInt("servingCount", 1),
                        notes = item.getString("notes"),
                        snapshot = snapshot,
                    ),
                    gramsText = item.getString("gramsText").orEmpty(),
                ))
            }
        }
    },
)

private val EditableAiItemsSaver = Saver<SnapshotStateList<EditableAiItem>, Bundle>(
    save = { items ->
        Bundle().apply {
            putParcelableArrayList("items", ArrayList(items.map { item ->
                Bundle().apply {
                    putString("name", item.name); putString("grams", item.grams); putString("calories", item.calories)
                    putString("protein", item.protein); putString("carbs", item.carbs); putString("fat", item.fat)
                    putString("confidence", item.confidence); putString("notes", item.notes)
                }
            }))
        }
    },
    restore = { bundle ->
        mutableStateListOf<EditableAiItem>().apply {
            BundleCompat.getParcelableArrayList(bundle, "items", Bundle::class.java).orEmpty().forEach { item ->
                add(EditableAiItem(
                    name = item.getString("name").orEmpty(), grams = item.getString("grams").orEmpty(),
                    calories = item.getString("calories").orEmpty(), protein = item.getString("protein").orEmpty(),
                    carbs = item.getString("carbs").orEmpty(), fat = item.getString("fat").orEmpty(),
                    confidence = item.getString("confidence"), notes = item.getString("notes"),
                ))
            }
        }
    },
)

data class BarcodeLookupUiResult(
    val target: BarcodeLookupTarget,
    val product: BarcodeProductLookupResult?,
    val barcode: String,
)

private sealed interface PendingNutritionDelete {
    data class Meal(val id: Long) : PendingNutritionDelete
    data class Food(val id: Long) : PendingNutritionDelete
    data class Recipe(val id: Long) : PendingNutritionDelete
}

private enum class NutritionAiResultTarget {
    MealDraft,
    ProductLibrary,
    RecipeDraft,
}

sealed interface NutritionUiState {
    data object Loading : NutritionUiState
    data class Success(
        val overview: NutritionOverview,
        val aiPreferences: AiPreferences,
        val scanResult: MealAnalysisResult? = null,
        val message: String? = null,
        val isAnalyzing: Boolean = false,
        val scanTarget: ScanTarget = ScanTarget.FOOD_EDITOR,
        val barcodeLookupResult: BarcodeLookupUiResult? = null,
        val pendingSubmits: Set<NutritionSubmitKey> = emptySet(),
    ) : NutritionUiState
    data class Error(val message: String) : NutritionUiState
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class NutritionViewModel @Inject constructor(
    private val observeNutritionUseCase: ObserveNutritionUseCase,
    preferencesRepository: UserPreferencesRepository,
    aiUsageGate: AiUsageGate,
    private val analyzeMealUseCase: AnalyzeMealUseCase,
    private val saveFoodItemUseCase: SaveFoodItemUseCase,
    private val saveRecipeUseCase: SaveRecipeUseCase,
    private val saveMealUseCase: SaveMealUseCase,
    private val deleteMealUseCase: DeleteMealUseCase,
    private val deleteFoodUseCase: DeleteFoodUseCase,
    private val deleteRecipeUseCase: DeleteRecipeUseCase,
    private val lookupBarcodeProductUseCase: LookupBarcodeProductUseCase,
    private val clearLastScanResultUseCase: ClearLastScanResultUseCase,
) : ViewModel() {
    private data class NutritionEphemeralState(
        val scanResult: MealAnalysisResult? = null,
        val message: String? = null,
        val isAnalyzing: Boolean = false,
        val scanTarget: ScanTarget = ScanTarget.FOOD_EDITOR,
        val barcodeLookupResult: BarcodeLookupUiResult? = null,
        val pendingSubmits: Set<NutritionSubmitKey> = emptySet(),
    )

    private val reloads = MutableStateFlow(0)
    private val overview = reloadableObservation(reloads) { observeNutritionUseCase() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val aiPreferences = reloadableObservation(reloads) {
        preferencesRepository.aiPreferences
            .combineResolvedAiPreferences(aiUsageGate)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val ephemeral = MutableStateFlow(NutritionEphemeralState())

    val uiState: StateFlow<NutritionUiState> = combine(overview, aiPreferences, ephemeral) { currentOverview, currentPreferences, temp ->
        when {
            currentOverview == null || currentPreferences == null -> NutritionUiState.Loading
            currentOverview.isFailure -> NutritionUiState.Error("Voedingsgegevens konden niet worden geladen.")
            currentPreferences.isFailure -> NutritionUiState.Error("Voedingsvoorkeuren konden niet worden geladen.")
            else -> NutritionUiState.Success(
                overview = currentOverview.getOrThrow(),
                aiPreferences = currentPreferences.getOrThrow(),
                scanResult = temp.scanResult ?: currentOverview.getOrThrow().scannedResult,
                message = temp.message,
                isAnalyzing = temp.isAnalyzing,
                scanTarget = temp.scanTarget,
                barcodeLookupResult = temp.barcodeLookupResult,
                pendingSubmits = temp.pendingSubmits,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NutritionUiState.Loading)

    fun retry() {
        reloads.update { it + 1 }
    }

    fun analyze(path: String, context: String, capturedAtMillis: Long) {
        viewModelScope.launch {
            val ai = aiPreferences.value?.getOrNull() ?: AiPreferences(false, "")
            if (!ai.enabled) {
                ephemeral.update { it.copy(message = "AI staat uit in Instellingen. Voeding werkt nog steeds volledig met handmatige invoer.") }
                return@launch
            }
            if (!ai.hasAnyReadyProvider()) {
                ephemeral.update { it.copy(message = "Er is geen Gemini of OpenAI API-sleutel ingesteld. Voeg er een toe in Instellingen of blijf handmatig werken.") }
                return@launch
            }
            ephemeral.update { it.copy(isAnalyzing = true, message = null) }
            runCatching { analyzeMealUseCase(path, context, capturedAtMillis) }
                .onSuccess {
                    ephemeral.update { state ->
                        state.copy(
                            scanResult = it,
                            message = when {
                                it.source == MealAnalysisSource.LOCAL_FALLBACK ->
                                    it.notes ?: "AI was tijdelijk niet beschikbaar. Probeer later opnieuw of voer de maaltijd handmatig in."
                                it.items.isEmpty() ->
                                    "Er kwam geen betrouwbare maaltijdinschatting terug. Probeer opnieuw of voer de maaltijd handmatig in."
                                else -> it.notes ?: "Controleer de AI-inschatting voordat je die aan je maaltijd toevoegt."
                            },
                            isAnalyzing = false,
                        )
                    }
                }
                .onFailure { error ->
                    ephemeral.update {
                        it.copy(
                            message = error.toAiUserMessage("Maaltijdanalyse mislukt. Probeer opnieuw of ga verder met handmatige invoer."),
                            isAnalyzing = false,
                        )
                    }
                }
        }
    }

    fun saveFood(
        id: Long?,
        name: String,
        barcode: String?,
        calories: String,
        protein: String,
        carbs: String,
        fat: String,
        defaultServingGrams: String,
        sourceType: FoodSourceType,
        onSaved: (FoodItem) -> Unit = {},
        onFailure: (Throwable) -> Unit = {},
    ) {
        val usesFoodGuard = sourceType != FoodSourceType.AI
        if (usesFoodGuard && NutritionSubmitKey.Food in ephemeral.value.pendingSubmits) return
        val errors = validateFoodInput(name, calories, protein, carbs, fat, defaultServingGrams)
        if (errors.hasErrors) {
            ephemeral.update { it.copy(message = "Vul eerst een naam en niet-negatieve waarden per 100g in.") }
            return
        }
        val parsedCalories = calories.toNutritionNumberOrNull(max = 5000.0) ?: return
        val parsedProtein = protein.toNutritionNumberOrNull(max = 1000.0) ?: return
        val parsedCarbs = carbs.toNutritionNumberOrNull(max = 1000.0) ?: return
        val parsedFat = fat.toNutritionNumberOrNull(max = 1000.0) ?: return
        val parsedDefaultServingGrams = defaultServingGrams.toNutritionNumberOrNull(max = 100_000.0)?.takeIf { it > 0.0 } ?: return
        if (usesFoodGuard) {
            ephemeral.update { it.copy(pendingSubmits = it.pendingSubmits + NutritionSubmitKey.Food, message = null) }
        }
        viewModelScope.launch {
            try {
                val item = saveFoodItemUseCase(id, name.trim(), barcode?.trim()?.ifBlank { null }, parsedCalories, parsedProtein, parsedCarbs, parsedFat, parsedDefaultServingGrams, sourceType)
                ephemeral.update { it.copy(message = "${item.name} opgeslagen.") }
                onSaved(item)
            } catch (error: Throwable) {
                ephemeral.update {
                    it.copy(
                        message = if (sourceType == FoodSourceType.AI) {
                            "Opslaan mislukt. Probeer het opnieuw."
                        } else {
                            "Product opslaan mislukt. Controleer je invoer en probeer opnieuw."
                        },
                    )
                }
                onFailure(error)
            } finally {
                if (usesFoodGuard) {
                    ephemeral.update { it.copy(pendingSubmits = it.pendingSubmits - NutritionSubmitKey.Food) }
                }
            }
        }
    }

    fun saveRecipe(id: Long?, name: String, notes: String, cookedGrams: String, ingredients: List<Pair<Long, Double>>, onSaved: () -> Unit = {}) {
        if (NutritionSubmitKey.Recipe in ephemeral.value.pendingSubmits) return
        val parsedCookedGrams = cookedGrams.trim().takeIf { it.isNotBlank() }?.toNutritionNumberOrNull(max = 100_000.0)
        val errors = validateRecipeInput(name, cookedGrams, ingredients)
        if (errors.hasErrors) {
            ephemeral.update { it.copy(message = "Vul een receptnaam, positieve ingrediënten en eventueel een bereid gewicht boven 0 in.") }
            return
        }
        ephemeral.update { it.copy(pendingSubmits = it.pendingSubmits + NutritionSubmitKey.Recipe, message = null) }
        viewModelScope.launch {
            try {
                saveRecipeUseCase(id, name.trim(), notes.trim(), parsedCookedGrams, ingredients)
                ephemeral.update { it.copy(message = "Recept opgeslagen.") }
                onSaved()
            } catch (_: Throwable) {
                ephemeral.update { it.copy(message = "Recept opslaan mislukt. Controleer je invoer en probeer opnieuw.") }
            } finally {
                ephemeral.update { it.copy(pendingSubmits = it.pendingSubmits - NutritionSubmitKey.Recipe) }
            }
        }
    }

    fun saveMeal(id: Long?, mealType: MealType, name: String, notes: String, items: List<MealEntryRequest>, onSaved: () -> Unit = {}) {
        if (NutritionSubmitKey.Meal in ephemeral.value.pendingSubmits) return
        if (validateMealInput(name, items).hasErrors) {
            ephemeral.update { it.copy(message = "Vul een maaltijdnaam en positieve hoeveelheden in voordat je opslaat.") }
            return
        }
        ephemeral.update { it.copy(pendingSubmits = it.pendingSubmits + NutritionSubmitKey.Meal, message = null) }
        viewModelScope.launch {
            performMealSave(
                save = { saveMealUseCase(id, mealType, name.trim(), notes.trim(), items) },
                onSaved = onSaved,
                message = { text -> ephemeral.update { it.copy(message = text) } },
                onFinished = { ephemeral.update { it.copy(pendingSubmits = it.pendingSubmits - NutritionSubmitKey.Meal) } },
            )
        }
    }

    fun deleteMeal(mealId: Long) {
        if (NutritionSubmitKey.Delete in ephemeral.value.pendingSubmits) return
        ephemeral.update { it.copy(pendingSubmits = it.pendingSubmits + NutritionSubmitKey.Delete, message = null) }
        viewModelScope.launch {
            try {
                deleteMealUseCase(mealId)
                ephemeral.update { it.copy(message = "Maaltijd verwijderd.") }
            } catch (_: Throwable) {
                ephemeral.update { it.copy(message = "Maaltijd verwijderen mislukt. Probeer opnieuw.") }
            } finally {
                ephemeral.update { it.copy(pendingSubmits = it.pendingSubmits - NutritionSubmitKey.Delete) }
            }
        }
    }

    fun deleteFood(foodId: Long) {
        if (overview.value?.getOrNull()?.recipes.orEmpty().any { recipe -> recipe.ingredients.any { it.foodItemId == foodId } }) {
            ephemeral.update { it.copy(message = "Dit product wordt nog gebruikt in recepten. Verwijder het eerst uit die recepten.") }
            return
        }
        if (NutritionSubmitKey.Delete in ephemeral.value.pendingSubmits) return
        ephemeral.update { it.copy(pendingSubmits = it.pendingSubmits + NutritionSubmitKey.Delete, message = null) }
        viewModelScope.launch {
            try {
                deleteFoodUseCase(foodId)
                ephemeral.update { it.copy(message = "Product verwijderd.") }
            } catch (_: Exception) {
                ephemeral.update { it.copy(message = "Product verwijderen mislukt. Probeer opnieuw.") }
            } finally {
                ephemeral.update { it.copy(pendingSubmits = it.pendingSubmits - NutritionSubmitKey.Delete) }
            }
        }
    }

    fun deleteRecipe(recipeId: Long) {
        if (NutritionSubmitKey.Delete in ephemeral.value.pendingSubmits) return
        ephemeral.update { it.copy(pendingSubmits = it.pendingSubmits + NutritionSubmitKey.Delete, message = null) }
        viewModelScope.launch {
            try {
                deleteRecipeUseCase(recipeId)
                ephemeral.update { it.copy(message = "Recept verwijderd.") }
            } catch (_: Throwable) {
                ephemeral.update { it.copy(message = "Recept verwijderen mislukt. Probeer opnieuw.") }
            } finally {
                ephemeral.update { it.copy(pendingSubmits = it.pendingSubmits - NutritionSubmitKey.Delete) }
            }
        }
    }

    fun tryStartAiBatchSave(): Boolean {
        val current = ephemeral.value.pendingSubmits
        val started = tryStartNutritionSubmit(current, NutritionSubmitKey.AiItems)
        if (started is NutritionSubmitStartResult.AlreadyPending) return false
        started as NutritionSubmitStartResult.Started
        ephemeral.update { it.copy(pendingSubmits = started.pendingKeys, message = null) }
        return true
    }

    fun finishAiBatchSave() {
        ephemeral.update { it.copy(pendingSubmits = finishNutritionSubmit(it.pendingSubmits, NutritionSubmitKey.AiItems)) }
    }

    fun setMessage(message: String?) {
        ephemeral.update { it.copy(message = message) }
    }

    fun setScanResult(result: MealAnalysisResult?) {
        ephemeral.update { it.copy(scanResult = result) }
        if (result == null) clearLastScanResultUseCase()
    }

    fun setScanTarget(target: ScanTarget) {
        ephemeral.update { it.copy(scanTarget = target) }
    }

    private val barcodeLookup = BarcodeLookupRequest(
        scope = viewModelScope,
        lookup = lookupBarcodeProductUseCase::invoke,
        publish = { result ->
            ephemeral.update {
                it.copy(
                    barcodeLookupResult = result,
                    message = result.product?.let { product -> "${product.name} gevonden via barcode." }
                        ?: "Barcode gevonden. Productdata ontbreekt; vul kcal en macro's handmatig in.",
                )
            }
        },
    )

    fun lookupBarcodeProduct(barcode: String, target: BarcodeLookupTarget) {
        ephemeral.update { it.copy(barcodeLookupResult = null) }
        barcodeLookup.start(barcode, target)
    }

    fun clearBarcodeLookupResult() {
        barcodeLookup.clear()
        ephemeral.update { it.copy(barcodeLookupResult = null) }
    }
}

@Composable
fun NutritionRoute(
    onAiScanner: (String) -> Unit,
    onOpenBarcodeScanner: () -> Unit,
    pendingBarcode: String? = null,
    onBarcodeClear: () -> Unit = {},
    windowWidthClass: TrainIqWindowWidthClass = TrainIqWindowWidthClass.Compact,
    viewModel: NutritionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NutritionScreen(
        uiState = uiState,
        onSaveFood = viewModel::saveFood,
        onSaveRecipe = viewModel::saveRecipe,
        onSaveMeal = viewModel::saveMeal,
        onDeleteMeal = viewModel::deleteMeal,
        onDeleteFood = viewModel::deleteFood,
        onDeleteRecipe = viewModel::deleteRecipe,
        onTryStartAiBatchSave = viewModel::tryStartAiBatchSave,
        onFinishAiBatchSave = viewModel::finishAiBatchSave,
        onSetScanResult = viewModel::setScanResult,
        onSetScanTarget = viewModel::setScanTarget,
        onLookupBarcodeProduct = viewModel::lookupBarcodeProduct,
        onClearBarcodeLookupResult = viewModel::clearBarcodeLookupResult,
        onSetMessage = viewModel::setMessage,
        onDismissMessage = { viewModel.setMessage(null) },
        onRetry = viewModel::retry,
        onAnalyzeImportedPhoto = viewModel::analyze,
        onAiScanner = onAiScanner,
        onOpenBarcodeScanner = onOpenBarcodeScanner,
        pendingBarcode = pendingBarcode,
        onBarcodeClear = onBarcodeClear,
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    uiState: NutritionUiState,
    onSaveFood: (Long?, String, String?, String, String, String, String, String, FoodSourceType, (FoodItem) -> Unit, (Throwable) -> Unit) -> Unit,
    onSaveRecipe: (Long?, String, String, String, List<Pair<Long, Double>>, () -> Unit) -> Unit,
    onSaveMeal: (Long?, MealType, String, String, List<MealEntryRequest>, () -> Unit) -> Unit,
    onDeleteMeal: (Long) -> Unit,
    onDeleteFood: (Long) -> Unit,
    onDeleteRecipe: (Long) -> Unit,
    onTryStartAiBatchSave: () -> Boolean,
    onFinishAiBatchSave: () -> Unit,
    onSetScanResult: (MealAnalysisResult?) -> Unit,
    onSetScanTarget: (ScanTarget) -> Unit = {},
    onLookupBarcodeProduct: (String, BarcodeLookupTarget) -> Unit = { _, _ -> },
    onClearBarcodeLookupResult: () -> Unit = {},
    onSetMessage: (String?) -> Unit,
    onDismissMessage: () -> Unit,
    onRetry: () -> Unit,
    onAnalyzeImportedPhoto: (String, String, Long) -> Unit = { _, _, _ -> },
    onAiScanner: (String) -> Unit,
    onOpenBarcodeScanner: () -> Unit,
    pendingBarcode: String? = null,
    onBarcodeClear: () -> Unit = {},
) {
    val context = LocalContext.current
    val successState = uiState as? NutritionUiState.Success
    val overview = successState?.overview
    val barcodeLookupResult = successState?.barcodeLookupResult
    val aiPreferences = successState?.aiPreferences ?: AiPreferences(false, "")
    val scanResult = successState?.scanResult
    val message = successState?.message
    val isAnalyzing = successState?.isAnalyzing == true
    val pendingSubmits = successState?.pendingSubmits.orEmpty()
    val isFoodSaving = NutritionSubmitKey.Food in pendingSubmits
    val isRecipeSaving = NutritionSubmitKey.Recipe in pendingSubmits
    val isMealSaving = NutritionSubmitKey.Meal in pendingSubmits
    val isDeletePending = NutritionSubmitKey.Delete in pendingSubmits
    val isAiBatchPending = NutritionSubmitKey.AiItems in pendingSubmits
    val haptics = LocalHapticFeedback.current
    val sectionTabs = nutritionSectionTabs()
    val nutritionListStates = List(nutritionInternalTabCount()) { rememberLazyListState() }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val recipeActionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val addToMealSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sectionMenuSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val foodEditorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val recipeEditorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ingredientPickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ingredientEditorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val nutritionListState = nutritionListStates[selectedTab.coerceIn(nutritionListStates.indices)]
    var aiResultTarget by rememberSaveable { mutableStateOf(NutritionAiResultTarget.MealDraft) }
    var showAddToMealActions by remember { mutableStateOf(false) }
    var showSectionMenu by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<PendingNutritionDelete?>(null) }
    var addToMealType by rememberSaveable { mutableStateOf(MealType.BREAKFAST) }
    var hasAddToMealTarget by rememberSaveable { mutableStateOf(false) }
    var pendingAiMealType by rememberSaveable { mutableStateOf<MealType?>(null) }
    var selectedFoodId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedRecipeIngredientFoodId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedRecipeId by rememberSaveable { mutableStateOf<Long?>(null) }
    var hydratedFoodId by rememberSaveable { mutableStateOf<Long?>(null) }
    var hydratedRecipeId by rememberSaveable { mutableStateOf<Long?>(null) }
    var hydratedScanResultHash by rememberSaveable { mutableStateOf<Int?>(null) }
    val selectedFood = overview?.foods?.firstOrNull { it.id == selectedFoodId }
    val selectedRecipeIngredientFood = overview?.foods?.firstOrNull { it.id == selectedRecipeIngredientFoodId }
    val selectedRecipe = overview?.recipes?.firstOrNull { it.id == selectedRecipeId }
    var showFoodEditor by rememberSaveable { mutableStateOf(false) }
    var showRecipeEditor by rememberSaveable { mutableStateOf(false) }
    var showIngredientPicker by rememberSaveable { mutableStateOf(false) }
    var showIngredientEditor by rememberSaveable { mutableStateOf(false) }
    var productSearchQuery by rememberSaveable { mutableStateOf("") }
    var recipeSearchQuery by rememberSaveable { mutableStateOf("") }
    var ingredientSearchQuery by rememberSaveable { mutableStateOf("") }

    var foodName by rememberSaveable { mutableStateOf("") }
    var barcode by rememberSaveable { mutableStateOf("") }
    var calories by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var carbs by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }
    var defaultServingGrams by rememberSaveable { mutableStateOf("100") }
    var foodErrors by remember { mutableStateOf(FoodFieldErrors()) }

    var recipeName by rememberSaveable { mutableStateOf("") }
    var recipeNotes by rememberSaveable { mutableStateOf("") }
    var recipeCookedGrams by rememberSaveable { mutableStateOf("") }
    var ingredientGrams by rememberSaveable { mutableStateOf("100") }
    var recipeAiContext by rememberSaveable { mutableStateOf("") }
    var quickIngredientName by rememberSaveable { mutableStateOf("") }
    var quickIngredientBarcode by rememberSaveable { mutableStateOf("") }
    var quickIngredientKcal by rememberSaveable { mutableStateOf("") }
    var quickIngredientProtein by rememberSaveable { mutableStateOf("") }
    var quickIngredientCarbs by rememberSaveable { mutableStateOf("") }
    var quickIngredientFat by rememberSaveable { mutableStateOf("") }
    var showRecipeActions by remember { mutableStateOf(false) }
    val recipeDraft = rememberSaveable(saver = RecipeDraftSaver) { mutableStateListOf<Pair<Long, Double>>() }
    var recipeErrors by remember { mutableStateOf(RecipeFieldErrors()) }
    var quickIngredientErrors by remember { mutableStateOf(FoodFieldErrors()) }

    var mealType by rememberSaveable { mutableStateOf(MealType.LUNCH) }
    var mealName by rememberSaveable { mutableStateOf("") }
    var mealNotes by rememberSaveable { mutableStateOf("") }
    var mealRecipeGrams by rememberSaveable { mutableStateOf("150") }
    var editingMealId by rememberSaveable { mutableStateOf<Long?>(null) }
    val mealDraft = rememberSaveable(saver = MealDraftSaver) { mutableStateListOf<EditableMealEntryRequest>() }
    var mealErrors by remember { mutableStateOf(MealFieldErrors()) }
    var mealRecipeGramsErrors by remember { mutableStateOf(QuickAddFieldErrors()) }

    var aiContext by rememberSaveable { mutableStateOf("") }
    val editableAiItems = rememberSaveable(saver = EditableAiItemsSaver) { mutableStateListOf<EditableAiItem>() }
    val photoImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val path = copyScannerImageFromUri(context, uri)
        if (path == null) {
            onSetMessage("Foto importeren mislukt. Kies een duidelijke JPG of PNG.")
            return@rememberLauncherForActivityResult
        }
        val activeContext = if (aiResultTarget == NutritionAiResultTarget.RecipeDraft) recipeAiContext else aiContext
        onAnalyzeImportedPhoto(path, activeContext, System.currentTimeMillis())
        selectedTab = 2
    }
    var aiItemErrors by remember { mutableStateOf<Map<Int, AiItemFieldErrors>>(emptyMap()) }
    var aiSaveProgress by remember { mutableStateOf(startAiBatchSaveProgress(0)) }
    val isAiSaving = !aiSaveProgress.isFinished || isAiBatchPending

    fun selectMealDraftTarget(type: MealType) {
        editingMealId = null
        mealType = type
        mealErrors = MealFieldErrors()
        addToMealType = type
        hasAddToMealTarget = true
    }

    fun preserveContextualMealTarget() {
        if (hasAddToMealTarget) {
            mealType = addToMealType
        }
    }

    fun applyDefaultMealName(itemName: String) {
        if (editingMealId == null && mealName.isBlank()) {
            mealName = itemName.trim()
        }
    }

    fun resetFoodEditorState() {
        onClearBarcodeLookupResult()
        selectedFoodId = null
        hydratedFoodId = null
        foodName = ""
        barcode = ""
        calories = ""
        protein = ""
        carbs = ""
        fat = ""
        defaultServingGrams = "100"
        foodErrors = FoodFieldErrors()
    }

    fun resetFoodEditor() {
        resetFoodEditorState()
        showFoodEditor = false
    }

    fun openNewFoodEditor() {
        resetFoodEditorState()
        showFoodEditor = true
    }

    fun resetRecipeEditorState() {
        selectedRecipeId = null
        hydratedRecipeId = null
        selectedRecipeIngredientFoodId = null
        recipeName = ""
        recipeNotes = ""
        recipeCookedGrams = ""
        ingredientGrams = "100"
        recipeAiContext = ""
        recipeDraft.clear()
        recipeErrors = RecipeFieldErrors()
    }

    fun resetRecipeEditor() {
        resetRecipeEditorState()
        showRecipeEditor = false
    }

    fun openNewRecipeEditor() {
        resetRecipeEditorState()
        showRecipeEditor = true
    }

    fun resetQuickIngredientEditor() {
        onClearBarcodeLookupResult()
        quickIngredientName = ""
        quickIngredientBarcode = ""
        quickIngredientKcal = ""
        quickIngredientProtein = ""
        quickIngredientCarbs = ""
        quickIngredientFat = ""
        ingredientGrams = "100"
        quickIngredientErrors = FoodFieldErrors()
        recipeErrors = recipeErrors.copy(ingredientGrams = null)
        showIngredientEditor = false
    }

    fun finishAiBatchItem(item: EditableAiItem, success: Boolean, onAllSucceeded: () -> Unit) {
        val next = aiSaveProgress.finishOne(success)
        aiSaveProgress = next
        if (success) {
            editableAiItems.remove(item)
            aiItemErrors = emptyMap()
        }
        if (next.isFinished) {
            onFinishAiBatchSave()
        }
        if (next.allSucceeded) {
            editableAiItems.clear()
            aiItemErrors = emptyMap()
            onAllSucceeded()
        }
    }

    fun saveAiFoodItemSequentially(batchItem: AiBatchItem): CompletableDeferred<Result<FoodItem>> {
        val result = CompletableDeferred<Result<FoodItem>>()
        val item = batchItem.item
        val grams = batchItem.grams
        onSaveFood(
            null,
            item.name,
            null,
            per100Value(item.calories, grams),
            per100Value(item.protein, grams),
            per100Value(item.carbs, grams),
            per100Value(item.fat, grams),
            formatNumber(grams),
            FoodSourceType.AI,
            { saved -> result.complete(Result.success(saved)) },
            { error -> result.complete(Result.failure(error)) },
        )
        return result
    }

    fun startAiFoodBatchSave(
        batchItems: List<AiBatchItem>,
        successMessage: (Int) -> String,
        partialFailureMessage: (Int, Int) -> String,
        onSavedItem: (FoodItem, Double) -> Unit,
        onAllSucceeded: () -> Unit,
    ) {
        if (!onTryStartAiBatchSave()) return
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        aiSaveProgress = startAiBatchSaveProgress(batchItems.size)
        coroutineScope.launch {
            var succeeded = 0
            batchItems.forEach { batchItem ->
                val saved = saveAiFoodItemSequentially(batchItem).await().getOrNull()
                if (saved != null) {
                    succeeded += 1
                    onSavedItem(saved, batchItem.grams)
                    finishAiBatchItem(item = batchItem.item, success = true, onAllSucceeded = onAllSucceeded)
                } else {
                    finishAiBatchItem(item = batchItem.item, success = false, onAllSucceeded = {})
                }
            }
            onSetMessage(
                if (succeeded == batchItems.size) {
                    successMessage(succeeded)
                } else {
                    partialFailureMessage(succeeded, batchItems.size - succeeded)
                },
            )
        }
    }

    fun addAiBatchItemsAsMealSnapshots(batchItems: List<AiBatchItem>) {
        batchItems.forEach { batchItem ->
            val request = batchItem.toSnapshotMealEntry()
            applyDefaultMealName(request.snapshot?.name.orEmpty())
            mealDraft += request.toEditableMealEntryRequest()
        }
        editableAiItems.clear()
        aiItemErrors = emptyMap()
        pendingAiMealType = null
        aiResultTarget = NutritionAiResultTarget.MealDraft
        selectedTab = 1
        onSetScanResult(null)
        onSetMessage("${batchItems.size} AI-items alleen aan deze maaltijd toegevoegd.")
    }

    fun saveAiBatchAsProducts(batchItems: List<AiBatchItem>) {
        startAiFoodBatchSave(
            batchItems = batchItems,
            successMessage = { count -> "$count AI-producten opgeslagen." },
            partialFailureMessage = { success, failed -> "$success AI-producten opgeslagen, $failed mislukt. Controleer de overgebleven items." },
            onSavedItem = { _, _ -> },
            onAllSucceeded = {
                aiResultTarget = NutritionAiResultTarget.MealDraft
                onSetScanResult(null)
                selectedTab = 4
            },
        )
    }

    fun saveAiBatchAsRecipeIngredients(batchItems: List<AiBatchItem>) {
        recipeName = recipeName.ifBlank { "AI-fotorecept" }
        showRecipeEditor = true
        startAiFoodBatchSave(
            batchItems = batchItems,
            successMessage = { count -> "$count AI-items toegevoegd aan je recept." },
            partialFailureMessage = { success, failed -> "$success AI-items toegevoegd, $failed mislukt. Controleer de overgebleven items." },
            onSavedItem = { saved, grams ->
                recipeDraft += saved.id to grams
                selectedRecipeIngredientFoodId = saved.id
            },
            onAllSucceeded = {
                aiResultTarget = NutritionAiResultTarget.MealDraft
                onSetScanResult(null)
                selectedTab = 3
            },
        )
    }

    LaunchedEffect(pendingBarcode) {
        if (pendingBarcode != null) {
            if (successState?.scanTarget == ScanTarget.RECIPE_DRAFT) {
                quickIngredientBarcode = pendingBarcode
                selectedTab = 3
                onDismissMessage()
                quickIngredientName = quickIngredientName.ifBlank { "Gescand product" }
                showIngredientEditor = true
                onLookupBarcodeProduct(pendingBarcode, BarcodeLookupTarget.RECIPE_DRAFT)
            } else {
                barcode = pendingBarcode
                showFoodEditor = true
                selectedTab = 4
                onLookupBarcodeProduct(pendingBarcode, BarcodeLookupTarget.FOOD_EDITOR)
            }
            onSetScanTarget(ScanTarget.FOOD_EDITOR)
            onBarcodeClear()
        }
    }

    LaunchedEffect(barcodeLookupResult) {
        val result = barcodeLookupResult ?: return@LaunchedEffect
        result.product?.let { product ->
            when (result.target) {
                BarcodeLookupTarget.FOOD_EDITOR -> {
                    if (!showFoodEditor || barcode.filter(Char::isDigit) != result.barcode) return@let
                    barcode = product.barcode
                    foodName = product.name
                    calories = formatNumber(product.caloriesPer100g)
                    protein = formatNumber(product.proteinPer100g)
                    carbs = formatNumber(product.carbsPer100g)
                    fat = formatNumber(product.fatPer100g)
                    defaultServingGrams = "100"
                    foodErrors = FoodFieldErrors()
                    showFoodEditor = true
                    selectedTab = 4
                }
                BarcodeLookupTarget.RECIPE_DRAFT -> {
                    if (!showIngredientEditor || quickIngredientBarcode.filter(Char::isDigit) != result.barcode) return@let
                    quickIngredientBarcode = product.barcode
                    quickIngredientName = product.name
                    quickIngredientKcal = formatNumber(product.caloriesPer100g)
                    quickIngredientProtein = formatNumber(product.proteinPer100g)
                    quickIngredientCarbs = formatNumber(product.carbsPer100g)
                    quickIngredientFat = formatNumber(product.fatPer100g)
                    quickIngredientErrors = FoodFieldErrors()
                    showIngredientEditor = true
                    selectedTab = 3
                }
            }
        }
        onClearBarcodeLookupResult()
    }

    LaunchedEffect(showFoodEditor, selectedFood?.id) {
        if (!showFoodEditor) return@LaunchedEffect
        val selectedFood = selectedFood ?: return@LaunchedEffect
        if (hydratedFoodId == selectedFood.id) return@LaunchedEffect
        foodName = selectedFood.name
        barcode = selectedFood.barcode.orEmpty()
        calories = formatNumber(selectedFood.caloriesPer100g)
        protein = formatNumber(selectedFood.proteinPer100g)
        carbs = formatNumber(selectedFood.carbsPer100g)
        fat = formatNumber(selectedFood.fatPer100g)
        defaultServingGrams = formatNumber(selectedFood.defaultServingGrams)
        hydratedFoodId = selectedFood.id
    }

    LaunchedEffect(selectedRecipe?.id) {
        selectedRecipe?.let {
            if (hydratedRecipeId == it.id) return@LaunchedEffect
            recipeName = it.name
            recipeNotes = it.notes.orEmpty()
            recipeCookedGrams = formatNullableNumber(it.totalCookedGrams)
            recipeDraft.clear()
            recipeDraft.addAll(it.ingredients.map { ingredient -> ingredient.foodItemId to ingredient.gramsUsed })
            hydratedRecipeId = it.id
        }
    }

    LaunchedEffect(scanResult) {
        val currentScanResult = scanResult
        if (currentScanResult == null) {
            hydratedScanResultHash = null
            return@LaunchedEffect
        }
        if (hydratedScanResultHash == currentScanResult.hashCode()) return@LaunchedEffect
        editableAiItems.clear()
        currentScanResult.items.forEach { editableAiItems += EditableAiItem.from(it) }
        if (aiResultTarget == NutritionAiResultTarget.MealDraft) {
            (pendingAiMealType ?: currentScanResult.suggestedMealType)?.let {
                mealType = it
                if (mealName in listOf("Breakfast", "Lunch", "Dinner", "Snack", "Ochtend", "Middag", "Avond", "Snacks")) {
                    mealName = it.dutchLabel
                }
            }
        }
        if (editableAiItems.isNotEmpty()) {
            selectedTab = 2
        }
        hydratedScanResultHash = currentScanResult.hashCode()
    }

    LaunchedEffect(message) {
        val currentMessage = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(currentMessage)
        onDismissMessage()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.spacing.medium,
                    vertical = MaterialTheme.spacing.small,
                ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ScreenHeader(title = "Voeding", subtitle = "Voeding loggen zonder gedoe")
                    }
                    Surface(
                        onClick = { showSectionMenu = true },
                        modifier = Modifier
                            .size(44.dp)
                            .semantics {
                                contentDescription = nutritionSectionMenuButtonDescription()
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Menu,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
            LazyColumn(
            state = nutritionListState,
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding(),
            contentPadding = PaddingValues(
                start = MaterialTheme.spacing.medium,
                top = MaterialTheme.spacing.medium,
                end = MaterialTheme.spacing.medium,
                bottom = 132.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        ) {
            when (uiState) {
                NutritionUiState.Loading -> {
                    items(8) { index ->
                        ShimmerCardPlaceholder(lineCount = if (index == 0) 3 else 5)
                    }
                }
                is NutritionUiState.Error -> {
                    item {
                        MessageCard(message = uiState.message)
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) { Text("Opnieuw proberen") }
                    }
                }
                is NutritionUiState.Success -> {
                    when (selectedTab) {
                        0 -> {
                            item {
                                DailyMealsDashboard(
                                    overview = overview,
                                    onAddToMeal = { type ->
                                        selectMealDraftTarget(type)
                                        showAddToMealActions = true
                                    },
                                    onEditMeal = { meal ->
                                        editingMealId = meal.id
                                        mealType = meal.mealType
                                        mealName = meal.name
                                        mealNotes = meal.notes.orEmpty()
                                        mealDraft.clear()
                                        mealDraft.addAll(meal.items.map {
                                            MealEntryRequest(
                                                itemType = it.itemType.toMealEntryType(),
                                                referenceId = it.referenceId,
                                                gramsUsed = it.gramsUsed,
                                                servingCount = it.servingCount,
                                                notes = it.notes,
                                                snapshot = it.takeIf { item -> item.itemType.name == "SNAPSHOT" }?.toMealEntrySnapshot(),
                                            ).toEditableMealEntryRequest()
                                        })
                                        selectedTab = 1
                                        coroutineScope.launch { nutritionListState.animateScrollToItem(1) }
                                    },
                                    onDeleteMeal = { pendingDelete = PendingNutritionDelete.Meal(it) },
                                )
                            }
                        }
                        1 -> {
                            if (mealDraft.isEmpty()) {
                                item {
                                    AppCard(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.primary) {
                                        Text("Maaltijdconcept", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                                        Text(
                                            "Gebruik Toevoegen bij een maaltijd, kies een product of recept, of scan met AI. Daarna controleer je hier alles voordat je opslaat.",
                                            color = MaterialTheme.trainIqColors.mutedText,
                                        )
                                    }
                                }
                            } else {
                                item {
                                MealDraftReviewCard(
                                    mealType = mealType,
                                    mealName = mealName,
                                    mealNotes = mealNotes,
                                    mealDraft = mealDraft.toList(),
                                    foods = overview?.foods.orEmpty(),
                                    recipes = overview?.recipes.orEmpty(),
                                    errors = mealErrors,
                                    isSaving = isMealSaving,
                                    isEditing = editingMealId != null,
                                    onMealTypeChange = { mealType = it },
                                    onMealNameChange = { mealName = it; mealErrors = mealErrors.copy(name = null) },
                                    onMealNotesChange = { mealNotes = it },
                                    onUpdateDraftItemGrams = { index, grams ->
                                        val parsed = grams.toNutritionNumberOrNull(max = 100_000.0)
                                        val current = mealDraft[index]
                                        if (parsed != null && parsed > 0.0) {
                                            mealDraft[index] = current.copy(
                                                request = current.request.copy(gramsUsed = parsed),
                                                gramsText = grams,
                                            )
                                        } else {
                                            mealDraft[index] = current.copy(gramsText = grams)
                                        }
                                        mealErrors = mealErrors.copy(items = null)
                                    },
                                    onUpdateDraftItemServingCount = { index, count ->
                                        val current = mealDraft[index]
                                        mealDraft[index] = current.copy(request = current.request.copy(servingCount = count.coerceAtLeast(1)))
                                    },
                                    onRemoveDraftItem = { index -> mealDraft.removeAt(index) },
                                    onSave = {
                                        val requests = mealDraft.toMealEntryRequestsOrNull()
                                        val errors = if (requests == null) {
                                            validateMealInput(mealName, emptyList()).copy(items = "Vul voor elk item een positief aantal gram in.")
                                        } else {
                                            validateMealInput(mealName, requests)
                                        }
                                        mealErrors = errors
                                        if (errors.hasErrors || isMealSaving) return@MealDraftReviewCard
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSaveMeal(editingMealId, mealType, mealName, mealNotes, requests.orEmpty()) {
                                            editingMealId = null
                                            mealName = ""
                                            mealNotes = ""
                                            mealDraft.clear()
                                            mealErrors = MealFieldErrors()
                                            onSetScanResult(null)
                                            selectedTab = 0
                                        }
                                    },
                                )
                                }
                            }
                        }
                        2 -> {
                            item {
                            AiMealAnalysisCard(
                                aiPreferences = aiPreferences,
                                target = aiResultTarget,
                                primaryLabel = when (aiResultTarget) {
                                    NutritionAiResultTarget.MealDraft -> "Aan maaltijd toevoegen"
                                    NutritionAiResultTarget.ProductLibrary -> "Producten opslaan"
                                    NutritionAiResultTarget.RecipeDraft -> "Als ingrediënten toevoegen"
                                },
                                aiContext = if (aiResultTarget == NutritionAiResultTarget.RecipeDraft) recipeAiContext else aiContext,
                                editableItems = editableAiItems.toList(),
                                itemErrors = aiItemErrors,
                                isSaving = isAiSaving,
                                isAnalyzing = isAnalyzing,
                                onContextChange = {
                                    if (aiResultTarget == NutritionAiResultTarget.RecipeDraft) {
                                        recipeAiContext = it
                                    } else {
                                        aiContext = it
                                    }
                                },
                                onOpenCamera = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onAiScanner(if (aiResultTarget == NutritionAiResultTarget.RecipeDraft) recipeAiContext else aiContext)
                                },
                                onImportPhoto = {
                                    photoImportLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                onChangeItem = { index, item ->
                                    editableAiItems[index] = item
                                    aiItemErrors = aiItemErrors - index
                                },
                                onDeleteItem = { index -> editableAiItems.removeAt(index) },
                                onPrimaryAction = {
                                    val errors = editableAiItems.mapIndexedNotNull { index, item ->
                                        validateEditableAiItem(item).takeIf { it.hasErrors }?.let { index to it }
                                    }.toMap()
                                    aiItemErrors = errors
                                    if (errors.isNotEmpty() || isAiSaving || editableAiItems.isEmpty()) return@AiMealAnalysisCard
                                    val batchItems = buildValidAiBatchItems(editableAiItems.toList())
                                    if (batchItems == null || batchItems.isEmpty()) {
                                        aiItemErrors = aiBatchNutritionErrors(editableAiItems.size)
                                        return@AiMealAnalysisCard
                                    }
                                    when (aiResultTarget) {
                                        NutritionAiResultTarget.MealDraft -> {
                                            addAiBatchItemsAsMealSnapshots(batchItems)
                                        }
                                        NutritionAiResultTarget.ProductLibrary -> {
                                            saveAiBatchAsProducts(batchItems)
                                        }
                                        NutritionAiResultTarget.RecipeDraft -> {
                                            saveAiBatchAsRecipeIngredients(batchItems)
                                        }
                                    }
                                },
                            )
                            }
                        }
                        3 -> {
                            item {
                                RecipesHeaderCard(
                                    recipeCount = overview?.recipes?.size ?: 0,
                                    onCreateClick = { showRecipeActions = true },
                                    onScanIngredient = {
                                        if (!showRecipeEditor) openNewRecipeEditor()
                                        onSetScanTarget(ScanTarget.RECIPE_DRAFT)
                                        onOpenBarcodeScanner()
                                    },
                                    onPhotoIngredient = {
                                        if (!showRecipeEditor) openNewRecipeEditor()
                                        aiResultTarget = NutritionAiResultTarget.RecipeDraft
                                        selectedTab = 2
                                        onAiScanner(recipeAiContext)
                                    },
                                    aiEnabled = aiPreferences.hasAnyReadyProvider(),
                                )
                            }
                            item {
                                SavedRecipesCard(
                                    recipes = overview?.recipes.orEmpty(),
                                    searchQuery = recipeSearchQuery,
                                    selectedRecipeId = selectedRecipeId,
                                    mealRecipeGrams = mealRecipeGrams,
                                    mealRecipeGramsError = mealRecipeGramsErrors.grams,
                                    isAddPending = isMealSaving,
                                    onSearchQueryChange = { recipeSearchQuery = it },
                                    onMealRecipeGramsChange = { mealRecipeGrams = it; mealRecipeGramsErrors = QuickAddFieldErrors() },
                                    onSelect = {
                                        selectedRecipeId = it
                                        showRecipeEditor = true
                                    },
                                    onUseInMeal = { recipe ->
                                        val errors = validateQuickAddGrams(mealRecipeGrams)
                                        mealRecipeGramsErrors = errors
                                        if (errors.hasErrors || isMealSaving) return@SavedRecipesCard
                                        val grams = mealRecipeGrams.toNutritionNumberOrNull(max = 100_000.0)
                                        if (grams != null && grams > 0.0) {
                                            preserveContextualMealTarget()
                                            applyDefaultMealName(recipe.name)
                                            mealDraft += MealEntryRequest(MealEntryType.RECIPE, recipe.id, grams).toEditableMealEntryRequest()
                                            mealErrors = MealFieldErrors()
                                            hasAddToMealTarget = false
                                            selectedTab = 1
                                        }
                                    },
                                    onDelete = { pendingDelete = PendingNutritionDelete.Recipe(it) },
                                )
                            }
                        }
                        4 -> {
                            item {
                                ProductsHeaderCard(
                                    foodCount = overview?.foods.orEmpty().size,
                                    onCreateClick = {
                                        openNewFoodEditor()
                                    },
                                    onScanBarcode = {
                                        openNewFoodEditor()
                                        onSetScanTarget(ScanTarget.FOOD_EDITOR)
                                        onOpenBarcodeScanner()
                                    },
                                    onPhotoProduct = {
                                        aiResultTarget = NutritionAiResultTarget.ProductLibrary
                                        selectedTab = 2
                                        onAiScanner(aiContext)
                                    },
                                    aiEnabled = aiPreferences.hasAnyReadyProvider(),
                                )
                            }
                            item {
                                SavedFoodsCard(
                                    foods = overview?.foods.orEmpty(),
                                    searchQuery = productSearchQuery,
                                    selectedFoodId = selectedFoodId,
                                    isAddPending = isMealSaving,
                                    onSearchQueryChange = { productSearchQuery = it },
                                    onSelect = {
                                        selectedFoodId = it
                                        showFoodEditor = true
                                    },
                                    onQuickAdd = { food ->
                                        if (isMealSaving) return@SavedFoodsCard
                                        val grams = food.defaultServingGrams.takeIf { it.isFinite() && it > 0.0 } ?: 100.0
                                        preserveContextualMealTarget()
                                        applyDefaultMealName(food.name)
                                        mealDraft += MealEntryRequest(MealEntryType.FOOD, food.id, grams).toEditableMealEntryRequest()
                                        mealErrors = MealFieldErrors()
                                        hasAddToMealTarget = false
                                        selectedTab = 1
                                    },
                                    onDelete = { pendingDelete = PendingNutritionDelete.Food(it) },
                                )
                            }
                        }
                        5 -> {
                            item {
                                MealHistoryCard(
                                    meals = overview?.meals.orEmpty(),
                                    foods = overview?.foods.orEmpty(),
                                    recipes = overview?.recipes.orEmpty(),
                                    onReuseMeal = { meal ->
                                        mealType = meal.mealType
                                        mealName = meal.name
                                        mealNotes = meal.notes.orEmpty()
                                        mealDraft.clear()
                                        mealDraft.addAll(meal.items.map {
                                            MealEntryRequest(
                                                itemType = it.itemType.toMealEntryType(),
                                                referenceId = it.referenceId,
                                                gramsUsed = it.gramsUsed,
                                                servingCount = it.servingCount,
                                                notes = it.notes,
                                                snapshot = it.takeIf { item -> item.itemType.name == "SNAPSHOT" }?.toMealEntrySnapshot(),
                                            ).toEditableMealEntryRequest()
                                        })
                                        selectedTab = 1
                                    },
                                    onDeleteMeal = { pendingDelete = PendingNutritionDelete.Meal(it) },
                                )
                            }
                        }
                    }
                }
            }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(MaterialTheme.spacing.medium),
        )
    }
    if (showFoodEditor) {
        ModalBottomSheet(
            onDismissRequest = { resetFoodEditor() },
            sheetState = foodEditorSheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MaterialTheme.spacing.medium)
                    .padding(bottom = 24.dp),
            ) {
                FoodEditorCard(
                    foodName = foodName,
                    barcode = barcode,
                    calories = calories,
                    protein = protein,
                    carbs = carbs,
                    fat = fat,
                    defaultServingGrams = defaultServingGrams,
                    onFoodNameChange = { foodName = it; foodErrors = foodErrors.copy(name = null) },
                    onBarcodeChange = { barcode = it },
                    onCaloriesChange = { calories = it; foodErrors = foodErrors.copy(calories = null) },
                    onProteinChange = { protein = it; foodErrors = foodErrors.copy(protein = null) },
                    onCarbsChange = { carbs = it; foodErrors = foodErrors.copy(carbs = null) },
                    onFatChange = { fat = it; foodErrors = foodErrors.copy(fat = null) },
                    onDefaultServingGramsChange = { defaultServingGrams = it; foodErrors = foodErrors.copy(defaultServingGrams = null) },
                    isEditing = selectedFoodId != null,
                    errors = foodErrors,
                    isSaving = isFoodSaving,
                    saveToMealOnly = hasAddToMealTarget && selectedFoodId == null,
                    onScanBarcode = {
                        onSetScanTarget(ScanTarget.FOOD_EDITOR)
                        onOpenBarcodeScanner()
                    },
                    onSave = {
                        val errors = validateFoodInput(foodName, calories, protein, carbs, fat, defaultServingGrams)
                        foodErrors = errors
                        if (errors.hasErrors || isFoodSaving) return@FoodEditorCard
                        if (hasAddToMealTarget && selectedFoodId == null) {
                            val grams = defaultServingGrams.toNutritionNumberOrNull(max = 100_000.0) ?: 100.0
                            applyDefaultMealName(foodName)
                            mealDraft += createSnapshotMealEntry(
                                name = foodName,
                                grams = grams,
                                caloriesPer100g = calories.toNutritionNumberOrNull(max = 5000.0) ?: 0.0,
                                proteinPer100g = protein.toNutritionNumberOrNull(max = 1000.0) ?: 0.0,
                                carbsPer100g = carbs.toNutritionNumberOrNull(max = 1000.0) ?: 0.0,
                                fatPer100g = fat.toNutritionNumberOrNull(max = 1000.0) ?: 0.0,
                            ).toEditableMealEntryRequest()
                            resetFoodEditor()
                            hasAddToMealTarget = false
                            selectedTab = 1
                            onSetMessage("Product alleen aan deze maaltijd toegevoegd.")
                            return@FoodEditorCard
                        }
                        onSaveFood(
                            selectedFoodId,
                            foodName,
                            barcode,
                            calories,
                            protein,
                            carbs,
                            fat,
                            defaultServingGrams,
                            if (barcode.isBlank()) FoodSourceType.MANUAL else FoodSourceType.BARCODE,
                            { resetFoodEditor() },
                            {},
                        )
                    },
                    onCancelEdit = { resetFoodEditor() },
                )
            }
        }
    }
    if (showRecipeEditor) {
        ModalBottomSheet(
            onDismissRequest = { resetRecipeEditor() },
            sheetState = recipeEditorSheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MaterialTheme.spacing.medium)
                    .padding(bottom = 24.dp),
            ) {
                RecipeEditorCard(
                    recipeName = recipeName,
                    recipeNotes = recipeNotes,
                    recipeCookedGrams = recipeCookedGrams,
                    ingredientGrams = ingredientGrams,
                    selectedFood = selectedRecipeIngredientFood,
                    draft = recipeDraft.toList(),
                    foods = overview?.foods.orEmpty(),
                    recipeAiContext = recipeAiContext,
                    aiEnabled = aiPreferences.hasAnyReadyProvider(),
                    isEditing = selectedRecipeId != null,
                    errors = recipeErrors,
                    isSaving = isRecipeSaving,
                    onRecipeNameChange = { recipeName = it; recipeErrors = recipeErrors.copy(name = null) },
                    onRecipeNotesChange = { recipeNotes = it },
                    onRecipeCookedGramsChange = { recipeCookedGrams = it; recipeErrors = recipeErrors.copy(cookedGrams = null) },
                    onIngredientGramsChange = { ingredientGrams = it; recipeErrors = recipeErrors.copy(ingredientGrams = null) },
                    onRecipeAiContextChange = { recipeAiContext = it },
                    onChooseIngredient = {
                        ingredientSearchQuery = ""
                        showIngredientPicker = true
                    },
                    onOpenIngredientEditor = { showIngredientEditor = true },
                    onAddIngredient = {
                        val gramsErrors = validateIngredientGrams(ingredientGrams)
                        recipeErrors = recipeErrors.copy(ingredientGrams = gramsErrors.ingredientGrams)
                        if (gramsErrors.hasErrors) return@RecipeEditorCard
                        val grams = ingredientGrams.toNutritionNumberOrNull(max = 100_000.0)
                        val foodId = selectedRecipeIngredientFoodId
                        if (grams != null && grams > 0.0 && foodId != null) {
                            recipeDraft += foodId to grams
                            ingredientGrams = ""
                            recipeErrors = recipeErrors.copy(ingredientGrams = null, ingredients = null)
                        }
                    },
                    onRemoveIngredient = { index -> recipeDraft.removeAt(index) },
                    onSave = {
                        val errors = validateRecipeInput(recipeName, recipeCookedGrams, recipeDraft.toList())
                        recipeErrors = errors
                        if (errors.hasErrors || isRecipeSaving) return@RecipeEditorCard
                        onSaveRecipe(selectedRecipeId, recipeName, recipeNotes, recipeCookedGrams, recipeDraft.toList()) {
                            resetRecipeEditor()
                        }
                    },
                    onScanBarcodeForRecipe = {
                        onSetScanTarget(ScanTarget.RECIPE_DRAFT)
                        onOpenBarcodeScanner()
                    },
                    onAiVisionForRecipe = {
                        aiResultTarget = NutritionAiResultTarget.RecipeDraft
                        selectedTab = 2
                        onAiScanner(recipeAiContext)
                    },
                    onImportPhotoForRecipe = {
                        aiResultTarget = NutritionAiResultTarget.RecipeDraft
                        selectedTab = 2
                        photoImportLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onCancelEdit = { resetRecipeEditor() },
                )
            }
        }
    }
    if (showIngredientPicker) {
        ModalBottomSheet(
            onDismissRequest = { showIngredientPicker = false },
            sheetState = ingredientPickerSheetState,
        ) {
            ProductPickerSheet(
                foods = overview?.foods.orEmpty(),
                searchQuery = ingredientSearchQuery,
                selectedFoodId = selectedRecipeIngredientFoodId,
                onSearchQueryChange = { ingredientSearchQuery = it },
                onSelect = { food ->
                    selectedRecipeIngredientFoodId = food.id
                    showIngredientPicker = false
                    recipeErrors = recipeErrors.copy(ingredientGrams = null, ingredients = null)
                },
            )
        }
    }
    if (showIngredientEditor) {
        ModalBottomSheet(
            onDismissRequest = { resetQuickIngredientEditor() },
            sheetState = ingredientEditorSheetState,
        ) {
            RecipeIngredientEditorSheet(
                ingredientGrams = ingredientGrams,
                quickIngredientName = quickIngredientName,
                quickIngredientBarcode = quickIngredientBarcode,
                quickIngredientKcal = quickIngredientKcal,
                quickIngredientProtein = quickIngredientProtein,
                quickIngredientCarbs = quickIngredientCarbs,
                quickIngredientFat = quickIngredientFat,
                errors = recipeErrors,
                quickIngredientErrors = quickIngredientErrors,
                isSaving = isFoodSaving,
                onIngredientGramsChange = { ingredientGrams = it; recipeErrors = recipeErrors.copy(ingredientGrams = null) },
                onQuickIngredientNameChange = { quickIngredientName = it; quickIngredientErrors = quickIngredientErrors.copy(name = null) },
                onQuickIngredientBarcodeChange = { quickIngredientBarcode = it },
                onQuickIngredientKcalChange = { quickIngredientKcal = it; quickIngredientErrors = quickIngredientErrors.copy(calories = null) },
                onQuickIngredientProteinChange = { quickIngredientProtein = it; quickIngredientErrors = quickIngredientErrors.copy(protein = null) },
                onQuickIngredientCarbsChange = { quickIngredientCarbs = it; quickIngredientErrors = quickIngredientErrors.copy(carbs = null) },
                onQuickIngredientFatChange = { quickIngredientFat = it; quickIngredientErrors = quickIngredientErrors.copy(fat = null) },
                onSave = {
                    val foodErrors = validateFoodInput(quickIngredientName, quickIngredientKcal, quickIngredientProtein, quickIngredientCarbs, quickIngredientFat)
                    val gramsErrors = validateIngredientGrams(ingredientGrams)
                    quickIngredientErrors = foodErrors
                    recipeErrors = recipeErrors.copy(ingredientGrams = gramsErrors.ingredientGrams)
                    if (foodErrors.hasErrors || gramsErrors.hasErrors || isFoodSaving) return@RecipeIngredientEditorSheet
                    val grams = ingredientGrams.toNutritionNumberOrNull(max = 100_000.0)
                    val kcal = quickIngredientKcal.toNutritionNumberOrNull(max = 5000.0)
                    val proteinValue = quickIngredientProtein.toNutritionNumberOrNull(max = 1000.0)
                    val carbsValue = quickIngredientCarbs.toNutritionNumberOrNull(max = 1000.0)
                    val fatValue = quickIngredientFat.toNutritionNumberOrNull(max = 1000.0)
                    if (quickIngredientName.isNotBlank() && grams != null && grams > 0.0 && kcal != null && proteinValue != null && carbsValue != null && fatValue != null) {
                        onSaveFood(
                            null,
                            quickIngredientName,
                            quickIngredientBarcode,
                            quickIngredientKcal,
                            quickIngredientProtein,
                            quickIngredientCarbs,
                            quickIngredientFat,
                            formatNumber(grams),
                            if (quickIngredientBarcode.isBlank()) FoodSourceType.MANUAL else FoodSourceType.BARCODE,
                            { saved ->
                                recipeDraft += saved.id to grams
                                selectedRecipeIngredientFoodId = saved.id
                                recipeErrors = recipeErrors.copy(ingredientGrams = null, ingredients = null)
                                resetQuickIngredientEditor()
                            },
                            {},
                        )
                    }
                },
                onCancel = { resetQuickIngredientEditor() },
                onScanBarcode = {
                    onSetScanTarget(ScanTarget.RECIPE_DRAFT)
                    onOpenBarcodeScanner()
                },
            )
        }
    }
    if (showRecipeActions) {
        ModalBottomSheet(
            onDismissRequest = { showRecipeActions = false },
            sheetState = recipeActionSheetState,
        ) {
            RecipeActionBottomSheet(
                aiEnabled = aiPreferences.hasAnyReadyProvider(),
                onDismiss = { showRecipeActions = false },
                onManualRecipe = {
                    showRecipeActions = false
                    openNewRecipeEditor()
                },
                onBarcodeIngredient = {
                    showRecipeActions = false
                    if (!showRecipeEditor) openNewRecipeEditor()
                    onSetScanTarget(ScanTarget.RECIPE_DRAFT)
                    onOpenBarcodeScanner()
                },
                onPhotoIngredient = {
                    showRecipeActions = false
                    if (!showRecipeEditor) openNewRecipeEditor()
                    aiResultTarget = NutritionAiResultTarget.RecipeDraft
                    selectedTab = 2
                    onAiScanner(recipeAiContext)
                },
                onExistingRecipeToMeal = {
                    showRecipeActions = false
                    selectedTab = 3
                },
            )
        }
    }
    if (showSectionMenu) {
        ModalBottomSheet(
            onDismissRequest = { showSectionMenu = false },
            sheetState = sectionMenuSheetState,
        ) {
            NutritionSectionMenuSheet(
                tabs = sectionTabs,
                selectedTab = selectedTab,
                onSelectTab = { tab ->
                    selectedTab = tab.index
                    hasAddToMealTarget = false
                    showSectionMenu = false
                },
                onDismiss = { showSectionMenu = false },
            )
        }
    }
    if (showAddToMealActions) {
        ModalBottomSheet(
            onDismissRequest = { showAddToMealActions = false },
            sheetState = addToMealSheetState,
        ) {
            AddToMealActionSheet(
                mealType = addToMealType,
                hasSavedFoods = overview?.foods?.isNotEmpty() == true,
                hasSavedRecipes = overview?.recipes?.isNotEmpty() == true,
                hasDraft = mealDraft.isNotEmpty(),
                aiEnabled = aiPreferences.hasAnyReadyProvider(),
                onDismiss = { showAddToMealActions = false },
                onManualFood = {
                    mealType = addToMealType
                    openNewFoodEditor()
                    showAddToMealActions = false
                    selectedTab = 4
                },
                onSavedFood = {
                    mealType = addToMealType
                    selectedFoodId = null
                    showAddToMealActions = false
                    selectedTab = 4
                },
                onRecipe = {
                    mealType = addToMealType
                    showAddToMealActions = false
                    selectedTab = 3
                },
                aiContext = aiContext,
                onAiContextChange = { aiContext = it },
                onPhotoAi = {
                    mealType = addToMealType
                    pendingAiMealType = addToMealType
                    showAddToMealActions = false
                    aiResultTarget = NutritionAiResultTarget.MealDraft
                    selectedTab = 2
                    onAiScanner(aiContext)
                },
                onOpenMealDraft = {
                    mealType = addToMealType
                    showAddToMealActions = false
                    selectedTab = 1
                },
            )
        }
    }
    pendingDelete?.let { delete ->
        ConfirmNutritionDeleteDialog(
            pendingDelete = delete,
            isDeleting = isDeletePending,
            onConfirm = {
                if (isDeletePending) return@ConfirmNutritionDeleteDialog
                pendingDelete = null
                when (delete) {
                    is PendingNutritionDelete.Meal -> onDeleteMeal(delete.id)
                    is PendingNutritionDelete.Food -> onDeleteFood(delete.id)
                    is PendingNutritionDelete.Recipe -> onDeleteRecipe(delete.id)
                }
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun NutritionSectionMenuSheet(
    tabs: List<NutritionSectionTab>,
    selectedTab: Int,
    onSelectTab: (NutritionSectionTab) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        BottomSheetHeader(title = "Voeding secties")
        tabs.forEach { tab ->
            val selected = selectedTab == tab.index
            if (selected) {
                Button(
                    onClick = { onSelectTab(tab) },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.small),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) { Text(tab.title) }
            } else {
                OutlinedButton(
                    onClick = { onSelectTab(tab) },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.small),
                ) { Text(tab.title) }
            }
        }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Sluiten") }
    }
}

private fun kotlinx.coroutines.flow.Flow<AiPreferences>.combineResolvedAiPreferences(
    aiUsageGate: AiUsageGate,
): kotlinx.coroutines.flow.Flow<AiPreferences> = map { legacySettings ->
    aiUsageGate.resolveSettings(legacySettings)
}

@Composable
private fun ConfirmNutritionDeleteDialog(
    pendingDelete: PendingNutritionDelete,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title = when (pendingDelete) {
        is PendingNutritionDelete.Meal -> "Maaltijd verwijderen?"
        is PendingNutritionDelete.Food -> "Product verwijderen?"
        is PendingNutritionDelete.Recipe -> "Recept verwijderen?"
    }
    val body = when (pendingDelete) {
        is PendingNutritionDelete.Meal -> "Deze maaltijd verdwijnt uit je voedingslog."
        is PendingNutritionDelete.Food -> "Dit product verdwijnt uit je productlijst. Recepten die dit product gebruiken kunnen hun ingrediënt of totalen verliezen."
        is PendingNutritionDelete.Recipe -> "Dit recept verdwijnt uit je opgeslagen recepten. Bestaande maaltijdlogs blijven als snapshot staan, maar concepten met dit recept werken niet meer."
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Text(body)
        },
        confirmButton = { Button(onClick = onConfirm, enabled = !isDeleting) { Text(if (isDeleting) "Verwijderen..." else "Verwijderen") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuleren") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryCard(overview: NutritionOverview?) {
    val calories = overview?.todaysCalories ?: 0.0
    val progress = nutritionEnergyProgressFraction(calories, overview?.energyBalance)
    AppCard(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.trainIqColors.amber) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Vandaag", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.trainIqColors.amber, fontWeight = FontWeight.SemiBold)
            Text("${formatNumber(calories)} kcal", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.trainIqColors.amber)
            Text(
                nutritionMacroSummary(overview?.todaysProtein ?: 0.0, overview?.todaysCarbs ?: 0.0, overview?.todaysFat ?: 0.0),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.trainIqColors.mutedText,
            )
            Text("${(progress * 100).toInt()}% van verbruik", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.trainIqColors.mutedText)
        }
        AppLinearProgress(progress = progress, accent = MaterialTheme.trainIqColors.amber)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            AppChip(label = "Producten ${overview?.foods?.size ?: 0}", accent = MaterialTheme.trainIqColors.amber)
            AppChip(label = "Maaltijden ${overview?.meals?.size ?: 0}", accent = MaterialTheme.trainIqColors.amber)
            AppChip(label = "Recepten ${overview?.recipes?.size ?: 0}", accent = MaterialTheme.trainIqColors.amber)
        }
            overview?.energyBalance?.let {
            Text(nutritionEnergyBalanceSummary(it), color = MaterialTheme.trainIqColors.mutedText)
            Text(nutritionEnergyBreakdownText(it), color = MaterialTheme.trainIqColors.mutedText)
            }
            overview?.todaysMealsByType?.forEach { (mealType, meals) ->
                if (meals.isNotEmpty()) {
                Text("${mealType.dutchLabel}: ${meals.size} gelogd", color = MaterialTheme.trainIqColors.mutedText)
                }
            }
    }
}

@Composable
private fun DailyMealsDashboard(
    overview: NutritionOverview?,
    onAddToMeal: (MealType) -> Unit,
    onEditMeal: (LoggedMeal) -> Unit,
    onDeleteMeal: (Long) -> Unit,
) {
    val sections = listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK)
    val hasMealsToday = overview?.todaysMealsByType?.values?.any { it.isNotEmpty() } == true
    AppCard(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.trainIqColors.amber) {
        Text("Voedingsdag", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.trainIqColors.amber, fontWeight = FontWeight.SemiBold)
        Text(
            "${formatNumber(overview?.todaysCalories ?: 0.0)} kcal",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.trainIqColors.amber,
        )
        NutritionMetricStrip(
            values = listOf(
                NutritionMetricValue("Kcal", formatNumber(overview?.todaysCalories ?: 0.0), MaterialTheme.trainIqColors.amber),
                NutritionMetricValue("Eiwit", "${formatNumber(overview?.todaysProtein ?: 0.0)} g", MaterialTheme.trainIqColors.mint),
                NutritionMetricValue("Kh", "${formatNumber(overview?.todaysCarbs ?: 0.0)} g", MaterialTheme.trainIqColors.blue),
                NutritionMetricValue("Vet", "${formatNumber(overview?.todaysFat ?: 0.0)} g", MaterialTheme.colorScheme.tertiary),
            ),
        )
        if (!hasMealsToday) {
            Text(
                "Start rustig: log eerst één maaltijd of product. De totalen bouwen daarna vanzelf mee.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.trainIqColors.mutedText,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            sections.forEach { type ->
                MealSectionCard(
                    mealType = type,
                    meals = overview?.todaysMealsByType?.get(type).orEmpty(),
                    onAdd = { onAddToMeal(type) },
                    onEditMeal = onEditMeal,
                    onDeleteMeal = onDeleteMeal,
                )
            }
        }
    }
}

@Composable
private fun MealSectionCard(
    mealType: MealType,
    meals: List<LoggedMeal>,
    onAdd: () -> Unit,
    onEditMeal: (LoggedMeal) -> Unit,
    onDeleteMeal: (Long) -> Unit,
) {
    val total = meals.fold(NutritionFacts.Zero) { acc, meal -> acc + meal.totalNutrition }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (meals.isEmpty()) {
            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.72f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.76f)
        },
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(mealType.dutchLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    IconButton(onClick = onAdd, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = mealSectionAddContentDescription(mealType),
                        )
                    }
                }
            }
            Text(
                "${formatNumber(total.calories)} kcal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )
            NutritionMetricStrip(
                values = listOf(
                    NutritionMetricValue("Kcal", formatNumber(total.calories), MaterialTheme.colorScheme.primary),
                    NutritionMetricValue("Eiwit", "${formatNumber(total.protein)} g", MaterialTheme.trainIqColors.mint),
                    NutritionMetricValue("Kh", "${formatNumber(total.carbs)} g", MaterialTheme.trainIqColors.blue),
                    NutritionMetricValue("Vet", "${formatNumber(total.fat)} g", MaterialTheme.colorScheme.tertiary),
                ),
            )
            if (meals.isEmpty()) {
                Text(mealSectionEmptyText(mealType), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.trainIqColors.mutedText)
            } else {
                meals.forEach { meal ->
                    MealEntryRow(meal = meal, onEditMeal = onEditMeal, onDeleteMeal = onDeleteMeal)
                }
            }
        }
    }
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun MealEntryRow(
    meal: LoggedMeal,
    onEditMeal: (LoggedMeal) -> Unit,
    onDeleteMeal: (Long) -> Unit,
) {
    var showActions by remember { mutableStateOf(false) }
    val actionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        onClick = { showActions = true },
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(meal.name, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            NutritionMetricGrid(
                values = listOf(
                    NutritionMetricValue("Kcal", formatNumber(meal.totalNutrition.calories), MaterialTheme.colorScheme.primary),
                    NutritionMetricValue("Eiwit", "${formatNumber(meal.totalNutrition.protein)} g", MaterialTheme.trainIqColors.mint),
                    NutritionMetricValue("Kh", "${formatNumber(meal.totalNutrition.carbs)} g", MaterialTheme.trainIqColors.blue),
                    NutritionMetricValue("Vet", "${formatNumber(meal.totalNutrition.fat)} g", MaterialTheme.colorScheme.tertiary),
                ),
            )
            meal.items.forEach { item ->
                Text(
                    "${item.name} · ${formatNumber(item.gramsUsed)}g x${item.servingCount.coerceAtLeast(1)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.trainIqColors.mutedText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
    if (showActions) {
        ModalBottomSheet(
            onDismissRequest = { showActions = false },
            sheetState = actionSheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.large, vertical = MaterialTheme.spacing.medium)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                Text(meal.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "${formatNumber(meal.totalNutrition.calories)} kcal - ${nutritionMacroSummary(meal.totalNutrition.protein, meal.totalNutrition.carbs, meal.totalNutrition.fat)}",
                    color = MaterialTheme.trainIqColors.mutedText,
                )
                Button(
                    onClick = {
                        showActions = false
                        onEditMeal(meal)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(mealEditActionLabel()) }
                OutlinedButton(
                    onClick = {
                        showActions = false
                        onDeleteMeal(meal.id)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Maaltijd verwijderen") }
            }
        }
    }
}

@Composable
private fun RecipesHeaderCard(
    recipeCount: Int,
    onCreateClick: () -> Unit,
    onScanIngredient: () -> Unit,
    onPhotoIngredient: () -> Unit,
    aiEnabled: Boolean,
) {
    AppCard(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.trainIqColors.amber) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Recepten", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.trainIqColors.amber, fontWeight = FontWeight.SemiBold)
                Text("$recipeCount opgeslagen", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text("Herbruikbare recepten - handmatig, barcode of AI-foto", color = MaterialTheme.trainIqColors.mutedText)
            }
            IconButton(onClick = onCreateClick) {
                Icon(Icons.Rounded.Add, contentDescription = "Recept toevoegen")
            }
        }
        EqualNutritionHeaderActions(
            items = listOf(
                NutritionHeaderAction("Recept maken", primary = true, onClick = onCreateClick),
                NutritionHeaderAction("Ingrediënt scannen", onClick = onScanIngredient),
                NutritionHeaderAction("Foto/AI ingrediënten", enabled = aiEnabled, onClick = onPhotoIngredient),
            ),
        )
    }
}

private data class NutritionMetricValue(
    val label: String,
    val value: String,
    val accent: Color,
)

@Composable
private fun NutritionMetricStrip(values: List<NutritionMetricValue>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        values.forEach { (label, value, accent) ->
            NutritionMetricPill(label, value, accent, modifier = Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
private fun NutritionMetricGrid(values: List<NutritionMetricValue>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        values.chunked(2).forEach { rowValues ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowValues.forEach { (label, value, accent) ->
                    NutritionMetricPill(label, value, accent, modifier = Modifier.weight(1f).fillMaxWidth())
                }
                if (rowValues.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun NutritionMetricPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RecipeActionBottomSheet(
    aiEnabled: Boolean,
    onDismiss: () -> Unit,
    onManualRecipe: () -> Unit,
    onBarcodeIngredient: () -> Unit,
    onPhotoIngredient: () -> Unit,
    onExistingRecipeToMeal: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.spacing.large, vertical = MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BottomSheetHeader(title = "Toevoegen of maken")
        Button(onClick = onManualRecipe, modifier = Modifier.fillMaxWidth()) { Text("Recept handmatig maken") }
        OutlinedButton(onClick = onBarcodeIngredient, modifier = Modifier.fillMaxWidth()) { Text("Barcode-product in recept scannen") }
        OutlinedButton(onClick = onPhotoIngredient, enabled = aiEnabled, modifier = Modifier.fillMaxWidth()) { Text("Product via foto/AI toevoegen") }
        OutlinedButton(onClick = onExistingRecipeToMeal, modifier = Modifier.fillMaxWidth()) { Text("Bestaand recept aan maaltijd toevoegen") }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Sluiten") }
    }
}

@Composable
private fun AddToMealActionSheet(
    mealType: MealType,
    hasSavedFoods: Boolean,
    hasSavedRecipes: Boolean,
    hasDraft: Boolean,
    aiEnabled: Boolean,
    aiContext: String,
    onDismiss: () -> Unit,
    onManualFood: () -> Unit,
    onSavedFood: () -> Unit,
    onRecipe: () -> Unit,
    onAiContextChange: (String) -> Unit,
    onPhotoAi: () -> Unit,
    onOpenMealDraft: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .clearFocusOnTapOutside()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.spacing.large, vertical = MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BottomSheetHeader(title = "Toevoegen aan ${mealType.dutchLabel}")
        Text("Kies een bron en controleer daarna voor opslaan.", color = MaterialTheme.trainIqColors.mutedText)
        Button(onClick = onManualFood, modifier = Modifier.fillMaxWidth()) { Text("Handmatig product maken") }
        OutlinedButton(onClick = onSavedFood, enabled = hasSavedFoods, modifier = Modifier.fillMaxWidth()) { Text("Opgeslagen product gebruiken") }
        OutlinedButton(onClick = onRecipe, enabled = hasSavedRecipes, modifier = Modifier.fillMaxWidth()) { Text("Opgeslagen recept gebruiken") }
        NutritionTextField(
            value = aiContext,
            onValueChange = onAiContextChange,
            label = "AI-context voor foto",
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
        )
        OutlinedButton(onClick = onPhotoAi, enabled = aiEnabled, modifier = Modifier.fillMaxWidth()) { Text("Foto / AI-inschatting") }
        if (hasDraft) {
            OutlinedButton(onClick = onOpenMealDraft, modifier = Modifier.fillMaxWidth()) { Text("Huidige maaltijd controleren") }
        }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Sluiten") }
    }
}

@Composable
private fun BottomSheetHeader(
    title: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WrappingNutritionActions(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        content = content,
    )
}

private data class NutritionHeaderAction(
    val label: String,
    val primary: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

@Composable
private fun EqualNutritionHeaderActions(items: List<NutritionHeaderAction>) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
        items.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                val singleFullWidth = rowItems.size == 1
                rowItems.forEach { item ->
                    NutritionHeaderActionCell(
                        item = item,
                        modifier = if (singleFullWidth) Modifier.fillMaxWidth() else Modifier.weight(1f).fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun NutritionHeaderActionCell(
    item: NutritionHeaderAction,
    modifier: Modifier = Modifier,
) {
    val buttonModifier = modifier.heightIn(min = 48.dp)
    if (item.primary) {
        Button(onClick = item.onClick, enabled = item.enabled, modifier = buttonModifier) {
            Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    } else {
        OutlinedButton(onClick = item.onClick, enabled = item.enabled, modifier = buttonModifier) {
            Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ProductsHeaderCard(
    foodCount: Int,
    onCreateClick: () -> Unit,
    onScanBarcode: () -> Unit,
    onPhotoProduct: () -> Unit,
    aiEnabled: Boolean,
) {
    AppCard(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.trainIqColors.amber) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Producten", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.trainIqColors.amber, fontWeight = FontWeight.SemiBold)
                Text("$foodCount opgeslagen", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text("Gebruik opgeslagen producten direct in maaltijden of recepten.", color = MaterialTheme.trainIqColors.mutedText)
            }
            IconButton(onClick = onCreateClick) {
                Icon(Icons.Rounded.Add, contentDescription = "Product toevoegen")
            }
        }
        EqualNutritionHeaderActions(
            items = listOf(
                NutritionHeaderAction("Product maken", primary = true, onClick = onCreateClick),
                NutritionHeaderAction("Barcode scannen", onClick = onScanBarcode),
                NutritionHeaderAction("Foto/AI product", enabled = aiEnabled, onClick = onPhotoProduct),
            ),
        )
    }
}

@Composable
private fun NutritionNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    imeSettledDelayMillis: Long = 320L,
) {
    TrainIqFormField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier
            .semantics(mergeDescendants = true) { contentDescription = label },
        context = TrainIqFormFieldContext.Nutrition,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        isError = error != null,
        errorText = error,
        imeSettledDelayMillis = imeSettledDelayMillis,
    )
}

@Composable
private fun NutritionTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    error: String? = null,
) {
    TrainIqFormField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier
            .semantics(mergeDescendants = true) { contentDescription = label },
        context = TrainIqFormFieldContext.Nutrition,
        singleLine = singleLine,
        isError = error != null,
        errorText = error,
    )
}

@Composable
private fun EmptyStateCard(title: String, body: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.trainIqColors.mutedText)
        }
    }
}

@Composable
private fun FoodEditorCard(
    foodName: String,
    barcode: String,
    calories: String,
    protein: String,
    carbs: String,
    fat: String,
    defaultServingGrams: String,
    onFoodNameChange: (String) -> Unit,
    onBarcodeChange: (String) -> Unit,
    onCaloriesChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onCarbsChange: (String) -> Unit,
    onFatChange: (String) -> Unit,
    onDefaultServingGramsChange: (String) -> Unit,
    isEditing: Boolean,
    errors: FoodFieldErrors,
    isSaving: Boolean,
    saveToMealOnly: Boolean,
    onScanBarcode: () -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
            Text(if (isEditing) "Product bewerken" else "Product", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Sla producten eenmalig op en voeg ze daarna toe aan maaltijden of gebruik ze als receptingrediënten.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.trainIqColors.mutedText,
            )
            NutritionTextField(value = foodName, onValueChange = onFoodNameChange, label = "Productnaam", modifier = Modifier.fillMaxWidth(), error = errors.name)
            NutritionTextField(value = barcode, onValueChange = onBarcodeChange, label = "Barcode (optioneel)", modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                NutritionNumberField(value = calories, onValueChange = onCaloriesChange, label = "kcal / 100g", modifier = Modifier.weight(1f), error = errors.calories)
                NutritionNumberField(value = protein, onValueChange = onProteinChange, label = "Eiwit / 100g", modifier = Modifier.weight(1f), error = errors.protein)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                NutritionNumberField(value = carbs, onValueChange = onCarbsChange, label = "Kh / 100g", modifier = Modifier.weight(1f), error = errors.carbs)
                NutritionNumberField(value = fat, onValueChange = onFatChange, label = "Vet / 100g", modifier = Modifier.weight(1f), error = errors.fat)
            }
            NutritionNumberField(
                value = defaultServingGrams,
                onValueChange = onDefaultServingGramsChange,
                label = "Standaard hoeveelheid (gram)",
                modifier = Modifier.fillMaxWidth(),
                error = errors.defaultServingGrams,
                imeSettledDelayMillis = 560L,
            )
            WrappingNutritionActions {
                Button(onClick = onSave, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        when {
                            isSaving -> "Opslaan..."
                            saveToMealOnly -> "Alleen aan maaltijd toevoegen"
                            isEditing -> "Wijzigingen opslaan"
                            else -> "Product opslaan"
                        },
                    )
                }
                OutlinedButton(onClick = onScanBarcode, modifier = Modifier.fillMaxWidth()) { Text("Barcode scannen") }
            }
            if (isEditing) {
                TextButton(onClick = onCancelEdit, modifier = Modifier.fillMaxWidth()) { Text("Annuleren en nieuw product") }
            }
            Text(
                "Barcode scannen vult productnaam, kcal en macro's in als Open Food Facts het product kent.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.trainIqColors.mutedText,
            )
        }
    }
}

@Composable
private fun NutritionSavedItemCard(
    title: String,
    nutritionLine: String,
    detailLine: String,
    selected: Boolean,
    primaryLabel: String,
    primaryEnabled: Boolean,
    editLabel: String,
    onPrimaryAction: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    nutritionLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.trainIqColors.mutedText,
                )
                Text(
                    detailLine,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.trainIqColors.amber,
                )
            }
            Button(
                onClick = onPrimaryAction,
                enabled = primaryEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(primaryLabel) }
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Text(editLabel)
                }
                TextButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                    Text("Verwijderen")
                }
            }
        }
    }
}

@Composable
private fun SavedFoodsCard(
    foods: List<FoodItem>,
    searchQuery: String,
    selectedFoodId: Long?,
    isAddPending: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSelect: (Long) -> Unit,
    onQuickAdd: (FoodItem) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val filteredFoods = remember(foods, searchQuery) { foods.filteredByProductQuery(searchQuery) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
            Text("Producten", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            ProductSearchField(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                label = "Zoek producten",
            )
            if (foods.isEmpty()) {
                EmptyStateCard(
                    title = "Nog geen opgeslagen producten",
                    body = "Maak handmatig een product of scan een barcode. Opgeslagen producten blijven herbruikbaar voor maaltijden en recepten.",
                )
            } else {
                if (filteredFoods.isEmpty()) {
                    EmptyStateCard(
                        title = "Geen producten gevonden",
                        body = "Pas je zoekterm aan of voeg een nieuw product toe met de plusknop.",
                    )
                }
                filteredFoods.forEach { food ->
                    NutritionSavedItemCard(
                        title = food.name,
                        nutritionLine = "${formatNumber(food.caloriesPer100g)} kcal/100g - ${nutritionMacroSummary(food.proteinPer100g, food.carbsPer100g, food.fatPer100g)}",
                        detailLine = "Standaard portie: ${formatNumber(food.defaultServingGrams)} g",
                        selected = selectedFoodId == food.id,
                        primaryLabel = savedFoodAddToMealLabel(),
                        primaryEnabled = !isAddPending,
                        editLabel = savedFoodEditLabel(selected = selectedFoodId == food.id),
                        onPrimaryAction = { onQuickAdd(food) },
                        onEdit = { onSelect(food.id) },
                        onDelete = { onDelete(food.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductPickerSheet(
    foods: List<FoodItem>,
    searchQuery: String,
    selectedFoodId: Long?,
    onSearchQueryChange: (String) -> Unit,
    onSelect: (FoodItem) -> Unit,
) {
    val filteredFoods = remember(foods, searchQuery) { foods.filteredByProductQuery(searchQuery) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        Text("Ingrediënt kiezen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        ProductSearchField(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            label = "Zoek opgeslagen producten",
        )
        if (foods.isEmpty()) {
            EmptyStateCard(
                title = "Nog geen producten",
                body = "Maak eerst een product of scan een barcode om het als receptingrediënt te gebruiken.",
            )
        } else if (filteredFoods.isEmpty()) {
            EmptyStateCard(
                title = "Geen producten gevonden",
                body = "Pas je zoekterm aan of voeg het ingrediënt handmatig toe.",
            )
        } else {
            filteredFoods.forEach { food ->
                Surface(
                    onClick = { onSelect(food) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = if (selectedFoodId == food.id) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(food.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${formatNumber(food.caloriesPer100g)} kcal/100g - ${nutritionMacroSummary(food.proteinPer100g, food.carbsPer100g, food.fatPer100g)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.trainIqColors.mutedText,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    label: String,
) {
    NutritionTextField(
        value = query,
        onValueChange = onQueryChange,
        label = label,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun RecipeIngredientEditorSheet(
    ingredientGrams: String,
    quickIngredientName: String,
    quickIngredientBarcode: String,
    quickIngredientKcal: String,
    quickIngredientProtein: String,
    quickIngredientCarbs: String,
    quickIngredientFat: String,
    errors: RecipeFieldErrors,
    quickIngredientErrors: FoodFieldErrors,
    isSaving: Boolean,
    onIngredientGramsChange: (String) -> Unit,
    onQuickIngredientNameChange: (String) -> Unit,
    onQuickIngredientBarcodeChange: (String) -> Unit,
    onQuickIngredientKcalChange: (String) -> Unit,
    onQuickIngredientProteinChange: (String) -> Unit,
    onQuickIngredientCarbsChange: (String) -> Unit,
    onQuickIngredientFatChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onScanBarcode: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        Text("Ingrediënt maken", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "Maak een product aan en voeg het direct met grammen toe aan dit recept.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.trainIqColors.mutedText,
        )
        NutritionTextField(value = quickIngredientName, onValueChange = onQuickIngredientNameChange, label = "Productnaam", modifier = Modifier.fillMaxWidth(), error = quickIngredientErrors.name)
        NutritionTextField(value = quickIngredientBarcode, onValueChange = onQuickIngredientBarcodeChange, label = "Barcode (optioneel)", modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            NutritionNumberField(value = quickIngredientKcal, onValueChange = onQuickIngredientKcalChange, label = "kcal / 100g", modifier = Modifier.weight(1f), error = quickIngredientErrors.calories)
            NutritionNumberField(value = quickIngredientProtein, onValueChange = onQuickIngredientProteinChange, label = "Eiwit / 100g", modifier = Modifier.weight(1f), error = quickIngredientErrors.protein)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            NutritionNumberField(value = quickIngredientCarbs, onValueChange = onQuickIngredientCarbsChange, label = "Kh / 100g", modifier = Modifier.weight(1f), error = quickIngredientErrors.carbs)
            NutritionNumberField(value = quickIngredientFat, onValueChange = onQuickIngredientFatChange, label = "Vet / 100g", modifier = Modifier.weight(1f), error = quickIngredientErrors.fat)
        }
        NutritionNumberField(value = ingredientGrams, onValueChange = onIngredientGramsChange, label = "Gram in recept", modifier = Modifier.fillMaxWidth(), error = errors.ingredientGrams)
        Button(onClick = onSave, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
            Text(if (isSaving) "Opslaan..." else "Product opslaan en toevoegen")
        }
        OutlinedButton(onClick = onScanBarcode, modifier = Modifier.fillMaxWidth()) { Text("Barcode scannen") }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Annuleren") }
    }
}

private fun List<FoodItem>.filteredByProductQuery(query: String): List<FoodItem> {
    val normalized = query.trim().lowercase(Locale.getDefault())
    if (normalized.isBlank()) return this
    return filter { food ->
        food.name.lowercase(Locale.getDefault()).contains(normalized) ||
            food.barcode.orEmpty().lowercase(Locale.getDefault()).contains(normalized) ||
            food.sourceType.name.lowercase(Locale.getDefault()).contains(normalized)
    }
}

private fun List<Recipe>.filteredByRecipeQuery(query: String): List<Recipe> {
    val normalized = query.trim().lowercase(Locale.getDefault())
    if (normalized.isBlank()) return this
    return filter { recipe ->
        recipe.name.lowercase(Locale.getDefault()).contains(normalized) ||
            recipe.notes.orEmpty().lowercase(Locale.getDefault()).contains(normalized) ||
            recipe.ingredients.any { ingredient ->
                ingredient.foodName.lowercase(Locale.getDefault()).contains(normalized)
            }
    }
}

@Composable
private fun RecipeEditorCard(
    recipeName: String,
    recipeNotes: String,
    recipeCookedGrams: String,
    ingredientGrams: String,
    selectedFood: FoodItem?,
    draft: List<Pair<Long, Double>>,
    foods: List<FoodItem>,
    recipeAiContext: String,
    aiEnabled: Boolean,
    isEditing: Boolean,
    errors: RecipeFieldErrors,
    isSaving: Boolean,
    onRecipeNameChange: (String) -> Unit,
    onRecipeNotesChange: (String) -> Unit,
    onRecipeCookedGramsChange: (String) -> Unit,
    onIngredientGramsChange: (String) -> Unit,
    onRecipeAiContextChange: (String) -> Unit,
    onChooseIngredient: () -> Unit,
    onOpenIngredientEditor: () -> Unit,
    onAddIngredient: () -> Unit,
    onRemoveIngredient: (Int) -> Unit,
    onSave: () -> Unit,
    onScanBarcodeForRecipe: () -> Unit,
    onAiVisionForRecipe: () -> Unit,
    onImportPhotoForRecipe: () -> Unit,
    onCancelEdit: () -> Unit,
) {
    val totalNutrition = draft.fold(NutritionFacts.Zero) { acc, (foodId, grams) ->
        val food = foods.firstOrNull { it.id == foodId }
        if (food == null) {
            acc
        } else {
            acc + NutritionFacts(
                calories = food.caloriesPer100g * grams / 100.0,
                protein = food.proteinPer100g * grams / 100.0,
                carbs = food.carbsPer100g * grams / 100.0,
                fat = food.fatPer100g * grams / 100.0,
            )
        }
    }
    val totalGrams = draft.sumOf { it.second }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
            Text(if (isEditing) "Recept bewerken" else "Receptmaker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Maak een herbruikbaar recept met opgeslagen producten, barcode- of handmatige ingrediënten, of gecontroleerde AI-suggesties.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.trainIqColors.mutedText,
            )
            Text("Receptdetails", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            NutritionTextField(value = recipeName, onValueChange = onRecipeNameChange, label = "Receptnaam", modifier = Modifier.fillMaxWidth(), error = errors.name)
            NutritionTextField(value = recipeNotes, onValueChange = onRecipeNotesChange, label = "Notities", modifier = Modifier.fillMaxWidth(), singleLine = false)
            NutritionNumberField(value = recipeCookedGrams, onValueChange = onRecipeCookedGramsChange, label = "Totaal bereid gewicht (optioneel)", modifier = Modifier.fillMaxWidth(), error = errors.cookedGrams)
            HorizontalDivider()
            Text("Ingrediënten", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Text("Ingrediëntbron", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.trainIqColors.mutedText)
            WrappingNutritionActions {
                OutlinedButton(onClick = onChooseIngredient, modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Text("Uit producten")
                }
                OutlinedButton(onClick = onOpenIngredientEditor, modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Text("Nieuw product")
                }
                OutlinedButton(onClick = onScanBarcodeForRecipe, modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Text("Barcode")
                }
                OutlinedButton(onClick = onAiVisionForRecipe, enabled = aiEnabled, modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Text("Foto/AI")
                }
            }
            Text(
                selectedFood?.name?.let { "Gekozen product: $it" } ?: "Kies een product of maak eerst een nieuw product voor dit recept.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.trainIqColors.mutedText,
            )
            if (aiEnabled) {
                NutritionTextField(value = recipeAiContext, onValueChange = onRecipeAiContextChange, label = "AI-context (optioneel)", modifier = Modifier.fillMaxWidth())
            } else {
                Text(
                    "AI-fotoherkenning is pas beschikbaar nadat je dit aanzet in Instellingen. Product kiezen, nieuw product maken en barcodes blijven werken.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.trainIqColors.mutedText,
                )
            }
            NutritionNumberField(
                value = ingredientGrams,
                onValueChange = onIngredientGramsChange,
                label = "Ingrediënt gram",
                modifier = Modifier.fillMaxWidth(),
                error = errors.ingredientGrams,
            )
            Button(onClick = onAddIngredient, enabled = selectedFood != null, modifier = Modifier.fillMaxWidth()) {
                Text("Ingrediënt toevoegen")
            }
            if (draft.isEmpty()) {
                EmptyStateCard(
                    title = errors.ingredients ?: "Nog geen ingrediënten",
                    body = "Voeg minimaal een ingrediënt met grammen toe. Totalen worden live bijgewerkt.",
                )
            } else {
                draft.forEachIndexed { index, (foodId, grams) ->
                    val food = foods.firstOrNull { it.id == foodId }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(food?.name ?: "Product", fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("${formatNumber(grams)}g gebruikt", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.trainIqColors.mutedText)
                            }
                            TextButton(onClick = { onRemoveIngredient(index) }) { Text("Verwijderen") }
                        }
                    }
                }
                RecipeTotalsCard(totalNutrition = totalNutrition, totalGrams = totalGrams)
            }
            Button(onClick = onSave, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) { Text(if (isSaving) "Opslaan..." else if (isEditing) "Wijzigingen opslaan" else "Recept opslaan") }
            if (isEditing) {
                TextButton(onClick = onCancelEdit, modifier = Modifier.fillMaxWidth()) { Text("Annuleren en nieuw recept") }
            }
            OutlinedButton(onClick = onImportPhotoForRecipe, enabled = aiEnabled, modifier = Modifier.fillMaxWidth()) {
                Text("Foto importeren")
            }
        }
    }
}

@Composable
private fun RecipeTotalsCard(title: String = "Recepttotalen", totalNutrition: NutritionFacts, totalGrams: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("${formatNumber(totalNutrition.calories)} kcal · ${formatNumber(totalGrams)}g totaal")
            Text(
                nutritionMacroSummary(totalNutrition.protein, totalNutrition.carbs, totalNutrition.fat),
                color = MaterialTheme.trainIqColors.mutedText,
            )
        }
    }
}

@Composable
private fun SavedRecipesCard(
    recipes: List<Recipe>,
    searchQuery: String,
    selectedRecipeId: Long?,
    mealRecipeGrams: String,
    mealRecipeGramsError: String?,
    isAddPending: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onMealRecipeGramsChange: (String) -> Unit,
    onSelect: (Long) -> Unit,
    onUseInMeal: (Recipe) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val filteredRecipes = recipes.filteredByRecipeQuery(searchQuery)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
            Text("Opgeslagen recepten", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (recipes.isEmpty()) {
                EmptyStateCard(
                    title = "Nog geen herbruikbare recepten",
                    body = "Gebruik + Toevoegen of de maker hieronder om recepten later opnieuw te loggen.",
                )
            } else {
                ProductSearchField(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    label = "Recept zoeken",
                )
                NutritionNumberField(
                    value = mealRecipeGrams,
                    onValueChange = onMealRecipeGramsChange,
                    label = "Gram bij toevoegen aan maaltijd",
                    modifier = Modifier.fillMaxWidth(),
                    error = mealRecipeGramsError,
                )
                if (filteredRecipes.isEmpty()) {
                    EmptyStateCard(
                        title = "Geen recepten gevonden",
                        body = "Pas je zoekterm aan of maak een nieuw recept.",
                    )
                }
                filteredRecipes.forEach { recipe ->
                    NutritionSavedItemCard(
                        title = recipe.name,
                        nutritionLine = "${formatNumber(recipe.totalNutrition.calories)} kcal - ${nutritionMacroSummary(recipe.totalNutrition.protein, recipe.totalNutrition.carbs, recipe.totalNutrition.fat)}",
                        detailLine = "${recipe.ingredients.size} ingrediënten",
                        selected = selectedRecipeId == recipe.id,
                        primaryLabel = "Aan maaltijd toevoegen",
                        primaryEnabled = !isAddPending,
                        editLabel = if (selectedRecipeId == recipe.id) "Bewerken" else "Bewerk",
                        onPrimaryAction = { onUseInMeal(recipe) },
                        onEdit = { onSelect(recipe.id) },
                        onDelete = { onDelete(recipe.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MealDraftReviewCard(
    mealType: MealType,
    mealName: String,
    mealNotes: String,
    mealDraft: List<EditableMealEntryRequest>,
    foods: List<FoodItem>,
    recipes: List<Recipe>,
    errors: MealFieldErrors,
    isSaving: Boolean,
    isEditing: Boolean,
    onMealTypeChange: (MealType) -> Unit,
    onMealNameChange: (String) -> Unit,
    onMealNotesChange: (String) -> Unit,
    onUpdateDraftItemGrams: (Int, String) -> Unit,
    onUpdateDraftItemServingCount: (Int, Int) -> Unit,
    onRemoveDraftItem: (Int) -> Unit,
    onSave: () -> Unit,
) {
    val draftTotals = mealDraft.fold(NutritionFacts.Zero) { acc, entry ->
        val request = entry.request
        val itemNutrition = when (request.itemType) {
            MealEntryType.FOOD -> foods.firstOrNull { it.id == request.referenceId }?.let { food ->
                NutritionFacts(
                    calories = food.caloriesPer100g * request.gramsUsed / 100.0,
                    protein = food.proteinPer100g * request.gramsUsed / 100.0,
                    carbs = food.carbsPer100g * request.gramsUsed / 100.0,
                    fat = food.fatPer100g * request.gramsUsed / 100.0,
                )
            }
            MealEntryType.RECIPE -> recipes.firstOrNull { it.id == request.referenceId }?.let { recipe ->
                val baseGrams = recipe.totalCookedGrams ?: recipe.ingredients.sumOf { it.gramsUsed }
                if (baseGrams > 0.0) {
                    val ratio = request.gramsUsed / baseGrams
                    NutritionFacts(
                        calories = recipe.totalNutrition.calories * ratio,
                        protein = recipe.totalNutrition.protein * ratio,
                        carbs = recipe.totalNutrition.carbs * ratio,
                        fat = recipe.totalNutrition.fat * ratio,
                    )
                } else {
                    null
                }
            }
            MealEntryType.SNAPSHOT -> request.snapshot?.let {
                NutritionFacts(
                    calories = it.calories,
                    protein = it.protein,
                    carbs = it.carbs,
                    fat = it.fat,
                )
            }
        } ?: NutritionFacts.Zero
        acc + NutritionFacts(
            calories = itemNutrition.calories * request.servingCount.coerceAtLeast(1),
            protein = itemNutrition.protein * request.servingCount.coerceAtLeast(1),
            carbs = itemNutrition.carbs * request.servingCount.coerceAtLeast(1),
            fat = itemNutrition.fat * request.servingCount.coerceAtLeast(1),
        )
    }.rounded()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
            Text("Maaltijd controleren", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Controleer het maaltijdmoment, de naam en de items voordat je ze opslaat in het overzicht van vandaag.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.trainIqColors.mutedText,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK).forEach { type ->
                    FilterChip(
                        selected = mealType == type,
                        onClick = { onMealTypeChange(type) },
                        label = { Text(type.dutchLabel) },
                    )
                }
            }
            Text("Wordt gelogd onder ${mealType.dutchLabel}.", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            NutritionTextField(value = mealName, onValueChange = onMealNameChange, label = "Maaltijdnaam", modifier = Modifier.fillMaxWidth(), error = errors.name)
            NutritionTextField(value = mealNotes, onValueChange = onMealNotesChange, label = "Notities", modifier = Modifier.fillMaxWidth(), singleLine = false)
            errors.items?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            mealDraft.forEachIndexed { index, entry ->
                val request = entry.request
                val label = when (request.itemType) {
                    MealEntryType.FOOD -> foods.firstOrNull { it.id == request.referenceId }?.name
                    MealEntryType.RECIPE -> recipes.firstOrNull { it.id == request.referenceId }?.name
                    MealEntryType.SNAPSHOT -> request.snapshot?.name
                } ?: "Item"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(label, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            NutritionNumberField(
                                value = entry.gramsText,
                                onValueChange = { onUpdateDraftItemGrams(index, it) },
                                label = "Gram per portie",
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(onClick = { onUpdateDraftItemServingCount(index, request.servingCount - 1) }, enabled = request.servingCount > 1) {
                                    Text("-")
                                }
                                Text("${request.servingCount.coerceAtLeast(1)}x", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                OutlinedButton(onClick = { onUpdateDraftItemServingCount(index, request.servingCount + 1) }) {
                                    Text("+")
                                }
                            }
                        }
                        TextButton(onClick = { onRemoveDraftItem(index) }) { Text("Verwijderen") }
                    }
                }
            }
            RecipeTotalsCard(title = "Maaltijdtotalen", totalNutrition = draftTotals, totalGrams = mealDraft.sumOf { it.request.gramsUsed * it.request.servingCount.coerceAtLeast(1) })
            Button(onClick = onSave, enabled = mealDraft.isNotEmpty() && !isSaving, modifier = Modifier.fillMaxWidth()) {
                Text(if (isSaving) "Opslaan..." else mealDraftSaveLabel(isEditing))
            }
        }
    }
}

@Composable
private fun AiMealAnalysisCard(
    aiPreferences: AiPreferences,
    target: NutritionAiResultTarget,
    primaryLabel: String,
    aiContext: String,
    editableItems: List<EditableAiItem>,
    itemErrors: Map<Int, AiItemFieldErrors>,
    isSaving: Boolean,
    isAnalyzing: Boolean,
    onContextChange: (String) -> Unit,
    onOpenCamera: () -> Unit,
    onImportPhoto: () -> Unit,
    onChangeItem: (Int, EditableAiItem) -> Unit,
    onDeleteItem: (Int) -> Unit,
    onPrimaryAction: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
            Text("Foto / AI-controle", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                when (target) {
                    NutritionAiResultTarget.MealDraft -> "Maaltijd scannen"
                    NutritionAiResultTarget.ProductLibrary -> "Product scannen"
                    NutritionAiResultTarget.RecipeDraft -> "Receptingrediënten scannen"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                when {
                    !aiPreferences.enabled -> "AI staat uit in Instellingen. Handmatig voeding loggen blijft werken."
                    !aiPreferences.hasAnyReadyProvider() -> "Voeg een Gemini of OpenAI API-sleutel toe in Instellingen om maaltijdanalyse te gebruiken."
                    target == NutritionAiResultTarget.ProductLibrary -> "Controleer de AI-inschatting en sla de overgebleven items op als producten."
                    target == NutritionAiResultTarget.RecipeDraft -> "Controleer de AI-inschatting en voeg de overgebleven items toe als receptingrediënten."
                    else -> "Geef context mee als je weet wat erin zit. TrainIQ gebruikt je tekst als waarheid en de foto voor ontbrekende details."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            NutritionTextField(value = aiContext, onValueChange = onContextChange, label = "Optionele context", modifier = Modifier.fillMaxWidth())
            Button(onClick = onOpenCamera, enabled = aiPreferences.hasAnyReadyProvider(), modifier = Modifier.fillMaxWidth()) {
                Text("Scanner openen")
            }
            OutlinedButton(onClick = onImportPhoto, enabled = aiPreferences.hasAnyReadyProvider(), modifier = Modifier.fillMaxWidth()) {
                Text("Foto importeren")
            }
            if (isAnalyzing) {
                Text(aiMealAnalyzingLabel(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                repeat(3) {
                    ShimmerCardPlaceholder(lineCount = 2)
                }
            }
            editableItems.forEachIndexed { index, item ->
                EditableAiItemCard(item = item, errors = itemErrors[index] ?: AiItemFieldErrors(), onChange = { onChangeItem(index, it) }, onDelete = { onDeleteItem(index) })
            }
            if (editableItems.isNotEmpty()) {
                Button(onClick = onPrimaryAction, enabled = !isSaving) { Text(if (isSaving) "Toevoegen..." else primaryLabel) }
            }
        }
    }
}

@Composable
private fun RecipeAiResultsCard(
    editableItems: List<EditableAiItem>,
    itemErrors: Map<Int, AiItemFieldErrors>,
    isSaving: Boolean,
    onChangeItem: (Int, EditableAiItem) -> Unit,
    onDeleteItem: (Int) -> Unit,
    onAddAsIngredients: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
            Text("AI-gevonden ingrediënten", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Controleer elk item en voeg ze daarna toe aan je recept.", style = MaterialTheme.typography.bodyMedium)
            editableItems.forEachIndexed { index, item ->
                EditableAiItemCard(item = item, errors = itemErrors[index] ?: AiItemFieldErrors(), onChange = { onChangeItem(index, it) }, onDelete = { onDeleteItem(index) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAddAsIngredients, enabled = !isSaving) { Text(if (isSaving) "Opslaan..." else "Als ingrediënten toevoegen") }
                TextButton(onClick = onDismiss) { Text("Sluiten") }
            }
        }
    }
}

@Composable
private fun MealHistoryCard(
    meals: List<LoggedMeal>,
    foods: List<FoodItem>,
    recipes: List<Recipe>,
    onReuseMeal: (LoggedMeal) -> Unit,
    onDeleteMeal: (Long) -> Unit,
) {
    val historyDays = meals.groupedHistoryDays()
    var expandedHistoryDayKey by rememberSaveable { mutableStateOf<String?>(historyDays.firstOrNull()?.key) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
            Text("Voedingshistorie", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Eerder gelogde maaltijden blijven voedingssnapshots en staan per dag samengevat.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.trainIqColors.mutedText)
            if (historyDays.isEmpty()) {
                EmptyStateCard(
                    title = "Nog geen gelogde maaltijden",
                    body = "Geloge maaltijden verschijnen hier per dag als samenvatting.",
                )
            } else {
                historyDays.forEach { day ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top,
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(day.dateLabel, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${day.mealCount} maaltijden • ${day.itemCount} items • ${day.mealTypeSummary}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.trainIqColors.mutedText,
                                    )
                                }
                                IconButton(onClick = {
                                    expandedHistoryDayKey = if (expandedHistoryDayKey == day.key) null else day.key
                                }) {
                                    Icon(
                                        if (expandedHistoryDayKey == day.key) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                        contentDescription = if (expandedHistoryDayKey == day.key) "Verbergen" else "Maaltijden bekijken",
                                    )
                                }
                            }
                            NutritionMetricStrip(
                                values = listOf(
                                    NutritionMetricValue("Kcal", formatNumber(day.totalNutrition.calories), MaterialTheme.colorScheme.primary),
                                    NutritionMetricValue("Eiwit", "${formatNumber(day.totalNutrition.protein)} g", MaterialTheme.trainIqColors.mint),
                                    NutritionMetricValue("Kh", "${formatNumber(day.totalNutrition.carbs)} g", MaterialTheme.trainIqColors.blue),
                                    NutritionMetricValue("Vet", "${formatNumber(day.totalNutrition.fat)} g", MaterialTheme.colorScheme.tertiary),
                                ),
                            )
                            TextButton(
                                onClick = { expandedHistoryDayKey = if (expandedHistoryDayKey == day.key) null else day.key },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (expandedHistoryDayKey == day.key) "Verbergen" else "Maaltijden bekijken")
                            }
                            if (expandedHistoryDayKey == day.key) {
                                day.meals.forEach { meal ->
                                    MealHistoryDetailCard(
                                        meal = meal,
                                        foods = foods,
                                        recipes = recipes,
                                        onReuseMeal = onReuseMeal,
                                        onDeleteMeal = onDeleteMeal,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealHistoryDetailCard(
    meal: LoggedMeal,
    foods: List<FoodItem>,
    recipes: List<Recipe>,
    onReuseMeal: (LoggedMeal) -> Unit,
    onDeleteMeal: (Long) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    meal.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                AppChip(label = meal.mealType.dutchLabel, accent = MaterialTheme.colorScheme.primary)
            }
            NutritionMetricGrid(
                values = listOf(
                    NutritionMetricValue("Kcal", formatNumber(meal.totalNutrition.calories), MaterialTheme.colorScheme.primary),
                    NutritionMetricValue("Eiwit", "${formatNumber(meal.totalNutrition.protein)} g", MaterialTheme.trainIqColors.mint),
                    NutritionMetricValue("Kh", "${formatNumber(meal.totalNutrition.carbs)} g", MaterialTheme.trainIqColors.blue),
                    NutritionMetricValue("Vet", "${formatNumber(meal.totalNutrition.fat)} g", MaterialTheme.colorScheme.tertiary),
                ),
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                meal.items.forEach { item ->
                    val liveName = when (item.itemType) {
                        LoggedMealItemType.FOOD -> foods.firstOrNull { it.id == item.referenceId }?.name
                        LoggedMealItemType.RECIPE -> recipes.firstOrNull { it.id == item.referenceId }?.name
                        LoggedMealItemType.SNAPSHOT -> null
                    } ?: item.name
                    MealHistoryItemRow(item = item, itemName = liveName)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { onReuseMeal(meal) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text("Opnieuw gebruiken", maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                }
                TextButton(onClick = { onDeleteMeal(meal.id) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text("Verwijderen", maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun MealHistoryItemRow(
    item: LoggedMealItem,
    itemName: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(itemName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${formatNumber(item.gramsUsed)} g x${item.servingCount.coerceAtLeast(1)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.trainIqColors.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "${formatNumber(item.nutritionSnapshot.calories)} kcal",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
            )
        }
    }
}

internal data class NutritionHistoryDay(
    val key: String,
    val dateLabel: String,
    val meals: List<LoggedMeal>,
    val totalNutrition: NutritionFacts,
    val mealCount: Int,
    val itemCount: Int,
    val mealTypeSummary: String,
)

internal fun List<LoggedMeal>.groupedHistoryDays(): List<NutritionHistoryDay> {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("nl-NL"))
    val zone = ZoneId.systemDefault()
    return sortedByDescending { it.timestamp }
        .groupBy { meal ->
            Instant.ofEpochMilli(meal.timestamp).atZone(zone).toLocalDate()
        }
        .map { (date, dayMeals) ->
            val sortedMeals = dayMeals.sortedBy { it.timestamp }
            val totalNutrition = sortedMeals.fold(NutritionFacts.Zero) { acc, meal -> acc + meal.totalNutrition }.rounded()
            val mealTypes = listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK)
                .filter { type -> sortedMeals.any { it.mealType == type } }
                .joinToString(", ") { it.dutchLabel }
            NutritionHistoryDay(
                key = date.toString(),
                dateLabel = date.format(formatter),
                meals = sortedMeals,
                totalNutrition = totalNutrition,
                mealCount = sortedMeals.size,
                itemCount = sortedMeals.sumOf { it.items.size },
                mealTypeSummary = mealTypes.ifBlank { "Geen maaltijdtype" },
            )
        }
}

@Composable
private fun EditableAiItemCard(item: EditableAiItem, errors: AiItemFieldErrors, onChange: (EditableAiItem) -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NutritionTextField(value = item.name, onValueChange = { onChange(item.copy(name = it)) }, label = "Naam", modifier = Modifier.fillMaxWidth(), error = errors.name)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                NutritionNumberField(value = item.grams, onValueChange = { onChange(item.copy(grams = it)) }, label = "Grammen", modifier = Modifier.weight(1f), error = errors.grams)
                NutritionNumberField(value = item.calories, onValueChange = { onChange(item.copy(calories = it)) }, label = "Calorieën", modifier = Modifier.weight(1f), error = errors.calories)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                NutritionNumberField(value = item.protein, onValueChange = { onChange(item.copy(protein = it)) }, label = "Eiwit", modifier = Modifier.weight(1f), error = errors.protein)
                NutritionNumberField(value = item.carbs, onValueChange = { onChange(item.copy(carbs = it)) }, label = "Koolhydraten", modifier = Modifier.weight(1f), error = errors.carbs)
                NutritionNumberField(value = item.fat, onValueChange = { onChange(item.copy(fat = it)) }, label = "Vet", modifier = Modifier.weight(1f), error = errors.fat)
            }
            item.confidence?.let { Text("Zekerheid: ${it.toDutchConfidenceLabel()}") }
            item.notes?.let { Text(it) }
            Text("Per 100g: ${per100Value(item.calories, item.grams.toNutritionNumberOrNull(max = 100_000.0) ?: 100.0)} kcal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.trainIqColors.mutedText)
            TextButton(onClick = onDelete) { Text("Item verwijderen") }
        }
    }
}

internal data class EditableAiItem(
    val name: String,
    val grams: String,
    val calories: String,
    val protein: String,
    val carbs: String,
    val fat: String,
    val confidence: String?,
    val notes: String?,
) {
    companion object {
        fun from(item: MealScanItem) = EditableAiItem(
            name = item.name,
            grams = formatNumber(item.estimatedGrams),
            calories = formatNumber(item.nutrition.calories),
            protein = formatNumber(item.nutrition.protein),
            carbs = formatNumber(item.nutrition.carbs),
            fat = formatNumber(item.nutrition.fat),
            confidence = item.confidence,
            notes = item.notes,
        )
    }
}

private data class AiBatchItem(
    val item: EditableAiItem,
    val grams: Double,
)

private data class EditableMealEntryRequest(
    val request: MealEntryRequest,
    val gramsText: String = formatNumber(request.gramsUsed),
)

private fun MealEntryRequest.toEditableMealEntryRequest(): EditableMealEntryRequest =
    EditableMealEntryRequest(request = this)

private fun List<EditableMealEntryRequest>.toMealEntryRequestsOrNull(): List<MealEntryRequest>? =
    map { entry ->
        val grams = entry.gramsText.toNutritionNumberOrNull(max = 100_000.0)
            ?.takeIf { it > 0.0 }
            ?: return null
        entry.request.copy(gramsUsed = grams)
    }

private fun AiBatchItem.toSnapshotMealEntry(): MealEntryRequest {
    val nutrition = MealEntrySnapshot(
        name = item.name.trim().ifBlank { "AI-item" },
        calories = item.calories.toNutritionNumberOrNull(max = 100_000.0) ?: 0.0,
        protein = item.protein.toNutritionNumberOrNull(max = 100_000.0) ?: 0.0,
        carbs = item.carbs.toNutritionNumberOrNull(max = 100_000.0) ?: 0.0,
        fat = item.fat.toNutritionNumberOrNull(max = 100_000.0) ?: 0.0,
    )
    return MealEntryRequest(
        itemType = MealEntryType.SNAPSHOT,
        referenceId = 0L,
        gramsUsed = grams,
        notes = item.notes?.ifBlank { null },
        snapshot = nutrition,
    )
}

private fun createSnapshotMealEntry(
    name: String,
    grams: Double,
    caloriesPer100g: Double,
    proteinPer100g: Double,
    carbsPer100g: Double,
    fatPer100g: Double,
): MealEntryRequest {
    val ratio = grams / 100.0
    return MealEntryRequest(
        itemType = MealEntryType.SNAPSHOT,
        referenceId = 0L,
        gramsUsed = grams,
        snapshot = MealEntrySnapshot(
            name = name.trim().ifBlank { "Tijdelijk product" },
            calories = caloriesPer100g * ratio,
            protein = proteinPer100g * ratio,
            carbs = carbsPer100g * ratio,
            fat = fatPer100g * ratio,
        ),
    )
}

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.1f", value)

private fun formatNullableNumber(value: Double?): String = value?.let(::formatNumber).orEmpty()

internal fun nutritionMacroSummary(protein: Double, carbs: Double, fat: Double): String =
    "Eiwit ${formatNumber(protein)} g - Kh ${formatNumber(carbs)} g - Vet ${formatNumber(fat)} g"

internal fun mealSectionEmptyText(mealType: MealType): String =
    "Nog niets gelogd voor ${mealType.dutchLabel.lowercase()}. Gebruik de plusknop om iets te loggen."

internal fun mealSectionAddContentDescription(mealType: MealType): String =
    "Toevoegen aan ${mealType.dutchLabel}"

internal fun aiMealAnalyzingLabel(): String = "Maaltijd analyseren..."

internal fun nutritionTabTitles(): List<String> =
    listOf("Vandaag", "Producten", "Recepten", "Historie")

private data class NutritionSectionTab(val title: String, val index: Int)

private fun nutritionSectionTabs(): List<NutritionSectionTab> =
    listOf(
        NutritionSectionTab("Vandaag", 0),
        NutritionSectionTab("Producten", 4),
        NutritionSectionTab("Recepten", 3),
        NutritionSectionTab("Historie", 5),
    )

private fun nutritionInternalTabCount(): Int = 6

internal fun nutritionSectionMenuButtonDescription(): String = "Voeding secties openen"

internal fun nutritionSectionMenuButtonLabel(): String = "Secties"

internal fun mealEditActionLabel(): String = "Hoeveelheid wijzigen"

internal fun mealDraftSaveLabel(isEditing: Boolean): String =
    if (isEditing) "Wijzigingen opslaan" else "Maaltijd opslaan"

internal fun savedFoodAddToMealLabel(): String = "Aan maaltijd toevoegen"

internal fun savedFoodEditLabel(selected: Boolean): String =
    if (selected) "Wordt bewerkt" else "Bewerken"

private fun LoggedMealItemType.toMealEntryType(): MealEntryType = when (this) {
    LoggedMealItemType.FOOD -> MealEntryType.FOOD
    LoggedMealItemType.RECIPE -> MealEntryType.RECIPE
    LoggedMealItemType.SNAPSHOT -> MealEntryType.SNAPSHOT
}

private fun LoggedMealItem.toMealEntrySnapshot(): MealEntrySnapshot =
    MealEntrySnapshot(
        name = name,
        calories = nutritionSnapshot.calories,
        protein = nutritionSnapshot.protein,
        carbs = nutritionSnapshot.carbs,
        fat = nutritionSnapshot.fat,
    )

internal fun nutritionEnergyProgressFraction(calories: Double, energyBalance: EnergyBalanceSnapshot?): Float {
    val target = energyBalance?.caloriesOut?.takeIf { it > 0 } ?: return 0f
    return (calories / target.toDouble()).toFloat().coerceIn(0f, 1f)
}

internal fun nutritionEnergyBalanceSummary(energyBalance: EnergyBalanceSnapshot): String {
    val label = when {
        energyBalance.balance < 0 -> "tekort"
        energyBalance.balance > 0 -> "overschot"
        else -> "in balans"
    }
    return "Netto calorieën ${abs(energyBalance.balance)} kcal $label"
}

internal fun nutritionEnergyBreakdownText(energyBalance: EnergyBalanceSnapshot): String =
    "TEF ${energyBalance.tefCalories} kcal - NEAT ${energyBalance.neatCalories} kcal - Training ${energyBalance.workoutCalories} kcal"

private fun per100Value(total: String, grams: Double): String {
    return formatNumber(per100Number(total, grams))
}

private fun per100Number(total: String, grams: Double): Double {
    val parsed = total.toNutritionNumberOrNull(max = 100_000.0) ?: 0.0
    return if (grams <= 0.0) 0.0 else parsed / grams * 100.0
}

private fun buildValidAiBatchItems(items: List<EditableAiItem>): List<AiBatchItem>? {
    val batchItems = items.mapNotNull { item ->
        val grams = item.grams.toNutritionNumberOrNull(max = 100_000.0) ?: return@mapNotNull null
        val caloriesPer100g = per100Number(item.calories, grams)
        val proteinPer100g = per100Number(item.protein, grams)
        val carbsPer100g = per100Number(item.carbs, grams)
        val fatPer100g = per100Number(item.fat, grams)
        if (
            caloriesPer100g !in 0.0..5000.0 ||
            proteinPer100g !in 0.0..1000.0 ||
            carbsPer100g !in 0.0..1000.0 ||
            fatPer100g !in 0.0..1000.0
        ) {
            return@mapNotNull null
        }
        AiBatchItem(item, grams)
    }
    return batchItems.takeIf { it.size == items.size }
}

private fun aiBatchNutritionErrors(itemCount: Int): Map<Int, AiItemFieldErrors> =
    (0 until itemCount).associateWith {
        AiItemFieldErrors(calories = "Controleer portie en voedingswaarden.")
    }

private fun String.toDutchConfidenceLabel(): String = when (trim().lowercase()) {
    "low" -> "laag"
    "medium" -> "gemiddeld"
    "high" -> "hoog"
    else -> this
}

private val MealType.dutchLabel: String
    get() = when (this) {
        MealType.BREAKFAST -> "Ochtend"
        MealType.LUNCH -> "Middag"
        MealType.DINNER -> "Avond"
        MealType.SNACK -> "Snacks"
    }
