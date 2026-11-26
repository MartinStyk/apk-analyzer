package sk.styk.martin.apkanalyzer.core.apppermissions.detail

import androidx.annotation.StringRes
import sk.styk.martin.apkanalyzer.core.apppermissions.R
import sk.styk.martin.apkanalyzer.core.apps.api.permissions.ProtectionFlag
import sk.styk.martin.apkanalyzer.core.apps.api.permissions.ProtectionLevel

@get:StringRes
val PermissionScope.labelRes: Int
    get() = when (this) {
        PermissionScope.Requested -> R.string.permissions_scope_requested
        PermissionScope.Defined -> R.string.permissions_scope_defined
    }

@get:StringRes
val GrantState.labelRes: Int
    get() = when (this) {
        GrantState.Granted -> R.string.permissions_granted
        GrantState.NotGranted -> R.string.permissions_not_granted
    }

@get:StringRes
val PermissionItem.grantExplanationRes: Int?
    get() = when (val state = grantState) {
        null -> null

        else -> when (protectionLevel) {
            ProtectionLevel.Dangerous -> when (state) {
                GrantState.Granted -> R.string.permissions_grant_dangerous_granted
                GrantState.NotGranted -> R.string.permissions_grant_dangerous_not_granted
            }

            ProtectionLevel.Signature -> when (state) {
                GrantState.Granted -> when {
                    isSelfDeclared -> R.string.permissions_grant_signature_self
                    ProtectionFlag.Privileged in protectionFlags -> R.string.permissions_grant_signature_granted_privileged
                    else -> R.string.permissions_grant_signature_granted
                }

                GrantState.NotGranted -> R.string.permissions_grant_signature_not_granted
            }

            ProtectionLevel.Internal -> when (state) {
                GrantState.Granted -> R.string.permissions_grant_internal_granted
                GrantState.NotGranted -> R.string.permissions_grant_internal_not_granted
            }

            ProtectionLevel.Normal -> when (state) {
                GrantState.Granted -> R.string.permissions_grant_normal_granted
                GrantState.NotGranted -> R.string.permissions_grant_normal_not_granted
            }

            null -> R.string.permissions_grant_unavailable
        }
    }

@get:StringRes
val ProtectionLevel?.labelRes: Int
    get() = when (this) {
        ProtectionLevel.Dangerous -> R.string.permissions_level_dangerous
        ProtectionLevel.Signature -> R.string.permissions_level_signature
        ProtectionLevel.Internal -> R.string.permissions_level_internal
        ProtectionLevel.Normal -> R.string.permissions_level_normal
        null -> R.string.permissions_level_unavailable
    }

@get:StringRes
val ProtectionLevel?.explanationRes: Int
    get() = when (this) {
        ProtectionLevel.Dangerous -> R.string.permissions_level_dangerous_explanation
        ProtectionLevel.Signature -> R.string.permissions_level_signature_explanation
        ProtectionLevel.Internal -> R.string.permissions_level_internal_explanation
        ProtectionLevel.Normal -> R.string.permissions_level_normal_explanation
        null -> R.string.permissions_level_unavailable_explanation
    }

@get:StringRes
val ProtectionFlag.explanationRes: Int
    get() = when (this) {
        ProtectionFlag.Privileged -> R.string.permissions_flag_privileged
        ProtectionFlag.AppOp -> R.string.permissions_flag_appop
        ProtectionFlag.Instant -> R.string.permissions_flag_instant
        ProtectionFlag.Development -> R.string.permissions_flag_development
    }
