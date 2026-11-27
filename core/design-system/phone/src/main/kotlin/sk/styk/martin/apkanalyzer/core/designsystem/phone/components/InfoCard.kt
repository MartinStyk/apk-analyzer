package sk.styk.martin.apkanalyzer.core.designsystem.phone.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.Shapes

@Composable
fun InfoCard(
    icon: ImageVector,
    title: String?,
    body: String,
    modifier: Modifier = Modifier,
    tone: InfoCardTone = InfoCardTone.Info,
    action: (@Composable () -> Unit)? = null,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(tone.containerColor, Shapes.CardShape)
            .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = if (action == null) 12.dp else 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                tint = tone.iconColor,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f).padding(end = 6.dp),
            ) {
                title?.let {
                    Text(
                        text = it,
                        style = AppTheme.typography.titleSmall,
                        color = tone.contentColor,
                    )
                }
                Text(
                    text = body,
                    style = if (title == null) AppTheme.typography.bodyMedium else AppTheme.typography.bodySmall,
                    color = tone.contentColor,
                )
            }
            trailingAction?.invoke()
        }
        action?.let {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth(),
            ) {
                it()
            }
        }
    }
}

private val InfoCardTone.containerColor: Color
    @Composable
    get() = when (this) {
        InfoCardTone.Info -> AppTheme.colors.secondaryContainer
        InfoCardTone.Attention -> AppTheme.colors.tertiaryContainer
        InfoCardTone.Warning -> AppTheme.colors.warningContainer
    }

private val InfoCardTone.contentColor: Color
    @Composable
    get() = when (this) {
        InfoCardTone.Info -> AppTheme.colors.onSecondaryContainer
        InfoCardTone.Attention -> AppTheme.colors.onTertiaryContainer
        InfoCardTone.Warning -> AppTheme.colors.onSurface
    }

private val InfoCardTone.iconColor: Color
    @Composable
    get() = when (this) {
        InfoCardTone.Warning -> AppTheme.colors.warning
        InfoCardTone.Info, InfoCardTone.Attention -> contentColor
    }

@Preview
@Composable
private fun InfoCardPreview() {
    ApkAnalyzerTheme {
        InfoCard(
            icon = ApkAnalyzerIcons.Info,
            title = "Changes between these versions",
            body = "Updated Nov 19, 11:30",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview
@Composable
private fun InfoCardWithActionPreview() {
    ApkAnalyzerTheme {
        InfoCard(
            icon = ApkAnalyzerIcons.Export,
            title = "Export your app inventory",
            body = "Pick the apps and details to include, then save them as a spreadsheet.",
            modifier = Modifier.padding(16.dp),
            action = { TextButton(text = "Got it", onClick = {}) },
        )
    }
}

@Preview
@Composable
private fun InfoCardAttentionPreview() {
    ApkAnalyzerTheme {
        InfoCard(
            icon = ApkAnalyzerIcons.Camera,
            title = "Camera permission added",
            body = "Needs your explicit approval before the app can use it.",
            tone = InfoCardTone.Attention,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview
@Composable
private fun InfoCardWarningPreview() {
    ApkAnalyzerTheme {
        InfoCard(
            icon = ApkAnalyzerIcons.Warning,
            title = null,
            body = "This device is missing 2 required features.",
            tone = InfoCardTone.Warning,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview
@Composable
private fun InfoCardDarkPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        InfoCard(
            icon = ApkAnalyzerIcons.Warning,
            title = "Signing certificate changed",
            body = "Android only accepts an update with a different certificate when the old key approved it.",
            tone = InfoCardTone.Warning,
            modifier = Modifier.padding(16.dp),
        )
    }
}
