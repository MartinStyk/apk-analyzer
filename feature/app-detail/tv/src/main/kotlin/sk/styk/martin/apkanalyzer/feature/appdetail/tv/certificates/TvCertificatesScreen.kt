package sk.styk.martin.apkanalyzer.feature.appdetail.tv.certificates

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.apps.signing.Certificate
import sk.styk.martin.apkanalyzer.core.apps.signing.CertificatePrincipal
import sk.styk.martin.apkanalyzer.core.apps.signing.CertificateTrustLevel
import sk.styk.martin.apkanalyzer.core.apps.signing.CertificateValidity
import sk.styk.martin.apkanalyzer.core.apps.signing.SigningSchemeVersion
import sk.styk.martin.apkanalyzer.core.apps.signing.displayLine
import sk.styk.martin.apkanalyzer.core.apps.signing.explanationRes
import sk.styk.martin.apkanalyzer.core.apps.signing.isWeak
import sk.styk.martin.apkanalyzer.core.apps.signing.labelRes
import sk.styk.martin.apkanalyzer.core.common.digest.toFingerprint
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.LocalFingerprintFormat
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.R
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.InfoRow
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.InfoSection
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.InfoSectionList
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.SectionEmpty
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.SectionError
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.SectionLoading
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.formatDate
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.toAppReference
import java.time.Instant
import sk.styk.martin.apkanalyzer.core.apps.R as AppsR

@Composable
internal fun TvCertificatesScreen(
    appDetailInput: AppDetailInput,
    viewModel: TvCertificatesViewModel = hiltViewModel { factory: TvCertificatesViewModel.Factory ->
        factory.create(appDetailInput)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TvCertificatesContent(state = state, appReference = appDetailInput.toAppReference(), onAction = viewModel::onAction)
}

@Composable
private fun TvCertificatesContent(
    state: TvCertificatesState,
    appReference: AppReference,
    onAction: (TvCertificatesAction) -> Unit,
) {
    when (state) {
        TvCertificatesState.Loading -> SectionLoading()

        TvCertificatesState.Error -> SectionError(
            message = stringResource(AppsR.string.certificates_error),
            onRetry = { onAction(TvCertificatesAction.Retry) },
        )

        is TvCertificatesState.Loaded -> if (state.currentCertificates.isEmpty() && state.pastCertificates.isEmpty()) {
            SectionEmpty(
                title = stringResource(AppsR.string.certificates_title),
                subtitle = state.appName,
                appReference = appReference,
                message = stringResource(AppsR.string.certificates_empty),
            )
        } else {
            InfoSectionList(
                title = stringResource(AppsR.string.certificates_title),
                subtitle = state.appName,
                sections = certificateSections(state),
                appReference = appReference,
            )
        }
    }
}

@Composable
private fun certificateSections(state: TvCertificatesState.Loaded): List<InfoSection> = buildList {
    state.currentCertificates.forEachIndexed { index, item ->
        add(
            InfoSection(
                icon = ApkAnalyzerIcons.VerifiedShield,
                title = when {
                    state.currentCertificates.size > 1 ->
                        stringResource(AppsR.string.certificates_signer_position, index + 1, state.currentCertificates.size)

                    else -> stringResource(AppsR.string.certificates_current_certificate)
                },
                rows = certificateRows(
                    keyPrefix = "current-$index",
                    item = item,
                    signingFacts = if (index == 0) state.signingFacts() else listOf(),
                ),
            ),
        )
    }
    state.pastCertificates.forEachIndexed { index, item ->
        val role = stringResource(
            if (index == state.pastCertificates.lastIndex) {
                AppsR.string.certificates_original_signing_key
            } else {
                AppsR.string.certificates_previous_signing_key
            },
        )
        add(
            InfoSection(
                icon = ApkAnalyzerIcons.VerifiedShield,
                title = if (state.pastCertificates.size == 1) {
                    role
                } else {
                    stringResource(AppsR.string.certificates_card_position, role, index + 1, state.pastCertificates.size)
                },
                rows = certificateRows(
                    keyPrefix = "past-$index",
                    item = item,
                    signingFacts = listOf(),
                ),
            ),
        )
    }
}

@Composable
private fun TvCertificatesState.Loaded.signingFacts(): List<InfoRow> = listOfNotNull(
    InfoRow(
        key = "signers",
        label = stringResource(AppsR.string.certificates_current_signers),
        value = stringResource(if (hasMultipleSigners) AppsR.string.certificates_multiple_signers else AppsR.string.certificates_single_signer),
        explanation = stringResource(
            if (hasMultipleSigners) AppsR.string.certificates_multiple_signers_explanation else AppsR.string.certificates_single_signer_explanation,
        ),
    ),
    signingSchemeVersions?.takeIf { it.isNotEmpty() }?.let { versions ->
        InfoRow(
            key = "scheme",
            label = stringResource(AppsR.string.certificates_scheme),
            value = versions.map { stringResource(it.labelRes) }.joinToString(", "),
            explanation = stringResource(AppsR.string.certificates_scheme_explanation),
        )
    },
)

@Composable
private fun certificateRows(
    keyPrefix: String,
    item: TvCertificate,
    signingFacts: List<InfoRow>,
): List<InfoRow> {
    val certificate = item.certificate
    val isDebug = certificate.trustLevel == CertificateTrustLevel.Debug
    val subject = certificate.subject
    return buildList {
        if (isDebug) {
            add(
                InfoRow(
                    key = "$keyPrefix:debug",
                    label = stringResource(AppsR.string.certificates_signer),
                    value = stringResource(AppsR.string.certificates_debug_title),
                    explanation = stringResource(AppsR.string.certificates_debug_description),
                    highlighted = true,
                ),
            )
        }
        val subjectRows = listOfNotNull(
            subject.organization?.takeIf { it.isNotBlank() }?.let { plainRow("$keyPrefix:organization", AppsR.string.certificates_organization, it) },
            subject.name?.takeIf { it.isNotBlank() }?.let { plainRow("$keyPrefix:name", AppsR.string.certificates_name, it) },
            subject.country?.takeIf { it.isNotBlank() }?.let { plainRow("$keyPrefix:country", AppsR.string.certificates_country, it) },
        )
        addAll(
            subjectRows.ifEmpty {
                listOf(plainRow("$keyPrefix:name", AppsR.string.certificates_name, stringResource(AppsR.string.certificates_unknown_signer)))
            },
        )
        if (certificate.isSelfSigned) {
            add(
                InfoRow(
                    key = "$keyPrefix:self-signed",
                    label = stringResource(AppsR.string.certificates_self_signed),
                    value = stringResource(AppsR.string.certificates_self_signed_value),
                    explanation = stringResource(AppsR.string.certificates_self_signed_explanation),
                ),
            )
        } else {
            add(
                plainRow(
                    "$keyPrefix:issuer",
                    AppsR.string.certificates_issuer,
                    certificate.issuer.displayLine ?: stringResource(AppsR.string.certificates_unknown_signer),
                ),
            )
        }
        add(
            InfoRow(
                key = "$keyPrefix:validity",
                label = stringResource(AppsR.string.certificates_validity),
                value = stringResource(
                    R.string.tv_value_pair,
                    stringResource(item.validity.labelRes),
                    stringResource(AppsR.string.certificates_date_range, formatDate(certificate.validFrom), formatDate(certificate.validUntil)),
                ),
                explanation = stringResource(item.validity.explanationRes),
                highlighted = item.validity != CertificateValidity.Valid,
                icon = ApkAnalyzerIcons.Calendar,
                status = stringResource(item.validity.labelRes),
            ),
        )
        addAll(signingFacts.map { it.copy(key = "$keyPrefix:${it.key}") })
        val assessment = certificate.signatureAlgorithmAssessment
        add(
            InfoRow(
                key = "$keyPrefix:algorithm",
                label = stringResource(AppsR.string.certificates_algorithm),
                value = stringResource(R.string.tv_value_pair, certificate.signAlgorithm, stringResource(assessment.labelRes)),
                explanation = stringResource(assessment.explanationRes, certificate.signAlgorithm),
                highlighted = assessment.isWeak,
                icon = ApkAnalyzerIcons.Lock,
                status = stringResource(assessment.labelRes),
            ),
        )
        add(
            InfoRow(
                key = "$keyPrefix:serial",
                label = stringResource(AppsR.string.certificates_serial),
                value = certificate.serialNumber,
                explanation = stringResource(AppsR.string.certificates_serial_explanation),
            ),
        )
        addAll(
            fingerprintRows("$keyPrefix:certificate", AppsR.string.certificates_certificate_fingerprints, certificate.certificateHashes()),
        )
        addAll(fingerprintRows("$keyPrefix:public-key", AppsR.string.certificates_public_key_fingerprints, certificate.publicKeyHashes()))
    }
}

@Composable
private fun fingerprintRows(
    keyPrefix: String,
    group: Int,
    hashes: List<Pair<Int, String>>,
): List<InfoRow> {
    val groupLabel = stringResource(group)
    val fingerprintFormat = LocalFingerprintFormat.current
    return hashes.map { (label, value) ->
        InfoRow(
            key = "$keyPrefix:$label",
            label = stringResource(R.string.tv_value_pair, groupLabel, stringResource(label)),
            value = value.toFingerprint(fingerprintFormat),
            icon = ApkAnalyzerIcons.Fingerprint,
            isCode = true,
            explanation = null,
        )
    }
}

private fun Certificate.certificateHashes() = listOf(
    AppsR.string.certificates_sha256 to certificateHashSha256,
    AppsR.string.certificates_sha1 to certificateHashSha1,
    AppsR.string.certificates_md5 to certificateHashMd5,
)

private fun Certificate.publicKeyHashes() = listOf(
    AppsR.string.certificates_sha256 to publicKeySha256,
    AppsR.string.certificates_sha1 to publicKeySha1,
    AppsR.string.certificates_md5 to publicKeyMd5,
)

@Composable
private fun plainRow(
    key: String,
    label: Int,
    value: String,
) = InfoRow(key = key, label = stringResource(label), value = value, explanation = null)

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TvCertificatesContentPreview() {
    val certificate = Certificate(
        signAlgorithm = "SHA256withRSA",
        certificateHashMd5 = "a1b2c3d4e5f60718293a4b5c6d7e8f90",
        certificateHashSha1 = "0123456789abcdef0123456789abcdef01234567",
        certificateHashSha256 = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        publicKeyMd5 = "f0e1d2c3b4a5968778695a4b3c2d1e0f",
        publicKeySha1 = "76543210fedcba9876543210fedcba9876543210",
        publicKeySha256 = "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210",
        validFrom = Instant.parse("2015-04-01T00:00:00Z"),
        validUntil = Instant.parse("2045-04-01T00:00:00Z"),
        serialNumber = "1234567890",
        subject = CertificatePrincipal(name = "Android", organization = "Google Inc.", country = "US"),
        isSelfSigned = true,
    )
    ApkAnalyzerTvTheme {
        TvCertificatesContent(
            appReference = AppReference.InstalledPackage(PackageName("com.google.android.youtube.tv")),
            state = TvCertificatesState.Loaded(
                appName = "YouTube",
                currentCertificates = listOf(TvCertificate(certificate, CertificateValidity.Valid)),
                pastCertificates = listOf(),
                hasMultipleSigners = false,
                signingSchemeVersions = listOf(SigningSchemeVersion.V2, SigningSchemeVersion.V3),
            ),
            onAction = {},
        )
    }
}
