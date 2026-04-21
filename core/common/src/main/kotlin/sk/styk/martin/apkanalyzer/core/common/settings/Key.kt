package sk.styk.martin.apkanalyzer.core.common.settings

sealed class Key<T> {
    data object ColorScheme : Key<ColorAppScheme>()

    data object OnboardingRequired : Key<Boolean>()

    data object AppStartNumber : Key<Int>()

    data object RecentlyViewedApps : Key<List<String>>()
}

enum class ColorAppScheme {
    Day,
    Night,
    FollowSystem,
}
