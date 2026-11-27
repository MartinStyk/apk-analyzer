package sk.styk.martin.apkanalyzer.core.featureflags.impl

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.featureflags.api.ApplicationVariant
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlag
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagRepository

class BuildFeatureFlagRepositoryTvTest {
    private val repository: FeatureFlagRepository = BuildFeatureFlagRepository(ApplicationVariant.Tv)

    @Test
    fun `Given a debug TV build When reading flags Then history is unsupported and disabled`() = runTest {
        val enabled = FeatureFlag.entries.map { repository[it].getOrThrow() }

        enabled shouldBe listOf(true, false, false, false)
    }
}
