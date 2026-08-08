package sk.styk.martin.apkanalyzer.feature.apps.impl.components.apkfilepicker

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.uilibrary.components.IconButton
import sk.styk.martin.apkanalyzer.core.uilibrary.components.IconButtonStyle
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.feature.apps.impl.R

@Composable
internal fun ApkFilePickerButton(onApkDetails: (String) -> Unit, modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) {
        ApkFilePickerButtonContent(
            enabled = true,
            onClick = {},
            modifier = modifier,
        )
    } else {
        ApkFilePickerButtonWithViewModel(
            onApkDetails = onApkDetails,
            modifier = modifier,
        )
    }
}

@Composable
private fun ApkFilePickerButtonWithViewModel(
    onApkDetails: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ApkFilePickerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val taskId = context.requireActivity().taskId
    val apkFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { viewModel.onAction(ApkFilePickerAction.ApkSelected(it, taskId)) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ApkFilePickerEvent.OpenDocument -> apkFileLauncher.launch(arrayOf(APK_MIME_TYPE))

                is ApkFilePickerEvent.OpenApkDetail -> {
                    onApkDetails(event.apkFilePath)
                    viewModel.onAction(ApkFilePickerAction.ApkDetailOpened(event.apkFilePath))
                }

                ApkFilePickerEvent.ShowOpenError -> {
                    Toast.makeText(context, R.string.apps_apk_open_error, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    ApkFilePickerButtonContent(
        enabled = state == ApkFilePickerState.Ready,
        onClick = { viewModel.onAction(ApkFilePickerAction.OpenPicker) },
        modifier = modifier,
    )
}

@Composable
internal fun ApkFilePickerButtonContent(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        imageVector = ApkAnalyzerIcons.FileUpload,
        style = IconButtonStyle.Filled,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentDescription = stringResource(R.string.content_description_analyze_apk),
    )
}

@Preview
@Composable
private fun ApkFilePickerButtonReadyPreview() {
    ApkAnalyzerTheme {
        ApkFilePickerButtonContent(
            enabled = true,
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun ApkFilePickerButtonCopyingPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        ApkFilePickerButtonContent(
            enabled = false,
            onClick = {},
        )
    }
}

private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

private fun Context.requireActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.requireActivity()
    else -> error("APK file picker requires an Activity context")
}
