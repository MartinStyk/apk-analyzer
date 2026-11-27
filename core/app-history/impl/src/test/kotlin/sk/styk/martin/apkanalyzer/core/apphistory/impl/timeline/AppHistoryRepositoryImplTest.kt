package sk.styk.martin.apkanalyzer.core.apphistory.impl.timeline

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryDeviceTimeline
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryEntry
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryInstallSource
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistorySection
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistorySnapshotId
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistorySnapshotRead
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryTimeline
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.ValueChange
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.changeCount
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.AppHistoryCaptureKey
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.AppHistoryPersistenceRepositoryFake
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.AppHistoryRecord
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.CapturedSection
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.StoredAppHistoryRecord
import sk.styk.martin.apkanalyzer.core.apps.api.installsource.InstallSourceChain
import sk.styk.martin.apkanalyzer.core.apps.api.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.api.model.Detail.WhatsApp
import sk.styk.martin.apkanalyzer.core.apps.api.packaging.NativeLibraries
import sk.styk.martin.apkanalyzer.core.apps.api.signing.SigningSchemeVersion
import sk.styk.martin.apkanalyzer.core.common.coroutines.TestDispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTrackerFake
import java.io.IOException
import java.time.Instant

class AppHistoryRepositoryImplTest {

    @get:Rule
    val dispatcherProvider = TestDispatcherProvider()

    private val persistence = AppHistoryPersistenceRepositoryFake()
    private val repository = AppHistoryRepositoryImpl(persistence, AppHistoryTimelineBuilder(AppHistoryDiffer()), PerformanceTrackerFake(), dispatcherProvider)

    @Test
    fun `Given no such snapshot When reading it Then the outcome is not found rather than a read failure`() = runTest {
        val read = repository.snapshot(AppHistorySnapshotId(7))

        read shouldBe AppHistorySnapshotRead.NotFound
    }

    @Test
    fun `Given a failing store When reading a snapshot Then the outcome is unavailable`() = runTest {
        persistence.readFailure = IOException("disk")

        val read = repository.snapshot(AppHistorySnapshotId(7))

        read shouldBe AppHistorySnapshotRead.Unavailable
    }

    @Test
    fun `Given a native library section that failed capture When reading the snapshot Then it stays not recorded`() = runTest {
        persistence += aStoredRecord(id = 7, nativeLibraries = CapturedSection.Unavailable)

        val read = repository.snapshot(AppHistorySnapshotId(7)) as AppHistorySnapshotRead.Found

        read.snapshot.nativeLibraries shouldBe AppHistorySection.NotRecorded
    }

    @Test
    fun `Given a captured unknown signing scheme When reading the snapshot Then it is recorded as unknown`() = runTest {
        persistence += aStoredRecord(id = 7, signingScheme = CapturedSection.Captured(null))

        val read = repository.snapshot(AppHistorySnapshotId(7)) as AppHistorySnapshotRead.Found

        read.snapshot.signingSchemes shouldBe AppHistorySection.Recorded(null)
    }

    @Test
    fun `Given a re-delivery where only the initiating package differs When reading the timeline Then no change appears`() = runTest {
        persistence += aStoredRecord(id = 1, detail = aDetail(InstallSourceChain(GooglePlay, PackageName("com.other.first"), null)))
        persistence += aStoredRecord(id = 2, detail = aDetail(InstallSourceChain(GooglePlay, PackageName("com.other.second"), null)))

        val timeline = repository.timeline(WhatsApp.info.packageName).first() as AppHistoryTimeline.Available

        timeline.entries.changeCount shouldBe 0
    }

    @Test
    fun `Given a move between two unrecognized stores with nothing else When reading the timeline Then the change stays visible`() = runTest {
        val first = PackageName("com.apkpure.aegon")
        val second = PackageName("com.other.store")
        persistence += aStoredRecord(id = 1, detail = aDetail(InstallSourceChain(first, null, null), source = AppSource.Unknown))
        persistence += aStoredRecord(id = 2, detail = aDetail(InstallSourceChain(second, null, null), source = AppSource.Unknown))

        val timeline = repository.timeline(WhatsApp.info.packageName).first() as AppHistoryTimeline.Available

        timeline.entries.changeCount shouldBe 1
        (timeline.entries.first() as AppHistoryEntry.Change).diff.installSource shouldBe ValueChange(
            AppHistoryInstallSource(AppSource.Unknown, first),
            AppHistoryInstallSource(AppSource.Unknown, second),
        )
    }

    @Test
    fun `Given the resolved source changes with the same installer When reading the timeline Then the change is visible`() = runTest {
        persistence += aStoredRecord(id = 1, detail = aDetail(InstallSourceChain(null, null, null), source = AppSource.Unknown))
        persistence += aStoredRecord(id = 2, detail = aDetail(InstallSourceChain(null, null, null), source = AppSource.Sideloaded))

        val timeline = repository.timeline(WhatsApp.info.packageName).first() as AppHistoryTimeline.Available

        (timeline.entries.first() as AppHistoryEntry.Change).diff.installSource shouldBe ValueChange(
            AppHistoryInstallSource(AppSource.Unknown, null),
            AppHistoryInstallSource(AppSource.Sideloaded, null),
        )
    }

    @Test
    fun `Given an app recorded once When reading the device timeline Then it is first seen without decoding its history`() = runTest {
        persistence += aStoredRecord(id = 1)

        val device = repository.deviceTimeline().first() as AppHistoryDeviceTimeline.Available

        device.apps.single().entries.single().shouldBeInstanceOf<AppHistoryEntry.FirstSeen>()
        persistence.recordReads shouldBe emptyList()
    }

    @Test
    fun `Given an app with updates When reading the device timeline Then its entries match its own timeline`() = runTest {
        persistence += aStoredRecord(id = 1, detail = aVersion(1), capturedAt = 1_000)
        persistence += aStoredRecord(id = 2, detail = aVersion(2), capturedAt = 2_000)

        val device = repository.deviceTimeline().first() as AppHistoryDeviceTimeline.Available
        val own = repository.timeline(WhatsApp.info.packageName).first() as AppHistoryTimeline.Available

        device.apps.single().entries shouldBe own.entries
        device.apps.single().diffsToLatest.keys shouldBe setOf(AppHistorySnapshotId(1))
    }

    @Test
    fun `Given an update that changed nothing visible When reading the device timeline Then it is kept as an unchanged update`() = runTest {
        persistence += aStoredRecord(id = 1, capturedAt = 1_000, lastUpdateTime = 1_000)
        persistence += aStoredRecord(id = 2, capturedAt = 2_000, lastUpdateTime = 2_000)

        val app = (repository.deviceTimeline().first() as AppHistoryDeviceTimeline.Available).apps.single()

        app.entries.changeCount shouldBe 0
        app.unchangedUpdates shouldBe listOf(Instant.ofEpochMilli(2_000))
    }

    @Test
    fun `Given another app was captured When reading the device timeline again Then unchanged apps are not decoded again`() = runTest {
        persistence += aStoredRecord(id = 1, capturedAt = 1_000)
        persistence += aStoredRecord(id = 2, detail = aVersion(2), capturedAt = 2_000)
        repository.deviceTimeline().first()

        persistence += aStoredRecord(id = 3, packageName = Signal, capturedAt = 3_000)
        persistence += aStoredRecord(id = 4, packageName = Signal, detail = aVersion(2), capturedAt = 4_000)
        repository.deviceTimeline().first()

        persistence.recordReads shouldBe listOf(WhatsApp.info.packageName, Signal)
    }

    @Test
    fun `Given a failing store When reading the device timeline Then it is unavailable`() = runTest {
        persistence += aStoredRecord(id = 1)
        persistence.headersFailure = IOException("disk")

        val device = repository.deviceTimeline().first()

        device shouldBe AppHistoryDeviceTimeline.Unavailable
    }
}

private val GooglePlay = PackageName("com.android.vending")
private val Signal = PackageName("org.thoughtcrime.securesms")

private fun aVersion(versionCode: Long) = WhatsApp.copy(info = WhatsApp.info.copy(versionCode = versionCode, versionName = "$versionCode.0"))

private fun aDetail(chain: InstallSourceChain, source: AppSource = AppSource.GooglePlay) =
    WhatsApp.copy(info = WhatsApp.info.copy(installSourceChain = chain, source = source))

private fun aStoredRecord(
    id: Long,
    detail: AppDetail = WhatsApp,
    packageName: PackageName = WhatsApp.info.packageName,
    capturedAt: Long = 1_000,
    lastUpdateTime: Long = 1_000,
    nativeLibraries: CapturedSection<NativeLibraries> = CapturedSection.Captured(NativeLibraries.Empty),
    signingScheme: CapturedSection<List<SigningSchemeVersion>?> = CapturedSection.Captured(emptyList()),
) = StoredAppHistoryRecord(
    id = id,
    record = AppHistoryRecord(
        packageName = packageName,
        capturedAt = Instant.ofEpochMilli(capturedAt),
        captureKey = AppHistoryCaptureKey(
            deviceId = "device",
            firstInstallTime = Instant.ofEpochMilli(1_000),
            lastUpdateTime = Instant.ofEpochMilli(lastUpdateTime),
            versionCode = detail.info.versionCode,
        ),
        detail = detail,
        nativeLibraries = nativeLibraries,
        signingScheme = signingScheme,
    ),
)
