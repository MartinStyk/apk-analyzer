package sk.styk.martin.apkanalyzer.core.apppermissions.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocalPermissionStatus(val packageName: String, val isGranted: Boolean = false) : Parcelable
