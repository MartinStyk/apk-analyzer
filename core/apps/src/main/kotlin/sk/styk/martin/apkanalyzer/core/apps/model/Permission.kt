package sk.styk.martin.apkanalyzer.core.apps.model

import android.content.pm.PermissionInfo

data class Permission(
    val name: String,
    val simpleName: String,
    val groupName: String? = null,
    val protectionLevel: Int = PermissionInfo.PROTECTION_NORMAL,
)
