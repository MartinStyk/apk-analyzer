package sk.styk.martin.apkanalyzer.core.designsystem.phone.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.AppIcon
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.ListItemPosition
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.shape
import sk.styk.martin.apkanalyzer.core.designsystem.phone.modifier.shimmer
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.Shapes

private val SkeletonIconSize = 44.dp

@Composable
fun AppRow(
    packageName: PackageName,
    applicationName: String,
    onClick: () -> Unit,
    position: ListItemPosition,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.surface, position.shape())
            .padding(2.dp)
            .clip(Shapes.CardShape)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        AppIcon(source = AppReference.InstalledPackage(packageName))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = applicationName,
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = packageName.value,
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingContent()
        }
    }
}

@Composable
fun AppRowSkeleton(position: ListItemPosition, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.surface, position.shape())
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(SkeletonIconSize)
                .clip(Shapes.CardShape)
                .background(AppTheme.colors.surfaceVariant)
                .shimmer(),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(14.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(11.dp),
            )
        }
    }
}

@Preview
@Composable
private fun AppRowPreview() {
    ApkAnalyzerTheme {
        Column {
            AppRow(
                packageName = PackageName("com.instagram.android"),
                applicationName = "Instagram",
                onClick = {},
                position = ListItemPosition.First,
            )
            AppRow(
                packageName = PackageName("com.whatsapp"),
                applicationName = "WhatsApp",
                onClick = {},
                position = ListItemPosition.Middle,
                trailingContent = { Tag(text = "34") },
            )
            AppRowSkeleton(position = ListItemPosition.Last)
        }
    }
}
