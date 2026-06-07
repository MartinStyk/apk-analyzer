package sk.styk.martin.apkanalyzer.core.apps.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ActivityData(val name: String, val packageName: String, val label: String? = null, val targetActivity: String? = null, val permission: String? = null, val parentName: String? = null, val isExported: Boolean = false) : Parcelable
