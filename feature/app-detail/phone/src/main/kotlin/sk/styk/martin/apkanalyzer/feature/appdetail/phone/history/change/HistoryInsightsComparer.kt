package sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.change

import sk.styk.martin.apkanalyzer.core.apphistory.timeline.AppHistoryRepository
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryEntry
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistorySnapshotRead
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistorySnapshotSummary
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.insight.AppDetailInsight
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.insight.AppDetailInsightEvaluator
import javax.inject.Inject

internal class HistoryInsightsComparer @Inject constructor(
    private val appHistoryRepository: AppHistoryRepository,
    private val insightEvaluator: AppDetailInsightEvaluator,
) {
    private val insightsBySnapshot = mutableMapOf<Long, List<AppDetailInsight>?>()

    suspend fun compare(packageName: String, change: AppHistoryEntry.Change): HistoryChangeSection.Insights? {
        val before = recordedInsights(packageName, change.previous) ?: return null
        val after = recordedInsights(packageName, change.snapshot) ?: return null
        val added = after - before.toSet()
        val removed = before - after.toSet()
        return HistoryChangeSection.Insights(added = added, removed = removed).takeIf { added.isNotEmpty() || removed.isNotEmpty() }
    }

    private suspend fun recordedInsights(packageName: String, summary: AppHistorySnapshotSummary): List<AppDetailInsight>? =
        insightsBySnapshot.getOrPut(summary.id.value) {
            (appHistoryRepository.snapshot(summary.id) as? AppHistorySnapshotRead.Found)?.let { read ->
                insightEvaluator.evaluate(
                    appDetail = read.snapshot.detail,
                    input = AppDetailInput.Historical(packageName = packageName, snapshotId = summary.id.value, capturedAt = summary.capturedAt),
                )
            }
        }
}
