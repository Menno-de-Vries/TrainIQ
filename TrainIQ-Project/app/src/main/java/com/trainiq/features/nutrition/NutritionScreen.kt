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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
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
import com.trainiq.domain.model.FoodItem
import com.trainiq.domain.model.FoodSourceType
import com.trainiq.domain.model.LoggedMeal
import com.trainiq.domain.model.MealAnalysisResult
import com.trainiq.domain.model.MealScanItem
import com.trainiq.domain.model.MealType
import com.trainiq.domain.model.NutritionOverview
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

sealed interface NutritionUiState {
    data object Loading : NutritionUiState
    data class Success(
        val overview: NutritionOverview,
        val aiPreferences: AiPreferences,
        val scanResult: MealAnalysisResult? = null,
        val message: String? = null,
        val isAnalyzing: Boolean = false,
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
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NutritionUiState.Loading)

    fun analyze(path: String, context: String, capturedAtMillis: Long) {
        viewModelScope.launch {
            val ai = aiPreferences.value
            if (!ai.enabled) {
                ephemeral.update { it.copy(message = "AI is disabled in Settings. Nutrition still works fully with manual entry.") }
                return@launch
            }
            if (ai.apiKey.isBlank()) {
                ephemeral.update { it.copy(message = "No Gemini API key is configured. Add one in Settings or keep using manual flows.") }
                return@launch
            }
            ephemeral.update { it.copy(isAnalyzing = true, message = null) }
            runCatching { analyzeMealUseCase(path, context, capturedAtMillis) }
                .onSuccess {
                    ephemeral.update { state ->
                        state.copy(
                            scanResult = it,
                            message = if (it.items.isEmpty()) {
                                "No reliable meal estimate was returned. You can retry or enter it manually."
                            } else {
                                it.notes ?: "Review the AI estimate before adding it to your meal."
                            },
                            isAnalyzing = false,
                        )
                    }
                }
                .onFailure {
                    ephemeral.update { it.copy(message = "Meal analysis failed. You can retry or continue with manual entry.", isAnalyzing = false) }
                }
        }
    }

    fun saveFood(
        name: String,
        barcode: String?,
        calories: String,
        protein: String,
        carbs: String,
        fat: String,
        sourceType: FoodSourceType,
        onSaved: (FoodItem) -> Unit = {},
    ) {
        val parsedCalories = calories.toDoubleOrNull()
        val parsedProtein = protein.toDoubleOrNull()
        val parsedCarbs = carbs.toDoubleOrNull()
        val parsedFat = fat.toDoubleOrNull()
        if (name.isBlank() || parsedCalories == null || parsedProtein == null || parsedCarbs == null || parsedFat == null) {
            ephemeral.update { it.copy(message = "Enter a name and valid per-100g values first.") }
            return
        }
        viewModelScope.launch {
            val item = saveFoodItemUseCase(null, name, barcode, parsedCalories, parsedProtein, parsedCarbs, parsedFat, sourceType)
            ephemeral.update { it.copy(message = "Saved ${item.name}.") }
            onSaved(item)
        }
    }

    fun saveRecipe(name: String, notes: String, cookedGrams: String, ingredients: List<Pair<Long, Double>>) {
        if (name.isBlank() || ingredients.isEmpty()) {
            ephemeral.update { it.copy(message = "Add a recipe name and at least one ingredient.") }
            return
        }
        viewModelScope.launch {
            saveRecipeUseCase(null, name, notes, cookedGrams.toDoubleOrNull(), ingredients)
            ephemeral.update { it.copy(message = "Recipe saved.") }
        }
    }

    fun saveMeal(mealType: MealType, name: String, notes: String, items: List<MealEntryRequest>, onSaved: () -> Unit = {}) {
        if (name.isBlank() || items.isEmpty()) {
            ephemeral.update { it.copy(message = "Add a meal name and at least one item before saving.") }
            return
        }
        viewModelScope.launch {
            saveMealUseCase(null, mealType, name, notes, items)
            ephemeral.update { it.copy(message = "Meal saved.") }
            onSaved()
        }
    }

    fun deleteMeal(mealId: Long) {
        viewModelScope.launch {
            deleteMealUseCase(mealId)
            ephemeral.update { it.copy(message = "Meal deleted.") }
        }
    }

    fun deleteFood(foodId: Long) {
        viewModelScope.launch {
            deleteFoodUseCase(foodId)
            ephemeral.update { it.copy(message = "Food deleted.") }
        }
    }

    fun deleteRecipe(recipeId: Long) {
        viewModelScope.launch {
            deleteRecipeUseCase(recipeId)
            ephemeral.update { it.copy(message = "Recipe deleted.") }
        }
    }

    fun setMessage(message: String?) {
        ephemeral.update { it.copy(message = message) }
    }

    fun setScanResult(result: MealAnalysisResult?) {
        ephemeral.update { it.copy(scanResult = result) }
        if (result == null) clearLastScanResultUseCase()
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
        onDismissMessage = { viewModel.setMessage(null) },
        onOpenAiScanner = onOpenAiScanner,
        onOpenBarcodeScanner = onOpenBarcodeScanner,
        pendingBarcode = pendingBarcode,
        onBarcodeClear = onBarcodeClear,
    )
}

@Composable
fun NutritionScreen(
    uiState: NutritionUiState,
    onSaveFood: (String, String?, String, String, String, String, FoodSourceType, (FoodItem) -> Unit) -> Unit,
    onSaveRecipe: (String, String, String, List<Pair<Long, Double>>) -> Unit,
    onSaveMeal: (MealType, String, String, List<MealEntryRequest>, () -> Unit) -> Unit,
    onDeleteMeal: (Long) -> Unit,
    onDeleteFood: (Long) -> Unit,
    onDeleteRecipe: (Long) -> Unit,
    onSetScanResult: (MealAnalysisResult?) -> Unit,
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
    val recipeDraft = remember { mutableStateListOf<Pair<Long, Double>>() }

    var mealType by remember { mutableStateOf(MealType.LUNCH) }
    var mealName by remember { mutableStateOf("Lunch") }
    var mealNotes by remember { mutableStateOf("") }
    var mealFoodGrams by remember { mutableStateOf("100") }
    var mealRecipeGrams by remember { mutableStateOf("150") }
    val mealDraft = remember { mutableStateListOf<MealEntryRequest>() }

    var aiContext by remember { mutableStateOf("") }
    val editableAiItems = remember { mutableStateListOf<EditableAiItem>() }

    LaunchedEffect(pendingBarcode) {
        if (pendingBarcode != null) {
            barcode = pendingBarcode
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

    LaunchedEffect(scanResult) {
        editableAiItems.clear()
        scanResult?.items?.forEach { editableAiItems += EditableAiItem.from(it) }
        scanResult?.suggestedMealType?.let {
            mealType = it
            if (mealName in listOf("Breakfast", "Lunch", "Dinner", "Snack")) {
                mealName = it.label
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = uiState,
            transitionSpec = { fadeIn(animationSpec = tween(240)) togetherWith fadeOut(animationSpec = tween(180)) },
            label = "nutrition-ui-state",
        ) { state ->
            if (state is NutritionUiState.Loading) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(MaterialTheme.spacing.medium),
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
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(MaterialTheme.spacing.medium),
                ) {
                    item { MessageCard(message = state.message) }
                }
                return@AnimatedContent
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(MaterialTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            ) {
                item { ScreenHeader(title = "Nutrition") }
                message?.let {
                    item { MessageCard(message = it, onDismiss = onDismissMessage) }
                }
                item { SummaryCard(overview) }
                item { QuickDraftCard(mealType, selectedFood, selectedRecipe, mealDraft.size, recipeDraft.size) }
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
                        onScanBarcode = onOpenBarcodeScanner,
                        onSave = {
                            onSaveFood(foodName, barcode, calories, protein, carbs, fat, if (barcode.isBlank()) FoodSourceType.MANUAL else FoodSourceType.BARCODE) {
                                selectedFoodId = it.id
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
                        onQuickAdd = { food -> mealDraft += MealEntryRequest(MealEntryType.FOOD, food.id, mealFoodGrams.toDoubleOrNull() ?: 100.0) },
                        onDelete = onDeleteFood,
                    )
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
                        onRecipeNameChange = { recipeName = it },
                        onRecipeNotesChange = { recipeNotes = it },
                        onRecipeCookedGramsChange = { recipeCookedGrams = it },
                        onIngredientGramsChange = { ingredientGrams = it },
                        onAddIngredient = {
                            val grams = ingredientGrams.toDoubleOrNull()
                            val foodId = selectedFoodId
                            if (grams != null && foodId != null) {
                                recipeDraft += foodId to grams
                                ingredientGrams = ""
                            }
                        },
                        onRemoveIngredient = { index -> recipeDraft.removeAt(index) },
                        onSave = {
                            onSaveRecipe(recipeName, recipeNotes, recipeCookedGrams, recipeDraft.toList())
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
                        onSelect = { selectedRecipeId = it },
                        onUseInMeal = { recipe -> mealDraft += MealEntryRequest(MealEntryType.RECIPE, recipe.id, mealRecipeGrams.toDoubleOrNull() ?: 150.0) },
                        onDelete = onDeleteRecipe,
                    )
                }
                item {
                    MealLoggerCard(
                        mealType = mealType,
                        mealName = mealName,
                        mealNotes = mealNotes,
                        mealFoodGrams = mealFoodGrams,
                        mealRecipeGrams = mealRecipeGrams,
                        selectedFood = selectedFood,
                        selectedRecipe = selectedRecipe,
                        mealDraft = mealDraft.toList(),
                        foods = overview?.foods.orEmpty(),
                        recipes = overview?.recipes.orEmpty(),
                        onMealTypeChange = { mealType = it },
                        onMealNameChange = { mealName = it },
                        onMealNotesChange = { mealNotes = it },
                        onMealFoodGramsChange = { mealFoodGrams = it },
                        onMealRecipeGramsChange = { mealRecipeGrams = it },
                        onAddFood = {
                            val foodId = selectedFoodId
                            val grams = mealFoodGrams.toDoubleOrNull()
                            if (foodId != null && grams != null) mealDraft += MealEntryRequest(MealEntryType.FOOD, foodId, grams)
                        },
                        onAddRecipe = {
                            val recipeId = selectedRecipeId
                            val grams = mealRecipeGrams.toDoubleOrNull()
                            if (recipeId != null && grams != null) mealDraft += MealEntryRequest(MealEntryType.RECIPE, recipeId, grams)
                        },
                        onRemoveDraftItem = { index -> mealDraft.removeAt(index) },
                        onSave = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSaveMeal(mealType, mealName, mealNotes, mealDraft.toList()) {
                                mealNotes = ""
                                mealDraft.clear()
                                onSetScanResult(null)
                            }
                        },
                    )
                }
                item {
                    AiMealAnalysisCard(
                        aiPreferences = aiPreferences,
                        aiContext = aiContext,
                        editableItems = editableAiItems,
                        isAnalyzing = isAnalyzing,
                        onContextChange = { aiContext = it },
                        onOpenCamera = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onOpenAiScanner(aiContext)
                        },
                        onChangeItem = { index, item -> editableAiItems[index] = item },
                        onDeleteItem = { index -> editableAiItems.removeAt(index) },
                        onSaveToDraft = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            editableAiItems.forEach { item ->
                                val grams = item.grams.toDoubleOrNull() ?: 100.0
                                onSaveFood(
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
                        },
                        onDeleteMeal = onDeleteMeal,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(overview: NutritionOverview?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
            Text("Today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Calories ${formatNumber(overview?.todaysCalories ?: 0.0)}")
            Text("Protein ${formatNumber(overview?.todaysProtein ?: 0.0)}g • Carbs ${formatNumber(overview?.todaysCarbs ?: 0.0)}g • Fat ${formatNumber(overview?.todaysFat ?: 0.0)}g")
            Text("Foods ${overview?.foods?.size ?: 0} • Recipes ${overview?.recipes?.size ?: 0} • Meals ${overview?.meals?.size ?: 0}")
            overview?.energyBalance?.let {
                Text("Energy balance ${it.balance} kcal • TEF ${it.tefCalories} • NEAT ${it.neatCalories} • EAT ${it.workoutCalories}")
            }
            overview?.todaysMealsByType?.forEach { (mealType, meals) ->
                if (meals.isNotEmpty()) {
                    Text("${mealType.label}: ${meals.size} logged")
                }
            }
        }
    }
}

@Composable
private fun QuickDraftCard(
    mealType: MealType,
    selectedFood: FoodItem?,
    selectedRecipe: Recipe?,
    mealDraftCount: Int,
    recipeDraftCount: Int,
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Current selection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Meal type: ${mealType.label}")
            Text("Food: ${selectedFood?.name ?: "None selected"}")
            Text("Recipe: ${selectedRecipe?.name ?: "None selected"}")
            Text("Recipe draft items: $recipeDraftCount • Meal draft items: $mealDraftCount")
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
            Text("Food item", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(value = foodName, onValueChange = onFoodNameChange, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = barcode, onValueChange = onBarcodeChange, label = { Text("Barcode (optional)") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = calories, onValueChange = onCaloriesChange, label = { Text("kcal / 100g") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = protein, onValueChange = onProteinChange, label = { Text("Protein") }, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = carbs, onValueChange = onCarbsChange, label = { Text("Carbs") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = fat, onValueChange = onFatChange, label = { Text("Fat") }, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSave) { Text("Save item") }
                OutlinedButton(onClick = onScanBarcode) { Text("Scan barcode") }
            }
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
            Text("Saved foods", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (foods.isEmpty()) {
                Text("Nothing saved yet.")
            } else {
                foods.forEach { food ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        FilterChip(
                            selected = selectedFoodId == food.id,
                            onClick = { onSelect(food.id) },
                            label = { Text("${food.name} • ${formatNumber(food.caloriesPer100g)} kcal/100g", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                        Row {
                            TextButton(onClick = { onQuickAdd(food) }) { Text("Add") }
                            TextButton(onClick = { onDelete(food.id) }) { Text("Delete") }
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
    onRecipeNameChange: (String) -> Unit,
    onRecipeNotesChange: (String) -> Unit,
    onRecipeCookedGramsChange: (String) -> Unit,
    onIngredientGramsChange: (String) -> Unit,
    onAddIngredient: () -> Unit,
    onRemoveIngredient: (Int) -> Unit,
    onSave: () -> Unit,
) {
    val totalCalories = draft.sumOf { (foodId, grams) -> foods.firstOrNull { it.id == foodId }?.let { food -> food.caloriesPer100g * grams / 100.0 } ?: 0.0 }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
            Text("Recipe builder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(value = recipeName, onValueChange = onRecipeNameChange, label = { Text("Recipe name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = recipeNotes, onValueChange = onRecipeNotesChange, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = recipeCookedGrams, onValueChange = onRecipeCookedGramsChange, label = { Text("Total cooked grams (optional)") }, modifier = Modifier.fillMaxWidth())
            Text("Selected ingredient: ${selectedFood?.name ?: "Choose a saved food"}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = ingredientGrams, onValueChange = onIngredientGramsChange, label = { Text("Ingredient grams") }, modifier = Modifier.weight(1f))
                Button(onClick = onAddIngredient) { Text("Add") }
            }
            if (draft.isEmpty()) {
                Text("Add ingredients to build the recipe total.")
            } else {
                draft.forEachIndexed { index, (foodId, grams) ->
                    val food = foods.firstOrNull { it.id == foodId }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${food?.name ?: "Food"} • ${formatNumber(grams)}g")
                        TextButton(onClick = { onRemoveIngredient(index) }) { Text("Remove") }
                    }
                }
                Text("Estimated total: ${formatNumber(totalCalories)} kcal")
            }
            Button(onClick = onSave, enabled = draft.isNotEmpty()) { Text("Save recipe") }
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
            Text("Saved recipes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (recipes.isEmpty()) {
                Text("No recipes saved yet.")
            } else {
                recipes.forEach { recipe ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        FilterChip(
                            selected = selectedRecipeId == recipe.id,
                            onClick = { onSelect(recipe.id) },
                            label = { Text("${recipe.name} • ${formatNumber(recipe.totalNutrition.calories)} kcal", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                        Row {
                            TextButton(onClick = { onUseInMeal(recipe) }) { Text("Add") }
                            TextButton(onClick = { onDelete(recipe.id) }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealLoggerCard(
    mealType: MealType,
    mealName: String,
    mealNotes: String,
    mealFoodGrams: String,
    mealRecipeGrams: String,
    selectedFood: FoodItem?,
    selectedRecipe: Recipe?,
    mealDraft: List<MealEntryRequest>,
    foods: List<FoodItem>,
    recipes: List<Recipe>,
    onMealTypeChange: (MealType) -> Unit,
    onMealNameChange: (String) -> Unit,
    onMealNotesChange: (String) -> Unit,
    onMealFoodGramsChange: (String) -> Unit,
    onMealRecipeGramsChange: (String) -> Unit,
    onAddFood: () -> Unit,
    onAddRecipe: () -> Unit,
    onRemoveDraftItem: (Int) -> Unit,
    onSave: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
            Text("Meal logger", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MealType.entries.forEach { option ->
                    FilterChip(
                        selected = mealType == option,
                        onClick = {
                            onMealTypeChange(option)
                            if (mealName in listOf("Breakfast", "Lunch", "Dinner", "Snack")) {
                                onMealNameChange(option.label)
                            }
                        },
                        label = { Text(option.label) },
                    )
                }
            }
            OutlinedTextField(value = mealName, onValueChange = onMealNameChange, label = { Text("Meal name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = mealNotes, onValueChange = onMealNotesChange, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
            Text("Food selection: ${selectedFood?.name ?: "None"}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = mealFoodGrams, onValueChange = onMealFoodGramsChange, label = { Text("Food grams") }, modifier = Modifier.weight(1f))
                Button(onClick = onAddFood, enabled = selectedFood != null) { Text("Add food") }
            }
            Text("Recipe selection: ${selectedRecipe?.name ?: "None"}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = mealRecipeGrams, onValueChange = onMealRecipeGramsChange, label = { Text("Recipe grams") }, modifier = Modifier.weight(1f))
                Button(onClick = onAddRecipe, enabled = selectedRecipe != null) { Text("Add recipe") }
            }
            if (mealDraft.isEmpty()) {
                Text("Build the meal by adding foods or a recipe.")
            } else {
                mealDraft.forEachIndexed { index, entry ->
                    val label = when (entry.itemType) {
                        MealEntryType.FOOD -> foods.firstOrNull { it.id == entry.referenceId }?.name
                        MealEntryType.RECIPE -> recipes.firstOrNull { it.id == entry.referenceId }?.name
                    } ?: "Item"
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("$label • ${formatNumber(entry.gramsUsed)}g")
                        TextButton(onClick = { onRemoveDraftItem(index) }) { Text("Remove") }
                    }
                }
            }
            Button(onClick = onSave, enabled = mealDraft.isNotEmpty()) { Text("Save meal") }
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
            Text("Primary action", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("Scan meal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                when {
                    !aiPreferences.enabled -> "AI is disabled in Settings. Manual nutrition logging still works."
                    aiPreferences.apiKey.isBlank() -> "Add a Gemini API key in Settings to enable meal analysis."
                    else -> "Open the full-screen scanner, capture the meal, then review each detected item before saving."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(value = aiContext, onValueChange = onContextChange, label = { Text("Optional context") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = onOpenCamera, enabled = aiPreferences.enabled && aiPreferences.apiKey.isNotBlank()) {
                Text("Open scanner")
            }
            if (isAnalyzing) {
                Text("Processing...", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                repeat(3) {
                    ShimmerCardPlaceholder(lineCount = 2)
                }
            }
            editableItems.forEachIndexed { index, item ->
                EditableAiItemCard(item = item, onChange = { onChangeItem(index, it) }, onDelete = { onDeleteItem(index) })
            }
            if (editableItems.isNotEmpty()) {
                Button(onClick = onSaveToDraft) { Text("Add AI items to meal draft") }
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
            Text("Meal history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (meals.isEmpty()) {
                Text("Saved meals will appear here once you log food or recipes.")
            } else {
                meals.forEach { meal ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(meal.name, fontWeight = FontWeight.SemiBold)
                            Text(meal.mealType.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text("${formatNumber(meal.totalNutrition.calories)} kcal • P${formatNumber(meal.totalNutrition.protein)} C${formatNumber(meal.totalNutrition.carbs)} F${formatNumber(meal.totalNutrition.fat)}")
                            meal.items.forEach { item ->
                                val liveName = when (item.itemType.name) {
                                    "FOOD" -> foods.firstOrNull { it.id == item.referenceId }?.name
                                    else -> recipes.firstOrNull { it.id == item.referenceId }?.name
                                } ?: item.name
                                Text("$liveName • ${formatNumber(item.gramsUsed)}g")
                            }
                            Row {
                                TextButton(onClick = { onReuseMeal(meal) }) { Text("Reuse") }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(onClick = { onDeleteMeal(meal.id) }) { Text("Delete") }
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
            OutlinedTextField(value = item.name, onValueChange = { onChange(item.copy(name = it)) }, label = { Text("Item") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = item.grams, onValueChange = { onChange(item.copy(grams = it)) }, label = { Text("Grams") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = item.calories, onValueChange = { onChange(item.copy(calories = it)) }, label = { Text("Calories") }, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = item.protein, onValueChange = { onChange(item.copy(protein = it)) }, label = { Text("Protein") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = item.carbs, onValueChange = { onChange(item.copy(carbs = it)) }, label = { Text("Carbs") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = item.fat, onValueChange = { onChange(item.copy(fat = it)) }, label = { Text("Fat") }, modifier = Modifier.weight(1f))
            }
            item.confidence?.let { Text("Confidence: $it") }
            item.notes?.let { Text(it) }
            Text("Per-100g preview: ${per100Value(item.calories, item.grams.toDoubleOrNull() ?: 100.0)} kcal")
            TextButton(onClick = onDelete) { Text("Remove item") }
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

private fun per100Value(total: String, grams: Double): String {
    val parsed = total.toDoubleOrNull() ?: 0.0
    val normalized = if (grams <= 0.0) 0.0 else parsed / grams * 100.0
    return formatNumber(normalized)
}
