package sk.styk.martin.apkanalyzer.core.appfunctions.usecase

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.appfunctions.model.AppDetailResult
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepositoryFake
import sk.styk.martin.apkanalyzer.core.apps.model.Detail.WhatsApp
import sk.styk.martin.apkanalyzer.core.apps.model.packageName
import sk.styk.martin.apkanalyzer.core.apps.permissions.Permissions
import sk.styk.martin.apkanalyzer.core.apps.signing.AppSigning
import sk.styk.martin.apkanalyzer.core.common.coroutines.TestDispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.model.AppSource

class GetAppDetailUseCaseTest {

    @get:Rule
    val dispatcherProvider = TestDispatcherProvider()

    private val appDetailRepository = AppDetailRepositoryFake(WhatsApp)
    private val useCase = GetAppDetailUseCase(appDetailRepository, dispatcherProvider)

    @Test
    fun `Given an installed app When getting details Then result contains each value in its own field`() = runTest {
        appDetailRepository += WhatsApp.copy(
            info = WhatsApp.info.copy(
                applicationName = "Messenger",
                versionName = "1.2.3",
                versionCode = 123,
                targetSdkLabel = "Target label",
                minSdkLabel = "Min label",
                source = AppSource.FDroid,
                isSystemApp = true,
            ),
            signing = AppSigning(hasMultipleSigners = true),
            permissions = Permissions(defined = emptyList(), used = WhatsApp.permissions.used.take(1)),
        )

        val detail = useCase(WhatsApp.packageName)

        detail shouldBe AppDetailResult(
            packageName = "com.whatsapp",
            applicationName = "Messenger",
            versionName = "1.2.3",
            versionCode = 123,
            targetSdkLabel = "Target label",
            minSdkLabel = "Min label",
            installSourceLabel = "F-Droid",
            isSystemApp = true,
            requestedPermissionCount = 1,
            hasMultipleSigners = true,
        )
    }
}
