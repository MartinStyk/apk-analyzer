package sk.styk.martin.apkanalyzer.feature.appdetail.tv.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.Icon
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme

@Composable
internal fun SectionEmpty(
    title: String,
    appName: String,
    appReference: AppReference,
    message: String,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        focusRequester.requestFocus()
    }
    SectionStateFrame(title = title, appName = appName, appReference = appReference, modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .focusRequester(focusRequester)
                .focusable(),
        ) {
            Icon(imageVector = ApkAnalyzerIcons.Check, tint = AppTheme.colors.primary, modifier = Modifier.size(48.dp))
            Text(text = message, style = AppTheme.typography.titleMedium, color = AppTheme.colors.onSurfaceVariant)
        }
    }
}

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SectionEmptyPreview() {
    ApkAnalyzerTvTheme {
        SectionEmpty(
            title = "Device requirements",
            appName = "YouTube",
            appReference = AppReference.InstalledPackage(PackageName("com.google.android.youtube.tv")),
            message = "This app declares no device requirements.",
        )
    }
}
