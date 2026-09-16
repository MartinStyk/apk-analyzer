package sk.styk.martin.apkanalyzer.core.apphistory.capture.snapshot

import kotlinx.serialization.Serializable
import sk.styk.martin.apkanalyzer.core.apps.components.Service

@Serializable
internal data class ServiceSnapshot(
    val name: String,
    val permission: String?,
    val isExported: Boolean,
    val isStopWithTask: Boolean,
    val isSingleUser: Boolean,
    val isIsolatedProcess: Boolean,
    val isExternalService: Boolean,
)

internal fun Service.toSnapshot() = ServiceSnapshot(
    name = name,
    permission = permission,
    isExported = isExported,
    isStopWithTask = isStopWithTask,
    isSingleUser = isSingleUser,
    isIsolatedProcess = isIsolatedProcess,
    isExternalService = isExternalService,
)
