package sk.styk.martin.apkanalyzer.core.apphistory.impl.restore

internal interface AppHistoryRestoreMerger {
    suspend fun mergeIfPending(): Result<Unit>
}
