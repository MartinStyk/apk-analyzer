package sk.styk.martin.apkanalyzer.core.apps.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PermissionDataAggregate(val definesPermissions: List<PermissionData>, val usesPermissions: List<UsedPermissionData>) : Parcelable
