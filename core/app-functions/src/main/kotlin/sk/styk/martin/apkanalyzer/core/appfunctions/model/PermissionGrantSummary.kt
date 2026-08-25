package sk.styk.martin.apkanalyzer.core.appfunctions.model

import androidx.appfunctions.AppFunctionSerializable

/** Whether an installed app currently holds one of its declared runtime permissions. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class PermissionGrantSummary(
    /** The permission's full name, for example "android.permission.CAMERA". */
    val permission: String,
    /** True if the user currently has this permission granted to the app. */
    val isGranted: Boolean,
)
