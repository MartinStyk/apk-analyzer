package sk.styk.martin.apkanalyzer.feature.appdetail.tv.certificates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.signing.SigningSchemeVersion
import sk.styk.martin.apkanalyzer.core.apps.signing.validityAt
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.AppDetailSectionRepository
import java.time.Instant

@HiltViewModel(assistedFactory = TvCertificatesViewModel.Factory::class)
internal class TvCertificatesViewModel @AssistedInject constructor(
    @Assisted private val appDetailInput: AppDetailInput,
    private val sectionRepository: AppDetailSectionRepository,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(appDetailInput: AppDetailInput): TvCertificatesViewModel
    }

    private val mutableState = MutableStateFlow<TvCertificatesState>(TvCertificatesState.Loading)
    val state: StateFlow<TvCertificatesState> = mutableState.asStateFlow()

    init {
        loadCertificates()
    }

    fun onAction(action: TvCertificatesAction) {
        when (action) {
            TvCertificatesAction.Retry -> loadCertificates()
        }
    }

    private fun loadCertificates() {
        mutableState.value = TvCertificatesState.Loading
        viewModelScope.launch(dispatcherProvider.default()) {
            val asOf = Instant.now()
            sectionRepository.detail(appDetailInput)
                .onSuccess { detail ->
                    mutableState.value = detail.toState(signingSchemeVersions = null, asOf = asOf)
                    val signingSchemeVersions = sectionRepository.signingSchemes(appDetailInput).getOrNull()
                    mutableState.value = detail.toState(signingSchemeVersions = signingSchemeVersions, asOf = asOf)
                }
                .onFailure { mutableState.value = TvCertificatesState.Error }
        }
    }
}

private fun AppDetail.toState(signingSchemeVersions: List<SigningSchemeVersion>?, asOf: Instant): TvCertificatesState.Loaded = TvCertificatesState.Loaded(
    appName = info.applicationName,
    currentCertificates = signing.currentCertificates.map { TvCertificate(it, it.validityAt(asOf)) },
    pastCertificates = signing.pastCertificates.reversed().map { TvCertificate(it, it.validityAt(asOf)) },
    hasMultipleSigners = signing.hasMultipleSigners,
    signingSchemeVersions = signingSchemeVersions,
)
