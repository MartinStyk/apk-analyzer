package sk.styk.martin.apkanalyzer.feature.appdetail.impl.components

sealed interface AppDetailBadge {
    data object Large : AppDetailBadge
    data object System : AppDetailBadge
    data object GooglePlay : AppDetailBadge
    data object RecentlyInstalled : AppDetailBadge
    data object RecentlyUpdated : AppDetailBadge
    data object RecentlyUsed : AppDetailBadge
}
