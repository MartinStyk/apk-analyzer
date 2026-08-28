package sk.styk.martin.apkanalyzer.core.appfunctions.usecase

import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.appfunctions.model.PermissionGrantSummary
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepository
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.apps.permissions.UsedPermission
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import javax.inject.Inject

internal class GetAppPermissionsUseCase @Inject constructor(
    private val appDetailRepository: AppDetailRepository,
    private val dispatcherProvider: DispatcherProvider,
) {
    suspend operator fun invoke(packageName: String): List<PermissionGrantSummary> = withContext(dispatcherProvider.io()) {
        appDetailRepository.details(AppReference.InstalledPackage(PackageName(packageName)))
            .getOrElse { throw appDetailLookupFailure(packageName, it) }
            .permissions.used
            .filter { it.permissionData.details?.protectionLevel == ProtectionLevel.Dangerous }
            .map { it.toPermissionGrantSummary() }
    }
}

private fun UsedPermission.toPermissionGrantSummary() = PermissionGrantSummary(
    permission = permissionData.name,
    isGranted = isGranted,
)
