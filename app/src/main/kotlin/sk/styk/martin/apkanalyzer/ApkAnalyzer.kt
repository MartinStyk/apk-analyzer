package sk.styk.martin.apkanalyzer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import sk.styk.martin.apkanalyzer.core.common.logger.Logger

@HiltAndroidApp
class ApkAnalyzer : Application() {
    override fun onCreate() {
        super.onCreate()
        Logger.init(logToConsole = BuildConfig.DEBUG)
    }
}
