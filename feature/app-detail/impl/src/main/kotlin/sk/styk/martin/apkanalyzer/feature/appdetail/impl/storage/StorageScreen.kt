package sk.styk.martin.apkanalyzer.feature.appdetail.impl.storage

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import sk.styk.martin.apkanalyzer.core.apps.storagestats.StorageBreakdown
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import sk.styk.martin.apkanalyzer.core.uilibrary.components.BarChartSegment
import sk.styk.martin.apkanalyzer.core.uilibrary.components.PermissionRationaleBottomSheet
import sk.styk.martin.apkanalyzer.core.uilibrary.components.SegmentedBarChart
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Toolbar
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.R
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.InfoRow
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.RationaleBottomSheet
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.SectionError
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.SectionLoading
import kotlin.math.roundToInt

@Composable
internal fun StorageScreen(
    appDetailInput: AppDetailInput,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StorageViewModel = hiltViewModel { factory: StorageViewModel.Factory ->
        factory.create(appDetailInput)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                StorageEvent.NavigateBack -> onBack()
                StorageEvent.ShowCopiedFeedback -> Toast.makeText(context, R.string.general_info_copied, Toast.LENGTH_SHORT).show()
                StorageEvent.OpenUsagePermissionSettings -> context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        }
    }

    StorageContent(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
private fun StorageContent(
    state: StorageState,
    onAction: (StorageAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        Toolbar(
            title = stringResource(R.string.storage_title),
            onBack = { onAction(StorageAction.Back) },
        )
        when (state) {
            StorageState.Loading -> SectionLoading()

            StorageState.Error -> SectionError(
                message = stringResource(R.string.storage_error),
                onRetry = { onAction(StorageAction.Retry) },
            )

            StorageState.MissingPermission -> PermissionRationaleBottomSheet(
                title = stringResource(R.string.storage_permission_title),
                description = stringResource(R.string.storage_permission_description),
                openSettingsLabel = stringResource(R.string.storage_permission_open_settings),
                onOpenSettings = { onAction(StorageAction.OpenPermissionSettings) },
                onDismiss = { onAction(StorageAction.Back) },
            )

            is StorageState.Loaded -> StorageLoadedContent(breakdown = state.breakdown, onAction = onAction)
        }
    }
}

@Composable
private fun StorageLoadedContent(
    breakdown: StorageBreakdown,
    onAction: (StorageAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var rationaleRow by remember { mutableStateOf<InfoRow?>(null) }
    val appLabel = stringResource(R.string.storage_legend_app)
    val dataLabel = stringResource(R.string.storage_legend_data)
    val cacheLabel = stringResource(R.string.storage_legend_cache)
    val appRationale = stringResource(R.string.storage_rationale_app)
    val dataRationale = stringResource(R.string.storage_rationale_data)
    val cacheRationale = stringResource(R.string.storage_rationale_cache)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text(
            text = breakdown.total.formatted(),
            style = AppTheme.typography.headlineSmall,
            color = AppTheme.colors.onBackground,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.storage_explanation),
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(20.dp))
        SegmentedBarChart(
            segments = persistentListOf(
                BarChartSegment(value = breakdown.appBytes.bytes.toFloat(), color = AppTheme.colors.primary),
                BarChartSegment(value = breakdown.dataBytes.bytes.toFloat(), color = AppTheme.colors.secondary),
                BarChartSegment(value = breakdown.cacheBytes.bytes.toFloat(), color = AppTheme.colors.tertiary),
            ),
        )
        Spacer(modifier = Modifier.height(20.dp))
        StorageLegendRow(
            color = AppTheme.colors.primary,
            label = appLabel,
            size = breakdown.appBytes,
            total = breakdown.total,
            onShowRationale = { rationaleRow = InfoRow(appLabel, breakdown.appBytes.formatted(), appRationale) },
            onCopy = { label, value -> onAction(StorageAction.CopyValue(label, value)) },
        )
        StorageLegendRow(
            color = AppTheme.colors.secondary,
            label = dataLabel,
            size = breakdown.dataBytes,
            total = breakdown.total,
            onShowRationale = { rationaleRow = InfoRow(dataLabel, breakdown.dataBytes.formatted(), dataRationale) },
            onCopy = { label, value -> onAction(StorageAction.CopyValue(label, value)) },
        )
        StorageLegendRow(
            color = AppTheme.colors.tertiary,
            label = cacheLabel,
            size = breakdown.cacheBytes,
            total = breakdown.total,
            onShowRationale = { rationaleRow = InfoRow(cacheLabel, breakdown.cacheBytes.formatted(), cacheRationale) },
            onCopy = { label, value -> onAction(StorageAction.CopyValue(label, value)) },
        )
    }

    rationaleRow?.let { row ->
        RationaleBottomSheet(row = row, onDismiss = { rationaleRow = null })
    }
}

@Composable
private fun StorageLegendRow(
    color: Color,
    label: String,
    size: AppSize,
    total: AppSize,
    onShowRationale: () -> Unit,
    onCopy: (label: String, value: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val percent = if (total.bytes > 0) (size.bytes.toFloat() / total.bytes.toFloat() * 100).roundToInt() else 0
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.CardShape)
            .combinedClickable(
                onClick = onShowRationale,
                onLongClick = { onCopy(label, size.formatted()) },
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = AppTheme.typography.titleSmall,
            color = AppTheme.colors.onBackground,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.storage_legend_value, size.formatted(), percent),
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun StorageLoadingPreview() {
    ApkAnalyzerTheme {
        StorageContent(state = StorageState.Loading, onAction = {})
    }
}

@Preview
@Composable
private fun StorageErrorPreview() {
    ApkAnalyzerTheme {
        StorageContent(state = StorageState.Error, onAction = {})
    }
}

@Preview
@Composable
private fun StorageMissingPermissionPreview() {
    ApkAnalyzerTheme {
        StorageContent(state = StorageState.MissingPermission, onAction = {})
    }
}

@Preview
@Composable
private fun StorageLoadedPreview() {
    ApkAnalyzerTheme {
        StorageContent(
            state = StorageState.Loaded(
                breakdown = StorageBreakdown(
                    appBytes = 620.megabytes,
                    dataBytes = 240.megabytes,
                    cacheBytes = 90.megabytes,
                ),
            ),
            onAction = {},
        )
    }
}
