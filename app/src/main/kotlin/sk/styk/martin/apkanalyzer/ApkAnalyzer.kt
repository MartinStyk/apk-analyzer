package sk.styk.martin.apkanalyzer

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import javax.inject.Inject

@HiltAndroidApp
class ApkAnalyzer :
    Application(),
    SingletonImageLoader.Factory {

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun onCreate() {
        super.onCreate()
        Logger.init(logToConsole = BuildConfig.DEBUG)
    }

    override fun newImageLoader(context: Context): ImageLoader = imageLoader
}
