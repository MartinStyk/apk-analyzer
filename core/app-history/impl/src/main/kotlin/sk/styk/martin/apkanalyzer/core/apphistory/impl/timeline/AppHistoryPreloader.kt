package sk.styk.martin.apkanalyzer.core.apphistory.impl.timeline

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.AppHistoryRepository
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryDeviceTimeline
import sk.styk.martin.apkanalyzer.core.apphistory.impl.capture.APP_HISTORY
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.device.DeviceInfoProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlag
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagRepository
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

private val PRELOAD_DELAY = 20.seconds

@Singleton
internal class AppHistoryPreloader @Inject constructor(
    private val historyRepository: Lazy<AppHistoryRepository>,
    private val featureFlagRepository: FeatureFlagRepository,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val appScope: CoroutineScope,
    private val dispatcherProvider: DispatcherProvider,
) : DefaultLifecycleObserver {
    private val isScheduled = AtomicBoolean(false)

    override fun onCreate(owner: LifecycleOwner) {
        if (!isScheduled.compareAndSet(false, true)) return
        appScope.launch(dispatcherProvider.default()) {
            if (canPreload()) {
                Logger.i(APP_HISTORY, "History preload scheduled in ${PRELOAD_DELAY.inWholeSeconds} seconds")
                delay(PRELOAD_DELAY)
                preload()
            }
        }
    }

    private suspend fun canPreload(): Boolean {
        if (!featureFlagRepository[FeatureFlag.WhatChangedTab]) {
            Logger.i(APP_HISTORY, "History preload skipped: What changed is disabled")
            return false
        }
        if (deviceInfoProvider.isLowRamDevice) {
            Logger.i(APP_HISTORY, "History preload skipped: low-RAM device")
            return false
        }
        return true
    }

    private suspend fun preload() {
        Logger.i(APP_HISTORY, "History preload started")
        when (val timeline = historyRepository.get().deviceTimeline().first()) {
            is AppHistoryDeviceTimeline.Available -> Logger.i(APP_HISTORY, "History preload completed: ${timeline.apps.size} apps")
            AppHistoryDeviceTimeline.Unavailable -> Logger.w(APP_HISTORY, "History preload failed: timeline unavailable")
        }
    }
}
