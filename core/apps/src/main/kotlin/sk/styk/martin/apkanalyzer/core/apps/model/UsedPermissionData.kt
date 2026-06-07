package sk.styk.martin.apkanalyzer.core.apps.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UsedPermissionData(val permissionData: PermissionData, var isGranted: Boolean) : Parcelable
