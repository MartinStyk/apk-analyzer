package sk.styk.martin.apkanalyzer.feature.appdetail.impl.appcomponents

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.R

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
        ComponentType.Activity -> R.string.components_type_activities
        ComponentType.Service -> R.string.components_type_services
        ComponentType.Receiver -> R.string.components_type_receivers
        ComponentType.Provider -> R.string.components_type_providers
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

@get:StringRes
internal val ComponentFlag.labelRes: Int
    get() = when (this) {
        ComponentFlag.StopWithTask -> R.string.components_flag_stop_with_task
        ComponentFlag.SingleUser -> R.string.components_flag_single_user
        ComponentFlag.IsolatedProcess -> R.string.components_flag_isolated_process
        ComponentFlag.ExternalService -> R.string.components_flag_external_service
    }

@get:StringRes
internal val ComponentFlag.explanationRes: Int
    get() = when (this) {
        ComponentFlag.StopWithTask -> R.string.components_flag_stop_with_task_explanation
        ComponentFlag.SingleUser -> R.string.components_flag_single_user_explanation
        ComponentFlag.IsolatedProcess -> R.string.components_flag_isolated_process_explanation
        ComponentFlag.ExternalService -> R.string.components_flag_external_service_explanation
    }
