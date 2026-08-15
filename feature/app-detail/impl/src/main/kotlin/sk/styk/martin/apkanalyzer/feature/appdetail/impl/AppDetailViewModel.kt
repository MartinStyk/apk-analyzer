package sk.styk.martin.apkanalyzer.feature.appdetail.impl

import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.apkfiles.TemporaryApkManager
import sk.styk.martin.apkanalyzer.core.apppermissions.PermissionLabelProvider
import sk.styk.martin.apkanalyzer.core.apps.AppClassificationThresholds
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepository
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.DeviceFeatures
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.DeviceFeaturesRepository
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.Feature
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.FeatureAvailability
import sk.styk.martin.apkanalyzer.core.apps.export.AppExportManager
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.common.analytics.AnalyticsEvent
import sk.styk.martin.apkanalyzer.core.common.analytics.AnalyticsTracker
import sk.styk.martin.apkanalyzer.core.common.clipboard.ClipboardManager
import sk.styk.martin.apkanalyzer.core.common.clipboard.CopyResult
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.isSideloaded
import sk.styk.martin.apkanalyzer.core.common.review.ReviewEligibilityTracker
import sk.styk.martin.apkanalyzer.core.userpreferences.recentlyviewed.RecentlyViewedAppsRepository
import sk.styk.martin.apkanalyzer.feature.appdetail.api.ApkFileLifetime
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.AppDetailBadge
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.insight.AppDetailInsight
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.insight.AppDetailInsightEvaluator
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

private const val TAG = "AppDetailViewModel"
private val MIN_QUALIFYING_DWELL: Duration = Duration.ofSeconds(8)

@Suppress("TooManyFunctions")
@HiltViewModel(assistedFactory = AppDetailViewModel.Factory::class)
internal class AppDetailViewModel @AssistedInject constructor(
    @Assisted private val appDetailInput: AppDetailInput,
    private val appDetailRepository: AppDetailRepository,
    private val appExportManager: AppExportManager,
    private val deviceFeaturesRepository: DeviceFeaturesRepository,
    private val permissionLabelProvider: PermissionLabelProvider,
    private val dispatcherProvider: DispatcherProvider,
    private val temporaryApkManager: TemporaryApkManager,
    private val recentlyViewedAppsRepository: RecentlyViewedAppsRepository,
    private val clipboardManager: ClipboardManager,
    private val summaryTextFormatter: AppSummaryTextFormatter,
    private val reviewEligibilityTracker: ReviewEligibilityTracker,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(target: AppDetailInput): AppDetailViewModel
    }

    private val source = MutableStateFlow<AppDetailSource>(AppDetailSource.Loading)
    private val exportInProgress = MutableStateFlow<AppDetailExport?>(null)

    val state: StateFlow<AppDetailState> = combine(source, exportInProgress) { source, exportInProgress ->
        when (source) {
            AppDetailSource.Loading -> AppDetailState.Loading
            AppDetailSource.Error -> AppDetailState.Error
            is AppDetailSource.Ready -> source.state.copy(exportInProgress = exportInProgress)
        }
    }
        .flowOn(dispatcherProvider.default())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppDetailState.Loading)

    private val eventChannel = Channel<AppDetailEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var activeExport: AppDetailExport? = null
    private val appReference = appDetailInput.toAppReference()

    private val sessionStartedAt = Instant.now()
    private var engaged = false

    init {
        loadDetail()
    }

    @Suppress("CyclomaticComplexMethod")
    fun onAction(action: AppDetailAction) {
        when (action) {
            is AppDetailAction.Retry -> loadDetail()

            is AppDetailAction.ViewManifest -> {
                trackAction(ACTION_VIEW_MANIFEST)
                sendSectionEvent(AppDetailEvent.NavigateToManifest, SECTION_MANIFEST)
            }

            is AppDetailAction.ExportApk -> requestDocument(AppDetailExport.Apk)

            is AppDetailAction.SaveIcon -> requestDocument(AppDetailExport.Icon)

            is AppDetailAction.ViewSummary -> viewSummary()

            is AppDetailAction.CopySummary -> copySummary()

            is AppDetailAction.ShareSummary -> shareSummary()

            is AppDetailAction.ShareSummaryUnavailable -> sendEvent(AppDetailEvent.ShowFeedback(AppDetailFeedback.ShareUnavailable))

            is AppDetailAction.OpenPlayStore -> withLoadedState { sendEvent(AppDetailEvent.OpenPlayStore(it.packageName)) }

            is AppDetailAction.OpenAppInfo -> withLoadedState { sendEvent(AppDetailEvent.OpenAppInfo(it.packageName)) }

            is AppDetailAction.NavigateGeneralDetails -> sendSectionEvent(AppDetailEvent.NavigateToGeneralDetails, SECTION_GENERAL)

            is AppDetailAction.NavigatePermissions -> sendSectionEvent(AppDetailEvent.NavigateToPermissions(action.permissionName), SECTION_PERMISSIONS)

            is AppDetailAction.NavigateComponents -> sendSectionEvent(AppDetailEvent.NavigateToComponents, SECTION_COMPONENTS)

            is AppDetailAction.NavigateActivities -> sendSectionEvent(AppDetailEvent.NavigateToActivities, SECTION_ACTIVITIES)

            is AppDetailAction.NavigateServices -> sendSectionEvent(AppDetailEvent.NavigateToServices, SECTION_SERVICES)

            is AppDetailAction.NavigateReceivers -> sendSectionEvent(AppDetailEvent.NavigateToReceivers, SECTION_RECEIVERS)

            is AppDetailAction.NavigateProviders -> sendSectionEvent(AppDetailEvent.NavigateToProviders, SECTION_PROVIDERS)

            is AppDetailAction.NavigateCertificates -> sendSectionEvent(AppDetailEvent.NavigateToCertificates, SECTION_CERTIFICATES)

            is AppDetailAction.NavigateFeatures -> sendSectionEvent(AppDetailEvent.NavigateToFeatures, SECTION_FEATURES)

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
        val dwelled = Duration.between(sessionStartedAt, Instant.now()) >= MIN_QUALIFYING_DWELL
        reviewEligibilityTracker.recordAppDetailSessionCompleted(qualified = engaged || dwelled)
    }

    private fun requestDocument(export: AppDetailExport) {
        withLoadedState { state ->
            if (exportInProgress.value != null) return@withLoadedState
            if (export == AppDetailExport.Apk && appDetailInput !is AppDetailInput.InstalledPackage) return@withLoadedState
            val extension = if (export == AppDetailExport.Apk) "apk" else "png"
            exportInProgress.value = export
            trackAction(export.analyticsAction)
            sendEvent(AppDetailEvent.CreateDocument(export, "${state.packageName}.$extension"))
        }
    }

    private fun viewSummary() {
        withLoadedState { state ->
            sendEvent(AppDetailEvent.ShowSummaryPreview(summaryTextFormatter.summary(state)))
        }
    }

    private fun copySummary() {
        withLoadedState { state ->
            engaged = true
            val summary = summaryTextFormatter.summary(state)
            val label = summaryTextFormatter.clipLabel(state.appName)
            if (clipboardManager.copy(label, summary) == CopyResult.FeedbackNotShown) {
                sendEvent(AppDetailEvent.ShowFeedback(AppDetailFeedback.SummaryCopied))
            }
        }
    }

    private fun shareSummary() {
        withLoadedState { state ->
            engaged = true
            sendEvent(AppDetailEvent.ShareSummary(summaryTextFormatter.summary(state)))
        }
    }

    private fun navigateToInsight(insight: AppDetailInsight) {
        when (insight) {
            AppDetailInsight.Debuggable,
            is AppDetailInsight.OutdatedTargetSdk,
            -> sendSectionEvent(AppDetailEvent.NavigateToGeneralDetails, SECTION_GENERAL)

            AppDetailInsight.DebugCertificate,
            AppDetailInsight.CertificateNotYetValid,
            -> sendSectionEvent(AppDetailEvent.NavigateToCertificates, SECTION_CERTIFICATES)

            is AppDetailInsight.SensitivePermission -> sendSectionEvent(
                AppDetailEvent.NavigateToPermissions(insight.permissionName),
                SECTION_PERMISSIONS,
            )
        }
    }

    private fun exportApk(destination: Uri) {
        if (appDetailInput !is AppDetailInput.InstalledPackage) return
        if (activeExport != null) return
        activeExport = AppDetailExport.Apk
        exportInProgress.value = AppDetailExport.Apk
        viewModelScope.launch {
            val feedback = appExportManager.exportApk(appReference, destination).fold(
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
        if (activeExport != null) return
        activeExport = AppDetailExport.Icon
        exportInProgress.value = AppDetailExport.Icon
        viewModelScope.launch {
            val result = appExportManager.exportIcon(appReference, destination)
            val feedback = result.fold(
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

    private fun sendSectionEvent(event: AppDetailEvent, section: String) {
        engaged = true
        analyticsTracker.track(AnalyticsEvent(EVENT_SECTION_OPENED, mapOf(PARAMETER_SECTION to section)))
        sendEvent(event)
    }

    private fun trackAction(action: String) {
        analyticsTracker.track(AnalyticsEvent(EVENT_ACTION_PERFORMED, mapOf(PARAMETER_ACTION to action)))
    }

    private fun withLoadedState(block: (AppDetailState.Loaded) -> Unit) {
        (source.value as? AppDetailSource.Ready)?.state?.let(block)
    }

    private fun loadDetail() {
        source.value = AppDetailSource.Loading
        viewModelScope.launch {
            val deviceFeatures = deviceFeaturesRepository.deviceFeatures()
            val detailResult = withContext(dispatcherProvider.default()) {
                appDetailRepository.details(appReference)
            }
            source.value = detailResult.fold(
                onSuccess = { detail ->
                    analyticsTracker.track(
                        AnalyticsEvent(
                            EVENT_APP_DETAIL_OPENED,
                            mapOf(PARAMETER_ANALYSIS_MODE to appReference.analyticsAnalysisMode),
                        ),
                    )
                    AppDetailSource.Ready(
                        detail.toLoadedState(permissionLabelProvider, deviceFeatures)
                            .withComputedBadges(Instant.now()),
                    )
                },
                onFailure = { AppDetailSource.Error },
            )
            if (detailResult.isSuccess && appReference is AppReference.InstalledPackage) {
                recentlyViewedAppsRepository.addRecent(appReference.packageName)
            }
        }
    }
}

private sealed interface AppDetailSource {
    data object Loading : AppDetailSource
    data object Error : AppDetailSource
    data class Ready(val state: AppDetailState.Loaded) : AppDetailSource
}

private const val EVENT_APP_DETAIL_OPENED = "app_detail_opened"
private const val EVENT_SECTION_OPENED = "app_detail_section_opened"
private const val EVENT_ACTION_PERFORMED = "app_detail_action_performed"
private const val PARAMETER_ANALYSIS_MODE = "analysis_mode"
private const val PARAMETER_SECTION = "section"
private const val PARAMETER_ACTION = "action"
private const val ACTION_VIEW_MANIFEST = "view_manifest"
private const val SECTION_MANIFEST = "manifest"
private const val SECTION_GENERAL = "general"
private const val SECTION_PERMISSIONS = "permissions"
private const val SECTION_COMPONENTS = "components"
private const val SECTION_ACTIVITIES = "activities"
private const val SECTION_SERVICES = "services"
private const val SECTION_RECEIVERS = "receivers"
private const val SECTION_PROVIDERS = "providers"
private const val SECTION_CERTIFICATES = "certificates"
private const val SECTION_FEATURES = "features"

private val AppReference.analyticsAnalysisMode: String
    get() = when (this) {
        is AppReference.ApkFile -> "apk_file"
        is AppReference.InstalledPackage -> "installed_package"
    }

private val AppDetailExport.analyticsAction: String
    get() = when (this) {
        AppDetailExport.Apk -> "export_apk"
        AppDetailExport.Icon -> "export_icon"
    }

private const val MAX_BADGES = 3
private const val MAX_REQUIREMENT_PREVIEWS = 6

private fun AppDetailState.Loaded.withComputedBadges(now: Instant): AppDetailState.Loaded = copy(
    badges = buildList {
        if (source.isSideloaded) add(AppDetailBadge.Sideloaded)
        if (insights.any { it is AppDetailInsight.SensitivePermission }) add(AppDetailBadge.DangerousPermissions)
        lastUsedTime?.let { lastUsed ->
            if (lastUsed.isBefore(now.minus(AppClassificationThresholds.UNUSED_PERIOD))) add(AppDetailBadge.Unused)
        }
        val effectiveSize = totalSize ?: apkSize
        if (effectiveSize >= AppClassificationThresholds.LARGE_SIZE) add(AppDetailBadge.Large)
        if (isSystemApp) add(AppDetailBadge.System)
        firstInstallTime?.let { installTime ->
            if (installTime.isAfter(now.minus(AppClassificationThresholds.RECENT_PERIOD))) add(AppDetailBadge.RecentlyInstalled)
        }
        lastUpdateTime?.let { updateTime ->
            if (updateTime.isAfter(now.minus(AppClassificationThresholds.RECENT_PERIOD))) add(AppDetailBadge.RecentlyUpdated)
        }
        lastUsedTime?.let { lastUsed ->
            if (lastUsed.isAfter(now.minus(AppClassificationThresholds.RECENTLY_USED_DAYS.days.toJavaDuration()))) add(AppDetailBadge.RecentlyUsed)
        }
        if (source == AppSource.GooglePlay) add(AppDetailBadge.GooglePlay)
    }.take(MAX_BADGES).toImmutableList(),
)

private fun AppDetail.toLoadedState(permissionLabelProvider: PermissionLabelProvider, deviceFeatures: DeviceFeatures): AppDetailState.Loaded {
    val dangerousPermissions = permissions.used.filter {
        it.permissionData.details?.protectionLevel == ProtectionLevel.Dangerous
    }
    val relevantDangerousPermissions = when (analysisMode) {
        AppDetail.AnalysisMode.InstalledPackage -> dangerousPermissions.filter { it.isGranted }
        AppDetail.AnalysisMode.ApkFile -> dangerousPermissions
    }
    val currentCertificate = signing.currentCertificates.firstOrNull()
    val insights = AppDetailInsightEvaluator.evaluate(
        appDetail = this,
        now = Instant.now(),
        deviceSdk = Build.VERSION.SDK_INT,
    )
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
            }
            .toImmutableList(),
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
            }
            .toImmutableList(),
        certificate = currentCertificate?.let { cert ->
            AppDetailState.Loaded.CertificateState(
                signAlgorithm = cert.signAlgorithm,
                sha256Fingerprint = cert.formattedSha256Fingerprint,
                issuer = cert.issuer,
                trustLevel = cert.trustLevel,
            )
        },
        totalSize = info.totalSize,
        lastUsedTime = info.lastUsedTime,
        installedSplitsCount = info.installedSplits.size,
        hasNativeLibraries = nativeLibraries.hasNativeCode,
        usesCleartextTraffic = info.usesCleartextTraffic,
        insights = insights,
    )
}
