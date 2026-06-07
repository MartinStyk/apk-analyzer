package sk.styk.martin.apkanalyzer.feature.appdetail.impl

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetailData
import sk.styk.martin.apkanalyzer.core.uilibrary.components.AppIcon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.LoadingSpinner
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.components.TextButton
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Toolbar
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun AppDetailScreen(
    appDetailInput: AppDetailInput,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppDetailViewModel = hiltViewModel { factory: AppDetailViewModel.Factory ->
        factory.create(appDetailInput)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AppDetailContent(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun AppDetailContent(
    state: AppDetailState,
    onAction: (AppDetailAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        Toolbar(
            title = when (state) {
                is AppDetailState.Loaded -> state.appName
                else -> stringResource(R.string.app_detail_title)
            },
            onBack = onBack,
        )

        when (state) {
            is AppDetailState.Loading -> LoadingContent()
            is AppDetailState.Loaded -> LoadedContent(state = state)
            is AppDetailState.Error -> ErrorContent(onAction = onAction)
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        LoadingSpinner()
    }
}

@Composable
private fun ErrorContent(onAction: (AppDetailAction) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.app_detail_error),
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colors.onBackground,
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(
            text = stringResource(R.string.app_detail_retry),
            onClick = { onAction(AppDetailAction.Retry) },
        )
    }
}

@Composable
private fun LoadedContent(state: AppDetailState.Loaded, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        AppHeader(state = state)
        Spacer(modifier = Modifier.height(24.dp))
        GeneralSection(state = state)
        Spacer(modifier = Modifier.height(16.dp))
        SdkSection(state = state)
        Spacer(modifier = Modifier.height(16.dp))
        CertificateSection(state = state)
        Spacer(modifier = Modifier.height(16.dp))
        ComponentsSection(state = state)
        Spacer(modifier = Modifier.height(16.dp))
        PermissionsSection(state = state)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AppHeader(state: AppDetailState.Loaded, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        AppIcon(
            packageName = state.packageName,
            size = 64.dp,
        )
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f),
        ) {
            Text(
                text = state.appName,
                style = AppTheme.typography.headlineSmall,
                color = AppTheme.colors.onBackground,
            )
            Text(
                text = state.packageName,
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant,
            )
            if (state.versionName != null) {
                Text(
                    text = "${state.versionName} (${state.versionCode})",
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = AppTheme.typography.titleMedium,
        color = AppTheme.colors.primary,
        modifier = modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.weight(0.4f),
        )
        Text(
            text = value,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onBackground,
            modifier = Modifier.weight(0.6f),
        )
    }
}

@Composable
private fun SectionCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.CardShape)
            .background(AppTheme.colors.surface)
            .padding(16.dp),
    ) {
        content()
    }
}

@Composable
private fun GeneralSection(state: AppDetailState.Loaded, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    SectionCard(modifier = modifier) {
        SectionHeader(title = stringResource(R.string.app_detail_general_section))
        DetailRow(
            label = stringResource(R.string.app_detail_package_name),
            value = state.packageName,
        )
        DetailRow(
            label = stringResource(R.string.app_detail_system_app),
            value = stringResource(if (state.isSystemApp) R.string.app_detail_yes else R.string.app_detail_no),
        )
        DetailRow(
            label = stringResource(R.string.app_detail_install_source),
            value = state.source,
        )
        DetailRow(
            label = stringResource(R.string.app_detail_apk_size),
            value = Formatter.formatShortFileSize(context, state.apkSize),
        )
        state.apkDirectory?.let {
            DetailRow(
                label = stringResource(R.string.app_detail_apk_directory),
                value = it,
            )
        }
        DetailRow(
            label = stringResource(R.string.app_detail_install_location),
            value = state.installLocation,
        )
        state.appInstaller?.let {
            DetailRow(
                label = stringResource(R.string.app_detail_app_installer),
                value = it,
            )
        }
        state.firstInstallTime?.let {
            DetailRow(
                label = stringResource(R.string.app_detail_first_install),
                value = formatTimestamp(it),
            )
        }
        state.lastUpdateTime?.let {
            DetailRow(
                label = stringResource(R.string.app_detail_last_update),
                value = formatTimestamp(it),
            )
        }
    }
}

@Composable
private fun SdkSection(state: AppDetailState.Loaded, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier) {
        SectionHeader(title = stringResource(R.string.app_detail_sdk_section))
        state.minSdkVersion?.let { sdk ->
            DetailRow(
                label = stringResource(R.string.app_detail_min_sdk),
                value = "$sdk${state.minSdkLabel?.let { " ($it)" } ?: ""}",
            )
        }
        state.targetSdkVersion?.let { sdk ->
            DetailRow(
                label = stringResource(R.string.app_detail_target_sdk),
                value = "$sdk${state.targetSdkLabel?.let { " ($it)" } ?: ""}",
            )
        }
    }
}

@Composable
private fun CertificateSection(state: AppDetailState.Loaded, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier) {
        SectionHeader(title = stringResource(R.string.app_detail_certificate_section))
        state.signAlgorithms.forEach { algorithm ->
            DetailRow(
                label = stringResource(R.string.app_detail_sign_algorithm),
                value = algorithm,
            )
        }
    }
}

@Composable
private fun ComponentsSection(state: AppDetailState.Loaded, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier) {
        SectionHeader(title = stringResource(R.string.app_detail_components_section))
        DetailRow(
            label = stringResource(R.string.app_detail_activities),
            value = state.activitiesCount.toString(),
        )
        DetailRow(
            label = stringResource(R.string.app_detail_services),
            value = state.servicesCount.toString(),
        )
        DetailRow(
            label = stringResource(R.string.app_detail_content_providers),
            value = state.contentProvidersCount.toString(),
        )
        DetailRow(
            label = stringResource(R.string.app_detail_broadcast_receivers),
            value = state.broadcastReceiversCount.toString(),
        )
        DetailRow(
            label = stringResource(R.string.app_detail_features),
            value = state.featuresCount.toString(),
        )
    }
}

@Composable
private fun PermissionsSection(state: AppDetailState.Loaded, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier) {
        SectionHeader(title = stringResource(R.string.app_detail_permissions_section))
        DetailRow(
            label = stringResource(R.string.app_detail_defined_permissions),
            value = state.definedPermissionsCount.toString(),
        )
        DetailRow(
            label = stringResource(R.string.app_detail_used_permissions),
            value = state.usedPermissionsCount.toString(),
        )
        if (state.usedPermissions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            state.usedPermissions.forEach { permission ->
                Text(
                    text = "• $permission",
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp),
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

@Preview
@Composable
private fun AppDetailLoadingPreview() {
    ApkAnalyzerTheme {
        AppDetailContent(
            state = AppDetailState.Loading,
            onAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun AppDetailErrorPreview() {
    ApkAnalyzerTheme {
        AppDetailContent(
            state = AppDetailState.Error,
            onAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun AppDetailLoadedPreview() {
    ApkAnalyzerTheme {
        AppDetailContent(
            state = sampleLoadedState(),
            onAction = {},
            onBack = {},
        )
    }
}

private fun sampleLoadedState() = AppDetailState.Loaded(
    analysisMode = AppDetailData.AnalysisMode.INSTALLED_PACKAGE,
    appName = "Sample App",
    packageName = "com.example.sampleapp",
    versionName = "1.2.3",
    versionCode = 42,
    isSystemApp = false,
    source = "GooglePlay",
    apkDirectory = "/data/app/com.example.sampleapp/base.apk",
    dataDirectory = "/data/data/com.example.sampleapp",
    apkSize = 15_000_000,
    minSdkVersion = 24,
    minSdkLabel = "Android 7.0",
    targetSdkVersion = 34,
    targetSdkLabel = "Android 14",
    installLocation = "InternalOnly",
    appInstaller = "com.android.vending",
    firstInstallTime = 1_700_000_000_000,
    lastUpdateTime = 1_710_000_000_000,
    signAlgorithms = persistentListOf("SHA256withRSA"),
    activitiesCount = 12,
    servicesCount = 3,
    contentProvidersCount = 2,
    broadcastReceiversCount = 5,
    definedPermissionsCount = 1,
    usedPermissionsCount = 8,
    featuresCount = 2,
    usedPermissions = persistentListOf(
        "INTERNET",
        "ACCESS_FINE_LOCATION",
        "CAMERA",
        "READ_CONTACTS",
        "WRITE_EXTERNAL_STORAGE",
        "VIBRATE",
        "RECEIVE_BOOT_COMPLETED",
        "WAKE_LOCK",
    ),
)
