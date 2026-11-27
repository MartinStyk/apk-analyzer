package sk.styk.martin.apkanalyzer.core.apphistory.api.timeline

import kotlinx.coroutines.flow.Flow
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryDeviceTimeline
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistorySnapshotId
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistorySnapshotRead
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryTimeline
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

interface AppHistoryRepository {
    fun timeline(packageName: PackageName): Flow<AppHistoryTimeline>

    fun deviceTimeline(): Flow<AppHistoryDeviceTimeline>

    suspend fun snapshot(id: AppHistorySnapshotId): AppHistorySnapshotRead
}
