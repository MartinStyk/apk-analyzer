package sk.styk.martin.apkanalyzer.feature.whatchanged.phone.log

import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.AppChangeItem

internal sealed interface WhatChangedLogAction {
    data object Back : WhatChangedLogAction

    data object Retry : WhatChangedLogAction

    data class ToggleFilter(val filter: LogFilter) : WhatChangedLogAction

    data class OpenItem(val item: AppChangeItem) : WhatChangedLogAction

    data object OpenLocked : WhatChangedLogAction

    data class CopyItem(val label: String, val value: String) : WhatChangedLogAction
}
