package sk.styk.martin.apkanalyzer.feature.appdetail.phone.history

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryEntry
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryFieldChange
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryInstallSource
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryVersion
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.ValueChange
import sk.styk.martin.apkanalyzer.core.apppermissions.detail.PermissionItem
import sk.styk.martin.apkanalyzer.core.apppermissions.detail.PermissionScope
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraryItem
import sk.styk.martin.apkanalyzer.core.apps.signing.Certificate
import sk.styk.martin.apkanalyzer.core.apps.signing.SigningSchemeVersion
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.appcomponents.ComponentItem
import java.time.Instant

internal enum class HistoryChangeGroupKind {
    Added,
    Removed,
    Changed,
}

@Immutable
internal sealed interface HistoryChangeRow {
    val key: String
    val fieldChanges: List<AppHistoryFieldChange>

    @Immutable
    data class Permission(
        override val key: String,
        val item: PermissionItem,
        override val fieldChanges: List<AppHistoryFieldChange> = emptyList(),
    ) : HistoryChangeRow

    @Immutable
    data class Component(
        override val key: String,
        val item: ComponentItem,
        override val fieldChanges: List<AppHistoryFieldChange> = emptyList(),
    ) : HistoryChangeRow

    @Immutable
    data class NativeLibrary(
        override val key: String,
        val item: NativeLibraryItem,
        override val fieldChanges: List<AppHistoryFieldChange> = emptyList(),
    ) : HistoryChangeRow
}

@Immutable
internal data class HistoryChangeGroup(
    val id: String,
    val kind: HistoryChangeGroupKind,
    val scope: PermissionScope? = null,
    val rows: List<HistoryChangeRow>,
)

@Immutable
internal data class HistorySdkLevel(val version: Int?, val label: String?)

@Immutable
internal sealed interface HistoryChangeSection {
    @Immutable
    data class Signing(
        val certificatesBefore: List<Certificate>?,
        val certificatesAfter: List<Certificate>?,
        val schemes: ValueChange<List<SigningSchemeVersion>>?,
    ) : HistoryChangeSection

    @Immutable
    data class Permissions(val groups: List<HistoryChangeGroup>) : HistoryChangeSection

    @Immutable
    data class InstallSource(val change: ValueChange<AppHistoryInstallSource>) : HistoryChangeSection

    @Immutable
    data class AppSettings(
        val debuggable: ValueChange<Boolean>?,
        val backup: ValueChange<Boolean>?,
        val cleartext: ValueChange<Boolean>?,
    ) : HistoryChangeSection

    @Immutable
    data class AndroidVersions(val minSdk: ValueChange<HistorySdkLevel>?, val targetSdk: ValueChange<HistorySdkLevel>?) : HistoryChangeSection

    @Immutable
    data class Components(val groups: List<HistoryChangeGroup>) : HistoryChangeSection

    @Immutable
    data class NativeLibraries(val groups: List<HistoryChangeGroup>) : HistoryChangeSection

    @Immutable
    data class Size(val change: ValueChange<AppSize>) : HistoryChangeSection
}

@Immutable
internal sealed interface AppHistoryChangeState {
    data object Loading : AppHistoryChangeState

    data object Error : AppHistoryChangeState

    data object NotFound : AppHistoryChangeState

    @Immutable
    data class Loaded(
        val appName: String,
        val kind: AppHistoryEntry.Change.Kind,
        val timestamp: Instant,
        val capturedAt: Instant,
        val version: AppHistoryVersion,
        val versionChange: ValueChange<AppHistoryVersion>?,
        val isOlderVersion: Boolean,
        val isSameVersionBuild: Boolean,
        val sections: List<HistoryChangeSection>,
        val isSigningMethodUncomparable: Boolean,
        val isNativeLibrariesUncomparable: Boolean,
    ) : AppHistoryChangeState {
        val hasUncomparableDetails: Boolean
            get() = isSigningMethodUncomparable || isNativeLibrariesUncomparable

        val hasNoChanges: Boolean
            get() = sections.isEmpty() && versionChange == null && !hasUncomparableDetails

        val showsDeviceNote: Boolean
            get() = kind == AppHistoryEntry.Change.Kind.NewDevice && sections.isNotEmpty()
    }
}
