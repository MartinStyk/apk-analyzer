package sk.styk.martin.apkanalyzer.core.designsystem.tv.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItemDefaults
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.tv.modifier.pointerClickable
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme
import androidx.tv.material3.ListItem as TvMaterialListItem

@Composable
fun SelectableListItem(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingLabel: String? = null,
    selectedIcon: ImageVector = ApkAnalyzerIcons.Check,
) {
    TvMaterialListItem(
        selected = selected,
        onClick = onClick,
        modifier = modifier.pointerClickable(ItemShape, onClick),
        shape = ListItemDefaults.shape(shape = ItemShape),
        trailingContent = if (selected) {
            { Icon(imageVector = selectedIcon, contentDescription = null, modifier = Modifier.size(20.dp)) }
        } else {
            null
        },
        colors = ListItemDefaults.colors(
            containerColor = AppTheme.colors.surface,
            contentColor = AppTheme.colors.onSurface,
            selectedContainerColor = AppTheme.colors.primaryContainer,
            selectedContentColor = AppTheme.colors.primary,
            focusedContainerColor = AppTheme.colors.surfaceVariant,
            focusedContentColor = AppTheme.colors.onSurface,
        ),
        border = ListItemDefaults.border(
            focusedBorder = Border(border = BorderStroke(width = FocusBorderWidth, color = AppTheme.colors.primary)),
        ),
        headlineContent = {
            Text(text = label, style = AppTheme.typography.titleMedium, maxLines = 1)
        },
        supportingContent = supportingLabel?.let {
            { Text(text = it, style = AppTheme.typography.bodyMedium, maxLines = 1) }
        },
    )
}

private val ItemShape = RoundedCornerShape(8.dp)

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF161618)
@Composable
private fun SelectableListItemPreview() {
    ApkAnalyzerTvTheme {
        Column {
            SelectableListItem(
                selected = true,
                onClick = {},
                label = "APK size",
                supportingLabel = "Largest first",
                selectedIcon = ApkAnalyzerIcons.SortDescending,
            )
            SelectableListItem(selected = false, onClick = {}, label = "Install date")
        }
    }
}
