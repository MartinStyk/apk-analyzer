package sk.styk.martin.apkanalyzer.feature.pro.phone

internal sealed interface ProEvent {
    data object Close : ProEvent
}
