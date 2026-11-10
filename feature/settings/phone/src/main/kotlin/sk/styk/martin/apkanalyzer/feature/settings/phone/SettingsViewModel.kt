package sk.styk.martin.apkanalyzer.feature.settings.phone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.common.applanguage.AppLanguageRepository
import sk.styk.martin.apkanalyzer.core.common.settings.Key
import sk.styk.martin.apkanalyzer.core.common.settings.PersistenceRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val persistenceRepository: PersistenceRepository,
    private val appLanguageRepository: AppLanguageRepository,
) : ViewModel() {

    private val eventChannel = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    val state = combine(
        persistenceRepository.observe(Key.ColorScheme),
        persistenceRepository.observe(Key.HexFingerprintFormat),
        persistenceRepository.observe(Key.RecentlyViewedAppsEnabled),
    ) { colorScheme, fingerprintFormat, recentlyViewedEnabled ->
        SettingsState(
            colorScheme = colorScheme,
            fingerprintFormat = fingerprintFormat,
            recentlyViewedAppsEnabled = recentlyViewedEnabled,
            appLanguageSetting = this.appLanguageRepository.getAppLanguageSetting(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsState())

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.ColorSchemeSelected -> {
                viewModelScope.launch { persistenceRepository.save(Key.ColorScheme, action.colorScheme) }
            }

            is SettingsAction.FingerprintFormatSelected -> {
                viewModelScope.launch { persistenceRepository.save(Key.HexFingerprintFormat, action.fingerprintFormat) }
            }

            is SettingsAction.RecentlyViewedAppsToggled -> {
                viewModelScope.launch { persistenceRepository.save(Key.RecentlyViewedAppsEnabled, action.enabled) }
            }

            is SettingsAction.OpenLanguageSettings -> {
                eventChannel.trySend(SettingsEvent.OpenAppLanguageSettings)
            }

            is SettingsAction.NavigateBack -> {
                eventChannel.trySend(SettingsEvent.NavigateBack)
            }
        }
    }
}
