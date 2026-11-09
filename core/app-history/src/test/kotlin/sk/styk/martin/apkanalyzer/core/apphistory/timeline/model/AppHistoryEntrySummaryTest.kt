package sk.styk.martin.apkanalyzer.core.apphistory.timeline.model

import io.kotest.matchers.shouldBe
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.Feature
import sk.styk.martin.apkanalyzer.core.apps.permissions.Permission
import sk.styk.martin.apkanalyzer.core.apps.permissions.PermissionDetails
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.apps.permissions.UsedPermission
import sk.styk.martin.apkanalyzer.core.apps.signing.Certificate
import sk.styk.martin.apkanalyzer.core.apps.signing.SigningSchemeVersion
import sk.styk.martin.apkanalyzer.core.apps.signing.WhatsAppCertificate
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import java.time.Instant

class AppHistoryEntrySummaryTest {

    @Test
    fun `Given only a signing scheme change When summarizing Then it is not a certificate change`() {
        val change = aChange(
            aDiff(signingSchemes = SetChange.Changed(added = listOf(SigningSchemeVersion.V3), removed = emptyList(), modified = emptyList())),
        )

        val parts = change.summaryParts { it }

        change.diff.hasCertificateChange shouldBe false
        parts shouldBe listOf(AppHistorySummaryPart.SigningMethodChanged)
    }

    @Test
    fun `Given a certificate change When summarizing Then the headline owns it and the summary omits signing`() {
        val certificates = SetChange.Changed<Certificate>(added = listOf(WhatsAppCertificate), removed = emptyList(), modified = emptyList())
        val schemes = SetChange.Changed(added = listOf(SigningSchemeVersion.V3), removed = emptyList(), modified = emptyList())
        val change = aChange(aDiff(certificates = certificates, signingSchemes = schemes))

        val parts = change.summaryParts { it }

        change.diff.hasCertificateChange shouldBe true
        parts shouldBe emptyList()
    }

    @Test
    fun `Given sections that could not be compared When summarizing Then nothing reads as removed`() {
        val change = aChange(aDiff(signingSchemes = SetChange.Uncomparable, nativeLibraries = SetChange.Uncomparable))

        val parts = change.summaryParts { it }

        parts shouldBe emptyList()
        change.hasOnlyUncomparableDetails shouldBe true
        change.isQuiet shouldBe false
    }

    @Test
    fun `Given a version bump with an unknown scheme When summarizing Then the row is quiet and shows no uncomparable mark`() {
        val change = aChange(
            aDiff(version = ValueChange(AppHistoryVersion(1, "1.0"), AppHistoryVersion(2, "2.0")), signingSchemes = SetChange.Uncomparable),
        )

        change.hasOnlyUncomparableDetails shouldBe false
        change.isQuiet shouldBe true
    }

    @Test
    fun `Given a size change and a flag change When summarizing Then the row is not quiet and size comes last`() {
        val change = aChange(
            aDiff(
                apkSize = ValueChange(10.megabytes, 12.megabytes),
                isDebuggable = ValueChange(before = false, after = true),
            ),
        )

        val parts = change.summaryParts { it }

        change.isQuiet shouldBe false
        parts shouldBe listOf(
            AppHistorySummaryPart.DebuggingChanged(isEnabled = true),
            AppHistorySummaryPart.SizeChanged(10.megabytes, 12.megabytes),
        )
    }

    @Test
    fun `Given flags and permissions changed When summarizing Then permissions come right after debugging`() {
        val added = aRequestedPermission("android.permission.CAMERA", ProtectionLevel.Dangerous)
        val change = aChange(
            aDiff(
                isDebuggable = ValueChange(before = false, after = true),
                usesCleartextTraffic = ValueChange(before = false, after = true),
                requestedPermissions = SetChange.Changed(added = listOf(added), removed = emptyList(), modified = emptyList()),
            ),
        )

        val parts = change.summaryParts { it }

        parts shouldBe listOf(
            AppHistorySummaryPart.DebuggingChanged(isEnabled = true),
            AppHistorySummaryPart.PermissionsAdded(firstLabel = "android.permission.CAMERA", otherCount = 0),
            AppHistorySummaryPart.UnencryptedTrafficChanged(isAllowed = true),
        )
    }

    @Test
    fun `Given a normal and a dangerous permission added When summarizing Then the dangerous one is named`() {
        val change = aChange(
            aDiff(
                requestedPermissions = SetChange.Changed(
                    added = listOf(
                        aRequestedPermission("android.permission.INTERNET", ProtectionLevel.Normal),
                        aRequestedPermission("android.permission.CAMERA", ProtectionLevel.Dangerous),
                    ),
                    removed = emptyList(),
                    modified = emptyList(),
                ),
            ),
        )

        val parts = change.summaryParts { it }

        parts shouldBe listOf(AppHistorySummaryPart.PermissionsAdded(firstLabel = "android.permission.CAMERA", otherCount = 1))
    }

    @Test
    fun `Given a system app first recorded long after its factory date When reading the overview Then the date is when it was recorded`() {
        val entries = listOf(AppHistoryEntry.FirstSeen(aSummary(lastUpdateTime = FactoryDate, capturedAt = RecordedDate)))

        val overview = entries.overview

        overview shouldBe AppHistoryOverview.FirstRecordedOnly(RecordedDate)
    }

    @Test
    fun `Given an older certificate change and a newer update When reading the overview Then the certificate change wins`() {
        val certificates = SetChange.Changed<Certificate>(added = listOf(WhatsAppCertificate), removed = emptyList(), modified = emptyList())
        val entries = listOf(
            aChange(aDiff(apkSize = ValueChange(10.megabytes, 12.megabytes)), lastUpdateTime = Instant.ofEpochMilli(3_000)),
            aChange(aDiff(certificates = certificates), lastUpdateTime = Instant.ofEpochMilli(2_000)),
            AppHistoryEntry.FirstSeen(aSummary()),
        )

        val overview = entries.overview

        overview shouldBe AppHistoryOverview.CertificateChanged(Instant.ofEpochMilli(2_000))
        entries.changeCount shouldBe 2
    }
}

private val FactoryDate = Instant.ofEpochMilli(1_230_768_000_000)
private val RecordedDate = Instant.ofEpochMilli(1_760_227_200_000)

private fun aRequestedPermission(name: String, level: ProtectionLevel) = UsedPermission(
    permissionData = Permission(
        name = name,
        details = PermissionDetails(
            groupName = null,
            protectionLevel = level,
            protectionFlags = emptySet(),
            description = null,
            declaringPackage = PackageName("android"),
        ),
    ),
    isGranted = false,
)

private fun aSummary(lastUpdateTime: Instant = Instant.ofEpochMilli(1_000), capturedAt: Instant = Instant.ofEpochMilli(1_000)) = AppHistorySnapshotSummary(
    id = AppHistorySnapshotId(1),
    applicationName = "WhatsApp",
    version = AppHistoryVersion(1, "1.0"),
    firstInstallTime = Instant.ofEpochMilli(1_000),
    lastUpdateTime = lastUpdateTime,
    capturedAt = capturedAt,
)

private fun aChange(diff: AppHistoryDiff, lastUpdateTime: Instant = Instant.ofEpochMilli(2_000)) = AppHistoryEntry.Change(
    kind = AppHistoryEntry.Change.Kind.Updated,
    snapshot = aSummary(lastUpdateTime = lastUpdateTime),
    previous = aSummary(),
    diff = diff,
)

private fun aDiff(
    version: ValueChange<AppHistoryVersion>? = null,
    apkSize: ValueChange<AppSize>? = null,
    isDebuggable: ValueChange<Boolean>? = null,
    usesCleartextTraffic: ValueChange<Boolean>? = null,
    requestedPermissions: SetChange<UsedPermission> = SetChange.Unchanged,
    certificates: SetChange<Certificate> = SetChange.Unchanged,
    signingSchemes: SetChange<SigningSchemeVersion> = SetChange.Unchanged,
    nativeLibraries: SetChange<NativeLibrary> = SetChange.Unchanged,
    requirements: SetChange<Feature> = SetChange.Unchanged,
) = AppHistoryDiff(
    version = version,
    minSdkVersion = null,
    targetSdkVersion = null,
    apkSize = apkSize,
    installSource = null,
    isDebuggable = isDebuggable,
    allowsBackup = null,
    usesCleartextTraffic = usesCleartextTraffic,
    requestedPermissions = requestedPermissions,
    definedPermissions = SetChange.Unchanged,
    activities = SetChange.Unchanged,
    services = SetChange.Unchanged,
    receivers = SetChange.Unchanged,
    providers = SetChange.Unchanged,
    certificates = certificates,
    signingSchemes = signingSchemes,
    nativeLibraries = nativeLibraries,
    requirements = requirements,
)
