package sk.styk.martin.apkanalyzer.feature.appdetail.impl.permissions

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import sk.styk.martin.apkanalyzer.core.uilibrary.components.BottomSheet
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Chip
import sk.styk.martin.apkanalyzer.core.uilibrary.components.ChipVariant
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Icon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.R

@Composable
internal fun PermissionDetailBottomSheet(
    item: PermissionItem,
    onCopy: (label: String, value: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BottomSheet(onDismiss = onDismiss, modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = item.icon,
                    tint = AppTheme.colors.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = item.label,
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colors.onSurface,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            item.description?.let { description ->
                Text(
                    text = description,
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
            DetailField(
                label = stringResource(R.string.permissions_detail_full_name),
                value = item.name,
                onCopy = onCopy,
            )
            item.grantState?.let { grantState ->
                DetailField(
                    label = stringResource(R.string.permissions_detail_grant_state),
                    value = stringResource(grantState.labelRes),
                    onCopy = onCopy,
                )
            }
            DetailField(
                label = stringResource(R.string.permissions_detail_protection_level),
                value = stringResource(item.protectionLevel.labelRes),
                onCopy = onCopy,
            )
            Text(
                text = stringResource(item.protectionLevel.explanationRes),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
            )
            if (item.protectionFlags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.protectionFlags.forEach { flag ->
                        Chip(
                            label = stringResource(flag.labelRes),
                            variant = ChipVariant.Default,
                        )
                    }
                }
            }
            item.groupName?.let { group ->
                Spacer(modifier = Modifier.height(4.dp))
                DetailField(
                    label = stringResource(R.string.permissions_detail_group),
                    value = group,
                    onCopy = onCopy,
                )
            }
            item.declaringPackage?.let { declaringPackage ->
                DetailField(
                    label = stringResource(R.string.permissions_detail_declared_by),
                    value = declaringPackage,
                    onCopy = onCopy,
                )
            }
        }
    }
}

@Composable
private fun DetailField(
    label: String,
    value: String,
    onCopy: (label: String, value: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.CardShape)
            .clickable { onCopy(label, value) }
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = AppTheme.typography.labelMedium,
            color = AppTheme.colors.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onBackground,
        )
    }
}

@Preview
@Composable
private fun PermissionDetailBottomSheetPreview() {
    ApkAnalyzerTheme {
        PermissionDetailBottomSheet(
            item = PermissionItem(
                name = "android.permission.ACCESS_FINE_LOCATION",
                label = "Precise location",
                description = "Read the exact position of the device from GPS and nearby networks.",
                groupName = "android.permission-group.LOCATION",
                protectionLevel = ProtectionLevel.Dangerous,
                protectionFlags = persistentListOf(ProtectionFlag.AppOp),
                grantState = GrantState.Denied,
                declaringPackage = "android",
            ),
            onCopy = { _, _ -> },
            onDismiss = {},
        )
    }
}
