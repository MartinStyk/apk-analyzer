package sk.styk.martin.apkanalyzer.core.common.settings

sealed interface Key<T : Any> {
    data object ColorScheme : Key<ColorAppScheme>

    data object HexFingerprintFormat : Key<FingerprintFormat>

    data object OnboardingRequired : Key<Boolean>

    data object AppStartNumber : Key<Int>

    data object RecentlyViewedAppsEnabled : Key<Boolean>

    data object AppDetailQualifiedSessionCount : Key<Int>

    data object ReviewFlowRequested : Key<Boolean>

    data object InventoryExportColumns : Key<Set<String>>

    data object InventoryExportFormat : Key<String>

    data object InventoryExportIntroDismissed : Key<Boolean>
}

enum class ColorAppScheme {
    Day,
    Night,
    FollowSystem,
}

enum class FingerprintFormat {
    ColonSeparated,
    Continuous,
}
