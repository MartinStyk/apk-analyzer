package sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme

@Composable
fun AppIcon(
    source: AppReference,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    missingIcon: ImageVector? = null,
) {
    var isMissing by remember(source) { mutableStateOf(false) }
    if (missingIcon != null && isMissing) {
        Image(
            imageVector = missingIcon,
            contentDescription = null,
            colorFilter = ColorFilter.tint(AppTheme.colors.onSurfaceVariant),
            modifier = modifier.size(size),
        )
    } else {
        AsyncImage(
            model = source,
            contentDescription = null,
            onError = { isMissing = true },
            modifier = modifier.size(size),
        )
    }
}
