package com.trainiq.data.repository

import android.content.Context
import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.trainiq.core.database.TrainIqDatabase
import com.trainiq.core.database.RoomMirrorImportRunEntity
import com.trainiq.core.database.UserProfileEntity
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
        assertNull(runtimeStore().readExportSnapshot().profile)
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

    private suspend fun runtimeStore(): RoomTrainIqRuntimeStore {
        val legacy = legacyStore()
        legacy.exportLegacyState()
        val job = SupervisorJob().also(runtimeJobs::add)
        val clock = object : WorkoutRestTimerClock {
            override fun now() = WorkoutRestTimerNow(1_000L, 1_000L, "fixture")
        }
        val delivery = object : WorkoutRestTimerDelivery {
            override fun schedule(timer: ScheduledWorkoutRestTimer) = Unit
            override fun cancel(identity: WorkoutRestTimerIdentity) = Unit
            override suspend fun show(timer: ScheduledWorkoutRestTimer, silent: Boolean) = false
        }
        val timers = WorkoutRestTimerCoordinator(RoomWorkoutRestTimerStore(database), delivery, clock)
        return RoomTrainIqRuntimeStore(database, legacy, CoroutineScope(job + Dispatchers.IO), timers, clock)
    }

    private fun legacyStore() = TrainIqLocalStore(context = isolatedContext)

    private fun profile(name: String) = UserProfileEntity(
        id = 1L, name = name, height = 180.0, weight = 80.0, bodyFat = 20.0,
        activityLevel = "MODERATE", goal = "MAINTENANCE", calorieTarget = 2200,
        proteinTarget = 160, carbsTarget = 240, fatTarget = 70, trainingFocus = "STRENGTH",
    )
}
