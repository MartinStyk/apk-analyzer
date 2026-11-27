package sk.styk.martin.apkanalyzer.feature.whatchanged.phone

import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.AppChangeItem

internal sealed interface WhatChangedAction {
    data object Retry : WhatChangedAction

    data object OpenFullHistory : WhatChangedAction

    data class SelectRange(val range: WhatChangedRange) : WhatChangedAction

    data class OpenChange(val item: AppChangeItem) : WhatChangedAction

    data class OpenAppHistory(val packageName: String, val appName: String) : WhatChangedAction

    data class CopyItem(val label: String, val value: String) : WhatChangedAction
}
