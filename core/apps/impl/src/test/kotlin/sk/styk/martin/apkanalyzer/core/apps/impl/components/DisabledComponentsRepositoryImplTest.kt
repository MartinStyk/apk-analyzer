package sk.styk.martin.apkanalyzer.core.apps.impl.components

import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ComponentInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.content.pm.ServiceInfo
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import sk.styk.martin.apkanalyzer.core.apps.api.components.DisabledComponents
import sk.styk.martin.apkanalyzer.core.common.coroutines.TestDispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTrackerFake

@RunWith(RobolectricTestRunner::class)
class DisabledComponentsRepositoryImplTest {

    @get:Rule
    val dispatcherProvider = TestDispatcherProvider()

    private val context: Context = RuntimeEnvironment.getApplication()
    private val packageManager = context.packageManager
    private val repository = DisabledComponentsRepositoryImpl(packageManager, dispatcherProvider, PerformanceTrackerFake())

    @Test
    fun `Given one disabled component of each kind When loading Then each kind lists it`() = runTest {
        shadowOf(packageManager).installPackage(
            aPackage(
                activities = listOf(anActivity(MAIN_ACTIVITY), anActivity(CLIPBOARD_ACTIVITY)),
                services = listOf(aService(SYNC_SERVICE), aService(DELEGATION_SERVICE)),
                receivers = listOf(anActivity(BOOT_RECEIVER), anActivity(WIDGET_RECEIVER)),
                providers = listOf(aProvider(FILE_PROVIDER), aProvider(SEARCH_PROVIDER)),
            ),
        )
        listOf(CLIPBOARD_ACTIVITY, DELEGATION_SERVICE, WIDGET_RECEIVER, SEARCH_PROVIDER).forEach { packageManager.disable(it) }

        val disabled = repository.disabledComponents(PackageName(DOCS)).getOrThrow()

        disabled shouldBe DisabledComponents(
            activities = setOf(CLIPBOARD_ACTIVITY),
            services = setOf(DELEGATION_SERVICE),
            receivers = setOf(WIDGET_RECEIVER),
            providers = setOf(SEARCH_PROVIDER),
        )
    }

    @Test
    fun `Given manifest disabled activities When one is turned on Then only the other is disabled`() = runTest {
        shadowOf(packageManager).installPackage(
            aPackage(activities = listOf(anActivity(MAIN_ACTIVITY, enabled = false), anActivity(LAUNCHER_ALIAS, enabled = false))),
        )
        packageManager.setComponentEnabledSetting(
            ComponentName(DOCS, MAIN_ACTIVITY),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )

        val disabled = repository.disabledComponents(PackageName(DOCS)).getOrThrow()

        disabled shouldBe DisabledComponents(activities = setOf(LAUNCHER_ALIAS))
    }

    @Test
    fun `Given a package that is not installed When loading Then the result is a failure`() = runTest {
        val result = repository.disabledComponents(PackageName(DOCS))

        result.isFailure shouldBe true
    }
}

private const val DOCS = "com.google.android.apps.docs"
private const val MAIN_ACTIVITY = "com.google.android.apps.docs.app.NewMainProxyActivity"
private const val CLIPBOARD_ACTIVITY = "com.google.android.apps.docs.drive.clipboard.SendTextToClipboardActivity"
private const val LAUNCHER_ALIAS = "com.google.androidbrowserhelper.trusted.LauncherActivityAlias"
private const val SYNC_SERVICE = "com.google.android.apps.docs.sync.ContentSyncService"
private const val DELEGATION_SERVICE = "com.google.android.libraries.docs.delegation.DelegationService"
private const val BOOT_RECEIVER = "com.google.android.apps.docs.receivers.BootCompletedReceiver"
private const val WIDGET_RECEIVER = "com.google.android.apps.docs.widget.WidgetUpdateReceiver"
private const val FILE_PROVIDER = "androidx.core.content.FileProvider"
private const val SEARCH_PROVIDER = "com.google.android.apps.docs.search.SearchSuggestionProvider"

private val docsApplication = ApplicationInfo().apply {
    packageName = DOCS
    enabled = true
}

private fun PackageManager.disable(componentName: String) {
    setComponentEnabledSetting(ComponentName(DOCS, componentName), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
}

private fun aPackage(
    activities: List<ActivityInfo> = emptyList(),
    services: List<ServiceInfo> = emptyList(),
    receivers: List<ActivityInfo> = emptyList(),
    providers: List<ProviderInfo> = emptyList(),
) = PackageInfo().apply {
    packageName = DOCS
    applicationInfo = docsApplication
    this.activities = activities.toTypedArray()
    this.services = services.toTypedArray()
    this.receivers = receivers.toTypedArray()
    this.providers = providers.toTypedArray()
}

private fun anActivity(name: String, enabled: Boolean = true) = ActivityInfo().ofDocs(name, enabled)

private fun aService(name: String) = ServiceInfo().ofDocs(name, enabled = true)

private fun aProvider(name: String) = ProviderInfo().ofDocs(name, enabled = true).apply { authority = name }

private fun <T : ComponentInfo> T.ofDocs(name: String, enabled: Boolean): T = apply {
    this.name = name
    packageName = DOCS
    applicationInfo = docsApplication
    this.enabled = enabled
}
