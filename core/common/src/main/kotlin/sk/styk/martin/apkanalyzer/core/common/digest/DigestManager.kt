package sk.styk.martin.apkanalyzer.core.common.digest

import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Locale
import javax.inject.Inject

class DigestManager
@Inject
constructor() {
    fun md5Digest(input: ByteArray): String = computeHash(algorithm = "Md5", input)

    fun md5Digest(input: String): String = md5Digest(input.toByteArray())

    fun sha1Digest(input: ByteArray): String = computeHash(algorithm = "SHA-1", input)

    fun sha1Digest(input: String): String = sha1Digest(input.toByteArray())

    fun sha256Digest(input: ByteArray): String = computeHash(algorithm = "SHA-256", input)

    fun sha256Digest(input: String): String = sha256Digest(input.toByteArray())

    fun byteToHexString(bArray: ByteArray): String {
        val sb = StringBuilder(bArray.size)
        val var5 = bArray.size

        for (var6 in 0 until var5) {
            val aBArray = bArray[var6]
            val sTemp = Integer.toHexString(255 and aBArray.toInt().toChar().code)
            if (sTemp.length < 2) {
                sb.append(0)
            }

            sb.append(sTemp.uppercase(Locale.getDefault()))
        }

        return sb.toString()
    }

    private fun getHexString(digest: ByteArray): String {
        val bi = BigInteger(1, digest)
        return String.format("%032x", bi)
    }

    private fun getDigest(algorithm: String): MessageDigest {
        try {
            return MessageDigest.getInstance(algorithm)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e.message, e)
        }
    }

    private fun computeHash(algorithm: String, input: ByteArray): String {
        val digest = getDigest(algorithm)
        digest.update(input)
        return getHexString(digest.digest())
    }
}
