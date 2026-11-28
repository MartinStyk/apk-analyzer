package sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.database.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

private const val BLOB_COMPRESSION_LEVEL = 6
private const val MAX_DECOMPRESSED_BLOB_BYTES = 8 * 1024 * 1024

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

internal fun ByteArray.decompressBlob(): String = InflaterInputStream(ByteArrayInputStream(this)).use { input ->
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var read = input.read(buffer)
    while (read >= 0) {
        if (output.size() + read > MAX_DECOMPRESSED_BLOB_BYTES) {
            throw IOException("History blob unpacks to more than $MAX_DECOMPRESSED_BLOB_BYTES bytes")
        }
        output.write(buffer, 0, read)
        read = input.read(buffer)
    }
    output.toByteArray().decodeToString()
}
