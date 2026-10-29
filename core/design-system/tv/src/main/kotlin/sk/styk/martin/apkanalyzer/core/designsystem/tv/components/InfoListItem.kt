package sk.styk.martin.apkanalyzer.core.designsystem.tv.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.LocalContentColor
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.tv.modifier.pointerClickable
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme
import androidx.tv.material3.ListItem as TvMaterialListItem

@Composable
fun InfoListItem(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null,
    highlighted: Boolean = false,
    leadingIcon: ImageVector? = null,
    iconTint: Color = AppTheme.colors.primary,
) {
    TvMaterialListItem(
        selected = false,
        onClick = onClick,
        modifier = modifier.pointerClickable(ItemShape, onClick),
        shape = ListItemDefaults.shape(shape = ItemShape),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = AppTheme.colors.onSurface,
            focusedContainerColor = AppTheme.colors.surfaceVariant,
            focusedContentColor = AppTheme.colors.onSurface,
        ),
        border = ListItemDefaults.border(
            focusedBorder = Border(border = BorderStroke(width = FocusBorderWidth, color = AppTheme.colors.primary), shape = ItemShape),
        ),
        scale = ListItemDefaults.scale(focusedScale = 1.02f),
        headlineContent = {
            Text(text = label, style = AppTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                text = value,
                style = AppTheme.typography.bodyMedium,
                color = if (highlighted) AppTheme.colors.warning else LocalContentColor.current.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = leadingIcon?.let { icon ->
            {
                IconBadge(icon = icon, tint = if (highlighted) AppTheme.colors.warning else iconTint)
            }
        },
        trailingContent = trailingIcon?.let { icon ->
            { Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp)) }
        },
    )
}

private val ItemShape = RoundedCornerShape(10.dp)

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun InfoListItemPreview() {
    ApkAnalyzerTvTheme {
        Column(modifier = Modifier.width(560.dp)) {
            InfoListItem(label = "Package name", value = "com.google.android.youtube.tv", onClick = {})
            InfoListItem(label = "Debug access", value = "Enabled", onClick = {}, highlighted = true)
            InfoListItem(label = "Split APKs", value = "4 installed", onClick = {}, trailingIcon = ApkAnalyzerIcons.ChevronRight)
        }
    }
}
