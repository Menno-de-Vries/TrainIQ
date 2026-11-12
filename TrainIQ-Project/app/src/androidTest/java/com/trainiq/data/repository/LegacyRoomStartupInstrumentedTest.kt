package com.trainiq.data.repository

import android.content.Context
import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.trainiq.core.database.TrainIqDatabase
import com.trainiq.core.database.RoomMirrorImportRunEntity
import com.trainiq.core.database.UserProfileEntity
import com.trainiq.core.database.ExerciseEntity
import com.trainiq.core.database.WorkoutSessionEntity
import com.trainiq.core.database.WorkoutSetEntity
import com.trainiq.data.local.TrainIqLocalStore
import com.trainiq.data.local.TrainIqStorageState
import com.trainiq.data.migration.JsonRoomImportPlanner
import com.trainiq.data.migration.RoomJsonImportSink
import com.trainiq.domain.workout.*
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class LegacyRoomStartupInstrumentedTest {
    private lateinit var database: TrainIqDatabase
    private lateinit var files: File
    private lateinit var isolatedContext: Context
    private val runtimeJobs = mutableListOf<Job>()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        files = File(context.cacheDir, "legacy-room-startup-${UUID.randomUUID()}").apply { mkdirs() }
        isolatedContext = object : ContextWrapper(context) {
            override fun getFilesDir(): File = files
        }
        database = Room.inMemoryDatabaseBuilder(context, TrainIqDatabase::class.java).build()
    }

    @After
    fun tearDown() = runBlocking {
        runtimeJobs.forEach { it.cancelAndJoin() }
        runtimeJobs.clear()
        database.close()
        files.deleteRecursively()
        Unit
    }

    @Test
    fun staleLegacyJsonCannotReplaceNewerRoomProfileOnStartup() = runBlocking {
        File(files, "trainiq-state.json").writeText(Gson().toJson(TrainIqStorageState(profile = profile("Legacy"))))
        database.dao().upsertUserProfile(profile("Current"))

        legacyStore().exportLegacyState()

        assertEquals("Current", database.dao().readUserProfileForExport()?.name)
    }

    @Test
    fun legacyProfileWithMissingOptionalFieldsMigratesOnFirstStartup() = runBlocking {
        val fixture = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .context.assets.open("room-import/missing-optional-fields-trainiq-state.json")
            .bufferedReader().use { it.readText() }
        File(files, "trainiq-state.json").writeText(fixture)

        val migrated = runtimeStore().readExportSnapshot()

        assertNotNull("Supported legacy profile must survive the first automatic migration", migrated.profile)
        assertEquals("Defaults Athlete", migrated.profile?.name)
        assertEquals(30, migrated.profile?.age)
        assertEquals("MALE", migrated.profile?.sex)
        assertEquals(200.0, migrated.mealItems.single().calories, 0.0)
        assertNotNull(database.dao().getMirrorImportRun("room-runtime-authority"))
    }

    @Test
    fun originalAggregateMealsRetainTheirDateAndNutritionOnFirstStartup() = runBlocking {
        File(files, "trainiq-state.json").writeText("""
            {"meals":[{"id":42,"date":1714557600000,"calories":600,"protein":35,"carbs":70,"fat":20}]}
        """.trimIndent())

        val migrated = runtimeStore().readExportSnapshot()

        val meal = migrated.meals.single()
        assertEquals(42L, meal.id)
        assertEquals(1714557600000L, meal.timestamp)
        val nutrition = migrated.mealItems.single()
        assertEquals(meal.id, nutrition.mealId)
        assertEquals(600.0, nutrition.calories, 0.0)
        assertEquals(35.0, nutrition.protein, 0.0)
        assertEquals(70.0, nutrition.carbs, 0.0)
        assertEquals(20.0, nutrition.fat, 0.0)
        assertEquals(600, database.dao().readMealsForExport().single().calories)
        assertNotNull(database.dao().getMirrorImportRun("room-runtime-authority"))
    }

    @Test
    fun modernMealSnapshotsPreserveDatesAndLeaveMealsWithoutItemsEmpty() = runBlocking {
        File(files, "trainiq-state.json").writeText("""
            {
              "meals":[
                {"id":42,"timestamp":1714557600000,"mealType":"LUNCH","name":"Empty modern meal"},
                {"id":43,"timestamp":1714561200000,"mealType":"SNACK","name":"Modern snack"}
              ],
              "mealItems":[{"id":44,"mealId":43,"calories":200,"protein":20,"carbs":10,"fat":8}]
            }
        """.trimIndent())

        val migrated = runtimeStore().readExportSnapshot()

        assertEquals(1714557600000L, migrated.meals.single { it.id == 42L }.timestamp)
        assertEquals(1714561200000L, migrated.meals.single { it.id == 43L }.timestamp)
        assertEquals(emptyList<com.trainiq.data.local.LoggedMealItemStorage>(), migrated.mealItems.filter { it.mealId == 42L })
        assertEquals(1, migrated.mealItems.size)
        assertEquals(200.0, migrated.mealItems.single().calories, 0.0)
    }

    @Test
    fun exerciseLibrarySeedingBeforeInitialFlowEmissionPreservesCustomHistoryIdentity() = runTest {
        val dao = database.dao()
        val custom = ExerciseEntity(7L, "Custom cable lift", "Core", "Cable")
        dao.insertExercises(listOf(custom))
        dao.importWorkoutSessions(listOf(WorkoutSessionEntity(id = 10L, date = 1_000L, duration = 60L)))
        dao.importWorkoutSets(listOf(WorkoutSetEntity(
            id = 11L, sessionId = 10L, exerciseId = custom.id, weight = 20.0, reps = 8, rpe = 7.0,
        )))
        // Keep the observation scope queued: writes must use Room even before its first Flow emission.
        val runtime = runtimeStore(backgroundScope)
        assertEquals(emptyList<ExerciseEntity>(), runtime.state.value.exercises)

        ExerciseLibrarySeeder(runtime).ensureSeeded()
        ExerciseLibrarySeeder(runtime).ensureSeeded()

        val historySet = dao.readWorkoutSetsForExport().single()
        assertEquals(custom, dao.getExercise(historySet.exerciseId))
        assertEquals(53, dao.readExercisesForExport().size)
        assertEquals(1, dao.readExercisesForExport().count { it.name == "Bench Press" })
    }

    @Test
    fun migratedLegacyProfileCannotReturnAfterItsLastRoomRowIsDeleted() = runBlocking {
        File(files, "trainiq-state.json").writeText(Gson().toJson(TrainIqStorageState(profile = profile("Legacy"))))
        val first = runtimeStore()
        assertEquals("Legacy", first.readExportSnapshot().profile?.name)
        first.clearProfile()

        val restarted = runtimeStore()

        assertNull(restarted.readExportSnapshot().profile)
    }

    @Test
    fun restoredRoomDataSurvivesStartupWithTheOriginalLegacyFile() = runBlocking {
        File(files, "trainiq-state.json").writeText(Gson().toJson(TrainIqStorageState(profile = profile("Legacy"))))
        val initial = runtimeStore()
        assertEquals("Legacy", initial.readExportSnapshot().profile?.name)
        RoomJsonImportSink(database).importTransaction(
            JsonRoomImportPlanner().plan(Gson().toJson(TrainIqStorageState(profile = profile("Restored")))),
        )

        val restarted = runtimeStore()

        assertEquals("Restored", restarted.readExportSnapshot().profile?.name)
    }

    @Test
    fun clearedRoomCannotResurrectLegacyDataBeforeLegacyFileCleanup() = runBlocking {
        File(files, "trainiq-state.json").writeText(Gson().toJson(TrainIqStorageState(profile = profile("Legacy"))))
        val initial = runtimeStore()
        assertEquals("Legacy", initial.readExportSnapshot().profile?.name)
        initial.clearAll()

        val restarted = runtimeStore()

        assertNull(restarted.readExportSnapshot().profile)
    }

    @Test
    fun absentAndInvalidLegacyFilesPreserveCurrentRoomData() = runBlocking {
        database.dao().upsertUserProfile(profile("Current"))
        assertEquals("Current", runtimeStore().readExportSnapshot().profile?.name)
        File(files, "trainiq-state.json").writeText("{ invalid-json")

        assertEquals("Current", runtimeStore().readExportSnapshot().profile?.name)
    }

    @Test
    fun historicalSuccessfulImportProtectsAnIntentionallyEmptyRoomDatabase() = runBlocking {
        File(files, "trainiq-state.json").writeText(Gson().toJson(TrainIqStorageState(profile = profile("Legacy"))))
        database.dao().insertMirrorImportRun(historicalImport(status = "SUCCESS", importedRows = 1))

        assertNull(runtimeStore().readExportSnapshot().profile)
    }

    @Test
    fun historicalFailedImportDoesNotPreventTheFirstSuccessfulLegacyMigration() = runBlocking {
        File(files, "trainiq-state.json").writeText(Gson().toJson(TrainIqStorageState(profile = profile("Legacy"))))
        database.dao().insertMirrorImportRun(historicalImport(status = "FAILED", importedRows = 0))

        assertEquals("Legacy", runtimeStore().readExportSnapshot().profile?.name)
    }

    @Test
    fun invalidInitialLegacySourceCanBeRepairedWithoutLosingItsFirstMigration() = runBlocking {
        val source = File(files, "trainiq-state.json")
        source.writeText("{ invalid-json")

        assertFailedInitialSourceCanRecover(source)
    }

    @Test
    fun unreadableInitialLegacySourceCanBeRepairedWithoutLosingItsFirstMigration() = runBlocking {
        val source = File(files, "trainiq-state.json")
        check(source.mkdir()) // An existing directory cannot be read as the legacy JSON file.

        assertFailedInitialSourceCanRecover(source)
    }

    @Test
    fun unreadableLegacySourceStillEstablishesAuthorityForExistingRoomData() = runBlocking {
        database.dao().upsertUserProfile(profile("Current"))
        val source = File(files, "trainiq-state.json")
        check(source.mkdir())
        val initial = runtimeStore()

        assertEquals("Current", initial.readExportSnapshot().profile?.name)
        assertNotNull(database.dao().getMirrorImportRun("room-runtime-authority"))
        initial.clearProfile()
        check(source.delete())
        source.writeText(Gson().toJson(TrainIqStorageState(profile = profile("Legacy"))))

        assertNull(runtimeStore().readExportSnapshot().profile)
    }

    private suspend fun assertFailedInitialSourceCanRecover(source: File) {
        val initial = runtimeStore()
        assertNull(initial.readExportSnapshot().profile)
        ExerciseLibrarySeeder(initial).ensureSeeded()
        assertEquals(emptyList<ExerciseEntity>(), database.dao().readExercisesForExport())
        assertNull(database.dao().getMirrorImportRun("room-runtime-authority"))
        if (source.isDirectory) check(source.delete())
        source.writeText(Gson().toJson(TrainIqStorageState(profile = profile("Recovered"))))
        val recovered = runtimeStore()

        assertEquals("Recovered", recovered.readExportSnapshot().profile?.name)
        recovered.clearProfile()

        assertNull(runtimeStore().readExportSnapshot().profile)
    }

    private fun historicalImport(status: String, importedRows: Int) = RoomMirrorImportRunEntity(
        generationId = "historical-$status", sourceFingerprint = "fixture", startedAt = 1_000L,
        finishedAt = 1_000L, status = status, schemaVersion = 21, expectedRowCount = 1,
        importedRowCount = importedRows, staleRowCount = 0, mismatchCount = 0,
    )

    private suspend fun runtimeStore(scopeOverride: CoroutineScope? = null): RoomTrainIqRuntimeStore {
        val legacy = legacyStore()
        legacy.exportLegacyState()
        val runtimeScope = scopeOverride ?: CoroutineScope(SupervisorJob().also(runtimeJobs::add) + Dispatchers.IO)
        val clock = object : WorkoutRestTimerClock {
            override fun now() = WorkoutRestTimerNow(1_000L, 1_000L, "fixture")
        }
        val delivery = object : WorkoutRestTimerDelivery {
            override fun schedule(timer: ScheduledWorkoutRestTimer) = Unit
            override fun cancel(identity: WorkoutRestTimerIdentity) = Unit
            override suspend fun show(timer: ScheduledWorkoutRestTimer, silent: Boolean) = false
        }
        val timers = WorkoutRestTimerCoordinator(RoomWorkoutRestTimerStore(database), delivery, clock)
        return RoomTrainIqRuntimeStore(database, legacy, runtimeScope, timers, clock)
    }

    private fun legacyStore() = TrainIqLocalStore(context = isolatedContext)

    private fun profile(name: String) = UserProfileEntity(
        id = 1L, name = name, height = 180.0, weight = 80.0, bodyFat = 20.0,
        activityLevel = "MODERATE", goal = "MAINTENANCE", calorieTarget = 2200,
        proteinTarget = 160, carbsTarget = 240, fatTarget = 70, trainingFocus = "STRENGTH",
    )
}
