package sk.styk.martin.apkanalyzer.feature.settings.impl

sealed interface SettingsEvent {
    data object NavigateBack : SettingsEvent
    data object OpenAppLanguageSettings : SettingsEvent
}
