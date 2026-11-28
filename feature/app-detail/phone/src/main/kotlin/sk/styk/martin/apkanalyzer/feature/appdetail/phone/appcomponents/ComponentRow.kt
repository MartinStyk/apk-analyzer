package sk.styk.martin.apkanalyzer.feature.appdetail.phone.appcomponents

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.apps.api.components.labelRes
import sk.styk.martin.apkanalyzer.core.common.model.ChangeDirection
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Icon
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.IconButton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.IconButtonStyle
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Tag
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.R
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.ChangeMarkedIcon
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.RowChangeLines
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.markedBy
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.R as DomainR

@Composable
internal fun ComponentRow(
    item: ComponentItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLaunch: (() -> Unit)? = null,
    changeLines: List<String> = emptyList(),
    changeMarker: ChangeDirection? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(Shapes.CardShape)
            .background(AppTheme.colors.surface)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        ChangeMarkedIcon(
            imageVector = item.type.icon,
            contentDescription = stringResource(item.type.sectionLabelRes),
            tint = AppTheme.colors.onSurfaceVariant,
            marker = changeMarker,
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.simpleName,
                style = AppTheme.typography.titleSmall.markedBy(changeMarker),
                color = AppTheme.colors.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.packagePath.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.packagePath,
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.StartEllipsis,
                )
            }
            RowChangeLines(lines = changeLines)
            val hasStatusTag = item.isDisabled || item.isUnprotected || item.isLauncher || item.isExported
            if (hasStatusTag || item.flags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (item.isDisabled) {
                        Tag(text = stringResource(DomainR.string.components_tag_disabled))
                    }
                    if (item.isUnprotected) {
                        Icon(
                            imageVector = ApkAnalyzerIcons.Warning,
                            contentDescription = stringResource(DomainR.string.components_tag_unprotected),
                            tint = AppTheme.colors.warning,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    if (item.isLauncher) {
                        Tag(
                            text = stringResource(DomainR.string.components_tag_launcher),
                            highlighted = true,
                        )
                    }
                    if (item.isExported) {
                        Tag(text = stringResource(DomainR.string.components_tag_exported))
                    }
                    item.flags.forEach { flag ->
                        Tag(text = stringResource(flag.labelRes))
                    }
                }
            }
        }
        if (item.isLaunchable && onLaunch != null) {
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                imageVector = ApkAnalyzerIcons.PlayArrow,
                onClick = onLaunch,
                style = IconButtonStyle.Filled,
                contentDescription = stringResource(R.string.components_launch),
                iconSize = 18.dp,
                modifier = Modifier.size(34.dp),
            )
        }
    }
}

private fun sampleComponent() = ComponentItem(
    name = "com.spotify.music.offline.DownloadSyncService",
    simpleName = "DownloadSyncService",
    packagePath = "com.spotify.music.offline",
    type = ComponentType.Service,
    isExported = true,
    isGuarded = false,
    isUnprotected = true,
    isLaunchable = false,
    isDisabled = false,
    flags = emptyList(),
    intentFilters = null,
    details = ComponentDetails.ServiceDetails(permission = null),
)

private fun sampleDisabledComponent() = ComponentItem(
    name = "com.google.androidbrowserhelper.trusted.LauncherActivityAlias",
    simpleName = "LauncherActivityAlias",
    packagePath = "com.google.androidbrowserhelper.trusted",
    type = ComponentType.Activity,
    isExported = true,
    isGuarded = false,
    isUnprotected = false,
    isLaunchable = false,
    isDisabled = true,
    flags = emptyList(),
    intentFilters = null,
    details = ComponentDetails.ActivityDetails(
        label = "Docs",
        targetActivity = "com.google.android.apps.docs.app.NewMainProxyActivity",
        parentName = null,
        permission = null,
        isLauncher = true,
    ),
)

@Preview
@Composable
private fun ComponentRowPreview() {
    ApkAnalyzerTheme {
        ComponentRow(item = sampleComponent(), onClick = {}, onLongClick = {})
    }
}

@Preview
@Composable
private fun ComponentRowDisabledPreview() {
    ApkAnalyzerTheme {
        ComponentRow(item = sampleDisabledComponent(), onClick = {}, onLongClick = {}, onLaunch = {})
    }
}

@Preview
@Composable
private fun ComponentRowAddedPreview() {
    ApkAnalyzerTheme {
        ComponentRow(item = sampleComponent(), onClick = {}, onLongClick = {}, changeMarker = ChangeDirection.Added)
    }
}
