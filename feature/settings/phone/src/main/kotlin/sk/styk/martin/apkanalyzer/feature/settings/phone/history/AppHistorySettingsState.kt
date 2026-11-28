package sk.styk.martin.apkanalyzer.feature.settings.phone.history

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apphistory.api.capture.AppHistoryRecording
import sk.styk.martin.apkanalyzer.core.apphistory.api.storage.AppHistoryStorageUsage

@Immutable
internal sealed interface AppHistorySettingsState {
    data object Loading : AppHistorySettingsState

    @Immutable
    data class Loaded(
        val recording: AppHistoryRecording,
        val usage: AppHistoryStorageUsage,
        val transfer: AppHistoryTransfer,
        val isTurnOffConfirmationVisible: Boolean,
    ) : AppHistorySettingsState
}

internal enum class AppHistoryTransfer {
    Idle,
    Exporting,
    Importing,
}
