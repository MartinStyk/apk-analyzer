package sk.styk.martin.apkanalyzer.feature.appdetail.phone.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Icon
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.Shapes

@Composable
internal fun WarningBanner(
    title: String?,
    message: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = if (title == null) Alignment.CenterVertically else Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.CardShape)
            .background(AppTheme.colors.warningContainer)
            .padding(14.dp),
    ) {
        Icon(
            imageVector = ApkAnalyzerIcons.Warning,
            contentDescription = null,
            tint = AppTheme.colors.warning,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            if (title != null) {
                Text(
                    text = title,
                    style = AppTheme.typography.titleSmall,
                    color = AppTheme.colors.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = message,
                style = if (title == null) AppTheme.typography.bodyMedium else AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurface,
            )
        }
    }
}

@Preview
@Composable
private fun WarningBannerWithTitlePreview() {
    ApkAnalyzerTheme {
        WarningBanner(
            title = "Signing certificate changed",
            message = "Android only accepts an update with a different certificate when the old key approved it.",
        )
    }
}

@Preview
@Composable
private fun WarningBannerMessageOnlyPreview() {
    ApkAnalyzerTheme {
        WarningBanner(title = null, message = "This device is missing 2 required features.")
    }
}
