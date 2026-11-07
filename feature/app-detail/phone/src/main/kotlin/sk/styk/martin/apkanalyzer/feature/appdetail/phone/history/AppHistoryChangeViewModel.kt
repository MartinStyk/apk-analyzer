package sk.styk.martin.apkanalyzer.feature.appdetail.phone.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.AppHistoryRepository
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryDiff
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryEntry
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryFieldChange
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryTimeline
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.SetChange
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.ValueChange
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.fieldChangesByLibraryName
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.fieldChangesFrom
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.hasCertificateChange
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.isOlderVersion
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.isSameVersionBuild
import sk.styk.martin.apkanalyzer.core.apppermissions.detail.PermissionItem
import sk.styk.martin.apkanalyzer.core.apppermissions.detail.PermissionItemMapper
import sk.styk.martin.apkanalyzer.core.apppermissions.detail.PermissionScope
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraryFile
import sk.styk.martin.apkanalyzer.core.apps.packaging.groupedByLibrary
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.apps.sdkversion.SdkVersionResolver
import sk.styk.martin.apkanalyzer.core.common.clipboard.ClipboardManager
import sk.styk.martin.apkanalyzer.core.common.clipboard.CopyResult
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.AppDetailAnalytics
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.AppDetailAnalyticsEvent
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.appcomponents.ComponentItem
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.appcomponents.mergeDuplicatesByStableKey
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.appcomponents.sortedForDisplay
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.appcomponents.toItem

@HiltViewModel(assistedFactory = AppHistoryChangeViewModel.Factory::class)
internal class AppHistoryChangeViewModel @AssistedInject constructor(
    @Assisted private val packageName: String,
    @Assisted private val snapshotId: Long,
    appHistoryRepository: AppHistoryRepository,
    private val permissionItemMapper: PermissionItemMapper,
    private val sdkVersionResolver: SdkVersionResolver,
    private val clipboardManager: ClipboardManager,
    private val analytics: AppDetailAnalytics,
    dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(packageName: String, snapshotId: Long): AppHistoryChangeViewModel
    }

    private var isOpenTracked = false

    private val change = RetryableFlow<AppHistoryChangeState>(loading = AppHistoryChangeState.Loading) {
        appHistoryRepository.timeline(PackageName(packageName))
            .map { it.toState() }
            .onEach(::trackOpened)
    }

    val state: StateFlow<AppHistoryChangeState> = change.flow
        .flowOn(dispatcherProvider.default())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppHistoryChangeState.Loading)

    private val eventChannel = Channel<AppHistoryChangeEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    fun onAction(action: AppHistoryChangeAction) {
        when (action) {
            AppHistoryChangeAction.Back -> sendEvent(AppHistoryChangeEvent.NavigateBack)

            AppHistoryChangeAction.Retry -> change.retry()

            AppHistoryChangeAction.OpenFullDetail -> openFullDetail()

            is AppHistoryChangeAction.CopyValue -> {
                if (clipboardManager.copy(action.label, action.value) == CopyResult.FeedbackNotShown) {
                    sendEvent(AppHistoryChangeEvent.ShowCopiedFeedback)
                }
            }
        }
    }

    private fun openFullDetail() {
        val loaded = state.value as? AppHistoryChangeState.Loaded ?: return
        analytics.track(AppDetailAnalyticsEvent.HistoryFullDetailOpened)
        sendEvent(
            AppHistoryChangeEvent.NavigateToFullDetail(
                AppDetailInput.Historical(
                    packageName = packageName,
                    snapshotId = snapshotId,
                    capturedAtEpochMillis = loaded.capturedAt.toEpochMilli(),
                ),
            ),
        )
    }

    private fun sendEvent(event: AppHistoryChangeEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    private fun trackOpened(state: AppHistoryChangeState) {
        if (isOpenTracked || state !is AppHistoryChangeState.Loaded) return
        isOpenTracked = true
        analytics.track(AppDetailAnalyticsEvent.HistoryChangeOpened(state.kind))
    }

    private fun AppHistoryTimeline.toState(): AppHistoryChangeState = when (this) {
        AppHistoryTimeline.Unavailable -> AppHistoryChangeState.Error

        is AppHistoryTimeline.Available ->
            entries
                .filterIsInstance<AppHistoryEntry.Change>()
                .firstOrNull { it.snapshot.id.value == snapshotId }
                ?.toLoadedState()
                ?: AppHistoryChangeState.NotFound
    }

    private fun AppHistoryEntry.Change.toLoadedState() = AppHistoryChangeState.Loaded(
        appName = snapshot.applicationName,
        kind = kind,
        timestamp = timestamp,
        capturedAt = snapshot.capturedAt,
        version = snapshot.version,
        versionChange = diff.version,
        isOlderVersion = isOlderVersion,
        isSameVersionBuild = isSameVersionBuild,
        sections = toSections(),
        isSigningMethodUncomparable = diff.signingSchemes is SetChange.Uncomparable,
        isNativeLibrariesUncomparable = diff.nativeLibraries is SetChange.Uncomparable,
    )

    private fun AppHistoryEntry.Change.toSections(): List<HistoryChangeSection> = listOfNotNull(
        diff.signingSection(),
        permissionsSection(),
        diff.installSource?.let { HistoryChangeSection.InstallSource(it) },
        diff.appSettingsSection(),
        diff.androidVersionsSection(),
        diff.componentsSection(),
        diff.nativeLibrariesSection(),
        diff.apkSize?.let { HistoryChangeSection.Size(it) },
    )

    private fun AppHistoryEntry.Change.permissionsSection(): HistoryChangeSection? {
        val analysedPackage = PackageName(packageName)
        val requested = diff.requestedPermissions as? SetChange.Changed
        val defined = diff.definedPermissions as? SetChange.Changed
        val modifiedDefined = defined?.modified.orEmpty()
        val fieldChangesByName = modifiedDefined.associate { it.after.name to it.after.fieldChangesFrom(it.before) }
        val groups = listOfNotNull(
            permissionGroup(
                id = "requested-added",
                kind = HistoryChangeGroupKind.Added,
                scope = PermissionScope.Requested,
                items = permissionItemMapper.recordedRequested(requested?.added.orEmpty(), analysedPackage),
            ),
            permissionGroup(
                id = "requested-removed",
                kind = HistoryChangeGroupKind.Removed,
                scope = PermissionScope.Requested,
                items = permissionItemMapper.recordedRequested(requested?.removed.orEmpty(), analysedPackage),
            ),
            permissionGroup(
                id = "defined-added",
                kind = HistoryChangeGroupKind.Added,
                scope = PermissionScope.Defined,
                items = permissionItemMapper.recordedDefined(defined?.added.orEmpty(), analysedPackage),
            ),
            permissionGroup(
                id = "defined-removed",
                kind = HistoryChangeGroupKind.Removed,
                scope = PermissionScope.Defined,
                items = permissionItemMapper.recordedDefined(defined?.removed.orEmpty(), analysedPackage),
            ),
            permissionGroup(
                id = "defined-changed",
                kind = HistoryChangeGroupKind.Changed,
                scope = PermissionScope.Defined,
                items = permissionItemMapper.recordedDefined(modifiedDefined.map { it.after }, analysedPackage),
                fieldChangesByName = fieldChangesByName,
            ),
        )
        return groups.takeIf { it.isNotEmpty() }?.let { HistoryChangeSection.Permissions(it) }
    }

    private fun AppHistoryDiff.androidVersionsSection(): HistoryChangeSection? {
        if (minSdkVersion == null && targetSdkVersion == null) return null
        return HistoryChangeSection.AndroidVersions(
            minSdk = minSdkVersion?.toSdkLevels(),
            targetSdk = targetSdkVersion?.toSdkLevels(),
        )
    }

    private fun ValueChange<Int?>.toSdkLevels() = ValueChange(
        before = HistorySdkLevel(before, sdkVersionResolver.resolveVersion(before)),
        after = HistorySdkLevel(after, sdkVersionResolver.resolveVersion(after)),
    )
}

private fun permissionGroup(
    id: String,
    kind: HistoryChangeGroupKind,
    scope: PermissionScope,
    items: List<PermissionItem>,
    fieldChangesByName: Map<String, List<AppHistoryFieldChange>> = emptyMap(),
): HistoryChangeGroup? = items
    .sortedWith(compareBy({ it.protectionLevel != ProtectionLevel.Dangerous }, { it.label.lowercase() }))
    .map { HistoryChangeRow.Permission(key = "$id:${it.name}", item = it, fieldChanges = fieldChangesByName[it.name].orEmpty()) }
    .takeIf { it.isNotEmpty() }
    ?.let { HistoryChangeGroup(id = id, kind = kind, scope = scope, rows = it) }

private fun AppHistoryDiff.signingSection(): HistoryChangeSection? {
    val certificateChange = certificates as? SetChange.Changed
    if (!hasCertificateChange && signingSchemeVersions == null) return null
    return HistoryChangeSection.Signing(
        certificatesBefore = certificateChange?.removed,
        certificatesAfter = certificateChange?.added,
        schemes = signingSchemeVersions,
    )
}

private fun AppHistoryDiff.appSettingsSection(): HistoryChangeSection? {
    if (isDebuggable == null && allowsBackup == null && usesCleartextTraffic == null) return null
    return HistoryChangeSection.AppSettings(debuggable = isDebuggable, backup = allowsBackup, cleartext = usesCleartextTraffic)
}

private fun AppHistoryDiff.componentsSection(): HistoryChangeSection? {
    val activities = activities as? SetChange.Changed
    val services = services as? SetChange.Changed
    val receivers = receivers as? SetChange.Changed
    val providers = providers as? SetChange.Changed
    val added = activities?.added.orEmpty().map { it.toItem(canLaunch = false, intentFilters = null) } +
        services?.added.orEmpty().map { it.toItem(intentFilters = null) } +
        receivers?.added.orEmpty().map { it.toItem(canLaunch = false, intentFilters = null) } +
        providers?.added.orEmpty().map { it.toItem(intentFilters = null) }
    val removed = activities?.removed.orEmpty().map { it.toItem(canLaunch = false, intentFilters = null) } +
        services?.removed.orEmpty().map { it.toItem(intentFilters = null) } +
        receivers?.removed.orEmpty().map { it.toItem(canLaunch = false, intentFilters = null) } +
        providers?.removed.orEmpty().map { it.toItem(intentFilters = null) }
    val modified = activities?.modified.orEmpty().map { it.after.toItem(canLaunch = false, intentFilters = null) to it.after.fieldChangesFrom(it.before) } +
        services?.modified.orEmpty().map { it.after.toItem(intentFilters = null) to it.after.fieldChangesFrom(it.before) } +
        receivers?.modified.orEmpty().map { it.after.toItem(canLaunch = false, intentFilters = null) to it.after.fieldChangesFrom(it.before) } +
        providers?.modified.orEmpty().map { it.after.toItem(intentFilters = null) to it.after.fieldChangesFrom(it.before) }
    val fieldChangesByKey = modified.associate { (item, changes) -> item.stableKey to changes }
    val groups = listOfNotNull(
        componentGroup(id = "components-added", kind = HistoryChangeGroupKind.Added, items = added),
        componentGroup(id = "components-removed", kind = HistoryChangeGroupKind.Removed, items = removed),
        componentGroup(
            id = "components-changed",
            kind = HistoryChangeGroupKind.Changed,
            items = modified.map {
                it.first
            },
            fieldChangesByKey = fieldChangesByKey,
        ),
    )
    return groups.takeIf { it.isNotEmpty() }?.let { HistoryChangeSection.Components(it) }
}

private fun componentGroup(
    id: String,
    kind: HistoryChangeGroupKind,
    items: List<ComponentItem>,
    fieldChangesByKey: Map<String, List<AppHistoryFieldChange>> = emptyMap(),
): HistoryChangeGroup? = items
    .mergeDuplicatesByStableKey()
    .sortedForDisplay()
    .map { HistoryChangeRow.Component(key = "$id:${it.stableKey}", item = it, fieldChanges = fieldChangesByKey[it.stableKey].orEmpty()) }
    .takeIf { it.isNotEmpty() }
    ?.let { HistoryChangeGroup(id = id, kind = kind, rows = it) }

private fun AppHistoryDiff.nativeLibrariesSection(): HistoryChangeSection? {
    val libraries = nativeLibraries as? SetChange.Changed ?: return null
    val groups = listOfNotNull(
        nativeGroup(id = "native-added", kind = HistoryChangeGroupKind.Added, files = libraries.added),
        nativeGroup(id = "native-removed", kind = HistoryChangeGroupKind.Removed, files = libraries.removed),
        nativeGroup(
            id = "native-changed",
            kind = HistoryChangeGroupKind.Changed,
            files = libraries.modified.map { it.after },
            fieldChangesByName = libraries.modified.fieldChangesByLibraryName(),
        ),
    )
    return groups.takeIf { it.isNotEmpty() }?.let { HistoryChangeSection.NativeLibraries(it) }
}

private fun nativeGroup(
    id: String,
    kind: HistoryChangeGroupKind,
    files: List<NativeLibraryFile>,
    fieldChangesByName: Map<String, List<AppHistoryFieldChange>> = emptyMap(),
): HistoryChangeGroup? = files
    .groupedByLibrary(deviceAbis = emptySet())
    .map { HistoryChangeRow.NativeLibrary(key = "$id:${it.name}", item = it, fieldChanges = fieldChangesByName[it.name].orEmpty()) }
    .takeIf { it.isNotEmpty() }
    ?.let { HistoryChangeGroup(id = id, kind = kind, rows = it) }
