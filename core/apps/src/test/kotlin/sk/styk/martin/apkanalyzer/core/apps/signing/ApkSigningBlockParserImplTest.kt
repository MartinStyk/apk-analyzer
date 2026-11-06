package sk.styk.martin.apkanalyzer.core.apps.signing

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val V2_ID = 0x7109871aL
private const val V3_ID = 0xf05368c0L
private const val V3_1_ID = 0x1b93ad61L
private const val UNKNOWN_ID = 0x42726577L

class ApkSigningBlockParserImplTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val parser: ApkSigningBlockParser = ApkSigningBlockParserImpl()

    @Test
    fun `Given a v2 block When parsing Then scheme is v2`() {
        val apk = anApkFile(anApk(aSigningBlock(aPair(V2_ID))))

        val versions = parser.parseSchemeVersions(apk)

        versions shouldBe setOf(SigningSchemeVersion.V2)
    }

    @Test
    fun `Given a v3 block When parsing Then scheme is v3`() {
        val apk = anApkFile(anApk(aSigningBlock(aPair(V3_ID))))

        val versions = parser.parseSchemeVersions(apk)

        versions shouldBe setOf(SigningSchemeVersion.V3)
    }

    @Test
    fun `Given a v3_1 block When parsing Then scheme is v3_1`() {
        val apk = anApkFile(anApk(aSigningBlock(aPair(V3_1_ID))))

        val versions = parser.parseSchemeVersions(apk)

        versions shouldBe setOf(SigningSchemeVersion.V3_1)
    }

    @Test
    fun `Given several pairs with an unknown one When parsing Then known schemes are found`() {
        val apk = anApkFile(anApk(aSigningBlock(aPair(UNKNOWN_ID, ByteArray(10)), aPair(V2_ID), aPair(V3_ID))))

        val versions = parser.parseSchemeVersions(apk)

        versions shouldBe setOf(SigningSchemeVersion.V2, SigningSchemeVersion.V3)
    }

    @Test
    fun `Given a comment holding a fake end record When parsing Then the real record is used`() {
        val fakeRecord = aLittleEndianBuffer(30).putInt(0x06054b50).array()
        val apk = anApkFile(anApk(aSigningBlock(aPair(V2_ID)), comment = fakeRecord))

        val versions = parser.parseSchemeVersions(apk)

        versions shouldBe setOf(SigningSchemeVersion.V2)
    }

    @Test
    fun `Given a zip with no signing block When parsing Then no scheme is found`() {
        val apk = anApkFile(anApk())

        val versions = parser.parseSchemeVersions(apk)

        versions shouldBe emptySet()
    }

    @Test
    fun `Given a zip too short to hold a signing block When parsing Then no scheme is found`() {
        val apk = anApkFile(anApk(entriesSize = 10))

        val versions = parser.parseSchemeVersions(apk)

        versions shouldBe emptySet()
    }

    @Test
    fun `Given a file with no end record When parsing Then result is unknown`() {
        val apk = anApkFile(ByteArray(100))

        val versions = parser.parseSchemeVersions(apk)

        versions.shouldBeNull()
    }

    @Test
    fun `Given a file shorter than an end record When parsing Then result is unknown`() {
        val apk = anApkFile(ByteArray(10))

        val versions = parser.parseSchemeVersions(apk)

        versions.shouldBeNull()
    }

    @Test
    fun `Given a zip64 central directory offset When parsing Then result is unknown`() {
        val apk = anApkFile(anApk(aSigningBlock(aPair(V2_ID)), centralDirectoryOffset = 0xFFFFFFFFL))

        val versions = parser.parseSchemeVersions(apk)

        versions.shouldBeNull()
    }

    @Test
    fun `Given a central directory offset past the file end When parsing Then result is unknown`() {
        val apk = anApkFile(anApk(aSigningBlock(aPair(V2_ID)), centralDirectoryOffset = 1_000_000L))

        val versions = parser.parseSchemeVersions(apk)

        versions.shouldBeNull()
    }

    @Test
    fun `Given block sizes that disagree When parsing Then result is unknown`() {
        val apk = anApkFile(anApk(aSigningBlock(aPair(V2_ID), leadingSize = 1L)))

        val versions = parser.parseSchemeVersions(apk)

        versions.shouldBeNull()
    }

    @Test
    fun `Given a block size reaching before the file start When parsing Then result is unknown`() {
        val apk = anApkFile(anApk(aSigningBlock(aPair(V2_ID), trailingSize = 10_000L)))

        val versions = parser.parseSchemeVersions(apk)

        versions.shouldBeNull()
    }

    @Test
    fun `Given a pair longer than the block When parsing Then result is unknown`() {
        val apk = anApkFile(anApk(aSigningBlock(aPair(V2_ID, declaredLength = 1_000L))))

        val versions = parser.parseSchemeVersions(apk)

        versions.shouldBeNull()
    }

    @Test
    fun `Given a pair too short to hold an id When parsing Then result is unknown`() {
        val apk = anApkFile(anApk(aSigningBlock(aPair(V2_ID, declaredLength = 3L))))

        val versions = parser.parseSchemeVersions(apk)

        versions.shouldBeNull()
    }

    @Test
    fun `Given trailing bytes too short for a pair When parsing Then result is unknown`() {
        val apk = anApkFile(anApk(aSigningBlock(aPair(V2_ID), ByteArray(3))))

        val versions = parser.parseSchemeVersions(apk)

        versions.shouldBeNull()
    }

    @Test(timeout = 5_000)
    fun `Given a pair length that wraps to no progress When parsing Then result is unknown`() {
        val apk = anApkFile(anApk(aSigningBlock(aPair(V2_ID, declaredLength = Long.MAX_VALUE - 7))))

        val versions = parser.parseSchemeVersions(apk)

        versions.shouldBeNull()
    }

    @Test(timeout = 5_000)
    fun `Given a pair length that wraps onto a forged id When parsing Then result is unknown`() {
        val valueHoldingForgedId = aLittleEndianBuffer(130).putInt(3, V3_ID.toInt()).array()
        val apk = anApkFile(anApk(aSigningBlock(aPair(0L, valueHoldingForgedId, declaredLength = Long.MAX_VALUE))))

        val versions = parser.parseSchemeVersions(apk)

        versions.shouldBeNull()
    }

    private fun anApkFile(bytes: ByteArray): String = temporaryFolder.newFile().apply { writeBytes(bytes) }.path
}

private fun anApk(
    signingBlock: ByteArray = ByteArray(0),
    entriesSize: Int = 64,
    centralDirectoryOffset: Long = entriesSize.toLong() + signingBlock.size,
    comment: ByteArray = ByteArray(0),
): ByteArray {
    val endRecord = aLittleEndianBuffer(22)
        .putInt(0x06054b50)
        .putInt(16, centralDirectoryOffset.toInt())
        .putShort(20, comment.size.toShort())
        .array()
    return ByteArray(entriesSize) + signingBlock + endRecord + comment
}

private fun aSigningBlock(
    vararg pairs: ByteArray,
    leadingSize: Long? = null,
    trailingSize: Long? = null,
): ByteArray {
    val payload = pairs.fold(ByteArray(0)) { all, pair -> all + pair }
    val size = payload.size + 24L
    val leading = aLittleEndianBuffer(8).putLong(leadingSize ?: size).array()
    val trailing = aLittleEndianBuffer(8).putLong(trailingSize ?: size).array()
    return leading + payload + trailing + "APK Sig Block 42".toByteArray(Charsets.US_ASCII)
}

private fun aPair(
    id: Long,
    value: ByteArray = ByteArray(4),
    declaredLength: Long = 4L + value.size,
): ByteArray = aLittleEndianBuffer(12 + value.size).putLong(declaredLength).putInt(id.toInt()).put(value).array()

private fun aLittleEndianBuffer(size: Int): ByteBuffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
