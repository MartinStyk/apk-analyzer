package sk.styk.martin.apkanalyzer.feature.whatchanged.phone

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistorySummaryPart
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.AppChangeItem
import java.time.Instant

@Immutable
internal sealed interface WhatChangedState {
    data object Loading : WhatChangedState

    data object Error : WhatChangedState

    data object Empty : WhatChangedState

    @Immutable
    data class Loaded(
        val range: WhatChangedRange,
        val since: Instant,
        val updateCount: Int,
        val newAppCount: Int,
        val lastChangeAt: Instant?,
        val needsAttention: List<AppChangeItem>,
        val changedApps: List<ChangedAppItem>,
        val uninstalledApps: List<UninstalledAppItem>,
    ) : WhatChangedState
}

@Immutable
internal data class ChangedAppItem(
    val packageName: String,
    val appName: String,
    val updateCount: Int,
    val isNew: Boolean,
    val netChange: NetChange,
)

@Immutable
internal sealed interface NetChange {
    data object NotCompared : NetChange

    data object None : NetChange

    data class Changed(val parts: List<AppHistorySummaryPart>, val hasCertificateChange: Boolean) : NetChange
}

@Immutable
internal data class UninstalledAppItem(
    val packageName: String,
    val appName: String,
    val changeCount: Int,
)
