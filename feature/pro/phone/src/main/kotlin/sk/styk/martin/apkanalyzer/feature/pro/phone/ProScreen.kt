package sk.styk.martin.apkanalyzer.feature.pro.phone

import androidx.activity.compose.LocalActivity
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryLockedPreview
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryProSummary
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryVersion
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.ValueChange
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.AppIcon
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Icon
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.ProBadge
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.ProButton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.SkeletonBox
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Tag
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.TextButton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.format.historyDate
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.entitlements.api.Entitlement
import sk.styk.martin.apkanalyzer.core.entitlements.api.Offer
import sk.styk.martin.apkanalyzer.feature.pro.api.PaywallReason
import sk.styk.martin.apkanalyzer.feature.pro.api.ProSheetResult
import sk.styk.martin.apkanalyzer.feature.pro.api.WhatChangedPlacement
import java.time.Instant

private val PictureShape = RoundedCornerShape(20.dp)
private val PictureCardShape = RoundedCornerShape(16.dp)
private val PlaceholderShape = RoundedCornerShape(4.dp)
private val AuthorPhotoSize = 44.dp
private val BuyButtonHeight = 56.dp
private const val PLACEHOLDER_ALPHA = 0.3f

@Composable
internal fun ProScreen(
    reason: PaywallReason,
    onClose: () -> Unit,
    onCloseWithResult: (ProSheetResult) -> Unit,
    viewModel: ProViewModel = hiltViewModel { factory: ProViewModel.Factory -> factory.create(reason) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ProEvent.Close -> onClose()
                is ProEvent.CloseWithResult -> onCloseWithResult(event.result)
            }
        }
    }

    ProContent(
        state = state,
        onUnlock = { activity?.let { viewModel.onAction(ProAction.Unlock(it)) } },
        onAction = viewModel::onAction,
    )
}

@Composable
private fun ProContent(
    state: ProState,
    onUnlock: () -> Unit,
    onAction: (ProAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 12.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val reason = state.reason) {
                is PaywallReason.WhatChanged -> HistoryPitch(history = state.history)
                is PaywallReason.Export -> ExportPitch(reason = reason, history = state.history)
                PaywallReason.Settings, PaywallReason.Launch -> SupportPitch(history = state.history)
            }
        }
        PurchaseArea(
            state = state,
            onUnlock = onUnlock,
            onAction = onAction,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun HistoryPitch(history: ProHistory, modifier: Modifier = Modifier) {
    val summary = history.summary
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = modifier) {
        summary?.latestLocked?.let { LockedChangePicture(preview = it) }
        if (summary == null) {
            if (history == ProHistory.Loading) HeadlineSkeleton() else Headline(text = stringResource(R.string.pro_history_fallback))
            return@Column
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Headline(
                text = pluralStringResource(R.plurals.pro_history_headline, summary.changeCount, summary.changeCount, summary.since.historyDate()),
            )
            Text(
                text = listOf(
                    pluralStringResource(R.plurals.pro_history_free, summary.freeAppCount, summary.freeAppCount),
                    pluralStringResource(R.plurals.pro_history_locked, summary.lockedAppCount, summary.lockedAppCount),
                ).joinToString(separator = " "),
                style = AppTheme.typography.bodyLarge,
                color = AppTheme.colors.onSurfaceVariant,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (summary.addedPermissionCount > 0) {
                BenefitLine(
                    icon = ApkAnalyzerIcons.Permissions,
                    text = pluralStringResource(R.plurals.pro_benefit_permissions, summary.addedPermissionCount, summary.addedPermissionCount),
                )
            }
            if (summary.componentChangeCount > 0) {
                BenefitLine(
                    icon = ApkAnalyzerIcons.Widgets,
                    text = pluralStringResource(R.plurals.pro_benefit_components, summary.componentChangeCount, summary.componentChangeCount),
                )
            }
            if (summary.sizeChangeCount > 0) {
                BenefitLine(
                    icon = ApkAnalyzerIcons.Storage,
                    text = pluralStringResource(R.plurals.pro_benefit_sizes, summary.sizeChangeCount, summary.sizeChangeCount),
                )
            }
            BenefitLine(icon = ApkAnalyzerIcons.Export, text = stringResource(R.string.pro_benefit_plus_export), isSecondary = true)
        }
    }
}

@Composable
private fun ExportPitch(
    reason: PaywallReason.Export,
    history: ProHistory,
    modifier: Modifier = Modifier,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = modifier) {
        if (reason.sampleAppNames.isNotEmpty()) ExportPreviewPicture(appNames = reason.sampleAppNames)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Headline(text = stringResource(R.string.pro_export_headline))
            Text(
                text = stringResource(R.string.pro_export_details, reason.freeDetailCount, reason.proDetailCount),
                style = AppTheme.typography.bodyLarge,
                color = AppTheme.colors.onSurfaceVariant,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BenefitLine(
                icon = ApkAnalyzerIcons.Permissions,
                text = pluralStringResource(R.plurals.pro_export_audit, reason.appCount, reason.appCount),
            )
            BenefitLine(icon = ApkAnalyzerIcons.VerifiedShield, text = stringResource(R.string.pro_export_certificates))
            BenefitLine(icon = ApkAnalyzerIcons.Widgets, text = stringResource(R.string.pro_export_components))
            val changeCount = history.summary?.changeCount ?: 0
            BenefitLine(
                icon = ApkAnalyzerIcons.History,
                text = if (changeCount > 0) {
                    pluralStringResource(R.plurals.pro_export_plus_history, changeCount, changeCount)
                } else {
                    stringResource(R.string.pro_export_plus_history_none)
                },
                isSecondary = true,
            )
        }
    }
}

@Composable
private fun SupportPitch(history: ProHistory, modifier: Modifier = Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier.padding(top = 8.dp)) {
        Text(
            text = stringResource(R.string.pro_still_free),
            style = AppTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 46.sp),
            color = AppTheme.colors.onSurface,
        )
        Text(
            text = stringResource(R.string.pro_still_free_body),
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colors.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 4.dp)) {
            val changeCount = history.summary?.changeCount ?: 0
            FeatureRow(
                icon = ApkAnalyzerIcons.History,
                title = stringResource(R.string.pro_feature_history),
                detail = if (changeCount > 0) {
                    pluralStringResource(R.plurals.pro_feature_history_count, changeCount, changeCount)
                } else {
                    stringResource(R.string.pro_feature_history_empty)
                },
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
            )
            FeatureRow(
                icon = ApkAnalyzerIcons.Export,
                title = stringResource(R.string.pro_feature_export_title),
                detail = stringResource(R.string.pro_feature_export_detail),
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            )
        }
    }
}

@Composable
private fun Headline(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AppTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
        color = AppTheme.colors.onSurface,
        modifier = modifier.padding(top = 4.dp),
    )
}

@Composable
private fun HeadlineSkeleton(modifier: Modifier = Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.padding(top = 8.dp)) {
        SkeletonBox(modifier = Modifier.fillMaxWidth(0.8f).height(24.dp))
        SkeletonBox(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp))
    }
}

@Composable
private fun BenefitLine(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    isSecondary: Boolean = false,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier) {
        Icon(
            imageVector = icon,
            tint = if (isSecondary) AppTheme.colors.onSurfaceVariant else AppTheme.colors.pro,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            style = AppTheme.typography.bodyLarge,
            color = if (isSecondary) AppTheme.colors.onSurfaceVariant else AppTheme.colors.onSurface,
        )
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    detail: String,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.proContainer, shape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(imageVector = icon, tint = AppTheme.colors.pro, modifier = Modifier.size(22.dp))
        Column {
            Text(text = title, style = AppTheme.typography.titleSmall, color = AppTheme.colors.onSurface)
            Text(text = detail, style = AppTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun LockedChangePicture(preview: AppHistoryLockedPreview, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(AppTheme.colors.proContainer, PictureShape)
            .padding(16.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(AppTheme.colors.surface, PictureCardShape)
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppIcon(
                    source = AppReference.InstalledPackage(preview.packageName),
                    size = 36.dp,
                    missingIcon = ApkAnalyzerIcons.Android,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = preview.applicationName, style = AppTheme.typography.titleMedium, color = AppTheme.colors.onSurface, maxLines = 1)
                    preview.version?.let { version ->
                        Text(
                            text = "${version.before.label} → ${version.after.label}",
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colors.onSurfaceVariant,
                        )
                    }
                }
                ProBadge()
            }
            if (preview.addedPermissionCount > 0) {
                PictureLine(pluralStringResource(R.plurals.pro_card_permissions, preview.addedPermissionCount, preview.addedPermissionCount))
            }
            if (preview.componentChangeCount > 0) {
                PictureLine(pluralStringResource(R.plurals.pro_card_components, preview.componentChangeCount, preview.componentChangeCount))
            }
            if (preview.isSizeChanged) {
                PictureLine(stringResource(R.string.pro_card_size))
            }
        }
    }
}

private val AppHistoryVersion.label: String
    get() = versionName ?: versionCode.toString()

@Composable
private fun PictureLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.size(8.dp).background(AppTheme.colors.pro, CircleShape))
        Text(text = text, style = AppTheme.typography.bodyMedium, color = AppTheme.colors.onSurface)
    }
}

@Composable
private fun ExportPreviewPicture(appNames: List<String>, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(AppTheme.colors.proContainer, PictureShape)
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(PictureCardShape)
                .background(AppTheme.colors.surface),
        ) {
            PreviewTableRow(
                first = { Text(stringResource(R.string.pro_export_preview_app), style = AppTheme.typography.labelMedium) },
                second = { PreviewHeaderCell(stringResource(R.string.pro_export_preview_permissions)) },
                third = { PreviewHeaderCell(stringResource(R.string.pro_export_preview_certificate)) },
            )
            appNames.forEach { name ->
                PreviewTableRow(
                    first = { Text(name, style = AppTheme.typography.bodySmall, color = AppTheme.colors.onSurface, maxLines = 1) },
                    second = { PreviewPlaceholder(widthFraction = 0.4f) },
                    third = { PreviewPlaceholder(widthFraction = 0.8f) },
                )
            }
        }
    }
}

@Composable
private fun PreviewTableRow(
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    third: @Composable () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(32.dp)) {
        Box(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) { first() }
        Box(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) { second() }
        Box(modifier = Modifier.weight(1.2f).padding(horizontal = 10.dp)) { third() }
    }
}

@Composable
private fun PreviewHeaderCell(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = text, style = AppTheme.typography.labelMedium, color = AppTheme.colors.pro, maxLines = 1)
    }
}

@Composable
private fun PreviewPlaceholder(widthFraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(8.dp)
            .clip(PlaceholderShape)
            .background(AppTheme.colors.onSurfaceVariant.copy(alpha = PLACEHOLDER_ALPHA)),
    )
}

@Composable
private fun SupportRow(@DrawableRes photo: Int, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Image(
            painter = painterResource(photo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(AuthorPhotoSize).clip(CircleShape),
        )
        Column {
            Text(text = stringResource(R.string.pro_support_title), style = AppTheme.typography.titleMedium, color = AppTheme.colors.onSurface)
            Text(text = stringResource(R.string.pro_author), style = AppTheme.typography.bodySmall, color = AppTheme.colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun PurchaseArea(
    state: ProState,
    onUnlock: () -> Unit,
    onAction: (ProAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSupportPitch = state.reason == PaywallReason.Settings || state.reason == PaywallReason.Launch
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SupportRow(photo = R.drawable.pro_author, modifier = Modifier.padding(bottom = 4.dp))
        statusMessage(state)?.let { message ->
            Text(
                text = message,
                style = AppTheme.typography.bodyMedium,
                color = if (state.status == ProStatus.Failed) AppTheme.colors.negative else AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        when (val offer = state.offer) {
            ProOffer.Loading -> SkeletonBox(modifier = Modifier.fillMaxWidth().height(BuyButtonHeight))

            is ProOffer.Available -> if (state.status == ProStatus.Pending) {
                Tag(text = stringResource(R.string.pro_pending))
            } else {
                ProButton(
                    text = stringResource(
                        if (isSupportPitch) R.string.pro_support_unlock else R.string.pro_unlock_lifetime,
                        offer.offer.formattedPrice,
                    ),
                    onClick = onUnlock,
                    enabled = !state.status.blocksActions,
                    modifier = Modifier.fillMaxWidth().height(BuyButtonHeight),
                )
            }

            ProOffer.Unavailable -> Unit
        }
        if (state.offer is ProOffer.Available) {
            Text(
                text = stringResource(R.string.pro_one_payment),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
            )
        }
        SecondaryActions(state = state, onAction = onAction)
    }
}

@Composable
private fun SecondaryActions(state: ProState, onAction: (ProAction) -> Unit) {
    val enabled = !state.status.blocksActions
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        when (state.reason) {
            is PaywallReason.Export -> TextButton(
                text = stringResource(R.string.pro_export_basics_only),
                onClick = { onAction(ProAction.ExportBasicsOnly) },
                enabled = enabled,
            )

            PaywallReason.Launch -> TextButton(
                text = stringResource(R.string.pro_continue_free),
                onClick = { onAction(ProAction.Dismiss) },
            )

            is PaywallReason.WhatChanged, PaywallReason.Settings -> Unit
        }
        if (state.offer is ProOffer.Available && state.reason != PaywallReason.Launch) {
            TextButton(
                text = stringResource(R.string.pro_restore),
                onClick = { onAction(ProAction.Restore) },
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun statusMessage(state: ProState): String? = when {
    state.offer == ProOffer.Unavailable -> stringResource(R.string.pro_unavailable)
    state.status == ProStatus.Failed -> stringResource(R.string.pro_failed)
    state.status == ProStatus.Pending -> stringResource(R.string.pro_pending_body)
    state.status == ProStatus.NothingToRestore -> stringResource(R.string.pro_restore_nothing)
    else -> null
}

private val PreviewOffer = ProOffer.Available(Offer(Entitlement.Pro, "€3.99"))

private val PreviewSummary = AppHistoryProSummary(
    since = Instant.parse("2026-10-14T10:00:00Z"),
    changeCount = 214,
    changedAppCount = 61,
    freeAppCount = 6,
    lockedAppCount = 55,
    addedPermissionCount = 38,
    componentChangeCount = 112,
    sizeChangeCount = 54,
    latestLocked = AppHistoryLockedPreview(
        packageName = PackageName("org.thoughtcrime.securesms"),
        applicationName = "Signal",
        version = ValueChange(AppHistoryVersion(1, "7.23"), AppHistoryVersion(2, "7.24")),
        addedPermissionCount = 2,
        componentChangeCount = 1,
        isSizeChanged = true,
    ),
)

private val PreviewExport = PaywallReason.Export(
    appCount = 142,
    sampleAppNames = listOf("Chrome", "WhatsApp", "Revolut", "Spotify"),
    freeDetailCount = 8,
    proDetailCount = 49,
)

@Preview(heightDp = 900)
@Composable
private fun ProContentWhatChangedPreview() {
    ApkAnalyzerTheme {
        ProContent(
            state = ProState(
                reason = PaywallReason.WhatChanged(WhatChangedPlacement.Card),
                offer = PreviewOffer,
                history = ProHistory.Available(PreviewSummary),
            ),
            onUnlock = {},
            onAction = {},
        )
    }
}

@Preview(heightDp = 900)
@Composable
private fun ProContentExportPreview() {
    ApkAnalyzerTheme {
        ProContent(
            state = ProState(reason = PreviewExport, offer = PreviewOffer, history = ProHistory.Available(PreviewSummary)),
            onUnlock = {},
            onAction = {},
        )
    }
}

@Preview(heightDp = 900)
@Composable
private fun ProContentLaunchPreview() {
    ApkAnalyzerTheme {
        ProContent(
            state = ProState(reason = PaywallReason.Launch, offer = PreviewOffer, history = ProHistory.Available(PreviewSummary)),
            onUnlock = {},
            onAction = {},
        )
    }
}

@Preview(heightDp = 900)
@Composable
private fun ProContentSettingsDarkPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        ProContent(
            state = ProState(reason = PaywallReason.Settings, offer = PreviewOffer, history = ProHistory.Available(PreviewSummary)),
            onUnlock = {},
            onAction = {},
        )
    }
}

@Preview(heightDp = 900)
@Composable
private fun ProContentLoadingPreview() {
    ApkAnalyzerTheme {
        ProContent(
            state = ProState(reason = PaywallReason.WhatChanged(WhatChangedPlacement.LockedRow)),
            onUnlock = {},
            onAction = {},
        )
    }
}

@Preview(heightDp = 900)
@Composable
private fun ProContentPendingPreview() {
    ApkAnalyzerTheme {
        ProContent(
            state = ProState(reason = PreviewExport, offer = PreviewOffer, status = ProStatus.Pending, history = ProHistory.Unavailable),
            onUnlock = {},
            onAction = {},
        )
    }
}

@Preview(heightDp = 900)
@Composable
private fun ProContentFailedPreview() {
    ApkAnalyzerTheme {
        ProContent(
            state = ProState(reason = PreviewExport, offer = PreviewOffer, status = ProStatus.Failed, history = ProHistory.Unavailable),
            onUnlock = {},
            onAction = {},
        )
    }
}

@Preview(heightDp = 900)
@Composable
private fun ProContentUnavailablePreview() {
    ApkAnalyzerTheme {
        ProContent(
            state = ProState(reason = PaywallReason.Settings, offer = ProOffer.Unavailable, history = ProHistory.Unavailable),
            onUnlock = {},
            onAction = {},
        )
    }
}
