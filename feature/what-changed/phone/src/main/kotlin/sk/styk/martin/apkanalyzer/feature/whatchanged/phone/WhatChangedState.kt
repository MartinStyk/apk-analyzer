package sk.styk.martin.apkanalyzer.feature.whatchanged.phone

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistorySummaryPart
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.access.LockableRow
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.AppChangeItem
import java.time.Instant

@Immutable
internal sealed interface WhatChangedState {
    data object Loading : WhatChangedState

    data object Error : WhatChangedState

    data class Empty(val isRecordingOff: Boolean) : WhatChangedState

    @Immutable
    data class Loaded(
        val range: WhatChangedRange,
        val since: Instant,
        val updateCount: Int,
        val newAppCount: Int,
        val lastChangeAt: Instant?,
        val needsAttention: List<LockableRow<AppChangeItem>>,
        val changedApps: List<LockableRow<ChangedAppItem>>,
        val uninstalledApps: List<LockableRow<UninstalledAppItem>>,
        val lockedAppCount: Int,
        val isRecordingOff: Boolean,
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
