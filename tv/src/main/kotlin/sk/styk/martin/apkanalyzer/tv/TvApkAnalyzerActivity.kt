package sk.styk.martin.apkanalyzer.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import sk.styk.martin.apkanalyzer.core.common.settings.FingerprintFormat
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.LocalFingerprintFormat
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme

@AndroidEntryPoint
class TvApkAnalyzerActivity : ComponentActivity() {
    private val viewModel: TvApkAnalyzerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ApkAnalyzerTvTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val fingerprintFormat = when (val current = state) {
                    TvApkAnalyzerState.Loading -> FingerprintFormat.ColonSeparated
                    is TvApkAnalyzerState.Data -> current.fingerprintFormat
                }
                CompositionLocalProvider(LocalFingerprintFormat provides fingerprintFormat) {
                    TvApkAnalyzerApp(onBackAtRoot = ::finish)
                }
            }
        }
    }
}
