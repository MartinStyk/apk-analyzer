@file:Suppress("TooManyFunctions")

package sk.styk.martin.apkanalyzer.feature.appdetail.phone

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryOverview
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.signing.CertificatePrincipal
import sk.styk.martin.apkanalyzer.core.apps.signing.CertificateTrustLevel
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import sk.styk.martin.apkanalyzer.core.common.util.openAppSystemPage
import sk.styk.martin.apkanalyzer.core.common.util.openGooglePlay
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.permissionIcon
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.requirementIcon
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Icon
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.LabeledHashBox
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.LoadingSpinner
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Toolbar
import sk.styk.martin.apkanalyzer.core.designsystem.phone.launcher.launchSafely
import sk.styk.martin.apkanalyzer.core.designsystem.phone.modifier.collapsingToolbarScroll
import sk.styk.martin.apkanalyzer.core.designsystem.phone.modifier.rememberCollapsingToolbarState
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.SectionErrorReason
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.insight.AppDetailInsight
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.insight.SensitiveAccess
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.insight.label
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.isHistorical
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.liveReference
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.recordedAt
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.AppDetailModeFrame
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.AppDetailToolbar
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.AppSummaryBottomSheet
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.SectionError
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.SplitApkExportBottomSheet
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.aisummary.AiSummaryCard
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.historyDate
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import sk.styk.martin.apkanalyzer.core.apps.R as AppsR

private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
private const val ICON_MIME_TYPE = "image/png"

@Suppress("CyclomaticComplexMethod")
@Composable
internal fun AppDetailScreen(
    appDetailInput: AppDetailInput,
    onBack: () -> Unit,
    onNavigateToManifest: () -> Unit,
    onNavigateToGeneralDetails: () -> Unit,
    onNavigateToPermissions: (permissionName: String?) -> Unit,
    onNavigateToComponents: () -> Unit,
    onNavigateToActivities: () -> Unit,
    onNavigateToServices: () -> Unit,
    onNavigateToReceivers: () -> Unit,
    onNavigateToProviders: () -> Unit,
    onNavigateToCertificates: () -> Unit,
    onNavigateToFeatures: () -> Unit,
    onNavigateToInventoryExport: (PackageName) -> Unit,
    onNavigateToHistory: (PackageName, appName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppDetailViewModel = hiltViewModel { factory: AppDetailViewModel.Factory ->
        factory.create(appDetailInput)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var splitApkExportName by rememberSaveable { mutableStateOf<String?>(null) }
    var summaryPreview by rememberSaveable { mutableStateOf<String?>(null) }
    val apkDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(APK_MIME_TYPE),
    ) { destination ->
        if (destination != null) {
            viewModel.onAction(AppDetailAction.ExportApkTo(destination))
        } else {
            viewModel.onAction(AppDetailAction.DocumentPickerCancelled(AppDetailExport.Apk))
        }
    }
    val iconDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ICON_MIME_TYPE),
    ) { destination ->
        if (destination != null) {
            viewModel.onAction(AppDetailAction.SaveIconTo(destination))
        } else {
            viewModel.onAction(AppDetailAction.DocumentPickerCancelled(AppDetailExport.Icon))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AppDetailEvent.OpenPlayStore -> context.openGooglePlay(event.packageName)

                is AppDetailEvent.OpenAppInfo -> context.openAppSystemPage(event.packageName).onFailure {
                    Toast.makeText(context, AppsR.string.app_info_unavailable, Toast.LENGTH_LONG).show()
                }

                is AppDetailEvent.CreateDocument -> {
                    val launcher = when (event.export) {
                        AppDetailExport.Apk -> apkDocumentLauncher
                        AppDetailExport.Icon -> iconDocumentLauncher
                    }
                    launcher.launchSafely(event.suggestedName) {
                        viewModel.onAction(AppDetailAction.DocumentPickerUnavailable(event.export))
                    }
                }

                is AppDetailEvent.ShowSummaryPreview -> summaryPreview = event.text

                is AppDetailEvent.ShareSummary -> {
                    try {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, event.text)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, null))
                    } catch (_: ActivityNotFoundException) {
                        viewModel.onAction(AppDetailAction.ShareSummaryUnavailable)
                    }
                }

                is AppDetailEvent.ShowFeedback -> {
                    val feedback = event.feedback
                    if (feedback is AppDetailFeedback.ApkSaved && feedback.baseApkOnly) {
                        splitApkExportName = feedback.displayName
                    } else {
                        Toast.makeText(context, feedback.message(context), Toast.LENGTH_LONG).show()
                    }
                }

                is AppDetailEvent.NavigateToManifest -> onNavigateToManifest()

                is AppDetailEvent.NavigateToGeneralDetails -> onNavigateToGeneralDetails()

                is AppDetailEvent.NavigateToPermissions -> onNavigateToPermissions(event.permissionName)

                is AppDetailEvent.NavigateToComponents -> onNavigateToComponents()

                is AppDetailEvent.NavigateToActivities -> onNavigateToActivities()

                is AppDetailEvent.NavigateToServices -> onNavigateToServices()

                is AppDetailEvent.NavigateToReceivers -> onNavigateToReceivers()

                is AppDetailEvent.NavigateToProviders -> onNavigateToProviders()

                is AppDetailEvent.NavigateToCertificates -> onNavigateToCertificates()

                is AppDetailEvent.NavigateToFeatures -> onNavigateToFeatures()

                is AppDetailEvent.NavigateToInventoryExport -> onNavigateToInventoryExport(event.packageName)

                is AppDetailEvent.NavigateToHistory -> onNavigateToHistory(event.packageName, event.appName)
            }
        }
    }

    AppDetailContent(
        state = state,
        appDetailInput = appDetailInput,
        showToolbarWhileLoading = appDetailInput.isHistorical,
        onAction = viewModel::onAction,
        onBack = onBack,
        modifier = modifier,
    )
    splitApkExportName?.let { displayName ->
        SplitApkExportBottomSheet(
            displayName = displayName,
            onDismiss = { splitApkExportName = null },
        )
    }
    summaryPreview?.let { summary ->
        AppSummaryBottomSheet(
            summary = summary,
            canExportInventory = (state as? AppDetailState.Loaded)?.canExportInventory == true,
            onCopy = {
                viewModel.onAction(AppDetailAction.CopySummary)
                summaryPreview = null
            },
            onShare = {
                viewModel.onAction(AppDetailAction.ShareSummary)
                summaryPreview = null
            },
            onExportInventory = {
                viewModel.onAction(AppDetailAction.ExportInventory)
                summaryPreview = null
            },
            onDismiss = { summaryPreview = null },
        )
    }
}

@Composable
private fun AppDetailContent(
    state: AppDetailState,
    appDetailInput: AppDetailInput,
    onAction: (AppDetailAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showToolbarWhileLoading: Boolean = false,
) {
    AppDetailModeFrame(recordedAt = appDetailInput.recordedAt, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.background),
        ) {
            when (state) {
                is AppDetailState.Loading -> {
                    if (showToolbarWhileLoading) {
                        Toolbar(
                            title = stringResource(R.string.app_detail_title),
                            onBack = onBack,
                        )
                    }
                    LoadingContent()
                }

                is AppDetailState.Error -> {
                    Toolbar(
                        title = stringResource(R.string.app_detail_title),
                        onBack = onBack,
                    )
                    ErrorContent(reason = state.reason, onAction = onAction)
                }

                is AppDetailState.Loaded -> {
                    LoadedContent(
                        state = state,
                        appDetailInput = appDetailInput,
                        onAction = onAction,
                        onBack = onBack,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadedContent(
    state: AppDetailState.Loaded,
    appDetailInput: AppDetailInput,
    onAction: (AppDetailAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val collapsingState = rememberCollapsingToolbarState()

    Column(modifier = modifier.fillMaxSize()) {
        AppDetailToolbar(
            state = state,
            appDetailInput = appDetailInput,
            collapsingState = collapsingState,
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .collapsingToolbarScroll(collapsingState)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            appDetailInput.liveReference?.let { reference ->
                AiSummaryCard(
                    reference = reference,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                )
            }
            OverviewSection(state = state, onAction = onAction)
            if (state.insights.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                WorthKnowingSection(state = state, onAction = onAction)
            }
            if (state.history != AppDetailHistoryStatus.Hidden) {
                Spacer(modifier = Modifier.height(12.dp))
                HistorySection(status = state.history, onClick = { onAction(AppDetailAction.NavigateHistory) })
            }
            if (!state.isHistorical) {
                Spacer(modifier = Modifier.height(12.dp))
                ActionsSection(state = state, onAction = onAction)
            }
            Spacer(modifier = Modifier.height(12.dp))
            CertificateSignatureSection(state = state, onAction = onAction)
            Spacer(modifier = Modifier.height(12.dp))
            PermissionsSection(state = state, onAction = onAction)
            Spacer(modifier = Modifier.height(12.dp))
            ComponentsSection(state = state, onAction = onAction)
            Spacer(modifier = Modifier.height(12.dp))
            RequirementsSection(state = state, onAction = onAction)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HistorySection(
    status: AppDetailHistoryStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val overview = (status as? AppDetailHistoryStatus.Available)?.overview
    val isCertificateChange = overview is AppHistoryOverview.CertificateChanged
    val line = when (status) {
        AppDetailHistoryStatus.Hidden, AppDetailHistoryStatus.Reading -> null
        AppDetailHistoryStatus.Unavailable -> stringResource(R.string.app_history_hub_unavailable)
        is AppDetailHistoryStatus.Available -> status.overview.hubLine()
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(Shapes.CardShape)
            .background(AppTheme.colors.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionHeader(title = stringResource(R.string.app_detail_history_section))
            Icon(
                imageVector = ApkAnalyzerIcons.ChevronRight,
                tint = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 16.dp),
        ) {
            if (line != null) {
                Icon(
                    imageVector = if (isCertificateChange) ApkAnalyzerIcons.Warning else ApkAnalyzerIcons.History,
                    tint = if (isCertificateChange) AppTheme.colors.warning else AppTheme.colors.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = line,
                    style = AppTheme.typography.bodySmall,
                    color = if (isCertificateChange) AppTheme.colors.onSurface else AppTheme.colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AppHistoryOverview.hubLine(): String = when (this) {
    AppHistoryOverview.NoEntries -> stringResource(R.string.app_history_hub_no_entries)
    is AppHistoryOverview.FirstRecordedOnly -> stringResource(R.string.app_history_hub_first_only, recordingSince.historyDate())
    is AppHistoryOverview.Changes -> pluralStringResource(R.plurals.app_history_hub_changes, count, count, latestAt.historyDate())
    is AppHistoryOverview.CertificateChanged -> stringResource(R.string.app_history_hub_certificate, changedAt.historyDate())
}

@Composable
private fun WorthKnowingSection(
    state: AppDetailState.Loaded,
    onAction: (AppDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = ApkAnalyzerIcons.Warning,
                tint = AppTheme.colors.warning,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            SectionHeader(title = stringResource(R.string.app_detail_worth_knowing))
        }
        Spacer(modifier = Modifier.height(8.dp))
        state.insights.forEach { insight ->
            WorthKnowingRow(
                insight = insight,
                onClick = { onAction(AppDetailAction.NavigateInsight(insight)) },
            )
        }
    }
}

@Composable
private fun WorthKnowingRow(
    insight: AppDetailInsight,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(Shapes.CardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = insight.label(LocalResources.current),
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = ApkAnalyzerIcons.ChevronRight,
            tint = AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = AppTheme.typography.titleMedium,
        color = AppTheme.colors.primary,
        modifier = modifier,
    )
}

@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(Shapes.CardShape)
            .background(AppTheme.colors.surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 16.dp),
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
                value = state.versionName ?: state.versionCode.toString(),
                modifier = Modifier.weight(1f),
            )
            OverviewGridCell(
                label = if (state.totalSize != null) stringResource(R.string.app_detail_total_size) else stringResource(R.string.app_detail_apk_size),
                value = state.totalSize?.formatted() ?: state.apkSize.formatted(),
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
                value = when {
                    state.targetSdkVersion != null && state.targetSdkLabel != null ->
                        stringResource(AppsR.string.app_detail_sdk_version, state.targetSdkVersion, state.targetSdkLabel)

                    state.targetSdkVersion != null ->
                        stringResource(AppsR.string.app_detail_sdk_version_no_label, state.targetSdkVersion)

                    else -> stringResource(AppsR.string.app_detail_unknown)
                },
                valueColor = if (state.isTargetSdkOutdated) AppTheme.colors.warning else AppTheme.colors.onBackground,
                modifier = Modifier.weight(1f),
            )
            OverviewGridCell(
                label = stringResource(R.string.app_detail_updated),
                value = state.lastUpdateTime?.let { formatTimestamp(it) }
                    ?: stringResource(AppsR.string.app_detail_unknown),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = pluralStringResource(
                when (state.overviewHighlight) {
                    OverviewHighlight.SplitApks -> R.plurals.app_detail_overview_more_details_with_split_apks
                    OverviewHighlight.NetworkSecurity -> R.plurals.app_detail_overview_more_details_with_network_security
                    OverviewHighlight.InstallHistory -> R.plurals.app_detail_overview_more_details_with_history
                    OverviewHighlight.InstallDate -> R.plurals.app_detail_overview_more_details_with_date
                    OverviewHighlight.None -> R.plurals.app_detail_overview_more_details
                },
                state.additionalGeneralInfoCount,
                state.additionalGeneralInfoCount,
            ),
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.onSurfaceVariant,
        )
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
            style = AppTheme.typography.labelMedium,
            color = AppTheme.colors.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = AppTheme.typography.titleSmall,
            color = valueColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun ActionsSection(
    state: AppDetailState.Loaded,
    onAction: (AppDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(Shapes.CardShape)
            .background(AppTheme.colors.surface),
    ) {
        SectionHeader(
            title = stringResource(R.string.app_detail_actions_section),
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
        ) {
            ActionItem(
                icon = ApkAnalyzerIcons.File,
                label = stringResource(R.string.app_detail_action_manifest),
                onClick = { onAction(AppDetailAction.ViewManifest) },
                iconTint = AppTheme.colors.primary,
            )
            if (state.analysisMode == AppDetail.AnalysisMode.InstalledPackage) {
                ActionItem(
                    icon = ApkAnalyzerIcons.Storage,
                    label = if (state.exportInProgress == AppDetailExport.Apk) {
                        stringResource(R.string.app_detail_action_saving)
                    } else {
                        stringResource(R.string.app_detail_action_export_apk)
                    },
                    onClick = { onAction(AppDetailAction.ExportApk) },
                    enabled = state.exportInProgress == null,
                    iconTint = AppTheme.colors.primary,
                )
            }
            ActionItem(
                icon = ApkAnalyzerIcons.Android,
                label = if (state.exportInProgress == AppDetailExport.Icon) {
                    stringResource(R.string.app_detail_action_saving)
                } else {
                    stringResource(R.string.app_detail_action_save_icon)
                },
                onClick = { onAction(AppDetailAction.SaveIcon) },
                enabled = state.exportInProgress == null,
                iconTint = AppTheme.colors.primary,
            )
            ActionItem(
                icon = ApkAnalyzerIcons.Share,
                label = stringResource(R.string.app_detail_action_summary),
                onClick = { onAction(AppDetailAction.ViewSummary) },
                iconTint = AppTheme.colors.primary,
            )
            ActionItem(
                icon = ApkAnalyzerIcons.Apps,
                label = stringResource(R.string.app_detail_action_play_store),
                onClick = { onAction(AppDetailAction.OpenPlayStore) },
                iconTint = AppTheme.colors.primary,
            )
            if (state.analysisMode == AppDetail.AnalysisMode.InstalledPackage) {
                ActionItem(
                    icon = ApkAnalyzerIcons.Settings,
                    label = stringResource(R.string.app_detail_action_app_info),
                    onClick = { onAction(AppDetailAction.OpenAppInfo) },
                    iconTint = AppTheme.colors.primary,
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
    }
}

@Composable
private fun RequirementPreviewIcon(preview: AppDetailState.Loaded.RequirementPreview, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .background(
                color = if (preview.isUnmetRequirement) AppTheme.colors.warningContainer else AppTheme.colors.surfaceVariant,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = requirementIcon(preview.name),
            tint = if (preview.isUnmetRequirement) AppTheme.colors.warning else AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    badgeCount: Int? = null,
    iconTint: Color = AppTheme.colors.onSurface,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(72.dp)
            .clip(Shapes.CardShape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(AppTheme.colors.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    tint = if (enabled) iconTint else AppTheme.colors.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
            if (badgeCount != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                        .clip(CircleShape)
                        .background(AppTheme.colors.primary)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = badgeCount.toString(),
                        style = AppTheme.typography.labelSmall,
                        color = AppTheme.colors.onPrimary,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = AppTheme.typography.labelSmall,
            color = if (enabled) AppTheme.colors.onSurface else AppTheme.colors.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CertificateSignatureSection(
    state: AppDetailState.Loaded,
    onAction: (AppDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cert = state.certificate ?: return
    val (icon, iconTint, signerText) = when (cert.trustLevel) {
        CertificateTrustLevel.Debug -> Triple(
            ApkAnalyzerIcons.Warning,
            AppTheme.colors.warning,
            stringResource(R.string.app_detail_certificate_debug),
        )

        CertificateTrustLevel.Valid -> Triple(
            ApkAnalyzerIcons.Verified,
            AppTheme.colors.primary,
            cert.signerDisplayName?.let { stringResource(R.string.app_detail_certificate_signed_by, it) }
                ?: stringResource(R.string.app_detail_certificate_unknown_signer),
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(Shapes.CardShape)
            .background(AppTheme.colors.surface)
            .clickable { onAction(AppDetailAction.NavigateCertificates) }
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionHeader(title = stringResource(R.string.app_detail_certificate_signature_section))
            Icon(
                imageVector = ApkAnalyzerIcons.ChevronRight,
                tint = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                tint = iconTint,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = signerText,
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        LabeledHashBox(
            label = stringResource(R.string.app_detail_certificate_sha256_fingerprint),
            value = cert.sha256Fingerprint,
        )
    }
}

@Composable
private fun PermissionsSection(
    state: AppDetailState.Loaded,
    onAction: (AppDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(Shapes.CardShape)
            .background(AppTheme.colors.surface)
            .clickable { onAction(AppDetailAction.NavigatePermissions()) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionHeader(title = stringResource(R.string.app_detail_permissions_section))
            Icon(
                imageVector = ApkAnalyzerIcons.ChevronRight,
                tint = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        val highlightedSensitiveCount = state.grantedDangerousPermissionsCount ?: state.dangerousPermissionsCount
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Icon(
                imageVector = if (highlightedSensitiveCount > 0) ApkAnalyzerIcons.DangerousPermissions else ApkAnalyzerIcons.Check,
                tint = if (highlightedSensitiveCount > 0) AppTheme.colors.warning else AppTheme.colors.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = when {
                    state.totalPermissionsCount == 0 ->
                        stringResource(R.string.app_detail_permissions_none)

                    state.dangerousPermissionsCount == 0 ->
                        stringResource(R.string.app_detail_permissions_no_dangerous)

                    state.grantedDangerousPermissionsCount == 0 ->
                        stringResource(R.string.app_detail_permissions_none_granted)

                    state.grantedDangerousPermissionsCount != null ->
                        pluralStringResource(
                            R.plurals.app_detail_permissions_granted_summary,
                            state.dangerousPermissionsCount,
                            state.grantedDangerousPermissionsCount,
                            state.dangerousPermissionsCount,
                        )

                    else ->
                        pluralStringResource(
                            R.plurals.app_detail_permissions_dangerous_summary,
                            state.dangerousPermissionsCount,
                            state.dangerousPermissionsCount,
                        )
                },
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
            )
        }
        if (state.dangerousPermissionPreviews.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
            ) {
                state.dangerousPermissionPreviews.forEach { preview ->
                    ActionItem(
                        icon = permissionIcon(
                            name = preview.name,
                            groupName = preview.groupName,
                        ),
                        label = preview.label,
                        onClick = { onAction(AppDetailAction.NavigatePermissions(preview.name)) },
                    )
                }
            }
        }
        if (state.totalPermissionsCount > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (state.dangerousPermissionsCount > 0) {
                    stringResource(
                        R.string.app_detail_count_pair,
                        pluralStringResource(
                            R.plurals.app_detail_permissions_requested_count,
                            state.totalPermissionsCount,
                            state.totalPermissionsCount,
                        ),
                        pluralStringResource(
                            R.plurals.app_detail_permissions_sensitive_count,
                            state.dangerousPermissionsCount,
                            state.dangerousPermissionsCount,
                        ),
                    )
                } else {
                    pluralStringResource(
                        R.plurals.app_detail_permissions_total,
                        state.totalPermissionsCount,
                        state.totalPermissionsCount,
                    )
                },
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ComponentsSection(
    state: AppDetailState.Loaded,
    onAction: (AppDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(Shapes.CardShape)
            .background(AppTheme.colors.surface)
            .clickable { onAction(AppDetailAction.NavigateComponents) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionHeader(title = stringResource(R.string.app_detail_components_section))
            Icon(
                imageVector = ApkAnalyzerIcons.ChevronRight,
                tint = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ActionItem(
                icon = ApkAnalyzerIcons.ComponentActivity,
                label = stringResource(R.string.app_detail_activities),
                badgeCount = state.activitiesCount,
                onClick = { onAction(AppDetailAction.NavigateActivities) },
            )
            ActionItem(
                icon = ApkAnalyzerIcons.ComponentService,
                label = stringResource(R.string.app_detail_services),
                badgeCount = state.servicesCount,
                onClick = { onAction(AppDetailAction.NavigateServices) },
            )
            ActionItem(
                icon = ApkAnalyzerIcons.ComponentReceiver,
                label = stringResource(R.string.app_detail_broadcast_receivers),
                badgeCount = state.broadcastReceiversCount,
                onClick = { onAction(AppDetailAction.NavigateReceivers) },
            )
            ActionItem(
                icon = ApkAnalyzerIcons.ComponentProvider,
                label = stringResource(R.string.app_detail_content_providers),
                badgeCount = state.contentProvidersCount,
                onClick = { onAction(AppDetailAction.NavigateProviders) },
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
    }
}

@Composable
private fun RequirementsSection(
    state: AppDetailState.Loaded,
    onAction: (AppDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(Shapes.CardShape)
            .background(AppTheme.colors.surface)
            .clickable { onAction(AppDetailAction.NavigateFeatures) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionHeader(title = stringResource(R.string.app_detail_features_section))
            Icon(
                imageVector = ApkAnalyzerIcons.ChevronRight,
                tint = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        if (state.requirementsCount == 0) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.app_detail_features_none),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
            return@Column
        }
        if (!state.isHistorical) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Icon(
                    imageVector = if (state.unmetRequirementsCount > 0) ApkAnalyzerIcons.Warning else ApkAnalyzerIcons.Check,
                    tint = if (state.unmetRequirementsCount > 0) AppTheme.colors.warning else AppTheme.colors.primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (state.unmetRequirementsCount > 0) {
                        pluralStringResource(
                            R.plurals.app_detail_features_unmet,
                            state.unmetRequirementsCount,
                            state.unmetRequirementsCount,
                        )
                    } else {
                        stringResource(R.string.app_detail_features_all_met)
                    },
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.onSurfaceVariant,
                )
            }
        }
        if (state.requirementPreviews.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                state.requirementPreviews.forEach { preview ->
                    RequirementPreviewIcon(preview = preview)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(
                R.string.app_detail_count_pair,
                pluralStringResource(
                    R.plurals.app_detail_features_required_count,
                    state.requiredFeaturesCount,
                    state.requiredFeaturesCount,
                ),
                pluralStringResource(
                    R.plurals.app_detail_features_optional_count,
                    state.optionalFeaturesCount,
                    state.optionalFeaturesCount,
                ),
            ),
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
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
private fun ErrorContent(
    reason: AppDetailErrorReason,
    onAction: (AppDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onRetry = { onAction(AppDetailAction.Retry) }
    when (reason) {
        AppDetailErrorReason.UnsupportedApk -> SectionError(message = stringResource(R.string.app_detail_error_unsupported_apk), modifier = modifier)

        is AppDetailErrorReason.Failed -> SectionError(
            reason = reason.reason,
            loadFailedMessage = stringResource(R.string.app_detail_error),
            onRetry = onRetry,
            modifier = modifier,
        )
    }
}

private fun AppDetailFeedback.message(context: Context): String = when (this) {
    is AppDetailFeedback.ApkSaved -> context.getString(R.string.app_detail_apk_saved, displayName)
    is AppDetailFeedback.IconSaved -> context.getString(R.string.app_detail_icon_saved, displayName)
    AppDetailFeedback.ApkSaveFailed -> context.getString(R.string.app_detail_apk_save_failed)
    AppDetailFeedback.IconSaveFailed -> context.getString(R.string.app_detail_icon_save_failed)
    AppDetailFeedback.DocumentPickerUnavailable -> context.getString(R.string.app_detail_document_picker_unavailable)
    AppDetailFeedback.SummaryCopied -> context.getString(R.string.app_detail_summary_copied)
    AppDetailFeedback.ShareUnavailable -> context.getString(R.string.app_detail_share_unavailable)
}

private fun formatTimestamp(instant: Instant): String {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return dateFormat.format(Date.from(instant))
}

@Preview
@Composable
private fun AppDetailLoadingPreview() {
    ApkAnalyzerTheme {
        AppDetailContent(
            state = AppDetailState.Loading,
            appDetailInput = AppDetailInput.InstalledPackage("com.spotify.music"),
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
            state = AppDetailState.Error(AppDetailErrorReason.Failed(SectionErrorReason.LoadFailed)),
            appDetailInput = AppDetailInput.InstalledPackage("com.spotify.music"),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun AppDetailErrorUnsupportedApkPreview() {
    ApkAnalyzerTheme {
        AppDetailContent(
            state = AppDetailState.Error(AppDetailErrorReason.UnsupportedApk),
            appDetailInput = AppDetailInput.ApkFile(apkFilePath = "/data/local/tmp/sample.apk"),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun AppDetailHistoricalLoadingPreview() {
    ApkAnalyzerTheme {
        AppDetailContent(
            state = AppDetailState.Loading,
            appDetailInput = sampleHistoricalInput,
            showToolbarWhileLoading = true,
            onAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun AppDetailRecordedVersionFailedPreview() {
    ApkAnalyzerTheme {
        AppDetailContent(
            state = AppDetailState.Error(AppDetailErrorReason.Failed(SectionErrorReason.VersionReadFailed)),
            appDetailInput = sampleHistoricalInput,
            onAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun AppDetailRecordedVersionNotFoundPreview() {
    ApkAnalyzerTheme {
        AppDetailContent(
            state = AppDetailState.Error(AppDetailErrorReason.Failed(SectionErrorReason.VersionNotFound)),
            appDetailInput = sampleHistoricalInput,
            onAction = {},
            onBack = {},
        )
    }
}

@Preview(heightDp = 1400)
@Composable
private fun AppDetailHistoricalLoadedPreview() {
    ApkAnalyzerTheme {
        AppDetailContent(
            state = sampleLoadedState().copy(
                analysisMode = AppDetail.AnalysisMode.Historical,
                grantedDangerousPermissionsCount = null,
                totalSize = null,
                unmetRequirementsCount = 0,
                insights = listOf(AppDetailInsight.Debuggable),
            ),
            appDetailInput = sampleHistoricalInput,
            onAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun HistorySectionReadingPreview() {
    ApkAnalyzerTheme {
        HistorySection(status = AppDetailHistoryStatus.Reading, onClick = {})
    }
}

@Preview
@Composable
private fun HistorySectionNoEntriesPreview() {
    ApkAnalyzerTheme {
        HistorySection(status = AppDetailHistoryStatus.Available(AppHistoryOverview.NoEntries), onClick = {})
    }
}

@Preview
@Composable
private fun HistorySectionFirstOnlyPreview() {
    ApkAnalyzerTheme {
        HistorySection(
            status = AppDetailHistoryStatus.Available(AppHistoryOverview.FirstRecordedOnly(Instant.ofEpochMilli(1_760_227_200_000))),
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun HistorySectionChangesPreview() {
    ApkAnalyzerTheme {
        HistorySection(
            status = AppDetailHistoryStatus.Available(AppHistoryOverview.Changes(count = 3, latestAt = Instant.ofEpochMilli(1_772_409_600_000))),
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun HistorySectionCertificatePreview() {
    ApkAnalyzerTheme {
        HistorySection(
            status = AppDetailHistoryStatus.Available(AppHistoryOverview.CertificateChanged(Instant.ofEpochMilli(1_771_027_200_000))),
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun HistorySectionUnavailablePreview() {
    ApkAnalyzerTheme {
        HistorySection(status = AppDetailHistoryStatus.Unavailable, onClick = {})
    }
}

@Preview
@Composable
private fun AppDetailLoadedPreview() {
    ApkAnalyzerTheme {
        AppDetailContent(
            state = sampleLoadedState(),
            appDetailInput = AppDetailInput.InstalledPackage("com.spotify.music"),
            onAction = {},
            onBack = {},
        )
    }
}

private val sampleHistoricalInput = AppDetailInput.Historical(
    packageName = "com.spotify.music",
    snapshotId = 12,
    capturedAt = Instant.ofEpochMilli(1_773_014_400_000),
)

internal fun sampleLoadedState() = AppDetailState.Loaded(
    analysisMode = AppDetail.AnalysisMode.InstalledPackage,
    appName = "Spotify",
    packageName = PackageName("com.spotify.music"),
    processName = "com.spotify.music",
    versionName = "9.4.12",
    versionCode = 90412,
    uid = 10234,
    description = null,
    isSystemApp = false,
    source = AppSource.GooglePlay,
    apkDirectory = "/example/app/com.spotify.music/base.apk",
    dataDirectory = "/example/user/0/com.spotify.music",
    apkSize = 152.megabytes,
    totalSize = 510.megabytes,
    targetSdkVersion = 35,
    targetSdkLabel = "Android 15",
    minSdkVersion = 24,
    minSdkLabel = "Android 7.0",
    installLocation = "InternalOnly",
    appInstaller = PackageName("com.android.vending"),
    firstInstallTime = Instant.ofEpochMilli(1_736_640_000_000),
    lastUpdateTime = Instant.ofEpochMilli(1_748_736_000_000),
    totalPermissionsCount = 32,
    dangerousPermissionsCount = 6,
    grantedDangerousPermissionsCount = 4,
    dangerousPermissionPreviews = listOf(
        AppDetailState.Loaded.PermissionPreview(
            name = "android.permission.CAMERA",
            groupName = "android.permission-group.CAMERA",
            label = "Camera",
        ),
        AppDetailState.Loaded.PermissionPreview(
            name = "android.permission.RECORD_AUDIO",
            groupName = "android.permission-group.MICROPHONE",
            label = "Microphone",
        ),
        AppDetailState.Loaded.PermissionPreview(
            name = "android.permission.ACCESS_FINE_LOCATION",
            groupName = "android.permission-group.LOCATION",
            label = "Location",
        ),
        AppDetailState.Loaded.PermissionPreview(
            name = "android.permission.READ_CONTACTS",
            groupName = "android.permission-group.CONTACTS",
            label = "Contacts",
        ),
        AppDetailState.Loaded.PermissionPreview(
            name = "android.permission.READ_MEDIA_AUDIO",
            groupName = "android.permission-group.READ_MEDIA_AURAL",
            label = "Music and audio",
        ),
        AppDetailState.Loaded.PermissionPreview(
            name = "android.permission.POST_NOTIFICATIONS",
            groupName = "android.permission-group.NOTIFICATIONS",
            label = "Notifications",
        ),
    ),
    definedPermissionsCount = 1,
    activitiesCount = 428,
    servicesCount = 57,
    contentProvidersCount = 4,
    broadcastReceiversCount = 89,
    certificatesCount = 2,
    requirementsCount = 12,
    requiredFeaturesCount = 9,
    optionalFeaturesCount = 3,
    unmetRequirementsCount = 1,
    requirementPreviews = listOf(
        AppDetailState.Loaded.RequirementPreview(name = "android.hardware.nfc", isUnmetRequirement = true),
        AppDetailState.Loaded.RequirementPreview(name = "android.hardware.camera", isUnmetRequirement = false),
        AppDetailState.Loaded.RequirementPreview(name = "android.hardware.wifi", isUnmetRequirement = false),
        AppDetailState.Loaded.RequirementPreview(name = "android.hardware.bluetooth_le", isUnmetRequirement = false),
        AppDetailState.Loaded.RequirementPreview(name = null, isUnmetRequirement = false),
    ),
    certificate = AppDetailState.Loaded.CertificateState(
        signAlgorithm = "SHA256withRSA",
        sha256Fingerprint = "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0:C1:D2:E3:F4:A5:B6:C7:D8:E9:F0:A1:B2",
        issuer = CertificatePrincipal(name = "Android", organization = "Google Inc."),
        trustLevel = CertificateTrustLevel.Valid,
    ),
    insights = listOf(
        AppDetailInsight.Debuggable,
        AppDetailInsight.Sideloaded,
        AppDetailInsight.Unused(monthsSinceLastUsed = 8),
        AppDetailInsight.SensitivePermission(
            access = SensitiveAccess.BackgroundLocation,
            permissionName = "android.permission.ACCESS_BACKGROUND_LOCATION",
        ),
    ),
)
