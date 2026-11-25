package sk.styk.martin.apkanalyzer.feature.appdetail.tv.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.AppIcon
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.rememberAppAccentColor
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.AccentBackdrop
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme

internal const val SECTION_BACKDROP_STRENGTH = 0.2f

@Composable
internal fun SectionStateFrame(
    title: String,
    appName: String,
    appReference: AppReference,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    AccentBackdrop(accent = rememberAppAccentColor(appReference), strength = SECTION_BACKDROP_STRENGTH, modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 27.dp)) {
            SectionAppHeader(title = title, appName = appName, appReference = appReference)
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center, content = content)
        }
    }
}

@Composable
internal fun SectionAppHeader(
    title: String,
    appName: String,
    appReference: AppReference,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier,
    ) {
        AppIcon(source = appReference, size = 56.dp)
        Column {
            Text(
                text = title,
                style = AppTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (appName.isNotBlank()) {
                Text(
                    text = appName,
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SectionAppHeaderPreview() {
    ApkAnalyzerTvTheme {
        SectionAppHeader(
            title = "Permissions",
            appName = "YouTube",
            appReference = AppReference.InstalledPackage(PackageName("com.google.android.youtube.tv")),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SectionAppHeaderLongNamePreview() {
    ApkAnalyzerTvTheme {
        SectionAppHeader(
            title = "Android manifest",
            appName = "Super Ultra Long Streaming Application Title That Should Truncate Cleanly",
            appReference = AppReference.InstalledPackage(PackageName("com.google.android.youtube.tv")),
            modifier = Modifier.width(420.dp).padding(16.dp),
        )
    }
}
