package sk.styk.martin.apkanalyzer.tv

import android.app.Application
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import coil3.ImageLoader
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import sk.styk.martin.apkanalyzer.core.common.analytics.AnalyticsFormFactor
import sk.styk.martin.apkanalyzer.core.common.analytics.AnalyticsTracker
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import javax.inject.Inject

@HiltAndroidApp
class TvApkAnalyzer :
    Application(),
    SingletonImageLoader.Factory {

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var lifecycleObservers: Set<@JvmSuppressWildcards DefaultLifecycleObserver>

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    override fun onCreate() {
        super.onCreate()
        Logger.init(logToConsole = BuildConfig.DEBUG)
        analyticsTracker.setFormFactor(AnalyticsFormFactor.Tv)
        lifecycleObservers.forEach { ProcessLifecycleOwner.get().lifecycle.addObserver(it) }
    }

    override fun newImageLoader(context: Context): ImageLoader = imageLoader
}
