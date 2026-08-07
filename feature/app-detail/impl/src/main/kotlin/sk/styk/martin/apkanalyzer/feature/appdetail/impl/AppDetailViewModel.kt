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
import sk.styk.martin.apkanalyzer.core.apppermissions.PermissionLabelProvider
import sk.styk.martin.apkanalyzer.core.apps.AppClassificationThresholds
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepository
import sk.styk.martin.apkanalyzer.core.apps.AppExportManager
import sk.styk.martin.apkanalyzer.core.apps.DeviceFeaturesRepository
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.model.DeviceFeatures
import sk.styk.martin.apkanalyzer.core.apps.model.Feature
import sk.styk.martin.apkanalyzer.core.apps.model.FeatureAvailability
import sk.styk.martin.apkanalyzer.core.apps.model.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.AppDetailBadge
import java.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

private const val TAG = "AppDetailViewModel"

@HiltViewModel(assistedFactory = AppDetailViewModel.Factory::class)
internal class AppDetailViewModel @AssistedInject constructor(
    @Assisted private val appDetailInput: AppDetailInput,
    private val appDetailRepository: AppDetailRepository,
    private val appExportManager: AppExportManager,
    private val deviceFeaturesRepository: DeviceFeaturesRepository,
    private val permissionLabelProvider: PermissionLabelProvider,
    private val dispatcherProvider: DispatcherProvider,
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

    init {
        loadDetail()
    }

    fun onAction(action: AppDetailAction) {
        when (action) {
            is AppDetailAction.Retry -> loadDetail()

            is AppDetailAction.ViewManifest -> sendEvent(AppDetailEvent.NavigateToManifest)

            is AppDetailAction.ExportApk -> requestDocument(AppDetailExport.Apk)

            is AppDetailAction.SaveIcon -> requestDocument(AppDetailExport.Icon)

            is AppDetailAction.OpenPlayStore -> withLoadedState { sendEvent(AppDetailEvent.OpenPlayStore(it.packageName)) }

            is AppDetailAction.OpenAppInfo -> withLoadedState { sendEvent(AppDetailEvent.OpenAppInfo(it.packageName)) }

            is AppDetailAction.NavigateGeneralDetails -> sendEvent(AppDetailEvent.NavigateToGeneralDetails)

            is AppDetailAction.NavigatePermissions -> sendEvent(AppDetailEvent.NavigateToPermissions(action.permissionName))

            is AppDetailAction.NavigateComponents -> sendEvent(AppDetailEvent.NavigateToComponents)

            is AppDetailAction.NavigateActivities -> sendEvent(AppDetailEvent.NavigateToActivities)

            is AppDetailAction.NavigateServices -> sendEvent(AppDetailEvent.NavigateToServices)

            is AppDetailAction.NavigateReceivers -> sendEvent(AppDetailEvent.NavigateToReceivers)

            is AppDetailAction.NavigateProviders -> sendEvent(AppDetailEvent.NavigateToProviders)

            is AppDetailAction.NavigateCertificates -> sendEvent(AppDetailEvent.NavigateToCertificates)

            is AppDetailAction.NavigateFeatures -> sendEvent(AppDetailEvent.NavigateToFeatures)

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

    private fun requestDocument(export: AppDetailExport) {
        withLoadedState { state ->
            if (exportInProgress.value != null) return@withLoadedState
            if (export == AppDetailExport.Apk && appDetailInput !is AppDetailInput.InstalledPackage) return@withLoadedState
            val extension = if (export == AppDetailExport.Apk) "apk" else "png"
            exportInProgress.value = export
            sendEvent(AppDetailEvent.CreateDocument(export, "${state.packageName}.$extension"))
        }
    }

    private fun navigateToInsight(insight: AppDetailInsight) {
        when (insight) {
            AppDetailInsight.DebugCertificate,
            AppDetailInsight.CertificateNotYetValid,
            -> sendEvent(AppDetailEvent.NavigateToCertificates)

            is AppDetailInsight.OutdatedTargetSdk -> sendEvent(AppDetailEvent.NavigateToGeneralDetails)

            is AppDetailInsight.SensitivePermission -> sendEvent(AppDetailEvent.NavigateToPermissions(insight.permissionName))
        }
    }

    private fun exportApk(destination: Uri) {
        if (appDetailInput !is AppDetailInput.InstalledPackage) return
        if (activeExport != null) return
        activeExport = AppDetailExport.Apk
        exportInProgress.value = AppDetailExport.Apk
        viewModelScope.launch {
            val feedback = appExportManager.exportApk(appReference, destination).fold(
                onSuccess = { AppDetailFeedback.ApkSaved(it.displayName, it.baseApkOnly) },
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
                onSuccess = { AppDetailFeedback.IconSaved(it.displayName) },
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

    private fun withLoadedState(block: (AppDetailState.Loaded) -> Unit) {
        (source.value as? AppDetailSource.Ready)?.state?.let(block)
    }

    private fun loadDetail() {
        source.value = AppDetailSource.Loading
        viewModelScope.launch {
            val deviceFeatures = deviceFeaturesRepository.deviceFeatures()
            source.value = withContext(dispatcherProvider.default()) {
                appDetailRepository.details(appReference)
            }.onFailure {
                Logger.e(TAG, it, "Can not load app detail for $appDetailInput")
            }.fold(
                onSuccess = { detail ->
                    AppDetailSource.Ready(
                        detail.toLoadedState(permissionLabelProvider, deviceFeatures)
                            .withComputedBadges(Instant.now()),
                    )
                },
                onFailure = { AppDetailSource.Error },
            )
        }
    }
}

private sealed interface AppDetailSource {
    data object Loading : AppDetailSource
    data object Error : AppDetailSource
    data class Ready(val state: AppDetailState.Loaded) : AppDetailSource
}

private const val MAX_BADGES = 3
private const val MAX_REQUIREMENT_PREVIEWS = 6

private fun AppDetailState.Loaded.withComputedBadges(now: Instant): AppDetailState.Loaded = copy(
    badges = buildList {
        if (source == AppSource.Unknown.name) add(AppDetailBadge.Sideloaded)
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
        if (source == AppSource.GooglePlay.name) add(AppDetailBadge.GooglePlay)
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
        source = info.source.name,
        apkDirectory = info.apkDirectory,
        dataDirectory = info.dataDirectory,
        apkSize = info.apkSize,
        targetSdkVersion = info.targetSdkVersion,
        targetSdkLabel = info.targetSdkLabel,
        minSdkVersion = info.minSdkVersion,
        minSdkLabel = info.minSdkLabel,
        installLocation = info.installLocation.name,
        appInstaller = info.appInstaller,
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
        insights = insights,
    )
}
