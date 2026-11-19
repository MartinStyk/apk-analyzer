package sk.styk.martin.apkanalyzer.feature.appdetail.phone.nativelibraries

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryChangeDirection
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraryItem
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraryVariant
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Tag
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.ChangeMarkedIcon
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.markedBy
import sk.styk.martin.apkanalyzer.core.apps.R as AppsR

@Composable
internal fun NativeLibraryRow(
    item: NativeLibraryItem,
    onClick: () -> Unit,
    onCopy: (label: String, value: String) -> Unit,
    modifier: Modifier = Modifier,
    changeLines: List<String> = emptyList(),
    changeMarker: AppHistoryChangeDirection? = null,
) {
    val copyLabel = stringResource(AppsR.string.nativelibraries_title)
    val copyValue = (listOf(item.name) + changeLines).joinToString("\n")
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.CardShape)
            .background(AppTheme.colors.surface)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onCopy(copyLabel, copyValue) },
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChangeMarkedIcon(
                imageVector = ApkAnalyzerIcons.Memory,
                contentDescription = null,
                tint = AppTheme.colors.primary,
                marker = changeMarker,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = AppTheme.typography.titleSmall.markedBy(changeMarker),
                    color = AppTheme.colors.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (changeLines.isEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.abis.joinToString(),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (changeLines.isEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.totalSize.formatted(),
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.onSurfaceVariant,
                )
            }
        }
        changeLines.forEach { line ->
            Text(
                text = line,
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurface,
            )
        }
        if (!item.isDeviceCompatible) {
            Tag(
                text = stringResource(AppsR.string.nativelibraries_not_supported),
                icon = ApkAnalyzerIcons.Warning,
            )
        }
    }
}

private val sampleLibrary = NativeLibraryItem(
    name = "libspotify.so",
    abis = listOf("arm64-v8a", "armeabi-v7a"),
    totalSize = 4.megabytes,
    isDeviceCompatible = true,
    variants = listOf(NativeLibraryVariant("arm64-v8a", 2.megabytes, "base.apk"), NativeLibraryVariant("armeabi-v7a", 2.megabytes, "base.apk")),
)

@Preview
@Composable
private fun NativeLibraryRowPreview() {
    ApkAnalyzerTheme {
        NativeLibraryRow(item = sampleLibrary, onClick = {}, onCopy = { _, _ -> })
    }
}

@Preview
@Composable
private fun NativeLibraryRowRemovedPreview() {
    ApkAnalyzerTheme {
        NativeLibraryRow(item = sampleLibrary, onClick = {}, onCopy = { _, _ -> }, changeMarker = AppHistoryChangeDirection.Removed)
    }
}
