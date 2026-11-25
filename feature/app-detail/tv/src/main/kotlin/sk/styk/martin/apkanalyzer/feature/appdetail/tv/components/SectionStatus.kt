package sk.styk.martin.apkanalyzer.feature.appdetail.tv.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.Button
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.R

@Composable
internal fun SectionLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = stringResource(R.string.tv_app_detail_loading), color = AppTheme.colors.onSurfaceVariant)
    }
}

@Composable
internal fun SectionLoading(
    title: String,
    appName: String,
    appReference: AppReference,
    modifier: Modifier = Modifier,
) {
    SectionStateFrame(title = title, appName = appName, appReference = appReference, modifier = modifier) {
        SectionLoading()
    }
}

@Composable
internal fun SectionError(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        focusRequester.requestFocus()
    }
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text(
                text = message,
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.onSurfaceVariant,
                modifier = if (onRetry == null) Modifier.focusRequester(focusRequester).focusable() else Modifier,
            )
            if (onRetry != null) {
                Button(
                    onClick = onRetry,
                    label = stringResource(R.string.tv_app_detail_retry),
                    modifier = Modifier.focusRequester(focusRequester),
                )
            }
        }
    }
}

@Composable
internal fun SectionError(
    title: String,
    appName: String,
    appReference: AppReference,
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    SectionStateFrame(title = title, appName = appName, appReference = appReference, modifier = modifier) {
        SectionError(message = message, onRetry = onRetry)
    }
}

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SectionErrorPreview() {
    ApkAnalyzerTvTheme {
        SectionError(message = "This app's details couldn't be loaded.", onRetry = {})
    }
}

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SectionErrorWithHeaderLongNamePreview() {
    ApkAnalyzerTvTheme {
        SectionError(
            title = "Permissions",
            appName = "Super Ultra Long Streaming Application Title That Should Truncate Cleanly",
            appReference = AppReference.InstalledPackage(PackageName("com.google.android.youtube.tv")),
            message = "Failed to load permissions.",
            onRetry = {},
        )
    }
}

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SectionLoadingPreview() {
    ApkAnalyzerTvTheme {
        SectionLoading()
    }
}

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SectionLoadingWithHeaderPreview() {
    ApkAnalyzerTvTheme {
        SectionLoading(
            title = "Android manifest",
            appName = "YouTube",
            appReference = AppReference.InstalledPackage(PackageName("com.google.android.youtube.tv")),
        )
    }
}
