package sk.styk.martin.apkanalyzer.core.apphistory.impl.capture

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.apphistory.api.capture.AppHistoryRecording
import sk.styk.martin.apkanalyzer.core.common.settings.Key
import sk.styk.martin.apkanalyzer.core.common.settings.PersistenceRepositoryFake
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlag
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagRepositoryFake

class AppHistoryRecordingRepositoryImplTest {

    private val featureFlags = FeatureFlagRepositoryFake()
    private val preferences = PersistenceRepositoryFake(Key.AppHistoryRecordingEnabled to true)
    private val repository = AppHistoryRecordingRepositoryImpl(featureFlags, preferences)

    @Test
    fun `Given the recording flag is off When the user wants recording Then recording is unavailable`() = runTest {
        val recording = repository.recording().first()

        recording shouldBe AppHistoryRecording.Unavailable
    }

    @Test
    fun `Given the recording flag is on When the user turns recording off Then recording is off`() = runTest {
        featureFlags[FeatureFlag.AppHistoryRecording] = true

        repository.setRecordingEnabled(false)

        repository.recording().first() shouldBe AppHistoryRecording.Off
    }
}
