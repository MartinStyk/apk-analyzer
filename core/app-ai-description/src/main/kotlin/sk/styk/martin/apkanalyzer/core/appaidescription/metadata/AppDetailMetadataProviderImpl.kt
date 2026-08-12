package sk.styk.martin.apkanalyzer.core.appaidescription.metadata

import sk.styk.martin.apkanalyzer.core.appaidescription.AppAiContext
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepository
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.apps.permissions.UsedPermission
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import javax.inject.Inject

private const val TAG = "AppDetailMetadataProvider"
private const val MAX_COMPONENTS_PER_KIND = 40
private const val MAX_PERMISSIONS = 30

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
        targetSdk = info.targetSdkVersion,
        permissions = permissions.used.mostRelevantNames(),
        activities = activities.map { it.name }.normalize(),
        services = services.map { it.name }.normalize(),
        receivers = receivers.map { it.name }.normalize(),
    )

    private fun List<UsedPermission>.mostRelevantNames(): List<String> = asSequence()
        .distinctBy { it.permissionData.name }
        .sortedWith(compareBy({ it.relevanceRank() }, { it.permissionData.name }))
        .take(MAX_PERMISSIONS)
        .map { it.permissionData.name }
        .toList()

    private fun UsedPermission.relevanceRank(): Int = when (permissionData.details?.protectionLevel) {
        ProtectionLevel.Dangerous -> 0
        ProtectionLevel.Normal -> 1
        ProtectionLevel.Signature -> 2
        ProtectionLevel.Internal -> 3
        null -> 4
    }

    private fun List<String>.normalize(): List<String> = distinct().sorted().take(MAX_COMPONENTS_PER_KIND)
}
