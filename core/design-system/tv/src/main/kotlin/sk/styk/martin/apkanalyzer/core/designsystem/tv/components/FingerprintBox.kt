package sk.styk.martin.apkanalyzer.core.designsystem.tv.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import sk.styk.martin.apkanalyzer.core.common.digest.toFingerprint
import sk.styk.martin.apkanalyzer.core.common.settings.FingerprintFormat
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.LocalFingerprintFormat
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme

@Composable
fun FingerprintBox(
    value: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    maxLines: Int = Int.MAX_VALUE,
    compact: Boolean = false,
) {
    CodeBox(
        text = value.toFingerprint(LocalFingerprintFormat.current),
        modifier = modifier,
        label = label,
        maxLines = maxLines,
        compact = compact,
    )
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun FingerprintBoxPreview() {
    ApkAnalyzerTvTheme {
        FingerprintBox(value = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun FingerprintBoxContinuousPreview() {
    ApkAnalyzerTvTheme {
        CompositionLocalProvider(LocalFingerprintFormat provides FingerprintFormat.Continuous) {
            FingerprintBox(value = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
        }
    }
}
