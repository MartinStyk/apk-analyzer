package sk.styk.martin.apkanalyzer.feature.appdetail.phone.appcomponents

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.R
import sk.styk.martin.apkanalyzer.core.apps.api.R as AppsR

internal val ComponentType.icon: ImageVector
    get() = when (this) {
        ComponentType.Activity -> ApkAnalyzerIcons.ComponentActivity
        ComponentType.Service -> ApkAnalyzerIcons.ComponentService
        ComponentType.Receiver -> ApkAnalyzerIcons.ComponentReceiver
        ComponentType.Provider -> ApkAnalyzerIcons.ComponentProvider
    }

@get:StringRes
internal val ComponentScope.labelRes: Int
    get() = when (this) {
        ComponentScope.All -> R.string.components_scope_all
        ComponentScope.Activities -> R.string.components_scope_activities
        ComponentScope.Services -> R.string.components_scope_services
        ComponentScope.Receivers -> R.string.components_scope_receivers
        ComponentScope.Providers -> R.string.components_scope_providers
    }

@get:StringRes
internal val ComponentFilter.labelRes: Int
    get() = when (this) {
        ComponentFilter.Exported -> R.string.components_filter_exported
        ComponentFilter.Unprotected -> R.string.components_filter_unprotected
    }

@get:StringRes
internal val ComponentType.sectionLabelRes: Int
    get() = when (this) {
        ComponentType.Activity -> AppsR.string.components_type_activities
        ComponentType.Service -> AppsR.string.components_type_services
        ComponentType.Receiver -> AppsR.string.components_type_receivers
        ComponentType.Provider -> AppsR.string.components_type_providers
    }

@get:PluralsRes
internal val ComponentScope.hintRes: Int
    get() = when (this) {
        ComponentScope.All -> R.plurals.components_filter_hint_all
        ComponentScope.Activities -> R.plurals.components_filter_hint_activities
        ComponentScope.Services -> R.plurals.components_filter_hint_services
        ComponentScope.Receivers -> R.plurals.components_filter_hint_receivers
        ComponentScope.Providers -> R.plurals.components_filter_hint_providers
    }
