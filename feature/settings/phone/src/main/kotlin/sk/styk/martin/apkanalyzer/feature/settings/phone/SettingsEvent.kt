package sk.styk.martin.apkanalyzer.feature.settings.phone

sealed interface SettingsEvent {
    data object NavigateBack : SettingsEvent
    data object OpenAppLanguageSettings : SettingsEvent
    data object OpenFeatureFlags : SettingsEvent
}
