package com.trainiq.features.nutrition

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.trainiq.core.datastore.AiPreferences
import com.trainiq.core.datastore.UserPreferencesRepository
import com.trainiq.domain.model.FoodItem
import com.trainiq.domain.model.FoodSourceType
import com.trainiq.domain.model.LoggedMeal
import com.trainiq.domain.model.MealAnalysisResult
import com.trainiq.domain.model.MealScanItem
import com.trainiq.domain.model.NutritionOverview
import com.trainiq.domain.model.Recipe
import com.trainiq.domain.repository.MealEntryRequest
import com.trainiq.domain.repository.MealEntryType
import com.trainiq.domain.usecase.AnalyzeMealUseCase
import com.trainiq.domain.usecase.DeleteFoodUseCase
import com.trainiq.domain.usecase.DeleteMealUseCase
import com.trainiq.domain.usecase.DeleteRecipeUseCase
import com.trainiq.domain.usecase.ObserveNutritionUseCase
import com.trainiq.domain.usecase.SaveFoodItemUseCase
import com.trainiq.domain.usecase.SaveMealUseCase
import com.trainiq.domain.usecase.SaveRecipeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
) : ViewModel() {
    val overview: StateFlow<NutritionOverview?> = observeNutritionUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val aiPreferences: StateFlow<AiPreferences> = preferencesRepository.aiPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AiPreferences(false, ""))

    private val _scanResult = MutableStateFlow<MealAnalysisResult?>(null)
    val scanResult: StateFlow<MealAnalysisResult?> = _scanResult.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun analyze(path: String, context: String) {
        viewModelScope.launch {
            val ai = aiPreferences.value
            if (!ai.enabled) {
                _message.value = "AI is disabled in Settings. Nutrition still works fully with manual entry."
                return@launch
            }
            if (ai.apiKey.isBlank()) {
                _message.value = "No Gemini API key is configured. Add one in Settings or keep using manual flows."
                return@launch
            }
            runCatching { analyzeMealUseCase(path, context) }
                .onSuccess {
                    _scanResult.value = it
                    _message.value = if (it.items.isEmpty()) {
                        "No reliable meal estimate was returned. You can retry or enter it manually."
                    } else {
                        it.notes ?: "Review the AI estimate before adding it to your meal."
                    }
                }
                .onFailure {
                    _message.value = "Meal analysis failed. You can retry or continue with manual entry."
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
            _message.value = "Enter a name and valid per-100g values first."
            return
        }
        viewModelScope.launch {
            val item = saveFoodItemUseCase(null, name, barcode, parsedCalories, parsedProtein, parsedCarbs, parsedFat, sourceType)
            _message.value = "Saved ${item.name}."
            onSaved(item)
        }
    }

    fun saveRecipe(name: String, notes: String, cookedGrams: String, ingredients: List<Pair<Long, Double>>) {
        if (name.isBlank() || ingredients.isEmpty()) {
            _message.value = "Add a recipe name and at least one ingredient."
            return
        }
        viewModelScope.launch {
            saveRecipeUseCase(null, name, notes, cookedGrams.toDoubleOrNull(), ingredients)
            _message.value = "Recipe saved."
        }
    }

    fun saveMeal(name: String, notes: String, items: List<MealEntryRequest>, onSaved: () -> Unit = {}) {
        if (name.isBlank() || items.isEmpty()) {
            _message.value = "Add a meal name and at least one item before saving."
            return
        }
        viewModelScope.launch {
            saveMealUseCase(null, name, notes, items)
            _message.value = "Meal saved."
            onSaved()
        }
    }

    fun deleteMeal(mealId: Long) {
        viewModelScope.launch {
            deleteMealUseCase(mealId)
            _message.value = "Meal deleted."
        }
    }

    fun deleteFood(foodId: Long) {
        viewModelScope.launch {
            deleteFoodUseCase(foodId)
            _message.value = "Food deleted."
        }
    }

    fun deleteRecipe(recipeId: Long) {
        viewModelScope.launch {
            deleteRecipeUseCase(recipeId)
            _message.value = "Recipe deleted."
        }
    }

    fun setMessage(message: String?) {
        _message.value = message
    }

    fun setScanResult(result: MealAnalysisResult?) {
        _scanResult.value = result
    }
}

@Composable
fun NutritionRoute(viewModel: NutritionViewModel = hiltViewModel()) {
    val overview by viewModel.overview.collectAsStateWithLifecycle()
    val aiPreferences by viewModel.aiPreferences.collectAsStateWithLifecycle()
    val scanResult by viewModel.scanResult.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    NutritionScreen(
        overview = overview,
        aiPreferences = aiPreferences,
        scanResult = scanResult,
        message = message,
        onAnalyze = viewModel::analyze,
        onSaveFood = viewModel::saveFood,
        onSaveRecipe = viewModel::saveRecipe,
        onSaveMeal = viewModel::saveMeal,
        onDeleteMeal = viewModel::deleteMeal,
        onDeleteFood = viewModel::deleteFood,
        onDeleteRecipe = viewModel::deleteRecipe,
        onSetScanResult = viewModel::setScanResult,
        onDismissMessage = { viewModel.setMessage(null) },
    )
}

@Composable
fun NutritionScreen(
    overview: NutritionOverview?,
    aiPreferences: AiPreferences,
    scanResult: MealAnalysisResult?,
    message: String?,
    onAnalyze: (String, String) -> Unit,
    onSaveFood: (String, String?, String, String, String, String, FoodSourceType, (FoodItem) -> Unit) -> Unit,
    onSaveRecipe: (String, String, String, List<Pair<Long, Double>>) -> Unit,
    onSaveMeal: (String, String, List<MealEntryRequest>, () -> Unit) -> Unit,
    onDeleteMeal: (Long) -> Unit,
    onDeleteFood: (Long) -> Unit,
    onDeleteRecipe: (Long) -> Unit,
    onSetScanResult: (MealAnalysisResult?) -> Unit,
    onDismissMessage: () -> Unit,
) {
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
    var showBarcodeScanner by remember { mutableStateOf(false) }

    var recipeName by remember { mutableStateOf("") }
    var recipeNotes by remember { mutableStateOf("") }
    var recipeCookedGrams by remember { mutableStateOf("") }
    var ingredientGrams by remember { mutableStateOf("100") }
    val recipeDraft = remember { mutableStateListOf<Pair<Long, Double>>() }

    var mealName by remember { mutableStateOf("Lunch") }
    var mealNotes by remember { mutableStateOf("") }
    var mealFoodGrams by remember { mutableStateOf("100") }
    var mealRecipeGrams by remember { mutableStateOf("150") }
    val mealDraft = remember { mutableStateListOf<MealEntryRequest>() }

    var aiContext by remember { mutableStateOf("") }
    var showAiCamera by remember { mutableStateOf(false) }
    val editableAiItems = remember { mutableStateListOf<EditableAiItem>() }

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
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text("Nutrition", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        message?.let {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(it, modifier = Modifier.weight(1f))
                        TextButton(onClick = onDismissMessage) { Text("Dismiss") }
                    }
                }
            }
        }
        item { SummaryCard(overview) }
        item { QuickDraftCard(selectedFood, selectedRecipe, mealDraft.size, recipeDraft.size) }
        item {
            FoodEditorCard(
                foodName = foodName,
                barcode = barcode,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                showBarcodeScanner = showBarcodeScanner,
                onFoodNameChange = { foodName = it },
                onBarcodeChange = { barcode = it },
                onCaloriesChange = { calories = it },
                onProteinChange = { protein = it },
                onCarbsChange = { carbs = it },
                onFatChange = { fat = it },
                onToggleBarcode = { showBarcodeScanner = !showBarcodeScanner },
                onBarcodeDetected = { barcode = it },
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
                mealName = mealName,
                mealNotes = mealNotes,
                mealFoodGrams = mealFoodGrams,
                mealRecipeGrams = mealRecipeGrams,
                selectedFood = selectedFood,
                selectedRecipe = selectedRecipe,
                mealDraft = mealDraft.toList(),
                foods = overview?.foods.orEmpty(),
                recipes = overview?.recipes.orEmpty(),
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
                    onSaveMeal(mealName, mealNotes, mealDraft.toList()) {
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
                showCamera = showAiCamera,
                onContextChange = { aiContext = it },
                onToggleCamera = { showAiCamera = !showAiCamera },
                onAnalyze = onAnalyze,
                onChangeItem = { index, item -> editableAiItems[index] = item },
                onDeleteItem = { index -> editableAiItems.removeAt(index) },
                onSaveToDraft = {
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

@Composable
private fun SummaryCard(overview: NutritionOverview?) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Calories ${formatNumber(overview?.todaysCalories ?: 0.0)}")
            Text("Protein ${formatNumber(overview?.todaysProtein ?: 0.0)}g • Carbs ${formatNumber(overview?.todaysCarbs ?: 0.0)}g • Fat ${formatNumber(overview?.todaysFat ?: 0.0)}g")
            Text("Foods ${overview?.foods?.size ?: 0} • Recipes ${overview?.recipes?.size ?: 0} • Meals ${overview?.meals?.size ?: 0}")
        }
    }
}

@Composable
private fun QuickDraftCard(selectedFood: FoodItem?, selectedRecipe: Recipe?, mealDraftCount: Int, recipeDraftCount: Int) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Current selection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
    showBarcodeScanner: Boolean,
    onFoodNameChange: (String) -> Unit,
    onBarcodeChange: (String) -> Unit,
    onCaloriesChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onCarbsChange: (String) -> Unit,
    onFatChange: (String) -> Unit,
    onToggleBarcode: () -> Unit,
    onBarcodeDetected: (String) -> Unit,
    onSave: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                OutlinedButton(onClick = onToggleBarcode) { Text(if (showBarcodeScanner) "Hide scanner" else "Scan barcode") }
            }
            if (showBarcodeScanner) BarcodeScannerCard(onBarcodeDetected = onBarcodeDetected)
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
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    mealName: String,
    mealNotes: String,
    mealFoodGrams: String,
    mealRecipeGrams: String,
    selectedFood: FoodItem?,
    selectedRecipe: Recipe?,
    mealDraft: List<MealEntryRequest>,
    foods: List<FoodItem>,
    recipes: List<Recipe>,
    onMealNameChange: (String) -> Unit,
    onMealNotesChange: (String) -> Unit,
    onMealFoodGramsChange: (String) -> Unit,
    onMealRecipeGramsChange: (String) -> Unit,
    onAddFood: () -> Unit,
    onAddRecipe: () -> Unit,
    onRemoveDraftItem: (Int) -> Unit,
    onSave: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Meal logger", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
    showCamera: Boolean,
    onContextChange: (String) -> Unit,
    onToggleCamera: () -> Unit,
    onAnalyze: (String, String) -> Unit,
    onChangeItem: (Int, EditableAiItem) -> Unit,
    onDeleteItem: (Int) -> Unit,
    onSaveToDraft: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("AI meal analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                when {
                    !aiPreferences.enabled -> "AI is disabled in Settings. Manual nutrition logging still works."
                    aiPreferences.apiKey.isBlank() -> "Add a Gemini API key in Settings to enable meal analysis."
                    else -> "Capture a meal photo, add context if helpful, then review each detected item before saving."
                },
            )
            OutlinedTextField(value = aiContext, onValueChange = onContextChange, label = { Text("Optional context") }, modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = onToggleCamera) { Text(if (showCamera) "Hide camera" else "Open camera") }
            if (showCamera) {
                AiCaptureCard(enabled = aiPreferences.enabled && aiPreferences.apiKey.isNotBlank(), onAnalyze = { path -> onAnalyze(path, aiContext) })
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
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Meal history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (meals.isEmpty()) {
                Text("Saved meals will appear here once you log food or recipes.")
            } else {
                meals.forEach { meal ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(meal.name, fontWeight = FontWeight.SemiBold)
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
private fun BarcodeScannerCard(onBarcodeDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }
    LaunchedEffect(Unit) { permissionLauncher.launch(Manifest.permission.CAMERA) }
    if (!hasPermission) {
        Text("Camera permission is required to scan barcodes.")
        return
    }
    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
    }
    DisposableEffect(controller, lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ALL_FORMATS)
                .build(),
        )
        controller.setImageAnalysisAnalyzer(ContextCompat.getMainExecutor(context)) { image ->
            processBarcodeImage(image, scanner, onBarcodeDetected)
        }
        onDispose { controller.clearImageAnalysisAnalyzer() }
    }
    AndroidView(factory = { PreviewView(it).apply { this.controller = controller } }, modifier = Modifier.fillMaxWidth().height(200.dp))
}

@Composable
private fun AiCaptureCard(enabled: Boolean, onAnalyze: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }
    LaunchedEffect(Unit) { permissionLauncher.launch(Manifest.permission.CAMERA) }
    if (!hasPermission) {
        Text("Camera permission is required for AI meal analysis.")
        return
    }
    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            bindToLifecycle(lifecycleOwner)
        }
    }
    cameraError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    AndroidView(factory = { PreviewView(it).apply { this.controller = controller } }, modifier = Modifier.fillMaxWidth().height(220.dp))
    Button(onClick = { takePhoto(context, controller, onAnalyze) { cameraError = it } }, enabled = enabled) { Text("Analyze meal") }
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

private fun processBarcodeImage(
    imageProxy: ImageProxy,
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    onBarcodeDetected: (String) -> Unit,
) {
    val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    scanner.process(image)
        .addOnSuccessListener { codes -> codes.firstOrNull()?.rawValue?.let(onBarcodeDetected) }
        .addOnCompleteListener { imageProxy.close() }
}

private fun takePhoto(
    context: Context,
    controller: LifecycleCameraController,
    onPhotoSaved: (String) -> Unit,
    onError: (String) -> Unit,
) {
    val file = File(context.cacheDir, "meal-${System.currentTimeMillis()}.jpg")
    val output = ImageCapture.OutputFileOptions.Builder(file).build()
    controller.takePicture(
        output,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onPhotoSaved(file.absolutePath)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("TrainIQ", "Camera capture failed", exception)
                onError("Unable to capture a photo on this device.")
            }
        },
    )
}

private fun formatNumber(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format("%.1f", value)

private fun formatNullableNumber(value: Double?): String = value?.let(::formatNumber).orEmpty()

private fun per100Value(total: String, grams: Double): String {
    val parsed = total.toDoubleOrNull() ?: 0.0
    val normalized = if (grams <= 0.0) 0.0 else parsed / grams * 100.0
    return formatNumber(normalized)
}
