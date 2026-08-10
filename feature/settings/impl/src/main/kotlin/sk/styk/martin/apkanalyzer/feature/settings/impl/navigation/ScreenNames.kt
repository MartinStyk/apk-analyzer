package sk.styk.martin.apkanalyzer.feature.settings.impl.navigation

import androidx.navigation3.runtime.NavKey
import sk.styk.martin.apkanalyzer.core.navigation.ScreenOpenEvent
import sk.styk.martin.apkanalyzer.feature.settings.api.SettingsNavKey

private const val SCREEN_SETTINGS = "settings"

fun screenOpenEvent(key: NavKey): ScreenOpenEvent? = when (key) {
    is SettingsNavKey -> ScreenOpenEvent(SCREEN_SETTINGS)
    else -> null
}
