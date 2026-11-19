package sk.styk.martin.apkanalyzer.feature.appdetail.phone.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryChangeDirection
import sk.styk.martin.apkanalyzer.core.apppermissions.detail.GrantState
import sk.styk.martin.apkanalyzer.core.apppermissions.detail.PermissionItem
import sk.styk.martin.apkanalyzer.core.apppermissions.detail.labelRes
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Tag
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.ChangeMarkedIcon
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.RowChangeLines
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.markedBy

@Composable
internal fun PermissionRow(
    item: PermissionItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String? = null,
    changeLines: List<String> = emptyList(),
    changeMarker: AppHistoryChangeDirection? = null,
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
            imageVector = item.icon,
            contentDescription = stringResource(item.protectionLevel.labelRes),
            tint = if (item.protectionLevel == ProtectionLevel.Dangerous) AppTheme.colors.primary else AppTheme.colors.onSurfaceVariant,
            marker = changeMarker,
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.label,
                style = AppTheme.typography.titleSmall.markedBy(changeMarker),
                color = AppTheme.colors.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.name,
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.StartEllipsis,
            )
            RowChangeLines(lines = changeLines)
            tag?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Tag(text = it)
            }
        }
        item.grantState?.let { grantState ->
            Spacer(modifier = Modifier.width(8.dp))
            GrantPill(grantState = grantState)
        }
    }
}

@Composable
private fun GrantPill(grantState: GrantState, modifier: Modifier = Modifier) {
    val color = when (grantState) {
        GrantState.Granted -> AppTheme.colors.primary
        GrantState.NotGranted -> AppTheme.colors.onSurfaceVariant
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = stringResource(grantState.labelRes),
            style = AppTheme.typography.labelMedium,
            color = color,
        )
    }
}

private fun samplePermission(grantState: GrantState? = GrantState.Granted) = PermissionItem(
    name = "android.permission.CAMERA",
    label = "Camera",
    description = null,
    groupName = null,
    protectionLevel = ProtectionLevel.Dangerous,
    protectionFlags = emptyList(),
    grantState = grantState,
    declaringPackage = PackageName("android"),
    isSelfDeclared = false,
)

@Preview
@Composable
private fun PermissionRowPreview() {
    ApkAnalyzerTheme {
        PermissionRow(item = samplePermission(), onClick = {}, onLongClick = {})
    }
}

@Preview
@Composable
private fun PermissionRowAddedPreview() {
    ApkAnalyzerTheme {
        PermissionRow(item = samplePermission(grantState = null), onClick = {
        }, onLongClick = {}, tag = "Dangerous", changeMarker = AppHistoryChangeDirection.Added)
    }
}

@Preview
@Composable
private fun PermissionRowRemovedPreview() {
    ApkAnalyzerTheme {
        PermissionRow(item = samplePermission(grantState = null), onClick = {}, onLongClick = {}, changeMarker = AppHistoryChangeDirection.Removed)
    }
}
