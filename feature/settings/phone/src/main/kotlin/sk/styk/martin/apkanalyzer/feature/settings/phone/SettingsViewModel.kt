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
import sk.styk.martin.apkanalyzer.core.entitlements.api.Entitlement
import sk.styk.martin.apkanalyzer.core.entitlements.api.EntitlementsRepository
import javax.inject.Inject

@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val persistenceRepository: PersistenceRepository,
    private val appLanguageRepository: AppLanguageRepository,
    entitlementsRepository: EntitlementsRepository,
) : ViewModel() {

    private val eventChannel = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    val state = combine(
        persistenceRepository.observe(Key.ColorScheme),
        persistenceRepository.observe(Key.HexFingerprintFormat),
        persistenceRepository.observe(Key.RecentlyViewedAppsEnabled),
        entitlementsRepository.entitlements(),
        entitlementsRepository.offers(),
    ) { colorScheme, fingerprintFormat, recentlyViewedEnabled, entitlements, offers ->
        SettingsState(
            colorScheme = colorScheme,
            fingerprintFormat = fingerprintFormat,
            recentlyViewedAppsEnabled = recentlyViewedEnabled,
            appLanguageSetting = this.appLanguageRepository.getAppLanguageSetting(),
            pro = when {
                offers.none { it.entitlement == Entitlement.Pro } -> SettingsPro.Hidden
                Entitlement.Pro in entitlements -> SettingsPro.Owned
                else -> SettingsPro.NotOwned
            },
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

            SettingsAction.OpenPro -> {
                eventChannel.trySend(SettingsEvent.OpenPro)
            }

            SettingsAction.OpenFeatureFlags -> {
                if (BuildConfig.DEBUG) eventChannel.trySend(SettingsEvent.OpenFeatureFlags)
            }

            is SettingsAction.NavigateBack -> {
                eventChannel.trySend(SettingsEvent.NavigateBack)
            }
        }
    }
}
