package sk.styk.martin.apkanalyzer.feature.appdetail.phone.requirements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.Feature
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.FeatureAvailability
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.RequirementItem
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.availabilityExplanationRes
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.labelRes
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.requirementVersionName
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.BottomSheet
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Icon
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.DetailField
import sk.styk.martin.apkanalyzer.core.apps.R as AppsR

@Composable
internal fun RequirementDetailBottomSheet(
    item: RequirementItem,
    label: String,
    onCopy: (label: String, value: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    showAvailability: Boolean = true,
) {
    BottomSheet(onDismiss = onDismiss, modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = item.icon,
                    tint = AppTheme.colors.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colors.onSurface,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            DetailField(
                label = stringResource(AppsR.string.requirements_detail_identifier),
                value = item.identifier,
                onCopy = onCopy,
            )
            DetailField(
                label = stringResource(AppsR.string.requirements_detail_necessity),
                value = stringResource(if (item.isRequired) AppsR.string.requirements_section_required else AppsR.string.requirements_section_optional),
                explanation = stringResource(
                    if (item.isRequired) AppsR.string.requirements_section_required_explanation else AppsR.string.requirements_section_optional_explanation,
                ),
                onCopy = onCopy,
            )
            if (showAvailability) {
                DetailField(
                    label = stringResource(AppsR.string.requirements_detail_availability),
                    value = stringResource(item.availability.labelRes),
                    explanation = stringResource(item.availabilityExplanationRes()),
                    onCopy = onCopy,
                )
            }
            item.versionDetail()?.let { versionDetail ->
                DetailField(
                    label = stringResource(AppsR.string.requirements_detail_version),
                    value = versionDetail,
                    onCopy = onCopy,
                )
            }
        }
    }
}

@Composable
private fun RequirementItem.versionDetail(): String? = when (this) {
    is RequirementItem.Hardware -> {
        val deviceVersion = deviceVersion
        when {
            requiredVersion == Feature.VERSION_UNSPECIFIED -> null

            deviceVersion == null -> requirementVersionName(name, requiredVersion)

            else -> stringResource(
                AppsR.string.requirements_detail_version_comparison,
                requirementVersionName(name, requiredVersion),
                requirementVersionName(name, deviceVersion),
            )
        }
    }

    is RequirementItem.OpenGlEs -> when (val deviceVersionName = deviceVersionName) {
        null -> versionName
        else -> stringResource(AppsR.string.requirements_detail_version_comparison, versionName, deviceVersionName)
    }
}

@Preview
@Composable
private fun RequirementDetailBottomSheetPreview() {
    ApkAnalyzerTheme {
        RequirementDetailBottomSheet(
            item = RequirementItem.Hardware(
                name = "android.hardware.vulkan.version",
                requiredVersion = 4198400,
                deviceVersion = 4194304,
                isRequired = true,
                availability = FeatureAvailability.Missing,
            ),
            label = "Vulkan graphics 1.1",
            onCopy = { _, _ -> },
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun RequirementDetailBottomSheetAvailablePreview() {
    ApkAnalyzerTheme {
        RequirementDetailBottomSheet(
            item = RequirementItem.Hardware(
                name = "android.hardware.camera",
                requiredVersion = Feature.VERSION_UNSPECIFIED,
                deviceVersion = 0,
                isRequired = false,
                availability = FeatureAvailability.Available,
            ),
            label = "Camera",
            onCopy = { _, _ -> },
            onDismiss = {},
        )
    }
}
