package sk.styk.martin.apkanalyzer.core.apps.analysis

import android.content.res.Resources
import sk.styk.martin.apkanalyzer.core.apps.model.ComponentIntentFilter
import sk.styk.martin.apkanalyzer.core.apps.model.ComponentIntentFilterKey
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

internal interface ComponentManifestParser {
    fun parseInstalled(
        resources: Resources,
        packageName: PackageName,
        expectedManifestCount: Int,
    ): List<Pair<ComponentIntentFilterKey, ComponentIntentFilter>>

    fun parse(resources: Resources): List<Pair<ComponentIntentFilterKey, ComponentIntentFilter>>
}
