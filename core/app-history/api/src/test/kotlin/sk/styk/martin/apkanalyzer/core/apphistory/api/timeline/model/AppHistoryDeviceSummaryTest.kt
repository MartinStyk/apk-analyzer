package sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model

import io.kotest.matchers.shouldBe
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.apps.api.permissions.Permission
import sk.styk.martin.apkanalyzer.core.apps.api.permissions.PermissionDetails
import sk.styk.martin.apkanalyzer.core.apps.api.permissions.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.apps.api.permissions.UsedPermission
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import java.time.Instant

class AppHistoryDeviceSummaryTest {

    @Test
    fun `Given updates inside and outside the range When summarizing Then only updates in range count, unchanged ones included`() {
        val updated = anApp(
            "com.updated",
            listOf(aChange(id = 2, previous = 1, day = 5, diff = aVersionDiff())),
            unchangedUpdates = listOf(day(6), day(1)),
        )
        val installed = anApp("com.installed", installedOn = day(4))
        val old = anApp("com.old", listOf(aChange(id = 2, previous = 1, day = 1, diff = aVersionDiff())))

        val summary = AppHistoryDeviceTimeline.Available(listOf(updated, installed, old)).summarySince(day(3))

        summary.updateCount shouldBe 2
        summary.newAppCount shouldBe 1
        summary.changedApps.map { it.packageName.value } shouldBe listOf("com.updated", "com.installed")
        summary.changedApps.first().latestAt shouldBe day(6)
    }

    @Test
    fun `Given a permission added and removed again in range When summarizing Then the row has no net change and attention marks it reverted`() {
        val camera = aDangerousPermission("android.permission.CAMERA")
        val app = anApp(
            "com.camera",
            listOf(
                aChange(
                    id = 3,
                    previous = 2,
                    day = 6,
                    diff = aVersionDiff().copy(requestedPermissions = SetChange.Changed(emptyList(), listOf(camera), emptyList())),
                ),
                aChange(
                    id = 2,
                    previous = 1,
                    day = 5,
                    diff = aVersionDiff().copy(requestedPermissions = SetChange.Changed(listOf(camera), emptyList(), emptyList())),
                ),
            ),
            diffsToLatest = mapOf(AppHistorySnapshotId(1) to aVersionDiff(), AppHistorySnapshotId(2) to aVersionDiff()),
        )

        val summary = AppHistoryDeviceTimeline.Available(listOf(app)).summarySince(day(3))

        summary.changedApps.single().updateCount shouldBe 2
        summary.changedApps.single().netChange?.requestedPermissions shouldBe SetChange.Unchanged
        summary.needsAttention.single().change.change.snapshot.id shouldBe AppHistorySnapshotId(2)
        summary.needsAttention.single().revertedAt shouldBe day(6)
    }

    @Test
    fun `Given a certificate change When summarizing Then it needs attention and is never marked reverted`() {
        val app =
            anApp(
                "com.signed",
                listOf(
                    aChange(id = 2, previous = 1, day = 5, diff = aVersionDiff().copy(certificates = SetChange.Changed(emptyList(), emptyList(), emptyList()))),
                ),
            )

        val summary = AppHistoryDeviceTimeline.Available(listOf(app)).summarySince(day(3))

        summary.needsAttention.single().revertedAt shouldBe null
    }

    @Test
    fun `Given a newer move to another device When reading the latest change Then the newest app change wins`() {
        val moved = anApp("com.moved", listOf(aChange(id = 2, previous = 1, day = 9, diff = aVersionDiff(), kind = AppHistoryEntry.Change.Kind.NewDevice)))
        val updated = anApp("com.updated", listOf(aChange(id = 2, previous = 1, day = 5, diff = aVersionDiff())))

        val latest = AppHistoryDeviceTimeline.Available(listOf(moved, updated)).latestChange

        latest?.packageName?.value shouldBe "com.updated"
    }
}

private fun day(day: Int): Instant = Instant.ofEpochSecond(day * 86_400L)

private fun aSummary(
    id: Long,
    at: Instant,
    installedOn: Instant = day(0),
) = AppHistorySnapshotSummary(
    id = AppHistorySnapshotId(id),
    applicationName = "App",
    version = AppHistoryVersion(id, "$id.0"),
    firstInstallTime = installedOn,
    lastUpdateTime = at,
    capturedAt = at,
)

private fun aChange(
    id: Long,
    previous: Long,
    day: Int,
    diff: AppHistoryDiff,
    kind: AppHistoryEntry.Change.Kind = AppHistoryEntry.Change.Kind.Updated,
) = AppHistoryEntry.Change(kind = kind, snapshot = aSummary(id, day(day)), previous = aSummary(previous, day(day - 1)), diff = diff)

private fun anApp(
    packageName: String,
    changes: List<AppHistoryEntry.Change> = emptyList(),
    installedOn: Instant = day(0),
    unchangedUpdates: List<Instant> = emptyList(),
    diffsToLatest: Map<AppHistorySnapshotId, AppHistoryDiff> = emptyMap(),
) = AppHistoryAppTimeline(
    packageName = PackageName(packageName),
    applicationName = packageName,
    entries = changes + AppHistoryEntry.FirstSeen(aSummary(id = 1, at = installedOn, installedOn = installedOn)),
    unchangedUpdates = unchangedUpdates,
    diffsToLatest = diffsToLatest,
)

private fun aDangerousPermission(name: String) = UsedPermission(
    permissionData = Permission(
        name = name,
        details = PermissionDetails(
            groupName = null,
            protectionLevel = ProtectionLevel.Dangerous,
            protectionFlags = emptySet(),
            description = null,
            declaringPackage = PackageName("android"),
        ),
    ),
    isGranted = false,
)

private fun aVersionDiff() = AppHistoryDiff(
    version = ValueChange(AppHistoryVersion(1, "1.0"), AppHistoryVersion(2, "2.0")),
    minSdkVersion = null,
    targetSdkVersion = null,
    apkSize = null,
    installSource = null,
    isDebuggable = null,
    allowsBackup = null,
    usesCleartextTraffic = null,
    requestedPermissions = SetChange.Unchanged,
    definedPermissions = SetChange.Unchanged,
    activities = SetChange.Unchanged,
    services = SetChange.Unchanged,
    receivers = SetChange.Unchanged,
    providers = SetChange.Unchanged,
    certificates = SetChange.Unchanged,
    signingSchemes = SetChange.Unchanged,
    nativeLibraries = SetChange.Unchanged,
    requirements = SetChange.Unchanged,
)
