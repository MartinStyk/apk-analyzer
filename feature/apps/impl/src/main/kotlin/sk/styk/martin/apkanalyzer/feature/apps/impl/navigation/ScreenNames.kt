package sk.styk.martin.apkanalyzer.feature.apps.impl.navigation

import androidx.navigation3.runtime.NavKey
import sk.styk.martin.apkanalyzer.core.navigation.ScreenOpenEvent
import sk.styk.martin.apkanalyzer.feature.apps.api.AppsNavKey

private const val SCREEN_APPS = "apps"
private const val SCREEN_APP_FILTER = "app_filter"
private const val SCREEN_APP_SEARCH = "app_search"
private const val SCREEN_PERMISSION_FILTER = "permission_filter"

fun screenOpenEvent(key: NavKey): ScreenOpenEvent? = when (key) {
    is AppsNavKey -> ScreenOpenEvent(SCREEN_APPS)
    is AppFilterNavKey -> ScreenOpenEvent(SCREEN_APP_FILTER)
    is AppSearchNavKey -> ScreenOpenEvent(SCREEN_APP_SEARCH)
    is PermissionFilterNavKey -> ScreenOpenEvent(SCREEN_PERMISSION_FILTER)
    else -> null
}
