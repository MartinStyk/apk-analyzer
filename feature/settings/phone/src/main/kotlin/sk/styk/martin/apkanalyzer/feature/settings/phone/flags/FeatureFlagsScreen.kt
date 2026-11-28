package sk.styk.martin.apkanalyzer.feature.settings.phone.flags

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Button
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.InfoCard
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.InfoCardTone
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.LoadingSpinner
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Switch
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.TextButton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Toolbar
import sk.styk.martin.apkanalyzer.core.designsystem.phone.modifier.card
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlag
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagOverride
import sk.styk.martin.apkanalyzer.core.featureflags.api.FeatureFlagStatus
import sk.styk.martin.apkanalyzer.feature.settings.phone.R

@Composable
internal fun FeatureFlagsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeatureFlagsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(onBack) {
        viewModel.events.collect { event ->
            when (event) {
                FeatureFlagsEvent.NavigateBack -> onBack()
            }
        }
    }
    FeatureFlagsContent(state = state, onAction = viewModel::onAction, modifier = modifier)
}

@Composable
private fun FeatureFlagsContent(
    state: FeatureFlagsState,
    onAction: (FeatureFlagsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(AppTheme.colors.background)) {
        Toolbar(
            title = stringResource(R.string.feature_flags_title),
            onBack = { onAction(FeatureFlagsAction.NavigateBack) },
        )
        when (state) {
            FeatureFlagsState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingSpinner()
            }

            FeatureFlagsState.Error -> Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                InfoCard(
                    icon = ApkAnalyzerIcons.Info,
                    title = null,
                    body = stringResource(R.string.feature_flags_load_failed),
                    tone = InfoCardTone.Warning,
                )
                Button(text = stringResource(R.string.feature_flags_retry), onClick = { onAction(FeatureFlagsAction.Retry) })
            }

            is FeatureFlagsState.Content -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            ) {
                item {
                    InfoCard(
                        icon = ApkAnalyzerIcons.Info,
                        title = null,
                        body = stringResource(R.string.feature_flags_explanation),
                    )
                }
                items(state.flags, key = { it.flag }) { status ->
                    FeatureFlagRow(status, !state.isSaving, onAction)
                }
                item {
                    TextButton(
                        text = stringResource(R.string.feature_flags_reset_all),
                        onClick = { onAction(FeatureFlagsAction.ResetAll) },
                        enabled = !state.isSaving && state.flags.any { it.override != FeatureFlagOverride.Default },
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureFlagRow(
    status: FeatureFlagStatus,
    enabled: Boolean,
    onAction: (FeatureFlagsAction) -> Unit,
) {
    val label = stringResource(
        when (status.flag) {
            FeatureFlag.InventoryExport -> R.string.feature_flags_inventory_export
            FeatureFlag.WhatChangedTab -> R.string.feature_flags_what_changed_tab
            FeatureFlag.AppDetailHistory -> R.string.feature_flags_history_display
            FeatureFlag.AppHistoryRecording -> R.string.feature_flags_history_recording
        },
    )
    Column(
        modifier = Modifier.fillMaxWidth().card().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.onSurface,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            Switch(
                checked = status.isEnabled,
                onCheckedChange = { checked ->
                    onAction(
                        FeatureFlagsAction.SetOverride(
                            status.flag,
                            if (checked) FeatureFlagOverride.Enabled else FeatureFlagOverride.Disabled,
                        ),
                    )
                },
                enabled = enabled,
                modifier = Modifier.semantics { contentDescription = label },
            )
        }
        Text(
            text = if (status.override == FeatureFlagOverride.Default) {
                stringResource(
                    if (status.defaultEnabled) R.string.feature_flags_default_on else R.string.feature_flags_default_off,
                )
            } else {
                stringResource(
                    if (status.isEnabled) R.string.feature_flags_override_on else R.string.feature_flags_override_off,
                )
            },
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.onSurfaceVariant,
        )
        TextButton(
            text = stringResource(R.string.feature_flags_reset),
            onClick = { onAction(FeatureFlagsAction.SetOverride(status.flag, FeatureFlagOverride.Default)) },
            enabled = enabled && status.override != FeatureFlagOverride.Default,
        )
    }
}

@Preview
@Composable
private fun FeatureFlagsContentPreview() {
    ApkAnalyzerTheme {
        FeatureFlagsContent(
            state = FeatureFlagsState.Content(
                listOf(
                    FeatureFlagStatus(FeatureFlag.InventoryExport, FeatureFlagOverride.Default, true),
                    FeatureFlagStatus(FeatureFlag.WhatChangedTab, FeatureFlagOverride.Default, true),
                    FeatureFlagStatus(FeatureFlag.AppDetailHistory, FeatureFlagOverride.Enabled, true),
                    FeatureFlagStatus(FeatureFlag.AppHistoryRecording, FeatureFlagOverride.Disabled, true),
                ),
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun FeatureFlagsErrorPreview() {
    ApkAnalyzerTheme {
        FeatureFlagsContent(state = FeatureFlagsState.Error, onAction = {})
    }
}
