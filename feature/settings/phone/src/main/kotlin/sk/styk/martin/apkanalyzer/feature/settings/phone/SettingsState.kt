package sk.styk.martin.apkanalyzer.feature.settings.phone

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.common.applanguage.AppLanguageSetting
import sk.styk.martin.apkanalyzer.core.common.settings.ColorAppScheme
import sk.styk.martin.apkanalyzer.core.common.settings.FingerprintFormat

@Immutable
data class SettingsState(
    val colorScheme: ColorAppScheme = ColorAppScheme.FollowSystem,
    val fingerprintFormat: FingerprintFormat = FingerprintFormat.ColonSeparated,
    val recentlyViewedAppsEnabled: Boolean = true,
    val appLanguageSetting: AppLanguageSetting = AppLanguageSetting.Unavailable,
)
