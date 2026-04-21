package sk.styk.martin.apkanalyzer.core.uilibrary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.app.PackageIcon
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppShapes
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme

@Composable
fun AppIcon(packageName: String, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    AsyncImage(
        model = PackageIcon(packageName),
        contentDescription = null,
        modifier = modifier
            .size(size)
//            .clip(AppShapes.CardShape)
//            .background(AppTheme.colors.surfaceVariant),
    )
}
