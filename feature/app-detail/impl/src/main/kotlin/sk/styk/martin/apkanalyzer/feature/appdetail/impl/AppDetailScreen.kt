package sk.styk.martin.apkanalyzer.feature.appdetail.impl

import android.content.ActivityNotFoundException
import android.content.Context
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.model.CertificatePrincipal
import sk.styk.martin.apkanalyzer.core.apps.model.CertificateTrustLevel
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Icon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.LoadingSpinner
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.components.TextButton
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Toolbar
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.collapsingToolbarScroll
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.rememberCollapsingToolbarState
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.AppDetailToolbar
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.HashBox
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.SplitApkExportBottomSheet
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.permissions.permissionIcon
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.requirements.requirementIcon
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale

private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
private const val ICON_MIME_TYPE = "image/png"

@Composable
internal fun AppDetailScreen(
    appDetailInput: AppDetailInput,
    onBack: () -> Unit,
    onOpenPlayStore: (packageName: PackageName) -> Unit,
    onOpenAppInfo: (packageName: PackageName) -> Unit,
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
    modifier: Modifier = Modifier,
    viewModel: AppDetailViewModel = hiltViewModel { factory: AppDetailViewModel.Factory ->
        factory.create(appDetailInput)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val appReference = appDetailInput.toAppReference()
    var splitApkExportName by rememberSaveable { mutableStateOf<String?>(null) }
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
                is AppDetailEvent.OpenPlayStore -> onOpenPlayStore(event.packageName)

                is AppDetailEvent.OpenAppInfo -> onOpenAppInfo(event.packageName)

                is AppDetailEvent.CreateDocument -> {
                    try {
                        when (event.export) {
                            AppDetailExport.Apk -> apkDocumentLauncher.launch(event.suggestedName)
                            AppDetailExport.Icon -> iconDocumentLauncher.launch(event.suggestedName)
                        }
                    } catch (_: ActivityNotFoundException) {
                        viewModel.onAction(AppDetailAction.DocumentPickerUnavailable(event.export))
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
            }
        }
    }

    AppDetailContent(
        state = state,
        appReference = appReference,
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
}

private fun AppDetailFeedback.message(context: Context): String = when (this) {
    is AppDetailFeedback.ApkSaved -> context.getString(R.string.app_detail_apk_saved, displayName)
    is AppDetailFeedback.IconSaved -> context.getString(R.string.app_detail_icon_saved, displayName)
    AppDetailFeedback.ApkSaveFailed -> context.getString(R.string.app_detail_apk_save_failed)
    AppDetailFeedback.IconSaveFailed -> context.getString(R.string.app_detail_icon_save_failed)
    AppDetailFeedback.DocumentPickerUnavailable -> context.getString(R.string.app_detail_document_picker_unavailable)
}

@Composable
private fun AppDetailContent(
    state: AppDetailState,
    appReference: AppReference,
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
                    appReference = appReference,
                    onAction = onAction,
                    onBack = onBack,
                )
            }
        }
    }
}

@Composable
private fun LoadedContent(
    state: AppDetailState.Loaded,
    appReference: AppReference,
    onAction: (AppDetailAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val collapsingState = rememberCollapsingToolbarState()

    Column(modifier = modifier.fillMaxSize()) {
        AppDetailToolbar(
            state = state,
            appReference = appReference,
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
            OverviewSection(state = state, onAction = onAction)
            if (state.insights.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                WorthKnowingSection(state = state, onAction = onAction)
            }
            Spacer(modifier = Modifier.height(12.dp))
            ActionsSection(state = state, onAction = onAction)
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
            text = insight.label(),
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
private fun AppDetailInsight.label(): String = when (this) {
    AppDetailInsight.DebugCertificate -> stringResource(R.string.app_detail_insight_debug_certificate)

    AppDetailInsight.CertificateNotYetValid -> stringResource(R.string.app_detail_insight_certificate_not_yet_valid)

    is AppDetailInsight.OutdatedTargetSdk -> pluralStringResource(
        R.plurals.app_detail_insight_outdated_target,
        apiLevelGap,
        targetSdk,
        apiLevelGap,
    )

    is AppDetailInsight.SensitivePermission -> when (access) {
        SensitiveAccess.BackgroundLocation -> stringResource(R.string.app_detail_insight_background_location)
        SensitiveAccess.Messages -> stringResource(R.string.app_detail_insight_messages)
        SensitiveAccess.CallHistory -> stringResource(R.string.app_detail_insight_call_history)
        SensitiveAccess.Contacts -> stringResource(R.string.app_detail_insight_contacts)
        SensitiveAccess.Calendar -> stringResource(R.string.app_detail_insight_calendar)
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
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
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
            .padding(horizontal = 8.dp)
            .clip(Shapes.CardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
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
                        stringResource(R.string.app_detail_sdk_version, state.targetSdkVersion, state.targetSdkLabel)

                    state.targetSdkVersion != null ->
                        stringResource(R.string.app_detail_sdk_version_no_label, state.targetSdkVersion)

                    else -> stringResource(R.string.app_detail_unknown)
                },
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
            )
            ActionItem(
                icon = ApkAnalyzerIcons.Apps,
                label = stringResource(R.string.app_detail_action_play_store),
                onClick = { onAction(AppDetailAction.OpenPlayStore) },
            )
            if (state.analysisMode == AppDetail.AnalysisMode.InstalledPackage) {
                ActionItem(
                    icon = ApkAnalyzerIcons.Settings,
                    label = stringResource(R.string.app_detail_action_app_info),
                    onClick = { onAction(AppDetailAction.OpenAppInfo) },
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
            tint = if (preview.isUnmetRequirement) AppTheme.colors.warning else AppTheme.colors.primary,
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
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(72.dp)
            .clip(Shapes.CardShape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(AppTheme.colors.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                tint = if (enabled) AppTheme.colors.primary else AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
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
        HashBox(
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
                        R.string.app_detail_permissions_counts,
                        state.totalPermissionsCount,
                        state.dangerousPermissionsCount,
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
    SectionCard(
        modifier = modifier,
        onClick = { onAction(AppDetailAction.NavigateComponents) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
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
        Spacer(modifier = Modifier.height(8.dp))
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
                R.string.app_detail_features_counts,
                state.requiredFeaturesCount,
                state.optionalFeaturesCount,
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
            appReference = AppReference.InstalledPackage(PackageName("com.spotify.music")),
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
            appReference = AppReference.InstalledPackage(PackageName("com.spotify.music")),
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
            appReference = AppReference.InstalledPackage(PackageName("com.spotify.music")),
            onAction = {},
            onBack = {},
        )
    }
}

private fun sampleLoadedState() = AppDetailState.Loaded(
    analysisMode = AppDetail.AnalysisMode.InstalledPackage,
    appName = "Spotify",
    packageName = PackageName("com.spotify.music"),
    processName = "com.spotify.music",
    versionName = "9.4.12",
    versionCode = 90412,
    uid = 10234,
    description = null,
    isSystemApp = false,
    source = "GooglePlay",
    apkDirectory = "/data/app/com.spotify.music/base.apk",
    dataDirectory = "/data/data/com.spotify.music",
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
    dangerousPermissionPreviews = persistentListOf(
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
    requirementPreviews = persistentListOf(
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
    insights = persistentListOf(
        AppDetailInsight.SensitivePermission(
            access = SensitiveAccess.BackgroundLocation,
            permissionName = "android.permission.ACCESS_BACKGROUND_LOCATION",
        ),
    ),
)
