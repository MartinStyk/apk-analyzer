package sk.styk.martin.apkanalyzer.core.apphistory.capture.snapshot

import kotlinx.serialization.Serializable
import sk.styk.martin.apkanalyzer.core.apps.components.BroadcastReceiver

@Serializable
internal data class BroadcastReceiverSnapshot(
    val name: String,
    val permission: String?,
    val isExported: Boolean,
)

internal fun BroadcastReceiver.toSnapshot() = BroadcastReceiverSnapshot(
    name = name,
    permission = permission,
    isExported = isExported,
)
