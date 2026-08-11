package sk.styk.martin.apkanalyzer.feature.appdetail.impl.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import sk.styk.martin.apkanalyzer.core.appaidescription.AppAiDescription
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Icon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
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
    val description by viewModel.state.collectAsStateWithLifecycle()
    description?.let {
        AiSummaryCardContent(description = it, modifier = modifier)
    }
}

@Composable
private fun AiSummaryCardContent(description: AppAiDescription, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.CardShape)
            .background(AppTheme.colors.aiAccentContainer, Shapes.CardShape)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.app_detail_ai_summary_title),
                style = AppTheme.typography.titleLarge,
                color = AppTheme.colors.aiAccent,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = ApkAnalyzerIcons.AiSparkle,
                tint = AppTheme.colors.aiAccent,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description.shortDescription,
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description.longDescription,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun AiSummaryCardContentPreview() {
    ApkAnalyzerTheme {
        AiSummaryCardContent(
            description = AppAiDescription(
                shortDescription = "Music and podcast streaming app for discovering and listening to audio content.",
                longDescription = "Spotify is a music and podcast streaming application that lets users discover and " +
                    "listen to audio content. It provides personalized recommendations and playlists alongside access " +
                    "to music and podcasts.",
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
