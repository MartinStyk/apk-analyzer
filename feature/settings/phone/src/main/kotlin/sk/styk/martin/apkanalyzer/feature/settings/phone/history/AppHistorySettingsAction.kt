package sk.styk.martin.apkanalyzer.feature.settings.phone.history

import android.net.Uri

internal sealed interface AppHistorySettingsAction {
    data class SetRecording(val enabled: Boolean) : AppHistorySettingsAction

    data object ConfirmTurnOff : AppHistorySettingsAction

    data object DismissTurnOff : AppHistorySettingsAction

    data object Export : AppHistorySettingsAction

    data class ExportTo(val destination: Uri) : AppHistorySettingsAction

    data object Import : AppHistorySettingsAction

    data class ImportFrom(val source: Uri) : AppHistorySettingsAction

    data object DocumentPickerUnavailable : AppHistorySettingsAction

    data object NavigateBack : AppHistorySettingsAction
}
