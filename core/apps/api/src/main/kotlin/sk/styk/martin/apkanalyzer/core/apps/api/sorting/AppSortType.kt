package sk.styk.martin.apkanalyzer.core.apps.sorting

import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import java.text.Collator
import java.util.Locale

enum class AppSortType {
    Name,
    TotalSize,
    ApkSize,
    InstallDate,
    LastUpdated,
    LastUsed,
    TargetSdk,
}

fun AppSortType.comparator(ascending: Boolean, locale: Locale = Locale.getDefault()): Comparator<InstalledApp> {
    val base: Comparator<InstalledApp> = when (this) {
        AppSortType.Name -> {
            val collator = Collator.getInstance(locale).apply { strength = Collator.SECONDARY }
            Comparator { a, b -> collator.compare(a.applicationName, b.applicationName) }
        }

        AppSortType.ApkSize -> compareBy { it.apkSize }

        AppSortType.TotalSize -> compareBy(nullsFirst()) { it.totalSize }

        AppSortType.InstallDate -> compareBy { it.installTime }

        AppSortType.TargetSdk -> compareBy { it.targetSdk }

        AppSortType.LastUpdated -> compareBy { it.lastUpdateTime }

        AppSortType.LastUsed -> compareBy(nullsFirst()) { it.lastUsedTime }
    }
    return if (ascending) base else base.reversed()
}
