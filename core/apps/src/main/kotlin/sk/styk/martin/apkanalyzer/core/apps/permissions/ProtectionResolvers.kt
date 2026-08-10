package sk.styk.martin.apkanalyzer.core.apps.permissions

import android.content.pm.PermissionInfo

@Suppress("DEPRECATION")
internal fun resolveProtectionLevel(protection: Int): ProtectionLevel = when (protection) {
    PermissionInfo.PROTECTION_DANGEROUS -> ProtectionLevel.Dangerous
    PermissionInfo.PROTECTION_SIGNATURE, PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM -> ProtectionLevel.Signature
    PermissionInfo.PROTECTION_INTERNAL -> ProtectionLevel.Internal
    else -> ProtectionLevel.Normal
}

internal fun resolveProtectionFlags(protectionFlags: Int): Set<ProtectionFlag> = protectionFlagsByMask
    .filterKeys { protectionFlags and it != 0 }
    .values
    .toSet()

private val protectionFlagsByMask = mapOf(
    PermissionInfo.PROTECTION_FLAG_PRIVILEGED to ProtectionFlag.Privileged,
    PermissionInfo.PROTECTION_FLAG_APPOP to ProtectionFlag.AppOp,
    PermissionInfo.PROTECTION_FLAG_INSTANT to ProtectionFlag.Instant,
    PermissionInfo.PROTECTION_FLAG_DEVELOPMENT to ProtectionFlag.Development,
)
