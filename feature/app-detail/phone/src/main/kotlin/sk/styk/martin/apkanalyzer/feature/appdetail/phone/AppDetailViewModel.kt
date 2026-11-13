package sk.styk.martin.apkanalyzer.feature.appdetail.phone

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.apkfiles.TemporaryApkManager
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.AppHistoryRepository
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryTimeline
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.overview
import sk.styk.martin.apkanalyzer.core.apppermissions.PermissionLabelProvider
import sk.styk.martin.apkanalyzer.core.apps.UnparsableApkFileException
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.DeviceFeatures
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.DeviceFeaturesRepository
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.Feature
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.FeatureAvailability
import sk.styk.martin.apkanalyzer.core.apps.export.AppExportManager
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.common.clipboard.ClipboardManager
import sk.styk.martin.apkanalyzer.core.common.clipboard.CopyResult
import sk.styk.martin.apkanalyzer.core.common.configuration.ConfigurationKey
import sk.styk.martin.apkanalyzer.core.common.configuration.ConfigurationRepository
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.review.ReviewEligibilityTracker
import sk.styk.martin.apkanalyzer.core.userpreferences.recentlyviewed.RecentlyViewedAppsRepository
import sk.styk.martin.apkanalyzer.feature.appdetail.api.ApkFileLifetime
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.AppDetailSection
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.AppDetailSectionRepository
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.analytics.AppDetailAnalytics
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.analytics.AppDetailAnalyticsEvent
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.insight.AppDetailBadgeEvaluator
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.insight.AppDetailInsight
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.insight.AppDetailInsightEvaluator
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.installedPackage
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.isHistorical
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.isOnDevice
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.liveReference
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.nativeLibrariesOrFallback
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.toSectionErrorReason
import java.time.Instant

private const val TAG = "AppDetailViewModel"

@Suppress("TooManyFunctions")
@HiltViewModel(assistedFactory = AppDetailViewModel.Factory::class)
internal class AppDetailViewModel @AssistedInject constructor(
    @Assisted private val appDetailInput: AppDetailInput,
    private val sectionRepository: AppDetailSectionRepository,
    private val deviceFeaturesRepository: DeviceFeaturesRepository,
    private val insightEvaluator: AppDetailInsightEvaluator,
    private val badgeEvaluator: AppDetailBadgeEvaluator,
    appHistoryRepository: AppHistoryRepository,
    private val appExportManager: AppExportManager,
    private val permissionLabelProvider: PermissionLabelProvider,
    private val dispatcherProvider: DispatcherProvider,
    private val temporaryApkManager: TemporaryApkManager,
    private val recentlyViewedAppsRepository: RecentlyViewedAppsRepository,
    private val clipboardManager: ClipboardManager,
    private val summaryTextFormatter: AppSummaryTextFormatter,
    private val reviewEligibilityTracker: ReviewEligibilityTracker,
    private val analytics: AppDetailAnalytics,
    configurationRepository: ConfigurationRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(target: AppDetailInput): AppDetailViewModel
    }

    private val source = MutableStateFlow<AppDetailSource>(AppDetailSource.Loading)
    private val exportInProgress = MutableStateFlow<AppDetailExport?>(null)
    private val liveReference = appDetailInput.liveReference

    private val historyStatus: StateFlow<AppDetailHistoryStatus> = appDetailInput.installedPackage
        ?.let { installedPackage ->
            configurationRepository.observe(ConfigurationKey.AppHistoryDisplayEnabled).flatMapLatest { isEnabled ->
                if (isEnabled) {
                    appHistoryRepository.timeline(installedPackage).map { it.toHistoryStatus() }
                } else {
                    flowOf(AppDetailHistoryStatus.Hidden)
                }
            }
        }
        .let { it ?: flowOf(AppDetailHistoryStatus.Hidden) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppDetailHistoryStatus.Reading)

    val state: StateFlow<AppDetailState> = combine(
        source,
        exportInProgress,
        configurationRepository.observe(ConfigurationKey.InventoryExportEnabled),
        historyStatus,
    ) { source, exportInProgress, isInventoryExportEnabled, history ->
        when (source) {
            AppDetailSource.Loading -> AppDetailState.Loading

            is AppDetailSource.Error -> AppDetailState.Error(source.reason)

            is AppDetailSource.Ready -> source.state.copy(
                exportInProgress = exportInProgress,
                isInventoryExportEnabled = isInventoryExportEnabled,
                history = history,
            )
        }
    }
        .flowOn(dispatcherProvider.default())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppDetailState.Loading)

    private val eventChannel = Channel<AppDetailEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var activeExport: AppDetailExport? = null
    private val sessionStartedAt = Instant.now()
    private var engaged = false

    init {
        loadDetail()
    }

    @Suppress("CyclomaticComplexMethod")
    fun onAction(action: AppDetailAction) {
        when (action) {
            is AppDetailAction.Retry -> loadDetail()

            is AppDetailAction.ViewManifest -> sendSectionEvent(AppDetailEvent.NavigateToManifest)

            is AppDetailAction.ExportApk -> requestDocument(AppDetailExport.Apk)

            is AppDetailAction.SaveIcon -> requestDocument(AppDetailExport.Icon)

            is AppDetailAction.ViewSummary -> viewSummary()

            is AppDetailAction.CopySummary -> copySummary()

            is AppDetailAction.ShareSummary -> shareSummary()

            is AppDetailAction.ExportInventory -> exportInventory()

            is AppDetailAction.ShareSummaryUnavailable -> sendEvent(AppDetailEvent.ShowFeedback(AppDetailFeedback.ShareUnavailable))

            is AppDetailAction.OpenPlayStore -> withLoadedState {
                trackAction(AppDetailAnalyticsEvent.Action.OpenPlayStore)
                sendEvent(AppDetailEvent.OpenPlayStore(it.packageName))
            }

            is AppDetailAction.OpenAppInfo -> withLoadedState {
                trackAction(AppDetailAnalyticsEvent.Action.OpenAppInfo)
                sendEvent(AppDetailEvent.OpenAppInfo(it.packageName))
            }

            is AppDetailAction.NavigateGeneralDetails -> sendSectionEvent(AppDetailEvent.NavigateToGeneralDetails)

            is AppDetailAction.NavigatePermissions -> sendSectionEvent(AppDetailEvent.NavigateToPermissions(action.permissionName))

            is AppDetailAction.NavigateComponents -> sendSectionEvent(AppDetailEvent.NavigateToComponents)

            is AppDetailAction.NavigateActivities -> sendSectionEvent(AppDetailEvent.NavigateToActivities)

            is AppDetailAction.NavigateServices -> sendSectionEvent(AppDetailEvent.NavigateToServices)

            is AppDetailAction.NavigateReceivers -> sendSectionEvent(AppDetailEvent.NavigateToReceivers)

            is AppDetailAction.NavigateProviders -> sendSectionEvent(AppDetailEvent.NavigateToProviders)

            is AppDetailAction.NavigateCertificates -> sendSectionEvent(AppDetailEvent.NavigateToCertificates)

            is AppDetailAction.NavigateFeatures -> sendSectionEvent(AppDetailEvent.NavigateToFeatures)

            is AppDetailAction.NavigateHistory -> withLoadedState {
                engaged = true
                trackAction(AppDetailAnalyticsEvent.Action.OpenHistory)
                sendEvent(AppDetailEvent.NavigateToHistory(it.packageName, it.appName))
            }

            is AppDetailAction.NavigateInsight -> navigateToInsight(action.insight)

            is AppDetailAction.ExportApkTo -> exportApk(action.destination)

            is AppDetailAction.SaveIconTo -> exportIcon(action.destination)

            is AppDetailAction.DocumentPickerUnavailable -> {
                exportInProgress.value = null
                sendEvent(AppDetailEvent.ShowFeedback(AppDetailFeedback.DocumentPickerUnavailable))
            }

            is AppDetailAction.DocumentPickerCancelled -> {
                if (exportInProgress.value == action.export) {
                    exportInProgress.value = null
                }
            }
        }
    }

    override fun onCleared() {
        val apkFile = appDetailInput as? AppDetailInput.ApkFile
        if (apkFile?.lifetime == ApkFileLifetime.Temporary) {
            temporaryApkManager.release(apkFile.apkFilePath).onFailure { error ->
                Logger.e(TAG, error, "Unable to release temporary APK")
            }
        }
        if (appDetailInput.isOnDevice) {
            reviewEligibilityTracker.recordAppDetailSessionCompleted(
                startTime = sessionStartedAt,
                endTime = Instant.now(),
                engaged = engaged,
            )
        }
    }

    private fun requestDocument(export: AppDetailExport) = withLoadedState { state ->
        if (exportInProgress.value != null || appDetailInput.isHistorical) return@withLoadedState
        if (export == AppDetailExport.Apk && appDetailInput !is AppDetailInput.InstalledPackage) return@withLoadedState
        val extension = if (export == AppDetailExport.Apk) "apk" else "png"
        exportInProgress.value = export
        trackAction(
            when (export) {
                AppDetailExport.Apk -> AppDetailAnalyticsEvent.Action.ExportApk
                AppDetailExport.Icon -> AppDetailAnalyticsEvent.Action.ExportIcon
            },
        )
        val fileNameBase = state.versionName?.let { "${state.packageName}_$it" } ?: state.packageName.toString()
        sendEvent(AppDetailEvent.CreateDocument(export, "$fileNameBase.$extension"))
    }

    private fun viewSummary() = withLoadedState { state ->
        trackAction(AppDetailAnalyticsEvent.Action.ViewSummary)
        sendEvent(AppDetailEvent.ShowSummaryPreview(summaryTextFormatter.summary(state)))
    }

    private fun copySummary() = withLoadedState { state ->
        engaged = true
        trackAction(AppDetailAnalyticsEvent.Action.CopySummary)
        val summary = summaryTextFormatter.summary(state)
        val label = summaryTextFormatter.clipLabel(state.appName)
        if (clipboardManager.copy(label, summary) == CopyResult.FeedbackNotShown) {
            sendEvent(AppDetailEvent.ShowFeedback(AppDetailFeedback.SummaryCopied))
        }
    }

    private fun shareSummary() = withLoadedState { state ->
        engaged = true
        trackAction(AppDetailAnalyticsEvent.Action.ShareSummary)
        sendEvent(AppDetailEvent.ShareSummary(summaryTextFormatter.summary(state)))
    }

    private fun exportInventory() = withLoadedState { state ->
        engaged = true
        trackAction(AppDetailAnalyticsEvent.Action.ExportInventory)
        sendEvent(AppDetailEvent.NavigateToInventoryExport(state.packageName))
    }

    private fun navigateToInsight(insight: AppDetailInsight) {
        when (insight) {
            AppDetailInsight.Debuggable,
            AppDetailInsight.Sideloaded,
            is AppDetailInsight.OutdatedTargetSdk,
            is AppDetailInsight.Unused,
            -> sendSectionEvent(AppDetailEvent.NavigateToGeneralDetails)

            AppDetailInsight.DebugCertificate,
            AppDetailInsight.CertificateNotYetValid,
            -> sendSectionEvent(AppDetailEvent.NavigateToCertificates)

            is AppDetailInsight.SensitivePermission ->
                sendSectionEvent(AppDetailEvent.NavigateToPermissions(insight.permissionName))
        }
    }

    private fun exportApk(destination: Uri) {
        if (appDetailInput !is AppDetailInput.InstalledPackage) return
        val reference = liveReference ?: return
        if (activeExport != null) return
        activeExport = AppDetailExport.Apk
        exportInProgress.value = AppDetailExport.Apk
        viewModelScope.launch {
            val feedback = appExportManager.exportApk(reference, destination).fold(
                onSuccess = {
                    engaged = true
                    AppDetailFeedback.ApkSaved(it.displayName, it.baseApkOnly)
                },
                onFailure = { AppDetailFeedback.ApkSaveFailed },
            )
            activeExport = null
            exportInProgress.value = null
            eventChannel.send(AppDetailEvent.ShowFeedback(feedback))
        }
    }

    private fun exportIcon(destination: Uri) {
        val reference = liveReference ?: return
        if (activeExport != null) return
        activeExport = AppDetailExport.Icon
        exportInProgress.value = AppDetailExport.Icon
        viewModelScope.launch {
            val feedback = appExportManager.exportIcon(reference, destination).fold(
                onSuccess = {
                    engaged = true
                    AppDetailFeedback.IconSaved(it.displayName)
                },
                onFailure = { AppDetailFeedback.IconSaveFailed },
            )
            activeExport = null
            exportInProgress.value = null
            eventChannel.send(AppDetailEvent.ShowFeedback(feedback))
        }
    }

    private fun sendEvent(event: AppDetailEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    private fun trackAction(action: AppDetailAnalyticsEvent.Action) {
        analytics.track(AppDetailAnalyticsEvent.ActionPerformed(action, appDetailInput))
    }

    private fun sendSectionEvent(event: AppDetailEvent) {
        engaged = true
        analytics.track(AppDetailAnalyticsEvent.SectionOpened(event.toSection(), appDetailInput))
        sendEvent(event)
    }

    private fun withLoadedState(block: (AppDetailState.Loaded) -> Unit) {
        (source.value as? AppDetailSource.Ready)?.state?.let(block)
    }

    private fun loadDetail() {
        source.value = AppDetailSource.Loading
        viewModelScope.launch(dispatcherProvider.default()) {
            val (deviceFeatures, detailResult) = coroutineScope {
                val featuresDeferred = async { if (appDetailInput.isOnDevice) deviceFeaturesRepository.deviceFeatures() else DeviceFeatures.Unknown }
                val detailDeferred = async { sectionRepository.detail(appDetailInput) }
                featuresDeferred.await() to detailDeferred.await()
            }
            val isNativeLibrariesRecorded = appDetailInput.isOnDevice ||
                sectionRepository.nativeLibrariesOrFallback(appDetailInput) is AppDetailSection.Available
            source.value = detailResult.fold(
                onSuccess = { detail ->
                    analytics.track(AppDetailAnalyticsEvent.Opened(appDetailInput))
                    AppDetailSource.Ready(detail.toLoadedState(deviceFeatures, isNativeLibrariesRecorded))
                },
                onFailure = { AppDetailSource.Error(it.toErrorReason()) },
            )
            val installedPackage = appDetailInput.installedPackage
            if (detailResult.isSuccess && installedPackage != null) {
                recentlyViewedAppsRepository.addRecent(installedPackage)
            }
        }
    }

    private fun AppDetail.toLoadedState(deviceFeatures: DeviceFeatures, isNativeLibrariesRecorded: Boolean): AppDetailState.Loaded {
        val dangerousPermissions = permissions.used.filter { it.isDangerous }
        val relevantDangerousPermissions = when (analysisMode) {
            AppDetail.AnalysisMode.InstalledPackage -> dangerousPermissions.filter { it.isGranted }
            AppDetail.AnalysisMode.ApkFile, AppDetail.AnalysisMode.Historical -> dangerousPermissions
        }
        val currentCertificate = signing.currentCertificates.firstOrNull()
        val insights = insightEvaluator.evaluate(appDetail = this, input = appDetailInput)
        val badges = badgeEvaluator.evaluate(info, input = appDetailInput)
        return AppDetailState.Loaded(
            analysisMode = analysisMode,
            appName = info.applicationName,
            packageName = info.packageName,
            processName = info.processName,
            versionName = info.versionName,
            versionCode = info.versionCode,
            uid = info.uid,
            description = info.description,
            isSystemApp = info.isSystemApp,
            source = info.source,
            apkDirectory = info.apkDirectory,
            dataDirectory = info.dataDirectory,
            apkSize = info.apkSize,
            targetSdkVersion = info.targetSdkVersion,
            targetSdkLabel = info.targetSdkLabel,
            minSdkVersion = info.minSdkVersion,
            minSdkLabel = info.minSdkLabel,
            installLocation = info.installLocation.name,
            appInstaller = info.installSourceChain.installingPackage,
            firstInstallTime = info.firstInstallTime,
            lastUpdateTime = info.lastUpdateTime,
            totalPermissionsCount = permissions.used.size,
            dangerousPermissionsCount = dangerousPermissions.size,
            grantedDangerousPermissionsCount = dangerousPermissions
                .takeIf { analysisMode == AppDetail.AnalysisMode.InstalledPackage }
                ?.count { it.isGranted },
            dangerousPermissionPreviews = relevantDangerousPermissions
                .map {
                    AppDetailState.Loaded.PermissionPreview(
                        name = it.permissionData.name,
                        groupName = it.permissionData.details?.groupName,
                        label = permissionLabelProvider.getLabel(it.permissionData.name),
                    )
                },
            definedPermissionsCount = permissions.defined.size,
            activitiesCount = activities.size,
            servicesCount = services.size,
            contentProvidersCount = contentProviders.size,
            broadcastReceiversCount = receivers.size,
            certificatesCount = signing.currentCertificates.size,
            requirementsCount = features.size,
            requiredFeaturesCount = features.count { it.isRequired },
            optionalFeaturesCount = features.count { !it.isRequired },
            unmetRequirementsCount = features.count { it.isRequired && deviceFeatures.availabilityOf(it) == FeatureAvailability.Missing },
            requirementPreviews = features
                .sortedWith(compareBy({ deviceFeatures.availabilityOf(it) != FeatureAvailability.Missing }, { !it.isRequired }))
                .take(MAX_REQUIREMENT_PREVIEWS)
                .map {
                    AppDetailState.Loaded.RequirementPreview(
                        name = (it as? Feature.Hardware)?.name,
                        isUnmetRequirement = it.isRequired && deviceFeatures.availabilityOf(it) == FeatureAvailability.Missing,
                    )
                },
            certificate = currentCertificate?.let { cert ->
                AppDetailState.Loaded.CertificateState(
                    signAlgorithm = cert.signAlgorithm,
                    sha256Fingerprint = cert.certificateHashSha256,
                    issuer = cert.issuer,
                    trustLevel = cert.trustLevel,
                )
            },
            totalSize = info.totalSize,
            lastUsedTime = info.lastUsedTime,
            installedSplitsCount = info.installedSplits.size,
            usesCleartextTraffic = info.usesCleartextTraffic,
            badges = badges.take(MAX_BADGES),
            insights = insights,
            isNativeLibrariesRecorded = isNativeLibrariesRecorded,
        )
    }
}

private sealed interface AppDetailSource {
    data object Loading : AppDetailSource
    data class Error(val reason: AppDetailErrorReason) : AppDetailSource
    data class Ready(val state: AppDetailState.Loaded) : AppDetailSource
}

private fun AppHistoryTimeline.toHistoryStatus(): AppDetailHistoryStatus = when (this) {
    is AppHistoryTimeline.Available -> AppDetailHistoryStatus.Available(entries.overview)
    AppHistoryTimeline.Unavailable -> AppDetailHistoryStatus.Unavailable
}

private fun Throwable.toErrorReason(): AppDetailErrorReason = when (this) {
    is UnparsableApkFileException -> AppDetailErrorReason.UnsupportedApk
    else -> AppDetailErrorReason.Failed(toSectionErrorReason())
}

private const val MAX_BADGES = 3
private const val MAX_REQUIREMENT_PREVIEWS = 6

private fun AppDetailEvent.toSection(): AppDetailAnalyticsEvent.Section = when (this) {
    AppDetailEvent.NavigateToManifest -> AppDetailAnalyticsEvent.Section.Manifest

    AppDetailEvent.NavigateToGeneralDetails -> AppDetailAnalyticsEvent.Section.General

    is AppDetailEvent.NavigateToPermissions -> AppDetailAnalyticsEvent.Section.Permissions

    AppDetailEvent.NavigateToComponents -> AppDetailAnalyticsEvent.Section.Components

    AppDetailEvent.NavigateToActivities -> AppDetailAnalyticsEvent.Section.Activities

    AppDetailEvent.NavigateToServices -> AppDetailAnalyticsEvent.Section.Services

    AppDetailEvent.NavigateToReceivers -> AppDetailAnalyticsEvent.Section.Receivers

    AppDetailEvent.NavigateToProviders -> AppDetailAnalyticsEvent.Section.Providers

    AppDetailEvent.NavigateToCertificates -> AppDetailAnalyticsEvent.Section.Certificates

    AppDetailEvent.NavigateToFeatures -> AppDetailAnalyticsEvent.Section.Features

    is AppDetailEvent.OpenPlayStore,
    is AppDetailEvent.OpenAppInfo,
    is AppDetailEvent.CreateDocument,
    is AppDetailEvent.ShowSummaryPreview,
    is AppDetailEvent.ShareSummary,
    is AppDetailEvent.NavigateToInventoryExport,
    is AppDetailEvent.ShowFeedback,
    is AppDetailEvent.NavigateToHistory,
    -> error("Unexpected event type: $this")
}
