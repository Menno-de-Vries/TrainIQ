package com.trainiq.data.repository

import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonParser
import com.trainiq.core.database.ActiveWorkoutCollapsedExerciseEntity
import com.trainiq.core.database.ActiveWorkoutDraftEntity
import com.trainiq.core.database.ActiveWorkoutSessionEntity
import com.trainiq.core.database.ActiveWorkoutSetEntity
import com.trainiq.core.database.BodyMeasurementEntity
import com.trainiq.core.database.ExerciseEntity
import com.trainiq.core.database.FoodItemEntity
import com.trainiq.core.database.MealEntity
import com.trainiq.core.database.MealItemEntity
import com.trainiq.core.database.PerformedExerciseEntity
import com.trainiq.core.database.RecipeEntity
import com.trainiq.core.database.RecipeIngredientEntity
import com.trainiq.core.database.RoutineSetEntity
import com.trainiq.core.database.TrainIqDatabase
import com.trainiq.core.database.UserProfileEntity
import com.trainiq.core.database.WorkoutDayEntity
import com.trainiq.core.database.WorkoutExerciseEntity
import com.trainiq.core.database.WorkoutLogEventEntity
import com.trainiq.core.database.WorkoutLogEventSetEntity
import com.trainiq.core.database.WorkoutRoutineEntity
import com.trainiq.core.database.WorkoutSessionEntity
import com.trainiq.core.database.WorkoutSetEntity
import com.trainiq.data.local.FoodItemStorage
import com.trainiq.data.local.LoggedMealStorage
import com.trainiq.data.local.LoggedMealItemStorage
import com.trainiq.data.local.RecipeStorage
import com.trainiq.data.local.RecipeIngredientStorage
import com.trainiq.data.local.TrainIqLocalStore
import com.trainiq.data.migration.JsonRoomImportPlanner
import com.trainiq.data.migration.RoomImportDryRun
import com.trainiq.data.migration.RoomJsonImportSink
import com.trainiq.data.migration.RoomMigrationChainVerificationMarker
import com.trainiq.data.migration.RoomMigrationChainVerificationMarkerSource
import com.trainiq.data.migration.RoomMigrationChainVerificationProvider
import com.trainiq.data.migration.RoomRuntimeReadinessGate
import com.trainiq.domain.model.FoodSourceType
import com.trainiq.domain.usecase.ExportAppDataUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TargetedRoomPersistenceInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dbName = "targeted-room-persistence-test.db"
    private lateinit var database: TrainIqDatabase
    private val storeJobs = mutableListOf<Job>()

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
        database = openDatabase()
    }

    @After
    fun tearDown() {
        closeDatabase()
        context.deleteDatabase(dbName)
    }

    @Test
    fun sequentialMealsAllocateDistinctIdsAndEditingPreservesOtherSnapshotsAfterReopen() = runTest {
        val store = runtimeStore()
        val items = listOf(LoggedMealItemStorage(id = 1, name = "Oats", calories = 120.0))
        val first = store.saveMeal(LoggedMealStorage(name = "First", timestamp = 1234L), items)
        val second = store.saveMeal(LoggedMealStorage(name = "Second"), items)
        assertTrue(first > 0 && second > 0 && first != second)
        store.saveMeal(LoggedMealStorage(id = first, name = "Edited"), items.map { it.copy(calories = 200.0) })
        closeDatabase()
        database = openDatabase()
        assertEquals(setOf("Edited", "Second"), database.dao().observeMeals().first().map { it.name }.toSet())
        assertEquals(1234L, database.dao().observeMeals().first().single { it.id == first }.date)
        val savedItems = database.dao().observeMealItems().first()
        assertEquals(2, savedItems.map { it.id }.toSet().size)
        assertEquals(120.0, savedItems.single { it.mealId == second }.calories, 0.0)
        assertEquals(200.0, savedItems.single { it.mealId == first }.calories, 0.0)
    }

    @Test
    fun sequentialRecipesAllocateDistinctIdsAndInvalidEditRollsBackAfterReopen() = runTest {
        val store = runtimeStore()
        val food = store.saveFood(FoodItemStorage(name = "Oats"))
        val ingredients = listOf(RecipeIngredientStorage(id = 1, foodItemId = food.id, gramsUsed = 100.0))
        val (first, firstIngredients) = store.saveRecipe(RecipeStorage(name = "First", createdAt = 1234L), ingredients)
        val (second, secondIngredients) = store.saveRecipe(RecipeStorage(name = "Second"), ingredients)
        assertTrue(first.id > 0 && second.id > 0 && first.id != second.id)
        assertTrue(firstIngredients.single().id != secondIngredients.single().id)
        assertEquals(second.id, secondIngredients.single().recipeId)
        assertEquals(secondIngredients.single().id, database.dao().observeRecipeIngredients().first().single { it.recipeId == second.id }.id)
        store.saveRecipe(first.copy(name = "Edited", createdAt = 9999L), ingredients.map { it.copy(gramsUsed = 200.0) })
        val failed = runCatching {
            store.saveRecipe(first.copy(name = "Invalid"), ingredients.map { it.copy(foodItemId = 999999L) })
        }
        assertTrue(failed.isFailure)
        closeDatabase()
        database = openDatabase()
        assertEquals(setOf("Edited", "Second"), database.dao().observeRecipes().first().map { it.name }.toSet())
        assertEquals(1234L, database.dao().observeRecipes().first().single { it.id == first.id }.createdAt)
        val savedIngredients = database.dao().observeRecipeIngredients().first()
        assertEquals(2, savedIngredients.map { it.id }.toSet().size)
        assertEquals(100.0, savedIngredients.single { it.recipeId == second.id }.gramsUsed, 0.0)
        assertEquals(200.0, savedIngredients.single { it.recipeId == first.id }.gramsUsed, 0.0)
    }

    @Test
    fun recipeSaveReturnsCurrentIngredientFoodsWithoutWaitingForObservation() = runTest {
        val store = runtimeStore()
        val food = store.saveFood(FoodItemStorage(name = "Oats", caloriesPer100g = 123.0))
        val saved = store.saveRecipe(RecipeStorage(name = "Bowl"),
            listOf(RecipeIngredientStorage(foodItemId = food.id, gramsUsed = 50.0)))
        assertEquals(listOf(food), saved.foods)
        assertEquals(food.id, saved.ingredients.single().foodItemId)
        val updated = store.saveFood(food.copy(caloriesPer100g = 234.0))
        val edited = store.saveRecipe(saved.recipe, saved.ingredients)
        assertEquals(listOf(updated), edited.foods)
    }

    @Test
    fun conflictingBarcodeEditPreservesBothProductsAndNewScanStillMatches() = runTest {
        val store = runtimeStore()
        val first = store.saveFood(FoodItemStorage(name = "First", barcode = "111"))
        val second = store.saveFood(FoodItemStorage(name = "Second", barcode = "222"))
        assertTrue(runCatching { store.saveFood(first.copy(barcode = "222", name = "Conflict")) }.isFailure)
        val rescanned = store.saveFood(FoodItemStorage(name = "Scanned", barcode = "222"))
        assertEquals(second.id, rescanned.id)
        closeDatabase()
        database = openDatabase()
        assertEquals("First", database.dao().getFoodItem(first.id)?.name)
        assertEquals("111", database.dao().getFoodItem(first.id)?.barcode)
        assertEquals("Scanned", database.dao().getFoodItem(second.id)?.name)
    }

    @Test
    fun targetedSequentialAiFoodSavesAllocateDistinctIdsWithoutFlowWait() = runTest {
        val runtimeStore = runtimeStore()

        val firstSaved = runtimeStore.saveFood(
            FoodItemStorage(
                name = "AI havermout",
                caloriesPer100g = 370.0,
                sourceType = FoodSourceType.AI,
                createdAt = 1_000L,
                updatedAt = 1_000L,
            ),
        )
        val secondSaved = runtimeStore.saveFood(
            FoodItemStorage(
                name = "AI banaan",
                caloriesPer100g = 89.0,
                sourceType = FoodSourceType.AI,
                createdAt = 2_000L,
                updatedAt = 2_000L,
            ),
        )

        val savedFoods = database.dao().observeFoodItems().first()
        assertTrue(firstSaved.id > 0L)
        assertTrue(secondSaved.id > 0L)
        assertTrue(firstSaved.id != secondSaved.id)
        assertEquals(2, savedFoods.size)
        assertEquals(setOf(firstSaved.id, secondSaved.id), savedFoods.map { it.id }.toSet())
    }

    @Test
    fun targetedImmediateExportReadsNewRoomRecordWithoutFlowWait() = runTest {
        val runtimeStore = runtimeStore()
        val persisted = runtimeStore.saveFood(
            FoodItemStorage(
                name = "Direct Room exportproduct",
                caloriesPer100g = 123.0,
                sourceType = FoodSourceType.AI,
                createdAt = 3_000L,
                updatedAt = 3_000L,
            ),
        )

        val json = JsonParser.parseString(ExportAppDataUseCase(runtimeStore).invoke()).asJsonObject
        val exportedFood = json.getAsJsonObject("data")
            .getAsJsonArray("foods")
            .map { it.asJsonObject }
            .single { it.get("id").asLong == persisted.id }

        assertEquals("Direct Room exportproduct", exportedFood.get("name").asString)
        assertEquals(123.0, exportedFood.get("caloriesPer100g").asDouble, 0.0)
    }

    @Test
    fun targetedRoutineNutritionAndSessionWritesSurviveDatabaseReopen() = runTest {
        val dao = database.dao()

        dao.insertGeneratedRoutineGraph(
            routine = WorkoutRoutineEntity(id = 1L, name = "QA routine", description = "Persisted", active = true),
            days = listOf(WorkoutDayEntity(id = 10L, routineId = 1L, name = "Dag 1", orderIndex = 0)),
            exercises = listOf(ExerciseEntity(id = 20L, name = "Squat", muscleGroup = "Benen", equipment = "Barbell")),
            workoutExercises = listOf(
                WorkoutExerciseEntity(
                    id = 30L,
                    dayId = 10L,
                    exerciseId = 20L,
                    targetSets = 1,
                    repRange = "5",
                    restSeconds = 120,
                ),
            ),
            sets = listOf(RoutineSetEntity(id = 40L, workoutExerciseId = 30L, orderIndex = 0, targetReps = 5)),
        )
        dao.importWorkoutSessions(listOf(WorkoutSessionEntity(id = 50L, date = 1_000L, duration = 600L, status = "COMPLETED", completed = true)))
        dao.insertPerformedExercises(listOf(PerformedExerciseEntity(id = 60L, sessionId = 50L, exerciseId = 20L, sourceWorkoutExerciseId = 30L)))
        dao.insertWorkoutSets(listOf(WorkoutSetEntity(id = 70L, sessionId = 50L, exerciseId = 20L, performedExerciseId = 60L, weight = 100.0, reps = 5, rpe = 8.0)))
        dao.saveRecipe(
            recipe = RecipeEntity(id = 80L, name = "Kwark bowl", createdAt = 1L, updatedAt = 2L),
            ingredients = emptyList(),
        )
        dao.insertFoodItems(
            listOf(
                FoodItemEntity(
                    id = 90L,
                    name = "Kwark",
                    caloriesPer100g = 60.0,
                    proteinPer100g = 10.0,
                    carbsPer100g = 4.0,
                    fatPer100g = 0.2,
                    sourceType = "MANUAL",
                    createdAt = 1L,
                    updatedAt = 2L,
                ),
            ),
        )
        dao.saveRecipe(
            recipe = RecipeEntity(id = 80L, name = "Kwark bowl", createdAt = 1L, updatedAt = 3L),
            ingredients = listOf(RecipeIngredientEntity(id = 100L, recipeId = 80L, foodItemId = 90L, gramsUsed = 250.0)),
        )
        dao.deleteWorkoutSessionCascade(50L)

        closeDatabase()
        database = openDatabase()
        val reopened = database.dao()

        assertEquals(listOf(1L), reopened.observeRoutines().first().map { it.id })
        assertEquals(listOf(10L), reopened.observeWorkoutDays().first().map { it.id })
        assertEquals(listOf(30L), reopened.observeWorkoutExercises().first().map { it.id })
        assertEquals(listOf(40L), reopened.observeRoutineSets().first().map { it.id })
        assertTrue(reopened.observeWorkoutSessions().first().none { it.id == 50L })
        assertTrue(reopened.observePerformedExercises().first().none { it.sessionId == 50L })
        assertTrue(reopened.observeWorkoutSets().first().none { it.sessionId == 50L })
        assertEquals(listOf(90L), reopened.observeFoodItems().first().map { it.id })
        assertEquals(listOf(80L), reopened.observeRecipes().first().map { it.id })
        assertEquals(listOf(100L), reopened.observeRecipeIngredients().first().map { it.id })
    }

    @Test
    fun targetedWorkoutDebriefRefreshSurvivesDatabaseReopen() = runTest {
        val dao = database.dao()

        dao.importWorkoutSessions(
            listOf(
                WorkoutSessionEntity(
                    id = 51L,
                    date = 2_000L,
                    duration = 1_800L,
                    status = "COMPLETED",
                    completed = true,
                    debriefSummary = "Lokale samenvatting",
                    debriefSource = "LOCAL_FALLBACK",
                ),
                WorkoutSessionEntity(
                    id = 52L,
                    date = 2_100L,
                    duration = 0L,
                    status = "DRAFT",
                    completed = false,
                    debriefSummary = "Niet bijwerken",
                    debriefSource = "LOCAL_FALLBACK",
                ),
            ),
        )

        val updatedRows = dao.updateWorkoutSessionDebrief(
            sessionId = 51L,
            summary = "AI samenvatting",
            progressionFeedback = "Meer volume dan vorige sessie.",
            recommendation = "Behoud belasting.",
            nextSessionFocus = "Techniek",
            recoveryScore = 82,
            intensitySignal = "MAINTAIN",
            wins = "Rust stabiel\nTopset gehaald",
            risks = "Geen",
            nextLoadTarget = "90 kg x 5",
            recoveryAdvice = "Slaap bewaken",
            source = "GEMINI",
        )
        val ignoredRows = dao.updateWorkoutSessionDebrief(
            sessionId = 52L,
            summary = "Mag niet landen",
            progressionFeedback = "Draft",
            recommendation = "Draft",
            nextSessionFocus = "Draft",
            recoveryScore = 10,
            intensitySignal = "DELOAD",
            wins = "Draft",
            risks = "Draft",
            nextLoadTarget = "Draft",
            recoveryAdvice = "Draft",
            source = "GEMINI",
        )

        closeDatabase()
        database = openDatabase()
        val reopened = database.dao()
        val completed = reopened.observeWorkoutSessions().first().single { it.id == 51L }
        val draft = reopened.observeWorkoutSessions().first().single { it.id == 52L }

        assertEquals(1, updatedRows)
        assertEquals(0, ignoredRows)
        assertEquals("AI samenvatting", completed.debriefSummary)
        assertEquals("Meer volume dan vorige sessie.", completed.debriefProgressionFeedback)
        assertEquals("Behoud belasting.", completed.debriefRecommendation)
        assertEquals("Techniek", completed.debriefNextSessionFocus)
        assertEquals(82, completed.debriefRecoveryScore)
        assertEquals("GEMINI", completed.debriefSource)
        assertEquals("Niet bijwerken", draft.debriefSummary)
        assertEquals("LOCAL_FALLBACK", draft.debriefSource)
    }

    @Test
    fun targetedGeneratedRoutineGraphSurvivesDatabaseReopen() = runTest {
        val dao = database.dao()

        dao.insertExercises(listOf(ExerciseEntity(id = 20L, name = "Bench", muscleGroup = "Borst", equipment = "Barbell")))
        dao.insertGeneratedRoutineGraph(
            routine = WorkoutRoutineEntity(id = 2L, name = "Generated split", description = "AI draft", active = false),
            days = listOf(
                WorkoutDayEntity(id = 21L, routineId = 2L, name = "Upper", orderIndex = 0),
                WorkoutDayEntity(id = 22L, routineId = 2L, name = "Lower", orderIndex = 1),
            ),
            exercises = listOf(
                ExerciseEntity(id = 20L, name = "Bench", muscleGroup = "Borst", equipment = "Barbell"),
                ExerciseEntity(id = 23L, name = "Squat", muscleGroup = "Benen", equipment = "Barbell"),
            ),
            workoutExercises = listOf(
                WorkoutExerciseEntity(id = 31L, dayId = 21L, exerciseId = 20L, targetSets = 3, repRange = "6-8", restSeconds = 120, targetWeightKg = 85.0, targetRpe = 8.0, orderIndex = 0),
                WorkoutExerciseEntity(id = 32L, dayId = 22L, exerciseId = 23L, targetSets = 4, repRange = "5", restSeconds = 150, targetWeightKg = 120.0, targetRpe = 8.5, orderIndex = 0),
            ),
            sets = listOf(
                RoutineSetEntity(id = 41L, workoutExerciseId = 31L, orderIndex = 0, setType = "WORKING", targetReps = 6, targetWeightKg = 85.0, restSeconds = 120, targetRpe = 8.0),
                RoutineSetEntity(id = 42L, workoutExerciseId = 31L, orderIndex = 1, setType = "WORKING", targetReps = 6, targetWeightKg = 85.0, restSeconds = 120, targetRpe = 8.0),
                RoutineSetEntity(id = 43L, workoutExerciseId = 32L, orderIndex = 0, setType = "WORKING", targetReps = 5, targetWeightKg = 120.0, restSeconds = 150, targetRpe = 8.5),
            ),
        )

        closeDatabase()
        database = openDatabase()
        val reopened = database.dao()
        val routine = reopened.observeRoutines().first().single { it.id == 2L }
        val days = reopened.observeWorkoutDays().first().filter { it.routineId == 2L }
        val workoutExercises = reopened.observeWorkoutExercises().first().filter { it.dayId in days.map { day -> day.id } }
        val sets = reopened.observeRoutineSets().first().filter { it.workoutExerciseId in workoutExercises.map { exercise -> exercise.id } }
        val exercises = reopened.observeExercises().first().associateBy { it.id }

        assertEquals("Generated split", routine.name)
        assertEquals(false, routine.active)
        assertEquals(listOf(21L, 22L), days.sortedBy { it.orderIndex }.map { it.id })
        assertEquals(listOf(31L, 32L), workoutExercises.sortedWith(compareBy<WorkoutExerciseEntity> { it.dayId }.thenBy { it.orderIndex }).map { it.id })
        assertEquals(listOf(41L, 42L, 43L), sets.sortedWith(compareBy<RoutineSetEntity> { it.workoutExerciseId }.thenBy { it.orderIndex }).map { it.id })
        assertEquals("Bench", exercises[20L]?.name)
        assertEquals("Squat", exercises[23L]?.name)
    }

    @Test
    fun targetedWorkoutDayAndExerciseMutationsSurviveDatabaseReopen() = runTest {
        val dao = database.dao()

        dao.insertGeneratedRoutineGraph(
            routine = WorkoutRoutineEntity(id = 1L, name = "Split", description = "Targeted", active = true),
            days = listOf(WorkoutDayEntity(id = 10L, routineId = 1L, name = "Dag 1", orderIndex = 0)),
            exercises = listOf(ExerciseEntity(id = 20L, name = "Bench", muscleGroup = "Borst", equipment = "Barbell")),
            workoutExercises = listOf(
                WorkoutExerciseEntity(
                    id = 30L,
                    dayId = 10L,
                    exerciseId = 20L,
                    targetSets = 3,
                    repRange = "6-8",
                    restSeconds = 120,
                ),
            ),
            sets = listOf(RoutineSetEntity(id = 40L, workoutExerciseId = 30L, orderIndex = 0, targetReps = 6)),
        )
        dao.addWorkoutExerciseToDay(
            day = null,
            exercise = ExerciseEntity(id = 21L, name = "Row", muscleGroup = "Rug", equipment = "Dumbbell"),
            workoutExercise = WorkoutExerciseEntity(
                id = 31L,
                dayId = 10L,
                exerciseId = 21L,
                targetSets = 2,
                repRange = "8-10",
                restSeconds = 90,
                orderIndex = 1,
            ),
            sets = listOf(RoutineSetEntity(id = 41L, workoutExerciseId = 31L, orderIndex = 0, targetReps = 8)),
        )
        dao.addWorkoutExerciseToDay(
            day = WorkoutDayEntity(id = 11L, routineId = 1L, name = "Dag 2", orderIndex = 1),
            exercise = ExerciseEntity(id = 22L, name = "Squat", muscleGroup = "Benen", equipment = "Barbell"),
            workoutExercise = WorkoutExerciseEntity(
                id = 32L,
                dayId = 11L,
                exerciseId = 22L,
                targetSets = 4,
                repRange = "5",
                restSeconds = 150,
            ),
            sets = listOf(RoutineSetEntity(id = 42L, workoutExerciseId = 32L, orderIndex = 0, targetReps = 5)),
        )
        dao.deleteWorkoutExerciseCascade(
            workoutExerciseId = 30L,
            activeSession = null,
            activeDrafts = emptyList(),
            activeCollapsedExercises = emptyList(),
            activeSets = emptyList(),
        )
        dao.deleteWorkoutDayCascade(10L)

        closeDatabase()
        database = openDatabase()
        val reopened = database.dao()

        assertEquals(listOf(11L), reopened.observeWorkoutDays().first().map { it.id })
        assertEquals(listOf(32L), reopened.observeWorkoutExercises().first().map { it.id })
        assertEquals(listOf(42L), reopened.observeRoutineSets().first().map { it.id })
        assertTrue(reopened.observeWorkoutExercises().first().none { it.dayId == 10L })
        assertTrue(reopened.observeRoutineSets().first().none { it.workoutExerciseId == 30L || it.workoutExerciseId == 31L })
    }

    @Test
    fun targetedRoutineCreateUpdateDeleteSurvivesDatabaseReopen() = runTest {
        val dao = database.dao()

        dao.insertRoutine(WorkoutRoutineEntity(id = 1L, name = "A", description = "Old", active = true))
        dao.insertRoutine(WorkoutRoutineEntity(id = 2L, name = "B", description = "Delete me", active = false))
        dao.insertWorkoutDays(listOf(WorkoutDayEntity(id = 10L, routineId = 2L, name = "Dag 1", orderIndex = 0)))
        dao.insertExercises(listOf(ExerciseEntity(id = 20L, name = "Bench", muscleGroup = "Borst", equipment = "Barbell")))
        dao.insertWorkoutExercises(listOf(WorkoutExerciseEntity(id = 30L, dayId = 10L, exerciseId = 20L, targetSets = 3, repRange = "6-8", restSeconds = 120, orderIndex = 0)))
        dao.insertRoutineSets(listOf(RoutineSetEntity(id = 40L, workoutExerciseId = 30L, orderIndex = 0, targetReps = 6)))
        dao.updateRoutine(routineId = 1L, name = "A updated", description = "Persisted")
        dao.deleteRoutineAndNormalizeActive(2L)

        closeDatabase()
        database = openDatabase()
        val reopened = database.dao()
        val routines = reopened.observeRoutines().first()

        assertEquals(listOf(1L), routines.map { it.id })
        assertEquals("A updated", routines.single().name)
        assertEquals("Persisted", routines.single().description)
        assertEquals(true, routines.single().active)
        assertTrue(reopened.observeWorkoutDays().first().none { it.routineId == 2L })
        assertTrue(reopened.observeWorkoutExercises().first().none { it.dayId == 10L })
        assertTrue(reopened.observeRoutineSets().first().none { it.workoutExerciseId == 30L })
    }

    @Test
    fun targetedRoutineCascadeDeleteSurvivesDatabaseReopen() = runTest {
        val dao = database.dao()

        dao.insertRoutines(
            listOf(
                WorkoutRoutineEntity(id = 1L, name = "Keep", description = "Targeted", active = true),
                WorkoutRoutineEntity(id = 2L, name = "Delete", description = "Targeted", active = false),
            ),
        )
        dao.insertWorkoutDays(
            listOf(
                WorkoutDayEntity(id = 10L, routineId = 2L, name = "Dag 1", orderIndex = 0),
                WorkoutDayEntity(id = 11L, routineId = 2L, name = "Dag 2", orderIndex = 1),
            ),
        )
        dao.insertExercises(
            listOf(
                ExerciseEntity(id = 20L, name = "Bench", muscleGroup = "Borst", equipment = "Barbell"),
                ExerciseEntity(id = 21L, name = "Row", muscleGroup = "Rug", equipment = "Cable"),
            ),
        )
        dao.insertWorkoutExercises(
            listOf(
                WorkoutExerciseEntity(id = 30L, dayId = 10L, exerciseId = 20L, targetSets = 3, repRange = "6-8", restSeconds = 120, orderIndex = 0),
                WorkoutExerciseEntity(id = 31L, dayId = 11L, exerciseId = 21L, targetSets = 4, repRange = "8-10", restSeconds = 90, orderIndex = 0),
            ),
        )
        dao.insertRoutineSets(
            listOf(
                RoutineSetEntity(id = 40L, workoutExerciseId = 30L, orderIndex = 0, targetReps = 6),
                RoutineSetEntity(id = 41L, workoutExerciseId = 31L, orderIndex = 0, targetReps = 8),
            ),
        )
        dao.deleteRoutineCascade(2L)

        closeDatabase()
        database = openDatabase()
        val reopened = database.dao()

        assertEquals(listOf(1L), reopened.observeRoutines().first().map { it.id })
        assertTrue(reopened.observeWorkoutDays().first().none { it.routineId == 2L })
        assertTrue(reopened.observeWorkoutExercises().first().none { it.id == 30L || it.id == 31L })
        assertTrue(reopened.observeRoutineSets().first().none { it.id == 40L || it.id == 41L })
        assertEquals(listOf(20L, 21L), reopened.observeExercises().first().map { it.id })
    }

    @Test
    fun targetedNutritionDeletesSurviveDatabaseReopen() = runTest {
        val dao = database.dao()

        dao.insertFoodItems(
            listOf(
                FoodItemEntity(
                    id = 90L,
                    name = "Kwark",
                    caloriesPer100g = 60.0,
                    proteinPer100g = 10.0,
                    carbsPer100g = 4.0,
                    fatPer100g = 0.2,
                    sourceType = "MANUAL",
                    createdAt = 1L,
                    updatedAt = 2L,
                ),
                FoodItemEntity(
                    id = 91L,
                    name = "Havermout",
                    caloriesPer100g = 370.0,
                    proteinPer100g = 13.0,
                    carbsPer100g = 60.0,
                    fatPer100g = 7.0,
                    sourceType = "MANUAL",
                    createdAt = 1L,
                    updatedAt = 2L,
                ),
            ),
        )
        dao.saveRecipe(
            recipe = RecipeEntity(id = 80L, name = "Kwark bowl", createdAt = 1L, updatedAt = 2L),
            ingredients = listOf(RecipeIngredientEntity(id = 100L, recipeId = 80L, foodItemId = 90L, gramsUsed = 250.0)),
        )
        dao.saveRecipe(
            recipe = RecipeEntity(id = 81L, name = "Havermout bowl", createdAt = 1L, updatedAt = 2L),
            ingredients = listOf(RecipeIngredientEntity(id = 101L, recipeId = 81L, foodItemId = 91L, gramsUsed = 80.0)),
        )
        dao.deleteRecipeWithIngredients(80L)
        dao.deleteFoodItem(90L)

        closeDatabase()
        database = openDatabase()
        val reopened = database.dao()

        assertEquals(listOf(91L), reopened.observeFoodItems().first().map { it.id })
        assertEquals(listOf(81L), reopened.observeRecipes().first().map { it.id })
        assertEquals(listOf(101L), reopened.observeRecipeIngredients().first().map { it.id })
        assertTrue(reopened.observeRecipeIngredients().first().none { it.recipeId == 80L || it.foodItemId == 90L })
    }

    @Test
    fun targetedProfileAndMeasurementMutationsSurviveDatabaseReopen() = runTest {
        val dao = database.dao()

        dao.upsertUserProfile(
            UserProfileEntity(
                id = 1L,
                name = "QA",
                age = 35,
                sex = "MALE",
                height = 180.0,
                weight = 82.5,
                bodyFat = 15.0,
                activityLevel = "MODERATE",
                goal = "STRENGTH",
                calorieTarget = 2_700,
                proteinTarget = 180,
                carbsTarget = 280,
                fatTarget = 80,
                trainingFocus = "POWERLIFTING",
            ),
        )
        dao.insertMeasurement(BodyMeasurementEntity(id = 10L, date = 1_000L, weight = 82.5, bodyFat = 15.0, muscleMass = 38.0))
        dao.insertMeasurement(BodyMeasurementEntity(id = 11L, date = 2_000L, weight = 83.0, bodyFat = 14.8, muscleMass = 38.4))
        dao.deleteMeasurement(10L)

        closeDatabase()
        database = openDatabase()
        val reopened = database.dao()

        assertEquals("QA", reopened.observeUserProfile().first()?.name)
        assertEquals(listOf(11L), reopened.observeMeasurements().first().map { it.id })
        assertEquals(listOf(83.0), reopened.observeMeasurements().first().map { it.weight })
        assertTrue(reopened.observeMeasurements().first().none { it.id == 10L })
    }

    @Test
    fun targetedProfileResetSurvivesDatabaseReopen() = runTest {
        val dao = database.dao()

        dao.upsertUserProfile(
            UserProfileEntity(
                id = 1L,
                name = "Reset me",
                age = 35,
                sex = "MALE",
                height = 180.0,
                weight = 82.5,
                bodyFat = 15.0,
                activityLevel = "MODERATE",
                goal = "STRENGTH",
                calorieTarget = 2_700,
                proteinTarget = 180,
                carbsTarget = 280,
                fatTarget = 80,
                trainingFocus = "POWERLIFTING",
            ),
        )
        dao.insertMeasurement(BodyMeasurementEntity(id = 12L, date = 3_000L, weight = 84.0, bodyFat = 14.5, muscleMass = 38.8))
        dao.clearMirrorUserProfile()

        closeDatabase()
        database = openDatabase()
        val reopened = database.dao()

        assertEquals(null, reopened.observeUserProfile().first())
        assertEquals(listOf(12L), reopened.observeMeasurements().first().map { it.id })
    }

    @Test
    fun targetedMealMutationsSurviveDatabaseReopen() = runTest {
        val dao = database.dao()

        dao.saveMeal(
            meal = MealEntity(
                id = 120L,
                date = 1_000L,
                mealType = "BREAKFAST",
                name = "Ontbijt",
                calories = 450,
                protein = 35,
                carbs = 45,
                fat = 12,
            ),
            items = listOf(
                MealItemEntity(
                    id = 121L,
                    mealId = 120L,
                    itemType = "FOOD",
                    referenceId = 90L,
                    name = "Kwark",
                    gramsUsed = 250.0,
                    calories = 150.0,
                    protein = 25.0,
                    carbs = 10.0,
                    fat = 1.0,
                ),
            ),
        )
        dao.saveMeal(
            meal = MealEntity(
                id = 122L,
                date = 2_000L,
                mealType = "LUNCH",
                name = "Lunch",
                calories = 650,
                protein = 45,
                carbs = 70,
                fat = 18,
            ),
            items = listOf(
                MealItemEntity(
                    id = 123L,
                    mealId = 122L,
                    itemType = "FOOD",
                    referenceId = 91L,
                    name = "Rijst",
                    gramsUsed = 300.0,
                    calories = 390.0,
                    protein = 8.0,
                    carbs = 84.0,
                    fat = 1.0,
                ),
            ),
        )
        dao.deleteMealWithItems(120L)

        closeDatabase()
        database = openDatabase()
        val reopened = database.dao()

        assertEquals(listOf(122L), reopened.observeMeals().first().map { it.id })
        assertEquals(listOf(123L), reopened.observeMealItems().first().map { it.id })
        assertTrue(reopened.observeMealItems().first().none { it.mealId == 120L })
    }

    @Test
    fun targetedMealItemSnapshotsDoNotChangeAfterProductAndRecipeEdits() = runTest {
        val dao = database.dao()

        dao.insertFoodItems(
            listOf(
                FoodItemEntity(
                    id = 90L,
                    name = "Kip rollade",
                    caloriesPer100g = 140.0,
                    proteinPer100g = 22.0,
                    carbsPer100g = 1.0,
                    fatPer100g = 5.0,
                    sourceType = "MANUAL",
                    createdAt = 1L,
                    updatedAt = 1L,
                ),
                FoodItemEntity(
                    id = 91L,
                    name = "Kaas",
                    caloriesPer100g = 350.0,
                    proteinPer100g = 25.0,
                    carbsPer100g = 2.0,
                    fatPer100g = 29.0,
                    sourceType = "MANUAL",
                    createdAt = 1L,
                    updatedAt = 1L,
                ),
            ),
        )
        dao.saveRecipe(
            recipe = RecipeEntity(id = 80L, name = "Wrap kip kaas", createdAt = 1L, updatedAt = 1L),
            ingredients = listOf(
                RecipeIngredientEntity(id = 100L, recipeId = 80L, foodItemId = 90L, gramsUsed = 80.0, orderIndex = 0),
                RecipeIngredientEntity(id = 101L, recipeId = 80L, foodItemId = 91L, gramsUsed = 30.0, orderIndex = 1),
            ),
        )
        dao.saveMeal(
            meal = MealEntity(
                id = 120L,
                date = 1_000L,
                mealType = "LUNCH",
                name = "Lunch snapshot",
                calories = 217,
                protein = 25,
                carbs = 2,
                fat = 13,
            ),
            items = listOf(
                MealItemEntity(
                    id = 121L,
                    mealId = 120L,
                    itemType = "FOOD",
                    referenceId = 90L,
                    name = "Kip rollade",
                    gramsUsed = 80.0,
                    calories = 112.0,
                    protein = 17.6,
                    carbs = 0.8,
                    fat = 4.0,
                    orderIndex = 0,
                ),
                MealItemEntity(
                    id = 122L,
                    mealId = 120L,
                    itemType = "FOOD",
                    referenceId = 91L,
                    name = "Kaas",
                    gramsUsed = 30.0,
                    calories = 105.0,
                    protein = 7.5,
                    carbs = 0.6,
                    fat = 8.7,
                    orderIndex = 1,
                ),
                MealItemEntity(
                    id = 123L,
                    mealId = 120L,
                    itemType = "RECIPE",
                    referenceId = 80L,
                    name = "Wrap kip kaas",
                    gramsUsed = 110.0,
                    calories = 217.0,
                    protein = 25.1,
                    carbs = 1.4,
                    fat = 12.7,
                    orderIndex = 2,
                ),
            ),
        )

        dao.insertFoodItems(
            listOf(
                FoodItemEntity(
                    id = 90L,
                    name = "Kip rollade aangepast",
                    caloriesPer100g = 999.0,
                    proteinPer100g = 1.0,
                    carbsPer100g = 99.0,
                    fatPer100g = 99.0,
                    sourceType = "MANUAL",
                    createdAt = 1L,
                    updatedAt = 2L,
                ),
                FoodItemEntity(
                    id = 91L,
                    name = "Kaas aangepast",
                    caloriesPer100g = 111.0,
                    proteinPer100g = 2.0,
                    carbsPer100g = 3.0,
                    fatPer100g = 4.0,
                    sourceType = "MANUAL",
                    createdAt = 1L,
                    updatedAt = 2L,
                ),
            ),
        )
        dao.saveRecipe(
            recipe = RecipeEntity(id = 80L, name = "Wrap aangepast", createdAt = 1L, updatedAt = 2L),
            ingredients = listOf(RecipeIngredientEntity(id = 102L, recipeId = 80L, foodItemId = 91L, gramsUsed = 200.0)),
        )

        closeDatabase()
        database = openDatabase()
        val reopened = database.dao()
        val snapshots = reopened.observeMealItems().first().sortedBy { it.orderIndex }

        assertEquals(listOf("Kip rollade", "Kaas", "Wrap kip kaas"), snapshots.map { it.name })
        assertEquals(listOf(112.0, 105.0, 217.0), snapshots.map { it.calories })
        assertEquals(listOf(17.6, 7.5, 25.1), snapshots.map { it.protein })
        assertEquals(listOf(90L, 91L, 80L), snapshots.map { it.referenceId })
        assertEquals("Kip rollade aangepast", reopened.observeFoodItems().first().single { it.id == 90L }.name)
        assertEquals("Wrap aangepast", reopened.observeRecipes().first().single { it.id == 80L }.name)
    }

    @Test
    fun targetedActiveWorkoutStartSurvivesDatabaseReopen() = runTest {
        val dao = database.dao()

        dao.insertRoutines(listOf(WorkoutRoutineEntity(id = 1L, name = "Start routine", description = "Targeted", active = true)))
        dao.insertWorkoutDays(listOf(WorkoutDayEntity(id = 10L, routineId = 1L, name = "Dag 1", orderIndex = 0)))
        dao.insertExercises(
            listOf(
                ExerciseEntity(id = 20L, name = "Bench", muscleGroup = "Borst", equipment = "Barbell"),
                ExerciseEntity(id = 21L, name = "Row", muscleGroup = "Rug", equipment = "Dumbbell"),
            ),
        )
        dao.insertWorkoutExercises(
            listOf(
                WorkoutExerciseEntity(id = 30L, dayId = 10L, exerciseId = 20L, targetSets = 3, repRange = "6-8", restSeconds = 120, orderIndex = 0),
                WorkoutExerciseEntity(id = 31L, dayId = 10L, exerciseId = 21L, targetSets = 3, repRange = "8-10", restSeconds = 90, orderIndex = 1),
            ),
        )
        dao.startOrResumeActiveWorkoutSession(
            activeSession = ActiveWorkoutSessionEntity(sessionId = 50L, dayId = 10L, routineId = 1L, startedAt = 1_000L, updatedAt = 1_000L),
            draftSession = WorkoutSessionEntity(
                id = 50L,
                date = 1_000L,
                duration = 0L,
                status = "DRAFT",
                completed = false,
                routineId = 1L,
                workoutDayId = 10L,
                startedAt = 1_000L,
            ),
            drafts = listOf(
                ActiveWorkoutDraftEntity(sessionId = 50L, exerciseId = 20L, weight = "80", reps = "8", rpe = "8", setType = "NORMAL"),
            ),
            performedExercises = listOf(
                PerformedExerciseEntity(id = 60L, sessionId = 50L, exerciseId = 20L, sourceWorkoutExerciseId = 30L, orderIndex = 0),
                PerformedExerciseEntity(id = 61L, sessionId = 50L, exerciseId = 21L, sourceWorkoutExerciseId = 31L, orderIndex = 1),
            ),
        )

        closeDatabase()
        database = openDatabase()
        val reopened = database.dao()

        assertEquals(listOf(50L), reopened.observeActiveWorkoutSessions().first().map { it.sessionId })
        assertEquals(listOf(50L), reopened.observeWorkoutSessions().first().filter { !it.completed && it.status == "DRAFT" }.map { it.id })
        assertEquals(listOf(20L), reopened.observeActiveWorkoutDrafts().first().map { it.exerciseId })
        assertEquals(listOf(60L, 61L), reopened.observePerformedExercises().first().filter { it.sessionId == 50L }.map { it.id })
    }

    @Test
    fun targetedActiveWorkoutRuntimeMutationsSurviveDatabaseReopen() = runTest {
        val dao = database.dao()

        dao.insertRoutines(listOf(WorkoutRoutineEntity(id = 1L, name = "Runtime routine", description = "Targeted", active = true)))
        dao.insertWorkoutDays(listOf(WorkoutDayEntity(id = 10L, routineId = 1L, name = "Dag 1", orderIndex = 0)))
        dao.insertExercises(listOf(ExerciseEntity(id = 20L, name = "Bench", muscleGroup = "Borst", equipment = "Barbell")))
        dao.insertWorkoutExercises(listOf(WorkoutExerciseEntity(id = 30L, dayId = 10L, exerciseId = 20L, targetSets = 3, repRange = "6-8", restSeconds = 120, orderIndex = 0)))
        dao.startOrResumeActiveWorkoutSession(
            activeSession = ActiveWorkoutSessionEntity(sessionId = 50L, dayId = 10L, routineId = 1L, startedAt = 1_000L, updatedAt = 1_000L),
            draftSession = WorkoutSessionEntity(id = 50L, date = 1_000L, duration = 0L, status = "DRAFT", completed = false, routineId = 1L, workoutDayId = 10L, startedAt = 1_000L),
            drafts = listOf(ActiveWorkoutDraftEntity(sessionId = 50L, exerciseId = 20L, weight = "80", reps = "8", rpe = "8", setType = "NORMAL")),
            performedExercises = listOf(PerformedExerciseEntity(id = 60L, sessionId = 50L, exerciseId = 20L, sourceWorkoutExerciseId = 30L, orderIndex = 0)),
        )
        dao.updateActiveWorkoutRestTimer(sessionId = 50L, endsAt = 3_000L, totalSeconds = 120, updatedAt = 1_100L)
        dao.updateActiveWorkoutDraft(
            sessionId = 50L,
            draft = ActiveWorkoutDraftEntity(sessionId = 50L, exerciseId = 20L, weight = "82.5", reps = "7", rpe = "8.5", setType = "WORKING"),
            updatedAt = 1_200L,
        )
        dao.setActiveWorkoutCollapsedExercise(
            collapsedExercise = ActiveWorkoutCollapsedExerciseEntity(sessionId = 50L, exerciseId = 20L),
            collapsed = true,
            updatedAt = 1_300L,
        )
        dao.logActiveWorkoutSet(
            session = ActiveWorkoutSessionEntity(sessionId = 50L, dayId = 10L, routineId = 1L, startedAt = 1_000L, updatedAt = 1_400L, restTimerEndsAt = 3_000L, restTimerTotalSeconds = 120),
            draft = ActiveWorkoutDraftEntity(sessionId = 50L, exerciseId = 20L, weight = "85", reps = "6", rpe = "9", setType = "WORKING"),
            set = ActiveWorkoutSetEntity(sessionId = 50L, id = 70L, exerciseId = 20L, performedExerciseId = 60L, sourceWorkoutExerciseId = 30L, weight = 85.0, reps = 6, rpe = 9.0, setType = "WORKING", restSeconds = 120, orderIndex = 0, completed = true, loggedAt = 1_400L),
            event = WorkoutLogEventEntity(id = 80L, dayId = 10L, sessionId = 50L, type = "ADD_SET", syncStatus = "PENDING", createdAt = 1_400L, undoExpiresAt = 2_400L),
            eventSets = listOf(
                WorkoutLogEventSetEntity(eventId = 80L, snapshotRole = "CURRENT", snapshotIndex = 0, id = 70L, exerciseId = 20L, performedExerciseId = 60L, sourceWorkoutExerciseId = 30L, weight = 85.0, reps = 6, rpe = 9.0, setType = "WORKING", restSeconds = 120, orderIndex = 0, completed = true, loggedAt = 1_400L),
            ),
            activeKey = 20L,
        )

        closeDatabase()
        database = openDatabase()
        val reopened = database.dao()
        val active = reopened.observeActiveWorkoutSessions().first().single()
        val draft = reopened.observeActiveWorkoutDrafts().first().single()
        val activeSet = reopened.observeActiveWorkoutSets().first().single()
        val event = reopened.observeWorkoutLogEvents().first().single()
        val eventSet = reopened.observeWorkoutLogEventSets().first().single()

        assertEquals(1_400L, active.updatedAt)
        assertEquals(3_000L, active.restTimerEndsAt)
        assertEquals(120, active.restTimerTotalSeconds)
        assertEquals("85", draft.weight)
        assertEquals("6", draft.reps)
        assertEquals("WORKING", draft.setType)
        assertEquals(70L, activeSet.id)
        assertEquals(85.0, activeSet.weight, 0.0)
        assertEquals(6, activeSet.reps)
        assertEquals("ADD_SET", event.type)
        assertEquals("PENDING", event.syncStatus)
        assertEquals(70L, eventSet.id)
        assertTrue(reopened.observeActiveWorkoutCollapsedExercises().first().isEmpty())
    }

    @Test
    fun targetedActiveWorkoutCollapseExpandSurvivesDatabaseReopen() = runTest {
        val dao = database.dao()

        dao.insertRoutines(listOf(WorkoutRoutineEntity(id = 1L, name = "Collapse routine", description = "Targeted", active = true)))
        dao.insertWorkoutDays(listOf(WorkoutDayEntity(id = 10L, routineId = 1L, name = "Dag 1", orderIndex = 0)))
        dao.insertExercises(listOf(ExerciseEntity(id = 20L, name = "Bench", muscleGroup = "Borst", equipment = "Barbell")))
        dao.insertWorkoutExercises(listOf(WorkoutExerciseEntity(id = 30L, dayId = 10L, exerciseId = 20L, targetSets = 3, repRange = "6-8", restSeconds = 120, orderIndex = 0)))
        dao.startOrResumeActiveWorkoutSession(
            activeSession = ActiveWorkoutSessionEntity(sessionId = 50L, dayId = 10L, routineId = 1L, startedAt = 1_000L, updatedAt = 1_000L),
            draftSession = WorkoutSessionEntity(id = 50L, date = 1_000L, duration = 0L, status = "DRAFT", completed = false, routineId = 1L, workoutDayId = 10L, startedAt = 1_000L),
            drafts = listOf(ActiveWorkoutDraftEntity(sessionId = 50L, exerciseId = 20L, weight = "80", reps = "8", rpe = "8", setType = "NORMAL")),
            performedExercises = listOf(PerformedExerciseEntity(id = 60L, sessionId = 50L, exerciseId = 20L, sourceWorkoutExerciseId = 30L, orderIndex = 0)),
        )
        dao.setActiveWorkoutCollapsedExercise(
            collapsedExercise = ActiveWorkoutCollapsedExerciseEntity(sessionId = 50L, exerciseId = 20L),
            collapsed = true,
            updatedAt = 1_300L,
        )
        dao.setActiveWorkoutCollapsedExercise(
            collapsedExercise = ActiveWorkoutCollapsedExerciseEntity(sessionId = 50L, exerciseId = 20L),
            collapsed = false,
            updatedAt = 1_600L,
        )

        closeDatabase()
        database = openDatabase()
        val reopened = database.dao()

        assertEquals(1_600L, reopened.observeActiveWorkoutSessions().first().single().updatedAt)
        assertTrue(reopened.observeActiveWorkoutCollapsedExercises().first().isEmpty())
    }

    @Test
    fun targetedActiveWorkoutDiscardSurvivesDatabaseReopenAndClearsRuntimeRows() = runTest {
        val dao = database.dao()

        dao.insertRoutines(listOf(WorkoutRoutineEntity(id = 1L, name = "Discard routine", description = "Targeted", active = true)))
        dao.insertWorkoutDays(listOf(WorkoutDayEntity(id = 10L, routineId = 1L, name = "Dag 1", orderIndex = 0)))
        dao.insertExercises(listOf(ExerciseEntity(id = 20L, name = "Bench", muscleGroup = "Borst", equipment = "Barbell")))
        dao.insertWorkoutExercises(listOf(WorkoutExerciseEntity(id = 30L, dayId = 10L, exerciseId = 20L, targetSets = 3, repRange = "6-8", restSeconds = 120, orderIndex = 0)))
        dao.startOrResumeActiveWorkoutSession(
            activeSession = ActiveWorkoutSessionEntity(sessionId = 50L, dayId = 10L, routineId = 1L, startedAt = 1_000L, updatedAt = 1_000L),
            draftSession = WorkoutSessionEntity(id = 50L, date = 1_000L, duration = 0L, status = "DRAFT", completed = false, routineId = 1L, workoutDayId = 10L, startedAt = 1_000L),
            drafts = listOf(ActiveWorkoutDraftEntity(sessionId = 50L, exerciseId = 20L, weight = "80", reps = "8", rpe = "8", setType = "NORMAL")),
            performedExercises = listOf(PerformedExerciseEntity(id = 60L, sessionId = 50L, exerciseId = 20L, sourceWorkoutExerciseId = 30L, orderIndex = 0)),
        )
        dao.insertActiveWorkoutCollapsedExercises(listOf(ActiveWorkoutCollapsedExerciseEntity(sessionId = 50L, exerciseId = 20L)))
        dao.insertActiveWorkoutSets(
            listOf(
                ActiveWorkoutSetEntity(sessionId = 50L, id = 70L, exerciseId = 20L, performedExerciseId = 60L, sourceWorkoutExerciseId = 30L, weight = 85.0, reps = 6, rpe = 9.0, setType = "WORKING", restSeconds = 120, orderIndex = 0, completed = true, loggedAt = 1_400L),
            ),
        )
        dao.insertWorkoutLogEvents(listOf(WorkoutLogEventEntity(id = 80L, dayId = 10L, sessionId = 50L, type = "ADD_SET", syncStatus = "PENDING", createdAt = 1_400L)))
        dao.insertWorkoutLogEventSets(
            listOf(
                WorkoutLogEventSetEntity(eventId = 80L, snapshotRole = "CURRENT", snapshotIndex = 0, id = 70L, exerciseId = 20L, performedExerciseId = 60L, sourceWorkoutExerciseId = 30L, weight = 85.0, reps = 6, rpe = 9.0, setType = "WORKING", restSeconds = 120, orderIndex = 0, completed = true, loggedAt = 1_400L),
            ),
        )

        dao.discardActiveWorkoutSession(sessionId = 50L)

        closeDatabase()
        database = openDatabase()
        val reopened = database.dao()

        assertTrue(reopened.observeActiveWorkoutSessions().first().isEmpty())
        assertTrue(reopened.observeActiveWorkoutDrafts().first().isEmpty())
        assertTrue(reopened.observeActiveWorkoutCollapsedExercises().first().isEmpty())
        assertTrue(reopened.observeActiveWorkoutSets().first().isEmpty())
        assertTrue(reopened.observeWorkoutLogEvents().first().isEmpty())
        assertTrue(reopened.observeWorkoutLogEventSets().first().isEmpty())
        assertTrue(reopened.observePerformedExercises().first().none { it.sessionId == 50L })
        assertTrue(reopened.observeWorkoutSessions().first().none { it.id == 50L })
        assertEquals(listOf(1L), reopened.observeRoutines().first().map { it.id })
        assertEquals(listOf(10L), reopened.observeWorkoutDays().first().map { it.id })
        assertEquals(listOf(30L), reopened.observeWorkoutExercises().first().map { it.id })
    }

    @Test
    fun targetedActiveWorkoutSetDeleteSurvivesDatabaseReopen() = runTest {
        val dao = database.dao()

        dao.insertRoutines(listOf(WorkoutRoutineEntity(id = 1L, name = "Delete set routine", description = "Targeted", active = true)))
        dao.insertWorkoutDays(listOf(WorkoutDayEntity(id = 10L, routineId = 1L, name = "Dag 1", orderIndex = 0)))
        dao.insertExercises(listOf(ExerciseEntity(id = 20L, name = "Bench", muscleGroup = "Borst", equipment = "Barbell")))
        dao.insertWorkoutExercises(listOf(WorkoutExerciseEntity(id = 30L, dayId = 10L, exerciseId = 20L, targetSets = 3, repRange = "6-8", restSeconds = 120, orderIndex = 0)))
        dao.startOrResumeActiveWorkoutSession(
            activeSession = ActiveWorkoutSessionEntity(sessionId = 50L, dayId = 10L, routineId = 1L, startedAt = 1_000L, updatedAt = 1_000L),
            draftSession = WorkoutSessionEntity(id = 50L, date = 1_000L, duration = 0L, status = "DRAFT", completed = false, routineId = 1L, workoutDayId = 10L, startedAt = 1_000L),
            drafts = listOf(ActiveWorkoutDraftEntity(sessionId = 50L, exerciseId = 20L, weight = "80", reps = "8", rpe = "8", setType = "NORMAL")),
            performedExercises = listOf(PerformedExerciseEntity(id = 60L, sessionId = 50L, exerciseId = 20L, sourceWorkoutExerciseId = 30L, orderIndex = 0)),
        )
        dao.insertActiveWorkoutSets(
            listOf(
                ActiveWorkoutSetEntity(sessionId = 50L, id = 70L, exerciseId = 20L, performedExerciseId = 60L, sourceWorkoutExerciseId = 30L, weight = 85.0, reps = 6, rpe = 9.0, setType = "WORKING", restSeconds = 120, orderIndex = 0, completed = true, loggedAt = 1_400L),
                ActiveWorkoutSetEntity(sessionId = 50L, id = 71L, exerciseId = 20L, performedExerciseId = 60L, sourceWorkoutExerciseId = 30L, weight = 87.5, reps = 5, rpe = 9.5, setType = "WORKING", restSeconds = 120, orderIndex = 1, completed = true, loggedAt = 1_500L),
            ),
        )
        dao.insertWorkoutLogEvents(
            listOf(
                WorkoutLogEventEntity(id = 80L, dayId = 10L, sessionId = 50L, type = "ADD_SET", syncStatus = "PENDING", createdAt = 1_400L),
                WorkoutLogEventEntity(id = 81L, dayId = 10L, sessionId = 50L, type = "ADD_SET", syncStatus = "PENDING", createdAt = 1_500L),
            ),
        )
        dao.insertWorkoutLogEventSets(
            listOf(
                WorkoutLogEventSetEntity(eventId = 80L, snapshotRole = "CURRENT", snapshotIndex = 0, id = 70L, exerciseId = 20L, performedExerciseId = 60L, sourceWorkoutExerciseId = 30L, weight = 85.0, reps = 6, rpe = 9.0, setType = "WORKING", restSeconds = 120, orderIndex = 0, completed = true, loggedAt = 1_400L),
                WorkoutLogEventSetEntity(eventId = 81L, snapshotRole = "CURRENT", snapshotIndex = 0, id = 71L, exerciseId = 20L, performedExerciseId = 60L, sourceWorkoutExerciseId = 30L, weight = 87.5, reps = 5, rpe = 9.5, setType = "WORKING", restSeconds = 120, orderIndex = 1, completed = true, loggedAt = 1_500L),
            ),
        )

        dao.deleteActiveWorkoutSet(sessionId = 50L, setId = 70L, updatedAt = 1_600L)

        closeDatabase()
        database = openDatabase()
        val reopened = database.dao()

        assertEquals(1_600L, reopened.observeActiveWorkoutSessions().first().single().updatedAt)
        assertEquals(listOf(71L), reopened.observeActiveWorkoutSets().first().map { it.id })
        assertEquals(listOf(81L), reopened.observeWorkoutLogEvents().first().map { it.id })
        assertEquals(listOf(71L), reopened.observeWorkoutLogEventSets().first().map { it.id })
    }

    @Test
    fun targetedActiveWorkoutSetTypeEditSurvivesDatabaseReopen() = runTest {
        val dao = database.dao()

        dao.insertRoutines(listOf(WorkoutRoutineEntity(id = 1L, name = "Type edit routine", description = "Targeted", active = true)))
        dao.insertWorkoutDays(listOf(WorkoutDayEntity(id = 10L, routineId = 1L, name = "Dag 1", orderIndex = 0)))
        dao.insertExercises(listOf(ExerciseEntity(id = 20L, name = "Bench", muscleGroup = "Borst", equipment = "Barbell")))
        dao.insertWorkoutExercises(listOf(WorkoutExerciseEntity(id = 30L, dayId = 10L, exerciseId = 20L, targetSets = 3, repRange = "6-8", restSeconds = 120, orderIndex = 0)))
        dao.startOrResumeActiveWorkoutSession(
            activeSession = ActiveWorkoutSessionEntity(sessionId = 50L, dayId = 10L, routineId = 1L, startedAt = 1_000L, updatedAt = 1_000L),
            draftSession = WorkoutSessionEntity(id = 50L, date = 1_000L, duration = 0L, status = "DRAFT", completed = false, routineId = 1L, workoutDayId = 10L, startedAt = 1_000L),
            drafts = listOf(ActiveWorkoutDraftEntity(sessionId = 50L, exerciseId = 20L, weight = "80", reps = "8", rpe = "8", setType = "NORMAL")),
            performedExercises = listOf(PerformedExerciseEntity(id = 60L, sessionId = 50L, exerciseId = 20L, sourceWorkoutExerciseId = 30L, orderIndex = 0)),
        )
        dao.insertActiveWorkoutSets(
            listOf(
                ActiveWorkoutSetEntity(sessionId = 50L, id = 70L, exerciseId = 20L, performedExerciseId = 60L, sourceWorkoutExerciseId = 30L, weight = 85.0, reps = 6, rpe = 9.0, setType = "WORKING", restSeconds = 120, orderIndex = 0, completed = true, loggedAt = 1_400L),
            ),
        )
        dao.insertWorkoutLogEvents(listOf(WorkoutLogEventEntity(id = 80L, dayId = 10L, sessionId = 50L, type = "ADD_SET", syncStatus = "PENDING", createdAt = 1_400L)))
        dao.insertWorkoutLogEventSets(
            listOf(
                WorkoutLogEventSetEntity(eventId = 80L, snapshotRole = "CURRENT", snapshotIndex = 0, id = 70L, exerciseId = 20L, performedExerciseId = 60L, sourceWorkoutExerciseId = 30L, weight = 85.0, reps = 6, rpe = 9.0, setType = "WORKING", restSeconds = 120, orderIndex = 0, completed = true, loggedAt = 1_400L),
            ),
        )

        dao.updateActiveWorkoutSetType(sessionId = 50L, setId = 70L, setType = "WARMUP", updatedAt = 1_700L)

        closeDatabase()
        database = openDatabase()
        val reopened = database.dao()

        assertEquals(1_700L, reopened.observeActiveWorkoutSessions().first().single().updatedAt)
        assertEquals("WARMUP", reopened.observeActiveWorkoutSets().first().single().setType)
        assertEquals("WARMUP", reopened.observeWorkoutLogEventSets().first().single().setType)
    }

    @Test
    fun targetedActiveWorkoutSetValueEditSurvivesDatabaseReopen() = runTest {
        val dao = database.dao()

        dao.insertRoutines(listOf(WorkoutRoutineEntity(id = 1L, name = "Value edit routine", description = "Targeted", active = true)))
        dao.insertWorkoutDays(listOf(WorkoutDayEntity(id = 10L, routineId = 1L, name = "Dag 1", orderIndex = 0)))
        dao.insertExercises(listOf(ExerciseEntity(id = 20L, name = "Bench", muscleGroup = "Borst", equipment = "Barbell")))
        dao.insertWorkoutExercises(listOf(WorkoutExerciseEntity(id = 30L, dayId = 10L, exerciseId = 20L, targetSets = 3, repRange = "6-8", restSeconds = 120, orderIndex = 0)))
        dao.startOrResumeActiveWorkoutSession(
            activeSession = ActiveWorkoutSessionEntity(sessionId = 50L, dayId = 10L, routineId = 1L, startedAt = 1_000L, updatedAt = 1_000L),
            draftSession = WorkoutSessionEntity(id = 50L, date = 1_000L, duration = 0L, status = "DRAFT", completed = false, routineId = 1L, workoutDayId = 10L, startedAt = 1_000L),
            drafts = listOf(ActiveWorkoutDraftEntity(sessionId = 50L, exerciseId = 20L, weight = "80", reps = "8", rpe = "8", setType = "NORMAL")),
            performedExercises = listOf(PerformedExerciseEntity(id = 60L, sessionId = 50L, exerciseId = 20L, sourceWorkoutExerciseId = 30L, orderIndex = 0)),
        )
        dao.insertActiveWorkoutSets(
            listOf(
                ActiveWorkoutSetEntity(sessionId = 50L, id = 70L, exerciseId = 20L, performedExerciseId = 60L, sourceWorkoutExerciseId = 30L, weight = 85.0, reps = 6, rpe = 9.0, setType = "WORKING", restSeconds = 120, orderIndex = 0, completed = true, loggedAt = 1_400L),
            ),
        )
        dao.insertWorkoutLogEvents(listOf(WorkoutLogEventEntity(id = 80L, dayId = 10L, sessionId = 50L, type = "ADD_SET", syncStatus = "PENDING", createdAt = 1_400L)))
        dao.insertWorkoutLogEventSets(
            listOf(
                WorkoutLogEventSetEntity(eventId = 80L, snapshotRole = "CURRENT", snapshotIndex = 0, id = 70L, exerciseId = 20L, performedExerciseId = 60L, sourceWorkoutExerciseId = 30L, weight = 85.0, reps = 6, rpe = 9.0, setType = "WORKING", restSeconds = 120, orderIndex = 0, completed = true, loggedAt = 1_400L),
            ),
        )

        dao.updateActiveWorkoutSet(
            sessionId = 50L,
            draft = ActiveWorkoutDraftEntity(sessionId = 50L, exerciseId = 20L, weight = "90", reps = "5", rpe = "9.5", setType = "WORKING"),
            set = ActiveWorkoutSetEntity(sessionId = 50L, id = 70L, exerciseId = 20L, performedExerciseId = 60L, sourceWorkoutExerciseId = 30L, weight = 90.0, reps = 5, rpe = 9.5, setType = "WORKING", restSeconds = 150, orderIndex = 0, completed = true, loggedAt = 1_800L),
            restTimerEndsAt = 4_000L,
            restTimerTotalSeconds = 150,
            updatedAt = 1_800L,
        )

        closeDatabase()
        database = openDatabase()
        val reopened = database.dao()
        val active = reopened.observeActiveWorkoutSessions().first().single()
        val draft = reopened.observeActiveWorkoutDrafts().first().single()
        val activeSet = reopened.observeActiveWorkoutSets().first().single()
        val eventSet = reopened.observeWorkoutLogEventSets().first().single()

        assertEquals(1_800L, active.updatedAt)
        assertEquals(4_000L, active.restTimerEndsAt)
        assertEquals(150, active.restTimerTotalSeconds)
        assertEquals("90", draft.weight)
        assertEquals("5", draft.reps)
        assertEquals(90.0, activeSet.weight, 0.0)
        assertEquals(5, activeSet.reps)
        assertEquals(9.5, activeSet.rpe, 0.0)
        assertEquals(150, activeSet.restSeconds)
        assertEquals(1_800L, activeSet.loggedAt)
        assertEquals(90.0, eventSet.weight, 0.0)
        assertEquals(5, eventSet.reps)
        assertEquals(9.5, eventSet.rpe, 0.0)
        assertEquals(150, eventSet.restSeconds)
        assertEquals(1_800L, eventSet.loggedAt)
    }

    @Test
    fun targetedActiveWorkoutFinishSurvivesDatabaseReopenAndClearsRuntimeRows() = runTest {
        val dao = database.dao()

        dao.insertRoutines(listOf(WorkoutRoutineEntity(id = 1L, name = "Finish routine", description = "Targeted", active = true)))
        dao.insertWorkoutDays(listOf(WorkoutDayEntity(id = 10L, routineId = 1L, name = "Dag 1", orderIndex = 0)))
        dao.insertExercises(listOf(ExerciseEntity(id = 20L, name = "Bench", muscleGroup = "Borst", equipment = "Barbell")))
        dao.insertWorkoutExercises(listOf(WorkoutExerciseEntity(id = 30L, dayId = 10L, exerciseId = 20L, targetSets = 3, repRange = "6-8", restSeconds = 120, orderIndex = 0)))
        dao.startOrResumeActiveWorkoutSession(
            activeSession = ActiveWorkoutSessionEntity(sessionId = 50L, dayId = 10L, routineId = 1L, startedAt = 1_000L, updatedAt = 1_000L),
            draftSession = WorkoutSessionEntity(id = 50L, date = 1_000L, duration = 0L, status = "DRAFT", completed = false, routineId = 1L, workoutDayId = 10L, startedAt = 1_000L),
            drafts = listOf(ActiveWorkoutDraftEntity(sessionId = 50L, exerciseId = 20L, weight = "80", reps = "8", rpe = "8", setType = "NORMAL")),
            performedExercises = listOf(PerformedExerciseEntity(id = 60L, sessionId = 50L, exerciseId = 20L, sourceWorkoutExerciseId = 30L, orderIndex = 0)),
        )
        dao.insertActiveWorkoutSets(
            listOf(
                ActiveWorkoutSetEntity(sessionId = 50L, id = 70L, exerciseId = 20L, performedExerciseId = 60L, sourceWorkoutExerciseId = 30L, weight = 85.0, reps = 6, rpe = 9.0, setType = "WORKING", restSeconds = 120, orderIndex = 0, completed = true, loggedAt = 1_400L),
            ),
        )
        dao.insertWorkoutLogEvents(listOf(WorkoutLogEventEntity(id = 80L, dayId = 10L, sessionId = 50L, type = "ADD_SET", syncStatus = "PENDING", createdAt = 1_400L)))
        dao.insertWorkoutLogEventSets(
            listOf(
                WorkoutLogEventSetEntity(eventId = 80L, snapshotRole = "CURRENT", snapshotIndex = 0, id = 70L, exerciseId = 20L, performedExerciseId = 60L, sourceWorkoutExerciseId = 30L, weight = 85.0, reps = 6, rpe = 9.0, setType = "WORKING", restSeconds = 120, orderIndex = 0, completed = true, loggedAt = 1_400L),
            ),
        )
        dao.finishActiveWorkoutSession(
            session = WorkoutSessionEntity(id = 50L, date = 1_000L, duration = 1_800L, status = "COMPLETED", completed = true, routineId = 1L, workoutDayId = 10L, startedAt = 1_000L, endedAt = 2_800L),
            performedExercises = listOf(PerformedExerciseEntity(id = 60L, sessionId = 50L, exerciseId = 20L, sourceWorkoutExerciseId = 30L, orderIndex = 0)),
            sets = listOf(WorkoutSetEntity(id = 90L, sessionId = 50L, exerciseId = 20L, performedExerciseId = 60L, weight = 85.0, reps = 6, rpe = 9.0, setType = "WORKING", restSeconds = 120, orderIndex = 0, completed = true, loggedAt = 1_400L, completedAt = 1_400L)),
            activeSessionId = 50L,
        )

        closeDatabase()
        database = openDatabase()
        val reopened = database.dao()
        val session = reopened.observeWorkoutSessions().first().single { it.id == 50L }
        val set = reopened.observeWorkoutSets().first().single()

        assertEquals("COMPLETED", session.status)
        assertEquals(true, session.completed)
        assertEquals(1_800L, session.duration)
        assertEquals(90L, set.id)
        assertEquals(85.0, set.weight, 0.0)
        assertTrue(reopened.observeActiveWorkoutSessions().first().isEmpty())
        assertTrue(reopened.observeActiveWorkoutDrafts().first().isEmpty())
        assertTrue(reopened.observeActiveWorkoutSets().first().isEmpty())
        assertTrue(reopened.observeWorkoutLogEvents().first().isEmpty())
        assertTrue(reopened.observeWorkoutLogEventSets().first().isEmpty())
    }

    @Test
    fun targetedActiveWorkoutUndoSurvivesDatabaseReopen() = runTest {
        val dao = database.dao()

        dao.insertRoutines(listOf(WorkoutRoutineEntity(id = 1L, name = "Undo routine", description = "Targeted", active = true)))
        dao.insertWorkoutDays(listOf(WorkoutDayEntity(id = 10L, routineId = 1L, name = "Dag 1", orderIndex = 0)))
        dao.insertExercises(listOf(ExerciseEntity(id = 20L, name = "Bench", muscleGroup = "Borst", equipment = "Barbell")))
        dao.insertWorkoutExercises(listOf(WorkoutExerciseEntity(id = 30L, dayId = 10L, exerciseId = 20L, targetSets = 3, repRange = "6-8", restSeconds = 120, orderIndex = 0)))
        dao.startOrResumeActiveWorkoutSession(
            activeSession = ActiveWorkoutSessionEntity(sessionId = 50L, dayId = 10L, routineId = 1L, startedAt = 1_000L, updatedAt = 1_000L),
            draftSession = WorkoutSessionEntity(id = 50L, date = 1_000L, duration = 0L, status = "DRAFT", completed = false, routineId = 1L, workoutDayId = 10L, startedAt = 1_000L),
            drafts = emptyList(),
            performedExercises = listOf(PerformedExerciseEntity(id = 60L, sessionId = 50L, exerciseId = 20L, sourceWorkoutExerciseId = 30L, orderIndex = 0)),
        )
        dao.insertActiveWorkoutSets(
            listOf(
                ActiveWorkoutSetEntity(sessionId = 50L, id = 70L, exerciseId = 20L, performedExerciseId = 60L, sourceWorkoutExerciseId = 30L, weight = 85.0, reps = 6, rpe = 9.0, setType = "WORKING", restSeconds = 120, orderIndex = 0, completed = true, loggedAt = 1_400L),
                ActiveWorkoutSetEntity(sessionId = 50L, id = 71L, exerciseId = 20L, performedExerciseId = 60L, sourceWorkoutExerciseId = 30L, weight = 80.0, reps = 8, rpe = 8.0, setType = "WORKING", restSeconds = 120, orderIndex = 1, completed = true, loggedAt = 1_450L),
            ),
        )
        dao.undoActiveWorkoutLogEvent(
            sessionId = 50L,
            targetSetId = 70L,
            undoEvent = WorkoutLogEventEntity(id = 81L, dayId = 10L, sessionId = 50L, type = "UNDO", syncStatus = "PENDING", createdAt = 1_500L, targetEventId = 80L),
            undoEventSets = listOf(
                WorkoutLogEventSetEntity(eventId = 81L, snapshotRole = "RESTORED", snapshotIndex = 0, id = 71L, exerciseId = 20L, performedExerciseId = 60L, sourceWorkoutExerciseId = 30L, weight = 80.0, reps = 8, rpe = 8.0, setType = "WORKING", restSeconds = 120, orderIndex = 0, completed = true, loggedAt = 1_200L),
            ),
            updatedAt = 1_500L,
        )

        closeDatabase()
        database = openDatabase()
        val reopened = database.dao()
        val active = reopened.observeActiveWorkoutSessions().first().single()
        val restoredSet = reopened.observeActiveWorkoutSets().first().single()
        val undoEvent = reopened.observeWorkoutLogEvents().first().single()
        val undoSnapshot = reopened.observeWorkoutLogEventSets().first().single()

        assertEquals(1_500L, active.updatedAt)
        assertEquals(71L, restoredSet.id)
        assertEquals(80.0, restoredSet.weight, 0.0)
        assertEquals("UNDO", undoEvent.type)
        assertEquals(80L, undoEvent.targetEventId)
        assertEquals("RESTORED", undoSnapshot.snapshotRole)
        assertEquals(71L, undoSnapshot.id)
    }

    @Test
    fun targetedActiveRoutineSelectionSurvivesDatabaseReopen() = runTest {
        val dao = database.dao()

        dao.insertRoutines(
            listOf(
                WorkoutRoutineEntity(id = 1L, name = "A", description = "Old", active = true),
                WorkoutRoutineEntity(id = 2L, name = "B", description = "New", active = false),
            ),
        )
        dao.setActiveRoutine(2L)

        closeDatabase()
        database = openDatabase()
        val reopened = database.dao()
        val routines = reopened.observeRoutines().first().associateBy { it.id }

        assertEquals(false, routines[1L]?.active)
        assertEquals(true, routines[2L]?.active)
    }

    @Test
    fun targetedExerciseReorderSurvivesDatabaseReopen() = runTest {
        val dao = database.dao()

        dao.insertRoutines(listOf(WorkoutRoutineEntity(id = 1L, name = "Order routine", description = "Targeted", active = true)))
        dao.insertWorkoutDays(listOf(WorkoutDayEntity(id = 10L, routineId = 1L, name = "Dag 1", orderIndex = 0)))
        dao.insertExercises(
            listOf(
                ExerciseEntity(id = 20L, name = "Bench", muscleGroup = "Borst", equipment = "Barbell"),
                ExerciseEntity(id = 21L, name = "Row", muscleGroup = "Rug", equipment = "Dumbbell"),
                ExerciseEntity(id = 22L, name = "Squat", muscleGroup = "Benen", equipment = "Barbell"),
            ),
        )
        dao.insertWorkoutExercises(
            listOf(
                WorkoutExerciseEntity(id = 30L, dayId = 10L, exerciseId = 20L, targetSets = 3, repRange = "6-8", restSeconds = 120, orderIndex = 0),
                WorkoutExerciseEntity(id = 31L, dayId = 10L, exerciseId = 21L, targetSets = 3, repRange = "8-10", restSeconds = 90, orderIndex = 1),
                WorkoutExerciseEntity(id = 32L, dayId = 10L, exerciseId = 22L, targetSets = 4, repRange = "5", restSeconds = 150, orderIndex = 2),
            ),
        )
        dao.reorderExercises(dayId = 10L, orderedIds = listOf(32L, 30L, 31L))

        closeDatabase()
        database = openDatabase()
        val reopened = database.dao()
        val exercises = reopened.observeWorkoutExercises().first().filter { it.dayId == 10L }

        assertEquals(listOf(32L, 30L, 31L), exercises.sortedWith(compareBy<WorkoutExerciseEntity> { it.orderIndex }.thenBy { it.id }).map { it.id })
        assertEquals(listOf(0, 1, 2), exercises.sortedBy { it.orderIndex }.map { it.orderIndex })
    }

    @Test
    fun targetedSupersetGroupSurvivesDatabaseReopen() = runTest {
        val dao = database.dao()

        dao.insertRoutines(listOf(WorkoutRoutineEntity(id = 1L, name = "Superset routine", description = "Targeted", active = true)))
        dao.insertWorkoutDays(listOf(WorkoutDayEntity(id = 10L, routineId = 1L, name = "Dag 1", orderIndex = 0)))
        dao.insertExercises(
            listOf(
                ExerciseEntity(id = 20L, name = "Bench", muscleGroup = "Borst", equipment = "Barbell"),
                ExerciseEntity(id = 21L, name = "Row", muscleGroup = "Rug", equipment = "Dumbbell"),
                ExerciseEntity(id = 22L, name = "Squat", muscleGroup = "Benen", equipment = "Barbell"),
            ),
        )
        dao.insertWorkoutExercises(
            listOf(
                WorkoutExerciseEntity(id = 30L, dayId = 10L, exerciseId = 20L, targetSets = 3, repRange = "6-8", restSeconds = 120, orderIndex = 0),
                WorkoutExerciseEntity(id = 31L, dayId = 10L, exerciseId = 21L, targetSets = 3, repRange = "8-10", restSeconds = 90, orderIndex = 1),
                WorkoutExerciseEntity(id = 32L, dayId = 10L, exerciseId = 22L, targetSets = 4, repRange = "5", restSeconds = 150, orderIndex = 2),
            ),
        )
        dao.setSupersetGroup(workoutExerciseIds = listOf(30L, 31L), groupId = 700L)

        closeDatabase()
        database = openDatabase()
        val reopened = database.dao()
        val exercises = reopened.observeWorkoutExercises().first().associateBy { it.id }

        assertEquals(700L, exercises[30L]?.supersetGroupId)
        assertEquals(700L, exercises[31L]?.supersetGroupId)
        assertEquals(null, exercises[32L]?.supersetGroupId)
    }

    @Test
    fun targetedWorkoutExercisePlanUpdateSurvivesDatabaseReopen() = runTest {
        val dao = database.dao()

        dao.insertRoutines(listOf(WorkoutRoutineEntity(id = 1L, name = "Plan routine", description = "Targeted", active = true)))
        dao.insertWorkoutDays(listOf(WorkoutDayEntity(id = 10L, routineId = 1L, name = "Dag 1", orderIndex = 0)))
        dao.insertExercises(listOf(ExerciseEntity(id = 20L, name = "Bench", muscleGroup = "Borst", equipment = "Barbell")))
        dao.insertWorkoutExercises(
            listOf(
                WorkoutExerciseEntity(id = 30L, dayId = 10L, exerciseId = 20L, targetSets = 2, repRange = "8-10", restSeconds = 90, orderIndex = 0),
            ),
        )
        dao.insertRoutineSets(
            listOf(
                RoutineSetEntity(id = 40L, workoutExerciseId = 30L, orderIndex = 0, targetReps = 8),
                RoutineSetEntity(id = 41L, workoutExerciseId = 30L, orderIndex = 1, targetReps = 8),
            ),
        )
        dao.replaceRoutineSetsForExercise(
            workoutExerciseId = 30L,
            workoutExercise = WorkoutExerciseEntity(
                id = 30L,
                dayId = 10L,
                exerciseId = 20L,
                targetSets = 3,
                repRange = "5",
                restSeconds = 150,
                targetWeightKg = 100.0,
                targetRpe = 8.5,
                setType = "WORKING",
                orderIndex = 0,
            ),
            sets = listOf(
                RoutineSetEntity(id = 40L, workoutExerciseId = 30L, orderIndex = 0, setType = "WORKING", targetReps = 5, targetWeightKg = 100.0, restSeconds = 150, targetRpe = 8.5),
                RoutineSetEntity(id = 41L, workoutExerciseId = 30L, orderIndex = 1, setType = "WORKING", targetReps = 5, targetWeightKg = 100.0, restSeconds = 150, targetRpe = 8.5),
                RoutineSetEntity(id = 42L, workoutExerciseId = 30L, orderIndex = 2, setType = "WORKING", targetReps = 5, targetWeightKg = 100.0, restSeconds = 150, targetRpe = 8.5),
            ),
        )

        closeDatabase()
        database = openDatabase()
        val reopened = database.dao()
        val exercise = reopened.observeWorkoutExercises().first().single { it.id == 30L }
        val sets = reopened.observeRoutineSets().first().filter { it.workoutExerciseId == 30L }

        assertEquals(3, exercise.targetSets)
        assertEquals("5", exercise.repRange)
        assertEquals(150, exercise.restSeconds)
        assertEquals(100.0, exercise.targetWeightKg, 0.0)
        assertEquals(8.5, exercise.targetRpe, 0.0)
        assertEquals(listOf(40L, 41L, 42L), sets.map { it.id })
        assertEquals(listOf(0, 1, 2), sets.map { it.orderIndex })
        assertTrue(sets.all { it.targetReps == 5 && it.restSeconds == 150 && it.targetWeightKg == 100.0 && it.targetRpe == 8.5 })
    }

    @Test
    fun targetedRoutineSetEditAndReplaceSurvivesDatabaseReopen() = runTest {
        val dao = database.dao()

        dao.insertRoutines(listOf(WorkoutRoutineEntity(id = 1L, name = "Set routine", description = "Targeted", active = true)))
        dao.insertWorkoutDays(listOf(WorkoutDayEntity(id = 10L, routineId = 1L, name = "Dag 1", orderIndex = 0)))
        dao.insertExercises(listOf(ExerciseEntity(id = 20L, name = "Bench", muscleGroup = "Borst", equipment = "Barbell")))
        dao.insertWorkoutExercises(
            listOf(
                WorkoutExerciseEntity(id = 30L, dayId = 10L, exerciseId = 20L, targetSets = 3, repRange = "8", restSeconds = 90, targetWeightKg = 80.0, targetRpe = 8.0, setType = "WORKING", orderIndex = 0),
            ),
        )
        dao.insertRoutineSets(
            listOf(
                RoutineSetEntity(id = 40L, workoutExerciseId = 30L, orderIndex = 0, setType = "WORKING", targetReps = 8, targetWeightKg = 80.0, restSeconds = 90, targetRpe = 8.0),
                RoutineSetEntity(id = 41L, workoutExerciseId = 30L, orderIndex = 1, setType = "WORKING", targetReps = 8, targetWeightKg = 80.0, restSeconds = 90, targetRpe = 8.0),
                RoutineSetEntity(id = 42L, workoutExerciseId = 30L, orderIndex = 2, setType = "WORKING", targetReps = 8, targetWeightKg = 80.0, restSeconds = 90, targetRpe = 8.0),
            ),
        )
        dao.updateRoutineSet(
            set = RoutineSetEntity(id = 41L, workoutExerciseId = 30L, orderIndex = 1, setType = "BACKOFF", targetReps = 10, targetWeightKg = 72.5, restSeconds = 75, targetRpe = 7.0),
            workoutExercise = WorkoutExerciseEntity(id = 30L, dayId = 10L, exerciseId = 20L, targetSets = 3, repRange = "10", restSeconds = 75, targetWeightKg = 72.5, targetRpe = 7.0, setType = "BACKOFF", orderIndex = 0),
        )
        dao.replaceRoutineSetsForExercise(
            workoutExerciseId = 30L,
            workoutExercise = WorkoutExerciseEntity(id = 30L, dayId = 10L, exerciseId = 20L, targetSets = 2, repRange = "10", restSeconds = 75, targetWeightKg = 72.5, targetRpe = 7.0, setType = "BACKOFF", orderIndex = 0),
            sets = listOf(
                RoutineSetEntity(id = 41L, workoutExerciseId = 30L, orderIndex = 0, setType = "BACKOFF", targetReps = 10, targetWeightKg = 72.5, restSeconds = 75, targetRpe = 7.0),
                RoutineSetEntity(id = 40L, workoutExerciseId = 30L, orderIndex = 1, setType = "WORKING", targetReps = 8, targetWeightKg = 80.0, restSeconds = 90, targetRpe = 8.0),
            ),
        )

        closeDatabase()
        database = openDatabase()
        val reopened = database.dao()
        val exercise = reopened.observeWorkoutExercises().first().single { it.id == 30L }
        val sets = reopened.observeRoutineSets().first().filter { it.workoutExerciseId == 30L }

        assertEquals(2, exercise.targetSets)
        assertEquals("10", exercise.repRange)
        assertEquals(75, exercise.restSeconds)
        assertEquals(72.5, exercise.targetWeightKg, 0.0)
        assertEquals(7.0, exercise.targetRpe, 0.0)
        assertEquals("BACKOFF", exercise.setType)
        assertEquals(listOf(41L, 40L), sets.map { it.id })
        assertEquals(listOf(0, 1), sets.map { it.orderIndex })
        assertTrue(sets.none { it.id == 42L })
        assertEquals("BACKOFF", sets.first { it.id == 41L }.setType)
        assertEquals(10, sets.first { it.id == 41L }.targetReps)
        assertEquals(72.5, sets.first { it.id == 41L }.targetWeightKg, 0.0)
    }

    @Test
    fun targetedReplaceExerciseInPlanSurvivesDatabaseReopen() = runTest {
        val dao = database.dao()

        dao.insertRoutines(listOf(WorkoutRoutineEntity(id = 1L, name = "Replace routine", description = "Targeted", active = true)))
        dao.insertWorkoutDays(listOf(WorkoutDayEntity(id = 10L, routineId = 1L, name = "Dag 1", orderIndex = 0)))
        dao.insertExercises(
            listOf(
                ExerciseEntity(id = 20L, name = "Bench", muscleGroup = "Borst", equipment = "Barbell"),
                ExerciseEntity(id = 21L, name = "Incline bench", muscleGroup = "Borst", equipment = "Dumbbell"),
            ),
        )
        dao.insertWorkoutExercises(
            listOf(
                WorkoutExerciseEntity(id = 30L, dayId = 10L, exerciseId = 20L, targetSets = 3, repRange = "6-8", restSeconds = 120, orderIndex = 0),
            ),
        )
        dao.insertWorkoutExercise(
            WorkoutExerciseEntity(id = 30L, dayId = 10L, exerciseId = 21L, targetSets = 3, repRange = "6-8", restSeconds = 120, orderIndex = 0),
        )

        closeDatabase()
        database = openDatabase()
        val reopened = database.dao()
        val exercise = reopened.observeWorkoutExercises().first().single { it.id == 30L }

        assertEquals(21L, exercise.exerciseId)
        assertEquals(3, exercise.targetSets)
        assertEquals("6-8", exercise.repRange)
    }

    @Test
    fun targetedReplaceExerciseInActiveWorkoutSurvivesDatabaseReopen() = runTest {
        val dao = database.dao()

        dao.insertRoutines(listOf(WorkoutRoutineEntity(id = 1L, name = "Active replace routine", description = "Targeted", active = true)))
        dao.insertWorkoutDays(listOf(WorkoutDayEntity(id = 10L, routineId = 1L, name = "Dag 1", orderIndex = 0)))
        dao.insertExercises(
            listOf(
                ExerciseEntity(id = 20L, name = "Bench", muscleGroup = "Borst", equipment = "Barbell"),
                ExerciseEntity(id = 21L, name = "Incline bench", muscleGroup = "Borst", equipment = "Dumbbell"),
            ),
        )
        dao.insertWorkoutExercises(
            listOf(
                WorkoutExerciseEntity(id = 30L, dayId = 10L, exerciseId = 20L, targetSets = 3, repRange = "6-8", restSeconds = 120, orderIndex = 0),
            ),
        )
        dao.insertActiveWorkoutSessions(
            listOf(
                ActiveWorkoutSessionEntity(
                    sessionId = 100L,
                    dayId = 10L,
                    routineId = 1L,
                    startedAt = 1_000L,
                    updatedAt = 1_000L,
                ),
            ),
        )
        dao.replaceWorkoutExerciseInActiveWorkout(
            workoutExercise = WorkoutExerciseEntity(id = 30L, dayId = 10L, exerciseId = 21L, targetSets = 3, repRange = "6-8", restSeconds = 120, orderIndex = 0),
            activeSessionId = 100L,
            updatedAt = 2_000L,
        )

        closeDatabase()
        database = openDatabase()
        val reopened = database.dao()
        val exercise = reopened.observeWorkoutExercises().first().single { it.id == 30L }
        val active = reopened.observeActiveWorkoutSessions().first().single()

        assertEquals(21L, exercise.exerciseId)
        assertEquals(3, exercise.targetSets)
        assertEquals(2_000L, active.updatedAt)
    }

    private fun openDatabase(): TrainIqDatabase =
        Room.databaseBuilder(context, TrainIqDatabase::class.java, dbName).build()

    private fun runtimeStore(): RoomTrainIqRuntimeStore {
        val isolatedFilesContext = object : ContextWrapper(context) {
            override fun getFilesDir() = context.cacheDir.resolve("targeted-room-runtime-store").also { it.mkdirs() }
        }
        isolatedFilesContext.filesDir.resolve("trainiq-state.json").delete()
        val planner = JsonRoomImportPlanner()
        val sink = RoomJsonImportSink(database)
        val legacyStore = TrainIqLocalStore(
            context = isolatedFilesContext,
            roomImportDryRun = RoomImportDryRun(planner, sink),
            roomRuntimeReadinessGate = RoomRuntimeReadinessGate(database.dao()),
            roomMigrationChainVerificationProvider = RoomMigrationChainVerificationProvider(
                markerSource = object : RoomMigrationChainVerificationMarkerSource {
                    override fun latestMarker(): RoomMigrationChainVerificationMarker? = null
                },
            ),
        )
        // The fixture owns observation jobs and joins them before closing the database.
        runBlocking { legacyStore.exportLegacyState() }
        val job = SupervisorJob().also { storeJobs.add(it) }
        return RoomTrainIqRuntimeStore(database, legacyStore, CoroutineScope(job + Dispatchers.IO))
    }

    private fun closeDatabase() {
        runBlocking { storeJobs.forEach { it.cancelAndJoin() } }
        storeJobs.clear()
        database.close()
    }
}
