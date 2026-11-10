package sk.styk.martin.apkanalyzer.feature.appdetail.domain.insight

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.model.Detail.WhatsApp
import sk.styk.martin.apkanalyzer.core.apps.signing.AppSigning
import sk.styk.martin.apkanalyzer.core.apps.signing.WhatsAppCertificate
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import java.time.Instant

class AppDetailInsightEvaluatorTest {

    @Test
    fun `Given a recorded app with an old target SDK When evaluating it Then no outdated target is asserted`() {
        val recorded = aRecordedApp(targetSdk = 19)

        val insights = AppDetailInsightEvaluator().evaluate(recorded, recordedAt(Instant.ofEpochMilli(1_760_227_200_000)))

        insights.filterIsInstance<AppDetailInsight.OutdatedTargetSdk>() shouldBe emptyList()
    }

    @Test
    fun `Given a certificate that was not yet valid when recorded When evaluating it Then it is judged at the recorded date`() {
        val validFrom = Instant.ofEpochMilli(2_000_000)
        val recorded = aRecordedApp(validFrom = validFrom)

        val insights = AppDetailInsightEvaluator().evaluate(recorded, recordedAt(validFrom.minusMillis(1)))

        insights shouldContain AppDetailInsight.CertificateNotYetValid
    }

    @Test
    fun `Given a certificate that became valid only after the recording When evaluating at a later date Then it is not flagged`() {
        val validFrom = Instant.ofEpochMilli(2_000_000)
        val recorded = aRecordedApp(validFrom = validFrom)

        val insights = AppDetailInsightEvaluator().evaluate(recorded, recordedAt(validFrom.plusMillis(1)))

        insights shouldNotContain AppDetailInsight.CertificateNotYetValid
    }
}

private fun aRecordedApp(targetSdk: Int = 35, validFrom: Instant = Instant.ofEpochMilli(1_000)) = WhatsApp.copy(
    analysisMode = AppDetail.AnalysisMode.Historical,
    info = WhatsApp.info.copy(isSystemApp = false, targetSdkVersion = targetSdk, isDebuggable = false),
    signing = AppSigning(currentCertificates = listOf(WhatsAppCertificate.copy(validFrom = validFrom))),
)

private fun recordedAt(capturedAt: Instant) = AppDetailInput.Historical(packageName = "com.whatsapp", snapshotId = 1, capturedAt = capturedAt)
