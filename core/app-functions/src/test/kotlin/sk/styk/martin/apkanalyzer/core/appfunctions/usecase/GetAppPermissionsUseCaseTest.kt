package sk.styk.martin.apkanalyzer.core.appfunctions.usecase

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.appfunctions.model.PermissionGrantSummary
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepositoryFake
import sk.styk.martin.apkanalyzer.core.apps.model.Detail.WhatsApp
import sk.styk.martin.apkanalyzer.core.apps.model.packageName
import sk.styk.martin.apkanalyzer.core.apps.permissions.Permission
import sk.styk.martin.apkanalyzer.core.apps.permissions.PermissionDetails
import sk.styk.martin.apkanalyzer.core.apps.permissions.Permissions
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.apps.permissions.UsedPermission
import sk.styk.martin.apkanalyzer.core.common.coroutines.TestDispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

class GetAppPermissionsUseCaseTest {

    @get:Rule
    val dispatcherProvider = TestDispatcherProvider()

    private val appDetailRepository = AppDetailRepositoryFake(WhatsApp)
    private val useCase = GetAppPermissionsUseCase(appDetailRepository, dispatcherProvider)

    @Test
    fun `Given permissions of every kind When getting permissions Then only dangerous ones are returned with their grant state`() = runTest {
        val used = listOf(
            aUsedPermission("granted.dangerous", ProtectionLevel.Dangerous, isGranted = true),
            aUsedPermission("denied.dangerous", ProtectionLevel.Dangerous, isGranted = false),
            aUsedPermission("normal", ProtectionLevel.Normal, isGranted = true),
            UsedPermission(Permission(name = "unknown", details = null), isGranted = true),
        )
        appDetailRepository += WhatsApp.copy(permissions = Permissions(defined = emptyList(), used = used))

        val permissions = useCase(WhatsApp.packageName)

        permissions shouldBe listOf(
            PermissionGrantSummary(permission = "granted.dangerous", isGranted = true),
            PermissionGrantSummary(permission = "denied.dangerous", isGranted = false),
        )
    }
}

private fun aUsedPermission(
    name: String,
    protectionLevel: ProtectionLevel,
    isGranted: Boolean,
) = UsedPermission(
    permissionData = Permission(
        name = name,
        details = PermissionDetails(
            groupName = null,
            protectionLevel = protectionLevel,
            protectionFlags = emptySet(),
            description = null,
            declaringPackage = PackageName("android"),
        ),
    ),
    isGranted = isGranted,
)
