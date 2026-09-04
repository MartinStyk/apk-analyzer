package sk.styk.martin.apkanalyzer.core.apphistory.capture.snapshot

import kotlinx.serialization.Serializable
import sk.styk.martin.apkanalyzer.core.apps.components.Activity

@Serializable
internal data class ActivitySnapshot(
    val name: String,
    val packageName: String,
    val label: String?,
    val targetActivity: String?,
    val permission: String?,
    val parentName: String?,
    val isExported: Boolean,
    val isLauncher: Boolean?,
)

internal fun Activity.toSnapshot() = ActivitySnapshot(
    name = name,
    packageName = packageName.value,
    label = label,
    targetActivity = targetActivity,
    permission = permission,
    parentName = parentName,
    isExported = isExported,
    isLauncher = isLauncher,
)
