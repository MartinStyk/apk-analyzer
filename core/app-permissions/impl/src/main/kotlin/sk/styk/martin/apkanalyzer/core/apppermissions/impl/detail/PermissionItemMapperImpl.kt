package sk.styk.martin.apkanalyzer.core.apppermissions.impl.detail

import sk.styk.martin.apkanalyzer.core.apppermissions.api.PermissionLabelProvider
import sk.styk.martin.apkanalyzer.core.apppermissions.api.detail.GrantState
import sk.styk.martin.apkanalyzer.core.apppermissions.api.detail.PermissionDescriptionProvider
import sk.styk.martin.apkanalyzer.core.apppermissions.api.detail.PermissionItem
import sk.styk.martin.apkanalyzer.core.apppermissions.api.detail.PermissionItemMapper
import sk.styk.martin.apkanalyzer.core.apps.api.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.api.permissions.Permission
import sk.styk.martin.apkanalyzer.core.apps.api.permissions.UsedPermission
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import javax.inject.Inject

internal class PermissionItemMapperImpl @Inject constructor(
    private val permissionLabelProvider: PermissionLabelProvider,
    private val permissionDescriptionProvider: PermissionDescriptionProvider,
) : PermissionItemMapper {

    override fun requested(detail: AppDetail): List<PermissionItem> {
        val reportsGrantState = detail.analysisMode == AppDetail.AnalysisMode.InstalledPackage
        return detail.permissions.used
            .map { used ->
                used.permissionData.toItem(
                    analysedPackage = detail.info.packageName,
                    grantState = when {
                        !reportsGrantState -> null
                        used.isGranted -> GrantState.Granted
                        else -> GrantState.NotGranted
                    },
                )
            }
            .mergeDuplicatesByName()
            .sortedBy { it.label.lowercase() }
    }

    override fun defined(detail: AppDetail): List<PermissionItem> = detail.permissions.defined
        .map { it.toItem(analysedPackage = detail.info.packageName, grantState = null) }
        .mergeDuplicatesByName()
        .sortedBy { it.label.lowercase() }

    override fun recordedRequested(permissions: List<UsedPermission>, analysedPackage: PackageName): List<PermissionItem> = permissions
        .map { it.permissionData.toItem(analysedPackage = analysedPackage, grantState = null) }
        .mergeDuplicatesByName()

    override fun recordedDefined(permissions: List<Permission>, analysedPackage: PackageName): List<PermissionItem> = permissions
        .map { it.toItem(analysedPackage = analysedPackage, grantState = null) }
        .mergeDuplicatesByName()

    private fun Permission.toItem(analysedPackage: PackageName, grantState: GrantState?) = PermissionItem(
        name = name,
        label = permissionLabelProvider.getLabel(name),
        description = permissionDescriptionProvider.describe(this),
        groupName = details?.groupName,
        protectionLevel = details?.protectionLevel,
        protectionFlags = details?.protectionFlags.orEmpty().toList(),
        grantState = grantState,
        declaringPackage = details?.declaringPackage,
        isSelfDeclared = details?.declaringPackage == analysedPackage,
    )
}

private fun List<PermissionItem>.mergeDuplicatesByName(): List<PermissionItem> =
    groupBy { it.name }.values.map { duplicates -> duplicates.reduce(::moreClassified) }

private fun moreClassified(a: PermissionItem, b: PermissionItem): PermissionItem = when {
    (a.protectionLevel == null) != (b.protectionLevel == null) -> if (a.protectionLevel != null) a else b
    else -> a
}
