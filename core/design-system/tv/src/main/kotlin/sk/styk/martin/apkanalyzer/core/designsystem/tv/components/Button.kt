package sk.styk.martin.apkanalyzer.core.designsystem.tv.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.tv.modifier.pointerClickable
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme
import androidx.tv.material3.OutlinedButton as TvMaterialOutlinedButton

@Composable
fun Button(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    TvMaterialOutlinedButton(onClick = onClick, modifier = modifier.pointerClickable(CircleShape, onClick)) {
        if (leadingIcon != null) {
            Icon(imageVector = leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp).padding(end = 4.dp))
        }
        Text(text = label, style = AppTheme.typography.labelLarge, maxLines = 1)
    }
}

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ButtonPreview() {
    ApkAnalyzerTvTheme {
        Button(onClick = {}, label = "Search apps", leadingIcon = ApkAnalyzerIcons.Search)
    }
}
