package sk.styk.martin.apkanalyzer.core.apps.api.sdkversion

interface SdkVersionResolver {
    fun resolveVersion(sdkVersion: Int?): String?
}
