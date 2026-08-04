package sk.styk.martin.apkanalyzer.core.apps.model

import android.content.pm.PermissionInfo

data class Permission(
    val name: String,
    val simpleName: String,
    val groupName: String? = null,
    val protection: Int = PermissionInfo.PROTECTION_NORMAL,
    val protectionFlags: Int = 0,
    val description: String? = null,
    val declaringPackage: String? = null,
)
