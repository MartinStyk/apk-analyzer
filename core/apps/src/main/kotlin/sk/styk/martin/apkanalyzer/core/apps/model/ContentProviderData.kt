package sk.styk.martin.apkanalyzer.core.apps.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ContentProviderData(val name: String, val authority: String? = null, val readPermission: String? = null, val writePermission: String? = null, val isExported: Boolean = false) : Parcelable
