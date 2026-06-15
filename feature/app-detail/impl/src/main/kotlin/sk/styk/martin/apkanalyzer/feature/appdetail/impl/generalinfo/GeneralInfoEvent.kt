package sk.styk.martin.apkanalyzer.feature.appdetail.impl.generalinfo

sealed interface GeneralInfoEvent {
    data object ShowCopiedFeedback : GeneralInfoEvent
}
