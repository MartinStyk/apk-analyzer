package sk.styk.martin.apkanalyzer.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import sk.styk.martin.apkanalyzer.core.common.review.InAppReviewLauncher
import javax.inject.Inject

@AndroidEntryPoint
class ApkAnalyzerActivity : ComponentActivity() {
    private val viewModel: ApkAnalyzerViewModel by viewModels()

    @Inject
    lateinit var inAppReviewLauncher: InAppReviewLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installAnimatedSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            splashScreen.KeepOnScreenWhile(state is ApkAnalyzerState.Loading)
            LaunchedEffect(Unit) {
                viewModel.events.collect { event ->
                    when (event) {
                        ApkAnalyzerEvent.RequestReview -> inAppReviewLauncher.launchReviewFlow(this@ApkAnalyzerActivity)
                    }
                }
            }
            ApkAnalyzerThemeHost(state = state) {
                ApkAnalyzerApp(onBackAtRoot = ::finish)
            }
        }
    }
}
