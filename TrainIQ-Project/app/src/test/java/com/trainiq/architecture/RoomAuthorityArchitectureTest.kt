package com.trainiq.architecture

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomAuthorityArchitectureTest {
    private val projectRoot = File(System.getProperty("user.dir"))
    private val mainSources = File(projectRoot, "src/main/java/com/trainiq")

    @Test
    fun trainIqRepositoryDoesNotDependOnLegacyLocalStore() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()

        assertFalse(repository.contains("TrainIqLocalStore"))
        assertFalse(repository.contains("localStore.state"))
        assertFalse(repository.contains("localStore.update"))
        assertTrue(repository.contains("RoomTrainIqRuntimeStore"))
    }

    @Test
    fun settingsViewModelUsesUseCasesForProfileResetAndDataClear() {
        val settings = File(mainSources, "features/settings/SettingsSection.kt").readText()
        val viewModelConstructor = settings.substringAfter("class SettingsViewModel @Inject constructor(")
            .substringBefore(") : ViewModel()")

        assertFalse(viewModelConstructor.contains("TrainIqLocalStore"))
        assertTrue(viewModelConstructor.contains("ResetProfileUseCase"))
        assertTrue(viewModelConstructor.contains("ClearAppDataUseCase"))
        assertTrue(settings.contains("resetProfileUseCase()"))
        assertTrue(settings.contains("clearAppDataUseCase()"))
    }

    @Test
    fun legacyLocalStoreDoesNotExposeRuntimeStateFlow() {
        val localStore = File(mainSources, "data/local/TrainIqLocalStore.kt").readText()

        assertFalse(localStore.contains("val state: StateFlow<TrainIqStorageState>"))
        assertFalse(localStore.contains("val state ="))
    }
}
