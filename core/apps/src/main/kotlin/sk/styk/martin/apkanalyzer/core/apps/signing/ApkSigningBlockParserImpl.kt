package sk.styk.martin.apkanalyzer.core.apps.signing

import java.io.RandomAccessFile
import javax.inject.Inject

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

internal class ApkSigningBlockParserImpl @Inject constructor() : ApkSigningBlockParser {

    override fun parseSchemeVersions(apkPath: String): Set<SigningSchemeVersion>? = RandomAccessFile(apkPath, "r").use(::parseSigningBlock)

    private fun parseSigningBlock(file: RandomAccessFile): Set<SigningSchemeVersion>? {
        val fileLength = file.length()
        val eocdOffset = findEocdOffset(file, fileLength)
        val centralDirOffset = eocdOffset?.let { readCentralDirectoryOffset(file, it) }
        return when {
            centralDirOffset == null || centralDirOffset !in 1..fileLength -> null
            centralDirOffset < SIGNING_BLOCK_MAGIC_SIZE + SIGNING_BLOCK_TRAILER_SIZE -> emptySet()
            !hasSigningBlockMagic(file, centralDirOffset) -> emptySet()
            else -> readSigningBlockPayload(file, centralDirOffset)?.let(::parseIdValuePairs)
        }
    }

    private fun hasSigningBlockMagic(file: RandomAccessFile, centralDirOffset: Long): Boolean {
        val magicBytes = ByteArray(SIGNING_BLOCK_MAGIC_SIZE)
        file.seek(centralDirOffset - SIGNING_BLOCK_MAGIC_SIZE)
        file.readFully(magicBytes)
        return String(magicBytes, Charsets.US_ASCII) == SIGNING_BLOCK_MAGIC
    }

    private fun readSigningBlockPayload(file: RandomAccessFile, centralDirOffset: Long): ByteArray? {
        val blockSize = file.readLongLEAt(centralDirOffset - SIGNING_BLOCK_TRAILER_SIZE)
        val blockStartOffset = centralDirOffset - 8 - blockSize
        val payloadStart = blockStartOffset + 8
        val payloadEnd = centralDirOffset - SIGNING_BLOCK_TRAILER_SIZE
        val payloadLength = payloadEnd - payloadStart
        val hasValidStructure = blockSize in 1..MAX_SIGNING_BLOCK_SIZE &&
            blockStartOffset >= 0 &&
            file.readLongLEAt(blockStartOffset) == blockSize &&
            payloadLength in 0..MAX_SIGNING_BLOCK_SIZE
        return if (hasValidStructure) {
            ByteArray(payloadLength.toInt()).also {
                file.seek(payloadStart)
                file.readFully(it)
            }
        } else {
            null
        }
    }

    private fun RandomAccessFile.readLongLEAt(offset: Long): Long {
        val bytes = ByteArray(8)
        seek(offset)
        readFully(bytes)
        return bytes.readLongLE(0)
    }

    private fun parseIdValuePairs(payload: ByteArray): Set<SigningSchemeVersion>? {
        val versions = mutableSetOf<SigningSchemeVersion>()
        var offset = 0
        var isValidPayload = true
        while (offset < payload.size && isValidPayload) {
            if (offset + 8 > payload.size) {
                isValidPayload = false
            } else {
                val pairLength = payload.readLongLE(offset)
                val idOffset = offset + 8
                if (pairLength < 4 || idOffset.toLong() + pairLength > payload.size) {
                    isValidPayload = false
                } else {
                    val id = payload.readUInt32LE(idOffset)
                    when (id) {
                        ID_SIGNATURE_SCHEME_V2 -> versions.add(SigningSchemeVersion.V2)
                        ID_SIGNATURE_SCHEME_V3 -> versions.add(SigningSchemeVersion.V3)
                        ID_SIGNATURE_SCHEME_V3_1 -> versions.add(SigningSchemeVersion.V3_1)
                    }
                    offset += (8 + pairLength).toInt()
                }
            }
        }
        return versions.takeIf { isValidPayload }
    }

    private fun findEocdOffset(file: RandomAccessFile, fileLength: Long): Long? {
        var eocdOffset: Long? = null
        if (fileLength >= EOCD_MIN_SIZE) {
            val searchSize = minOf(fileLength, (EOCD_MIN_SIZE + EOCD_MAX_COMMENT_SIZE).toLong()).toInt()
            val buffer = ByteArray(searchSize)
            file.seek(fileLength - searchSize)
            file.readFully(buffer)

            for (pos in (searchSize - EOCD_MIN_SIZE) downTo 0) {
                if (buffer.readUInt32LE(pos) == EOCD_SIGNATURE) {
                    val commentLength = buffer.readUInt16LE(pos + EOCD_COMMENT_LENGTH_FIELD)
                    if (pos + EOCD_MIN_SIZE + commentLength == searchSize) {
                        eocdOffset = fileLength - searchSize + pos
                        break
                    }
                }
            }
        }
        return eocdOffset
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
