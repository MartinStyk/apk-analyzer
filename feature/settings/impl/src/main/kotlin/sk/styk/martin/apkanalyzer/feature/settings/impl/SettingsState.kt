package sk.styk.martin.apkanalyzer.feature.settings.impl

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.common.settings.ColorAppScheme

@Immutable
data class SettingsState(val colorScheme: ColorAppScheme = ColorAppScheme.FollowSystem, val recentlyViewedAppsEnabled: Boolean = true)
