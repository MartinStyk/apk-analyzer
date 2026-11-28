package sk.styk.martin.apkanalyzer.feature.pro.phone

import sk.styk.martin.apkanalyzer.feature.pro.api.ProSheetResult

internal sealed interface ProEvent {
    data object Close : ProEvent

    data class CloseWithResult(val result: ProSheetResult) : ProEvent
}
