package sk.styk.martin.apkanalyzer.feature.whatchanged.phone

import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput

internal sealed interface WhatChangedEvent {
    data object NavigateToFullHistory : WhatChangedEvent

    data object NavigateToSettings : WhatChangedEvent

    data class NavigateToChange(
        val packageName: String,
        val snapshotId: Long,
        val appName: String,
    ) : WhatChangedEvent

    data class NavigateToFullDetail(val input: AppDetailInput.Historical) : WhatChangedEvent

    data class NavigateToAppHistory(val packageName: String, val appName: String) : WhatChangedEvent

    data class NavigateToPaywall(val lockedAppCount: Int) : WhatChangedEvent

    data object ShowCopiedFeedback : WhatChangedEvent
}
