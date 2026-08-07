package sk.styk.martin.apkanalyzer.feature.appdetail.impl

import androidx.compose.runtime.Immutable

internal enum class SensitiveAccess {
    BackgroundLocation,
    Messages,
    CallHistory,
    Contacts,
    Calendar,
}

@Immutable
internal sealed interface AppDetailInsight {
    data object DebugCertificate : AppDetailInsight
    data object CertificateNotYetValid : AppDetailInsight
    data class OutdatedTargetSdk(val targetSdk: Int, val deviceSdk: Int) : AppDetailInsight {
        val apiLevelGap: Int
            get() = deviceSdk - targetSdk
    }
    data class SensitivePermission(val access: SensitiveAccess, val permissionName: String) : AppDetailInsight
}
