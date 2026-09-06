package com.trainiq.features.progress

import com.trainiq.domain.model.*
import com.trainiq.domain.repository.ProgressRepository
import com.trainiq.domain.usecase.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.After

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressSaveTest {
    @After
    fun resetDispatcher() { Dispatchers.resetMain() }

    @Test
    fun duplicateSubmitIsIgnoredAndFailureAllowsRetryWithOriginalValues() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        var attempts = 0
        val gate = CompletableDeferred<Unit>()
        val writes = mutableListOf<ValidatedProgressMeasurement>()
        val repository = object : ProgressRepository {
            override fun observeProgressOverview() = flowOf(ProgressOverview(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), 0.0, null))
            override suspend fun analyzeBodyMeasurementPhoto(path: String, context: String): BodyMeasurementPhotoResult = error("unused")
            override suspend fun deleteMeasurement(measurementId: Long) = Unit
            override suspend fun addMeasurement(weight: Double, bodyFat: Double, muscleMass: Double) {
                attempts++
                if (attempts == 1) { gate.await(); error("disk failure") }
                writes += ValidatedProgressMeasurement(weight, bodyFat, muscleMass)
            }
        }
        val vm = ProgressViewModel(ObserveProgressUseCase(repository), AnalyzeBodyMeasurementPhotoUseCase(repository), AddMeasurementUseCase(repository), DeleteMeasurementUseCase(repository))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        runCurrent()
        vm.addMeasurement("80,5", "20", "40")
        vm.addMeasurement("80,5", "20", "40")
        runCurrent()
        assertEquals(1, attempts)
        assertTrue((vm.uiState.value as ProgressUiState.Success).isSaving)
        gate.complete(Unit)
        runCurrent()
        assertFalse((vm.uiState.value as ProgressUiState.Success).isSaving)
        assertTrue((vm.uiState.value as ProgressUiState.Success).message!!.text.contains("mislukt"))
        assertTrue(writes.isEmpty())
        vm.addMeasurement("80,5", "20", "40")
        runCurrent()
        assertEquals(listOf(ValidatedProgressMeasurement(80.5, 20.0, 40.0)), writes)
        assertEquals(writes.single(), (vm.uiState.value as ProgressUiState.Success).savedMeasurement)
        vm.addMeasurement("invalid", "20", "40")
        runCurrent()
        assertEquals(2, attempts)

    }
}
