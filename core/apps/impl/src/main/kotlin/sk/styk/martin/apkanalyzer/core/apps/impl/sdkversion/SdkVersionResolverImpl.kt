package sk.styk.martin.apkanalyzer.core.apps.impl.sdkversion

import sk.styk.martin.apkanalyzer.core.apps.api.sdkversion.SdkVersionResolver
import sk.styk.martin.apkanalyzer.core.apps.impl.R
import sk.styk.martin.apkanalyzer.core.common.resources.ResourcesManager
import javax.inject.Inject

internal class SdkVersionResolverImpl @Inject constructor(private val resourcesManager: ResourcesManager) : SdkVersionResolver {
    override fun resolveVersion(sdkVersion: Int?): String? = sdkVersion?.let { sdk ->
        val versions = resourcesManager.getStringArray(R.array.android_versions)
        versions.getOrNull(sdk - 1) ?: "Android ${sdk - 20}"
    }
}
