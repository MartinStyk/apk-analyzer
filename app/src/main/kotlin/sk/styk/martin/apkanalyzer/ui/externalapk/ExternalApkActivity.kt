package sk.styk.martin.apkanalyzer.ui.externalapk

import android.content.ContentResolver
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import sk.styk.martin.apkanalyzer.ui.ApkAnalyzerThemeHost
import sk.styk.martin.apkanalyzer.ui.ApkAnalyzerViewModel

@AndroidEntryPoint
class ExternalApkActivity : ComponentActivity() {
    private val appViewModel: ApkAnalyzerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sourceUri = intent.externalApkUri()
        if (sourceUri == null) {
            finishAndRemoveTask()
            return
        }

        enableEdgeToEdge()
        setContent {
            val appState by appViewModel.state.collectAsStateWithLifecycle()
            ApkAnalyzerThemeHost(state = appState) {
                ExternalApkApp(
                    sourceUri = sourceUri,
                    taskId = taskId,
                    onClose = ::finishAndRemoveTask,
                )
            }
        }
    }
}

@Suppress("DEPRECATION", "ReturnCount")
private fun Intent.externalApkUri(): String? {
    if (action != Intent.ACTION_VIEW && action != Intent.ACTION_INSTALL_PACKAGE) return null
    val uri = data ?: return null
    return uri
        .takeIf { it.scheme == ContentResolver.SCHEME_CONTENT || it.scheme == ContentResolver.SCHEME_FILE }
        ?.toString()
}
