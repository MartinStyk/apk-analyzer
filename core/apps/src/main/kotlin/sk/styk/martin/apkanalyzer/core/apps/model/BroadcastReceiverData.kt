package sk.styk.martin.apkanalyzer.core.apps.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BroadcastReceiverData(val name: String, val permission: String? = null, val isExported: Boolean = false) : Parcelable
