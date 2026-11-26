package sk.styk.martin.apkanalyzer.feature.apps.tv.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.apps.api.filtering.ActivityQuickFilter
import sk.styk.martin.apkanalyzer.core.apps.api.filtering.PermissionPreset
import sk.styk.martin.apkanalyzer.core.apps.api.filtering.QuickFilter
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.Button
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.FilterChip
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.SectionHeader
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.tv.focus.RestorableFocus
import sk.styk.martin.apkanalyzer.core.designsystem.tv.focus.rememberRestorableFocus
import sk.styk.martin.apkanalyzer.core.designsystem.tv.focus.restorableFocus
import sk.styk.martin.apkanalyzer.core.designsystem.tv.modifier.bleedHorizontally
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme
import sk.styk.martin.apkanalyzer.feature.apps.tv.R

@Composable
internal fun TvAppFilterScreen(viewModel: TvAppFilterViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TvAppFilterContent(state = state, onAction = viewModel::onAction)
}

@Composable
private fun TvAppFilterContent(state: TvAppFilterState, onAction: (TvAppFilterAction) -> Unit) {
    val focus = rememberRestorableFocus(
        availableKeys = buildSet {
            add(CLEAR_KEY)
            state.sources.forEach { add(sourceKey(it.value)) }
            state.installQuickFilters.forEach { add(quickFilterKey(it.value)) }
            state.usageQuickFilters.forEach { add(quickFilterKey(it.value)) }
            state.activityFilters.forEach { add(activityKey(it.value)) }
            state.permissionPresets.forEach { add(presetKey(it.value)) }
            state.sdkVersions.forEach { add(sdkKey(it.sdk)) }
        },
        defaultKey = state.sources.firstOrNull()?.let { sourceKey(it.value) } ?: CLEAR_KEY,
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 27.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(key = HEADER_KEY) {
            Header(state = state, focus = focus, onAction = onAction)
        }
        chipSection(
            key = "sources",
            titleRes = R.string.tv_filter_section_source,
            options = state.sources,
        ) { option ->
            FilterChip(
                selected = option.selected,
                onClick = { onAction(TvAppFilterAction.SourceToggled(option.value)) },
                label = stringResource(R.string.tv_filter_option_count, option.value.label(), option.appCount),
                modifier = Modifier.restorableFocus(focus, sourceKey(option.value)),
            )
        }
        chipSection(
            key = "install",
            titleRes = R.string.tv_filter_section_install,
            options = state.installQuickFilters,
        ) { option ->
            FilterChip(
                selected = option.selected,
                onClick = { onAction(TvAppFilterAction.QuickFilterToggled(option.value)) },
                label = option.value.label(),
                modifier = Modifier.restorableFocus(focus, quickFilterKey(option.value)),
            )
        }
        chipSection(
            key = "usage",
            titleRes = R.string.tv_filter_section_usage,
            options = state.usageQuickFilters + state.activityFilters,
        ) { option ->
            when (val value = option.value) {
                is QuickFilter -> FilterChip(
                    selected = option.selected,
                    onClick = { onAction(TvAppFilterAction.QuickFilterToggled(value)) },
                    label = value.label(),
                    modifier = Modifier.restorableFocus(focus, quickFilterKey(value)),
                )

                is ActivityQuickFilter -> FilterChip(
                    selected = option.selected,
                    onClick = { onAction(TvAppFilterAction.ActivityFilterToggled(value)) },
                    label = value.label(),
                    modifier = Modifier.restorableFocus(focus, activityKey(value)),
                )
            }
        }
        chipSection(
            key = "permissions",
            titleRes = R.string.tv_filter_section_permissions,
            options = state.permissionPresets,
        ) { option ->
            FilterChip(
                selected = option.selected,
                onClick = { onAction(TvAppFilterAction.PermissionPresetToggled(option.value)) },
                label = option.value.label(),
                modifier = Modifier.restorableFocus(focus, presetKey(option.value)),
            )
        }
        chipSection(
            key = "sdk",
            titleRes = R.string.tv_filter_section_sdk,
            options = state.sdkVersions,
        ) { option ->
            val versionLabel = option.versionName
                ?.let { stringResource(R.string.tv_filter_sdk_option, it, option.sdk) }
                ?: stringResource(R.string.tv_filter_sdk_option_unknown, option.sdk)
            FilterChip(
                selected = option.selected,
                onClick = { onAction(TvAppFilterAction.SdkVersionToggled(option.sdk)) },
                label = stringResource(R.string.tv_filter_option_count, versionLabel, option.appCount),
                modifier = Modifier.restorableFocus(focus, sdkKey(option.sdk)),
            )
        }
    }
}

@Composable
private fun Header(
    state: TvAppFilterState,
    focus: RestorableFocus,
    onAction: (TvAppFilterAction) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Column {
            Text(text = stringResource(R.string.tv_filter_title), style = AppTheme.typography.headlineSmall)
            Text(
                text = pluralStringResource(R.plurals.tv_filter_matching, state.matchingAppCount, state.matchingAppCount),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant,
            )
        }
        if (state.isActive) {
            Button(
                onClick = { onAction(TvAppFilterAction.ClearAll) },
                label = stringResource(R.string.tv_filter_clear_all),
                modifier = Modifier.restorableFocus(focus, CLEAR_KEY),
            )
        }
    }
}

private fun <T> LazyListScope.chipSection(
    key: String,
    titleRes: Int,
    options: List<T>,
    chip: @Composable (T) -> Unit,
) {
    if (options.isEmpty()) return
    item(key = key) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionHeader(text = stringResource(titleRes), modifier = Modifier.padding(top = 8.dp))
            LazyRow(
                modifier = Modifier.bleedHorizontally(FocusBleed),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = FocusBleed, vertical = 8.dp),
            ) {
                items(options) { option -> chip(option) }
            }
        }
    }
}

@Composable
private fun AppSource.label(): String = stringResource(
    when (this) {
        AppSource.GooglePlay -> R.string.tv_filter_source_google_play
        AppSource.SamsungGalaxyStore -> R.string.tv_filter_source_samsung_galaxy_store
        AppSource.AmazonAppstore -> R.string.tv_filter_source_amazon_appstore
        AppSource.HuaweiAppGallery -> R.string.tv_filter_source_huawei_app_gallery
        AppSource.XiaomiGetApps -> R.string.tv_filter_source_xiaomi_get_apps
        AppSource.FDroid -> R.string.tv_filter_source_fdroid
        AppSource.AuroraStore -> R.string.tv_filter_source_aurora_store
        AppSource.Sideloaded -> R.string.tv_filter_source_sideloaded
        AppSource.LocalInstall -> R.string.tv_filter_source_local_install
        AppSource.SystemPreinstalled -> R.string.tv_filter_source_system
        AppSource.Unknown -> R.string.tv_filter_source_unknown
    },
)

@Composable
private fun QuickFilter.label(): String = stringResource(
    when (this) {
        QuickFilter.RecentlyInstalled -> R.string.tv_filter_recently_installed
        QuickFilter.RecentlyUpdated -> R.string.tv_filter_recently_updated
        QuickFilter.Large -> R.string.tv_filter_large
        QuickFilter.SensitivePermissions -> R.string.tv_filter_preset_sensitive
    },
)

@Composable
private fun ActivityQuickFilter.label(): String = stringResource(
    when (this) {
        ActivityQuickFilter.RecentlyUsed -> R.string.tv_filter_recently_used
        ActivityQuickFilter.Unused -> R.string.tv_filter_unused
    },
)

@Composable
private fun PermissionPreset.label(): String = stringResource(
    when (this) {
        PermissionPreset.Sensitive -> R.string.tv_filter_preset_sensitive
        PermissionPreset.Camera -> R.string.tv_filter_preset_camera
        PermissionPreset.Microphone -> R.string.tv_filter_preset_microphone
        PermissionPreset.Location -> R.string.tv_filter_preset_location
        PermissionPreset.Contacts -> R.string.tv_filter_preset_contacts
        PermissionPreset.Sms -> R.string.tv_filter_preset_sms
        PermissionPreset.Storage -> R.string.tv_filter_preset_storage
    },
)

private fun sourceKey(source: AppSource) = "source:${source.name}"

private fun quickFilterKey(filter: QuickFilter) = "quick:${filter.name}"

private fun activityKey(filter: ActivityQuickFilter) = "activity:${filter.name}"

private fun presetKey(preset: PermissionPreset) = "preset:$preset"

private fun sdkKey(sdk: Int) = "sdk:$sdk"

private val FocusBleed = 16.dp
private const val HEADER_KEY = "header"
private const val CLEAR_KEY = "clear"

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TvAppFilterContentPreview() {
    ApkAnalyzerTvTheme {
        TvAppFilterContent(
            state = TvAppFilterState(
                matchingAppCount = 12,
                isActive = true,
                sources = listOf(
                    TvCountedOption(AppSource.GooglePlay, selected = true, appCount = 12),
                    TvCountedOption(AppSource.SystemPreinstalled, selected = false, appCount = 140),
                ),
                installQuickFilters = listOf(
                    TvFilterOption(QuickFilter.RecentlyInstalled, selected = false),
                    TvFilterOption(QuickFilter.RecentlyUpdated, selected = false),
                ),
                permissionPresets = PermissionPreset.all.map { TvFilterOption(it, selected = false) },
                sdkVersions = listOf(
                    TvSdkOption(sdk = 35, versionName = "Android 15", selected = false, appCount = 40),
                    TvSdkOption(sdk = 34, versionName = "Android 14", selected = false, appCount = 22),
                ),
            ),
            onAction = {},
        )
    }
}
