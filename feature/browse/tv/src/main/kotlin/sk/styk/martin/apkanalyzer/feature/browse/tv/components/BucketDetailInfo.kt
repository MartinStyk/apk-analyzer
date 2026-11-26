package sk.styk.martin.apkanalyzer.feature.browse.tv.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseBucketDetail
import sk.styk.martin.apkanalyzer.core.apppermissions.detail.explanationRes
import sk.styk.martin.apkanalyzer.core.apppermissions.detail.labelRes
import sk.styk.martin.apkanalyzer.core.apps.api.permissions.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.apps.api.signing.CertificateValidity
import sk.styk.martin.apkanalyzer.core.apps.api.signing.displayLine
import sk.styk.martin.apkanalyzer.core.apps.api.signing.explanationRes
import sk.styk.martin.apkanalyzer.core.apps.api.signing.isWeak
import sk.styk.martin.apkanalyzer.core.apps.api.signing.labelRes
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.Tag
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme
import java.text.DateFormat
import java.util.Date
import sk.styk.martin.apkanalyzer.core.appindex.R as AppIndexR
import sk.styk.martin.apkanalyzer.core.apps.api.R as AppsR

@Composable
internal fun BucketDetailInfo(detail: BrowseBucketDetail, modifier: Modifier = Modifier) {
    when (detail) {
        is BrowseBucketDetail.Permission -> PermissionInfo(detail, modifier)
        is BrowseBucketDetail.Certificate -> CertificateInfo(detail, modifier)
    }
}

@Composable
private fun PermissionInfo(detail: BrowseBucketDetail.Permission, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Tag(
            label = stringResource(detail.protectionLevel.labelRes),
            icon = if (detail.protectionLevel == ProtectionLevel.Dangerous) ApkAnalyzerIcons.Warning else ApkAnalyzerIcons.Permissions,
        )
        InfoText(text = stringResource(detail.protectionLevel.explanationRes))
        detail.description?.let { InfoText(text = it) }
    }
}

@Composable
private fun CertificateInfo(detail: BrowseBucketDetail.Certificate, modifier: Modifier = Modifier) {
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Tag(
                label = stringResource(detail.validity.labelRes),
                icon = if (detail.validity == CertificateValidity.Valid) ApkAnalyzerIcons.Calendar else ApkAnalyzerIcons.Warning,
            )
            Tag(
                label = stringResource(detail.algorithmAssessment.labelRes),
                icon = if (detail.algorithmAssessment.isWeak) ApkAnalyzerIcons.Warning else ApkAnalyzerIcons.VerifiedShield,
            )
            if (detail.isSelfSigned) {
                Tag(label = stringResource(AppIndexR.string.browse_certificate_self_signed))
            }
        }
        detail.subject.organization?.let { InfoField(label = stringResource(AppsR.string.certificates_organization), value = it) }
        detail.subject.name?.takeIf { it != detail.subject.organization }?.let { InfoField(label = stringResource(AppsR.string.certificates_name), value = it) }
        detail.subject.country?.let { InfoField(label = stringResource(AppsR.string.certificates_country), value = it) }
        if (!detail.isSelfSigned) {
            detail.issuer.displayLine?.let { InfoField(label = stringResource(AppsR.string.certificates_issuer), value = it) }
        }
        InfoField(
            label = stringResource(AppsR.string.certificates_validity),
            value = stringResource(
                AppsR.string.certificates_date_range,
                dateFormat.format(Date.from(detail.validFrom)),
                dateFormat.format(Date.from(detail.validUntil)),
            ),
        )
        if (detail.validity != CertificateValidity.Valid) {
            InfoText(text = stringResource(detail.validity.explanationRes))
        }
        InfoText(text = stringResource(detail.algorithmAssessment.explanationRes, detail.algorithm))
        if (detail.isSelfSigned) {
            InfoText(text = stringResource(AppIndexR.string.browse_certificate_self_signed_explanation))
        }
    }
}

@Composable
private fun InfoField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, style = AppTheme.typography.labelMedium, color = AppTheme.colors.onSurfaceVariant)
        Text(text = value, style = AppTheme.typography.bodyLarge)
    }
}

@Composable
private fun InfoText(text: String) {
    Text(text = text, style = AppTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceVariant)
}

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun BucketDetailInfoPreview() {
    ApkAnalyzerTvTheme {
        BucketDetailInfo(
            detail = BrowseBucketDetail.Permission(
                protectionLevel = ProtectionLevel.Dangerous,
                description = "Allows the app to take pictures and videos with any camera.",
            ),
            modifier = Modifier.width(400.dp),
        )
    }
}
