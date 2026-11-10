package sk.styk.martin.apkanalyzer.feature.appdetail.tv

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepository
import sk.styk.martin.apkanalyzer.core.apps.UnparsableApkFileException
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentKind
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.DeviceFeatures
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.DeviceFeaturesRepository
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.requirements
import sk.styk.martin.apkanalyzer.core.apps.insight.AppDetailBadgeEvaluator
import sk.styk.martin.apkanalyzer.core.apps.insight.AppDetailInsight
import sk.styk.martin.apkanalyzer.core.apps.insight.AppDetailInsightEvaluator
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.signing.CertificateTrustLevel
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.userpreferences.recentlyviewed.RecentlyViewedAppsRepository
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import java.time.Instant

@HiltViewModel(assistedFactory = TvAppDetailViewModel.Factory::class)
internal class TvAppDetailViewModel @AssistedInject constructor(
    @Assisted appDetailInput: AppDetailInput,
    private val appDetailRepository: AppDetailRepository,
    private val recentlyViewedAppsRepository: RecentlyViewedAppsRepository,
    private val deviceFeaturesRepository: DeviceFeaturesRepository,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(appDetailInput: AppDetailInput): TvAppDetailViewModel
    }

    private val appReference = appDetailInput.toAppReference()

    private val mutableState = MutableStateFlow<TvAppDetailState>(TvAppDetailState.Loading)
    val state: StateFlow<TvAppDetailState> = mutableState.asStateFlow()

    private val eventChannel = Channel<TvAppDetailEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        loadDetail()
    }

    fun onAction(action: TvAppDetailAction) {
        when (action) {
            TvAppDetailAction.Retry -> loadDetail()
            TvAppDetailAction.OpenAppInfo -> withLoadedState { eventChannel.trySend(TvAppDetailEvent.OpenAppInfo(it.packageName)) }
            TvAppDetailAction.OpenPlayStore -> withLoadedState { eventChannel.trySend(TvAppDetailEvent.OpenPlayStore(it.packageName)) }
            TvAppDetailAction.OpenGeneralInfo -> eventChannel.trySend(TvAppDetailEvent.NavigateToGeneralInfo)
            TvAppDetailAction.OpenPermissions -> eventChannel.trySend(TvAppDetailEvent.NavigateToPermissions(permissionName = null))
            TvAppDetailAction.OpenComponents -> eventChannel.trySend(TvAppDetailEvent.NavigateToComponents)
            TvAppDetailAction.OpenCertificates -> eventChannel.trySend(TvAppDetailEvent.NavigateToCertificates)
            TvAppDetailAction.OpenRequirements -> eventChannel.trySend(TvAppDetailEvent.NavigateToRequirements)
            TvAppDetailAction.OpenManifest -> eventChannel.trySend(TvAppDetailEvent.NavigateToManifest)
            is TvAppDetailAction.OpenInsight -> eventChannel.trySend(action.insight.destination())
        }
    }

    private fun withLoadedState(block: (TvAppDetailState.Loaded) -> Unit) {
        (mutableState.value as? TvAppDetailState.Loaded)?.let(block)
    }

    private fun loadDetail() {
        mutableState.value = TvAppDetailState.Loading
        viewModelScope.launch(dispatcherProvider.default()) {
            val deviceFeatures = deviceFeaturesRepository.deviceFeatures()
            val detailResult = appDetailRepository.details(appReference)
            mutableState.value = detailResult.fold(
                onSuccess = { it.toLoadedState(deviceFeatures) },
                onFailure = { TvAppDetailState.Error(canRetry = it !is UnparsableApkFileException) },
            )
            if (detailResult.isSuccess && appReference is AppReference.InstalledPackage) {
                recentlyViewedAppsRepository.addRecent(appReference.packageName)
            }
        }
    }

    private fun AppDetail.toLoadedState(deviceFeatures: DeviceFeatures): TvAppDetailState.Loaded {
        val now = Instant.now()
        val sensitivePermissions = permissions.used.filter { it.isDangerous }
        val requirements = requirements(deviceFeatures)
        return TvAppDetailState.Loaded(
            appReference = appReference,
            analysisMode = analysisMode,
            appName = info.applicationName,
            packageName = info.packageName,
            versionName = info.versionName,
            versionCode = info.versionCode,
            targetSdkVersion = info.targetSdkVersion,
            targetSdkLabel = info.targetSdkLabel,
            apkSize = info.apkSize,
            totalSize = info.totalSize,
            lastUpdateTime = info.lastUpdateTime,
            badges = AppDetailBadgeEvaluator.evaluate(info, now),
            insights = AppDetailInsightEvaluator.evaluate(appDetail = this, now = now, deviceSdk = Build.VERSION.SDK_INT),
            totalPermissionsCount = permissions.used.size,
            sensitivePermissionsCount = sensitivePermissions.size,
            grantedSensitivePermissionsCount = sensitivePermissions
                .takeIf { analysisMode == AppDetail.AnalysisMode.InstalledPackage }
                ?.count { it.isGranted },
            permissionLevelCounts = permissions.used.groupingBy { it.permissionData.details?.protectionLevel }.eachCount(),
            componentCounts = mapOf(
                ComponentKind.Activity to activities.size,
                ComponentKind.Service to services.size,
                ComponentKind.Receiver to receivers.size,
                ComponentKind.Provider to contentProviders.size,
            ),
            signerName = signing.currentCertificates.firstOrNull()?.issuer?.displayName,
            isDebugSigned = signing.currentCertificates.any { it.trustLevel == CertificateTrustLevel.Debug },
            sha256Fingerprint = signing.currentCertificates.firstOrNull()?.certificateHashSha256,
            requiredFeaturesCount = requirements.sections.firstOrNull { it.isRequired }?.requirements?.size ?: 0,
            optionalFeaturesCount = requirements.sections.firstOrNull { !it.isRequired }?.requirements?.size ?: 0,
            missingRequiredFeaturesCount = requirements.missingRequiredCount,
        )
    }
}

private fun AppDetailInsight.destination(): TvAppDetailEvent = when (this) {
    AppDetailInsight.Debuggable,
    AppDetailInsight.Sideloaded,
    is AppDetailInsight.OutdatedTargetSdk,
    is AppDetailInsight.Unused,
    -> TvAppDetailEvent.NavigateToGeneralInfo

    is AppDetailInsight.SensitivePermission -> TvAppDetailEvent.NavigateToPermissions(permissionName)

    AppDetailInsight.DebugCertificate,
    AppDetailInsight.CertificateNotYetValid,
    -> TvAppDetailEvent.NavigateToCertificates
}
