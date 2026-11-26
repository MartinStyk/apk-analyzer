package sk.styk.martin.apkanalyzer.feature.appdetail.phone.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryChangeDirection
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Icon
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.R

internal fun TextStyle.markedBy(marker: AppHistoryChangeDirection?): TextStyle =
    if (marker == AppHistoryChangeDirection.Removed) copy(textDecoration = TextDecoration.LineThrough) else this

@Composable
internal fun ChangeMarkedIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    tint: Color,
    marker: AppHistoryChangeDirection?,
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
private fun ChangeBadge(marker: AppHistoryChangeDirection, modifier: Modifier = Modifier) {
    ChangeMarkerDot(
        marker = marker,
        contentDescription = stringResource(
            when (marker) {
                AppHistoryChangeDirection.Added -> R.string.app_history_group_added
                AppHistoryChangeDirection.Removed -> R.string.app_history_group_removed
                AppHistoryChangeDirection.Changed -> R.string.app_history_group_changed
            },
        ),
        modifier = modifier
            .offset(x = 6.dp, y = (-5).dp)
            .size(16.dp)
            .background(AppTheme.colors.surface, CircleShape)
            .padding(2.dp),
    )
}

@Composable
internal fun ChangeMarkerDot(
    marker: AppHistoryChangeDirection,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    color: Color = marker.color,
) {
    val isOutlined = marker == AppHistoryChangeDirection.Changed
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(14.dp)
            .then(if (isOutlined) Modifier.border(1.5.dp, color, CircleShape) else Modifier.background(color, CircleShape))
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
    ) {
        Icon(
            imageVector = when (marker) {
                AppHistoryChangeDirection.Added -> ApkAnalyzerIcons.Add
                AppHistoryChangeDirection.Removed -> ApkAnalyzerIcons.Remove
                AppHistoryChangeDirection.Changed -> ApkAnalyzerIcons.ChevronRight
            },
            tint = if (isOutlined) color else AppTheme.colors.surface,
            modifier = Modifier.size(10.dp),
        )
    }
}

private val AppHistoryChangeDirection.color: Color
    @Composable
    get() = when (this) {
        AppHistoryChangeDirection.Added -> AppTheme.colors.primary
        AppHistoryChangeDirection.Removed, AppHistoryChangeDirection.Changed -> AppTheme.colors.onSurfaceVariant
    }

@Preview
@Composable
private fun ChangeMarkedIconPreview() {
    ApkAnalyzerTheme {
        Row(modifier = Modifier.background(AppTheme.colors.surface).padding(16.dp)) {
            ChangeMarkedIcon(ApkAnalyzerIcons.Camera, contentDescription = null, tint = AppTheme.colors.primary, marker = AppHistoryChangeDirection.Added)
            Spacer(modifier = Modifier.width(24.dp))
            ChangeMarkedIcon(
                ApkAnalyzerIcons.Phone,
                contentDescription = null,
                tint = AppTheme.colors.onSurfaceVariant,
                marker = AppHistoryChangeDirection.Removed,
            )
        }
    }
}
