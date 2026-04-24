package sk.styk.martin.apkanalyzer.feature.settings.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.common.settings.Key
import sk.styk.martin.apkanalyzer.core.common.settings.PersistenceRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val persistenceRepository: PersistenceRepository) : ViewModel() {

    private val eventChannel = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    val state = combine(
        persistenceRepository.observe(Key.ColorScheme),
        persistenceRepository.observe(Key.RecentlyViewedAppsEnabled),
    ) { colorScheme, recentlyViewedEnabled ->
        SettingsState(
            colorScheme = colorScheme,
            recentlyViewedAppsEnabled = recentlyViewedEnabled,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsState())

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.ColorSchemeSelected -> {
                viewModelScope.launch { persistenceRepository.save(Key.ColorScheme, action.colorScheme) }
            }

            is SettingsAction.RecentlyViewedAppsToggled -> {
                viewModelScope.launch { persistenceRepository.save(Key.RecentlyViewedAppsEnabled, action.enabled) }
            }

            is SettingsAction.NavigateBack -> {
                eventChannel.trySend(SettingsEvent.NavigateBack)
            }
        }
    }
}
