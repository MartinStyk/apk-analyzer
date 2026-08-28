package sk.styk.martin.apkanalyzer.core.appfunctions

import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.AppFunctionStringValueConstraint
import dagger.hilt.android.AndroidEntryPoint
import sk.styk.martin.apkanalyzer.core.appfunctions.model.AppDetailResult
import sk.styk.martin.apkanalyzer.core.appfunctions.model.AppSummary
import sk.styk.martin.apkanalyzer.core.appfunctions.model.CertificateSummary
import sk.styk.martin.apkanalyzer.core.appfunctions.model.PermissionGrantSummary
import sk.styk.martin.apkanalyzer.core.appfunctions.usecase.FindAppsUseCase
import sk.styk.martin.apkanalyzer.core.appfunctions.usecase.GetAppCertificatesUseCase
import sk.styk.martin.apkanalyzer.core.appfunctions.usecase.GetAppDetailUseCase
import sk.styk.martin.apkanalyzer.core.appfunctions.usecase.GetAppPermissionsUseCase
import sk.styk.martin.apkanalyzer.core.appfunctions.usecase.SearchFilters
import sk.styk.martin.apkanalyzer.core.appfunctions.usecase.SortBy
import javax.inject.Inject

/**
 * Lets an on-device AI agent search apps installed on this device and inspect one app's version,
 * SDK targeting, install source, permissions, and signing certificates, without opening the app.
 *
 * Every function here is a read-only lookup; none of them change anything on the device.
 */
@RequiresApi(36)
@AndroidEntryPoint
@AppFunctionServiceEntryPoint(
    serviceName = "ApkAnalyzerAppFunctionServiceImpl",
    appFunctionXmlFileName = "apk_analyzer_app_function_service",
)
abstract class ApkAnalyzerAppFunctionService : AppFunctionService() {

    @Inject internal lateinit var findAppsUseCase: FindAppsUseCase

    @Inject internal lateinit var getAppDetailUseCase: GetAppDetailUseCase

    @Inject internal lateinit var getAppCertificatesUseCase: GetAppCertificatesUseCase

    @Inject internal lateinit var getAppPermissionsUseCase: GetAppPermissionsUseCase

    /**
     * Search and filter installed apps, combining as many conditions as needed in one call — for
     * example "large unused apps from Google Play" combines "minTotalSizeMb", "unusedForDays", and
     * "installSource" together instead of three separate lookups.
     *
     * Call this first when the user names an app by its display name rather than its exact
     * package name, then pass the resolved packageName to "getAppDetail", "getAppCertificates", or
     * "getAppPermissions".
     *
     * @param query Text matched case-insensitively against the app's display name or package
     *   name.
     * @param permission An Android permission the app must declare, for example
     *   "android.permission.CAMERA". A bare name such as "CAMERA" is treated as
     *   "android.permission.CAMERA".
     * @param installSource The app must be installed from this source: one of "GooglePlay",
     *   "SamsungGalaxyStore", "AmazonAppstore", "HuaweiAppGallery", "XiaomiGetApps", "FDroid",
     *   "AuroraStore", "Sideloaded", "LocalInstall", "SystemPreinstalled", or "Unknown".
     * @param targetSdk The exact Android API level the app must target, for example 34 for
     *   Android 14.
     * @param minTotalSizeMb The app's installed size, including its data when known, must be at
     *   least this many megabytes. Use this for "large apps" style queries.
     * @param unusedForDays The app must not have been opened in at least this many days, or never
     *   recorded as opened at all. Use this for "unused apps" style queries.
     * @param sortBy How to order the results: "Name" (alphabetical, used when omitted),
     *   "SizeDescending" (largest first), "SizeAscending" (smallest first), "LastUsedAscending"
     *   (least recently used, or never used, first), or "LastUsedDescending" (most recently used
     *   first).
     * @return Up to 25 apps matching every condition supplied, in the requested order. Empty if
     *   none match.
     * @throws AppFunctionInvalidArgumentException If every parameter is left at its default (there
     *   is nothing to search or filter by), or if installSource or sortBy isn't one of the
     *   accepted values. If thrown, ask the user what to search for or narrow by.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun findApps(
        query: String? = null,
        permission: String? = null,
        @AppFunctionStringValueConstraint(
            enumValues = [
                "GooglePlay", "SamsungGalaxyStore", "AmazonAppstore", "HuaweiAppGallery",
                "XiaomiGetApps", "FDroid", "AuroraStore", "Sideloaded", "LocalInstall",
                "SystemPreinstalled", "Unknown",
            ],
        )
        installSource: String? = null,
        targetSdk: Int? = null,
        minTotalSizeMb: Int? = null,
        unusedForDays: Int? = null,
        @AppFunctionStringValueConstraint(
            enumValues = ["Name", "SizeDescending", "SizeAscending", "LastUsedAscending", "LastUsedDescending"],
        )
        sortBy: String? = null,
    ): List<AppSummary> = findAppsUseCase(
        SearchFilters.parse(query, permission, installSource, targetSdk, minTotalSizeMb, unusedForDays),
        SortBy.parse(sortBy),
    )

    /**
     * Get version, SDK targeting, install source, permission count, and signing summary for one
     * installed app.
     *
     * Required workflow: call "findApps" first if you only have the app's display name, to
     * resolve it to an exact package name.
     *
     * @param packageName The app's exact package name, for example "com.example.app".
     * @return The app's detail summary.
     * @throws AppFunctionElementNotFoundException If no app with packageName is currently
     *   installed. If thrown, call "findApps" to search by name instead.
     * @throws AppFunctionAppUnknownException If the app's details couldn't be read for another
     *   reason. If thrown, tell the user the lookup failed unexpectedly.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getAppDetail(packageName: String): AppDetailResult = getAppDetailUseCase(packageName)

    /**
     * Get the certificates currently used to sign one installed app.
     *
     * Required workflow: call "findApps" first if you only have the app's display name, to
     * resolve it to an exact package name.
     *
     * @param packageName The app's exact package name, for example "com.example.app".
     * @return The app's current signing certificates. Most apps have exactly one; more than one
     *   means the app is signed with multiple simultaneous signers.
     * @throws AppFunctionElementNotFoundException If no app with packageName is currently
     *   installed. If thrown, call "findApps" to search by name instead.
     * @throws AppFunctionAppUnknownException If the app's certificates couldn't be read for
     *   another reason. If thrown, tell the user the lookup failed unexpectedly.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getAppCertificates(packageName: String): List<CertificateSummary> = getAppCertificatesUseCase(packageName)

    /**
     * Get an installed app's runtime permissions and whether each one is currently granted.
     *
     * Required workflow: call "findApps" first if you only have the app's display name, to
     * resolve it to an exact package name.
     *
     * @param packageName The app's exact package name, for example "com.example.app".
     * @return The app's runtime permissions and their grant state. Doesn't include permissions
     *   that are always granted at install time and never need user approval.
     * @throws AppFunctionElementNotFoundException If no app with packageName is currently
     *   installed. If thrown, call "findApps" to search by name instead.
     * @throws AppFunctionAppUnknownException If the app's permissions couldn't be read for
     *   another reason. If thrown, tell the user the lookup failed unexpectedly.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getAppPermissions(packageName: String): List<PermissionGrantSummary> = getAppPermissionsUseCase(packageName)
}
