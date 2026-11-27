package sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.ProBadge
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.ListItemPosition
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.shape
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.R

private const val PLACEHOLDER_ALPHA = 0.3f
private val PlaceholderShape = RoundedCornerShape(4.dp)

@Composable
internal fun LockedRow(
    position: ListItemPosition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.what_changed_locked_row)
    val placeholderColor = AppTheme.colors.onSurfaceVariant.copy(alpha = PLACEHOLDER_ALPHA)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.surface, position.shape())
            .padding(2.dp)
            .clip(Shapes.CardShape)
            .clickable(
                onClickLabel = stringResource(R.string.what_changed_locked_click_label),
                role = Role.Button,
                onClick = onClick,
            )
            .clearAndSetSemantics { contentDescription = description }
            .padding(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .weight(1f)
                .blur(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(Shapes.CardShape)
                    .background(placeholderColor),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f).padding(top = 2.dp)) {
                PlaceholderBar(widthFraction = 0.6f, height = 12.dp, color = placeholderColor)
                PlaceholderBar(widthFraction = 0.85f, height = 10.dp, color = placeholderColor)
                PlaceholderBar(widthFraction = 0.45f, height = 10.dp, color = placeholderColor)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        ProBadge()
    }
}

@Composable
private fun PlaceholderBar(
    widthFraction: Float,
    height: Dp,
    color: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(PlaceholderShape)
            .background(color),
    )
}

@Preview
@Composable
private fun LockedRowPreview() {
    ApkAnalyzerTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            LockedRow(position = ListItemPosition.First, onClick = {})
            LockedRow(position = ListItemPosition.Last, onClick = {})
        }
    }
}

@Preview
@Composable
private fun LockedRowDarkPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        LockedRow(position = ListItemPosition.Single, onClick = {}, modifier = Modifier.padding(16.dp))
    }
}
