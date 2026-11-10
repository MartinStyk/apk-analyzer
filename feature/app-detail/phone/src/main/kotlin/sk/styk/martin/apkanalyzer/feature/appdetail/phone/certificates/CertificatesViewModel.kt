package sk.styk.martin.apkanalyzer.feature.appdetail.phone.certificates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepository
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.signing.Certificate
import sk.styk.martin.apkanalyzer.core.apps.signing.SigningSchemeRepository
import sk.styk.martin.apkanalyzer.core.apps.signing.SigningSchemeVersion
import sk.styk.martin.apkanalyzer.core.apps.signing.validityAt
import sk.styk.martin.apkanalyzer.core.common.clipboard.ClipboardManager
import sk.styk.martin.apkanalyzer.core.common.clipboard.CopyResult
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.toAppReference
import java.time.Instant

@HiltViewModel(assistedFactory = CertificatesViewModel.Factory::class)
internal class CertificatesViewModel @AssistedInject constructor(
    @Assisted private val appDetailInput: AppDetailInput,
    private val appDetailRepository: AppDetailRepository,
    private val signingSchemeRepository: SigningSchemeRepository,
    private val dispatcherProvider: DispatcherProvider,
    private val clipboardManager: ClipboardManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(target: AppDetailInput): CertificatesViewModel
    }

    val state: StateFlow<CertificatesState>
        field = MutableStateFlow<CertificatesState>(CertificatesState.Loading)

    private val eventChannel = Channel<CertificatesEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        loadCertificates()
    }

    fun onAction(action: CertificatesAction) {
        when (action) {
            CertificatesAction.Retry -> loadCertificates()

            is CertificatesAction.CopyValue -> {
                if (clipboardManager.copy(action.label, action.value) == CopyResult.FeedbackNotShown) {
                    viewModelScope.launch { eventChannel.send(CertificatesEvent.ShowCopiedFeedback) }
                }
            }
        }
    }

    private fun loadCertificates() {
        state.value = CertificatesState.Loading
        viewModelScope.launch(dispatcherProvider.default()) {
            val reference = appDetailInput.toAppReference()
            appDetailRepository.details(reference)
                .onSuccess { detail ->
                    state.value = detail.toCertificatesState(signingSchemeVersions = null)
                    val signingSchemeVersions = signingSchemeRepository.signingSchemeVersions(reference).getOrNull()
                    state.value = detail.toCertificatesState(signingSchemeVersions = signingSchemeVersions)
                }
                .onFailure {
                    state.value = CertificatesState.Error
                }
        }
    }
}

private fun AppDetail.toCertificatesState(signingSchemeVersions: List<SigningSchemeVersion>?): CertificatesState.Loaded {
    val now = Instant.now()
    return CertificatesState.Loaded(
        currentCertificates = signing.currentCertificates.map { it.toCertificateItem(now) },
        pastCertificates = signing.pastCertificates.reversed().map { it.toCertificateItem(now) },
        hasMultipleSigners = signing.hasMultipleSigners,
        signingSchemeVersions = signingSchemeVersions,
    )
}

private fun Certificate.toCertificateItem(now: Instant) = CertificateItem(
    signAlgorithm = signAlgorithm,
    signatureAlgorithmAssessment = signatureAlgorithmAssessment,
    certificateHashMd5 = certificateHashMd5,
    certificateHashSha1 = certificateHashSha1,
    certificateHashSha256 = certificateHashSha256,
    publicKeyMd5 = publicKeyMd5,
    publicKeySha1 = publicKeySha1,
    publicKeySha256 = publicKeySha256,
    validFrom = validFrom,
    validUntil = validUntil,
    serialNumber = serialNumber,
    issuer = issuer,
    subject = subject,
    trustLevel = trustLevel,
    isSelfSigned = isSelfSigned,
    validity = validityAt(now),
)
