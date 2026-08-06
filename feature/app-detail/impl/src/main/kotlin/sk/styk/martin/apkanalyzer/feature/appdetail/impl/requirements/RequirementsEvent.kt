package sk.styk.martin.apkanalyzer.feature.appdetail.impl.requirements

sealed interface RequirementsEvent {
    data object ShowCopiedFeedback : RequirementsEvent
}
