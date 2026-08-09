package sk.styk.martin.apkanalyzer.core.apps.analysis

import sk.styk.martin.apkanalyzer.core.apps.model.SigningSchemeVersion
import java.io.RandomAccessFile
import java.util.zip.ZipFile

internal object ApkSigningBlockAnalyzer {

    private const val EOCD_SIGNATURE = 0x06054b50L
    private const val EOCD_MIN_SIZE = 22
    private const val EOCD_MAX_COMMENT_SIZE = 0xFFFF
    private const val EOCD_CENTRAL_DIR_OFFSET_FIELD = 16
    private const val EOCD_COMMENT_LENGTH_FIELD = 20
    private const val ZIP64_MAGIC_OFFSET = 0xFFFFFFFFL

    private const val SIGNING_BLOCK_MAGIC = "APK Sig Block 42"
    private const val SIGNING_BLOCK_MAGIC_SIZE = 16
    private const val SIGNING_BLOCK_TRAILER_SIZE = 24
    private const val MAX_SIGNING_BLOCK_SIZE = 20_000_000L

    private const val ID_SIGNATURE_SCHEME_V2 = 0x7109871aL
    private const val ID_SIGNATURE_SCHEME_V3 = 0xf05368c0L
    private const val ID_SIGNATURE_SCHEME_V3_1 = 0x1b93ad61L

    fun detectSchemeVersions(apkPath: String): List<SigningSchemeVersion>? {
        val blockVersions = readSigningBlockVersions(apkPath) ?: return null
        val hasV1 = hasJarSignature(apkPath) ?: return null
        val versions = buildSet {
            if (hasV1) add(SigningSchemeVersion.V1)
            addAll(blockVersions)
        }
        return versions.takeIf { it.isNotEmpty() }?.sorted()
    }

    private fun hasJarSignature(apkPath: String): Boolean? = runCatching {
        ZipFile(apkPath).use { zip ->
            zip.entries().asSequence().any { entry ->
                val name = entry.name
                name.startsWith("META-INF/") &&
                    (name.endsWith(".RSA") || name.endsWith(".DSA") || name.endsWith(".EC"))
            }
        }
    }.getOrNull()

    private fun readSigningBlockVersions(apkPath: String): Set<SigningSchemeVersion>? = runCatching {
        RandomAccessFile(apkPath, "r").use(::parseSigningBlock)
    }.getOrNull()

    private fun parseSigningBlock(file: RandomAccessFile): Set<SigningSchemeVersion>? {
        val fileLength = file.length()
        val eocdOffset = findEocdOffset(file, fileLength) ?: return null
        val centralDirOffset = readCentralDirectoryOffset(file, eocdOffset) ?: return null
        if (centralDirOffset <= 0 || centralDirOffset > fileLength) return null
        if (centralDirOffset < SIGNING_BLOCK_MAGIC_SIZE + SIGNING_BLOCK_TRAILER_SIZE) return emptySet()

        val magicBytes = ByteArray(SIGNING_BLOCK_MAGIC_SIZE)
        file.seek(centralDirOffset - SIGNING_BLOCK_MAGIC_SIZE)
        file.readFully(magicBytes)
        if (String(magicBytes, Charsets.US_ASCII) != SIGNING_BLOCK_MAGIC) return emptySet()

        val trailerSizeBytes = ByteArray(8)
        file.seek(centralDirOffset - SIGNING_BLOCK_TRAILER_SIZE)
        file.readFully(trailerSizeBytes)
        val blockSize = trailerSizeBytes.readLongLE(0)
        if (blockSize <= 0 || blockSize > MAX_SIGNING_BLOCK_SIZE) return null

        val blockStartOffset = centralDirOffset - 8 - blockSize
        if (blockStartOffset < 0) return null

        val headerSizeBytes = ByteArray(8)
        file.seek(blockStartOffset)
        file.readFully(headerSizeBytes)
        if (headerSizeBytes.readLongLE(0) != blockSize) return null

        val payloadStart = blockStartOffset + 8
        val payloadEnd = centralDirOffset - SIGNING_BLOCK_TRAILER_SIZE
        val payloadLength = payloadEnd - payloadStart
        if (payloadLength < 0 || payloadLength > MAX_SIGNING_BLOCK_SIZE) return null

        val payload = ByteArray(payloadLength.toInt())
        file.seek(payloadStart)
        file.readFully(payload)

        return parseIdValuePairs(payload)
    }

    private fun parseIdValuePairs(payload: ByteArray): Set<SigningSchemeVersion>? {
        val versions = mutableSetOf<SigningSchemeVersion>()
        var offset = 0
        while (offset < payload.size) {
            if (offset + 8 > payload.size) return null
            val pairLength = payload.readLongLE(offset)
            val idOffset = offset + 8
            if (pairLength < 4 || idOffset.toLong() + pairLength > payload.size) return null
            val id = payload.readUInt32LE(idOffset)
            when (id) {
                ID_SIGNATURE_SCHEME_V2 -> versions.add(SigningSchemeVersion.V2)
                ID_SIGNATURE_SCHEME_V3 -> versions.add(SigningSchemeVersion.V3)
                ID_SIGNATURE_SCHEME_V3_1 -> versions.add(SigningSchemeVersion.V3_1)
            }
            offset += (8 + pairLength).toInt()
        }
        return versions
    }

    private fun findEocdOffset(file: RandomAccessFile, fileLength: Long): Long? {
        if (fileLength < EOCD_MIN_SIZE) return null
        val searchSize = minOf(fileLength, (EOCD_MIN_SIZE + EOCD_MAX_COMMENT_SIZE).toLong()).toInt()
        val buffer = ByteArray(searchSize)
        file.seek(fileLength - searchSize)
        file.readFully(buffer)

        for (pos in (searchSize - EOCD_MIN_SIZE) downTo 0) {
            if (buffer.readUInt32LE(pos) == EOCD_SIGNATURE) {
                val commentLength = buffer.readUInt16LE(pos + EOCD_COMMENT_LENGTH_FIELD)
                if (pos + EOCD_MIN_SIZE + commentLength == searchSize) {
                    return fileLength - searchSize + pos
                }
            }
        }
        return null
    }

    private fun readCentralDirectoryOffset(file: RandomAccessFile, eocdOffset: Long): Long? {
        val fieldBytes = ByteArray(4)
        file.seek(eocdOffset + EOCD_CENTRAL_DIR_OFFSET_FIELD)
        file.readFully(fieldBytes)
        val offset = fieldBytes.readUInt32LE(0)
        return offset.takeUnless { it == ZIP64_MAGIC_OFFSET }
    }

    private fun ByteArray.readUInt16LE(offset: Int): Int = (this[offset].toInt() and 0xFF) or ((this[offset + 1].toInt() and 0xFF) shl 8)

    private fun ByteArray.readUInt32LE(offset: Int): Long = (this[offset].toLong() and 0xFF) or
        ((this[offset + 1].toLong() and 0xFF) shl 8) or
        ((this[offset + 2].toLong() and 0xFF) shl 16) or
        ((this[offset + 3].toLong() and 0xFF) shl 24)

    private fun ByteArray.readLongLE(offset: Int): Long {
        var result = 0L
        for (i in 0 until 8) {
            result = result or ((this[offset + i].toLong() and 0xFF) shl (8 * i))
        }
        return result
    }
}
