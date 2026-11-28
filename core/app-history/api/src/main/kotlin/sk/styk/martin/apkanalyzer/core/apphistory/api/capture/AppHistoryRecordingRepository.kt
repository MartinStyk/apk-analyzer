package sk.styk.martin.apkanalyzer.core.apphistory.api.capture

import kotlinx.coroutines.flow.Flow

interface AppHistoryRecordingRepository {
    fun recording(): Flow<AppHistoryRecording>

    suspend fun setRecordingEnabled(enabled: Boolean): Result<Unit>
}

enum class AppHistoryRecording {
    Unavailable,
    On,
    Off,
}
