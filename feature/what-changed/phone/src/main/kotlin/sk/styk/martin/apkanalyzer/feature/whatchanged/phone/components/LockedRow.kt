package sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.AppIcon
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.ProBadge
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.ListItemPosition
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.shape
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.R

private const val PLACEHOLDER_ALPHA = 0.3f
private const val PLACEHOLDER_WIDTH = 0.7f
private val PlaceholderShape = RoundedCornerShape(4.dp)
private val LockedIconSize = 36.dp

@Composable
internal fun LockedRow(
    packageName: String,
    appName: String,
    position: ListItemPosition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = "$appName. ${stringResource(R.string.what_changed_locked_row)}"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.surface, position.shape())
            .padding(2.dp)
            .clip(Shapes.CardShape)
            .clickable(
                onClickLabel = stringResource(R.string.what_changed_locked_click_label),
                role = Role.Button,
                onClick = onClick,
            )
            .clearAndSetSemantics { contentDescription = description }
            .padding(12.dp),
    ) {
        AppIcon(
            source = AppReference.InstalledPackage(PackageName(packageName)),
            size = LockedIconSize,
            missingIcon = ApkAnalyzerIcons.Android,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            Text(
                text = appName,
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.onSurface,
                maxLines = 1,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(PLACEHOLDER_WIDTH)
                    .height(10.dp)
                    .clip(PlaceholderShape)
                    .background(AppTheme.colors.onSurfaceVariant.copy(alpha = PLACEHOLDER_ALPHA)),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        ProBadge()
    }
}

@Preview
@Composable
private fun LockedRowPreview() {
    ApkAnalyzerTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            LockedRow(packageName = "org.thoughtcrime.securesms", appName = "Signal", position = ListItemPosition.First, onClick = {})
            LockedRow(packageName = "com.strava", appName = "Strava", position = ListItemPosition.Last, onClick = {})
        }
    }
}

@Preview
@Composable
private fun LockedRowDarkPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        LockedRow(
            packageName = "org.thoughtcrime.securesms",
            appName = "Signal",
            position = ListItemPosition.Single,
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
