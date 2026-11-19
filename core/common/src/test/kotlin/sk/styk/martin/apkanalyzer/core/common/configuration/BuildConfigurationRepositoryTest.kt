package sk.styk.martin.apkanalyzer.core.common.configuration

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BuildConfigurationRepositoryTest {

    private val tv: ConfigurationRepository = BuildConfigurationRepository(ApplicationVariant.Tv)

    @Test
    fun `Given the TV application When reading history configuration Then recording and display are disabled`() = runTest {
        val recording = tv.get(ConfigurationKey.AppHistoryRecordingEnabled)
        val display = tv.observe(ConfigurationKey.AppHistoryDisplayEnabled).first()

        recording shouldBe false
        display shouldBe false
    }
}
