package sk.styk.martin.apkanalyzer.feature.appdetail.tv

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
import sk.styk.martin.apkanalyzer.core.apps.UnparsableApkFileException
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentKind
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.DeviceFeatures
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.DeviceFeaturesRepository
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.requirements
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.signing.CertificateTrustLevel
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.userpreferences.recentlyviewed.RecentlyViewedAppsRepository
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.AppDetailSectionRepository
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.analytics.AppDetailAnalytics
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.analytics.AppDetailAnalyticsEvent
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.insight.AppDetailBadgeEvaluator
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.insight.AppDetailInsight
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.insight.AppDetailInsightEvaluator
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.installedPackage
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.liveReference

@HiltViewModel(assistedFactory = TvAppDetailViewModel.Factory::class)
internal class TvAppDetailViewModel @AssistedInject constructor(
    @Assisted private val appDetailInput: AppDetailInput,
    private val sectionRepository: AppDetailSectionRepository,
    private val deviceFeaturesRepository: DeviceFeaturesRepository,
    private val insightEvaluator: AppDetailInsightEvaluator,
    private val badgeEvaluator: AppDetailBadgeEvaluator,
    private val recentlyViewedAppsRepository: RecentlyViewedAppsRepository,
    private val dispatcherProvider: DispatcherProvider,
    private val analytics: AppDetailAnalytics,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(appDetailInput: AppDetailInput): TvAppDetailViewModel
    }

    private val appReference = checkNotNull(appDetailInput.liveReference)

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

            TvAppDetailAction.OpenAppInfo -> withLoadedState {
                analytics.track(AppDetailAnalyticsEvent.ActionPerformed(AppDetailAnalyticsEvent.Action.OpenAppInfo, appDetailInput))
                eventChannel.trySend(TvAppDetailEvent.OpenAppInfo(it.packageName))
            }

            TvAppDetailAction.OpenPlayStore -> withLoadedState {
                analytics.track(AppDetailAnalyticsEvent.ActionPerformed(AppDetailAnalyticsEvent.Action.OpenPlayStore, appDetailInput))
                eventChannel.trySend(TvAppDetailEvent.OpenPlayStore(it.packageName))
            }

            TvAppDetailAction.OpenGeneralInfo -> sendSectionEvent(TvAppDetailEvent.NavigateToGeneralInfo)

            TvAppDetailAction.OpenPermissions -> sendSectionEvent(TvAppDetailEvent.NavigateToPermissions(permissionName = null))

            TvAppDetailAction.OpenComponents -> sendSectionEvent(TvAppDetailEvent.NavigateToComponents)

            TvAppDetailAction.OpenCertificates -> sendSectionEvent(TvAppDetailEvent.NavigateToCertificates)

            TvAppDetailAction.OpenRequirements -> sendSectionEvent(TvAppDetailEvent.NavigateToRequirements)

            TvAppDetailAction.OpenManifest -> sendSectionEvent(TvAppDetailEvent.NavigateToManifest)

            is TvAppDetailAction.OpenInsight -> sendSectionEvent(action.insight.destination())
        }
    }

    private fun sendSectionEvent(event: TvAppDetailEvent) {
        analytics.track(AppDetailAnalyticsEvent.SectionOpened(event.toSection(), appDetailInput))
        eventChannel.trySend(event)
    }

    private fun withLoadedState(block: (TvAppDetailState.Loaded) -> Unit) {
        (mutableState.value as? TvAppDetailState.Loaded)?.let(block)
    }

    private fun loadDetail() {
        mutableState.value = TvAppDetailState.Loading
        viewModelScope.launch(dispatcherProvider.default()) {
            val deviceFeatures = deviceFeaturesRepository.deviceFeatures()
            val detailResult = sectionRepository.detail(appDetailInput)
            mutableState.value = detailResult.fold(
                onSuccess = {
                    analytics.track(AppDetailAnalyticsEvent.Opened(appDetailInput))
                    it.toLoadedState(deviceFeatures)
                },
                onFailure = { TvAppDetailState.Error(canRetry = it !is UnparsableApkFileException) },
            )
            val installedPackage = appDetailInput.installedPackage
            if (detailResult.isSuccess && installedPackage != null) {
                recentlyViewedAppsRepository.addRecent(installedPackage)
            }
        }
    }

    private fun TvAppDetailEvent.toSection(): AppDetailAnalyticsEvent.Section = when (this) {
        TvAppDetailEvent.NavigateToGeneralInfo -> AppDetailAnalyticsEvent.Section.General

        is TvAppDetailEvent.NavigateToPermissions -> AppDetailAnalyticsEvent.Section.Permissions

        TvAppDetailEvent.NavigateToComponents -> AppDetailAnalyticsEvent.Section.Components

        TvAppDetailEvent.NavigateToCertificates -> AppDetailAnalyticsEvent.Section.Certificates

        TvAppDetailEvent.NavigateToRequirements -> AppDetailAnalyticsEvent.Section.Features

        TvAppDetailEvent.NavigateToManifest -> AppDetailAnalyticsEvent.Section.Manifest

        is TvAppDetailEvent.OpenAppInfo,
        is TvAppDetailEvent.OpenPlayStore,
        -> error("Not a section navigation event")
    }

    private fun AppDetail.toLoadedState(deviceFeatures: DeviceFeatures): TvAppDetailState.Loaded {
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
            badges = badgeEvaluator.evaluate(info, input = appDetailInput),
            insights = insightEvaluator.evaluate(appDetail = this, input = appDetailInput),
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
