package sk.styk.martin.apkanalyzer.feature.appexport.phone

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.appexport.format.InventoryFormat
import sk.styk.martin.apkanalyzer.core.common.util.openBrowser
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.animation.NAV_DURATION_STANDARD
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Button
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.ConfirmDialog
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.IconButton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.launcher.launchSafely
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.feature.appexport.phone.apps.AppSelectionState
import sk.styk.martin.apkanalyzer.feature.appexport.phone.apps.AppSelectionSummary
import sk.styk.martin.apkanalyzer.feature.appexport.phone.apps.AppsPage
import sk.styk.martin.apkanalyzer.feature.appexport.phone.columns.ColumnsPage
import sk.styk.martin.apkanalyzer.feature.appexport.phone.components.StepScaffold
import sk.styk.martin.apkanalyzer.feature.appexport.phone.export.ExportBottomBar
import sk.styk.martin.apkanalyzer.feature.appexport.phone.export.ExportPage

@Composable
internal fun InventoryExportScreen(
    scope: InventoryExportScope,
    onBack: () -> Unit,
    onEditFilters: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InventoryExportViewModel = hiltViewModel { factory: InventoryExportViewModel.Factory ->
        factory.create(scope)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val csvDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(InventoryFormat.Csv.mimeType),
    ) { destination ->
        if (destination != null) viewModel.onAction(InventoryExportAction.SaveTo(destination, InventoryFormat.Csv))
    }
    val jsonDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(InventoryFormat.Json.mimeType),
    ) { destination ->
        if (destination != null) viewModel.onAction(InventoryExportAction.SaveTo(destination, InventoryFormat.Json))
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                InventoryExportEvent.NavigateBack -> onBack()

                InventoryExportEvent.NavigateToFilter -> onEditFilters()

                InventoryExportEvent.OpenUsageAccessSettings -> context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))

                is InventoryExportEvent.OpenFormatReference -> context.openBrowser(event.url)

                is InventoryExportEvent.CreateDocument -> {
                    val launcher = when (event.format) {
                        InventoryFormat.Csv -> csvDocumentLauncher
                        InventoryFormat.Json -> jsonDocumentLauncher
                    }
                    launcher.launchSafely(event.fileName) {
                        viewModel.onAction(InventoryExportAction.DocumentPickerUnavailable)
                    }
                }

                is InventoryExportEvent.ShareFile -> try {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = event.mimeType
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        clipData = ClipData.newRawUri(null, event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, null))
                } catch (_: ActivityNotFoundException) {
                    viewModel.onAction(InventoryExportAction.ShareUnavailable)
                }

                is InventoryExportEvent.ShowFeedback ->
                    Toast.makeText(context, event.feedback.message(context), Toast.LENGTH_LONG).show()
            }
        }
    }

    BackHandler(enabled = state.isExporting) {
        viewModel.onAction(InventoryExportAction.Back)
    }

    InventoryExportContent(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@OptIn(ExperimentalTransitionApi::class)
@Composable
private fun InventoryExportContent(
    state: InventoryExportState,
    onAction: (InventoryExportAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.showStopConfirmation) {
        ConfirmDialog(
            title = stringResource(R.string.inventory_export_stop_title),
            message = stringResource(R.string.inventory_export_stop_message),
            confirmText = stringResource(R.string.inventory_export_stop_and_back),
            dismissText = stringResource(R.string.inventory_export_keep_exporting),
            onConfirm = { onAction(InventoryExportAction.ConfirmStopAndBack) },
            onDismiss = { onAction(InventoryExportAction.DismissStopConfirmation) },
        )
    }
    val transitionState = remember { SeekableTransitionState(state.step) }
    val transition = rememberTransition(transitionState, label = "inventory-export-step")

    LaunchedEffect(state.step) {
        if (transitionState.currentState != state.step || transitionState.targetState != state.step) {
            transitionState.animateTo(state.step)
        }
    }
    PredictiveBackHandler(enabled = state.previousStep != null && !state.isExporting) { progress ->
        val previous = checkNotNull(state.previousStep)
        var committed = false
        try {
            progress.collect { transitionState.seekTo(it.progress, previous) }
            committed = true
            onAction(InventoryExportAction.Back)
        } finally {
            if (!committed) withContext(NonCancellable) { transitionState.snapTo(state.step) }
        }
    }

    StepScaffold(
        currentStep = state.step,
        steps = state.steps,
        canSelectStep = !state.isExporting,
        onSelectStep = { onAction(InventoryExportAction.GoToStep(it)) },
        onBack = { onAction(InventoryExportAction.Back) },
        bottomBar = { InventoryExportBottomBar(state = state, onAction = onAction) },
        modifier = modifier,
        actions = {
            if (state.step == InventoryExportStep.Apps && !state.showIntro) {
                IconButton(
                    imageVector = ApkAnalyzerIcons.Info,
                    onClick = { onAction(InventoryExportAction.ShowIntro) },
                    contentDescription = stringResource(R.string.inventory_export_intro_about),
                )
            }
        },
    ) {
        transition.AnimatedContent(
            transitionSpec = {
                val enterTween = tween<IntOffset>(NAV_DURATION_STANDARD, easing = LinearOutSlowInEasing)
                val exitTween = tween<IntOffset>(NAV_DURATION_STANDARD, easing = FastOutLinearInEasing)
                if (targetState > initialState) {
                    slideInHorizontally(enterTween) { it } togetherWith slideOutHorizontally(exitTween) { -it / 3 }
                } else {
                    slideInHorizontally(enterTween) { -it / 3 } togetherWith slideOutHorizontally(exitTween) { it }
                }
            },
        ) { step ->
            val pageModifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.background)
            when (step) {
                InventoryExportStep.Export -> ExportPage(state = state, onAction = onAction, modifier = pageModifier)
                InventoryExportStep.Apps -> AppsPage(state = state, onAction = onAction, modifier = pageModifier)
                InventoryExportStep.Columns -> ColumnsPage(state = state, onAction = onAction, modifier = pageModifier)
            }
        }
    }
}

@Composable
private fun InventoryExportBottomBar(state: InventoryExportState, onAction: (InventoryExportAction) -> Unit) {
    when (state.step) {
        InventoryExportStep.Export -> ExportBottomBar(state = state, onAction = onAction)

        InventoryExportStep.Apps, InventoryExportStep.Columns -> Button(
            text = stringResource(R.string.inventory_export_next),
            onClick = { onAction(InventoryExportAction.Next) },
            enabled = state.canProceed,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun InventoryExportFeedback.message(context: Context): String {
    val resources = context.resources
    return when (this) {
        is InventoryExportFeedback.Saved -> {
            val saved = resources.getQuantityString(R.plurals.inventory_export_saved, appCount, appCount, displayName)
            if (incompleteAppCount > 0) {
                saved + "\n" + resources.getQuantityString(R.plurals.inventory_export_incomplete, incompleteAppCount, incompleteAppCount)
            } else {
                saved
            }
        }

        is InventoryExportFeedback.SharedWithGaps ->
            resources.getQuantityString(R.plurals.inventory_export_incomplete, incompleteAppCount, incompleteAppCount)

        InventoryExportFeedback.Failed -> context.getString(R.string.inventory_export_failed)

        InventoryExportFeedback.DocumentPickerUnavailable -> context.getString(R.string.inventory_export_picker_unavailable)

        InventoryExportFeedback.ShareUnavailable -> context.getString(R.string.inventory_export_share_unavailable)
    }
}

private val previewApps = AppSelectionState(
    summary = AppSelectionSummary(isLoaded = true, candidateCount = 142, selectedCount = 138, isFilterActive = true),
)

@Preview
@Composable
private fun InventoryExportContentPreview() {
    ApkAnalyzerTheme {
        InventoryExportContent(
            state = InventoryExportState(
                step = InventoryExportStep.Export,
                apps = previewApps,
                preview = InventoryPreviewState.Content(
                    text = "application_name,package_name,version_name\r\nChrome,com.android.chrome,126.0\r\nMaps,com.google.android.apps.maps,11.1",
                    previewedAppCount = 3,
                    isTruncated = true,
                ),
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun InventoryExportContentExportingPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        InventoryExportContent(
            state = InventoryExportState(
                step = InventoryExportStep.Export,
                apps = previewApps,
                progress = InventoryExportProgressState.Running(processedApps = 57, totalApps = 138),
            ),
            onAction = {},
        )
    }
}
