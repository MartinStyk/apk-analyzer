package sk.styk.martin.apkanalyzer.core.featureflags.debug

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import sk.styk.martin.apkanalyzer.core.common.coroutines.TestDispatcherProvider
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlag
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagOverride
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagOverridesRepository
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagRepository
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagRepositoryFake
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagStatus
import java.io.File

class DataStoreFeatureFlagRepositoryTest {
    @get:Rule
    val dispatcherProvider = TestDispatcherProvider()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.io())
    private val dataStore = PreferenceDataStoreFactory.create(scope = scope) {
        File(temporaryFolder.root, "flags.preferences_pb")
    }
    private val defaults = FeatureFlagRepositoryFake(FeatureFlag.WhatChangedTab, FeatureFlag.AppDetailHistory, FeatureFlag.AppHistoryRecording)
    private var implementation = DataStoreFeatureFlagRepository(dataStore, defaults)
    private val repository: FeatureFlagRepository get() = implementation
    private val overrides: FeatureFlagOverridesRepository get() = implementation

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `Given no stored values When reading Then the build repository supplies defaults`() = runTest {
        val statuses = overrides.observe().first()

        statuses shouldBe listOf(
            FeatureFlagStatus(FeatureFlag.InventoryExport, FeatureFlagOverride.Default, false),
            FeatureFlagStatus(FeatureFlag.WhatChangedTab, FeatureFlagOverride.Default, true),
            FeatureFlagStatus(FeatureFlag.AppDetailHistory, FeatureFlagOverride.Default, true),
            FeatureFlagStatus(FeatureFlag.AppHistoryRecording, FeatureFlagOverride.Default, true),
        )
    }

    @Test
    fun `Given a saved false override When storage reopens Then the override survives`() = runTest {
        overrides[FeatureFlag.AppHistoryRecording] = FeatureFlagOverride.Disabled
        scope.cancel()
        scope.coroutineContext.job.join()

        val reopenedStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(temporaryFolder.root, "flags.preferences_pb")
        }
        implementation = DataStoreFeatureFlagRepository(reopenedStore, defaults)
        val enabled = repository.get(FeatureFlag.AppHistoryRecording)
        val status = overrides.observe().first().first { it.flag == FeatureFlag.AppHistoryRecording }

        enabled shouldBe false
        status shouldBe FeatureFlagStatus(FeatureFlag.AppHistoryRecording, FeatureFlagOverride.Disabled, true)
    }

    @Test
    fun `Given two saved overrides When resetting one Then only its stored key is removed`() = runTest {
        overrides[FeatureFlag.InventoryExport] = FeatureFlagOverride.Enabled
        overrides[FeatureFlag.AppHistoryRecording] = FeatureFlagOverride.Disabled

        overrides[FeatureFlag.AppHistoryRecording] = FeatureFlagOverride.Default
        val statuses = overrides.observe().first()

        statuses.map { it.override } shouldBe
            listOf(FeatureFlagOverride.Enabled, FeatureFlagOverride.Default, FeatureFlagOverride.Default, FeatureFlagOverride.Default)
        statuses.map { it.isEnabled } shouldBe listOf(true, true, true, true)
    }

    @Test
    fun `Given saved overrides When resetting all Then no override remains`() = runTest {
        overrides[FeatureFlag.InventoryExport] = FeatureFlagOverride.Enabled
        overrides[FeatureFlag.AppHistoryRecording] = FeatureFlagOverride.Disabled

        overrides.resetAll()
        val statuses = overrides.observe().first()

        statuses.map { it.override } shouldBe
            listOf(FeatureFlagOverride.Default, FeatureFlagOverride.Default, FeatureFlagOverride.Default, FeatureFlagOverride.Default)
        statuses.map { it.isEnabled } shouldBe listOf(false, true, true, true)
    }

    @Test
    fun `Given app detail history is disabled When reading flags Then the tab and recording stay enabled`() = runTest {
        overrides[FeatureFlag.AppDetailHistory] = FeatureFlagOverride.Disabled

        val tab = repository.get(FeatureFlag.WhatChangedTab)
        val detail = repository.get(FeatureFlag.AppDetailHistory)
        val recording = repository.get(FeatureFlag.AppHistoryRecording)

        tab shouldBe true
        detail shouldBe false
        recording shouldBe true
    }

    @Test
    fun `Given a disabled default When an enabled override is saved Then the flag is enabled`() = runTest {
        defaults[FeatureFlag.InventoryExport] = false
        overrides[FeatureFlag.InventoryExport] = FeatureFlagOverride.Enabled

        val enabled = repository.get(FeatureFlag.InventoryExport)

        enabled shouldBe true
    }
}
