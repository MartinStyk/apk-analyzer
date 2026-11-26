package sk.styk.martin.apkanalyzer.feature.appdetail.phone.history

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryEntry
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryVersion
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.ValueChange
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.isOlderVersion
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.isSameVersionBuild
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.label
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.R

internal enum class HistoryVersionTag(@StringRes val labelRes: Int) {
    OlderVersion(R.string.app_history_older_version),
    SameVersionBuild(R.string.app_history_same_version),
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
