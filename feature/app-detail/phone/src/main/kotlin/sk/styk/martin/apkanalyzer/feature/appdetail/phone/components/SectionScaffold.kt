package sk.styk.martin.apkanalyzer.feature.appdetail.phone.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.R
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.RecordedVersionFailure

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
internal fun RecordedVersionError(
    failure: RecordedVersionFailure,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (failure) {
        RecordedVersionFailure.NotFound -> SectionError(
            message = stringResource(R.string.app_history_version_not_found),
            modifier = modifier,
        )

        RecordedVersionFailure.ReadFailed -> SectionError(
            message = stringResource(R.string.app_history_version_load_failed),
            modifier = modifier,
            onRetry = onRetry,
        )
    }
}

@Preview
@Composable
private fun RecordedVersionNotFoundPreview() {
    ApkAnalyzerTheme {
        RecordedVersionError(failure = RecordedVersionFailure.NotFound, onRetry = {})
    }
}

@Preview
@Composable
private fun RecordedVersionReadFailedPreview() {
    ApkAnalyzerTheme {
        RecordedVersionError(failure = RecordedVersionFailure.ReadFailed, onRetry = {})
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
