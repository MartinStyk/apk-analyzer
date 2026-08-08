package sk.styk.martin.apkanalyzer.feature.appdetail.impl.permissions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import sk.styk.martin.apkanalyzer.core.apps.model.ProtectionFlag
import sk.styk.martin.apkanalyzer.core.apps.model.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.uilibrary.components.BottomSheet
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Icon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.R
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.DetailField

private const val UNDEFINED_PERMISSION_GROUP = "android.permission-group.UNDEFINED"

@Suppress("LongMethod")
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
            item.description?.let { description ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = description,
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            DetailField(
                label = stringResource(R.string.permissions_detail_full_name),
                value = item.name,
                onCopy = onCopy,
            )
            item.declaringPackage?.let { declaringPackage ->
                DetailField(
                    label = stringResource(R.string.permissions_detail_declared_by),
                    value = declaringPackage.value,
                    onCopy = onCopy,
                )
            }
            item.groupName?.takeIf { it != UNDEFINED_PERMISSION_GROUP }?.let { group ->
                DetailField(
                    label = stringResource(R.string.permissions_detail_group),
                    value = group,
                    onCopy = onCopy,
                )
            }

            SheetSection(title = stringResource(R.string.permissions_detail_section_status)) {
                item.grantState?.let { grantState ->
                    DetailField(
                        label = stringResource(R.string.permissions_detail_grant_state),
                        value = stringResource(grantState.labelRes),
                        explanation = item.grantExplanationRes?.let { stringResource(it) },
                        onCopy = onCopy,
                    )
                }
                DetailField(
                    label = stringResource(R.string.permissions_detail_protection_level),
                    value = stringResource(item.protectionLevel.labelRes),
                    explanation = stringResource(item.protectionLevel.explanationRes),
                    onCopy = onCopy,
                )
                if (item.protectionFlags.isNotEmpty()) {
                    LabelledBlock(label = stringResource(R.string.permissions_detail_extra_rules)) {
                        item.protectionFlags.forEach { flag ->
                            Text(
                                text = stringResource(flag.explanationRes),
                                style = AppTheme.typography.bodySmall,
                                color = AppTheme.colors.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.padding(top = 24.dp)) {
        Text(
            text = title,
            style = AppTheme.typography.titleSmall,
            color = AppTheme.colors.primary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        content()
    }
}

@Composable
private fun LabelledBlock(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = AppTheme.typography.labelMedium,
            color = AppTheme.colors.onSurfaceVariant,
        )
        content()
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
                protectionFlags = persistentListOf(ProtectionFlag.AppOp, ProtectionFlag.Instant),
                grantState = GrantState.NotGranted,
                declaringPackage = PackageName("android"),
                isSelfDeclared = false,
            ),
            onCopy = { _, _ -> },
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun PermissionDetailBottomSheetSignaturePreview() {
    ApkAnalyzerTheme {
        PermissionDetailBottomSheet(
            item = PermissionItem(
                name = "com.facebook.katana.provider.ACCESS",
                label = "Access",
                description = null,
                groupName = null,
                protectionLevel = ProtectionLevel.Signature,
                protectionFlags = persistentListOf(),
                grantState = GrantState.NotGranted,
                declaringPackage = PackageName("com.facebook.katana"),
                isSelfDeclared = false,
            ),
            onCopy = { _, _ -> },
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun PermissionDetailBottomSheetSelfDeclaredPreview() {
    ApkAnalyzerTheme {
        PermissionDetailBottomSheet(
            item = PermissionItem(
                name = "com.instagram.android.permission.CROSS_PROCESS_BROADCAST_MANAGER",
                label = "Cross Process Broadcast Manager",
                description = null,
                groupName = null,
                protectionLevel = ProtectionLevel.Signature,
                protectionFlags = persistentListOf(),
                grantState = GrantState.Granted,
                declaringPackage = PackageName("com.instagram.android"),
                isSelfDeclared = true,
            ),
            onCopy = { _, _ -> },
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun PermissionDetailBottomSheetUnresolvedPreview() {
    ApkAnalyzerTheme {
        PermissionDetailBottomSheet(
            item = PermissionItem(
                name = "com.sonymobile.home.permission.PROVIDER_INSERT_BADGE",
                label = "Provider Insert Badge",
                description = null,
                groupName = null,
                protectionLevel = null,
                protectionFlags = persistentListOf(),
                grantState = GrantState.NotGranted,
                declaringPackage = null,
                isSelfDeclared = false,
            ),
            onCopy = { _, _ -> },
            onDismiss = {},
        )
    }
}
