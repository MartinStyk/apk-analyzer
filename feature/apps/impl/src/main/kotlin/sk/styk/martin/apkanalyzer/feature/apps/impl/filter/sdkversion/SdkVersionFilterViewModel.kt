package sk.styk.martin.apkanalyzer.feature.apps.impl.filter.sdkversion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.apps.sdkversion.SdkVersionResolver
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.SdkVersionFilterCoordinator
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.SdkVersionFilterDraft
import javax.inject.Inject

@HiltViewModel
class SdkVersionFilterViewModel @Inject constructor(
    private val sdkVersionFilterCoordinator: SdkVersionFilterCoordinator,
    private val sdkVersionResolver: SdkVersionResolver,
    installedAppsRepository: InstalledAppsRepository,
) : ViewModel() {

    private val input = sdkVersionFilterCoordinator.consumeInput()
    private val selectedSdkVersions = MutableStateFlow(input.selectedSdkVersions.toPersistentSet())

    private val eventChannel = Channel<SdkVersionFilterEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    val state = combine(installedAppsRepository.apps(), selectedSdkVersions) { apps, selected ->
        val options = apps.map { it.targetSdk }.distinct()
            .sortedDescending()
            .map { sdkVersion ->
                SdkVersionOption(
                    sdkVersion = sdkVersion,
                    isSelected = sdkVersion in selected,
                    androidVersionName = sdkVersionResolver.resolveVersion(sdkVersion),
                )
            }
            .toImmutableList()
        SdkVersionFilterState(options = options)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SdkVersionFilterState())

    fun onAction(action: SdkVersionFilterAction) {
        when (action) {
            is SdkVersionFilterAction.SdkVersionToggled -> selectedSdkVersions.update { current ->
                if (action.sdkVersion in current) {
                    (current - action.sdkVersion).toPersistentSet()
                } else {
                    (current + action.sdkVersion).toPersistentSet()
                }
            }

            SdkVersionFilterAction.Reset -> selectedSdkVersions.value = persistentSetOf()

            SdkVersionFilterAction.NavigateBack -> {
                sdkVersionFilterCoordinator.submitResult(SdkVersionFilterDraft(selectedSdkVersions = selectedSdkVersions.value))
                eventChannel.trySend(SdkVersionFilterEvent.NavigateBack)
            }
        }
    }
}
