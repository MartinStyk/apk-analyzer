package sk.styk.martin.apkanalyzer.core.apps.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ServiceData(
    val name: String,
    val permission: String? = null,
    val isExported: Boolean = false,
    var isStopWithTask: Boolean = false,
    var isSingleUser: Boolean = false,
    var isIsolatedProcess: Boolean = false,
    var isExternalService: Boolean = false,
) : Parcelable
