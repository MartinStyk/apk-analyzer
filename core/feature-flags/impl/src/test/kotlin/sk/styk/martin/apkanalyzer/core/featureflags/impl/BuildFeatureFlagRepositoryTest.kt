package sk.styk.martin.apkanalyzer.core.featureflags.impl

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.featureflags.api.ApplicationVariant
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlag
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagRepository

class BuildFeatureFlagRepositoryTest {
    private val repository: FeatureFlagRepository = BuildFeatureFlagRepository(ApplicationVariant.Phone)

    @Test
    fun `Given a debug phone build When reading flags Then all existing defaults are enabled`() = runTest {
        val enabled = FeatureFlag.entries.map { repository[it].getOrThrow() }

        enabled shouldBe listOf(true, true, true, true)
    }
}
