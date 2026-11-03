package sk.styk.martin.apkanalyzer.feature.appexport.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class AppInventoryExportNavKey(val packageName: String) : NavKey
