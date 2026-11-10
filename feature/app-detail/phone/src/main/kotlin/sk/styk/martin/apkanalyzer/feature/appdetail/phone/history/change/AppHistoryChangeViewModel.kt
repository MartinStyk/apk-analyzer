package sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.change

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.AppHistoryRepository
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryDiff
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryEntry
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryFieldChange
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryTimeline
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.SetChange
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.ValueChange
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.fieldChangesFrom
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.hasCertificateChange
import sk.styk.martin.apkanalyzer.core.apppermissions.detail.PermissionItem
import sk.styk.martin.apkanalyzer.core.apppermissions.detail.PermissionItemMapper
import sk.styk.martin.apkanalyzer.core.apppermissions.detail.PermissionScope
import sk.styk.martin.apkanalyzer.core.apps.components.Activity
import sk.styk.martin.apkanalyzer.core.apps.components.BroadcastReceiver
import sk.styk.martin.apkanalyzer.core.apps.components.ContentProvider
import sk.styk.martin.apkanalyzer.core.apps.components.Service
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.DeviceFeatures
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.Feature
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.toRequirementItem
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraryFile
import sk.styk.martin.apkanalyzer.core.apps.packaging.groupedByLibrary
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.apps.sdkversion.SdkVersionResolver
import sk.styk.martin.apkanalyzer.core.common.clipboard.ClipboardManager
import sk.styk.martin.apkanalyzer.core.common.clipboard.CopyResult
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.analytics.AppDetailAnalytics
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.analytics.AppDetailAnalyticsEvent
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.appcomponents.ComponentItem
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.appcomponents.mergeDuplicatesByStableKey
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.appcomponents.sortedForDisplay
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.appcomponents.toItem
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.versionTag

@HiltViewModel(assistedFactory = AppHistoryChangeViewModel.Factory::class)
internal class AppHistoryChangeViewModel @AssistedInject constructor(
    @Assisted private val packageName: String,
    @Assisted private val snapshotId: Long,
    private val appHistoryRepository: AppHistoryRepository,
    private val permissionItemMapper: PermissionItemMapper,
    private val sdkVersionResolver: SdkVersionResolver,
    private val clipboardManager: ClipboardManager,
    private val analytics: AppDetailAnalytics,
    private val insightsComparer: HistoryInsightsComparer,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(packageName: String, snapshotId: Long): AppHistoryChangeViewModel
    }

    private var isOpenTracked = false
    private var loadJob: Job? = null

    val state: StateFlow<AppHistoryChangeState>
        field = MutableStateFlow<AppHistoryChangeState>(AppHistoryChangeState.Loading)

    private val eventChannel = Channel<AppHistoryChangeEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        load()
    }

    fun onAction(action: AppHistoryChangeAction) {
        when (action) {
            AppHistoryChangeAction.Back -> sendEvent(AppHistoryChangeEvent.NavigateBack)

            AppHistoryChangeAction.Retry -> load()

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
        sendEvent(
            AppHistoryChangeEvent.NavigateToFullDetail(
                AppDetailInput.Historical(
                    packageName = packageName,
                    snapshotId = snapshotId,
                    capturedAt = loaded.capturedAt,
                ),
            ),
        )
    }

    private fun load() {
        loadJob?.cancel()
        state.value = AppHistoryChangeState.Loading
        loadJob = viewModelScope.launch(dispatcherProvider.default()) {
            appHistoryRepository.timeline(PackageName(packageName)).collect { timeline ->
                state.value = timeline.toState()
            }
        }
    }

    private fun sendEvent(event: AppHistoryChangeEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    private fun trackOpened(change: AppHistoryEntry.Change) {
        if (isOpenTracked) return
        isOpenTracked = true
        analytics.track(AppDetailAnalyticsEvent.HistoryChangeOpened(change.diff.changedCategories))
    }

    private suspend fun AppHistoryTimeline.toState(): AppHistoryChangeState = when (this) {
        AppHistoryTimeline.Unavailable -> AppHistoryChangeState.Error

        is AppHistoryTimeline.Available ->
            entries
                .filterIsInstance<AppHistoryEntry.Change>()
                .firstOrNull { it.snapshot.id.value == snapshotId }
                ?.also(::trackOpened)
                ?.let { it.toLoadedState(insights = insightsComparer.compare(packageName, it)) }
                ?: AppHistoryChangeState.NotFound
    }

    private fun AppHistoryEntry.Change.toLoadedState(insights: HistoryChangeSection.Insights?) = AppHistoryChangeState.Loaded(
        appName = snapshot.applicationName,
        kind = kind,
        timestamp = timestamp,
        capturedAt = snapshot.capturedAt,
        version = snapshot.version,
        versionChange = diff.version,
        versionTag = versionTag,
        sections = listOfNotNull(insights) + toSections(),
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
        diff.requirementsSection(),
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
    val schemeChange = signingSchemes as? SetChange.Changed
    if (!hasCertificateChange && schemeChange == null) return null
    return HistoryChangeSection.Signing(
        certificatesBefore = certificateChange?.removed,
        certificatesAfter = certificateChange?.added,
        schemes = schemeChange,
    )
}

private fun AppHistoryDiff.appSettingsSection(): HistoryChangeSection? {
    if (isDebuggable == null && allowsBackup == null && usesCleartextTraffic == null) return null
    return HistoryChangeSection.AppSettings(debuggable = isDebuggable, backup = allowsBackup, cleartext = usesCleartextTraffic)
}

private class ComponentChanges(
    val added: List<ComponentItem>,
    val removed: List<ComponentItem>,
    val modified: List<Pair<ComponentItem, List<AppHistoryFieldChange>>>,
)

private fun <T> SetChange<T>.toComponentChanges(toItem: (T) -> ComponentItem, fieldChanges: T.(before: T) -> List<AppHistoryFieldChange>): ComponentChanges? =
    (this as? SetChange.Changed)?.let { changed ->
        ComponentChanges(
            added = changed.added.map(toItem),
            removed = changed.removed.map(toItem),
            modified = changed.modified.map { toItem(it.after) to it.after.fieldChanges(it.before) },
        )
    }

private fun AppHistoryDiff.componentsSection(): HistoryChangeSection? {
    val changes = listOfNotNull(
        activities.toComponentChanges({ it.toItem(canLaunch = false, intentFilters = null) }, Activity::fieldChangesFrom),
        services.toComponentChanges({ it.toItem(intentFilters = null) }, Service::fieldChangesFrom),
        receivers.toComponentChanges({ it.toItem(canLaunch = false, intentFilters = null) }, BroadcastReceiver::fieldChangesFrom),
        providers.toComponentChanges({ it.toItem(intentFilters = null) }, ContentProvider::fieldChangesFrom),
    )
    val modified = changes.flatMap { it.modified }
    val groups = listOfNotNull(
        componentGroup(id = "components-added", kind = HistoryChangeGroupKind.Added, items = changes.flatMap { it.added }),
        componentGroup(id = "components-removed", kind = HistoryChangeGroupKind.Removed, items = changes.flatMap { it.removed }),
        componentGroup(
            id = "components-changed",
            kind = HistoryChangeGroupKind.Changed,
            items = modified.map { it.first },
            fieldChangesByKey = modified.associate { (item, fieldChanges) -> item.stableKey to fieldChanges },
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

private fun AppHistoryDiff.requirementsSection(): HistoryChangeSection? {
    val features = requirements as? SetChange.Changed ?: return null
    val groups = listOfNotNull(
        requirementGroup(id = "requirements-added", kind = HistoryChangeGroupKind.Added, features = features.added),
        requirementGroup(id = "requirements-removed", kind = HistoryChangeGroupKind.Removed, features = features.removed),
        requirementGroup(
            id = "requirements-changed",
            kind = HistoryChangeGroupKind.Changed,
            features = features.modified.map { it.after },
            fieldChanges = features.modified.map { it.after.fieldChangesFrom(it.before) },
        ),
    )
    return groups.takeIf { it.isNotEmpty() }?.let { HistoryChangeSection.Requirements(it) }
}

private fun requirementGroup(
    id: String,
    kind: HistoryChangeGroupKind,
    features: List<Feature>,
    fieldChanges: List<List<AppHistoryFieldChange>> = emptyList(),
): HistoryChangeGroup? = features
    .mapIndexed { index, feature ->
        val item = feature.toRequirementItem(DeviceFeatures.Unknown)
        HistoryChangeRow.Requirement(key = "$id:${item.identifier}", item = item, fieldChanges = fieldChanges.getOrNull(index).orEmpty())
    }
    .sortedWith(compareBy({ !it.item.isRequired }, { it.item.identifier }))
    .takeIf { it.isNotEmpty() }
    ?.let { HistoryChangeGroup(id = id, kind = kind, rows = it) }

private fun AppHistoryDiff.nativeLibrariesSection(): HistoryChangeSection? {
    val libraries = nativeLibraries as? SetChange.Changed ?: return null
    val groups = listOfNotNull(
        nativeGroup(id = "native-added", kind = HistoryChangeGroupKind.Added, files = libraries.added.flatMap { it.files }),
        nativeGroup(id = "native-removed", kind = HistoryChangeGroupKind.Removed, files = libraries.removed.flatMap { it.files }),
        nativeGroup(
            id = "native-changed",
            kind = HistoryChangeGroupKind.Changed,
            files = libraries.modified.flatMap { it.after.files },
            fieldChangesByName = libraries.modified.associate { it.after.name to it.after.fieldChangesFrom(it.before) },
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
