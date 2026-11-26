package sk.styk.martin.apkanalyzer.feature.apps.tv.filter

import sk.styk.martin.apkanalyzer.core.apps.api.filtering.ActivityQuickFilter
import sk.styk.martin.apkanalyzer.core.apps.api.filtering.PermissionPreset
import sk.styk.martin.apkanalyzer.core.apps.api.filtering.QuickFilter
import sk.styk.martin.apkanalyzer.core.common.model.AppSource

internal sealed interface TvAppFilterAction {
    data class SourceToggled(val source: AppSource) : TvAppFilterAction
    data class QuickFilterToggled(val filter: QuickFilter) : TvAppFilterAction
    data class ActivityFilterToggled(val filter: ActivityQuickFilter) : TvAppFilterAction
    data class PermissionPresetToggled(val preset: PermissionPreset) : TvAppFilterAction
    data class SdkVersionToggled(val sdk: Int) : TvAppFilterAction
    data object ClearAll : TvAppFilterAction
}
