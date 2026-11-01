package sk.styk.martin.apkanalyzer.feature.apps.phone.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import sk.styk.martin.apkanalyzer.feature.apps.phone.filter.AppFilterScope

@Serializable
data class AppFilterNavKey(val scope: AppFilterScope) : NavKey
