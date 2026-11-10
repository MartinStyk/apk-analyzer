package sk.styk.martin.apkanalyzer.feature.appdetail.tv.requirements

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.FeatureAvailability
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.RequirementItem
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.RequirementSection
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.Requirements
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.availabilityExplanationRes
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.explanationRes
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.label
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.labelRes
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.missingText
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.requirementIcon
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.iconReference
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.R
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.InfoRow
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.InfoSection
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.InfoSectionList
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.SectionEmpty
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.SectionError
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.SectionLoading
import sk.styk.martin.apkanalyzer.core.apps.R as AppsR

@Composable
internal fun TvRequirementsScreen(
    appDetailInput: AppDetailInput,
    viewModel: TvRequirementsViewModel = hiltViewModel { factory: TvRequirementsViewModel.Factory ->
        factory.create(appDetailInput)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TvRequirementsContent(state = state, appReference = appDetailInput.iconReference, onAction = viewModel::onAction)
}

@Composable
private fun TvRequirementsContent(
    state: TvRequirementsState,
    appReference: AppReference,
    onAction: (TvRequirementsAction) -> Unit,
) {
    when (state) {
        TvRequirementsState.Loading -> SectionLoading()

        TvRequirementsState.Error -> SectionError(
            message = stringResource(AppsR.string.requirements_error),
            onRetry = { onAction(TvRequirementsAction.Retry) },
        )

        is TvRequirementsState.Loaded -> if (state.requirements.sections.isEmpty()) {
            SectionEmpty(
                title = stringResource(AppsR.string.requirements_title),
                subtitle = state.appName,
                appReference = appReference,
                message = stringResource(AppsR.string.requirements_empty),
            )
        } else {
            InfoSectionList(
                title = stringResource(AppsR.string.requirements_title),
                subtitle = state.appName,
                appReference = appReference,
                sections = state.requirements.sections.map { requirementSection(it) },
            )
        }
    }
}

@Composable
private fun requirementSection(section: RequirementSection): InfoSection {
    val resources = LocalResources.current
    val identifierLabel = stringResource(AppsR.string.requirements_detail_identifier)
    val sectionExplanation = stringResource(section.explanationRes)
    return InfoSection(
        title = stringResource(AppsR.string.requirements_section_header, stringResource(section.labelRes), section.requirements.size),
        rows = section.requirements.map { item ->
            InfoRow(
                key = "${section.isRequired}:${item.identifier}",
                label = item.label(resources),
                value = when (item.availability) {
                    FeatureAvailability.Missing -> item.missingText(resources)
                    else -> resources.getString(item.availability.labelRes)
                },
                explanation = listOf(
                    resources.getString(item.availabilityExplanationRes()),
                    sectionExplanation,
                    resources.getString(R.string.tv_label_value, identifierLabel, item.identifier),
                ).joinToString(separator = "\n\n"),
                highlighted = item.isRequired && item.availability == FeatureAvailability.Missing,
                icon = when (item) {
                    is RequirementItem.Hardware -> requirementIcon(item.name)
                    is RequirementItem.OpenGlEs -> ApkAnalyzerIcons.Graphics
                },
                status = resources.getString(item.availability.labelRes),
            )
        },
    )
}

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TvRequirementsContentPreview() {
    ApkAnalyzerTvTheme {
        TvRequirementsContent(
            appReference = AppReference.InstalledPackage(PackageName("com.google.android.youtube.tv")),
            state = TvRequirementsState.Loaded(
                appName = "Camera Pro",
                requirements = Requirements(
                    sections = listOf(
                        RequirementSection(
                            isRequired = true,
                            requirements = listOf(
                                RequirementItem.Hardware(
                                    name = "android.hardware.touchscreen",
                                    requiredVersion = 0,
                                    deviceVersion = null,
                                    isRequired = true,
                                    availability = FeatureAvailability.Missing,
                                ),
                            ),
                        ),
                    ),
                    missingRequiredCount = 1,
                ),
            ),
            onAction = {},
        )
    }
}
