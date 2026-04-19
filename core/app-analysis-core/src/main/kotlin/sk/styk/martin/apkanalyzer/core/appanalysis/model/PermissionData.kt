package sk.styk.martin.apkanalyzer.core.appanalysis.model

import android.content.pm.PermissionInfo
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PermissionData(val name: String, val simpleName: String, val groupName: String? = null, val protectionLevel: Int = PermissionInfo.PROTECTION_NORMAL) : Parcelable
