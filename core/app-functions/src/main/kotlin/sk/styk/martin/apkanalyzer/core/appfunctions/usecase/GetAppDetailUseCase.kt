package sk.styk.martin.apkanalyzer.core.appfunctions.usecase

import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.appfunctions.model.AppDetailResult
import sk.styk.martin.apkanalyzer.core.appfunctions.toAppFunctionLabel
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepository
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import javax.inject.Inject

internal class GetAppDetailUseCase @Inject constructor(
    private val appDetailRepository: AppDetailRepository,
    private val dispatcherProvider: DispatcherProvider,
) {
    suspend operator fun invoke(packageName: String): AppDetailResult = withContext(dispatcherProvider.io()) {
        appDetailRepository.details(AppReference.InstalledPackage(PackageName(packageName)))
            .getOrElse { throw appDetailLookupFailure(packageName, it) }
            .toAppDetailResult()
    }
}

private fun AppDetail.toAppDetailResult() = AppDetailResult(
    packageName = info.packageName.value,
    applicationName = info.applicationName,
    versionName = info.versionName,
    versionCode = info.versionCode,
    targetSdkLabel = info.targetSdkLabel,
    minSdkLabel = info.minSdkLabel,
    installSourceLabel = info.source.toAppFunctionLabel(),
    isSystemApp = info.isSystemApp,
    requestedPermissionCount = permissions.used.size,
    hasMultipleSigners = signing.hasMultipleSigners,
)
