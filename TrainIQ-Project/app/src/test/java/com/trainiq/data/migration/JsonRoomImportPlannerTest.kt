package com.trainiq.data.migration

import com.trainiq.core.database.MealEntity
import com.trainiq.data.local.TrainIqStorageState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonRoomImportPlannerTest {
    @Test
    fun representativeJsonMapsToRoomCompatibleTablesAndReportsParityGaps() {
        val source = fixture("valid-representative-trainiq-state.json")

        val plan = JsonRoomImportPlanner().plan(source)

        assertEquals("Fixture Athlete", plan.profile?.name)
        assertEquals(1, plan.routines.size)
        assertEquals(1, plan.days.size)
        assertEquals(1, plan.exercises.size)
        assertEquals(1, plan.workoutExercises.size)
        assertEquals(1, plan.routineSets.size)
        assertEquals(1, plan.sessions.size)
        assertEquals(1, plan.performedExercises.size)
        assertEquals(1, plan.workoutSets.size)
        assertEquals(1, plan.meals.size)
        assertEquals(640, plan.meals.single().calories)
        assertEquals(1, plan.foodItems.size)
        assertEquals(1, plan.recipes.size)
        assertEquals(1, plan.recipeIngredients.size)
        assertEquals(1, plan.mealItems.size)
        assertEquals(1, plan.activeWorkoutSessions.size)
        assertEquals(1, plan.activeWorkoutDrafts.size)
        assertEquals(1, plan.activeWorkoutCollapsedExercises.size)
        assertEquals(1, plan.activeWorkoutSets.size)
        assertEquals(1, plan.workoutLogEvents.size)
        assertEquals(1, plan.workoutLogEventSets.size)
        assertEquals(1, plan.measurements.size)
        assertTrue(plan.schemaParityGaps.isEmpty())
    }

    @Test
    fun currentLiveShapeFixtureMapsWithoutSchemaParityGaps() {
        val source = fixture("live-shape-current-trainiq-state.json")

        val plan = JsonRoomImportPlanner().plan(source)

        assertEquals("Live Shape Athlete", plan.profile?.name)
        assertEquals(1, plan.routines.size)
        assertEquals(2, plan.days.size)
        assertEquals(2, plan.exercises.size)
        assertEquals(2, plan.workoutExercises.size)
        assertEquals(2, plan.routineSets.size)
        assertEquals(1, plan.sessions.size)
        assertEquals(1, plan.performedExercises.size)
        assertEquals(1, plan.workoutSets.size)
        assertEquals(1, plan.meals.size)
        assertEquals(2, plan.foodItems.size)
        assertEquals(1, plan.recipes.size)
        assertEquals(2, plan.recipeIngredients.size)
        assertEquals(1, plan.mealItems.size)
        assertEquals(1, plan.activeWorkoutSessions.size)
        assertEquals(1, plan.activeWorkoutSets.size)
        assertEquals(1, plan.workoutLogEvents.size)
        assertEquals(2, plan.workoutLogEventSets.size)
        assertEquals(1, plan.measurements.size)
        assertTrue(plan.importedRowCount() > 25)
        assertTrue(plan.schemaParityGaps.isEmpty())
        assertEquals(source, fixture("live-shape-current-trainiq-state.json"))
    }

    @Test
    fun minimalJsonCreatesEmptyImportPlanWithoutMutatingSource() {
        val source = fixture("minimal-valid-trainiq-state.json")

        val plan = JsonRoomImportPlanner().plan(source)

        assertEquals(source, fixture("minimal-valid-trainiq-state.json"))
        assertEquals(null, plan.profile)
        assertTrue(plan.allTableRows().all { it == 0 })
        assertTrue(plan.schemaParityGaps.isEmpty())
    }

    @Test
    fun missingOptionalFieldsUseStorageDefaultsAndAggregateMealItems() {
        val plan = JsonRoomImportPlanner().plan(fixture("missing-optional-fields-trainiq-state.json"))

        assertNotNull(plan.profile)
        assertEquals(30, plan.profile?.age)
        assertEquals("MALE", plan.profile?.sex)
        assertEquals(
            MealEntity(
                id = 201,
                date = 1714557600000,
                mealType = "LUNCH",
                name = "",
                notes = null,
                calories = 200,
                protein = 20,
                carbs = 10,
                fat = 8,
            ),
            plan.meals.single(),
        )
    }

    @Test
    fun malformedJsonReturnsFallbackWithoutImporting() = runTest {
        val source = fixture("malformed-trainiq-state.json")
        val fallback = TrainIqStorageState()
        val sink = RecordingImportSink()

        val outcome = JsonRoomImportCoordinator(JsonRoomImportPlanner(), sink)
            .tryImport(sourceJson = source, fallbackState = fallback)

        assertTrue(outcome is JsonRoomImportOutcome.Failed)
        outcome as JsonRoomImportOutcome.Failed
        assertEquals(fallback, outcome.fallbackState)
        assertEquals(source, outcome.sourceJson)
        assertTrue(sink.committedPlans.isEmpty())
    }

    @Test
    fun failedImportDoesNotMutateSourceAndDoesNotTrustRoom() = runTest {
        val source = fixture("valid-representative-trainiq-state.json")
        val fallback = TrainIqStorageState()
        val sink = RecordingImportSink(fail = true)

        val outcome = JsonRoomImportCoordinator(JsonRoomImportPlanner(), sink)
            .tryImport(sourceJson = source, fallbackState = fallback)

        assertTrue(outcome is JsonRoomImportOutcome.Failed)
        outcome as JsonRoomImportOutcome.Failed
        assertEquals(fallback, outcome.fallbackState)
        assertEquals(source, outcome.sourceJson)
        assertFalse(outcome.roomTrusted)
        assertTrue(sink.committedPlans.isEmpty())
    }

    @Test
    fun repeatedImportBuildsStableIdempotentPlans() = runTest {
        val source = fixture("valid-representative-trainiq-state.json")
        val sink = RecordingImportSink()
        val coordinator = JsonRoomImportCoordinator(JsonRoomImportPlanner(), sink)

        val first = coordinator.tryImport(source, TrainIqStorageState())
        val second = coordinator.tryImport(source, TrainIqStorageState())

        assertTrue(first is JsonRoomImportOutcome.Imported)
        assertTrue(second is JsonRoomImportOutcome.Imported)
        assertEquals(sink.committedPlans[0], sink.committedPlans[1])
    }

    private fun JsonRoomImportPlan.allTableRows(): List<Int> = listOf(
        if (profile == null) 0 else 1,
        routines.size,
        days.size,
        exercises.size,
        workoutExercises.size,
        routineSets.size,
        sessions.size,
        performedExercises.size,
        workoutSets.size,
        meals.size,
        measurements.size,
    )

    private fun fixture(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource("room-import/$name")) { "Missing fixture $name" }
            .readText()
}

private class RecordingImportSink(
    private val fail: Boolean = false,
) : JsonRoomImportSink {
    val committedPlans = mutableListOf<JsonRoomImportPlan>()

    override suspend fun importTransaction(
        plan: JsonRoomImportPlan,
        mirrorRun: RoomMirrorImportRun?,
    ): RoomMirrorImportReport {
        if (fail) error("fixture import failure")
        committedPlans += plan
        return RoomMirrorImportReport(
            generationId = mirrorRun?.generationId,
            expectedRowCount = plan.importedRowCount(),
            importedRowCount = plan.importedRowCount(),
            staleRowsRemoved = 0,
            mismatchCount = 0,
        )
    }
}
