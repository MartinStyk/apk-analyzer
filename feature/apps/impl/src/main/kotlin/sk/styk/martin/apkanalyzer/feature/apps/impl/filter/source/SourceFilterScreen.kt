package sk.styk.martin.apkanalyzer.feature.apps.impl.filter.source

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Checkbox
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.components.TextButton
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Toolbar
import sk.styk.martin.apkanalyzer.core.uilibrary.lazylist.ListItemPosition
import sk.styk.martin.apkanalyzer.core.uilibrary.lazylist.itemsPositioned
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.apps.impl.R

@Composable
internal fun SourceFilterScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SourceFilterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SourceFilterEvent.NavigateBack -> onBack()
            }
        }
    }

    BackHandler {
        viewModel.onAction(SourceFilterAction.NavigateBack)
    }

    SourceFilterContent(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
private fun SourceFilterContent(
    state: SourceFilterState,
    onAction: (SourceFilterAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        Toolbar(
            title = stringResource(R.string.filter_source_section),
            onBack = { onAction(SourceFilterAction.NavigateBack) },
            actions = {
                TextButton(
                    text = stringResource(R.string.filter_reset),
                    onClick = { onAction(SourceFilterAction.Reset) },
                )
            },
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsPositioned(
                items = state.options,
                key = { _, option -> option.source.name },
            ) { position, option ->
                SourceOptionRow(
                    option = option,
                    position = position,
                    onToggle = { onAction(SourceFilterAction.SourceToggled(option.source)) },
                )
            }
        }
    }
}

@Composable
private fun SourceOptionRow(
    option: SourceOption,
    position: ListItemPosition,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape: Shape = when (position) {
        ListItemPosition.Single -> Shapes.CardShape
        ListItemPosition.First -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ListItemPosition.Middle -> RectangleShape
        ListItemPosition.Last -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.surface, shape)
            .padding(2.dp)
            .clip(Shapes.CardShape)
            .clickable(onClick = onToggle)
            .padding(start = 4.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Checkbox(checked = option.isSelected, onCheckedChange = { onToggle() })
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = option.source.displayName(),
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurface,
        )
    }
}

@Composable
private fun AppSource.displayName(): String = when (this) {
    AppSource.GooglePlay -> stringResource(R.string.filter_source_google_play)
    AppSource.SamsungGalaxyStore -> stringResource(R.string.filter_source_samsung_galaxy_store)
    AppSource.AmazonAppstore -> stringResource(R.string.filter_source_amazon_appstore)
    AppSource.HuaweiAppGallery -> stringResource(R.string.filter_source_huawei_app_gallery)
    AppSource.XiaomiGetApps -> stringResource(R.string.filter_source_xiaomi_get_apps)
    AppSource.FDroid -> stringResource(R.string.filter_source_fdroid)
    AppSource.AuroraStore -> stringResource(R.string.filter_source_aurora_store)
    AppSource.Sideloaded -> stringResource(R.string.filter_source_sideloaded)
    AppSource.LocalInstall -> stringResource(R.string.filter_source_local_install)
    AppSource.SystemPreinstalled -> stringResource(R.string.filter_source_system)
    AppSource.Unknown -> stringResource(R.string.filter_source_unknown)
}

@Preview
@Composable
private fun SourceFilterContentPreview() {
    ApkAnalyzerTheme {
        SourceFilterContent(
            state = SourceFilterState(
                options = persistentListOf(
                    SourceOption(AppSource.GooglePlay, isSelected = true),
                    SourceOption(AppSource.SystemPreinstalled, isSelected = false),
                    SourceOption(AppSource.Sideloaded, isSelected = false),
                ),
            ),
            onAction = {},
        )
    }
}
