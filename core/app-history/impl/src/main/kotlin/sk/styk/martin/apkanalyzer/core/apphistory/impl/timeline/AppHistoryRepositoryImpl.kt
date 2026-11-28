package sk.styk.martin.apkanalyzer.core.apphistory.impl.timeline

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transform
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.AppHistoryRepository
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryAppTimeline
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryDeviceTimeline
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistorySection
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistorySnapshot
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistorySnapshotId
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistorySnapshotRead
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryTimeline
import sk.styk.martin.apkanalyzer.core.apphistory.impl.capture.APP_HISTORY
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.AppHistoryPersistenceRepository
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.AppHistorySnapshotHeader
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.CapturedSection
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.StoredAppHistoryRecord
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTracker
import sk.styk.martin.apkanalyzer.core.common.performance.TraceOutcome
import sk.styk.martin.apkanalyzer.core.common.performance.appCount
import sk.styk.martin.apkanalyzer.core.common.performance.outcome
import sk.styk.martin.apkanalyzer.core.common.performance.startCancellableTrace
import sk.styk.martin.apkanalyzer.core.common.performance.timedSection
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
internal class AppHistoryRepositoryImpl @Inject constructor(
    private val persistenceRepository: AppHistoryPersistenceRepository,
    private val timelineBuilder: AppHistoryTimelineBuilder,
    private val performanceTracker: PerformanceTracker,
    private val dispatcherProvider: DispatcherProvider,
    appScope: CoroutineScope,
) : AppHistoryRepository {

    private val deviceCache = ConcurrentHashMap<PackageName, CachedAppTimeline>()

    private val cachedDeviceTimeline = persistenceRepository.observeHeaders()
        .conflate()
        .transform { result ->
            emit(result)
            delay(1.seconds)
        }
        .map { result ->
            buildDeviceTimeline(result.getOrThrow())
        }
        .retryWhen { throwable, attempt ->
            if (attempt == 0L) {
                Logger.e(APP_HISTORY, throwable, "Device timeline load failed; retrying while subscribed")
            } else {
                Logger.d(APP_HISTORY, "Device timeline load still unavailable; retrying while subscribed")
            }
            emit(AppHistoryDeviceTimeline.Unavailable)
            delay(1.seconds)
            true
        }
        .flowOn(dispatcherProvider.default())
        .shareIn(appScope, SharingStarted.WhileSubscribed(), replay = 1)

    override fun timeline(packageName: PackageName): Flow<AppHistoryTimeline> = persistenceRepository.observe(packageName)
        .map { result ->
            result.fold(
                onSuccess = { records -> AppHistoryTimeline.Available(timelineBuilder.entries(records)) },
                onFailure = { throwable ->
                    Logger.e(APP_HISTORY, throwable, "Timeline load failed for ${packageName.value}")
                    AppHistoryTimeline.Unavailable
                },
            )
        }
        .flowOn(dispatcherProvider.default())

    override fun deviceTimeline(): Flow<AppHistoryDeviceTimeline> = cachedDeviceTimeline

    override suspend fun snapshot(id: AppHistorySnapshotId): AppHistorySnapshotRead = persistenceRepository.read(id.value).fold(
        onSuccess = { stored -> stored?.let { AppHistorySnapshotRead.Found(it.toSnapshot()) } ?: AppHistorySnapshotRead.NotFound },
        onFailure = { throwable ->
            Logger.e(APP_HISTORY, throwable, "Snapshot ${id.value} could not be read")
            AppHistorySnapshotRead.Unavailable
        },
    )

    private suspend fun buildDeviceTimeline(headers: List<AppHistorySnapshotHeader>): AppHistoryDeviceTimeline =
        performanceTracker.startCancellableTrace("app_history_device_timeline") {
            timedSection(tag = APP_HISTORY, operation = "Device timeline build", metric = "build_ms") {
                val headersByPackage = headers.groupBy { it.packageName }
                deviceCache.keys.retainAll(headersByPackage.keys)
                var decodedCount = 0
                var cachedCount = 0
                val apps = headersByPackage.mapNotNull { (packageName, packageHeaders) ->
                    currentCoroutineContext().ensureActive()
                    deviceCache[packageName]?.takeIf { it.headers == packageHeaders }?.let {
                        cachedCount++
                        return@mapNotNull it.timeline
                    }
                    val timeline = if (packageHeaders.size == 1) {
                        timelineBuilder.firstSeenOnly(packageHeaders.single())
                    } else {
                        decodedCount++
                        timelineBuilder.appTimeline(persistenceRepository.records(packageName).getOrThrow())
                    }
                    deviceCache[packageName] = CachedAppTimeline(packageHeaders, timeline)
                    timeline
                }
                appCount = headersByPackage.size
                this["decoded_count"] = decodedCount
                this["cached_count"] = cachedCount
                outcome = TraceOutcome.Success
                Logger.d(
                    APP_HISTORY,
                    "Device timeline built: ${headersByPackage.size} apps, ${headersByPackage.size - cachedCount} rebuilt, " +
                        "$decodedCount decoded, $cachedCount cached",
                )
                AppHistoryDeviceTimeline.Available(apps)
            }
        }
}

private data class CachedAppTimeline(val headers: List<AppHistorySnapshotHeader>, val timeline: AppHistoryAppTimeline?)

private fun StoredAppHistoryRecord.toSnapshot() = AppHistorySnapshot(
    id = AppHistorySnapshotId(id),
    capturedAt = record.capturedAt,
    detail = record.detail,
    nativeLibraries = record.nativeLibraries.toSection(),
    signingSchemes = record.signingScheme.toSection(),
)

private fun <T> CapturedSection<T>.toSection(): AppHistorySection<T> = when (this) {
    is CapturedSection.Captured -> AppHistorySection.Recorded(value)
    CapturedSection.Unavailable -> AppHistorySection.NotRecorded
}
