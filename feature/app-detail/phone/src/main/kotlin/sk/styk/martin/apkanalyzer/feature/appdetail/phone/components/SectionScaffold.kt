package sk.styk.martin.apkanalyzer.feature.appdetail.phone.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.LoadingSpinner
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.TextButton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Toolbar
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.SectionErrorReason
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.R
import java.time.Instant

@Composable
internal fun AppDetailSectionScaffold(
    title: String,
    appName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    recordedAt: Instant? = null,
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    AppDetailModeFrame(recordedAt = recordedAt, modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.background),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Toolbar(
                    title = title,
                    subtitle = appName.takeIf { it.isNotBlank() },
                    onBack = onBack,
                )
                content()
            }
            overlay()
        }
    }
}

@Composable
internal fun SectionLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        LoadingSpinner()
    }
}

@Composable
internal fun SectionError(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colors.onBackground,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                text = stringResource(R.string.app_detail_retry),
                onClick = onRetry,
            )
        }
    }
}

@Composable
internal fun SectionError(
    reason: SectionErrorReason,
    loadFailedMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (reason) {
        SectionErrorReason.LoadFailed -> SectionError(message = loadFailedMessage, modifier = modifier, onRetry = onRetry)

        SectionErrorReason.VersionNotFound -> SectionError(message = stringResource(R.string.app_history_version_not_found), modifier = modifier)

        SectionErrorReason.VersionReadFailed -> SectionError(
            message = stringResource(R.string.app_history_version_load_failed),
            modifier = modifier,
            onRetry = onRetry,
        )
    }
}

@Preview
@Composable
private fun AppDetailSectionScaffoldPreview() {
    ApkAnalyzerTheme {
        AppDetailSectionScaffold(
            title = "Permissions",
            appName = "Spotify",
            onBack = {},
        ) {
            SectionLoading()
        }
    }
}

@Preview(widthDp = 320)
@Composable
private fun AppDetailSectionScaffoldLongAppNamePreview() {
    ApkAnalyzerTheme {
        AppDetailSectionScaffold(
            title = "Native libraries",
            appName = "Very Long Enterprise Authenticator And Security Manager Application Name",
            onBack = {},
        ) {
            SectionLoading()
        }
    }
}

@Preview
@Composable
private fun AppDetailSectionScaffoldHistoricalPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        AppDetailSectionScaffold(
            title = "Certificates",
            appName = "Spotify",
            onBack = {},
            recordedAt = Instant.ofEpochMilli(1_773_014_400_000),
        ) {
            SectionLoading()
        }
    }
}

@Preview
@Composable
private fun RecordedVersionNotFoundPreview() {
    ApkAnalyzerTheme {
        SectionError(reason = SectionErrorReason.VersionNotFound, loadFailedMessage = "Failed to load permissions", onRetry = {})
    }
}

@Preview
@Composable
private fun RecordedVersionReadFailedPreview() {
    ApkAnalyzerTheme {
        SectionError(reason = SectionErrorReason.VersionReadFailed, loadFailedMessage = "Failed to load permissions", onRetry = {})
    }
}

@Preview
@Composable
private fun SectionLoadingPreview() {
    ApkAnalyzerTheme {
        SectionLoading()
    }
}

@Preview
@Composable
private fun SectionErrorPreview() {
    ApkAnalyzerTheme {
        SectionError(message = "Failed to load general information")
    }
}

@Preview
@Composable
private fun SectionErrorRetryPreview() {
    ApkAnalyzerTheme {
        SectionError(message = "Failed to load permissions", onRetry = {})
    }
}
