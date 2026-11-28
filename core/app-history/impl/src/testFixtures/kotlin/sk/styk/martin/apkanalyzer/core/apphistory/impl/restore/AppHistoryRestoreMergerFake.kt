package sk.styk.martin.apkanalyzer.core.apphistory.impl.restore

internal class AppHistoryRestoreMergerFake : AppHistoryRestoreMerger {
    var mergeCount: Int = 0
        private set

    override suspend fun mergeIfPending(): Result<Unit> {
        mergeCount++
        return Result.success(Unit)
    }
}
