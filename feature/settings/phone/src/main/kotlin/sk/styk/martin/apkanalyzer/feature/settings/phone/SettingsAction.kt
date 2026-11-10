package sk.styk.martin.apkanalyzer.feature.settings.phone

import sk.styk.martin.apkanalyzer.core.common.settings.ColorAppScheme
import sk.styk.martin.apkanalyzer.core.common.settings.FingerprintFormat

sealed interface SettingsAction {
    data class ColorSchemeSelected(val colorScheme: ColorAppScheme) : SettingsAction
    data class FingerprintFormatSelected(val fingerprintFormat: FingerprintFormat) : SettingsAction
    data class RecentlyViewedAppsToggled(val enabled: Boolean) : SettingsAction
    data object OpenLanguageSettings : SettingsAction
    data object NavigateBack : SettingsAction
}
