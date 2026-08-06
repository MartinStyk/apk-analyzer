package sk.styk.martin.apkanalyzer.feature.appdetail.impl.certificates

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
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.model.Certificate
import sk.styk.martin.apkanalyzer.core.common.clipboard.ClipboardManager
import sk.styk.martin.apkanalyzer.core.common.clipboard.CopyResult
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

private const val TAG = "CertificatesViewModel"

@HiltViewModel(assistedFactory = CertificatesViewModel.Factory::class)
internal class CertificatesViewModel @AssistedInject constructor(
    @Assisted private val appDetailInput: AppDetailInput,
    private val appDetailRepository: AppDetailRepository,
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
        viewModelScope.launch {
            state.value = withContext(dispatcherProvider.default()) {
                when (appDetailInput) {
                    is AppDetailInput.InstalledPackage -> appDetailRepository.installedPackageDetails(appDetailInput.packageName)
                    is AppDetailInput.ApkFile -> appDetailRepository.apkFilePackageDetails(File(appDetailInput.apkFilePath))
                }.onFailure {
                    Logger.e(TAG, it, "Can not load certificates for $appDetailInput")
                }.fold(
                    onSuccess = { it.toCertificatesState() },
                    onFailure = { CertificatesState.Error },
                )
            }
        }
    }
}

private fun AppDetail.toCertificatesState(): CertificatesState.Loaded {
    val now = Instant.now()
    val zoneId = ZoneId.systemDefault()
    return CertificatesState.Loaded(
        currentCertificates = signing.currentCertificates.map { it.toCertificateItem(now, zoneId) }.toImmutableList(),
        pastCertificates = signing.pastCertificates.reversed().map { it.toCertificateItem(now, zoneId) }.toImmutableList(),
    )
}

private fun Certificate.toCertificateItem(now: Instant, zoneId: ZoneId) = CertificateItem(
    signAlgorithm = signAlgorithm,
    signatureAlgorithmStrength = signatureAlgorithmStrength,
    certificateHashMd5 = certificateHashMd5.toFingerprint(),
    certificateHashSha1 = certificateHashSha1.toFingerprint(),
    certificateHashSha256 = certificateHashSha256.toFingerprint(),
    publicKeyMd5 = publicKeyMd5.toFingerprint(),
    publicKeySha1 = publicKeySha1.toFingerprint(),
    publicKeySha256 = publicKeySha256.toFingerprint(),
    validFrom = validFrom.atZone(zoneId).toLocalDate(),
    validUntil = validUntil.atZone(zoneId).toLocalDate(),
    serialNumber = serialNumber,
    issuer = issuer,
    subject = subject,
    trustLevel = trustLevel,
    isSelfSigned = isSelfSigned,
    validity = when {
        now.isBefore(validFrom) -> CertificateValidity.NotYetValid
        now.isAfter(validUntil) -> CertificateValidity.Expired
        else -> CertificateValidity.Valid
    },
)

private fun String.toFingerprint(): String = uppercase(Locale.ROOT).chunked(2).joinToString(":")
