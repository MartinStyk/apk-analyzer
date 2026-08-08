package sk.styk.martin.apkanalyzer.ui.externalapk

import android.widget.Toast
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import sk.styk.martin.apkanalyzer.R
import sk.styk.martin.apkanalyzer.core.navigation.Navigator
import sk.styk.martin.apkanalyzer.core.navigation.rememberNavigationState
import sk.styk.martin.apkanalyzer.core.navigation.toEntries
import sk.styk.martin.apkanalyzer.core.uilibrary.components.LoadingSpinner
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.LocalSharedTransitionScope
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailNavKey
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.navigation.appDetailEntries

@Composable
internal fun ExternalApkApp(
    sourceUri: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExternalApkViewModel = hiltViewModel { factory: ExternalApkViewModel.Factory ->
        factory.create(sourceUri)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(state) {
        if (state == ExternalApkState.Error) {
            Toast.makeText(context, R.string.external_apk_open_error, Toast.LENGTH_LONG).show()
            onClose()
        }
    }
    when (val currentState = state) {
        ExternalApkState.Loading,
        ExternalApkState.Error,
        -> ExternalApkLoadingScreen(modifier = modifier)

        is ExternalApkState.Loaded -> key(currentState.input.apkFilePath) {
            ExternalApkNavigation(
                input = currentState.input,
                onClose = onClose,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun ExternalApkNavigation(
    input: AppDetailInput.ApkFile,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rootKey = remember(input) { AppDetailNavKey(input) }
    val topLevelKeys = remember(rootKey) { listOf(rootKey) }
    val navigationState = rememberNavigationState(
        startKey = rootKey,
        topLevelKeys = topLevelKeys,
    )
    val navigator = remember(navigationState) {
        Navigator(
            navigationState = navigationState,
            onBackAtRoot = onClose,
        )
    }

    Scaffold(modifier = modifier) { paddings ->
        SharedTransitionLayout {
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                val entries = entryProvider {
                    appDetailEntries(navigator)
                }
                NavDisplay(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddings),
                    entries = navigationState.toEntries(entries),
                    onBack = navigator::goBack,
                )
            }
        }
    }
}

@Composable
private fun ExternalApkLoadingScreen(modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier) { paddings ->
        LoadingSpinner(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings),
        )
    }
}

@Preview
@Composable
private fun ExternalApkLoadingScreenPreview() {
    ApkAnalyzerTheme {
        ExternalApkLoadingScreen()
    }
}
