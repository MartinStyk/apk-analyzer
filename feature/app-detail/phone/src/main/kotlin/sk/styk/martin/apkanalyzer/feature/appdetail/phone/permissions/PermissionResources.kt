package sk.styk.martin.apkanalyzer.feature.appdetail.phone.permissions

import androidx.compose.ui.graphics.vector.ImageVector
import sk.styk.martin.apkanalyzer.core.apppermissions.api.detail.PermissionItem
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.permissionIcon

internal val PermissionItem.icon: ImageVector
    get() = permissionIcon(name = name, groupName = groupName)
