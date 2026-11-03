package sk.styk.martin.apkanalyzer.feature.apps.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class AppFilterNavKey(val scopeId: String) : NavKey
