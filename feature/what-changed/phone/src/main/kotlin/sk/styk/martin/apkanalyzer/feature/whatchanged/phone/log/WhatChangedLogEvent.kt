package sk.styk.martin.apkanalyzer.feature.whatchanged.phone.log

import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput

internal sealed interface WhatChangedLogEvent {
    data object NavigateBack : WhatChangedLogEvent

    data class NavigateToChange(
        val packageName: String,
        val snapshotId: Long,
        val appName: String,
    ) : WhatChangedLogEvent

    data class NavigateToFullDetail(val input: AppDetailInput.Historical) : WhatChangedLogEvent

    data object NavigateToPaywall : WhatChangedLogEvent

    data object ShowCopiedFeedback : WhatChangedLogEvent
}
