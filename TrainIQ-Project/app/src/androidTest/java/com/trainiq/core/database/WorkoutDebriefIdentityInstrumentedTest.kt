package com.trainiq.core.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trainiq.core.workout.workoutDebriefWorkName
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutDebriefIdentityInstrumentedTest {
    private lateinit var database: TrainIqDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            TrainIqDatabase::class.java,
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun staleDebriefCannotUpdateAReplacementWorkoutWithTheSameNumericId() = runBlocking {
        val dao = database.dao()
        dao.importWorkoutSessions(
            listOf(
                WorkoutSessionEntity(
                    id = 42L,
                    date = 1_000L,
                    duration = 60L,
                    startedAt = 1_000L,
                    endedAt = 1_060L,
                    debriefGenerationId = "old-generation",
                ),
            ),
        )
        assertEquals("old-generation", dao.getWorkoutSessionDebriefGenerationId(42L))
        dao.deleteWorkoutSessionById(42L)
        dao.importWorkoutSessions(
            listOf(
                WorkoutSessionEntity(
                    id = 42L,
                    date = 2_000L,
                    duration = 60L,
                    startedAt = 2_000L,
                    endedAt = 2_060L,
                    debriefGenerationId = "replacement-generation",
                ),
            ),
        )
        assertEquals("replacement-generation", dao.getWorkoutSessionDebriefGenerationId(42L))

        val changedRows = dao.updateWorkoutSessionDebrief(
            sessionId = 42L,
            generationId = "old-generation",
            summary = "stale result",
            progressionFeedback = "",
            recommendation = "",
            nextSessionFocus = "",
            recoveryScore = 75,
            intensitySignal = "MAINTAIN",
            wins = "",
            risks = "",
            nextLoadTarget = "",
            recoveryAdvice = "",
            source = "GEMINI_2_5_FLASH",
        )

        assertEquals(0, changedRows)
        assertEquals("LOCAL_FALLBACK", dao.getCompletedWorkoutSession(42L)?.debriefSource)
        assertEquals("replacement-generation", dao.getCompletedWorkoutSession(42L)?.debriefGenerationId)
    }

    @Test
    fun priorAndReplacementWorkoutsWithTheSameIdHaveDifferentDurableWorkNames() {
        assertNotEquals(
            workoutDebriefWorkName(42L, "old-generation"),
            workoutDebriefWorkName(42L, "replacement-generation"),
        )
        assertTrue(workoutDebriefWorkName(42L, "old-generation").contains("old-generation"))
    }
}
