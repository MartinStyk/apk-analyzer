package sk.styk.martin.apkanalyzer.core.appaidescription.metadata

import sk.styk.martin.apkanalyzer.core.appaidescription.AppAiContext
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepository
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import javax.inject.Inject

private const val TAG = "AppDetailMetadataProvider"
private const val MAX_COMPONENTS_PER_KIND = 40

internal class AppDetailMetadataProviderImpl @Inject constructor(private val appDetailRepository: AppDetailRepository) : AppMetadataProvider {

    override suspend fun getAppContext(reference: AppReference): AppAiContext? {
        Logger.d(TAG, "AI metadata loading started: reference=$reference")
        val detail = appDetailRepository.details(reference).getOrElse {
            Logger.w(TAG, it, "AI metadata loading failed: app detail unavailable, reference=$reference")
            return null
        }
        Logger.d(TAG, "AI metadata loading finished: reference=$reference")
        return detail.toAppAiContext()
    }

    private fun AppDetail.toAppAiContext() = AppAiContext(
        packageName = info.packageName.value,
        appName = info.applicationName,
        versionCode = info.versionCode,
        targetSdk = info.targetSdkVersion,
        permissions = permissions.defined.map { it.name }.distinct(),
        activities = activities.map { it.name }.distinct().take(MAX_COMPONENTS_PER_KIND),
        services = services.map { it.name }.distinct().take(MAX_COMPONENTS_PER_KIND),
        receivers = receivers.map { it.name }.distinct().take(MAX_COMPONENTS_PER_KIND),
    )
}
