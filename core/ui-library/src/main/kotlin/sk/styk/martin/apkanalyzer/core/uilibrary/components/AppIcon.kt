package sk.styk.martin.apkanalyzer.core.uilibrary.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import sk.styk.martin.apkanalyzer.core.common.model.AppReference

@Composable
fun AppIcon(
    source: AppReference,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    AsyncImage(
        model = source,
        contentDescription = null,
        modifier = modifier.size(size),
    )
}
