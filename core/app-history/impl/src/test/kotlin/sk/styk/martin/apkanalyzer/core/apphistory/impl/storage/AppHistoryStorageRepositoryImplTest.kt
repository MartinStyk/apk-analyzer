package sk.styk.martin.apkanalyzer.core.apphistory.impl.storage

import android.content.Context
import android.net.Uri
import androidx.room.Room
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import sk.styk.martin.apkanalyzer.core.apphistory.api.storage.AppHistoryImport
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.database.AppHistoryDatabase
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.database.entity.AppHistoryBlobEntity
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.database.entity.AppHistoryDeviceEntity
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.database.entity.AppHistorySnapshotEntity
import sk.styk.martin.apkanalyzer.core.common.coroutines.TestDispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.file.DocumentFileManagerFake
import java.io.File

@RunWith(RobolectricTestRunner::class)
class AppHistoryStorageRepositoryImplTest {

    @get:Rule
    val dispatcherProvider = TestDispatcherProvider()

    private val context: Context = RuntimeEnvironment.getApplication()
    private val documents = DocumentFileManagerFake()
    private val exportedFile = Uri.parse("content://com.android.externalstorage.documents/document/history.db")
    private val oldPhone = Room.databaseBuilder(context, AppHistoryDatabase::class.java, "old_phone.db").build()
    private val newPhone = Room.databaseBuilder(context, AppHistoryDatabase::class.java, "app_history_v2.db").build()
    private val oldPhoneStorage =
        AppHistoryStorageRepositoryImpl(context, oldPhone.appHistoryReadDao(), oldPhone.appHistoryWriteDao(), documents, dispatcherProvider)
    private val newPhoneStorage =
        AppHistoryStorageRepositoryImpl(context, newPhone.appHistoryReadDao(), newPhone.appHistoryWriteDao(), documents, dispatcherProvider)

    @Before
    fun setUp() {
        context.getDatabasePath("app_history_v2.db").parentFile!!.mkdirs()
    }

    @After
    fun tearDown() {
        oldPhone.close()
        newPhone.close()
        context.deleteDatabase("old_phone.db")
        context.deleteDatabase("app_history_v2.db")
    }

    @Test
    fun `Given history When moved Then all rows arrive`() = runTest {
        oldPhone.appHistoryWriteDao().insertSnapshotWithBlobs(Pixel, WhatsAppV1, listOf(aBlob("com.whatsapp", hash = 11, content = "whatsapp permissions")))
        oldPhone.appHistoryWriteDao().insertSnapshotWithBlobs(
            Pixel,
            SignalV1,
            listOf(aBlob("org.thoughtcrime.securesms", hash = 21, content = "signal permissions")),
        )
        oldPhoneStorage.export(exportedFile).getOrThrow()

        val imported = newPhoneStorage.import(exportedFile).getOrThrow()

        imported shouldBe AppHistoryImport(importedAppCount = 2)
        newPhone.appHistoryReadDao().allSnapshots().map { it.copy(id = 0) } shouldContainExactlyInAnyOrder listOf(WhatsAppV1, SignalV1)
        newPhone.appHistoryReadDao().allBlobs().map { Triple(it.packageName, it.hash, it.content.decodeToString()) } shouldContainExactlyInAnyOrder listOf(
            Triple("com.whatsapp", 11L, "whatsapp permissions"),
            Triple("org.thoughtcrime.securesms", 21L, "signal permissions"),
        )
        newPhone.appHistoryReadDao().allDevices() shouldBe listOf(Pixel)
    }

    @Test
    fun `Given known rows When reimported Then none are added`() = runTest {
        oldPhone.appHistoryWriteDao().insertSnapshotWithBlobs(Pixel, WhatsAppV1, listOf(aBlob("com.whatsapp", hash = 11, content = "whatsapp permissions")))
        oldPhoneStorage.export(exportedFile).getOrThrow()
        newPhoneStorage.import(exportedFile).getOrThrow()

        val importedAgain = newPhoneStorage.import(exportedFile).getOrThrow()

        importedAgain shouldBe AppHistoryImport(importedAppCount = 0)
        newPhone.appHistoryReadDao().allSnapshots().size shouldBe 1
    }

    @Test
    fun `Given a CSV file When importing Then it fails`() = runTest {
        documents[exportedFile] = "name,version\nWhatsApp,2.24".toByteArray()

        val result = newPhoneStorage.import(exportedFile)

        result.isFailure shouldBe true
        newPhone.appHistoryReadDao().allSnapshots() shouldBe emptyList()
        File(context.noBackupFilesDir, "app_history_v2_import.db").exists() shouldBe false
    }
}

private val Pixel = AppHistoryDeviceEntity(deviceId = "pixel-8", firstSeenAt = 1_700_000_000_000, manufacturer = "Google", model = "Pixel 8")

private val WhatsAppV1 = aSnapshot(packageName = "com.whatsapp", applicationName = "WhatsApp", permissionsHash = 11)

private val SignalV1 = aSnapshot(packageName = "org.thoughtcrime.securesms", applicationName = "Signal", permissionsHash = 21)

private fun aBlob(
    packageName: String,
    hash: Long,
    content: String,
) = AppHistoryBlobEntity(packageName = packageName, hash = hash, content = content.toByteArray())

private fun aSnapshot(
    packageName: String,
    applicationName: String,
    permissionsHash: Long,
) = AppHistorySnapshotEntity(
    packageName = packageName,
    deviceId = "pixel-8",
    capturedAt = 1_700_000_100_000,
    blobFormatVersion = 1,
    firstInstallTime = 1_690_000_000_000,
    lastUpdateTime = 1_699_000_000_000,
    applicationName = applicationName,
    processName = packageName,
    versionCode = 1,
    versionName = "1.0",
    isSystemApp = false,
    isDebuggable = false,
    allowsBackup = true,
    usesCleartextTraffic = false,
    uid = 10_123,
    sharedUserId = null,
    description = null,
    installLocation = "auto",
    installingPackage = "com.android.vending",
    initiatingPackage = "com.android.vending",
    originatingPackage = null,
    apkSize = 52_428_800,
    targetSdkVersion = 34,
    minSdkVersion = 26,
    permissionsHash = permissionsHash,
    activitiesHash = null,
    servicesHash = null,
    receiversHash = null,
    providersHash = null,
    featuresHash = null,
    signingHash = null,
    nativeLibrariesHash = null,
    signingSchemeHash = null,
    installedSplitsHash = null,
)
