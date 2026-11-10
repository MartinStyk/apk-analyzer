package sk.styk.martin.apkanalyzer.feature.appdetail.domain.insight

import io.kotest.matchers.collections.shouldNotContain
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.apps.model.Detail.WhatsApp
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import java.time.Instant

class AppDetailBadgeEvaluatorTest {

    @Test
    fun `Given a recorded version installed right before capture When evaluating badges Then no recency badge is claimed`() {
        val capturedAt = Instant.now()
        val info = WhatsApp.info.copy(firstInstallTime = capturedAt, lastUpdateTime = capturedAt, lastUsedTime = capturedAt)
        val input = AppDetailInput.Historical(packageName = "com.whatsapp", snapshotId = 1, capturedAt = capturedAt)

        val badges = AppDetailBadgeEvaluator().evaluate(info, input)

        badges shouldNotContain AppDetailBadge.RecentlyInstalled
        badges shouldNotContain AppDetailBadge.RecentlyUpdated
        badges shouldNotContain AppDetailBadge.RecentlyUsed
    }
}
