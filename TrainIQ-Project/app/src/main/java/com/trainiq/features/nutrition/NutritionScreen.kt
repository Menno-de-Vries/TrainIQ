package com.trainiq.features.nutrition

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
    ) {
        val parsedCalories = calories.toNutritionNumberOrNull(max = 5000.0)
        val parsedProtein = protein.toNutritionNumberOrNull(max = 1000.0)
        val parsedCarbs = carbs.toNutritionNumberOrNull(max = 1000.0)
        val parsedFat = fat.toNutritionNumberOrNull(max = 1000.0)
        if (name.isBlank() || parsedCalories == null || parsedProtein == null || parsedCarbs == null || parsedFat == null) {
            ephemeral.update { it.copy(message = "Vul eerst een naam en niet-negatieve waarden per 100g in.") }
            return
        }
        viewModelScope.launch {
            val item = saveFoodItemUseCase(id, name.trim(), barcode?.trim()?.ifBlank { null }, parsedCalories, parsedProtein, parsedCarbs, parsedFat, sourceType)
            ephemeral.update { it.copy(message = "${item.name} opgeslagen.") }
            onSaved(item)
        }
    }

    fun saveRecipe(name: String, notes: String, cookedGrams: String, ingredients: List<Pair<Long, Double>>) {
        val parsedCookedGrams = cookedGrams.trim().takeIf { it.isNotBlank() }?.toNutritionNumberOrNull(max = 100_000.0)
        if (name.isBlank() || ingredients.isEmpty() || ingredients.any { it.second <= 0.0 || !it.second.isFinite() } || (cookedGrams.isNotBlank() && parsedCookedGrams == null)) {
            ephemeral.update { it.copy(message = "Vul een receptnaam en positieve hoeveelheden voor ingrediënten in.") }
            return
        }
        viewModelScope.launch {
            saveRecipeUseCase(null, name.trim(), notes.trim(), parsedCookedGrams, ingredients)
            ephemeral.update { it.copy(message = "Recept opgeslagen.") }
        }
    }

    fun saveMeal(id: Long?, mealType: MealType, name: String, notes: String, items: List<MealEntryRequest>, onSaved: () -> Unit = {}) {
        if (name.isBlank() || items.isEmpty() || items.any { it.gramsUsed <= 0.0 || !it.gramsUsed.isFinite() }) {
            ephemeral.update { it.copy(message = "Vul een maaltijdnaam en positieve hoeveelheden in voordat je opslaat.") }
            return
        }
        viewModelScope.launch {
            saveMealUseCase(id, mealType, name.trim(), notes.trim(), items)
            ephemeral.update { it.copy(message = "Maaltijd opgeslagen.") }
            onSaved()
        }
    }

    fun deleteMeal(mealId: Long) {
        viewModelScope.launch {
            deleteMealUseCase(mealId)
            ephemeral.update { it.copy(message = "Maaltijd verwijderd.") }
        }
    }

    fun deleteFood(foodId: Long) {
        viewModelScope.launch {
            deleteFoodUseCase(foodId)
            ephemeral.update { it.copy(message = "Product verwijderd.") }
        }
    }

    fun deleteRecipe(recipeId: Long) {
        viewModelScope.launch {
            deleteRecipeUseCase(recipeId)
            ephemeral.update { it.copy(message = "Recept verwijderd.") }
        }
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
        onSetScanResult = viewModel::setScanResult,
        onSetScanTarget = viewModel::setScanTarget,
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
    onSaveFood: (Long?, String, String?, String, String, String, String, FoodSourceType, (FoodItem) -> Unit) -> Unit,
    onSaveRecipe: (String, String, String, List<Pair<Long, Double>>) -> Unit,
    onSaveMeal: (Long?, MealType, String, String, List<MealEntryRequest>, () -> Unit) -> Unit,
    onDeleteMeal: (Long) -> Unit,
    onDeleteFood: (Long) -> Unit,
    onDeleteRecipe: (Long) -> Unit,
    onSetScanResult: (MealAnalysisResult?) -> Unit,
    onSetScanTarget: (ScanTarget) -> Unit = {},
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

    var mealType by remember { mutableStateOf(MealType.LUNCH) }
    var mealName by remember { mutableStateOf(MealType.LUNCH.dutchLabel) }
    var mealNotes by remember { mutableStateOf("") }
    var mealFoodGrams by remember { mutableStateOf("100") }
    var mealRecipeGrams by remember { mutableStateOf("150") }
    var editingMealId by remember { mutableStateOf<Long?>(null) }
    val mealDraft = remember { mutableStateListOf<MealEntryRequest>() }

    var aiContext by remember { mutableStateOf("") }
    val editableAiItems = remember { mutableStateListOf<EditableAiItem>() }

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
                                    onMealTypeChange = { mealType = it },
                                    onMealNameChange = { mealName = it },
                                    onMealNotesChange = { mealNotes = it },
                                    onRemoveDraftItem = { index -> mealDraft.removeAt(index) },
                                    onSave = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSaveMeal(editingMealId, mealType, mealName, mealNotes, mealDraft.toList()) {
                                            editingMealId = null
                                            mealNotes = ""
                                            mealDraft.clear()
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
                                    isAnalyzing = isAnalyzing,
                                    onContextChange = { aiContext = it },
                                    onOpenCamera = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        aiScanForRecipe = false
                                        onOpenAiScanner(aiContext)
                                    },
                                    onChangeItem = { index, item -> editableAiItems[index] = item },
                                    onDeleteItem = { index -> editableAiItems.removeAt(index) },
                                    onSaveToDraft = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        editableAiItems.forEach { item ->
                                            val grams = item.grams.toNutritionNumberOrNull(max = 100_000.0) ?: 100.0
                                            onSaveFood(
                                                null,
                                                item.name,
                                                null,
                                                per100Value(item.calories, grams),
                                                per100Value(item.protein, grams),
                                                per100Value(item.carbs, grams),
                                                per100Value(item.fat, grams),
                                                FoodSourceType.AI,
                                            ) { saved -> mealDraft += MealEntryRequest(MealEntryType.FOOD, saved.id, grams) }
                                        }
                                        editableAiItems.clear()
                                        onSetScanResult(null)
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
                                            onChangeItem = { index, item -> editableAiItems[index] = item },
                                            onDeleteItem = { index -> editableAiItems.removeAt(index) },
                                            onSaveAsRecipe = {
                                            recipeName = recipeName.ifBlank { "AI-fotorecept" }
                                            editableAiItems.forEach { item ->
                                                val grams = item.grams.toNutritionNumberOrNull(max = 100_000.0) ?: 100.0
                                                onSaveFood(
                                                    null,
                                                    item.name,
                                                    null,
                                                    per100Value(item.calories, grams),
                                                    per100Value(item.protein, grams),
                                                    per100Value(item.carbs, grams),
                                                    per100Value(item.fat, grams),
                                                    FoodSourceType.AI,
                                                ) { saved -> recipeDraft += saved.id to grams }
                                            }
                                            editableAiItems.clear()
                                            aiScanForRecipe = false
                                            onSetScanResult(null)
                                        },
                                        onAddToMeal = {
                                            editableAiItems.forEach { item ->
                                                val grams = item.grams.toNutritionNumberOrNull(max = 100_000.0) ?: 100.0
                                                onSaveFood(
                                                    null,
                                                    item.name,
                                                    null,
                                                    per100Value(item.calories, grams),
                                                    per100Value(item.protein, grams),
                                                    per100Value(item.carbs, grams),
                                                    per100Value(item.fat, grams),
                                                    FoodSourceType.AI,
                                                ) { saved -> mealDraft += MealEntryRequest(MealEntryType.FOOD, saved.id, grams) }
                                            }
                                            editableAiItems.clear()
                                            aiScanForRecipe = false
                                            selectedTab = 0
                                            onSetScanResult(null)
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
                                    onRecipeNameChange = { recipeName = it },
                                    onRecipeNotesChange = { recipeNotes = it },
                                    onRecipeCookedGramsChange = { recipeCookedGrams = it },
                                    onIngredientGramsChange = { ingredientGrams = it },
                                    onRecipeAiContextChange = { recipeAiContext = it },
                                    onQuickIngredientNameChange = { quickIngredientName = it },
                                    onQuickIngredientBarcodeChange = { quickIngredientBarcode = it },
                                    onQuickIngredientKcalChange = { quickIngredientKcal = it },
                                    onQuickIngredientProteinChange = { quickIngredientProtein = it },
                                    onQuickIngredientCarbsChange = { quickIngredientCarbs = it },
                                    onQuickIngredientFatChange = { quickIngredientFat = it },
                                    onAddIngredient = {
                                        val grams = ingredientGrams.toNutritionNumberOrNull(max = 100_000.0)
                                        val foodId = selectedFoodId
                                        if (grams != null && foodId != null) {
                                            recipeDraft += foodId to grams
                                            ingredientGrams = ""
                                        }
                                    },
                                    onCreateIngredient = {
                                        val grams = ingredientGrams.toNutritionNumberOrNull(max = 100_000.0)
                                        val kcal = quickIngredientKcal.toNutritionNumberOrNull(max = 5000.0)
                                        val proteinValue = quickIngredientProtein.toNutritionNumberOrNull(max = 1000.0)
                                        val carbsValue = quickIngredientCarbs.toNutritionNumberOrNull(max = 1000.0)
                                        val fatValue = quickIngredientFat.toNutritionNumberOrNull(max = 1000.0)
                                        if (quickIngredientName.isNotBlank() && grams != null && kcal != null && proteinValue != null && carbsValue != null && fatValue != null) {
                                            onSaveFood(
                                                null,
                                                quickIngredientName,
                                                quickIngredientBarcode,
                                                quickIngredientKcal,
                                                quickIngredientProtein,
                                                quickIngredientCarbs,
                                                quickIngredientFat,
                                                if (quickIngredientBarcode.isBlank()) FoodSourceType.MANUAL else FoodSourceType.BARCODE,
                                            ) { saved ->
                                                recipeDraft += saved.id to grams
                                                selectedFoodId = saved.id
                                                quickIngredientName = ""
                                                quickIngredientBarcode = ""
                                                quickIngredientKcal = ""
                                                quickIngredientProtein = ""
                                                quickIngredientCarbs = ""
                                                quickIngredientFat = ""
                                                ingredientGrams = "100"
                                            }
                                        }
                                    },
                                    onRemoveIngredient = { index -> recipeDraft.removeAt(index) },
                                    onSave = {
                                        val canClearDraft = recipeName.isNotBlank() &&
                                            recipeDraft.all { it.second > 0.0 && it.second.isFinite() } &&
                                            (recipeCookedGrams.isBlank() || recipeCookedGrams.toNutritionNumberOrNull(max = 100_000.0) != null)
                                        onSaveRecipe(recipeName, recipeNotes, recipeCookedGrams, recipeDraft.toList())
                                        if (canClearDraft) {
                                            selectedRecipeId = null
                                            recipeName = ""
                                            recipeNotes = ""
                                            recipeCookedGrams = ""
                                            recipeDraft.clear()
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
                                )
                            }
                            item {
                                SavedRecipesCard(
                                    recipes = overview?.recipes.orEmpty(),
                                    selectedRecipeId = selectedRecipeId,
                                    onSelect = { selectedRecipeId = it },
                                    onUseInMeal = { recipe ->
                                        mealDraft += MealEntryRequest(MealEntryType.RECIPE, recipe.id, mealRecipeGrams.toNutritionNumberOrNull(max = 100_000.0) ?: 150.0)
                                        selectedTab = 0
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
                                    onFoodNameChange = { foodName = it },
                                    onBarcodeChange = { barcode = it },
                                    onCaloriesChange = { calories = it },
                                    onProteinChange = { protein = it },
                                    onCarbsChange = { carbs = it },
                                    onFatChange = { fat = it },
                                    onScanBarcode = {
                                        onSetScanTarget(ScanTarget.FOOD_EDITOR)
                                        onOpenBarcodeScanner()
                                    },
                                    onSave = {
                                        onSaveFood(selectedFoodId, foodName, barcode, calories, protein, carbs, fat, if (barcode.isBlank()) FoodSourceType.MANUAL else FoodSourceType.BARCODE) {
                                            selectedFoodId = null
                                            foodName = ""
                                            barcode = ""
                                            calories = ""
                                            protein = ""
                                            carbs = ""
                                            fat = ""
                                        }
                                    },
                                )
                            }
                            item {
                                SavedFoodsCard(
                                    foods = overview?.foods.orEmpty(),
                                    selectedFoodId = selectedFoodId,
                                    onSelect = { selectedFoodId = it },
                                    onQuickAdd = { food ->
                                        val grams = mealFoodGrams.toNutritionNumberOrNull(max = 100_000.0) ?: 100.0
                                        mealDraft += MealEntryRequest(MealEntryType.FOOD, food.id, grams)
                                        selectedTab = 0
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
            onConfirm = {
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
        is PendingNutritionDelete.Food -> "Dit product verdwijnt uit je productlijst."
        is PendingNutritionDelete.Recipe -> "Dit recept verdwijnt uit je opgeslagen recepten."
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
        confirmButton = { Button(onClick = onConfirm) { Text("Verwijderen") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuleren") } },
    )
}

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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onEditMeal(meal) }) { Text("Hoeveelheid aanpassen") }
                TextButton(onClick = { onDeleteMeal(meal.id) }) { Text("Verwijderen") }
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
    onChangeItem: (Int, EditableAiItem) -> Unit,
    onDeleteItem: (Int) -> Unit,
    onSaveAsRecipe: () -> Unit,
    onAddToMeal: () -> Unit,
) {
    AppCard(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.tertiary) {
        Text("Fotocontrole", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("AI-waarden zijn schattingen. Controleer en pas ze aan voordat je opslaat.", color = MaterialTheme.trainIqColors.mutedText)
        editableItems.forEachIndexed { index, item ->
            EditableAiItemCard(item = item, onChange = { onChangeItem(index, it) }, onDelete = { onDeleteItem(index) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onSaveAsRecipe, modifier = Modifier.weight(1f)) { Text("Opslaan als recept") }
            OutlinedButton(onClick = onAddToMeal, modifier = Modifier.weight(1f)) { Text("Aan maaltijd toevoegen") }
        }
    }
}

@Composable
private fun NutritionNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.bringIntoViewOnFocus(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
    )
}

@Composable
private fun NutritionTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.bringIntoViewOnFocus(),
        singleLine = singleLine,
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
    onScanBarcode: () -> Unit,
    onSave: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
            Text("Product", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Sla producten eenmalig op en voeg ze daarna toe aan maaltijden of gebruik ze als receptingrediënten.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.trainIqColors.mutedText,
            )
            NutritionTextField(value = foodName, onValueChange = onFoodNameChange, label = "Productnaam", modifier = Modifier.fillMaxWidth())
            NutritionTextField(value = barcode, onValueChange = onBarcodeChange, label = "Barcode (optioneel)", modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                NutritionNumberField(value = calories, onValueChange = onCaloriesChange, label = "kcal / 100g", modifier = Modifier.weight(1f))
                NutritionNumberField(value = protein, onValueChange = onProteinChange, label = "Eiwit / 100g", modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                NutritionNumberField(value = carbs, onValueChange = onCarbsChange, label = "Koolhydraten / 100g", modifier = Modifier.weight(1f))
                NutritionNumberField(value = fat, onValueChange = onFatChange, label = "Vet / 100g", modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onSave, modifier = Modifier.weight(1f)) { Text("Product opslaan") }
                OutlinedButton(onClick = onScanBarcode, modifier = Modifier.weight(1f)) { Text("Barcode scannen") }
            }
            Text(
                "Barcode zoeken opent de scanner als die beschikbaar is. Vul de waarden handmatig in als er geen productdata wordt gevonden.",
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
                                Button(onClick = { onQuickAdd(food) }, modifier = Modifier.weight(1f)) { Text("Aan maaltijd") }
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
            Text("Receptmaker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Maak een herbruikbaar recept met opgeslagen producten, barcode- of handmatige ingrediënten, of gecontroleerde AI-suggesties.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.trainIqColors.mutedText,
            )
            NutritionTextField(value = recipeName, onValueChange = onRecipeNameChange, label = "Receptnaam", modifier = Modifier.fillMaxWidth())
            NutritionTextField(value = recipeNotes, onValueChange = onRecipeNotesChange, label = "Notities", modifier = Modifier.fillMaxWidth(), singleLine = false)
            NutritionNumberField(value = recipeCookedGrams, onValueChange = onRecipeCookedGramsChange, label = "Totaal bereid gewicht (optioneel)", modifier = Modifier.fillMaxWidth())
            Text("Opgeslagen ingrediënt: ${selectedFood?.name ?: "Kies een product uit Producten"}", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                NutritionNumberField(value = ingredientGrams, onValueChange = onIngredientGramsChange, label = "Ingrediënt gram", modifier = Modifier.weight(1f))
                Button(onClick = onAddIngredient, enabled = selectedFood != null, modifier = Modifier.weight(1f)) { Text("Toevoegen") }
            }
            HorizontalDivider()
            Text("Ingrediënt handmatig maken", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            NutritionTextField(value = quickIngredientName, onValueChange = onQuickIngredientNameChange, label = "Ingrediëntnaam", modifier = Modifier.fillMaxWidth())
            NutritionTextField(value = quickIngredientBarcode, onValueChange = onQuickIngredientBarcodeChange, label = "Barcode (optioneel)", modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                NutritionNumberField(value = quickIngredientKcal, onValueChange = onQuickIngredientKcalChange, label = "kcal / 100g", modifier = Modifier.weight(1f))
                NutritionNumberField(value = quickIngredientProtein, onValueChange = onQuickIngredientProteinChange, label = "Eiwit / 100g", modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                NutritionNumberField(value = quickIngredientCarbs, onValueChange = onQuickIngredientCarbsChange, label = "Koolhydraten / 100g", modifier = Modifier.weight(1f))
                NutritionNumberField(value = quickIngredientFat, onValueChange = onQuickIngredientFatChange, label = "Vet / 100g", modifier = Modifier.weight(1f))
            }
            OutlinedButton(onClick = onCreateIngredient, modifier = Modifier.fillMaxWidth()) { Text("Ingrediënt opslaan en toevoegen") }
            if (draft.isEmpty()) {
                EmptyStateCard(
                    title = "Nog geen ingrediënten",
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
            Button(onClick = onSave, enabled = draft.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("Recept opslaan") }
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
private fun RecipeTotalsCard(totalNutrition: NutritionFacts, totalGrams: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Recepttotalen", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
                                Button(onClick = { onUseInMeal(recipe) }, modifier = Modifier.weight(1f)) { Text("Aan maaltijd") }
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
    onMealTypeChange: (MealType) -> Unit,
    onMealNameChange: (String) -> Unit,
    onMealNotesChange: (String) -> Unit,
    onRemoveDraftItem: (Int) -> Unit,
    onSave: () -> Unit,
) {
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
            NutritionTextField(value = mealName, onValueChange = onMealNameChange, label = "Maaltijdnaam", modifier = Modifier.fillMaxWidth())
            NutritionTextField(value = mealNotes, onValueChange = onMealNotesChange, label = "Notities", modifier = Modifier.fillMaxWidth(), singleLine = false)
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
                        Text("$label • ${formatNumber(entry.gramsUsed)}g", maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onRemoveDraftItem(index) }) { Text("Verwijderen") }
                    }
                }
            }
            Button(onClick = onSave, enabled = mealDraft.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("Maaltijd opslaan") }
        }
    }
}

@Composable
private fun AiMealAnalysisCard(
    aiPreferences: AiPreferences,
    aiContext: String,
    editableItems: List<EditableAiItem>,
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
                EditableAiItemCard(item = item, onChange = { onChangeItem(index, it) }, onDelete = { onDeleteItem(index) })
            }
            if (editableItems.isNotEmpty()) {
                Button(onClick = onSaveToDraft) { Text("AI-items aan maaltijd toevoegen") }
            }
        }
    }
}

@Composable
private fun RecipeAiResultsCard(
    editableItems: List<EditableAiItem>,
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
                EditableAiItemCard(item = item, onChange = { onChangeItem(index, it) }, onDelete = { onDeleteItem(index) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAddAsIngredients) { Text("Als ingrediënten toevoegen") }
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
private fun EditableAiItemCard(item: EditableAiItem, onChange: (EditableAiItem) -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NutritionTextField(value = item.name, onValueChange = { onChange(item.copy(name = it)) }, label = "Naam", modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                NutritionNumberField(value = item.grams, onValueChange = { onChange(item.copy(grams = it)) }, label = "Grammen", modifier = Modifier.weight(1f))
                NutritionNumberField(value = item.calories, onValueChange = { onChange(item.copy(calories = it)) }, label = "Calorieën", modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                NutritionNumberField(value = item.protein, onValueChange = { onChange(item.copy(protein = it)) }, label = "Eiwit", modifier = Modifier.weight(1f))
                NutritionNumberField(value = item.carbs, onValueChange = { onChange(item.copy(carbs = it)) }, label = "Koolhydraten", modifier = Modifier.weight(1f))
                NutritionNumberField(value = item.fat, onValueChange = { onChange(item.copy(fat = it)) }, label = "Vet", modifier = Modifier.weight(1f))
            }
            item.confidence?.let { Text("Zekerheid: $it") }
            item.notes?.let { Text(it) }
            Text("Per 100g: ${per100Value(item.calories, item.grams.toNutritionNumberOrNull(max = 100_000.0) ?: 100.0)} kcal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.trainIqColors.mutedText)
            TextButton(onClick = onDelete) { Text("Item verwijderen") }
        }
    }
}

private data class EditableAiItem(
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

private fun formatNumber(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format("%.1f", value)

private fun formatNullableNumber(value: Double?): String = value?.let(::formatNumber).orEmpty()

private fun String.toNutritionNumberOrNull(max: Double): Double? {
    val parsed = trim().replace(',', '.').toDoubleOrNull() ?: return null
    return parsed.takeIf { it.isFinite() && it >= 0.0 && it <= max }
}

private fun per100Value(total: String, grams: Double): String {
    val parsed = total.toNutritionNumberOrNull(max = 100_000.0) ?: 0.0
    val normalized = if (grams <= 0.0) 0.0 else parsed / grams * 100.0
    return formatNumber(normalized)
}

private val MealType.dutchLabel: String
    get() = when (this) {
        MealType.BREAKFAST -> "Ochtend"
        MealType.LUNCH -> "Middag"
        MealType.DINNER -> "Avond"
        MealType.SNACK -> "Snacks"
    }
