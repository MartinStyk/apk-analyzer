package sk.styk.martin.apkanalyzer.feature.settings.phone.history

internal sealed interface AppHistorySettingsEvent {
    data object NavigateBack : AppHistorySettingsEvent

    data class CreateExportDocument(val fileName: String) : AppHistorySettingsEvent

    data object OpenImportDocument : AppHistorySettingsEvent

    data class ShowFeedback(val feedback: AppHistorySettingsFeedback) : AppHistorySettingsEvent
}

internal sealed interface AppHistorySettingsFeedback {
    data object Exported : AppHistorySettingsFeedback

    data object ExportFailed : AppHistorySettingsFeedback

    data class Imported(val appCount: Int) : AppHistorySettingsFeedback

    data object NothingNewToImport : AppHistorySettingsFeedback

    data object ImportFailed : AppHistorySettingsFeedback

    data object DocumentPickerUnavailable : AppHistorySettingsFeedback
}
