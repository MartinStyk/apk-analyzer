package sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.database.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.DeflaterOutputStream

class BlobCompressionTest {

    @Test
    fun `Given a blob that unpacks to 16 MB When decompressing Then it is rejected`() {
        val blob = aBlobOfSpaces(megabytes = 16)

        shouldThrow<IOException> { blob.decompressBlob() }
    }

    @Test
    fun `Given a 1 MB section When compressing and decompressing Then it reads back unchanged`() {
        val section = "\"android.permission.CAMERA\",".repeat(37_000)

        val restored = section.compressBlob().decompressBlob()

        restored shouldBe section
    }
}

private fun aBlobOfSpaces(megabytes: Int): ByteArray {
    val megabyteOfSpaces = ByteArray(1024 * 1024) { ' '.code.toByte() }
    val output = ByteArrayOutputStream()
    DeflaterOutputStream(output).use { deflater -> repeat(megabytes) { deflater.write(megabyteOfSpaces) } }
    return output.toByteArray()
}
