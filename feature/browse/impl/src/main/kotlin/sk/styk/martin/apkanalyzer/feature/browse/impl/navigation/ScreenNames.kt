package sk.styk.martin.apkanalyzer.feature.browse.impl.navigation

import androidx.navigation3.runtime.NavKey
import sk.styk.martin.apkanalyzer.core.navigation.ScreenOpenEvent
import sk.styk.martin.apkanalyzer.feature.browse.api.BrowseNavKey

private const val SCREEN_BROWSE = "browse"
private const val SCREEN_BROWSE_OPTIONS = "browse_options"
private const val SCREEN_BROWSE_APPS = "browse_apps"

fun screenOpenEvent(key: NavKey): ScreenOpenEvent? = when (key) {
    is BrowseNavKey -> ScreenOpenEvent(SCREEN_BROWSE)
    is BrowseOptionsNavKey -> ScreenOpenEvent(SCREEN_BROWSE_OPTIONS, "dimension=${key.dimension}")
    is BrowseAppsNavKey -> ScreenOpenEvent(SCREEN_BROWSE_APPS, "dimension=${key.dimension}")
    else -> null
}
