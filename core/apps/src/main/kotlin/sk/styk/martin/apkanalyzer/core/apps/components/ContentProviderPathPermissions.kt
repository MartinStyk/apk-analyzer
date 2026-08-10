package sk.styk.martin.apkanalyzer.core.apps.components

import android.content.pm.PathPermission
import android.os.PatternMatcher

internal fun resolvePathPermissions(pathPermissions: Array<PathPermission>?): List<ProviderPathPermission> = pathPermissions.orEmpty().map {
    ProviderPathPermission(
        path = it.path,
        matchType = resolvePathMatchType(it.type),
        readPermission = it.readPermission,
        writePermission = it.writePermission,
    )
}

private fun resolvePathMatchType(type: Int): ProviderPathMatchType = when (type) {
    PatternMatcher.PATTERN_LITERAL -> ProviderPathMatchType.Literal
    PatternMatcher.PATTERN_PREFIX -> ProviderPathMatchType.Prefix
    PatternMatcher.PATTERN_SIMPLE_GLOB -> ProviderPathMatchType.SimpleGlob
    PatternMatcher.PATTERN_ADVANCED_GLOB -> ProviderPathMatchType.AdvancedGlob
    PatternMatcher.PATTERN_SUFFIX -> ProviderPathMatchType.Suffix
    else -> ProviderPathMatchType.Literal
}
