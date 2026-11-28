package sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.change

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryEntry
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryFieldChange
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryInstallSource
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryRecordedOn
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryVersion
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.SetChange
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.ValueChange
import sk.styk.martin.apkanalyzer.core.apppermissions.api.detail.PermissionItem
import sk.styk.martin.apkanalyzer.core.apppermissions.api.detail.PermissionScope
import sk.styk.martin.apkanalyzer.core.apps.api.devicefeatures.RequirementItem
import sk.styk.martin.apkanalyzer.core.apps.api.packaging.NativeLibraryItem
import sk.styk.martin.apkanalyzer.core.apps.api.signing.Certificate
import sk.styk.martin.apkanalyzer.core.apps.api.signing.SigningSchemeVersion
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.ChangeDirection
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.insight.AppDetailInsight
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.appcomponents.ComponentItem
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.HistoryVersionTag
import java.time.Instant

internal fun historyRowKey(groupId: String, itemId: String): String = "$groupId:$itemId"

@Immutable
internal sealed interface HistoryChangeRow {
    val key: String
    val name: String
    val fieldChanges: List<AppHistoryFieldChange>

    @Immutable
    data class Permission(
        override val key: String,
        val item: PermissionItem,
        override val fieldChanges: List<AppHistoryFieldChange> = emptyList(),
    ) : HistoryChangeRow {
        override val name: String
            get() = item.name
    }

    @Immutable
    data class Component(
        override val key: String,
        val item: ComponentItem,
        override val fieldChanges: List<AppHistoryFieldChange> = emptyList(),
    ) : HistoryChangeRow {
        override val name: String
            get() = item.name
    }

    @Immutable
    data class NativeLibrary(
        override val key: String,
        val item: NativeLibraryItem,
        override val fieldChanges: List<AppHistoryFieldChange> = emptyList(),
    ) : HistoryChangeRow {
        override val name: String
            get() = item.name
    }

    @Immutable
    data class Requirement(
        override val key: String,
        val item: RequirementItem,
        override val fieldChanges: List<AppHistoryFieldChange> = emptyList(),
    ) : HistoryChangeRow {
        override val name: String
            get() = item.identifier
    }
}

@Immutable
internal data class HistoryChangeGroup(
    val id: String,
    val kind: ChangeDirection,
    val scope: PermissionScope? = null,
    val rows: List<HistoryChangeRow>,
)

@Immutable
internal data class HistorySdkLevel(val version: Int?, val label: String?)

@Immutable
internal sealed interface HistoryChangeSection {
    sealed interface Grouped : HistoryChangeSection {
        val groups: List<HistoryChangeGroup>
    }

    @Immutable
    data class WorthKnowing(
        val hasCertificateChange: Boolean,
        val isCertificateWarning: Boolean,
        val dangerousPermissions: List<HistoryChangeRow.Permission>,
        val addedInsights: List<AppDetailInsight>,
        val removedInsights: List<AppDetailInsight>,
    ) : HistoryChangeSection

    @Immutable
    data class Signing(
        val certificatesBefore: List<Certificate>?,
        val certificatesAfter: List<Certificate>?,
        val schemes: SetChange.Changed<SigningSchemeVersion>?,
    ) : HistoryChangeSection

    @Immutable
    data class Permissions(override val groups: List<HistoryChangeGroup>) : Grouped

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
    data class Components(override val groups: List<HistoryChangeGroup>) : Grouped

    @Immutable
    data class NativeLibraries(override val groups: List<HistoryChangeGroup>) : Grouped

    @Immutable
    data class Requirements(override val groups: List<HistoryChangeGroup>) : Grouped

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
        val recordedOn: AppHistoryRecordedOn,
        val previousRecordedOn: AppHistoryRecordedOn,
        val version: AppHistoryVersion,
        val versionChange: ValueChange<AppHistoryVersion>?,
        val versionTag: HistoryVersionTag?,
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
