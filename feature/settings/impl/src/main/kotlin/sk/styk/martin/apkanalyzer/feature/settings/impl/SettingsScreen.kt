package sk.styk.martin.apkanalyzer.feature.settings.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import sk.styk.martin.apkanalyzer.core.common.settings.ColorAppScheme
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Chip
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Switch
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Toolbar
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.card
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.sharedBounds
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.NavigateBack -> onBack()
            }
        }
    }

    SettingsContent(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
private fun SettingsContent(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .sharedBounds("settings")
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        Toolbar(
            title = stringResource(R.string.settings_title),
            onBack = { onAction(SettingsAction.NavigateBack) },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppearanceSection(
                colorScheme = state.colorScheme,
                onColorSchemeSelect = { onAction(SettingsAction.ColorSchemeSelected(it)) },
            )

            AppsSection(
                recentlyViewedAppsEnabled = state.recentlyViewedAppsEnabled,
                onRecentlyViewedAppsToggle = { onAction(SettingsAction.RecentlyViewedAppsToggled(it)) },
            )
        }
    }
}

@Composable
private fun AppearanceSection(
    colorScheme: ColorAppScheme,
    onColorSchemeSelect: (ColorAppScheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.settings_appearance),
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .card()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_color_scheme),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ColorAppScheme.entries.forEach { scheme ->
                    Chip(
                        label = scheme.displayName(),
                        selected = colorScheme == scheme,
                        onClick = { onColorSchemeSelect(scheme) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppsSection(
    recentlyViewedAppsEnabled: Boolean,
    onRecentlyViewedAppsToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.settings_apps_section),
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .card()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            val recentlyViewedLabel = stringResource(R.string.settings_recently_viewed_toggle)
            Text(
                text = recentlyViewedLabel,
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurface,
            )
            Switch(
                checked = recentlyViewedAppsEnabled,
                onCheckedChange = onRecentlyViewedAppsToggle,
                modifier = Modifier.semantics { contentDescription = recentlyViewedLabel },
            )
        }
    }
}

@Composable
private fun ColorAppScheme.displayName(): String = when (this) {
    ColorAppScheme.Day -> stringResource(R.string.settings_color_scheme_day)
    ColorAppScheme.Night -> stringResource(R.string.settings_color_scheme_night)
    ColorAppScheme.FollowSystem -> stringResource(R.string.settings_color_scheme_follow_system)
}

@Preview
@Composable
private fun SettingsContentPreview() {
    ApkAnalyzerTheme {
        SettingsContent(
            state = SettingsState(
                colorScheme = ColorAppScheme.FollowSystem,
                recentlyViewedAppsEnabled = true,
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun SettingsContentDayRecentlyDisabledPreview() {
    ApkAnalyzerTheme {
        SettingsContent(
            state = SettingsState(
                colorScheme = ColorAppScheme.Day,
                recentlyViewedAppsEnabled = false,
            ),
            onAction = {},
        )
    }
}
