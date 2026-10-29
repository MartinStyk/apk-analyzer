package sk.styk.martin.apkanalyzer.core.apphistory.timeline

import kotlinx.coroutines.flow.Flow
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryTimeline
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

interface AppHistoryRepository {
    fun timeline(packageName: PackageName): Flow<AppHistoryTimeline>
}
