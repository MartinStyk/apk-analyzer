package sk.styk.martin.apkanalyzer.core.featureflags.debug

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlag
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagOverride
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagOverridesRepository
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagRepository
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagRepositoryFake
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagStatus
import java.io.IOException

class DataStoreFeatureFlagRepositoryObservationTest {
    private val defaults = FeatureFlagRepositoryFake(FeatureFlag.AppHistoryRecording)
    private val readBarrier = CompletableDeferred<Unit>()
    private val preferences = MutableStateFlow(emptyPreferences())
    private val dataStore = mockk<DataStore<Preferences>> {
        every { data } returns flow {
            readBarrier.await()
            emitAll(preferences)
        }
    }
    private val implementation = DataStoreFeatureFlagRepository(dataStore, defaults)
    private val repository: FeatureFlagRepository = implementation
    private val overrides: FeatureFlagOverridesRepository = implementation

    @Test
    fun `Given storage is still loading When observing Then no default is emitted before the override`() = runTest {
        preferences.value = preferencesOf(booleanPreferencesKey("app_history_recording") to false)
        val emitted = mutableListOf<Boolean>()
        val editorEmitted = mutableListOf<FeatureFlagStatus>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observe(FeatureFlag.AppHistoryRecording).collect { emitted += it }
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            overrides.observe().collect { editorEmitted += it.first { status -> status.flag == FeatureFlag.AppHistoryRecording } }
        }
        runCurrent()
        emitted shouldBe emptyList()
        editorEmitted shouldBe emptyList()

        readBarrier.complete(Unit)
        runCurrent()

        emitted shouldBe listOf(false)
        editorEmitted shouldBe listOf(FeatureFlagStatus(FeatureFlag.AppHistoryRecording, FeatureFlagOverride.Disabled, true))
    }

    @Test
    fun `Given an active observer When its override changes Then it receives the new value once`() = runTest {
        readBarrier.complete(Unit)
        val emitted = mutableListOf<Boolean>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observe(FeatureFlag.AppHistoryRecording).collect { emitted += it }
        }
        runCurrent()

        preferences.value = preferencesOf(booleanPreferencesKey("app_history_recording") to false)
        runCurrent()
        preferences.value = preferencesOf(
            booleanPreferencesKey("app_history_recording") to false,
            booleanPreferencesKey("inventory_export") to false,
        )
        runCurrent()

        emitted shouldBe listOf(true, false)
    }

    @Test
    fun `Given unreadable storage When reading flags Then the storage exception propagates`() = runTest {
        val failure = IOException("Cannot read feature flags")
        every { dataStore.data } returns flow { throw failure }

        val readFailure = shouldThrow<IOException> { repository.get(FeatureFlag.AppHistoryRecording) }
        val editorFailure = shouldThrow<IOException> { overrides.observe().first() }

        readFailure shouldBe failure
        editorFailure shouldBe failure
    }

    @Test
    fun `Given unwritable storage When changing overrides Then the storage exception propagates`() = runTest {
        val failure = IOException("Cannot write feature flags")
        coEvery { dataStore.updateData(any()) } throws failure

        val setFailure = shouldThrow<IOException> { overrides[FeatureFlag.AppHistoryRecording] = FeatureFlagOverride.Disabled }
        val resetFailure = shouldThrow<IOException> { overrides.resetAll() }

        setFailure shouldBe failure
        resetFailure shouldBe failure
    }
}
