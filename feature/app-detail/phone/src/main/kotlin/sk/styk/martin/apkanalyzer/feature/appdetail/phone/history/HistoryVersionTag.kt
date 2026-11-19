package sk.styk.martin.apkanalyzer.feature.appdetail.phone.history

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryEntry
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryVersion
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.ValueChange
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.isOlderVersion
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.isSameVersionBuild
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.label
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.apphistory.R as HistoryR

internal enum class HistoryVersionTag(@StringRes val labelRes: Int) {
    OlderVersion(HistoryR.string.app_history_older_version),
    SameVersionBuild(HistoryR.string.app_history_same_version),
}

internal val AppHistoryEntry.Change.versionTag: HistoryVersionTag?
    get() = when {
        isOlderVersion -> HistoryVersionTag.OlderVersion
        isSameVersionBuild -> HistoryVersionTag.SameVersionBuild
        else -> null
    }

internal fun AppHistoryEntry.Change.Kind.icon(versionTag: HistoryVersionTag?): ImageVector = when (this) {
    AppHistoryEntry.Change.Kind.Updated -> when (versionTag) {
        HistoryVersionTag.OlderVersion -> ApkAnalyzerIcons.Downgrade
        HistoryVersionTag.SameVersionBuild -> ApkAnalyzerIcons.Sync
        null -> ApkAnalyzerIcons.Upgrade
    }

    AppHistoryEntry.Change.Kind.Reinstalled -> ApkAnalyzerIcons.Export

    AppHistoryEntry.Change.Kind.NewDevice -> ApkAnalyzerIcons.Screen
}

internal fun AppHistoryVersion.text(change: ValueChange<AppHistoryVersion>?, resources: Resources): String = change?.label(resources) ?: label(resources)
