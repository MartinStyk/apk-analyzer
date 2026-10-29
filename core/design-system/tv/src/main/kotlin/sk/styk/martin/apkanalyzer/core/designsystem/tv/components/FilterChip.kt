package sk.styk.martin.apkanalyzer.core.designsystem.tv.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChipDefaults
import androidx.tv.material3.Icon
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.tv.modifier.pointerClickable
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme
import androidx.tv.material3.FilterChip as TvMaterialFilterChip

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null,
) {
    TvMaterialFilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier.pointerClickable(ChipShape, onClick),
        shape = FilterChipDefaults.shape(shape = ChipShape),
        trailingIcon = trailingIcon?.let { icon ->
            { Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
        },
    ) {
        Text(text = label, style = AppTheme.typography.labelLarge, maxLines = 1)
    }
}

private val ChipShape = RoundedCornerShape(8.dp)

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun FilterChipPreview() {
    ApkAnalyzerTvTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(selected = true, onClick = {}, label = "Name", trailingIcon = ApkAnalyzerIcons.SortAscending)
            FilterChip(selected = false, onClick = {}, label = "Install date")
        }
    }
}
