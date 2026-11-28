package sk.styk.martin.apkanalyzer.core.apps.impl

import android.content.ComponentName
import android.content.Context
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import sk.styk.martin.apkanalyzer.core.apps.api.PackageChangesObserverFake
import sk.styk.martin.apkanalyzer.core.apps.api.storagestats.StorageStatsRepositoryFake
import sk.styk.martin.apkanalyzer.core.apps.api.usagestats.UsageStatsRepositoryFake
import sk.styk.martin.apkanalyzer.core.apps.impl.installsource.InstallSourceResolverImpl
import sk.styk.martin.apkanalyzer.core.apps.impl.permissions.PermissionDefinitionResolverImpl
import sk.styk.martin.apkanalyzer.core.apps.impl.sdkversion.SdkVersionResolverImpl
import sk.styk.martin.apkanalyzer.core.apps.impl.signing.CertificateExtractorImpl
import sk.styk.martin.apkanalyzer.core.common.digest.DigestManager
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTrackerFake
import sk.styk.martin.apkanalyzer.core.common.resources.ResourcesManager

@RunWith(RobolectricTestRunner::class)
class AppDetailRepositoryImplTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val packageManager = context.packageManager
    private val repository = AppDetailRepositoryImpl(
        packageManager = packageManager,
        sdkVersionResolver = SdkVersionResolverImpl(ResourcesManager(context)),
        installSourceResolver = InstallSourceResolverImpl(packageManager),
        certificateExtractor = CertificateExtractorImpl(DigestManager()),
        permissionDefinitionResolver = PermissionDefinitionResolverImpl(packageManager),
        storageStatsRepository = StorageStatsRepositoryFake(),
        usageStatsRepository = UsageStatsRepositoryFake(),
        packageChangesObserver = PackageChangesObserverFake(),
        performanceTracker = PerformanceTrackerFake(),
    )

    @Test
    fun `Given the app disabled an activity When loading details Then the activity is still listed`() = runTest {
        shadowOf(packageManager).installPackage(aPackage(MAIN_ACTIVITY, CLIPBOARD_ACTIVITY))
        packageManager.disable(CLIPBOARD_ACTIVITY)

        val detail = repository.details(AppReference.InstalledPackage(PackageName(DOCS))).getOrThrow()

        detail.activities.map { it.name } shouldBe listOf(MAIN_ACTIVITY, CLIPBOARD_ACTIVITY)
    }

    @Test
    fun `Given the app disabled its launcher alias When loading details Then the alias is still a launcher`() = runTest {
        val docs = aPackage()
        shadowOf(packageManager).installPackage(docs)
        listOf(MAIN_ACTIVITY, LAUNCHER_ALIAS).forEach { shadowOf(packageManager).addOrUpdateActivity(anActivity(it, docs.applicationInfo!!)) }
        shadowOf(packageManager).addIntentFilterForActivity(ComponentName(DOCS, LAUNCHER_ALIAS), aLauncherFilter())
        packageManager.disable(LAUNCHER_ALIAS)

        val detail = repository.details(AppReference.InstalledPackage(PackageName(DOCS))).getOrThrow()

        detail.activities.associate { it.name to it.isLauncher } shouldBe mapOf(MAIN_ACTIVITY to false, LAUNCHER_ALIAS to true)
    }
}

private const val DOCS = "com.google.android.apps.docs"
private const val MAIN_ACTIVITY = "com.google.android.apps.docs.app.NewMainProxyActivity"
private const val CLIPBOARD_ACTIVITY = "com.google.android.apps.docs.drive.clipboard.SendTextToClipboardActivity"
private const val LAUNCHER_ALIAS = "com.google.androidbrowserhelper.trusted.LauncherActivityAlias"

private fun PackageManager.disable(activityName: String) {
    setComponentEnabledSetting(ComponentName(DOCS, activityName), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
}

private fun aLauncherFilter() = IntentFilter("android.intent.action.MAIN").apply { addCategory("android.intent.category.LAUNCHER") }

private fun aPackage(vararg activityNames: String): PackageInfo {
    val application = ApplicationInfo().apply {
        packageName = DOCS
        enabled = true
    }
    return PackageInfo().apply {
        packageName = DOCS
        applicationInfo = application
        activities = activityNames.map { anActivity(it, application) }.toTypedArray()
    }
}

private fun anActivity(name: String, application: ApplicationInfo) = ActivityInfo().apply {
    this.name = name
    packageName = DOCS
    applicationInfo = application
    enabled = true
    exported = true
}
