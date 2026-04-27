package com.trainiq.features.nutrition

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.trainiq.core.datastore.AiPreferences
import com.trainiq.core.datastore.UserPreferencesRepository
import com.trainiq.core.theme.spacing
import com.trainiq.core.ui.MessageCard
import com.trainiq.core.ui.ScreenHeader
import com.trainiq.core.ui.ShimmerCardPlaceholder
import com.trainiq.core.ui.AppCard
import com.trainiq.core.ui.AppChip
import com.trainiq.core.ui.AppLinearProgress
import com.trainiq.core.ui.bringIntoViewOnFocus
import com.trainiq.core.ui.clearFocusOnScrollOrDrag
import com.trainiq.core.theme.trainIqColors
import com.trainiq.domain.model.FoodItem
import com.trainiq.domain.model.FoodSourceType
import com.trainiq.domain.model.LoggedMeal
import com.trainiq.domain.model.MealAnalysisResult
import com.trainiq.domain.model.MealScanItem
import com.trainiq.domain.model.MealType
import com.trainiq.domain.model.NutritionOverview
import com.trainiq.domain.model.NutritionFacts
import com.trainiq.domain.model.Recipe
import com.trainiq.domain.model.rounded
import com.trainiq.domain.repository.MealEntryRequest
import com.trainiq.domain.repository.MealEntryType
import com.trainiq.domain.usecase.AnalyzeMealUseCase
import com.trainiq.domain.usecase.ClearLastScanResultUseCase
import com.trainiq.domain.usecase.DeleteFoodUseCase
import com.trainiq.domain.usecase.DeleteMealUseCase
import com.trainiq.domain.usecase.DeleteRecipeUseCase
import com.trainiq.domain.usecase.ObserveNutritionUseCase
import com.trainiq.domain.usecase.SaveFoodItemUseCase
import com.trainiq.domain.usecase.SaveMealUseCase
import com.trainiq.domain.usecase.SaveRecipeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ScanTarget { FOOD_EDITOR, RECIPE_DRAFT }

private sealed interface PendingNutritionDelete {
    data class Meal(val id: Long) : PendingNutritionDelete
    data class Food(val id: Long) : PendingNutritionDelete
    data class Recipe(val id: Long) : PendingNutritionDelete
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
        val pendingSubmits: Set<NutritionSubmitKey> = emptySet(),
    ) : NutritionUiState
    data class Error(val message: String) : NutritionUiState
}

@HiltViewModel
class NutritionViewModel @Inject constructor(
    observeNutritionUseCase: ObserveNutritionUseCase,
    preferencesRepository: UserPreferencesRepository,
    private val analyzeMealUseCase: AnalyzeMealUseCase,
    private val saveFoodItemUseCase: SaveFoodItemUseCase,
    private val saveRecipeUseCase: SaveRecipeUseCase,
    private val saveMealUseCase: SaveMealUseCase,
    private val deleteMealUseCase: DeleteMealUseCase,
    private val deleteFoodUseCase: DeleteFoodUseCase,
    private val deleteRecipeUseCase: DeleteRecipeUseCase,
    private val clearLastScanResultUseCase: ClearLastScanResultUseCase,
) : ViewModel() {
    private data class NutritionEphemeralState(
        val scanResult: MealAnalysisResult? = null,
        val message: String? = null,
        val isAnalyzing: Boolean = false,
        val scanTarget: ScanTarget = ScanTarget.FOOD_EDITOR,
        val pendingSubmits: Set<NutritionSubmitKey> = emptySet(),
    )

    private val overview: StateFlow<NutritionOverview?> = observeNutritionUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val aiPreferences: StateFlow<AiPreferences> = preferencesRepository.aiPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AiPreferences(false, ""))
    private val ephemeral = MutableStateFlow(NutritionEphemeralState())

    val uiState: StateFlow<NutritionUiState> = combine(overview, aiPreferences, ephemeral) { currentOverview, currentPreferences, temp ->
        when {
            currentOverview == null -> NutritionUiState.Loading
            else -> NutritionUiState.Success(
                overview = currentOverview,
                aiPreferences = currentPreferences,
                scanResult = temp.scanResult ?: currentOverview.scannedResult,
                message = temp.message,
                isAnalyzing = temp.isAnalyzing,
                scanTarget = temp.scanTarget,
                pendingSubmits = temp.pendingSubmits,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NutritionUiState.Loading)

    fun analyze(path: String, context: String, capturedAtMillis: Long) {
        viewModelScope.launch {
            val ai = aiPreferences.value
            if (!ai.enabled) {
                ephemeral.update { it.copy(message = "AI staat uit in Instellingen. Voeding werkt nog steeds volledig met handmatige invoer.") }
                return@launch
            }
            if (ai.apiKey.isBlank()) {
                ephemeral.update { it.copy(message = "Er is geen Gemini API-sleutel ingesteld. Voeg er een toe in Instellingen of blijf handmatig werken.") }
                return@launch
            }
            ephemeral.update { it.copy(isAnalyzing = true, message = null) }
            runCatching { analyzeMealUseCase(path, context, capturedAtMillis) }
                .onSuccess {
                    ephemeral.update { state ->
                        state.copy(
                            scanResult = it,
                            message = if (it.items.isEmpty()) {
                                "Er kwam geen betrouwbare maaltijdinschatting terug. Probeer opnieuw of voer de maaltijd handmatig in."
                            } else {
                                it.notes ?: "Controleer de AI-inschatting voordat je die aan je maaltijd toevoegt."
                            },
                            isAnalyzing = false,
                        )
                    }
                }
                .onFailure {
                    ephemeral.update { it.copy(message = "Maaltijdanalyse mislukt. Probeer opnieuw of ga verder met handmatige invoer.", isAnalyzing = false) }
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
        sourceType: FoodSourceType,
        onSaved: (FoodItem) -> Unit = {},
        onFailure: (Throwable) -> Unit = {},
    ) {
        val usesFoodGuard = sourceType != FoodSourceType.AI
        if (usesFoodGuard && NutritionSubmitKey.Food in ephemeral.value.pendingSubmits) return
        val errors = validateFoodInput(name, calories, protein, carbs, fat)
        if (errors.hasErrors) {
            ephemeral.update { it.copy(message = "Vul eerst een naam en niet-negatieve waarden per 100g in.") }
            return
        }
        val parsedCalories = calories.toNutritionNumberOrNull(max = 5000.0) ?: return
        val parsedProtein = protein.toNutritionNumberOrNull(max = 1000.0) ?: return
        val parsedCarbs = carbs.toNutritionNumberOrNull(max = 1000.0) ?: return
        val parsedFat = fat.toNutritionNumberOrNull(max = 1000.0) ?: return
        if (usesFoodGuard) {
            ephemeral.update { it.copy(pendingSubmits = it.pendingSubmits + NutritionSubmitKey.Food, message = null) }
        }
        viewModelScope.launch {
            try {
                val item = saveFoodItemUseCase(id, name.trim(), barcode?.trim()?.ifBlank { null }, parsedCalories, parsedProtein, parsedCarbs, parsedFat, sourceType)
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
            runCatching { saveMealUseCase(id, mealType, name.trim(), notes.trim(), items) }
                .onSuccess {
                    ephemeral.update { it.copy(message = "Maaltijd opgeslagen.") }
                    onSaved()
                }
                .onFailure {
                    ephemeral.update { it.copy(message = "Deze maaltijd bevat een verwijderd product of recept. Verwijder het item uit je concept en probeer opnieuw.") }
                }
            ephemeral.update { it.copy(pendingSubmits = it.pendingSubmits - NutritionSubmitKey.Meal) }
        }
    }

    fun deleteMeal(mealId: Long) {
        if (NutritionSubmitKey.Delete in ephemeral.value.pendingSubmits) return
        ephemeral.update { it.copy(pendingSubmits = it.pendingSubmits + NutritionSubmitKey.Delete, message = null) }
        viewModelScope.launch {
            try {
                deleteMealUseCase(mealId)
                ephemeral.update { it.copy(message = "Maaltijd verwijderd.") }
            } finally {
                ephemeral.update { it.copy(pendingSubmits = it.pendingSubmits - NutritionSubmitKey.Delete) }
            }
        }
    }

    fun deleteFood(foodId: Long) {
        if (overview.value?.recipes.orEmpty().any { recipe -> recipe.ingredients.any { it.foodItemId == foodId } }) {
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
}

@Composable
fun NutritionRoute(
    onOpenAiScanner: (String) -> Unit,
    onOpenBarcodeScanner: () -> Unit,
    pendingBarcode: String? = null,
    onBarcodeClear: () -> Unit = {},
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
        onSetMessage = viewModel::setMessage,
        onDismissMessage = { viewModel.setMessage(null) },
        onOpenAiScanner = onOpenAiScanner,
        onOpenBarcodeScanner = onOpenBarcodeScanner,
        pendingBarcode = pendingBarcode,
        onBarcodeClear = onBarcodeClear,
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    uiState: NutritionUiState,
    onSaveFood: (Long?, String, String?, String, String, String, String, FoodSourceType, (FoodItem) -> Unit, (Throwable) -> Unit) -> Unit,
    onSaveRecipe: (Long?, String, String, String, List<Pair<Long, Double>>, () -> Unit) -> Unit,
    onSaveMeal: (Long?, MealType, String, String, List<MealEntryRequest>, () -> Unit) -> Unit,
    onDeleteMeal: (Long) -> Unit,
    onDeleteFood: (Long) -> Unit,
    onDeleteRecipe: (Long) -> Unit,
    onTryStartAiBatchSave: () -> Boolean,
    onFinishAiBatchSave: () -> Unit,
    onSetScanResult: (MealAnalysisResult?) -> Unit,
    onSetScanTarget: (ScanTarget) -> Unit = {},
    onSetMessage: (String?) -> Unit,
    onDismissMessage: () -> Unit,
    onOpenAiScanner: (String) -> Unit,
    onOpenBarcodeScanner: () -> Unit,
    pendingBarcode: String? = null,
    onBarcodeClear: () -> Unit = {},
) {
    val successState = uiState as? NutritionUiState.Success
    val overview = successState?.overview
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
    val nutritionListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val recipeActionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val addToMealSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableStateOf(0) }
    var aiScanForRecipe by remember { mutableStateOf(false) }
    var showAddToMealActions by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<PendingNutritionDelete?>(null) }
    var addToMealType by remember { mutableStateOf(MealType.BREAKFAST) }
    var selectedFoodId by remember { mutableStateOf<Long?>(null) }
    var selectedRecipeId by remember { mutableStateOf<Long?>(null) }
    val selectedFood = overview?.foods?.firstOrNull { it.id == selectedFoodId }
    val selectedRecipe = overview?.recipes?.firstOrNull { it.id == selectedRecipeId }

    var foodName by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var foodErrors by remember { mutableStateOf(FoodFieldErrors()) }

    var recipeName by remember { mutableStateOf("") }
    var recipeNotes by remember { mutableStateOf("") }
    var recipeCookedGrams by remember { mutableStateOf("") }
    var ingredientGrams by remember { mutableStateOf("100") }
    var recipeAiContext by remember { mutableStateOf("") }
    var quickIngredientName by remember { mutableStateOf("") }
    var quickIngredientBarcode by remember { mutableStateOf("") }
    var quickIngredientKcal by remember { mutableStateOf("") }
    var quickIngredientProtein by remember { mutableStateOf("") }
    var quickIngredientCarbs by remember { mutableStateOf("") }
    var quickIngredientFat by remember { mutableStateOf("") }
    var showRecipeActions by remember { mutableStateOf(false) }
    val recipeDraft = remember { mutableStateListOf<Pair<Long, Double>>() }
    var recipeErrors by remember { mutableStateOf(RecipeFieldErrors()) }
    var quickIngredientErrors by remember { mutableStateOf(FoodFieldErrors()) }

    var mealType by remember { mutableStateOf(MealType.LUNCH) }
    var mealName by remember { mutableStateOf(MealType.LUNCH.dutchLabel) }
    var mealNotes by remember { mutableStateOf("") }
    var mealFoodGrams by remember { mutableStateOf("100") }
    var mealRecipeGrams by remember { mutableStateOf("150") }
    var editingMealId by remember { mutableStateOf<Long?>(null) }
    val mealDraft = remember { mutableStateListOf<MealEntryRequest>() }
    var mealErrors by remember { mutableStateOf(MealFieldErrors()) }
    var mealFoodGramsErrors by remember { mutableStateOf(QuickAddFieldErrors()) }
    var mealRecipeGramsErrors by remember { mutableStateOf(QuickAddFieldErrors()) }

    var aiContext by remember { mutableStateOf("") }
    val editableAiItems = remember { mutableStateListOf<EditableAiItem>() }
    var aiItemErrors by remember { mutableStateOf<Map<Int, AiItemFieldErrors>>(emptyMap()) }
    var aiSaveProgress by remember { mutableStateOf(startAiBatchSaveProgress(0)) }
    val isAiSaving = !aiSaveProgress.isFinished || isAiBatchPending

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

    LaunchedEffect(pendingBarcode) {
        if (pendingBarcode != null) {
            if (successState?.scanTarget == ScanTarget.RECIPE_DRAFT) {
                quickIngredientBarcode = pendingBarcode
                selectedTab = 1
                onDismissMessage()
                quickIngredientName = quickIngredientName.ifBlank { "Gescand product" }
            } else {
                barcode = pendingBarcode
                selectedTab = 2
            }
            onSetScanTarget(ScanTarget.FOOD_EDITOR)
            onBarcodeClear()
        }
    }

    LaunchedEffect(selectedFood?.id) {
        selectedFood?.let {
            foodName = it.name
            barcode = it.barcode.orEmpty()
            calories = formatNumber(it.caloriesPer100g)
            protein = formatNumber(it.proteinPer100g)
            carbs = formatNumber(it.carbsPer100g)
            fat = formatNumber(it.fatPer100g)
        }
    }

    LaunchedEffect(selectedRecipe?.id) {
        selectedRecipe?.let {
            recipeName = it.name
            recipeNotes = it.notes.orEmpty()
            recipeCookedGrams = formatNullableNumber(it.totalCookedGrams)
            recipeDraft.clear()
            recipeDraft.addAll(it.ingredients.map { ingredient -> ingredient.foodItemId to ingredient.gramsUsed })
        }
    }

    LaunchedEffect(selectedTab) {
        nutritionListState.scrollToItem(0)
    }

    LaunchedEffect(scanResult) {
        editableAiItems.clear()
        scanResult?.items?.forEach { editableAiItems += EditableAiItem.from(it) }
        if (!aiScanForRecipe) {
            scanResult?.suggestedMealType?.let {
                mealType = it
                if (mealName in listOf("Breakfast", "Lunch", "Dinner", "Snack", "Ochtend", "Middag", "Avond", "Snacks")) {
                    mealName = it.dutchLabel
                }
            }
        } else if (editableAiItems.isNotEmpty()) {
            selectedTab = 1
        }
    }

    val tabs = listOf("Vandaag", "Recepten", "Producten", "Historie")

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = uiState,
            transitionSpec = { fadeIn(animationSpec = tween(240)) togetherWith fadeOut(animationSpec = tween(180)) },
            label = "nutrition-ui-state",
        ) { state ->
            if (state is NutritionUiState.Loading) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .clearFocusOnScrollOrDrag()
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
                    item { ShimmerCardPlaceholder(lineCount = 3) }
                    item { ShimmerCardPlaceholder(lineCount = 5) }
                    item { ShimmerCardPlaceholder(lineCount = 4) }
                }
                return@AnimatedContent
            }
            if (state is NutritionUiState.Error) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .clearFocusOnScrollOrDrag(),
                    contentPadding = PaddingValues(MaterialTheme.spacing.medium),
                ) {
                    item { MessageCard(message = state.message) }
                }
                return@AnimatedContent
            }
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = MaterialTheme.spacing.medium,
                        vertical = MaterialTheme.spacing.small,
                    ),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                ) {
                    ScreenHeader(title = "Voeding", subtitle = "Voeding loggen zonder gedoe")
                    message?.let { MessageCard(message = it, onDismiss = onDismissMessage) }
                }
                Surface(tonalElevation = 2.dp) {
                    TabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title) },
                            )
                        }
                    }
                }
                LazyColumn(
                    state = nutritionListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .clearFocusOnScrollOrDrag()
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
                    when (selectedTab) {
                        0 -> {
                            item {
                                DailyMealsDashboard(
                                    overview = overview,
                                    onAddToMeal = { type ->
                                        editingMealId = null
                                        mealType = type
                                        mealName = type.dutchLabel
                                        addToMealType = type
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
                                                itemType = if (it.itemType.name == "FOOD") MealEntryType.FOOD else MealEntryType.RECIPE,
                                                referenceId = it.referenceId,
                                                gramsUsed = it.gramsUsed,
                                                notes = it.notes,
                                            )
                                        })
                                        coroutineScope.launch { nutritionListState.animateScrollToItem(1) }
                                    },
                                    onDeleteMeal = { pendingDelete = PendingNutritionDelete.Meal(it) },
                                )
                            }
                            if (mealDraft.isNotEmpty()) {
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
                                    onMealTypeChange = { mealType = it },
                                    onMealNameChange = { mealName = it },
                                    onMealNotesChange = { mealNotes = it },
                                    onUpdateDraftItemGrams = { index, grams ->
                                        val parsed = grams.toNutritionNumberOrNull(max = 100_000.0)
                                        if (parsed != null && parsed > 0.0) {
                                            val current = mealDraft[index]
                                            mealDraft[index] = current.copy(gramsUsed = parsed)
                                        }
                                    },
                                    onRemoveDraftItem = { index -> mealDraft.removeAt(index) },
                                    onSave = {
                                        val errors = validateMealInput(mealName, mealDraft.toList())
                                        mealErrors = errors
                                        if (errors.hasErrors || isMealSaving) return@MealDraftReviewCard
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSaveMeal(editingMealId, mealType, mealName, mealNotes, mealDraft.toList()) {
                                            editingMealId = null
                                            mealNotes = ""
                                            mealDraft.clear()
                                            mealErrors = MealFieldErrors()
                                            onSetScanResult(null)
                                        }
                                    },
                                )
                                }
                            }
                            if (editableAiItems.isNotEmpty() || isAnalyzing) {
                                item {
                                AiMealAnalysisCard(
                                    aiPreferences = aiPreferences,
                                    aiContext = aiContext,
                                    editableItems = if (aiScanForRecipe) emptyList() else editableAiItems.toList(),
                                    itemErrors = aiItemErrors,
                                    isSaving = isAiSaving,
                                    isAnalyzing = isAnalyzing,
                                    onContextChange = { aiContext = it },
                                    onOpenCamera = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        aiScanForRecipe = false
                                        onOpenAiScanner(aiContext)
                                    },
                                    onChangeItem = { index, item ->
                                        editableAiItems[index] = item
                                        aiItemErrors = aiItemErrors - index
                                    },
                                    onDeleteItem = { index -> editableAiItems.removeAt(index) },
                                    onSaveToDraft = {
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
                                        startAiFoodBatchSave(
                                            batchItems = batchItems,
                                            successMessage = { count -> "$count AI-items toegevoegd aan je maaltijd." },
                                            partialFailureMessage = { success, failed -> "$success AI-items toegevoegd, $failed mislukt. Controleer de overgebleven items." },
                                            onSavedItem = { saved, grams ->
                                                mealDraft += MealEntryRequest(MealEntryType.FOOD, saved.id, grams)
                                            },
                                            onAllSucceeded = { onSetScanResult(null) },
                                        )
                                    },
                                )
                                }
                            }
                        }
                        1 -> {
                            item {
                                RecipesHeaderCard(
                                    recipeCount = overview?.recipes?.size ?: 0,
                                    onCreateClick = { showRecipeActions = true },
                                )
                            }
                            if (aiScanForRecipe && editableAiItems.isNotEmpty()) {
                                item {
                                    PhotoFoodReviewCard(
                                        editableItems = editableAiItems.toList(),
                                        itemErrors = aiItemErrors,
                                        isSaving = isAiSaving,
                                            onChangeItem = { index, item ->
                                                editableAiItems[index] = item
                                                aiItemErrors = aiItemErrors - index
                                            },
                                            onDeleteItem = { index -> editableAiItems.removeAt(index) },
                                            onSaveAsRecipe = {
                                            val errors = editableAiItems.mapIndexedNotNull { index, item ->
                                                validateEditableAiItem(item).takeIf { it.hasErrors }?.let { index to it }
                                            }.toMap()
                                            aiItemErrors = errors
                                            if (errors.isNotEmpty() || isAiSaving || editableAiItems.isEmpty()) return@PhotoFoodReviewCard
                                            val batchItems = buildValidAiBatchItems(editableAiItems.toList())
                                            if (batchItems == null || batchItems.isEmpty()) {
                                                aiItemErrors = aiBatchNutritionErrors(editableAiItems.size)
                                                return@PhotoFoodReviewCard
                                            }
                                            recipeName = recipeName.ifBlank { "AI-fotorecept" }
                                            startAiFoodBatchSave(
                                                batchItems = batchItems,
                                                successMessage = { count -> "$count AI-items toegevoegd aan je recept." },
                                                partialFailureMessage = { success, failed -> "$success AI-items toegevoegd, $failed mislukt. Controleer de overgebleven items." },
                                                onSavedItem = { saved, grams ->
                                                    recipeDraft += saved.id to grams
                                                },
                                                onAllSucceeded = {
                                                    aiScanForRecipe = false
                                                    onSetScanResult(null)
                                                },
                                            )
                                        },
                                        onAddToMeal = {
                                            val errors = editableAiItems.mapIndexedNotNull { index, item ->
                                                validateEditableAiItem(item).takeIf { it.hasErrors }?.let { index to it }
                                            }.toMap()
                                            aiItemErrors = errors
                                            if (errors.isNotEmpty() || isAiSaving || editableAiItems.isEmpty()) return@PhotoFoodReviewCard
                                            val batchItems = buildValidAiBatchItems(editableAiItems.toList())
                                            if (batchItems == null || batchItems.isEmpty()) {
                                                aiItemErrors = aiBatchNutritionErrors(editableAiItems.size)
                                                return@PhotoFoodReviewCard
                                            }
                                            startAiFoodBatchSave(
                                                batchItems = batchItems,
                                                successMessage = { count -> "$count AI-items toegevoegd aan je maaltijd." },
                                                partialFailureMessage = { success, failed -> "$success AI-items toegevoegd, $failed mislukt. Controleer de overgebleven items." },
                                                onSavedItem = { saved, grams ->
                                                    mealDraft += MealEntryRequest(MealEntryType.FOOD, saved.id, grams)
                                                },
                                                onAllSucceeded = {
                                                    aiScanForRecipe = false
                                                    selectedTab = 0
                                                    onSetScanResult(null)
                                                },
                                            )
                                        },
                                    )
                                }
                            }
                            item {
                                RecipeEditorCard(
                                    recipeName = recipeName,
                                    recipeNotes = recipeNotes,
                                    recipeCookedGrams = recipeCookedGrams,
                                    ingredientGrams = ingredientGrams,
                                    selectedFood = selectedFood,
                                    draft = recipeDraft.toList(),
                                    foods = overview?.foods.orEmpty(),
                                    recipeAiContext = recipeAiContext,
                                    aiEnabled = aiPreferences.enabled && aiPreferences.apiKey.isNotBlank(),
                                    quickIngredientName = quickIngredientName,
                                    quickIngredientBarcode = quickIngredientBarcode,
                                    quickIngredientKcal = quickIngredientKcal,
                                    quickIngredientProtein = quickIngredientProtein,
                                    quickIngredientCarbs = quickIngredientCarbs,
                                    quickIngredientFat = quickIngredientFat,
                                    isEditing = selectedRecipeId != null,
                                    errors = recipeErrors,
                                    quickIngredientErrors = quickIngredientErrors,
                                    isSaving = isRecipeSaving,
                                    isQuickIngredientSaving = isFoodSaving,
                                    onRecipeNameChange = { recipeName = it; recipeErrors = recipeErrors.copy(name = null) },
                                    onRecipeNotesChange = { recipeNotes = it },
                                    onRecipeCookedGramsChange = { recipeCookedGrams = it; recipeErrors = recipeErrors.copy(cookedGrams = null) },
                                    onIngredientGramsChange = { ingredientGrams = it; recipeErrors = recipeErrors.copy(ingredientGrams = null) },
                                    onRecipeAiContextChange = { recipeAiContext = it },
                                    onQuickIngredientNameChange = { quickIngredientName = it; quickIngredientErrors = quickIngredientErrors.copy(name = null) },
                                    onQuickIngredientBarcodeChange = { quickIngredientBarcode = it },
                                    onQuickIngredientKcalChange = { quickIngredientKcal = it; quickIngredientErrors = quickIngredientErrors.copy(calories = null) },
                                    onQuickIngredientProteinChange = { quickIngredientProtein = it; quickIngredientErrors = quickIngredientErrors.copy(protein = null) },
                                    onQuickIngredientCarbsChange = { quickIngredientCarbs = it; quickIngredientErrors = quickIngredientErrors.copy(carbs = null) },
                                    onQuickIngredientFatChange = { quickIngredientFat = it; quickIngredientErrors = quickIngredientErrors.copy(fat = null) },
                                    onAddIngredient = {
                                        val gramsErrors = validateIngredientGrams(ingredientGrams)
                                        recipeErrors = recipeErrors.copy(ingredientGrams = gramsErrors.ingredientGrams)
                                        if (gramsErrors.hasErrors) return@RecipeEditorCard
                                        val grams = ingredientGrams.toNutritionNumberOrNull(max = 100_000.0)
                                        val foodId = selectedFoodId
                                        if (grams != null && grams > 0.0 && foodId != null) {
                                            recipeDraft += foodId to grams
                                            ingredientGrams = ""
                                            recipeErrors = recipeErrors.copy(ingredientGrams = null, ingredients = null)
                                        }
                                    },
                                    onCreateIngredient = {
                                        val foodErrors = validateFoodInput(quickIngredientName, quickIngredientKcal, quickIngredientProtein, quickIngredientCarbs, quickIngredientFat)
                                        val gramsErrors = validateIngredientGrams(ingredientGrams)
                                        quickIngredientErrors = foodErrors
                                        recipeErrors = recipeErrors.copy(ingredientGrams = gramsErrors.ingredientGrams)
                                        if (foodErrors.hasErrors || gramsErrors.hasErrors || isFoodSaving) return@RecipeEditorCard
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
                                                if (quickIngredientBarcode.isBlank()) FoodSourceType.MANUAL else FoodSourceType.BARCODE,
                                                { saved ->
                                                    recipeDraft += saved.id to grams
                                                    selectedFoodId = saved.id
                                                    quickIngredientName = ""
                                                    quickIngredientBarcode = ""
                                                    quickIngredientKcal = ""
                                                    quickIngredientProtein = ""
                                                    quickIngredientCarbs = ""
                                                    quickIngredientFat = ""
                                                    ingredientGrams = "100"
                                                    quickIngredientErrors = FoodFieldErrors()
                                                    recipeErrors = recipeErrors.copy(ingredientGrams = null, ingredients = null)
                                                },
                                                {},
                                            )
                                        }
                                    },
                                    onRemoveIngredient = { index -> recipeDraft.removeAt(index) },
                                    onSave = {
                                        val errors = validateRecipeInput(recipeName, recipeCookedGrams, recipeDraft.toList())
                                        recipeErrors = errors
                                        if (errors.hasErrors || isRecipeSaving) return@RecipeEditorCard
                                        onSaveRecipe(selectedRecipeId, recipeName, recipeNotes, recipeCookedGrams, recipeDraft.toList()) {
                                            selectedRecipeId = null
                                            recipeName = ""
                                            recipeNotes = ""
                                            recipeCookedGrams = ""
                                            recipeDraft.clear()
                                            recipeErrors = RecipeFieldErrors()
                                        }
                                    },
                                    onScanBarcodeForRecipe = {
                                        onSetScanTarget(ScanTarget.RECIPE_DRAFT)
                                        onOpenBarcodeScanner()
                                    },
                                    onAiVisionForRecipe = {
                                        aiScanForRecipe = true
                                        onOpenAiScanner(recipeAiContext)
                                    },
                                    onCancelEdit = {
                                        selectedRecipeId = null
                                        recipeName = ""
                                        recipeNotes = ""
                                        recipeCookedGrams = ""
                                        recipeDraft.clear()
                                    },
                                )
                            }
                            item {
                                SavedRecipesCard(
                                    recipes = overview?.recipes.orEmpty(),
                                    selectedRecipeId = selectedRecipeId,
                                    mealRecipeGrams = mealRecipeGrams,
                                    mealRecipeGramsError = mealRecipeGramsErrors.grams,
                                    isAddPending = isMealSaving,
                                    onMealRecipeGramsChange = { mealRecipeGrams = it; mealRecipeGramsErrors = QuickAddFieldErrors() },
                                    onSelect = { selectedRecipeId = it },
                                    onUseInMeal = { recipe ->
                                        val errors = validateQuickAddGrams(mealRecipeGrams)
                                        mealRecipeGramsErrors = errors
                                        if (errors.hasErrors || isMealSaving) return@SavedRecipesCard
                                        val grams = mealRecipeGrams.toNutritionNumberOrNull(max = 100_000.0)
                                        if (grams != null && grams > 0.0) {
                                            mealDraft += MealEntryRequest(MealEntryType.RECIPE, recipe.id, grams)
                                            mealErrors = MealFieldErrors()
                                            selectedTab = 0
                                        }
                                    },
                                    onDelete = { pendingDelete = PendingNutritionDelete.Recipe(it) },
                                )
                            }
                        }
                        2 -> {
                            item {
                                FoodEditorCard(
                                    foodName = foodName,
                                    barcode = barcode,
                                    calories = calories,
                                    protein = protein,
                                    carbs = carbs,
                                    fat = fat,
                                    onFoodNameChange = { foodName = it; foodErrors = foodErrors.copy(name = null) },
                                    onBarcodeChange = { barcode = it },
                                    onCaloriesChange = { calories = it; foodErrors = foodErrors.copy(calories = null) },
                                    onProteinChange = { protein = it; foodErrors = foodErrors.copy(protein = null) },
                                    onCarbsChange = { carbs = it; foodErrors = foodErrors.copy(carbs = null) },
                                    onFatChange = { fat = it; foodErrors = foodErrors.copy(fat = null) },
                                    isEditing = selectedFoodId != null,
                                    errors = foodErrors,
                                    isSaving = isFoodSaving,
                                    onScanBarcode = {
                                        onSetScanTarget(ScanTarget.FOOD_EDITOR)
                                        onOpenBarcodeScanner()
                                    },
                                    onSave = {
                                        val errors = validateFoodInput(foodName, calories, protein, carbs, fat)
                                        foodErrors = errors
                                        if (errors.hasErrors || isFoodSaving) return@FoodEditorCard
                                        onSaveFood(
                                            selectedFoodId,
                                            foodName,
                                            barcode,
                                            calories,
                                            protein,
                                            carbs,
                                            fat,
                                            if (barcode.isBlank()) FoodSourceType.MANUAL else FoodSourceType.BARCODE,
                                            {
                                                selectedFoodId = null
                                                foodName = ""
                                                barcode = ""
                                                calories = ""
                                                protein = ""
                                                carbs = ""
                                                fat = ""
                                                foodErrors = FoodFieldErrors()
                                            },
                                            {},
                                        )
                                    },
                                    onCancelEdit = {
                                        selectedFoodId = null
                                        foodName = ""
                                        barcode = ""
                                        calories = ""
                                        protein = ""
                                        carbs = ""
                                        fat = ""
                                        foodErrors = FoodFieldErrors()
                                    },
                                )
                            }
                            item {
                                SavedFoodsCard(
                                    foods = overview?.foods.orEmpty(),
                                    selectedFoodId = selectedFoodId,
                                    mealFoodGrams = mealFoodGrams,
                                    mealFoodGramsError = mealFoodGramsErrors.grams,
                                    isAddPending = isMealSaving,
                                    onMealFoodGramsChange = { mealFoodGrams = it; mealFoodGramsErrors = QuickAddFieldErrors() },
                                    onSelect = { selectedFoodId = it },
                                    onQuickAdd = { food ->
                                        val errors = validateQuickAddGrams(mealFoodGrams)
                                        mealFoodGramsErrors = errors
                                        if (errors.hasErrors || isMealSaving) return@SavedFoodsCard
                                        val grams = mealFoodGrams.toNutritionNumberOrNull(max = 100_000.0)
                                        if (grams != null && grams > 0.0) {
                                            mealDraft += MealEntryRequest(MealEntryType.FOOD, food.id, grams)
                                            mealErrors = MealFieldErrors()
                                            selectedTab = 0
                                        }
                                    },
                                    onDelete = { pendingDelete = PendingNutritionDelete.Food(it) },
                                )
                            }
                        }
                        3 -> {
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
                                                itemType = if (it.itemType.name == "FOOD") MealEntryType.FOOD else MealEntryType.RECIPE,
                                                referenceId = it.referenceId,
                                                gramsUsed = it.gramsUsed,
                                                notes = it.notes,
                                            )
                                        })
                                        selectedTab = 0
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
    if (showRecipeActions) {
        ModalBottomSheet(
            onDismissRequest = { showRecipeActions = false },
            sheetState = recipeActionSheetState,
        ) {
            RecipeActionBottomSheet(
                aiEnabled = aiPreferences.enabled && aiPreferences.apiKey.isNotBlank(),
                onManualRecipe = { showRecipeActions = false },
                onBarcodeIngredient = {
                    showRecipeActions = false
                    onSetScanTarget(ScanTarget.RECIPE_DRAFT)
                    onOpenBarcodeScanner()
                },
                onPhotoIngredient = {
                    showRecipeActions = false
                    aiScanForRecipe = true
                    onOpenAiScanner(recipeAiContext)
                },
                onPhotoDirect = {
                    showRecipeActions = false
                    aiScanForRecipe = true
                    onOpenAiScanner(recipeAiContext)
                },
                onExistingRecipeToMeal = {
                    showRecipeActions = false
                    selectedTab = 1
                },
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
                aiEnabled = aiPreferences.enabled && aiPreferences.apiKey.isNotBlank(),
                onManualFood = {
                    showAddToMealActions = false
                    selectedTab = 2
                },
                onSavedFood = {
                    showAddToMealActions = false
                    selectedTab = 2
                },
                onRecipe = {
                    showAddToMealActions = false
                    selectedTab = 1
                },
                onPhotoAi = {
                    showAddToMealActions = false
                    aiScanForRecipe = false
                    onOpenAiScanner(aiContext)
                },
                onOpenMealDraft = {
                    showAddToMealActions = false
                    selectedTab = 0
                    coroutineScope.launch { nutritionListState.animateScrollToItem(1) }
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
                when (delete) {
                    is PendingNutritionDelete.Meal -> onDeleteMeal(delete.id)
                    is PendingNutritionDelete.Food -> onDeleteFood(delete.id)
                    is PendingNutritionDelete.Recipe -> onDeleteRecipe(delete.id)
                }
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
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
            Text(
                body,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = { Button(onClick = onConfirm, enabled = !isDeleting) { Text(if (isDeleting) "Verwijderen..." else "Verwijderen") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuleren") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryCard(overview: NutritionOverview?) {
    val calories = overview?.todaysCalories ?: 0.0
    val progress = (calories / 2800.0).toFloat().coerceIn(0f, 1f)
    AppCard(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.trainIqColors.amber) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Vandaag", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text(
                    "${formatNumber(calories)} kcal • ${formatNumber(overview?.todaysProtein ?: 0.0)}g eiwit • ${formatNumber(overview?.todaysCarbs ?: 0.0)}g koolhydraten • ${formatNumber(overview?.todaysFat ?: 0.0)}g vet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.trainIqColors.mutedText,
                )
            }
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.trainIqColors.amber)
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
            Text("Netto calorieën ${it.balance} kcal • TEF ${it.tefCalories} • NEAT ${it.neatCalories} • training ${it.workoutCalories}", color = MaterialTheme.trainIqColors.mutedText)
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
    AppCard(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.primary) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Vandaag", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text(
                    "${formatNumber(overview?.todaysProtein ?: 0.0)}g eiwit · ${formatNumber(overview?.todaysCarbs ?: 0.0)}g koolhydraten · ${formatNumber(overview?.todaysFat ?: 0.0)}g vet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.trainIqColors.mutedText,
                )
            }
            Text(
                "${formatNumber(overview?.todaysCalories ?: 0.0)} kcal",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
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

@Composable
private fun MealSectionCard(
    mealType: MealType,
    meals: List<LoggedMeal>,
    onAdd: () -> Unit,
    onEditMeal: (LoggedMeal) -> Unit,
    onDeleteMeal: (Long) -> Unit,
) {
    val total = meals.fold(NutritionFacts.Zero) { acc, meal -> acc + meal.totalNutrition }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(mealType.dutchLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${formatNumber(total.calories)} kcal · ${formatNumber(total.protein)}g eiwit · ${formatNumber(total.carbs)}g koolhydraten · ${formatNumber(total.fat)}g vet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.trainIqColors.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(onClick = onAdd) { Text("Toevoegen") }
            }
            if (meals.isEmpty()) {
                Text("Nog niets toegevoegd", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.trainIqColors.mutedText)
            } else {
                meals.forEach { meal ->
                    MealEntryRow(meal = meal, onEditMeal = onEditMeal, onDeleteMeal = onDeleteMeal)
                }
            }
        }
    }
}

@Composable
private fun MealEntryRow(
    meal: LoggedMeal,
    onEditMeal: (LoggedMeal) -> Unit,
    onDeleteMeal: (Long) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(meal.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${formatNumber(meal.totalNutrition.calories)} kcal", color = MaterialTheme.colorScheme.primary)
            }
            meal.items.forEach { item ->
                Text("${item.name} · ${formatNumber(item.gramsUsed)}g", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.trainIqColors.mutedText)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TextButton(onClick = { onEditMeal(meal) }, modifier = Modifier.fillMaxWidth()) { Text("Hoeveelheid aanpassen") }
                TextButton(onClick = { onDeleteMeal(meal.id) }, modifier = Modifier.fillMaxWidth()) { Text("Maaltijd verwijderen") }
            }
        }
    }
}

@Composable
private fun RecipesHeaderCard(recipeCount: Int, onCreateClick: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.secondary) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Recepten", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text("$recipeCount herbruikbare recepten - handmatig, barcode of AI-foto", color = MaterialTheme.trainIqColors.mutedText)
            }
            Button(onClick = onCreateClick) { Text("+ Toevoegen") }
        }
    }
}

@Composable
private fun RecipeActionBottomSheet(
    aiEnabled: Boolean,
    onManualRecipe: () -> Unit,
    onBarcodeIngredient: () -> Unit,
    onPhotoIngredient: () -> Unit,
    onPhotoDirect: () -> Unit,
    onExistingRecipeToMeal: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clearFocusOnScrollOrDrag()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.spacing.large, vertical = MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Toevoegen of maken", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Button(onClick = onManualRecipe, modifier = Modifier.fillMaxWidth()) { Text("Recept handmatig maken") }
        OutlinedButton(onClick = onBarcodeIngredient, modifier = Modifier.fillMaxWidth()) { Text("Barcode-product in recept scannen") }
        OutlinedButton(onClick = onPhotoIngredient, enabled = aiEnabled, modifier = Modifier.fillMaxWidth()) { Text("Product via foto/AI toevoegen") }
        OutlinedButton(onClick = onPhotoDirect, enabled = aiEnabled, modifier = Modifier.fillMaxWidth()) { Text("Foto nemen: recept of vandaag") }
        OutlinedButton(onClick = onExistingRecipeToMeal, modifier = Modifier.fillMaxWidth()) { Text("Bestaand recept aan maaltijd toevoegen") }
    }
}

@Composable
private fun AddToMealActionSheet(
    mealType: MealType,
    hasSavedFoods: Boolean,
    hasSavedRecipes: Boolean,
    hasDraft: Boolean,
    aiEnabled: Boolean,
    onManualFood: () -> Unit,
    onSavedFood: () -> Unit,
    onRecipe: () -> Unit,
    onPhotoAi: () -> Unit,
    onOpenMealDraft: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .clearFocusOnScrollOrDrag()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.spacing.large, vertical = MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Toevoegen aan ${mealType.dutchLabel}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Kies een bron en controleer daarna voor opslaan.", color = MaterialTheme.trainIqColors.mutedText)
        Button(onClick = onManualFood, modifier = Modifier.fillMaxWidth()) { Text("Handmatig product maken") }
        OutlinedButton(onClick = onSavedFood, enabled = hasSavedFoods, modifier = Modifier.fillMaxWidth()) { Text("Opgeslagen product gebruiken") }
        OutlinedButton(onClick = onRecipe, enabled = hasSavedRecipes, modifier = Modifier.fillMaxWidth()) { Text("Opgeslagen recept gebruiken") }
        OutlinedButton(onClick = onPhotoAi, enabled = aiEnabled, modifier = Modifier.fillMaxWidth()) { Text("Foto / AI-inschatting") }
        if (hasDraft) {
            OutlinedButton(onClick = onOpenMealDraft, modifier = Modifier.fillMaxWidth()) { Text("Huidige maaltijd controleren") }
        }
    }
}

@Composable
private fun PhotoFoodReviewCard(
    editableItems: List<EditableAiItem>,
    itemErrors: Map<Int, AiItemFieldErrors>,
    isSaving: Boolean,
    onChangeItem: (Int, EditableAiItem) -> Unit,
    onDeleteItem: (Int) -> Unit,
    onSaveAsRecipe: () -> Unit,
    onAddToMeal: () -> Unit,
) {
    AppCard(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.tertiary) {
        Text("Fotocontrole", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("AI-waarden zijn schattingen. Controleer en pas ze aan voordat je opslaat.", color = MaterialTheme.trainIqColors.mutedText)
        editableItems.forEachIndexed { index, item ->
            EditableAiItemCard(item = item, errors = itemErrors[index] ?: AiItemFieldErrors(), onChange = { onChangeItem(index, it) }, onDelete = { onDeleteItem(index) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onSaveAsRecipe, enabled = !isSaving, modifier = Modifier.weight(1f)) { Text(if (isSaving) "Opslaan..." else "Opslaan als recept") }
            OutlinedButton(onClick = onAddToMeal, enabled = !isSaving, modifier = Modifier.weight(1f)) { Text("Aan maaltijd toevoegen") }
        }
    }
}

@Composable
private fun NutritionNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.bringIntoViewOnFocus(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
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
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.bringIntoViewOnFocus(),
        singleLine = singleLine,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
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
    onFoodNameChange: (String) -> Unit,
    onBarcodeChange: (String) -> Unit,
    onCaloriesChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onCarbsChange: (String) -> Unit,
    onFatChange: (String) -> Unit,
    isEditing: Boolean,
    errors: FoodFieldErrors,
    isSaving: Boolean,
    onScanBarcode: () -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
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
                NutritionNumberField(value = carbs, onValueChange = onCarbsChange, label = "Koolhydraten / 100g", modifier = Modifier.weight(1f), error = errors.carbs)
                NutritionNumberField(value = fat, onValueChange = onFatChange, label = "Vet / 100g", modifier = Modifier.weight(1f), error = errors.fat)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onSave, enabled = !isSaving, modifier = Modifier.weight(1f)) { Text(if (isSaving) "Opslaan..." else if (isEditing) "Wijzigingen opslaan" else "Product opslaan") }
                OutlinedButton(onClick = onScanBarcode, modifier = Modifier.weight(1f)) { Text("Barcode scannen") }
            }
            if (isEditing) {
                TextButton(onClick = onCancelEdit, modifier = Modifier.fillMaxWidth()) { Text("Annuleren en nieuw product") }
            }
            Text(
                "Barcode scannen vult alleen de herkenningscode in. Vul voedingswaarden handmatig in; automatische productdata is nog niet gekoppeld.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.trainIqColors.mutedText,
            )
        }
    }
}

@Composable
private fun SavedFoodsCard(
    foods: List<FoodItem>,
    selectedFoodId: Long?,
    mealFoodGrams: String,
    mealFoodGramsError: String?,
    isAddPending: Boolean,
    onMealFoodGramsChange: (String) -> Unit,
    onSelect: (Long) -> Unit,
    onQuickAdd: (FoodItem) -> Unit,
    onDelete: (Long) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
            Text("Producten", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (foods.isEmpty()) {
                EmptyStateCard(
                    title = "Nog geen opgeslagen producten",
                    body = "Maak handmatig een product of scan een barcode. Opgeslagen producten blijven herbruikbaar voor maaltijden en recepten.",
                )
            } else {
                NutritionNumberField(
                    value = mealFoodGrams,
                    onValueChange = onMealFoodGramsChange,
                    label = "Gram bij toevoegen aan maaltijd",
                    modifier = Modifier.fillMaxWidth(),
                    error = mealFoodGramsError,
                )
                foods.forEach { food ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedFoodId == food.id) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                        ),
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(food.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${formatNumber(food.caloriesPer100g)} kcal/100g · ${formatNumber(food.proteinPer100g)}g eiwit · ${formatNumber(food.carbsPer100g)}g koolhydraten · ${formatNumber(food.fatPer100g)}g vet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.trainIqColors.mutedText,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(onClick = { onSelect(food.id) }, modifier = Modifier.weight(1f)) { Text(if (selectedFoodId == food.id) "Geselecteerd" else "Gebruiken") }
                                Button(onClick = { onQuickAdd(food) }, enabled = !isAddPending, modifier = Modifier.weight(1f)) { Text("Aan maaltijd") }
                                TextButton(onClick = { onDelete(food.id) }) { Text("Verwijderen") }
                            }
                        }
                    }
                }
            }
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
    quickIngredientName: String,
    quickIngredientBarcode: String,
    quickIngredientKcal: String,
    quickIngredientProtein: String,
    quickIngredientCarbs: String,
    quickIngredientFat: String,
    isEditing: Boolean,
    errors: RecipeFieldErrors,
    quickIngredientErrors: FoodFieldErrors,
    isSaving: Boolean,
    isQuickIngredientSaving: Boolean,
    onRecipeNameChange: (String) -> Unit,
    onRecipeNotesChange: (String) -> Unit,
    onRecipeCookedGramsChange: (String) -> Unit,
    onIngredientGramsChange: (String) -> Unit,
    onRecipeAiContextChange: (String) -> Unit,
    onQuickIngredientNameChange: (String) -> Unit,
    onQuickIngredientBarcodeChange: (String) -> Unit,
    onQuickIngredientKcalChange: (String) -> Unit,
    onQuickIngredientProteinChange: (String) -> Unit,
    onQuickIngredientCarbsChange: (String) -> Unit,
    onQuickIngredientFatChange: (String) -> Unit,
    onAddIngredient: () -> Unit,
    onCreateIngredient: () -> Unit,
    onRemoveIngredient: (Int) -> Unit,
    onSave: () -> Unit,
    onScanBarcodeForRecipe: () -> Unit,
    onAiVisionForRecipe: () -> Unit,
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
            NutritionTextField(value = recipeName, onValueChange = onRecipeNameChange, label = "Receptnaam", modifier = Modifier.fillMaxWidth(), error = errors.name)
            NutritionTextField(value = recipeNotes, onValueChange = onRecipeNotesChange, label = "Notities", modifier = Modifier.fillMaxWidth(), singleLine = false)
            NutritionNumberField(value = recipeCookedGrams, onValueChange = onRecipeCookedGramsChange, label = "Totaal bereid gewicht (optioneel)", modifier = Modifier.fillMaxWidth(), error = errors.cookedGrams)
            Text("Opgeslagen ingrediënt: ${selectedFood?.name ?: "Kies een product uit Producten"}", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                NutritionNumberField(value = ingredientGrams, onValueChange = onIngredientGramsChange, label = "Ingrediënt gram", modifier = Modifier.weight(1f), error = errors.ingredientGrams)
                Button(onClick = onAddIngredient, enabled = selectedFood != null, modifier = Modifier.weight(1f)) { Text("Toevoegen") }
            }
            HorizontalDivider()
            Text("Ingrediënt handmatig maken", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            NutritionTextField(value = quickIngredientName, onValueChange = onQuickIngredientNameChange, label = "Ingrediëntnaam", modifier = Modifier.fillMaxWidth(), error = quickIngredientErrors.name)
            NutritionTextField(value = quickIngredientBarcode, onValueChange = onQuickIngredientBarcodeChange, label = "Barcode (optioneel)", modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                NutritionNumberField(value = quickIngredientKcal, onValueChange = onQuickIngredientKcalChange, label = "kcal / 100g", modifier = Modifier.weight(1f), error = quickIngredientErrors.calories)
                NutritionNumberField(value = quickIngredientProtein, onValueChange = onQuickIngredientProteinChange, label = "Eiwit / 100g", modifier = Modifier.weight(1f), error = quickIngredientErrors.protein)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                NutritionNumberField(value = quickIngredientCarbs, onValueChange = onQuickIngredientCarbsChange, label = "Koolhydraten / 100g", modifier = Modifier.weight(1f), error = quickIngredientErrors.carbs)
                NutritionNumberField(value = quickIngredientFat, onValueChange = onQuickIngredientFatChange, label = "Vet / 100g", modifier = Modifier.weight(1f), error = quickIngredientErrors.fat)
            }
            OutlinedButton(onClick = onCreateIngredient, enabled = !isQuickIngredientSaving, modifier = Modifier.fillMaxWidth()) { Text(if (isQuickIngredientSaving) "Opslaan..." else "Ingrediënt opslaan en toevoegen") }
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
            HorizontalDivider()
            Text("Barcode- en fotohulp", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (aiEnabled) {
                NutritionTextField(value = recipeAiContext, onValueChange = onRecipeAiContextChange, label = "AI-context (optioneel)", modifier = Modifier.fillMaxWidth())
            } else {
                Text(
                    "AI-fotoherkenning is pas beschikbaar nadat je dit aanzet in Instellingen. Handmatig invoeren en barcodes blijven werken.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.trainIqColors.mutedText,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onScanBarcodeForRecipe, modifier = Modifier.weight(1f)) { Text("Barcode scannen") }
                OutlinedButton(onClick = onAiVisionForRecipe, enabled = aiEnabled, modifier = Modifier.weight(1f)) { Text("AI-herkenning gebruiken") }
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
                "${formatNumber(totalNutrition.protein)}g eiwit · ${formatNumber(totalNutrition.carbs)}g koolhydraten · ${formatNumber(totalNutrition.fat)}g vet",
                color = MaterialTheme.trainIqColors.mutedText,
            )
        }
    }
}

@Composable
private fun SavedRecipesCard(
    recipes: List<Recipe>,
    selectedRecipeId: Long?,
    mealRecipeGrams: String,
    mealRecipeGramsError: String?,
    isAddPending: Boolean,
    onMealRecipeGramsChange: (String) -> Unit,
    onSelect: (Long) -> Unit,
    onUseInMeal: (Recipe) -> Unit,
    onDelete: (Long) -> Unit,
) {
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
                NutritionNumberField(
                    value = mealRecipeGrams,
                    onValueChange = onMealRecipeGramsChange,
                    label = "Gram bij toevoegen aan maaltijd",
                    modifier = Modifier.fillMaxWidth(),
                    error = mealRecipeGramsError,
                )
                recipes.forEach { recipe ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedRecipeId == recipe.id) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                        ),
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(recipe.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${formatNumber(recipe.totalNutrition.calories)} kcal · ${formatNumber(recipe.totalNutrition.protein)}g eiwit · ${formatNumber(recipe.totalNutrition.carbs)}g koolhydraten · ${formatNumber(recipe.totalNutrition.fat)}g vet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.trainIqColors.mutedText,
                            )
                            Text("${recipe.ingredients.size} ingrediënten", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(onClick = { onSelect(recipe.id) }, modifier = Modifier.weight(1f)) { Text(if (selectedRecipeId == recipe.id) "Bewerken" else "Bewerk") }
                                Button(onClick = { onUseInMeal(recipe) }, enabled = !isAddPending, modifier = Modifier.weight(1f)) { Text("Aan maaltijd") }
                                TextButton(onClick = { onDelete(recipe.id) }) { Text("Verwijderen") }
                            }
                        }
                    }
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
    mealDraft: List<MealEntryRequest>,
    foods: List<FoodItem>,
    recipes: List<Recipe>,
    errors: MealFieldErrors,
    isSaving: Boolean,
    onMealTypeChange: (MealType) -> Unit,
    onMealNameChange: (String) -> Unit,
    onMealNotesChange: (String) -> Unit,
    onUpdateDraftItemGrams: (Int, String) -> Unit,
    onRemoveDraftItem: (Int) -> Unit,
    onSave: () -> Unit,
) {
    val draftTotals = mealDraft.fold(NutritionFacts.Zero) { acc, entry ->
        val itemNutrition = when (entry.itemType) {
            MealEntryType.FOOD -> foods.firstOrNull { it.id == entry.referenceId }?.let { food ->
                NutritionFacts(
                    calories = food.caloriesPer100g * entry.gramsUsed / 100.0,
                    protein = food.proteinPer100g * entry.gramsUsed / 100.0,
                    carbs = food.carbsPer100g * entry.gramsUsed / 100.0,
                    fat = food.fatPer100g * entry.gramsUsed / 100.0,
                )
            }
            MealEntryType.RECIPE -> recipes.firstOrNull { it.id == entry.referenceId }?.let { recipe ->
                val baseGrams = recipe.totalCookedGrams ?: recipe.ingredients.sumOf { it.gramsUsed }
                if (baseGrams > 0.0) {
                    val ratio = entry.gramsUsed / baseGrams
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
        } ?: NutritionFacts.Zero
        acc + itemNutrition
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
            MealType.entries.chunked(2).forEach { rowOptions ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    rowOptions.forEach { option ->
                        FilterChip(
                            selected = mealType == option,
                            onClick = {
                                onMealTypeChange(option)
                                if (mealName in listOf("Breakfast", "Lunch", "Dinner", "Snack", "Ochtend", "Middag", "Avond", "Snacks")) {
                                    onMealNameChange(option.dutchLabel)
                                }
                            },
                            label = { Text(option.dutchLabel) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowOptions.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            NutritionTextField(value = mealName, onValueChange = onMealNameChange, label = "Maaltijdnaam", modifier = Modifier.fillMaxWidth(), error = errors.name)
            NutritionTextField(value = mealNotes, onValueChange = onMealNotesChange, label = "Notities", modifier = Modifier.fillMaxWidth(), singleLine = false)
            errors.items?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            mealDraft.forEachIndexed { index, entry ->
                val label = when (entry.itemType) {
                    MealEntryType.FOOD -> foods.firstOrNull { it.id == entry.referenceId }?.name
                    MealEntryType.RECIPE -> recipes.firstOrNull { it.id == entry.referenceId }?.name
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
                                value = formatNumber(entry.gramsUsed),
                                onValueChange = { onUpdateDraftItemGrams(index, it) },
                                label = "Gram",
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        TextButton(onClick = { onRemoveDraftItem(index) }) { Text("Verwijderen") }
                    }
                }
            }
            RecipeTotalsCard(title = "Maaltijdtotalen", totalNutrition = draftTotals, totalGrams = mealDraft.sumOf { it.gramsUsed })
            Button(onClick = onSave, enabled = mealDraft.isNotEmpty() && !isSaving, modifier = Modifier.fillMaxWidth()) { Text(if (isSaving) "Opslaan..." else "Maaltijd opslaan") }
        }
    }
}

@Composable
private fun AiMealAnalysisCard(
    aiPreferences: AiPreferences,
    aiContext: String,
    editableItems: List<EditableAiItem>,
    itemErrors: Map<Int, AiItemFieldErrors>,
    isSaving: Boolean,
    isAnalyzing: Boolean,
    onContextChange: (String) -> Unit,
    onOpenCamera: () -> Unit,
    onChangeItem: (Int, EditableAiItem) -> Unit,
    onDeleteItem: (Int) -> Unit,
    onSaveToDraft: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
            Text("Foto / AI-controle", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("Maaltijd scannen", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                when {
                    !aiPreferences.enabled -> "AI staat uit in Instellingen. Handmatig voeding loggen blijft werken."
                    aiPreferences.apiKey.isBlank() -> "Voeg een Gemini API-sleutel toe in Instellingen om maaltijdanalyse te gebruiken."
                    else -> "Open de scanner op volledig scherm, fotografeer de maaltijd en controleer elk gevonden item voordat je opslaat."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            NutritionTextField(value = aiContext, onValueChange = onContextChange, label = "Optionele context", modifier = Modifier.fillMaxWidth())
            Button(onClick = onOpenCamera, enabled = aiPreferences.enabled && aiPreferences.apiKey.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text("Scanner openen")
            }
            if (isAnalyzing) {
                Text("Verwerken...", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                repeat(3) {
                    ShimmerCardPlaceholder(lineCount = 2)
                }
            }
            editableItems.forEachIndexed { index, item ->
                EditableAiItemCard(item = item, errors = itemErrors[index] ?: AiItemFieldErrors(), onChange = { onChangeItem(index, it) }, onDelete = { onDeleteItem(index) })
            }
            if (editableItems.isNotEmpty()) {
                Button(onClick = onSaveToDraft, enabled = !isSaving) { Text(if (isSaving) "Opslaan..." else "AI-items aan maaltijd toevoegen") }
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
            Text("Voedingshistorie", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Eerder gelogde maaltijden blijven voedingssnapshots, zodat latere product- of receptwijzigingen oude logs niet aanpassen.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.trainIqColors.mutedText)
            if (meals.isEmpty()) {
                EmptyStateCard(
                    title = "Nog geen gelogde maaltijden",
                    body = "Maaltijden uit Ochtend, Middag, Avond of Snacks verschijnen hier om te controleren en hergebruiken.",
                )
            } else {
                meals.forEach { meal ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(meal.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(meal.mealType.dutchLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text("${formatNumber(meal.totalNutrition.calories)} kcal • E${formatNumber(meal.totalNutrition.protein)} K${formatNumber(meal.totalNutrition.carbs)} V${formatNumber(meal.totalNutrition.fat)}")
                            meal.items.forEach { item ->
                                val liveName = when (item.itemType.name) {
                                    "FOOD" -> foods.firstOrNull { it.id == item.referenceId }?.name
                                    else -> recipes.firstOrNull { it.id == item.referenceId }?.name
                                } ?: item.name
                                Text("$liveName • ${formatNumber(item.gramsUsed)}g", maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(onClick = { onReuseMeal(meal) }, modifier = Modifier.weight(1f)) { Text("Opnieuw gebruiken") }
                                TextButton(onClick = { onDeleteMeal(meal.id) }) { Text("Verwijderen") }
                            }
                        }
                    }
                }
            }
        }
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

private fun formatNumber(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format("%.1f", value)

private fun formatNullableNumber(value: Double?): String = value?.let(::formatNumber).orEmpty()

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
