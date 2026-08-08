package sk.styk.martin.apkanalyzer.core.common.settings

sealed interface Key<T : Any> {
    data object ColorScheme : Key<ColorAppScheme>

    data object OnboardingRequired : Key<Boolean>

    data object AppStartNumber : Key<Int>

    data object RecentlyViewedApps : Key<List<String>>

    data object RecentlyViewedAppsEnabled : Key<Boolean>

    data object SearchHistory : Key<List<String>>
}

enum class ColorAppScheme {
    Day,
    Night,
    FollowSystem,
}
