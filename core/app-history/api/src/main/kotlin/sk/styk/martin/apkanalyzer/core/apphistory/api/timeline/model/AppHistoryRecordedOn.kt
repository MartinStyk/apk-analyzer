package sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model

sealed interface AppHistoryRecordedOn {
    data object ThisDevice : AppHistoryRecordedOn

    data class OtherDevice(val name: String) : AppHistoryRecordedOn
}
