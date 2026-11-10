package sk.styk.martin.apkanalyzer.core.apphistory.timeline

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.apphistory.storage.AppHistoryCaptureKey
import sk.styk.martin.apkanalyzer.core.apphistory.storage.AppHistoryPersistenceRepositoryFake
import sk.styk.martin.apkanalyzer.core.apphistory.storage.AppHistoryRecord
import sk.styk.martin.apkanalyzer.core.apphistory.storage.CapturedSection
import sk.styk.martin.apkanalyzer.core.apphistory.storage.StoredAppHistoryRecord
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryEntry
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryInstallSource
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistorySection
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistorySnapshotId
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistorySnapshotRead
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryTimeline
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.ValueChange
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.changeCount
import sk.styk.martin.apkanalyzer.core.apps.installsource.InstallSourceChain
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.model.Detail.WhatsApp
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraries
import sk.styk.martin.apkanalyzer.core.apps.signing.SigningSchemeVersion
import sk.styk.martin.apkanalyzer.core.common.coroutines.TestDispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import java.io.IOException
import java.time.Instant

class AppHistoryRepositoryImplTest {

    @get:Rule
    val dispatcherProvider = TestDispatcherProvider()

    private val persistence = AppHistoryPersistenceRepositoryFake()
    private val repository = AppHistoryRepositoryImpl(persistence, AppHistoryDiffer(), dispatcherProvider)

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
}

private val GooglePlay = PackageName("com.android.vending")

private fun aDetail(chain: InstallSourceChain, source: AppSource = AppSource.GooglePlay) =
    WhatsApp.copy(info = WhatsApp.info.copy(installSourceChain = chain, source = source))

private fun aStoredRecord(
    id: Long,
    detail: AppDetail = WhatsApp,
    nativeLibraries: CapturedSection<NativeLibraries> = CapturedSection.Captured(NativeLibraries.Empty),
    signingScheme: CapturedSection<List<SigningSchemeVersion>?> = CapturedSection.Captured(emptyList()),
) = StoredAppHistoryRecord(
    id = id,
    record = AppHistoryRecord(
        packageName = WhatsApp.info.packageName,
        capturedAt = Instant.ofEpochMilli(1_000),
        captureKey = AppHistoryCaptureKey(
            deviceId = "device",
            firstInstallTime = Instant.ofEpochMilli(1_000),
            lastUpdateTime = Instant.ofEpochMilli(1_000),
            versionCode = 1,
        ),
        detail = detail,
        nativeLibraries = nativeLibraries,
        signingScheme = signingScheme,
    ),
)
