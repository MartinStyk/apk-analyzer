package sk.styk.martin.apkanalyzer.feature.appdetail.phone.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.common.model.ChangeDirection
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.ChangeMarkerDot
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Icon
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.R

internal fun TextStyle.markedBy(marker: ChangeDirection?): TextStyle =
    if (marker == ChangeDirection.Removed) copy(textDecoration = TextDecoration.LineThrough) else this

@Composable
internal fun ChangeMarkedIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    tint: Color,
    marker: ChangeDirection?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(22.dp)) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        marker?.let { ChangeBadge(marker = it, modifier = Modifier.align(Alignment.TopEnd)) }
    }
}

@Composable
private fun ChangeBadge(marker: ChangeDirection, modifier: Modifier = Modifier) {
    ChangeMarkerDot(
        direction = marker,
        contentDescription = stringResource(
            when (marker) {
                ChangeDirection.Added -> R.string.app_history_group_added
                ChangeDirection.Removed -> R.string.app_history_group_removed
                ChangeDirection.Changed -> R.string.app_history_group_changed
            },
        ),
        modifier = modifier
            .offset(x = 6.dp, y = (-5).dp)
            .size(16.dp)
            .background(AppTheme.colors.surface, CircleShape)
            .padding(2.dp),
    )
}

@Preview
@Composable
private fun ChangeMarkedIconPreview() {
    ApkAnalyzerTheme {
        Row(modifier = Modifier.background(AppTheme.colors.surface).padding(16.dp)) {
            ChangeMarkedIcon(ApkAnalyzerIcons.Camera, contentDescription = null, tint = AppTheme.colors.primary, marker = ChangeDirection.Added)
            Spacer(modifier = Modifier.width(24.dp))
            ChangeMarkedIcon(
                ApkAnalyzerIcons.Phone,
                contentDescription = null,
                tint = AppTheme.colors.onSurfaceVariant,
                marker = ChangeDirection.Removed,
            )
        }
    }
}
