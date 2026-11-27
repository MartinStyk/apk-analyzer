package sk.styk.martin.apkanalyzer.core.designsystem.phone.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.common.model.ChangeDirection
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme

@Composable
fun ChangeToken(
    text: String,
    direction: ChangeDirection,
    isEmphasized: Boolean,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        if (isEmphasized) {
            ChangeMarkerDot(direction = direction, contentDescription = null, color = AppTheme.colors.tertiary)
        } else {
            ChangeMarkerDot(direction = direction, contentDescription = null)
        }
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            style = if (isEmphasized) AppTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium) else AppTheme.typography.bodySmall,
            color = if (isEmphasized) AppTheme.colors.tertiary else textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ChangeMarkerDot(
    direction: ChangeDirection,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    color: Color = direction.color,
) {
    val isOutlined = direction == ChangeDirection.Changed
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(14.dp)
            .then(if (isOutlined) Modifier.border(1.5.dp, color, CircleShape) else Modifier.background(color, CircleShape))
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
    ) {
        Icon(
            imageVector = when (direction) {
                ChangeDirection.Added -> ApkAnalyzerIcons.Add
                ChangeDirection.Removed -> ApkAnalyzerIcons.Remove
                ChangeDirection.Changed -> ApkAnalyzerIcons.ChevronRight
            },
            tint = if (isOutlined) color else AppTheme.colors.surface,
            modifier = Modifier.size(10.dp),
        )
    }
}

private val ChangeDirection.color: Color
    @Composable
    get() = when (this) {
        ChangeDirection.Added -> AppTheme.colors.primary
        ChangeDirection.Removed, ChangeDirection.Changed -> AppTheme.colors.onSurfaceVariant
    }

private val PreviewTokens = listOf(
    Triple("Camera added", ChangeDirection.Added, true),
    Triple("2 activities added", ChangeDirection.Added, false),
    Triple("1 permission removed", ChangeDirection.Removed, false),
    Triple("Size up 8.0 MB", ChangeDirection.Changed, false),
)

@Composable
private fun ChangeTokenSamples() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .background(AppTheme.colors.surface)
            .padding(16.dp),
    ) {
        PreviewTokens.forEach { (text, direction, isEmphasized) ->
            ChangeToken(text = text, direction = direction, isEmphasized = isEmphasized, textColor = AppTheme.colors.onSurfaceVariant)
        }
    }
}

@Preview
@Composable
private fun ChangeTokenPreview() {
    ApkAnalyzerTheme {
        ChangeTokenSamples()
    }
}

@Preview
@Composable
private fun ChangeTokenDarkPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        ChangeTokenSamples()
    }
}
