package sk.styk.martin.apkanalyzer.feature.appdetail.impl.navigation

import androidx.navigation3.runtime.NavKey
import sk.styk.martin.apkanalyzer.core.navigation.ScreenOpenEvent
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailNavKey

private const val SCREEN_APP_DETAIL = "app_detail"
private const val SCREEN_GENERAL_INFO = "general_info"
private const val SCREEN_CERTIFICATES = "certificates"
private const val SCREEN_COMPONENTS = "components"
private const val SCREEN_INTENT_FILTERS = "intent_filters"
private const val SCREEN_MANIFEST = "manifest"
private const val SCREEN_NATIVE_LIBRARIES = "native_libraries"
private const val SCREEN_PERMISSIONS = "permissions"
private const val SCREEN_REQUIREMENTS = "requirements"
private const val SCREEN_SPLIT_APKS = "split_apks"

fun screenOpenEvent(key: NavKey): ScreenOpenEvent? = when (key) {
    is AppDetailNavKey -> ScreenOpenEvent(SCREEN_APP_DETAIL, key.detailInput.diagnosticContext())
    is GeneralInfoNavKey -> ScreenOpenEvent(SCREEN_GENERAL_INFO, key.detailInput.diagnosticContext())
    is CertificatesNavKey -> ScreenOpenEvent(SCREEN_CERTIFICATES, key.detailInput.diagnosticContext())
    is ComponentsNavKey -> ScreenOpenEvent(SCREEN_COMPONENTS, key.detailInput.diagnosticContext())
    is IntentFiltersNavKey -> ScreenOpenEvent(SCREEN_INTENT_FILTERS, key.detailInput.diagnosticContext())
    is ManifestNavKey -> ScreenOpenEvent(SCREEN_MANIFEST, key.detailInput.diagnosticContext())
    is NativeLibrariesNavKey -> ScreenOpenEvent(SCREEN_NATIVE_LIBRARIES, key.detailInput.diagnosticContext())
    is PermissionsNavKey -> ScreenOpenEvent(SCREEN_PERMISSIONS, key.detailInput.diagnosticContext())
    is RequirementsNavKey -> ScreenOpenEvent(SCREEN_REQUIREMENTS, key.detailInput.diagnosticContext())
    is SplitApksNavKey -> ScreenOpenEvent(SCREEN_SPLIT_APKS, key.detailInput.diagnosticContext())
    else -> null
}

private fun AppDetailInput.diagnosticContext(): String = when (this) {
    is AppDetailInput.InstalledPackage -> "mode=installed package=$packageName"
    is AppDetailInput.ApkFile -> "mode=apk_file apk_path=$apkFilePath"
}
