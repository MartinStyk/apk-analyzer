package sk.styk.martin.apkanalyzer.core.apps.insight

import io.kotest.matchers.collections.shouldNotContain
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.apps.model.Detail.WhatsApp
import java.time.Instant

class AppDetailBadgeEvaluatorTest {

    @Test
    fun `Given a recorded app updated just now When evaluating badges Then no recency badge is claimed`() {
        val now = Instant.now()
        val info = WhatsApp.info.copy(firstInstallTime = now, lastUpdateTime = now, lastUsedTime = now)

        val badges = AppDetailBadgeEvaluator.evaluateRecorded(info)

        badges shouldNotContain AppDetailBadge.RecentlyInstalled
        badges shouldNotContain AppDetailBadge.RecentlyUpdated
        badges shouldNotContain AppDetailBadge.RecentlyUsed
    }
}
