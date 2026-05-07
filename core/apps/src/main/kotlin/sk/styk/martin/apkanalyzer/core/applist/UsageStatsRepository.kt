package sk.styk.martin.apkanalyzer.core.applist

interface UsageStatsRepository {
    fun isPermissionGranted(): Boolean
    fun lastUsedTimes(): Map<String, Long>
}
