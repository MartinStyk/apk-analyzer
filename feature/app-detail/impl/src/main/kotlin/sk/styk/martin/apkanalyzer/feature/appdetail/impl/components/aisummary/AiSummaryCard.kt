package sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.aisummary

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.AppAiDescription
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Icon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.LoadingSpinner
import sk.styk.martin.apkanalyzer.core.uilibrary.components.SkeletonBox
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.components.TextButton
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.R

@Composable
internal fun AiSummaryCard(
    reference: AppReference,
    modifier: Modifier = Modifier,
    viewModel: AiSummaryViewModel = hiltViewModel { factory: AiSummaryViewModel.Factory ->
        factory.create(reference)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AiSummaryCardContent(
        state = state,
        onDownloadClick = { viewModel.onAction(AiSummaryAction.DownloadModel) },
        modifier = modifier,
    )
}

@Composable
private fun AiSummaryCardContent(
    state: AiSummaryState,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state is AiSummaryState.Hidden) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.CardShape)
            .background(AppTheme.colors.aiAccentContainer, Shapes.CardShape)
            .animateContentSize()
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.app_detail_ai_summary_title),
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.aiAccent,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = ApkAnalyzerIcons.AiSparkle,
                tint = AppTheme.colors.aiAccent,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        when (state) {
            is AiSummaryState.Loaded -> Text(
                text = state.description.description,
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
            )

            AiSummaryState.Loading -> AiSummaryLoadingLines()

            AiSummaryState.Downloadable -> TextButton(
                text = stringResource(R.string.app_detail_ai_summary_download_action),
                onClick = onDownloadClick,
            )

            AiSummaryState.Downloading -> AiSummaryDownloadingRow()

            AiSummaryState.Hidden -> Unit
        }
    }
}

@Composable
private fun AiSummaryLoadingLines(modifier: Modifier = Modifier) {
    val color = AppTheme.colors.aiAccent.copy(alpha = 0.15f)
    val highlightColor = AppTheme.colors.aiAccent.copy(alpha = 0.35f)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SkeletonBox(modifier = Modifier.fillMaxWidth().height(14.dp), color = color, highlightColor = highlightColor)
        SkeletonBox(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp), color = color, highlightColor = highlightColor)
    }
}

@Composable
private fun AiSummaryDownloadingRow(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        LoadingSpinner(size = 16.dp, strokeWidth = 2.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.app_detail_ai_summary_downloading),
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun AiSummaryCardContentLoadedPreview() {
    ApkAnalyzerTheme {
        AiSummaryCardContent(
            state = AiSummaryState.Loaded(
                AppAiDescription(
                    description = "Lets users discover and listen to music and podcasts, with personalized " +
                        "recommendations and offline playback for saved content.",
                ),
            ),
            onDownloadClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview
@Composable
private fun AiSummaryCardContentLoadingPreview() {
    ApkAnalyzerTheme {
        AiSummaryCardContent(
            state = AiSummaryState.Loading,
            onDownloadClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview
@Composable
private fun AiSummaryCardContentDownloadablePreview() {
    ApkAnalyzerTheme {
        AiSummaryCardContent(
            state = AiSummaryState.Downloadable,
            onDownloadClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview
@Composable
private fun AiSummaryCardContentDownloadingPreview() {
    ApkAnalyzerTheme {
        AiSummaryCardContent(
            state = AiSummaryState.Downloading,
            onDownloadClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
