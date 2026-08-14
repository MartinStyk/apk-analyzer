package sk.styk.martin.apkanalyzer.feature.browse.impl.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.apps.signing.SignatureAlgorithmAssessment
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.uilibrary.components.AppIcon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.BottomSheet
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Chip
import sk.styk.martin.apkanalyzer.core.uilibrary.components.ChipVariant
import sk.styk.martin.apkanalyzer.core.uilibrary.components.IconButton
import sk.styk.martin.apkanalyzer.core.uilibrary.components.LoadingSpinner
import sk.styk.martin.apkanalyzer.core.uilibrary.components.SearchBarActive
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Toolbar
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.collapsingHeader
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.collapsingHeaderContainer
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.rememberCollapsingHeaderState
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.browse.impl.R
import sk.styk.martin.apkanalyzer.feature.browse.impl.domain.BrowseBucketSelection
import sk.styk.martin.apkanalyzer.feature.browse.impl.model.BrowseDimension

@Composable
internal fun BrowseAppsScreen(
    dimension: BrowseDimension,
    bucket: BrowseBucketSelection,
    onBack: () -> Unit,
    onOpenApp: (PackageName) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BrowseAppsViewModel = hiltViewModel { factory: BrowseAppsViewModel.Factory ->
        factory.create(dimension, bucket)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BrowseAppsEvent.NavigateToAppDetail -> onOpenApp(event.packageName)
            }
        }
    }

    BrowseAppsContent(
        bucketLabel = bucket.label,
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun BrowseAppsContent(
    bucketLabel: String,
    state: BrowseAppsState,
    onAction: (BrowseAppsAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDetailSheet by rememberSaveable { mutableStateOf(false) }
    val bucketDetail = (state as? BrowseAppsState.Loaded)?.bucketDetail

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        Toolbar(
            title = bucketLabel,
            onBack = onBack,
            actions = {
                if (bucketDetail != null) {
                    IconButton(
                        imageVector = ApkAnalyzerIcons.Info,
                        onClick = { showDetailSheet = true },
                        contentDescription = stringResource(R.string.browse_bucket_info_content_description, bucketLabel),
                    )
                }
            },
        )

        when (state) {
            is BrowseAppsState.Loading -> Box(modifier = Modifier.fillMaxSize()) {
                LoadingSpinner(modifier = Modifier.align(Alignment.Center))
            }

            is BrowseAppsState.Loaded -> BrowseAppsList(state = state, onAction = onAction)
        }
    }

    if (showDetailSheet && bucketDetail != null) {
        BrowseBucketDetailSheet(
            bucketLabel = bucketLabel,
            detail = bucketDetail,
            onDismiss = { showDetailSheet = false },
        )
    }
}

@Composable
private fun BrowseBucketDetailSheet(
    bucketLabel: String,
    detail: BrowseBucketDetail,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BottomSheet(onDismiss = onDismiss, modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(
                text = bucketLabel,
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.onSurface,
            )
            Spacer(modifier = Modifier.height(12.dp))

            when (detail) {
                is BrowseBucketDetail.Permission -> PermissionDetailContent(detail)
                is BrowseBucketDetail.Certificate -> CertificateDetailContent(detail)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PermissionDetailContent(detail: BrowseBucketDetail.Permission, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        detail.protectionLevel?.let { protectionLevel ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip(label = stringResource(protectionLevel.labelRes), variant = protectionLevel.chipVariant())
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(protectionLevel.explanationRes),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant,
            )
        }
        detail.description?.let { description ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = description,
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CertificateDetailContent(detail: BrowseBucketDetail.Certificate, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip(label = stringResource(detail.algorithmAssessment.labelRes), variant = detail.algorithmAssessment.chipVariant())
            if (detail.isSelfSigned) {
                Chip(label = stringResource(R.string.browse_certificate_self_signed), variant = ChipVariant.Default)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(detail.algorithmAssessment.explanationRes, detail.algorithm),
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurfaceVariant,
        )
        if (detail.isSelfSigned) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.browse_certificate_self_signed_explanation),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant,
            )
        }
    }
}

private val ProtectionLevel.labelRes: Int
    get() = when (this) {
        ProtectionLevel.Dangerous -> R.string.browse_protection_level_dangerous
        ProtectionLevel.Signature -> R.string.browse_protection_level_signature
        ProtectionLevel.Internal -> R.string.browse_protection_level_internal
        ProtectionLevel.Normal -> R.string.browse_protection_level_normal
    }

private val ProtectionLevel.explanationRes: Int
    get() = when (this) {
        ProtectionLevel.Dangerous -> R.string.browse_protection_level_dangerous_explanation
        ProtectionLevel.Signature -> R.string.browse_protection_level_signature_explanation
        ProtectionLevel.Internal -> R.string.browse_protection_level_internal_explanation
        ProtectionLevel.Normal -> R.string.browse_protection_level_normal_explanation
    }

private fun ProtectionLevel.chipVariant(): ChipVariant = when (this) {
    ProtectionLevel.Dangerous -> ChipVariant.Warning
    ProtectionLevel.Signature, ProtectionLevel.Internal, ProtectionLevel.Normal -> ChipVariant.Default
}

private val SignatureAlgorithmAssessment.labelRes: Int
    get() = when (this) {
        SignatureAlgorithmAssessment.ModernDigest -> R.string.browse_algorithm_strong

        SignatureAlgorithmAssessment.WeakSha1Digest,
        SignatureAlgorithmAssessment.WeakMd5Digest,
        SignatureAlgorithmAssessment.WeakMd2Digest,
        -> R.string.browse_algorithm_weak

        SignatureAlgorithmAssessment.Unknown -> R.string.browse_algorithm_unknown
    }

private val SignatureAlgorithmAssessment.explanationRes: Int
    get() = when (this) {
        SignatureAlgorithmAssessment.ModernDigest -> R.string.browse_algorithm_strong_explanation
        SignatureAlgorithmAssessment.WeakSha1Digest -> R.string.browse_algorithm_sha1_explanation
        SignatureAlgorithmAssessment.WeakMd5Digest -> R.string.browse_algorithm_md5_explanation
        SignatureAlgorithmAssessment.WeakMd2Digest -> R.string.browse_algorithm_md2_explanation
        SignatureAlgorithmAssessment.Unknown -> R.string.browse_algorithm_unknown_explanation
    }

private fun SignatureAlgorithmAssessment.chipVariant(): ChipVariant = when (this) {
    SignatureAlgorithmAssessment.ModernDigest -> ChipVariant.Positive

    SignatureAlgorithmAssessment.WeakSha1Digest,
    SignatureAlgorithmAssessment.WeakMd5Digest,
    SignatureAlgorithmAssessment.WeakMd2Digest,
    -> ChipVariant.Warning

    SignatureAlgorithmAssessment.Unknown -> ChipVariant.Default
}

@Composable
private fun BrowseAppsList(
    state: BrowseAppsState.Loaded,
    onAction: (BrowseAppsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val collapsingState = rememberCollapsingHeaderState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .collapsingHeaderContainer(collapsingState),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { collapsingState.headerOffset }
                .background(AppTheme.colors.background)
                .collapsingHeader(collapsingState),
        ) {
            SearchBarActive(
                query = state.query,
                placeholder = pluralStringResource(R.plurals.browse_apps_filter_hint, state.totalApps, state.totalApps),
                sharedElementKey = null,
                onQueryChange = { onAction(BrowseAppsAction.ChangeQuery(it)) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        if (state.apps.isNotEmpty()) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { collapsingState.contentOffset },
            ) {
                items(items = state.apps, key = { it.packageName.value }) { app ->
                    BrowseAppRow(
                        app = app,
                        onClick = { onAction(BrowseAppsAction.AppClicked(app.packageName)) },
                    )
                }
            }
        } else {
            Text(
                text = stringResource(R.string.browse_apps_empty_query, state.query),
                style = AppTheme.typography.bodyLarge,
                color = AppTheme.colors.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { collapsingState.contentOffset }
                    .padding(horizontal = 32.dp, vertical = 48.dp),
            )
        }
    }
}

@Composable
private fun BrowseAppRow(
    app: BrowseAppItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.CardShape)
            .background(AppTheme.colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        AppIcon(source = AppReference.InstalledPackage(app.packageName))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.applicationName,
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = app.packageName.value,
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview
@Composable
private fun BrowseAppsLoadingPreview() {
    ApkAnalyzerTheme {
        BrowseAppsContent(bucketLabel = "Camera", state = BrowseAppsState.Loading, onAction = {}, onBack = {})
    }
}

@Preview
@Composable
private fun BrowseAppsLoadedPreview() {
    ApkAnalyzerTheme {
        BrowseAppsContent(bucketLabel = "Camera", state = sampleLoadedState(), onAction = {}, onBack = {})
    }
}

@Preview
@Composable
private fun BrowseAppsEmptyPreview() {
    ApkAnalyzerTheme {
        BrowseAppsContent(
            bucketLabel = "Camera",
            state = sampleLoadedState().copy(query = "xyz", apps = persistentListOf()),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun BrowseBucketDetailPermissionPreview() {
    ApkAnalyzerTheme {
        BrowseBucketDetailSheet(
            bucketLabel = "Camera",
            detail = BrowseBucketDetail.Permission(
                protectionLevel = ProtectionLevel.Dangerous,
                description = "Required to be able to access the camera device.",
            ),
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun BrowseBucketDetailCertificatePreview() {
    ApkAnalyzerTheme {
        BrowseBucketDetailSheet(
            bucketLabel = "FC:98:DA:E6:3A:D3:96:26",
            detail = BrowseBucketDetail.Certificate(
                algorithm = "SHA1withRSA",
                algorithmAssessment = SignatureAlgorithmAssessment.WeakSha1Digest,
                isSelfSigned = true,
            ),
            onDismiss = {},
        )
    }
}

private fun sampleLoadedState() = BrowseAppsState.Loaded(
    query = "",
    totalApps = 2,
    apps = persistentListOf(
        BrowseAppItem(packageName = PackageName("com.instagram.android"), applicationName = "Instagram"),
        BrowseAppItem(packageName = PackageName("com.spotify.music"), applicationName = "Spotify"),
    ).toImmutableList(),
    bucketDetail = BrowseBucketDetail.Permission(protectionLevel = ProtectionLevel.Dangerous, description = null),
)
