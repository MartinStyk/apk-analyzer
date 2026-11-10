package sk.styk.martin.apkanalyzer.feature.appdetail.tv.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.CodeBox
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.IconBadge
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.ScrollablePanel
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.Tag
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme

@Composable
internal fun DetailPane(
    title: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    explanation: String? = null,
    icon: ImageVector? = null,
    status: String? = null,
    highlighted: Boolean = false,
    isCodeValue: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PaneHeader(title = title, icon = icon, status = status, highlighted = highlighted)
        if (value != null) {
            if (isCodeValue) {
                CodeBox(text = value)
            } else {
                Text(text = value, style = AppTheme.typography.titleMedium)
            }
        }
        if (explanation != null) {
            Text(text = explanation, style = AppTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceVariant)
        }
        content()
    }
}

@Composable
private fun PaneHeader(
    title: String,
    icon: ImageVector?,
    status: String?,
    highlighted: Boolean,
) {
    val accent = if (highlighted) AppTheme.colors.warning else AppTheme.colors.primary
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        if (icon != null) {
            IconBadge(icon = icon, tint = accent, size = 40.dp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = title, style = AppTheme.typography.titleMedium, color = accent)
            if (status != null) {
                Tag(label = status)
            }
        }
    }
}

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun DetailPanePreview() {
    ApkAnalyzerTvTheme {
        ScrollablePanel(modifier = Modifier.width(560.dp)) {
            DetailPane(
                title = "Package name",
                value = "com.google.android.youtube.tv",
                explanation = "The unique identifier Android and app stores use for this app. No two apps on a device can share it.",
            )
        }
    }
}
