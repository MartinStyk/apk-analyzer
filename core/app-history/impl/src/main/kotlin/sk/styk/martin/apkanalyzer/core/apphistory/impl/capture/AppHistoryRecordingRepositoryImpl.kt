package sk.styk.martin.apkanalyzer.core.apphistory.impl.capture

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import sk.styk.martin.apkanalyzer.core.apphistory.api.capture.AppHistoryRecording
import sk.styk.martin.apkanalyzer.core.apphistory.api.capture.AppHistoryRecordingRepository
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.settings.Key
import sk.styk.martin.apkanalyzer.core.common.settings.PersistenceRepository
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlag
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AppHistoryRecordingRepositoryImpl @Inject constructor(
    private val featureFlagRepository: FeatureFlagRepository,
    private val persistenceRepository: PersistenceRepository,
) : AppHistoryRecordingRepository {

    override fun recording(): Flow<AppHistoryRecording> = combine(
        featureFlagRepository.observe(FeatureFlag.AppHistoryRecording),
        persistenceRepository.observe(Key.AppHistoryRecordingEnabled),
    ) { isAvailable, isEnabled ->
        when {
            !isAvailable -> AppHistoryRecording.Unavailable
            isEnabled -> AppHistoryRecording.On
            else -> AppHistoryRecording.Off
        }
    }.distinctUntilChanged()

    override suspend fun setRecordingEnabled(enabled: Boolean): Result<Unit> = runCatchingCancellable {
        persistenceRepository.save(Key.AppHistoryRecordingEnabled, enabled)
        Logger.i(APP_HISTORY, "History recording turned ${if (enabled) "on" else "off"} by the user")
    }.onFailure {
        Logger.w(APP_HISTORY, it, "History recording preference could not be saved")
    }
}
