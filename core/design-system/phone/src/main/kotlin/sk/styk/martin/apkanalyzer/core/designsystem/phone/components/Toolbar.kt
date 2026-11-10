package sk.styk.martin.apkanalyzer.core.designsystem.phone.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.R
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme

@Composable
fun Toolbar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.background)
            .padding(start = 4.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
    ) {
        IconButton(
            imageVector = ApkAnalyzerIcons.Back,
            onClick = onBack,
            contentDescription = stringResource(R.string.content_description_back),
        )
        if (subtitle == null) {
            Text(
                text = title,
                style = AppTheme.typography.titleLarge,
                color = AppTheme.colors.onBackground,
                modifier = Modifier.weight(1f),
            )
        } else {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTheme.typography.titleLarge,
                    color = AppTheme.colors.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        actions()
    }
}

@Preview
@Composable
private fun ToolbarSimplePreview() {
    ApkAnalyzerTheme {
        Toolbar(
            title = "Filter",
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun ToolbarWithSubtitlePreview() {
    ApkAnalyzerTheme {
        Toolbar(
            title = "Spotify",
            subtitle = "History",
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun ToolbarWithActionPreview() {
    ApkAnalyzerTheme {
        Toolbar(
            title = "Filter",
            onBack = {},
            actions = {
                TextButton(text = "Reset", onClick = {})
            },
        )
    }
}
