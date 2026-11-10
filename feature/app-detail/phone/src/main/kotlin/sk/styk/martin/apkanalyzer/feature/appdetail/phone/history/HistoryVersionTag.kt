package sk.styk.martin.apkanalyzer.feature.appdetail.phone.history

import android.content.res.Resources
import androidx.annotation.StringRes
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryEntry
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryVersion
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.ValueChange
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.isOlderVersion
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.isSameVersionBuild
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.label
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

internal fun AppHistoryVersion.text(change: ValueChange<AppHistoryVersion>?, resources: Resources): String = change?.label(resources) ?: label(resources)

internal fun String.unlessSameAs(headline: String): String? = takeIf { it != headline }
