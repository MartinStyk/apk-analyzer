package sk.styk.martin.apkanalyzer.core.apps.analysis

import android.content.pm.PermissionInfo
import sk.styk.martin.apkanalyzer.core.apps.model.ProtectionFlag
import sk.styk.martin.apkanalyzer.core.apps.model.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.bytes
import java.io.File

fun computeApkSize(sourceDir: String?): AppSize = (sourceDir?.let { File(it).length() } ?: 0L).bytes

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
