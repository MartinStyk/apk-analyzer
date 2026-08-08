package sk.styk.martin.apkanalyzer.feature.appdetail.impl.requirements

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import sk.styk.martin.apkanalyzer.core.apps.model.Feature
import sk.styk.martin.apkanalyzer.core.apps.model.FeatureAvailability
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Icon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Toolbar
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.R
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.ListSectionHeader
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.SectionError
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.SectionLoading

@Composable
internal fun RequirementsScreen(
    appDetailInput: AppDetailInput,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RequirementsViewModel = hiltViewModel { factory: RequirementsViewModel.Factory ->
        factory.create(appDetailInput)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                RequirementsEvent.ShowCopiedFeedback -> Toast.makeText(context, R.string.general_info_copied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    RequirementsContent(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun RequirementsContent(
    state: RequirementsState,
    onAction: (RequirementsAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        Toolbar(
            title = stringResource(R.string.requirements_title),
            onBack = onBack,
        )
        when (state) {
            is RequirementsState.Loading -> SectionLoading()

            is RequirementsState.Error -> SectionError(
                message = stringResource(R.string.requirements_error),
                onRetry = { onAction(RequirementsAction.Retry) },
            )

            is RequirementsState.Loaded -> LoadedContent(state = state, onAction = onAction)
        }
    }
}

@Composable
private fun LoadedContent(
    state: RequirementsState.Loaded,
    onAction: (RequirementsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.hasRequirements) {
        EmptyContent(modifier = modifier)
        return
    }

    var detailIdentifier by rememberSaveable { mutableStateOf<String?>(null) }
    val detailSection = remember(detailIdentifier, state.sections) {
        state.sections.firstOrNull { section -> section.requirements.any { it.identifier == detailIdentifier } }
    }
    val detailItem = detailSection?.requirements?.firstOrNull { it.identifier == detailIdentifier }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (state.missingRequiredCount > 0) {
            item(key = MISSING_SUMMARY_KEY) {
                MissingSummary(missingRequiredCount = state.missingRequiredCount)
            }
        }
        state.sections.forEach { section ->
            item(key = section.isRequired) {
                ListSectionHeader(
                    title = stringResource(
                        R.string.requirements_section_header,
                        stringResource(section.labelRes).uppercase(),
                        section.requirements.size,
                    ),
                    explanation = stringResource(section.explanationRes),
                )
            }
            items(items = section.requirements, key = { it.identifier }) { requirement ->
                RequirementRow(
                    item = requirement,

                    onClick = { detailIdentifier = requirement.identifier },
                    onLongClick = { label -> onAction(RequirementsAction.CopyValue(label, requirement.identifier)) },
                )
            }
        }
    }

    if (detailItem != null && detailSection != null) {
        RequirementDetailBottomSheet(
            item = detailItem,

            label = detailItem.label(),
            onCopy = { label, value -> onAction(RequirementsAction.CopyValue(label, value)) },
            onDismiss = { detailIdentifier = null },
        )
    }
}

@Composable
private fun MissingSummary(missingRequiredCount: Int, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .clip(Shapes.CardShape)
            .background(AppTheme.colors.warningContainer)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = ApkAnalyzerIcons.Warning,
            contentDescription = null,
            tint = AppTheme.colors.warning,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = pluralStringResource(R.plurals.requirements_missing_summary, missingRequiredCount, missingRequiredCount),
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurface,
        )
    }
}

@Composable
private fun RequirementRow(
    item: RequirementItem,
    onClick: () -> Unit,
    onLongClick: (label: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = item.label()
    val showIdentifier = label != item.identifier

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(Shapes.CardShape)
            .background(AppTheme.colors.surface)
            .combinedClickable(onClick = onClick, onLongClick = { onLongClick(label) })
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (showIdentifier) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.identifier,
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.StartEllipsis,
                )
            }
        }
        if (item.availability == FeatureAvailability.Missing) {
            Spacer(modifier = Modifier.width(8.dp))
            MissingMarker(item = item)
        }
    }
}

@Composable
private fun MissingMarker(item: RequirementItem, modifier: Modifier = Modifier) {
    Text(
        text = when {
            item is RequirementItem.OpenGlEs && item.deviceVersionName != null ->
                stringResource(R.string.requirements_missing_version, item.versionName, item.deviceVersionName)

            item is RequirementItem.Hardware && item.isVersionMiss ->
                stringResource(
                    R.string.requirements_missing_version,
                    requirementVersionName(item.name, item.requiredVersion),
                    requirementVersionName(item.name, checkNotNull(item.deviceVersion)),
                )

            else -> stringResource(R.string.requirements_missing)
        },
        style = AppTheme.typography.labelMedium,
        color = if (item.isRequired) AppTheme.colors.warning else AppTheme.colors.onSurfaceVariant,
        textAlign = TextAlign.End,
        modifier = modifier.width(96.dp),
    )
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
    ) {
        Text(
            text = stringResource(R.string.requirements_empty),
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colors.onBackground,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RequirementItem.label(): String = when (this) {
    is RequirementItem.Hardware -> {
        val name = labelRes?.let { stringResource(it) } ?: name
        if (requiredVersion == Feature.VERSION_UNSPECIFIED) {
            name
        } else {
            stringResource(R.string.requirements_label_with_version, name, requirementVersionName(identifier, requiredVersion))
        }
    }

    is RequirementItem.OpenGlEs -> stringResource(R.string.requirements_open_gl_es, versionName)
}

private const val MISSING_SUMMARY_KEY = "missing_summary"

@Preview
@Composable
private fun RequirementsLoadingPreview() {
    ApkAnalyzerTheme {
        RequirementsContent(
            state = RequirementsState.Loading,
            onAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun RequirementsErrorPreview() {
    ApkAnalyzerTheme {
        RequirementsContent(
            state = RequirementsState.Error,
            onAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun RequirementsLoadedPreview() {
    ApkAnalyzerTheme {
        RequirementsContent(
            state = sampleLoadedState(),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun RequirementsEmptyPreview() {
    ApkAnalyzerTheme {
        RequirementsContent(
            state = RequirementsState.Loaded(sections = persistentListOf(), missingRequiredCount = 0),
            onAction = {},
            onBack = {},
        )
    }
}

private fun sampleLoadedState() = RequirementsState.Loaded(
    missingRequiredCount = 4,
    sections = persistentListOf(
        RequirementSection(
            isRequired = true,
            requirements = persistentListOf(
                RequirementItem.Hardware(
                    name = "android.hardware.nfc",
                    requiredVersion = Feature.VERSION_UNSPECIFIED,
                    deviceVersion = null,
                    isRequired = true,
                    availability = FeatureAvailability.Missing,
                ),
                RequirementItem.Hardware(
                    name = "android.hardware.vulkan.version",
                    requiredVersion = 4198400,
                    deviceVersion = 4194304,
                    isRequired = true,
                    availability = FeatureAvailability.Missing,
                ),
                RequirementItem.Hardware(
                    name = "android.hardware.camera",
                    requiredVersion = Feature.VERSION_UNSPECIFIED,
                    deviceVersion = 0,
                    isRequired = true,
                    availability = FeatureAvailability.Available,
                ),
                RequirementItem.Hardware(
                    name = "android.hardware.wifi",
                    requiredVersion = Feature.VERSION_UNSPECIFIED,
                    deviceVersion = 0,
                    isRequired = true,
                    availability = FeatureAvailability.Available,
                ),
                RequirementItem.Hardware(
                    name = "com.sonymobile.hardware.tv",
                    requiredVersion = Feature.VERSION_UNSPECIFIED,
                    deviceVersion = null,
                    isRequired = true,
                    availability = FeatureAvailability.Missing,
                ),
                RequirementItem.OpenGlEs(
                    versionName = "3.1",
                    deviceVersionName = "3.0",
                    identifier = "0x00030001",
                    isRequired = true,
                    availability = FeatureAvailability.Missing,
                ),
            ),
        ),
        RequirementSection(
            isRequired = false,
            requirements = persistentListOf(
                RequirementItem.Hardware(
                    name = "android.hardware.bluetooth_le",
                    requiredVersion = Feature.VERSION_UNSPECIFIED,
                    deviceVersion = 0,
                    isRequired = false,
                    availability = FeatureAvailability.Available,
                ),
                RequirementItem.Hardware(
                    name = "android.hardware.sensor.heartrate",
                    requiredVersion = Feature.VERSION_UNSPECIFIED,
                    deviceVersion = null,
                    isRequired = false,
                    availability = FeatureAvailability.Missing,
                ),
            ),
        ),
    ),
)
