package sk.styk.martin.apkanalyzer.feature.appdetail.impl.certificates

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import sk.styk.martin.apkanalyzer.core.apps.model.CertificatePrincipal
import sk.styk.martin.apkanalyzer.core.apps.model.CertificateTrustLevel
import sk.styk.martin.apkanalyzer.core.apps.model.SignatureAlgorithmAssessment
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Chip
import sk.styk.martin.apkanalyzer.core.uilibrary.components.ChipVariant
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Icon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Toolbar
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.R
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.HashBox
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.InfoRow
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.RationaleBottomSheet
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.SectionCard
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.SectionError
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.SectionLoading
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
internal fun CertificatesScreen(
    appDetailInput: AppDetailInput,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CertificatesViewModel = hiltViewModel { factory: CertificatesViewModel.Factory ->
        factory.create(appDetailInput)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                CertificatesEvent.ShowCopiedFeedback -> Toast.makeText(context, R.string.certificates_copied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    CertificatesContent(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun CertificatesContent(
    state: CertificatesState,
    onAction: (CertificatesAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        Toolbar(
            title = stringResource(R.string.certificates_title),
            onBack = onBack,
        )
        when (state) {
            CertificatesState.Loading -> SectionLoading()

            CertificatesState.Error -> SectionError(
                message = stringResource(R.string.certificates_error),
                onRetry = { onAction(CertificatesAction.Retry) },
            )

            is CertificatesState.Loaded -> LoadedContent(state = state, onAction = onAction)
        }
    }
}

@Composable
private fun LoadedContent(
    state: CertificatesState.Loaded,
    onAction: (CertificatesAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var rationaleRow by remember { mutableStateOf<InfoRow?>(null) }

    if (state.currentCertificates.isEmpty() && state.pastCertificates.isEmpty()) {
        EmptyContent(modifier = modifier)
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier.fillMaxSize(),
        ) {
            if (state.currentCertificates.isNotEmpty()) {
                item(key = "current-certificates-header") {
                    CertificateGroupHeader(
                        title = if (state.currentCertificates.size == 1) {
                            stringResource(R.string.certificates_current_certificate)
                        } else {
                            stringResource(R.string.certificates_current_certificates)
                        },
                    )
                }
            }
            itemsIndexed(
                items = state.currentCertificates,
                key = { _, certificate -> certificate.certificateHashSha256 },
            ) { index, certificate ->
                CertificateCard(
                    certificate = certificate,
                    currentSignerCount = state.currentCertificates.size.takeIf { index == 0 },
                    label = if (state.currentCertificates.size > 1) {
                        stringResource(R.string.certificates_signer_position, index + 1, state.currentCertificates.size)
                    } else {
                        null
                    },
                    onCopy = { label, value -> onAction(CertificatesAction.CopyValue(label, value)) },
                    onShowRationale = { rationaleRow = it },
                )
            }
            if (state.pastCertificates.isNotEmpty()) {
                item(key = "signing-history-header") {
                    CertificateGroupHeader(
                        title = stringResource(R.string.certificates_history),
                        description = pluralStringResource(
                            R.plurals.certificates_history_description,
                            state.pastCertificates.size,
                            state.pastCertificates.size,
                        ),
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
            itemsIndexed(
                items = state.pastCertificates,
                key = { _, certificate -> certificate.certificateHashSha256 },
            ) { index, certificate ->
                val role = if (index == state.pastCertificates.lastIndex) {
                    stringResource(R.string.certificates_original_signing_key)
                } else {
                    stringResource(R.string.certificates_previous_signing_key)
                }
                CertificateCard(
                    certificate = certificate,
                    currentSignerCount = null,
                    label = if (state.pastCertificates.size == 1) {
                        role
                    } else {
                        stringResource(R.string.certificates_card_position, role, index + 1, state.pastCertificates.size)
                    },
                    onCopy = { label, value -> onAction(CertificatesAction.CopyValue(label, value)) },
                    onShowRationale = { rationaleRow = it },
                )
            }
        }
    }

    rationaleRow?.let { row ->
        RationaleBottomSheet(
            row = row,
            onDismiss = { rationaleRow = null },
        )
    }
}

@Composable
private fun CertificateCard(
    certificate: CertificateItem,
    currentSignerCount: Int?,
    label: String?,
    onCopy: (label: String, value: String) -> Unit,
    onShowRationale: (InfoRow) -> Unit,
    modifier: Modifier = Modifier,
) {
    val serialLabel = stringResource(R.string.certificates_serial)
    val selfSignedLabel = stringResource(R.string.certificates_self_signed)
    val selfSignedInfo = InfoRow(
        label = selfSignedLabel,
        value = stringResource(R.string.certificates_self_signed_value),
        rationale = stringResource(R.string.certificates_self_signed_explanation),
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (label != null) {
            Text(
                text = label,
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.onSurface,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        if (certificate.trustLevel == CertificateTrustLevel.Debug) {
            DebugBanner()
        }
        SectionCard(
            title = stringResource(R.string.certificates_signer),
            status = selfSignedLabel.takeIf { certificate.isSelfSigned },
            onClickStatus = { onShowRationale(selfSignedInfo) }.takeIf { certificate.isSelfSigned },
            onLongClickStatus = { onCopy(selfSignedLabel, selfSignedLabel) }.takeIf { certificate.isSelfSigned },
        ) {
            SignerSection(
                certificate = certificate,
                onCopy = onCopy,
            )
        }
        SectionCard(title = stringResource(R.string.certificates_details)) {
            ValiditySection(
                certificate = certificate,
                onShowRationale = onShowRationale,
                onCopy = onCopy,
            )
            currentSignerCount?.let { signerCount ->
                SigningSection(
                    signerCount = signerCount,
                    algorithm = certificate.signAlgorithm,
                    algorithmAssessment = certificate.signatureAlgorithmAssessment,
                    onShowRationale = onShowRationale,
                    onCopy = onCopy,
                )
            } ?: AlgorithmFact(
                algorithm = certificate.signAlgorithm,
                assessment = certificate.signatureAlgorithmAssessment,
                onShowRationale = onShowRationale,
                onCopy = onCopy,
            )
            InfoFact(
                label = serialLabel,
                value = certificate.serialNumber,
                onCopy = onCopy,
                rationale = stringResource(R.string.certificates_serial_explanation),
                onShowRationale = onShowRationale,
            )
        }
        SectionCard(title = stringResource(R.string.certificates_certificate_fingerprints)) {
            FingerprintSection(
                certificate = certificate,
                onCopy = onCopy,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun CertificateGroupHeader(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
    ) {
        Text(
            text = title,
            style = AppTheme.typography.titleLarge,
            color = AppTheme.colors.onBackground,
        )
        if (description != null) {
            Text(
                text = description,
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DebugBanner(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.CardShape)
            .background(AppTheme.colors.warningContainer)
            .padding(14.dp),
    ) {
        Icon(
            imageVector = ApkAnalyzerIcons.Warning,
            contentDescription = null,
            tint = AppTheme.colors.warning,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = stringResource(R.string.certificates_debug_title),
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.certificates_debug_description),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurface,
            )
        }
    }
}

@Composable
private fun SignerSection(
    certificate: CertificateItem,
    onCopy: (label: String, value: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val subjectFields = listOfNotNull(
        certificate.subject.organization?.takeIf { it.isNotBlank() }?.let {
            stringResource(R.string.certificates_organization) to it
        },
        certificate.subject.name?.takeIf { it.isNotBlank() }?.let {
            stringResource(R.string.certificates_name) to it
        },
        certificate.subject.country?.takeIf { it.isNotBlank() }?.let {
            stringResource(R.string.certificates_country) to it
        },
    )

    Column(modifier = modifier.fillMaxWidth()) {
        if (subjectFields.isEmpty()) {
            InfoFact(
                label = stringResource(R.string.certificates_name),
                value = stringResource(R.string.certificates_unknown_signer),
                onCopy = onCopy,
            )
        } else {
            subjectFields.forEach { (label, value) ->
                InfoFact(label = label, value = value, onCopy = onCopy)
            }
        }
        if (!certificate.isSelfSigned) {
            InfoFact(
                label = stringResource(R.string.certificates_issuer),
                value = certificate.issuer.toDisplayName() ?: stringResource(R.string.certificates_unknown_signer),
                onCopy = onCopy,
            )
        }
    }
}

@Composable
private fun ValiditySection(
    certificate: CertificateItem,
    onShowRationale: (InfoRow) -> Unit,
    onCopy: (label: String, value: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = when (certificate.validity) {
        CertificateValidity.Valid -> stringResource(R.string.certificates_valid)
        CertificateValidity.Expired -> stringResource(R.string.certificates_expired)
        CertificateValidity.NotYetValid -> stringResource(R.string.certificates_not_yet_valid)
    }
    val validityLabel = stringResource(R.string.certificates_validity)
    val validityRationale = when (certificate.validity) {
        CertificateValidity.Valid -> stringResource(R.string.certificates_valid_explanation)
        CertificateValidity.Expired -> stringResource(R.string.certificates_expired_explanation)
        CertificateValidity.NotYetValid -> stringResource(R.string.certificates_not_yet_valid_explanation)
    }
    val dateFormatter = remember {
        DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault())
    }
    val dateRange = stringResource(
        R.string.certificates_date_range,
        dateFormatter.format(certificate.validFrom),
        dateFormatter.format(certificate.validUntil),
    )
    val validityInfo = InfoRow(validityLabel, status, validityRationale)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
            .clip(Shapes.CardShape)
            .combinedClickable(
                onClick = { onShowRationale(validityInfo) },
                onLongClick = { onCopy(validityLabel, dateRange) },
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = validityLabel.uppercase(),
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colors.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = dateRange,
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.onSurface,
            )
        }
        Chip(
            label = status,
            variant = certificate.validity.chipVariant(),
            onClick = { onShowRationale(validityInfo) },
            onLongClick = { onCopy(validityLabel, status) },
        )
    }
}

@Composable
private fun SigningSection(
    signerCount: Int,
    algorithm: String,
    algorithmAssessment: SignatureAlgorithmAssessment,
    onShowRationale: (InfoRow) -> Unit,
    onCopy: (label: String, value: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val signerState = if (signerCount > 1) {
        stringResource(R.string.certificates_multiple_signers)
    } else {
        stringResource(R.string.certificates_single_signer)
    }
    val signerRationale = if (signerCount > 1) {
        stringResource(R.string.certificates_multiple_signers_explanation)
    } else {
        stringResource(R.string.certificates_single_signer_explanation)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        InfoFact(
            label = stringResource(R.string.certificates_current_signers),
            value = signerState,
            rationale = signerRationale,
            onShowRationale = onShowRationale,
            onCopy = onCopy,
        )
        AlgorithmFact(
            algorithm = algorithm,
            assessment = algorithmAssessment,
            onShowRationale = onShowRationale,
            onCopy = onCopy,
        )
    }
}

@Composable
private fun InfoFact(
    label: String,
    value: String,
    onCopy: (label: String, value: String) -> Unit,
    modifier: Modifier = Modifier,
    rationale: String? = null,
    onShowRationale: ((InfoRow) -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
            .clip(Shapes.CardShape)
            .combinedClickable(
                onClick = {
                    if (rationale != null && onShowRationale != null) {
                        onShowRationale(InfoRow(label, value, rationale))
                    } else {
                        onCopy(label, value)
                    }
                },
                onLongClick = { onCopy(label, value) },
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label.uppercase(),
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colors.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.onSurface,
            )
        }
        if (rationale != null && onShowRationale != null) {
            Icon(
                imageVector = ApkAnalyzerIcons.Info,
                contentDescription = null,
                tint = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier
                    .padding(12.dp)
                    .size(18.dp),
            )
        }
    }
}

@Composable
private fun AlgorithmFact(
    algorithm: String,
    assessment: SignatureAlgorithmAssessment,
    onShowRationale: (InfoRow) -> Unit,
    onCopy: (label: String, value: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.certificates_algorithm)
    val assessmentLabel = when (assessment) {
        SignatureAlgorithmAssessment.ModernDigest -> stringResource(R.string.certificates_algorithm_strong)

        SignatureAlgorithmAssessment.WeakSha1Digest,
        SignatureAlgorithmAssessment.WeakMd5Digest,
        SignatureAlgorithmAssessment.WeakMd2Digest,
        -> stringResource(R.string.certificates_algorithm_weak)

        SignatureAlgorithmAssessment.Unknown -> stringResource(R.string.certificates_algorithm_unknown)
    }
    val rationale = when (assessment) {
        SignatureAlgorithmAssessment.ModernDigest ->
            stringResource(R.string.certificates_algorithm_strong_explanation, algorithm)

        SignatureAlgorithmAssessment.WeakSha1Digest ->
            stringResource(R.string.certificates_algorithm_sha1_explanation, algorithm)

        SignatureAlgorithmAssessment.WeakMd5Digest ->
            stringResource(R.string.certificates_algorithm_md5_explanation, algorithm)

        SignatureAlgorithmAssessment.WeakMd2Digest ->
            stringResource(R.string.certificates_algorithm_md2_explanation, algorithm)

        SignatureAlgorithmAssessment.Unknown ->
            stringResource(R.string.certificates_algorithm_unknown_explanation, algorithm)
    }
    val algorithmInfo = InfoRow(label, algorithm, rationale)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
            .clip(Shapes.CardShape)
            .combinedClickable(
                onClick = { onShowRationale(algorithmInfo) },
                onLongClick = { onCopy(label, algorithm) },
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label.uppercase(),
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colors.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = algorithm,
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.onSurface,
            )
        }
        Chip(
            label = assessmentLabel,
            variant = assessment.chipVariant(),
            onClick = { onShowRationale(algorithmInfo) },
            onLongClick = { onCopy(label, assessmentLabel) },
        )
    }
}

private fun SignatureAlgorithmAssessment.chipVariant() = when (this) {
    SignatureAlgorithmAssessment.ModernDigest -> ChipVariant.Positive

    SignatureAlgorithmAssessment.WeakSha1Digest,
    SignatureAlgorithmAssessment.WeakMd5Digest,
    SignatureAlgorithmAssessment.WeakMd2Digest,
    -> ChipVariant.Warning

    SignatureAlgorithmAssessment.Unknown -> ChipVariant.Default
}

private fun CertificateValidity.chipVariant() = when (this) {
    CertificateValidity.Valid -> ChipVariant.Positive
    CertificateValidity.Expired -> ChipVariant.Default
    CertificateValidity.NotYetValid -> ChipVariant.Negative
}

@Composable
private fun FingerprintSection(
    certificate: CertificateItem,
    onCopy: (label: String, value: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var publicKeyExpanded by rememberSaveable(certificate.certificateHashSha256) { mutableStateOf(false) }
    val copyDescription = stringResource(R.string.certificates_copy, stringResource(R.string.certificates_fingerprint))

    Column(modifier = modifier.fillMaxWidth()) {
        FingerprintBoxes(
            sha256 = certificate.certificateHashSha256,
            sha1 = certificate.certificateHashSha1,
            md5 = certificate.certificateHashMd5,
            copyDescription = copyDescription,
            onCopy = onCopy,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(Shapes.CardShape)
                .clickable { publicKeyExpanded = !publicKeyExpanded }
                .heightIn(min = 48.dp),
        ) {
            Text(
                text = stringResource(R.string.certificates_public_key_fingerprints),
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = ApkAnalyzerIcons.ChevronRight,
                contentDescription = if (publicKeyExpanded) {
                    stringResource(R.string.certificates_collapse_public_key)
                } else {
                    stringResource(R.string.certificates_expand_public_key)
                },
                tint = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(if (publicKeyExpanded) 90f else 0f),
            )
        }
        if (publicKeyExpanded) {
            FingerprintBoxes(
                sha256 = certificate.publicKeySha256,
                sha1 = certificate.publicKeySha1,
                md5 = certificate.publicKeyMd5,
                copyDescription = copyDescription,
                onCopy = onCopy,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun FingerprintBoxes(
    sha256: String,
    sha1: String,
    md5: String,
    copyDescription: String,
    onCopy: (label: String, value: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sha256Label = stringResource(R.string.certificates_sha256)
    val sha1Label = stringResource(R.string.certificates_sha1)
    val md5Label = stringResource(R.string.certificates_md5)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        HashBox(
            label = sha256Label,
            value = sha256,
            copyContentDescription = copyDescription,
            onCopy = { onCopy(sha256Label, sha256) },
        )
        HashBox(
            label = sha1Label,
            value = sha1,
            copyContentDescription = copyDescription,
            onCopy = { onCopy(sha1Label, sha1) },
        )
        HashBox(
            label = md5Label,
            value = md5,
            copyContentDescription = copyDescription,
            onCopy = { onCopy(md5Label, md5) },
        )
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        Icon(
            imageVector = ApkAnalyzerIcons.Lock,
            contentDescription = null,
            tint = AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.certificates_empty),
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colors.onBackground,
        )
    }
}

private fun CertificatePrincipal.toDisplayName(): String? = listOfNotNull(
    organization?.takeIf { it.isNotBlank() },
    name?.takeIf { it.isNotBlank() },
    country?.takeIf { it.isNotBlank() },
).distinct().takeIf { it.isNotEmpty() }?.joinToString(" · ")

@Preview
@Composable
private fun CertificatesLoadedPreview() {
    ApkAnalyzerTheme {
        CertificatesContent(
            state = sampleLoadedState(),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun CertificatesDebugPreview() {
    ApkAnalyzerTheme {
        CertificatesContent(
            state = sampleLoadedState(
                certificate = sampleCertificate().copy(
                    trustLevel = CertificateTrustLevel.Debug,
                    validity = CertificateValidity.Expired,
                ),
            ),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun CertificatesRotationHistoryPreview() {
    ApkAnalyzerTheme {
        CertificatesContent(
            state = CertificatesState.Loaded(
                currentCertificates = persistentListOf(
                    sampleCertificate().copy(
                        certificateHashSha256 = "B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0:C1:D2:E3:F4:A5:B6:C7:D8:E9:F0:A1:B2:C3",
                    ),
                ),
                pastCertificates = persistentListOf(sampleCertificate()),
            ),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun CertificatesLoadingPreview() {
    ApkAnalyzerTheme {
        CertificatesContent(
            state = CertificatesState.Loading,
            onAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun CertificatesErrorPreview() {
    ApkAnalyzerTheme {
        CertificatesContent(
            state = CertificatesState.Error,
            onAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun CertificatesEmptyPreview() {
    ApkAnalyzerTheme {
        CertificatesContent(
            state = CertificatesState.Loaded(
                currentCertificates = persistentListOf(),
                pastCertificates = persistentListOf(),
            ),
            onAction = {},
            onBack = {},
        )
    }
}

private fun sampleLoadedState(certificate: CertificateItem = sampleCertificate()) = CertificatesState.Loaded(
    currentCertificates = persistentListOf(certificate),
    pastCertificates = persistentListOf(),
)

private fun sampleCertificate() = CertificateItem(
    signAlgorithm = "SHA256withRSA",
    signatureAlgorithmAssessment = SignatureAlgorithmAssessment.ModernDigest,
    certificateHashMd5 = "25:D6:8E:11:9F:4C:2A:70:B3:5E:C8:D1:21:42:8A:0F",
    certificateHashSha1 = "38:91:8A:45:3D:07:19:93:54:F8:B1:9A:7A:6D:2B:EC:96:5A:21:22",
    certificateHashSha256 = "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0:C1:D2:E3:F4:A5:B6:C7:D8:E9:F0:A1:B2",
    publicKeyMd5 = "AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99",
    publicKeySha1 = "11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44",
    publicKeySha256 = "01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF",
    validFrom = Instant.parse("2008-08-21T12:00:00Z"),
    validUntil = Instant.parse("2045-09-12T12:00:00Z"),
    serialNumber = "0A:1B:2C:3D:4E:5F:60:71",
    issuer = CertificatePrincipal(name = "Android", organization = "Google Inc.", country = "US"),
    subject = CertificatePrincipal(name = "Android", organization = "Google Inc.", country = "US"),
    trustLevel = CertificateTrustLevel.Valid,
    isSelfSigned = true,
    validity = CertificateValidity.Valid,
)
