package sk.styk.martin.apkanalyzer.core.apphistory.storage.database.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

private const val BLOB_COMPRESSION_LEVEL = 6

internal fun String.compressBlob(): ByteArray {
    val deflater = Deflater(BLOB_COMPRESSION_LEVEL)
    return try {
        val output = ByteArrayOutputStream()
        DeflaterOutputStream(output, deflater).use { it.write(encodeToByteArray()) }
        output.toByteArray()
    } finally {
        deflater.end()
    }
}

internal fun ByteArray.decompressBlob(): String = InflaterInputStream(ByteArrayInputStream(this)).use { it.readBytes() }.decodeToString()
