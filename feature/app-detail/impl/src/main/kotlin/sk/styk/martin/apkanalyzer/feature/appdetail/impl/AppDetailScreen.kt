package sk.styk.martin.apkanalyzer.feature.appdetail.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.uilibrary.components.AppIcon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Icon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.IconButton
import sk.styk.martin.apkanalyzer.core.uilibrary.components.LoadingSpinner
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.components.TextButton
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Toolbar
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.CollapsingToolbarState
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.collapsingToolbarScroll
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.rememberCollapsingToolbarState
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
    onOpenPlayStore: (packageName: String) -> Unit,
    onOpenAppInfo: (packageName: String) -> Unit,
    onExportApk: (packageName: String) -> Unit,
    onSaveIcon: (packageName: String) -> Unit,
    onNavigateToManifest: () -> Unit,
    onNavigateToGeneralDetails: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToActivities: () -> Unit,
    onNavigateToServices: () -> Unit,
    onNavigateToReceivers: () -> Unit,
    onNavigateToProviders: () -> Unit,
    onNavigateToCertificates: () -> Unit,
    onNavigateToFeatures: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppDetailViewModel = hiltViewModel { factory: AppDetailViewModel.Factory ->
        factory.create(appDetailInput)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AppDetailEvent.OpenPlayStore -> onOpenPlayStore(event.packageName)
                is AppDetailEvent.OpenAppInfo -> onOpenAppInfo(event.packageName)
                is AppDetailEvent.ExportApk -> onExportApk(event.packageName)
                is AppDetailEvent.SaveIcon -> onSaveIcon(event.packageName)
                is AppDetailEvent.NavigateToManifest -> onNavigateToManifest()
                is AppDetailEvent.NavigateToGeneralDetails -> onNavigateToGeneralDetails()
                is AppDetailEvent.NavigateToPermissions -> onNavigateToPermissions()
                is AppDetailEvent.NavigateToActivities -> onNavigateToActivities()
                is AppDetailEvent.NavigateToServices -> onNavigateToServices()
                is AppDetailEvent.NavigateToReceivers -> onNavigateToReceivers()
                is AppDetailEvent.NavigateToProviders -> onNavigateToProviders()
                is AppDetailEvent.NavigateToCertificates -> onNavigateToCertificates()
                is AppDetailEvent.NavigateToFeatures -> onNavigateToFeatures()
            }
        }
    }

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
        when (state) {
            is AppDetailState.Loading -> {
                Toolbar(
                    title = stringResource(R.string.app_detail_title),
                    onBack = onBack,
                )
                LoadingContent()
            }

            is AppDetailState.Error -> {
                Toolbar(
                    title = stringResource(R.string.app_detail_title),
                    onBack = onBack,
                )
                ErrorContent(onAction = onAction)
            }

            is AppDetailState.Loaded -> {
                LoadedContent(
                    state = state,
                    onAction = onAction,
                    onBack = onBack,
                )
            }
        }
    }
}

private val ICON_SIZE_EXPANDED = 72.dp
private val ICON_SIZE_COLLAPSED = 32.dp

@Composable
private fun LoadedContent(
    state: AppDetailState.Loaded,
    onAction: (AppDetailAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val collapsingState = rememberCollapsingToolbarState()

    Column(modifier = modifier.fillMaxSize()) {
        AppDetailToolbar(
            state = state,
            collapsingState = collapsingState,
            onBack = onBack,
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .collapsingToolbarScroll(collapsingState),
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item { OverviewSection(state = state, onAction = onAction) }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item { ActionsSection(onAction = onAction) }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item { PermissionsSection(state = state, onAction = onAction) }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item { ComponentsSection(state = state, onAction = onAction) }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item { CertificatesSection(state = state, onAction = onAction) }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item { FeaturesSection(state = state, onAction = onAction) }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}


private val BACK_BUTTON_SIZE = 48.dp
private val TOOLBAR_PADDING_VERTICAL = 12.dp
private val TOOLBAR_PADDING_START = 4.dp

@Composable
private fun AppDetailToolbar(
    state: AppDetailState.Loaded,
    collapsingState: CollapsingToolbarState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val progress = collapsingState.progress
    val iconSize = lerp(ICON_SIZE_EXPANDED, ICON_SIZE_COLLAPSED, progress)

    val backButtonEndX = TOOLBAR_PADDING_START + BACK_BUTTON_SIZE
    val backButtonCenterY = TOOLBAR_PADDING_VERTICAL + BACK_BUTTON_SIZE / 2

    // Expanded: icon below back button, left-aligned with horizontal padding
    val expandedIconX = 16.dp
    val expandedIconY = TOOLBAR_PADDING_VERTICAL + BACK_BUTTON_SIZE + 8.dp

    // Collapsed: icon next to back button, vertically centered with it
    val collapsedIconX = backButtonEndX
    val collapsedIconY = backButtonCenterY - ICON_SIZE_COLLAPSED / 2

    val iconX = lerp(expandedIconX, collapsedIconX, progress)
    val iconY = lerp(expandedIconY, collapsedIconY, progress)
    val iconCenterY = iconY + iconSize / 2

    // Texts positioned to the right of icon, vertically centered on it
    val textX = iconX + iconSize + lerp(16.dp, 8.dp, progress)
    val appNameHeight = 24.dp
    val packageNameHeight = 20.dp
    val packageNameSpacing = 4.dp
    val expandedTextsBlockHeight = appNameHeight + packageNameSpacing + packageNameHeight
    val collapsedTextsBlockHeight = appNameHeight

    val textsBlockHeight = lerp(expandedTextsBlockHeight, collapsedTextsBlockHeight, progress)
    val appNameY = iconCenterY - textsBlockHeight / 2
    val packageNameY = appNameY + appNameHeight + packageNameSpacing

    val expandedTotalHeight = expandedIconY + ICON_SIZE_EXPANDED + 16.dp
    val collapsedTotalHeight = TOOLBAR_PADDING_VERTICAL * 2 + BACK_BUTTON_SIZE
    val totalHeight = lerp(expandedTotalHeight, collapsedTotalHeight, progress)

    LaunchedEffect(Unit) {
        with(density) {
            collapsingState.maxCollapsePx = (expandedTotalHeight - collapsedTotalHeight).toPx()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight)
            .background(AppTheme.colors.background),
    ) {
        IconButton(
            imageVector = ApkAnalyzerIcons.Back,
            onClick = onBack,
            modifier = Modifier.offset(x = TOOLBAR_PADDING_START, y = TOOLBAR_PADDING_VERTICAL),
        )

        AppIcon(
            packageName = state.packageName,
            size = iconSize,
            modifier = Modifier.offset(x = iconX, y = iconY),
        )

        Text(
            text = state.appName,
            style = AppTheme.typography.titleLarge,
            color = AppTheme.colors.onBackground,
            modifier = Modifier.offset(x = textX, y = appNameY),
        )

        Text(
            text = state.packageName,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurfaceVariant,
            modifier = Modifier
                .offset(x = textX, y = packageNameY)
                .graphicsLayer { alpha = (1f - progress).coerceIn(0f, 1f) },
        )
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
private fun NavigableRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.weight(0.4f),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(0.6f),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = value,
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onBackground,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = ApkAnalyzerIcons.ChevronRight,
                tint = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SectionCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(Shapes.CardShape)
            .background(AppTheme.colors.surface)
            .padding(16.dp),
    ) {
        content()
    }
}

@Composable
private fun OverviewSection(
    state: AppDetailState.Loaded,
    onAction: (AppDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val versionValue = buildString {
        append(state.versionName ?: stringResource(R.string.app_detail_unknown))
        append(" (${state.versionCode})")
    }
    val sizeLabel = if (state.totalSize != null) {
        stringResource(R.string.app_detail_total_size)
    } else {
        stringResource(R.string.app_detail_apk_size)
    }
    val sizeValue = AppSize(state.totalSize ?: state.apkSize).formatted()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(Shapes.CardShape)
            .background(AppTheme.colors.surface)
            .clickable { onAction(AppDetailAction.NavigateGeneralDetails) }
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionHeader(title = stringResource(R.string.app_detail_overview_section))
            Icon(
                imageVector = ApkAnalyzerIcons.ChevronRight,
                tint = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OverviewGridCell(
                label = stringResource(R.string.app_detail_version),
                value = versionValue,
                modifier = Modifier.weight(1f),
            )
            OverviewGridCell(
                label = sizeLabel,
                value = sizeValue,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OverviewGridCell(
                label = stringResource(R.string.app_detail_target_sdk),
                value = state.targetSdkVersion?.let { "API $it" }
                    ?: stringResource(R.string.app_detail_unknown),
                valueColor = if (state.isTargetSdkOutdated) AppTheme.colors.warning else AppTheme.colors.onBackground,
                modifier = Modifier.weight(1f),
            )
            OverviewGridCell(
                label = stringResource(R.string.app_detail_updated),
                value = state.lastUpdateTime?.let { formatTimestamp(it) }
                    ?: stringResource(R.string.app_detail_unknown),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun OverviewGridCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = AppTheme.colors.onBackground,
) {
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Text(
            text = label.uppercase(),
            style = AppTheme.typography.labelSmall,
            color = AppTheme.colors.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = AppTheme.typography.titleSmall,
            color = valueColor,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionsSection(onAction: (AppDetailAction) -> Unit, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier) {
        SectionHeader(title = stringResource(R.string.app_detail_actions_section))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2,
        ) {
            ActionItem(
                icon = ApkAnalyzerIcons.File,
                label = stringResource(R.string.app_detail_action_manifest),
                onClick = { onAction(AppDetailAction.ViewManifest) },
                modifier = Modifier.weight(1f),
            )
            ActionItem(
                icon = ApkAnalyzerIcons.Storage,
                label = stringResource(R.string.app_detail_action_export_apk),
                onClick = { onAction(AppDetailAction.ExportApk) },
                modifier = Modifier.weight(1f),
            )
            ActionItem(
                icon = ApkAnalyzerIcons.Android,
                label = stringResource(R.string.app_detail_action_save_icon),
                onClick = { onAction(AppDetailAction.SaveIcon) },
                modifier = Modifier.weight(1f),
            )
            ActionItem(
                icon = ApkAnalyzerIcons.Apps,
                label = stringResource(R.string.app_detail_action_play_store),
                onClick = { onAction(AppDetailAction.OpenPlayStore) },
                modifier = Modifier.weight(1f),
            )
            ActionItem(
                icon = ApkAnalyzerIcons.Settings,
                label = stringResource(R.string.app_detail_action_app_info),
                onClick = { onAction(AppDetailAction.OpenAppInfo) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(Shapes.CardShape)
            .clickable(onClick = onClick)
            .background(AppTheme.colors.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Icon(
            imageVector = icon,
            tint = AppTheme.colors.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = AppTheme.typography.labelLarge,
            color = AppTheme.colors.onBackground,
            maxLines = 1,
        )
    }
}

@Composable
private fun PermissionsSection(
    state: AppDetailState.Loaded,
    onAction: (AppDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier) {
        SectionHeader(title = stringResource(R.string.app_detail_permissions_section))
        DetailRow(
            label = stringResource(R.string.app_detail_total_permissions),
            value = state.totalPermissionsCount.toString(),
        )
        DetailRow(
            label = stringResource(R.string.app_detail_dangerous_permissions),
            value = state.dangerousPermissionsCount.toString(),
        )
        Spacer(modifier = Modifier.height(4.dp))
        NavigableRow(
            label = stringResource(R.string.app_detail_view_permissions),
            value = "",
            onClick = { onAction(AppDetailAction.NavigatePermissions) },
        )
    }
}

@Composable
private fun ComponentsSection(
    state: AppDetailState.Loaded,
    onAction: (AppDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier) {
        SectionHeader(title = stringResource(R.string.app_detail_components_section))
        NavigableRow(
            label = stringResource(R.string.app_detail_activities),
            value = state.activitiesCount.toString(),
            onClick = { onAction(AppDetailAction.NavigateActivities) },
        )
        NavigableRow(
            label = stringResource(R.string.app_detail_services),
            value = state.servicesCount.toString(),
            onClick = { onAction(AppDetailAction.NavigateServices) },
        )
        NavigableRow(
            label = stringResource(R.string.app_detail_broadcast_receivers),
            value = state.broadcastReceiversCount.toString(),
            onClick = { onAction(AppDetailAction.NavigateReceivers) },
        )
        NavigableRow(
            label = stringResource(R.string.app_detail_content_providers),
            value = state.contentProvidersCount.toString(),
            onClick = { onAction(AppDetailAction.NavigateProviders) },
        )
    }
}

@Composable
private fun CertificatesSection(
    state: AppDetailState.Loaded,
    onAction: (AppDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier) {
        SectionHeader(title = stringResource(R.string.app_detail_certificates_section))
        DetailRow(
            label = stringResource(R.string.app_detail_certificates_count),
            value = state.certificatesCount.toString(),
        )
        Spacer(modifier = Modifier.height(4.dp))
        NavigableRow(
            label = stringResource(R.string.app_detail_view_details),
            value = "",
            onClick = { onAction(AppDetailAction.NavigateCertificates) },
        )
    }
}

@Composable
private fun FeaturesSection(
    state: AppDetailState.Loaded,
    onAction: (AppDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier) {
        SectionHeader(title = stringResource(R.string.app_detail_features_section))
        DetailRow(
            label = stringResource(R.string.app_detail_features_count),
            value = state.featuresCount.toString(),
        )
        Spacer(modifier = Modifier.height(4.dp))
        NavigableRow(
            label = stringResource(R.string.app_detail_view_details),
            value = "",
            onClick = { onAction(AppDetailAction.NavigateFeatures) },
        )
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

private fun formatTimestamp(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
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
    analysisMode = AppDetail.AnalysisMode.InstalledPackage,
    appName = "Spotify",
    packageName = "com.spotify.music",
    processName = "com.spotify.music",
    versionName = "9.4.12",
    versionCode = 90412,
    uid = 10234,
    description = null,
    isSystemApp = false,
    source = "GooglePlay",
    apkDirectory = "/data/app/com.spotify.music/base.apk",
    dataDirectory = "/data/data/com.spotify.music",
    apkSize = 152_000_000,
    totalSize = 534_773_760,
    targetSdkVersion = 35,
    targetSdkLabel = "Android 15",
    minSdkVersion = 24,
    minSdkLabel = "Android 7.0",
    installLocation = "InternalOnly",
    appInstaller = "com.android.vending",
    firstInstallTime = 1_736_640_000_000,
    lastUpdateTime = 1_748_736_000_000,
    totalPermissionsCount = 32,
    dangerousPermissionsCount = 6,
    definedPermissionsCount = 1,
    activitiesCount = 428,
    servicesCount = 57,
    contentProvidersCount = 4,
    broadcastReceiversCount = 89,
    certificatesCount = 2,
    featuresCount = 12,
)
