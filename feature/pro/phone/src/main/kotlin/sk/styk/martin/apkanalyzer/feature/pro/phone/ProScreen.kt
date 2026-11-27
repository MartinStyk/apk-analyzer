package sk.styk.martin.apkanalyzer.feature.pro.phone

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Button
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Icon
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.SkeletonBox
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Tag
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.TextButton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.entitlements.api.Entitlement
import sk.styk.martin.apkanalyzer.core.entitlements.api.Offer
import sk.styk.martin.apkanalyzer.feature.pro.api.PaywallReason

@Composable
internal fun ProScreen(
    reason: PaywallReason,
    onClose: () -> Unit,
    viewModel: ProViewModel = hiltViewModel { factory: ProViewModel.Factory -> factory.create(reason) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ProEvent.Close -> onClose()
            }
        }
    }

    ProContent(
        state = state,
        onUnlock = { activity?.let { viewModel.onAction(ProAction.Unlock(it)) } },
        onRestore = { viewModel.onAction(ProAction.Restore) },
    )
}

@Composable
private fun ProContent(
    state: ProState,
    onUnlock: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.pro_title),
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.pro,
            )
            Text(
                text = headline(state.reason),
                style = AppTheme.typography.headlineSmall,
                color = AppTheme.colors.onSurface,
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureLine(stringResource(R.string.pro_feature_export))
                FeatureLine(stringResource(R.string.pro_feature_what_changed))
            }
            Text(
                text = stringResource(R.string.pro_supporter),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant,
            )
        }
        PurchaseArea(
            state = state,
            onUnlock = onUnlock,
            onRestore = onRestore,
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}

@Composable
private fun headline(reason: PaywallReason): String = when (reason) {
    is PaywallReason.Export -> pluralStringResource(R.plurals.pro_headline_export, reason.appCount, reason.appCount)
    is PaywallReason.WhatChanged -> pluralStringResource(R.plurals.pro_headline_what_changed, reason.lockedAppCount, reason.lockedAppCount)
}

@Composable
private fun FeatureLine(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = ApkAnalyzerIcons.Check,
            contentDescription = null,
            tint = AppTheme.colors.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colors.onSurface,
        )
    }
}

@Composable
private fun PurchaseArea(
    state: ProState,
    onUnlock: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        statusMessage(state)?.let { message ->
            Text(
                text = message,
                style = AppTheme.typography.bodyMedium,
                color = if (state.status == ProStatus.Failed) AppTheme.colors.negative else AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        when (val offer = state.offer) {
            ProOffer.Loading -> SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            )

            is ProOffer.Available -> if (state.status == ProStatus.Pending) {
                Tag(text = stringResource(R.string.pro_pending))
            } else {
                Button(
                    text = stringResource(R.string.pro_unlock, offer.offer.formattedPrice),
                    onClick = onUnlock,
                    enabled = !state.status.blocksActions,
                    containerColor = AppTheme.colors.proContainer,
                    contentColor = AppTheme.colors.pro,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            ProOffer.Unavailable -> Unit
        }
        if (state.offer is ProOffer.Available) {
            Text(
                text = stringResource(R.string.pro_one_time),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
            )
            TextButton(
                text = stringResource(R.string.pro_restore),
                onClick = onRestore,
                enabled = !state.status.blocksActions,
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

@Preview
@Composable
private fun ProContentExportPreview() {
    ApkAnalyzerTheme {
        ProContent(
            state = ProState(reason = PaywallReason.Export(appCount = 142), offer = ProOffer.Available(Offer(Entitlement.Pro, "€3.99"))),
            onUnlock = {},
            onRestore = {},
        )
    }
}

@Preview
@Composable
private fun ProContentWhatChangedPreview() {
    ApkAnalyzerTheme {
        ProContent(
            state = ProState(reason = PaywallReason.WhatChanged(lockedAppCount = 11), offer = ProOffer.Available(Offer(Entitlement.Pro, "€3.99"))),
            onUnlock = {},
            onRestore = {},
        )
    }
}

@Preview
@Composable
private fun ProContentLoadingPreview() {
    ApkAnalyzerTheme {
        ProContent(
            state = ProState(reason = PaywallReason.Export(appCount = 12)),
            onUnlock = {},
            onRestore = {},
        )
    }
}

@Preview
@Composable
private fun ProContentPendingPreview() {
    ApkAnalyzerTheme {
        ProContent(
            state = ProState(
                reason = PaywallReason.WhatChanged(lockedAppCount = 4),
                offer = ProOffer.Available(Offer(Entitlement.Pro, "€3.99")),
                status = ProStatus.Pending,
            ),
            onUnlock = {},
            onRestore = {},
        )
    }
}

@Preview
@Composable
private fun ProContentFailedPreview() {
    ApkAnalyzerTheme {
        ProContent(
            state = ProState(
                reason = PaywallReason.Export(appCount = 38),
                offer = ProOffer.Available(Offer(Entitlement.Pro, "€3.99")),
                status = ProStatus.Failed,
            ),
            onUnlock = {},
            onRestore = {},
        )
    }
}

@Preview
@Composable
private fun ProContentUnavailablePreview() {
    ApkAnalyzerTheme {
        ProContent(
            state = ProState(reason = PaywallReason.Export(appCount = 38), offer = ProOffer.Unavailable),
            onUnlock = {},
            onRestore = {},
        )
    }
}
