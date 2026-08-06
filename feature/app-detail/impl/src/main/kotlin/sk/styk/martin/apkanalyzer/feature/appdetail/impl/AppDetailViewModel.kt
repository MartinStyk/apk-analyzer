package sk.styk.martin.apkanalyzer.feature.appdetail.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.apppermissions.PermissionLabelProvider
import sk.styk.martin.apkanalyzer.core.apps.AppClassificationThresholds
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepository
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.model.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.AppDetailBadge
import java.io.File
import java.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

private const val TAG = "AppDetailViewModel"

@HiltViewModel(assistedFactory = AppDetailViewModel.Factory::class)
internal class AppDetailViewModel @AssistedInject constructor(
    @Assisted private val appDetailInput: AppDetailInput,
    private val appDetailRepository: AppDetailRepository,
    private val permissionLabelProvider: PermissionLabelProvider,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(target: AppDetailInput): AppDetailViewModel
    }

    private val _state = MutableStateFlow<AppDetailState>(AppDetailState.Loading)
    val state: StateFlow<AppDetailState> = _state

    private val eventChannel = Channel<AppDetailEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        loadDetail()
    }

    fun onAction(action: AppDetailAction) {
        when (action) {
            is AppDetailAction.Retry -> loadDetail()
            is AppDetailAction.ViewManifest -> sendEvent(AppDetailEvent.NavigateToManifest)
            is AppDetailAction.ExportApk -> withLoadedState { sendEvent(AppDetailEvent.ExportApk(it.packageName)) }
            is AppDetailAction.SaveIcon -> withLoadedState { sendEvent(AppDetailEvent.SaveIcon(it.packageName)) }
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
        }
    }

    private fun sendEvent(event: AppDetailEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    private fun withLoadedState(block: (AppDetailState.Loaded) -> Unit) {
        (_state.value as? AppDetailState.Loaded)?.let(block)
    }

    private fun loadDetail() {
        _state.value = AppDetailState.Loading
        viewModelScope.launch {
            _state.value = withContext(dispatcherProvider.default()) {
                when (appDetailInput) {
                    is AppDetailInput.InstalledPackage -> appDetailRepository.installedPackageDetails(appDetailInput.packageName)
                    is AppDetailInput.ApkFile -> appDetailRepository.apkFilePackageDetails(File(appDetailInput.apkFilePath))
                }
            }.onFailure {
                Logger.e(TAG, it, "Can not load app detail for $appDetailInput")
            }.fold(
                onSuccess = { detail ->
                    detail.toLoadedState(permissionLabelProvider).let { state ->
                        state.copy(badges = computeBadges(state))
                    }
                },
                onFailure = { AppDetailState.Error },
            )
        }
    }

    private fun computeBadges(state: AppDetailState.Loaded): ImmutableList<AppDetailBadge> {
        val now = Instant.now()
        return buildList {
            if (state.source == AppSource.Unknown.name) add(AppDetailBadge.Sideloaded)
            if (state.dangerousPermissionsCount > 0) add(AppDetailBadge.DangerousPermissions)
            state.lastUsedTime?.let { lastUsed ->
                if (lastUsed.isBefore(now.minus(AppClassificationThresholds.UNUSED_PERIOD))) add(AppDetailBadge.Unused)
            }
            val effectiveSize = state.totalSize ?: state.apkSize
            if (effectiveSize >= AppClassificationThresholds.LARGE_SIZE) add(AppDetailBadge.Large)
            if (state.isSystemApp) add(AppDetailBadge.System)
            state.firstInstallTime?.let { installTime ->
                if (installTime.isAfter(now.minus(AppClassificationThresholds.RECENT_PERIOD))) add(AppDetailBadge.RecentlyInstalled)
            }
            state.lastUpdateTime?.let { updateTime ->
                if (updateTime.isAfter(now.minus(AppClassificationThresholds.RECENT_PERIOD))) add(AppDetailBadge.RecentlyUpdated)
            }
            state.lastUsedTime?.let { lastUsed ->
                if (lastUsed.isAfter(now.minus(AppClassificationThresholds.RECENTLY_USED_DAYS.days.toJavaDuration()))) add(AppDetailBadge.RecentlyUsed)
            }
            if (state.source == AppSource.GooglePlay.name) add(AppDetailBadge.GooglePlay)
        }.take(MAX_BADGES).toImmutableList()
    }

    private companion object {
        const val MAX_BADGES = 3
    }
}

private fun AppDetail.toLoadedState(permissionLabelProvider: PermissionLabelProvider): AppDetailState.Loaded {
    val dangerousPermissions = permissions.used.filter {
        it.permissionData.details?.protectionLevel == ProtectionLevel.Dangerous
    }
    val relevantDangerousPermissions = when (analysisMode) {
        AppDetail.AnalysisMode.InstalledPackage -> dangerousPermissions.filter { it.isGranted }
        AppDetail.AnalysisMode.ApkFile -> dangerousPermissions
    }
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
        featuresCount = features.size,
        certificate = signing.currentCertificates.firstOrNull()?.let { cert ->
            AppDetailState.Loaded.CertificateState(
                signAlgorithm = cert.signAlgorithm,
                sha256Fingerprint = cert.formattedSha256Fingerprint,
                issuer = cert.issuer,
                trustLevel = cert.trustLevel,
            )
        },
        totalSize = info.totalSize,
        lastUsedTime = info.lastUsedTime,
    )
}
