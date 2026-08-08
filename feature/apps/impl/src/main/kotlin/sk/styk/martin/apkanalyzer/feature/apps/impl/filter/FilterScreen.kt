package sk.styk.martin.apkanalyzer.feature.apps.impl.filter

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import sk.styk.martin.apkanalyzer.core.uilibrary.components.BottomSheet
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Button
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Chip
import sk.styk.martin.apkanalyzer.core.uilibrary.components.DateRangePickerDialog
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Icon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.LoadingSpinner
import sk.styk.martin.apkanalyzer.core.uilibrary.components.RangeSlider
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.components.TextButton
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Toolbar
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.card
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.feature.apps.impl.R
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.AppFilterState
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.AppSizeRange
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.DateRange
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.PermissionPreset
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.UnusedAppsPeriod
import java.text.DateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale

@Composable
internal fun FilterScreen(
    onBack: () -> Unit,
    onPermissionFilter: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FilterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                FilterEvent.NavigateBack -> onBack()

                FilterEvent.OpenUsagePermissionSettings -> {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }

                FilterEvent.NavigateToPermissionFilter -> onPermissionFilter()
            }
        }
    }

    BackHandler(enabled = state.hasUnsavedChanges) {
        viewModel.onAction(FilterAction.NavigateBack)
    }

    FilterContent(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Suppress("LongMethod")
@Composable
private fun FilterContent(
    state: FilterState,
    onAction: (FilterAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        Toolbar(
            title = stringResource(R.string.filter),
            onBack = { onAction(FilterAction.NavigateBack) },
            actions = {
                TextButton(
                    text = stringResource(R.string.filter_reset),
                    onClick = { onAction(FilterAction.Reset) },
                )
            },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            SourceSection(
                selectedSources = state.filter.selectedSources,
                onSourceToggle = { source, selected -> onAction(FilterAction.SourceToggled(source, selected)) },
            )

            PermissionsSection(
                activePermissionPresets = state.activePermissionPresets,
                extraPermissionCount = state.extraPermissionCount,
                onPresetToggle = { onAction(FilterAction.PermissionPresetToggled(it)) },
                onOpenPermissionFilter = { onAction(FilterAction.OpenPermissionFilter) },
            )

            UnusedAppsSection(
                sectionState = state.unusedAppsSectionState,
                selectedPeriod = state.filter.unusedPeriod,
                onPeriodSelect = { onAction(FilterAction.UnusedPeriodSelected(it)) },
                onGrantAccess = { onAction(FilterAction.OpenUsagePermissionSettings) },
            )

            InstallTimeSection(
                installTimeRange = state.filter.installTimeRange,
                onSelectDateRange = { showDatePicker = true },
                onClearDateRange = { onAction(FilterAction.InstallTimeRangeCleared) },
            )

            ApkSizeSection(
                sectionState = state.apkSizeSectionState,
                selectedRange = state.filter.apkSizeRange,
                onRangeChange = { onAction(FilterAction.ApkSizeRangeChanged(it)) },
            )

            TotalSizeSection(
                sectionState = state.totalSizeSectionState,
                selectedRange = state.filter.totalSizeRange,
                onRangeChange = { onAction(FilterAction.TotalSizeRangeChanged(it)) },
                onGrantAccess = { onAction(FilterAction.OpenUsagePermissionSettings) },
            )

            SdkVersionSection(
                availableSdkVersions = state.availableSdkVersions,
                selectedSdkVersions = state.filter.selectedSdkVersions,
                onSdkVersionToggle = { onAction(FilterAction.SdkVersionToggled(it)) },
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            text = stringResource(R.string.filter_apply),
            onClick = { onAction(FilterAction.Apply) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
        )
    }

    if (showDatePicker) {
        DateRangePickerDialog(
            initialStartMs = state.filter.installTimeRange?.start?.toEpochMilli(),
            initialEndMs = state.filter.installTimeRange?.end?.toEpochMilli(),
            onConfirm = { start, end ->
                onAction(FilterAction.InstallTimeRangeChanged(DateRange(Instant.ofEpochMilli(start), Instant.ofEpochMilli(end))))
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }

    if (state.showUnsavedChangesSheet) {
        UnsavedChangesBottomSheet(
            onSave = { onAction(FilterAction.SaveAndClose) },
            onDiscard = { onAction(FilterAction.DiscardChanges) },
            onDismiss = { onAction(FilterAction.DismissUnsavedChangesSheet) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SourceSection(
    selectedSources: ImmutableSet<AppSource>,
    onSourceToggle: (AppSource, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterSection(
        title = stringResource(R.string.filter_source_section),
        modifier = modifier,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppSource.entries.forEach { source ->
                val isSelected = source in selectedSources
                Chip(
                    label = source.displayName(),
                    selected = isSelected,
                    onClick = { onSourceToggle(source, !isSelected) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SdkVersionSection(
    availableSdkVersions: ImmutableList<Int>,
    selectedSdkVersions: ImmutableSet<Int>,
    onSdkVersionToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (availableSdkVersions.isEmpty()) return

    FilterSection(
        title = stringResource(R.string.filter_sdk_section),
        modifier = modifier,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            availableSdkVersions.forEach { sdk ->
                Chip(
                    label = sdk.toAndroidVersionLabel(),
                    selected = sdk in selectedSdkVersions,
                    onClick = { onSdkVersionToggle(sdk) },
                )
            }
        }
    }
}

@Composable
private fun ApkSizeSection(
    sectionState: ApkSizeSectionState,
    selectedRange: AppSizeRange?,
    onRangeChange: (AppSizeRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterSection(
        title = stringResource(R.string.filter_apk_size_section),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.filter_apk_size_description),
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        when (sectionState) {
            ApkSizeSectionState.Loading -> LoadingSpinner(
                size = 24.dp,
                strokeWidth = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            )

            is ApkSizeSectionState.RangeAvailable -> {
                val effectiveRange = selectedRange ?: sectionState.bounds
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = effectiveRange.min.formatted(),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    RangeSlider(
                        value = effectiveRange.min.megabytes.toFloat()..effectiveRange.max.megabytes.toFloat(),
                        onValueChange = { floatRange ->
                            onRangeChange(
                                AppSizeRange(
                                    min = floatRange.start.megabytes,
                                    max = floatRange.endInclusive.megabytes,
                                ),
                            )
                        },
                        valueRange = sectionState.bounds.min.megabytes.toFloat()..sectionState.bounds.max.megabytes.toFloat(),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = effectiveRange.max.formatted(),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TotalSizeSection(
    sectionState: TotalSizeSectionState,
    selectedRange: AppSizeRange?,
    onRangeChange: (AppSizeRange) -> Unit,
    onGrantAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterSection(
        title = stringResource(R.string.filter_total_size_section),
        modifier = modifier,
    ) {
        when (sectionState) {
            TotalSizeSectionState.PermissionMissing -> TotalSizeLockedContent(onGrantAccess = onGrantAccess)

            TotalSizeSectionState.Loading -> LoadingSpinner(
                size = 24.dp,
                strokeWidth = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            )

            is TotalSizeSectionState.RangeAvailable -> {
                Text(
                    text = stringResource(R.string.filter_total_size_description),
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                val effectiveRange = selectedRange ?: sectionState.bounds
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = effectiveRange.min.formatted(),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    RangeSlider(
                        value = effectiveRange.min.megabytes.toFloat()..effectiveRange.max.megabytes.toFloat(),
                        onValueChange = { floatRange ->
                            onRangeChange(
                                AppSizeRange(
                                    min = floatRange.start.megabytes,
                                    max = floatRange.endInclusive.megabytes,
                                ),
                            )
                        },
                        valueRange = sectionState.bounds.min.megabytes.toFloat()..sectionState.bounds.max.megabytes.toFloat(),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = effectiveRange.max.formatted(),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun InstallTimeSection(
    installTimeRange: DateRange?,
    onSelectDateRange: () -> Unit,
    onClearDateRange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterSection(
        title = stringResource(R.string.filter_install_time_section),
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val label = if (installTimeRange != null) {
                stringResource(
                    R.string.filter_date_range,
                    installTimeRange.start.toShortDate(),
                    installTimeRange.end.toShortDate(),
                )
            } else {
                stringResource(R.string.filter_date_any)
            }

            Chip(
                label = label,
                selected = installTimeRange != null,
                leadingIcon = ApkAnalyzerIcons.Calendar,
                onClick = onSelectDateRange,
            )

            if (installTimeRange != null) {
                val clearDescription = stringResource(R.string.filter_date_clear)
                Chip(
                    label = "",
                    leadingIcon = ApkAnalyzerIcons.Clear,
                    onClick = onClearDateRange,
                    modifier = Modifier.semantics { contentDescription = clearDescription },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UnusedAppsSection(
    sectionState: UnusedAppsSectionState,
    selectedPeriod: UnusedAppsPeriod?,
    onPeriodSelect: (UnusedAppsPeriod) -> Unit,
    onGrantAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterSection(
        title = stringResource(R.string.filter_unused_section),
        modifier = modifier,
    ) {
        when (sectionState) {
            UnusedAppsSectionState.PermissionMissing -> UnusedAppsLockedContent(onGrantAccess = onGrantAccess)

            UnusedAppsSectionState.Loading -> LoadingSpinner(
                size = 24.dp,
                strokeWidth = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            )

            UnusedAppsSectionState.Available -> FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                UnusedAppsPeriod.entries.forEach { period ->
                    Chip(
                        label = period.label(),
                        selected = selectedPeriod == period,
                        onClick = { onPeriodSelect(period) },
                    )
                }
            }
        }
    }
}

@Composable
private fun UnusedAppsLockedContent(onGrantAccess: () -> Unit, modifier: Modifier = Modifier) {
    LockedContent(
        description = stringResource(R.string.filter_unused_locked_description),
        grantAccessLabel = stringResource(R.string.filter_unused_grant_access),
        onGrantAccess = onGrantAccess,
        modifier = modifier,
    )
}

@Composable
private fun TotalSizeLockedContent(onGrantAccess: () -> Unit, modifier: Modifier = Modifier) {
    LockedContent(
        description = stringResource(R.string.filter_total_size_locked_description),
        grantAccessLabel = stringResource(R.string.filter_total_size_grant_access),
        onGrantAccess = onGrantAccess,
        modifier = modifier,
    )
}

@Composable
private fun LockedContent(
    description: String,
    grantAccessLabel: String,
    onGrantAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = ApkAnalyzerIcons.Lock,
                tint = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = description,
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant,
            )
        }
        Chip(
            label = grantAccessLabel,
            onClick = onGrantAccess,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PermissionsSection(
    activePermissionPresets: ImmutableList<PermissionPreset>,
    extraPermissionCount: Int,
    onPresetToggle: (PermissionPreset) -> Unit,
    onOpenPermissionFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.filter_permissions_section),
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .card()
                .clickable(onClick = onOpenPermissionFilter)
                .padding(16.dp),
        ) {
            PermissionPreset.all.forEach { preset ->
                val isSelected = preset in activePermissionPresets
                Logger.e("XXX", "preset $preset")
                Chip(
                    label = preset.displayLabel(),
                    selected = isSelected,
                    onClick = { onPresetToggle(preset) },
                )
            }
            if (extraPermissionCount > 0) {
                Chip(
                    label = stringResource(R.string.filter_permissions_count, extraPermissionCount),
                    selected = true,
                    onClick = onOpenPermissionFilter,
                )
            } else {
                Chip(
                    label = stringResource(R.string.filter_permissions_more),
                    leadingIcon = ApkAnalyzerIcons.Search,
                    onClick = onOpenPermissionFilter,
                )
            }
        }
    }
}

@Composable
private fun PermissionPreset.displayLabel(): String = when (this) {
    PermissionPreset.Sensitive -> stringResource(R.string.filter_permissions_preset_sensitive)
    PermissionPreset.Camera -> stringResource(R.string.filter_permissions_preset_camera)
    PermissionPreset.Microphone -> stringResource(R.string.filter_permissions_preset_microphone)
    PermissionPreset.Location -> stringResource(R.string.filter_permissions_preset_location)
    PermissionPreset.Contacts -> stringResource(R.string.filter_permissions_preset_contacts)
    PermissionPreset.Sms -> stringResource(R.string.filter_permissions_preset_sms)
    PermissionPreset.Storage -> stringResource(R.string.filter_permissions_preset_storage)
}

@Composable
private fun FilterSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .card()
                .padding(16.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun UnsavedChangesBottomSheet(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit,
) {
    BottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.filter_unsaved_title),
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.onSurface,
            )
            Text(
                text = stringResource(R.string.filter_unsaved_subtitle),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                text = stringResource(R.string.filter_unsaved_save),
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(
                text = stringResource(R.string.filter_unsaved_discard),
                onClick = onDiscard,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AppSource.displayName(): String = when (this) {
    AppSource.GooglePlay -> stringResource(R.string.filter_source_google_play)
    AppSource.SystemPreinstalled -> stringResource(R.string.filter_source_system)
    AppSource.Unknown -> stringResource(R.string.filter_source_unknown)
}

@Composable
private fun UnusedAppsPeriod.label(): String = when (this) {
    UnusedAppsPeriod.ONE_MONTH -> stringResource(R.string.filter_unused_months_count, 1)
    UnusedAppsPeriod.TWO_MONTHS -> stringResource(R.string.filter_unused_months_count, 2)
    UnusedAppsPeriod.THREE_MONTHS -> stringResource(R.string.filter_unused_months_count, 3)
    UnusedAppsPeriod.SIX_MONTHS -> stringResource(R.string.filter_unused_months_count, 6)
    UnusedAppsPeriod.ONE_YEAR -> stringResource(R.string.filter_unused_year)
}

private fun Int.toAndroidVersionLabel(): String {
    val name = when (this) {
        21 -> "Android 5"
        22 -> "Android 5.1"
        23 -> "Android 6"
        24 -> "Android 7"
        25 -> "Android 7.1"
        26 -> "Android 8"
        27 -> "Android 8.1"
        28 -> "Android 9"
        29 -> "Android 10"
        30 -> "Android 11"
        31 -> "Android 12"
        32 -> "Android 12L"
        33 -> "Android 13"
        34 -> "Android 14"
        35 -> "Android 15"
        36 -> "Android 16"
        else -> "API $this"
    }
    return "$name (API $this)"
}

private fun Instant.toShortDate(): String = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(Date.from(this))

@Preview
@Composable
private fun FilterContentDefaultPreview() {
    ApkAnalyzerTheme {
        FilterContent(
            state = FilterState(
                filter = AppFilterState(),
                apkSizeSectionState = ApkSizeSectionState.Loading,
                totalSizeSectionState = TotalSizeSectionState.PermissionMissing,
                unusedAppsSectionState = UnusedAppsSectionState.PermissionMissing,
                availableSdkVersions = persistentListOf(35, 34, 33, 31, 30, 29, 28),
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun FilterContentPreview() {
    ApkAnalyzerTheme {
        FilterContent(
            state = FilterState(
                filter = AppFilterState(
                    selectedSources = persistentSetOf(AppSource.GooglePlay),
                    selectedSdkVersions = persistentSetOf(34, 35),
                    apkSizeRange = AppSizeRange(10.megabytes, 200.megabytes),
                    installTimeRange = DateRange(
                        start = Instant.ofEpochMilli(1_700_000_000_000L),
                        end = Instant.ofEpochMilli(1_720_000_000_000L),
                    ),
                ),
                apkSizeSectionState = ApkSizeSectionState.RangeAvailable(AppSizeRange(1.megabytes, 512.megabytes)),
                totalSizeSectionState = TotalSizeSectionState.RangeAvailable(AppSizeRange(1.megabytes, 2048.megabytes)),
                unusedAppsSectionState = UnusedAppsSectionState.Available,
                availableSdkVersions = persistentListOf(35, 34, 33, 31, 30, 29, 28),
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun FilterContentWithPermissionsPreview() {
    ApkAnalyzerTheme {
        FilterContent(
            state = FilterState(
                filter = AppFilterState(),
                apkSizeSectionState = ApkSizeSectionState.Loading,
                totalSizeSectionState = TotalSizeSectionState.PermissionMissing,
                unusedAppsSectionState = UnusedAppsSectionState.PermissionMissing,
                availableSdkVersions = persistentListOf(35, 34),
                activePermissionPresets = persistentListOf(
                    PermissionPreset.Camera,
                    PermissionPreset.Location,
                ),
                extraPermissionCount = 2,
            ),
            onAction = {},
        )
    }
}
