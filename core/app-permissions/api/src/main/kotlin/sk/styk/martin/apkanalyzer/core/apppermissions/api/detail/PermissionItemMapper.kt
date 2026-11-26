package sk.styk.martin.apkanalyzer.core.apppermissions.api.detail

import sk.styk.martin.apkanalyzer.core.apps.api.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.api.permissions.Permission
import sk.styk.martin.apkanalyzer.core.apps.api.permissions.UsedPermission
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

interface PermissionItemMapper {
    fun requested(detail: AppDetail): List<PermissionItem>

    fun defined(detail: AppDetail): List<PermissionItem>

    fun recordedRequested(permissions: List<UsedPermission>, analysedPackage: PackageName): List<PermissionItem>

    fun recordedDefined(permissions: List<Permission>, analysedPackage: PackageName): List<PermissionItem>
}
