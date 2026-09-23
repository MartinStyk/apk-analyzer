package sk.styk.martin.apkanalyzer.core.apphistory.restore

internal interface AppHistoryRestoreMerger {
    suspend fun mergeIfPending(): Result<Unit>
}
