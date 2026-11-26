package sk.styk.martin.apkanalyzer.core.apps.impl.model

import android.content.pm.ApplicationInfo
import sk.styk.martin.apkanalyzer.core.apps.api.model.AppCategory

internal fun resolveAppCategory(category: Int): AppCategory = when (category) {
    ApplicationInfo.CATEGORY_GAME -> AppCategory.Game
    ApplicationInfo.CATEGORY_AUDIO -> AppCategory.Audio
    ApplicationInfo.CATEGORY_VIDEO -> AppCategory.Video
    ApplicationInfo.CATEGORY_IMAGE -> AppCategory.Image
    ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.Social
    ApplicationInfo.CATEGORY_NEWS -> AppCategory.News
    ApplicationInfo.CATEGORY_MAPS -> AppCategory.Maps
    ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.Productivity
    ApplicationInfo.CATEGORY_ACCESSIBILITY -> AppCategory.Accessibility
    else -> AppCategory.Undefined
}
