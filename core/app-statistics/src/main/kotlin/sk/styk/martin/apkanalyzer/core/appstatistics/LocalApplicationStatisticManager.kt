package sk.styk.martin.apkanalyzer.core.appstatistics

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.flow
import sk.styk.martin.apkanalyzer.core.appanalysis.AppGeneralDataManager
import sk.styk.martin.apkanalyzer.core.appanalysis.AppInstallSourceManager
import sk.styk.martin.apkanalyzer.core.appanalysis.CertificateManager
import sk.styk.martin.apkanalyzer.core.appanalysis.MAX_SDK_VERSION
import sk.styk.martin.apkanalyzer.core.appanalysis.model.AppSource
import sk.styk.martin.apkanalyzer.core.appanalysis.model.InstallLocation
import sk.styk.martin.apkanalyzer.core.applist.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.appstatistics.model.PercentagePair
import sk.styk.martin.apkanalyzer.core.appstatistics.model.StatisticsData
import sk.styk.martin.apkanalyzer.core.appstatistics.model.toMathStats
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import javax.inject.Inject

private const val ANALYSIS_FLAGS =
    PackageManager.GET_SIGNING_CERTIFICATES or
        PackageManager.GET_ACTIVITIES or
        PackageManager.GET_SERVICES or
        PackageManager.GET_PROVIDERS or
        PackageManager.GET_RECEIVERS or
        PackageManager.GET_PERMISSIONS

@WorkerThread
class LocalApplicationStatisticManager
@Inject
internal constructor(
    private val packageManager: PackageManager,
    private val installedAppsRepository: InstalledAppsRepository,
    private val generalDataService: AppGeneralDataManager,
    private val certificateService: CertificateManager,
    private val appInstallSourceManager: AppInstallSourceManager,
) {
    sealed class StatisticsLoadingStatus {
        data class Loading(val currentProgress: Int, val totalProgress: Int) : StatisticsLoadingStatus()

        data class Data(val data: StatisticsData) : StatisticsLoadingStatus()
    }

    suspend fun loadStatisticsData() = flow {
        val allApps = installedAppsRepository.getAll()
        emit(StatisticsLoadingStatus.Loading(0, allApps.size))

        val statsBuilder = StatisticsDataBuilder(allApps.size)

        allApps.forEachIndexed { index, appListData ->
            statsBuilder.add(singleAppStatisticsData(appListData.packageName))
            emit(StatisticsLoadingStatus.Loading(index, allApps.size))
        }

        emit(StatisticsLoadingStatus.Data(statsBuilder.build()))
    }

    private fun singleAppStatisticsData(packageName: String): StatisticsAppData? {
        val packageInfo =
            try {
                packageManager.getPackageInfo(packageName, ANALYSIS_FLAGS)
            } catch (e: Exception) {
                Logger.w("AppStatistics", e, "Package info for statistics failed. Package name= $packageName")
                return null
            }

        val applicationInfo = packageInfo.applicationInfo ?: return null

        val isSystemApp = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

        return StatisticsAppData(
            packageName = packageName,
            isSystemApp = isSystemApp,
            installLocation = packageInfo.installLocation,
            targetSdk = applicationInfo.targetSdkVersion,
            minSdk = applicationInfo.minSdkVersion,
            apkSize = if (applicationInfo.sourceDir != null) generalDataService.computeApkSize(applicationInfo.sourceDir) else 0,
            appSource = appInstallSourceManager.getAppInstallSource(packageInfo),
            signAlgorithm = certificateService.getSignAlgorithm(packageInfo) ?: "Unknown",
            activities = packageInfo.activities?.size ?: 0,
            services = packageInfo.services?.size ?: 0,
            providers = packageInfo.providers?.size ?: 0,
            receivers = packageInfo.receivers?.size ?: 0,
            definedPermissions = packageInfo.permissions?.size ?: 0,
            usedPermissions = packageInfo.requestedPermissions?.size ?: 0,
        )
    }

    private data class StatisticsAppData(
        var packageName: String,
        var isSystemApp: Boolean = false,
        var installLocation: Int = 0,
        var targetSdk: Int = 0,
        var minSdk: Int = 0,
        var apkSize: Long = 0,
        var appSource: AppSource = AppSource.UNKNOWN,
        var signAlgorithm: String,
        var activities: Int = 0,
        var services: Int = 0,
        var providers: Int = 0,
        var receivers: Int = 0,
        var usedPermissions: Int = 0,
        var definedPermissions: Int = 0,
    )

    private class StatisticsDataBuilder(datasetSize: Int) {
        private val arraySize = datasetSize + 1

        private var analyzeSuccess = 0
        private var analyzeFailed = 0

        private var systemApps: Int = 0
        private val installLocation = HashMap<InstallLocation, MutableList<String>>(3)
        private val targetSdk = HashMap<Int, MutableList<String>>(MAX_SDK_VERSION)
        private val minSdk = HashMap<Int, MutableList<String>>(MAX_SDK_VERSION)
        private val appSource = HashMap<AppSource, MutableList<String>>(AppSource.values().size)

        private val apkSize = FloatArray(arraySize)

        private val signAlgorithm = HashMap<String, MutableList<String>>(5)

        private val activities = FloatArray(arraySize)
        private val services = FloatArray(arraySize)
        private val providers = FloatArray(arraySize)
        private val receivers = FloatArray(arraySize)

        private val usedPermissions = FloatArray(arraySize)
        private val definedPermissions = FloatArray(arraySize)

        fun build(): StatisticsData = StatisticsData(
            analyzeSuccess = PercentagePair.from(analyzeSuccess, analyzeSuccess + analyzeFailed),
            analyzeFailed = PercentagePair.from(analyzeFailed, analyzeSuccess + analyzeFailed),
            systemApps = PercentagePair.from(systemApps, analyzeSuccess),
            installLocation = installLocation,
            targetSdk = targetSdk,
            minSdk = minSdk,
            appSource = appSource,
            apkSize = apkSize.toMathStats(),
            signAlgorithm = signAlgorithm,
            activities = activities.toMathStats(),
            services = services.toMathStats(),
            receivers = receivers.toMathStats(),
            providers = providers.toMathStats(),
            usedPermissions = usedPermissions.toMathStats(),
            definedPermissions = definedPermissions.toMathStats(),
        )

        fun add(appData: StatisticsAppData?) {
            if (appData == null) {
                analyzeFailed++
                return
            }

            analyzeSuccess++
            if (appData.isSystemApp) systemApps++
            addToMap(installLocation, InstallLocation.from(appData.installLocation), appData.packageName)
            addToMap(targetSdk, appData.targetSdk, appData.packageName)
            addToMap(minSdk, appData.minSdk, appData.packageName)
            apkSize[analyzeSuccess] = appData.apkSize.toFloat()
            addToMap(signAlgorithm, appData.signAlgorithm, appData.packageName)
            addToMap(appSource, appData.appSource, appData.packageName)

            activities[analyzeSuccess] = appData.activities.toFloat()
            services[analyzeSuccess] = appData.services.toFloat()
            providers[analyzeSuccess] = appData.providers.toFloat()
            receivers[analyzeSuccess] = appData.receivers.toFloat()

            usedPermissions[analyzeSuccess] = appData.usedPermissions.toFloat()
            definedPermissions[analyzeSuccess] = appData.definedPermissions.toFloat()
        }

        private fun <T> addToMap(map: MutableMap<T, MutableList<String>>, key: T, packageName: String) {
            var apps: MutableList<String>? = map[key]

            if (apps != null) {
                apps.add(packageName)
            } else {
                apps = mutableListOf(packageName)
            }

            map[key] = apps
        }
    }
}
