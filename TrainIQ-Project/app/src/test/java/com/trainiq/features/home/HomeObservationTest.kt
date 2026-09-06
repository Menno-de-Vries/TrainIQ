package com.trainiq.features.home

import com.trainiq.domain.model.*
import com.trainiq.domain.repository.HomeRepository
import com.trainiq.domain.usecase.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.After

@OptIn(ExperimentalCoroutinesApi::class)
class HomeObservationTest {
    @After
    fun resetDispatcher() { Dispatchers.resetMain() }

    @Test
    fun failedObservationBecomesRetryableErrorAndResubscribesToLiveUpdates() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val data = MutableStateFlow(HomeDashboard(null, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, null, 0, "Initial"))
        var subscriptions = 0
        val repository = object : HomeRepository {
            override fun observeDashboard() = flow {
                subscriptions++
                if (subscriptions == 1) error("read failed")
                emitAll(data)
            }
            override suspend fun getHealthConnectStatus() = HealthConnectStatus(state = HealthConnectState.NO_DATA, message = "No data")
            override suspend fun refreshDashboardData() = Unit
        }
        val vm = HomeViewModel(ObserveHomeDashboardUseCase(repository), BuildHomeDashboardUseCase(), GetHealthConnectStatusUseCase(repository), RefreshDashboardDataUseCase(repository))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        runCurrent()
        assertTrue(vm.uiState.value is HomeUiState.Error)
        vm.refreshDashboardAndHealthStatus()
        runCurrent()
        assertEquals(2, subscriptions)
        assertEquals("Initial", (vm.uiState.value as HomeUiState.Success).dashboard.coachInsight)
        data.value = data.value.copy(coachInsight = "Updated")
        runCurrent()
        assertEquals("Updated", (vm.uiState.value as HomeUiState.Success).dashboard.coachInsight)

    }
}
