package sk.styk.martin.apkanalyzer.feature.appdetail.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class ApkFileDetailNavKey(val apkFilePath: String) : NavKey
