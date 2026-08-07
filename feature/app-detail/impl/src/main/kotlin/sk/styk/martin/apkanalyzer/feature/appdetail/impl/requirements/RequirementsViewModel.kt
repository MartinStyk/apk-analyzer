package sk.styk.martin.apkanalyzer.feature.appdetail.impl.requirements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepository
import sk.styk.martin.apkanalyzer.core.apps.DeviceFeaturesRepository
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.model.DeviceFeatures
import sk.styk.martin.apkanalyzer.core.apps.model.Feature
import sk.styk.martin.apkanalyzer.core.apps.model.FeatureAvailability
import sk.styk.martin.apkanalyzer.core.common.clipboard.ClipboardManager
import sk.styk.martin.apkanalyzer.core.common.clipboard.CopyResult
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import java.io.File

private const val TAG = "RequirementsViewModel"

@HiltViewModel(assistedFactory = RequirementsViewModel.Factory::class)
internal class RequirementsViewModel @AssistedInject constructor(
    @Assisted private val appDetailInput: AppDetailInput,
    private val appDetailRepository: AppDetailRepository,
    private val deviceFeaturesRepository: DeviceFeaturesRepository,
    private val dispatcherProvider: DispatcherProvider,
    private val clipboardManager: ClipboardManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(target: AppDetailInput): RequirementsViewModel
    }

    val state: StateFlow<RequirementsState>
        field = MutableStateFlow<RequirementsState>(RequirementsState.Loading)

    private val _events = Channel<RequirementsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadRequirements()
    }

    fun onAction(action: RequirementsAction) {
        when (action) {
            is RequirementsAction.Retry -> loadRequirements()

            is RequirementsAction.CopyValue -> {
                if (clipboardManager.copy(action.label, action.value) == CopyResult.FeedbackNotShown) {
                    viewModelScope.launch { _events.send(RequirementsEvent.ShowCopiedFeedback) }
                }
            }
        }
    }

    private fun loadRequirements() {
        state.value = RequirementsState.Loading
        viewModelScope.launch {
            val deviceFeatures = deviceFeaturesRepository.deviceFeatures()
            state.value = withContext(dispatcherProvider.default()) {
                when (appDetailInput) {
                    is AppDetailInput.InstalledPackage -> appDetailRepository.installedPackageDetails(appDetailInput.packageName)
                    is AppDetailInput.ApkFile -> appDetailRepository.apkFilePackageDetails(File(appDetailInput.apkFilePath))
                }
            }.onFailure {
                Logger.e(TAG, it, "Can not load requirements for $appDetailInput")
            }.fold(
                onSuccess = { it.toRequirementsState(deviceFeatures) },
                onFailure = { RequirementsState.Error },
            )
        }
    }
}

private fun AppDetail.toRequirementsState(deviceFeatures: DeviceFeatures): RequirementsState.Loaded {
    val (required, optional) = features
        .map { it.toRequirementItem(deviceFeatures) }
        .sortedByDescending { it.isRequired }
        .distinctBy { it.identifier }
        .partition { it.isRequired }

    return RequirementsState.Loaded(
        sections = listOfNotNull(
            required.toSectionOrNull(isRequired = true),
            optional.toSectionOrNull(isRequired = false),
        ).toImmutableList(),
        missingRequiredCount = required.count { it.availability == FeatureAvailability.Missing },
    )
}

private fun List<RequirementItem>.toSectionOrNull(isRequired: Boolean) = takeIf { it.isNotEmpty() }
    ?.let {
        RequirementSection(
            isRequired = isRequired,
            requirements = it.sortedWith(requirementOrder).toImmutableList(),
        )
    }

private val requirementOrder = compareBy<RequirementItem>(
    { it.availability != FeatureAvailability.Missing },
    { it is RequirementItem.OpenGlEs },
    { it.identifier },
)

private fun Feature.toRequirementItem(deviceFeatures: DeviceFeatures): RequirementItem {
    val availability = deviceFeatures.availabilityOf(this)
    return when (this) {
        is Feature.Hardware -> RequirementItem.Hardware(
            name = name,
            requiredVersion = version,
            deviceVersion = deviceFeatures.versionOf(name),
            isRequired = isRequired,
            availability = availability,
        )

        is Feature.OpenGlEs -> RequirementItem.OpenGlEs(
            versionName = versionName,
            deviceVersionName = deviceFeatures.openGlEsVersionName,
            identifier = "0x%08X".format(reqGlEsVersion),
            isRequired = isRequired,
            availability = availability,
        )
    }
}
