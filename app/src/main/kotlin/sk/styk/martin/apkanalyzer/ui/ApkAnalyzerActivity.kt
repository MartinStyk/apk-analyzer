package sk.styk.martin.apkanalyzer.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import sk.styk.martin.apkanalyzer.core.common.settings.ColorAppScheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme

@AndroidEntryPoint
class ApkAnalyzerActivity : ComponentActivity() {
    private val viewModel: ApkAnalyzerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()

            val isDarkTheme =
                when (val s = state) {
                    is ApkAnalyzerState.Loading -> {
                        isSystemInDarkTheme()
                    }

                    is ApkAnalyzerState.Data -> {
                        when (s.colorAppScheme) {
                            ColorAppScheme.Day -> false
                            ColorAppScheme.Night -> true
                            ColorAppScheme.FollowSystem -> isSystemInDarkTheme()
                        }
                    }
                }

            val nightMode =
                when (val s = state) {
                    is ApkAnalyzerState.Loading -> {
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }

                    is ApkAnalyzerState.Data -> {
                        when (s.colorAppScheme) {
                            ColorAppScheme.Day -> AppCompatDelegate.MODE_NIGHT_NO
                            ColorAppScheme.Night -> AppCompatDelegate.MODE_NIGHT_YES
                            ColorAppScheme.FollowSystem -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        }
                    }
                }
            LaunchedEffect(nightMode) {
                AppCompatDelegate.setDefaultNightMode(nightMode)
            }

            ApkAnalyzerTheme(isDarkTheme = isDarkTheme) {
                ApkAnalyzerApp()
            }
        }
    }
}
