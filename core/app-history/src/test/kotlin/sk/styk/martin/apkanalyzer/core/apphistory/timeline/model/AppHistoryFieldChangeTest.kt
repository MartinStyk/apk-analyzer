package sk.styk.martin.apkanalyzer.core.apphistory.timeline.model

import io.kotest.matchers.shouldBe
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.apps.components.Activity
import sk.styk.martin.apkanalyzer.core.apps.components.ContentProvider
import sk.styk.martin.apkanalyzer.core.apps.components.ProviderPathMatchType
import sk.styk.martin.apkanalyzer.core.apps.components.ProviderPathPermission
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraryFile
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.megabytes

class AppHistoryFieldChangeTest {

    @Test
    fun `Given an activity that became exported When listing its changes Then the field is named`() {
        val old = anActivity(isExported = false)
        val new = anActivity(isExported = true)

        val changes = new.fieldChangesFrom(old)

        changes shouldBe listOf(AppHistoryFieldChange.Flag(AppHistoryItemField.Exported, before = false, after = true))
    }

    @Test
    fun `Given a provider whose path rule was replaced When listing its changes Then both the removed and the added rule are kept`() {
        val oldRule = ProviderPathPermission(path = "/media", matchType = ProviderPathMatchType.Prefix, readPermission = null)
        val newRule = ProviderPathPermission(path = "/media", matchType = ProviderPathMatchType.Prefix, readPermission = "com.whatsapp.READ")

        val changes = aProvider(listOf(newRule)).fieldChangesFrom(aProvider(listOf(oldRule)))

        changes shouldBe listOf(
            AppHistoryFieldChange.PathRule(RuleDirection.Removed, oldRule),
            AppHistoryFieldChange.PathRule(RuleDirection.Added, newRule),
        )
    }

    @Test
    fun `Given two ABIs of a library that grew and shrank by the same amount When listing changes Then each is reported`() {
        val before = NativeLibrary("libfoo.so", listOf(aLibraryFile("arm64-v8a", 10.megabytes), aLibraryFile("x86_64", 12.megabytes)))
        val after = NativeLibrary("libfoo.so", listOf(aLibraryFile("arm64-v8a", 12.megabytes), aLibraryFile("x86_64", 10.megabytes)))

        val changes = after.fieldChangesFrom(before)

        changes shouldBe listOf(
            AppHistoryFieldChange.SizeValue("arm64-v8a", ValueChange(10.megabytes, 12.megabytes)),
            AppHistoryFieldChange.SizeValue("x86_64", ValueChange(12.megabytes, 10.megabytes)),
        )
    }
}

private fun anActivity(isExported: Boolean) = Activity(name = "com.whatsapp.Main", packageName = PackageName("com.whatsapp"), isExported = isExported)

private fun aProvider(pathPermissions: List<ProviderPathPermission>) = ContentProvider(
    name = "com.whatsapp.Provider",
    authority = "com.whatsapp.provider",
    pathPermissions = pathPermissions,
)

private fun aLibraryFile(abi: String, size: AppSize) = NativeLibraryFile(name = "libfoo.so", abi = abi, size = size, containingApkFileName = "base.apk")
