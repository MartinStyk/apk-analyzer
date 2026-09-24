package sk.styk.martin.apkanalyzer.core.common.io

import java.io.InputStream

fun InputStream.limited(byteCount: Long): LimitedInputStream = LimitedInputStream(this, byteCount)

class LimitedInputStream(private val delegate: InputStream, private var remaining: Long) : InputStream() {

    override fun read(): Int {
        if (remaining <= 0) return -1
        val result = delegate.read()
        if (result >= 0) remaining--
        return result
    }

    override fun read(
        b: ByteArray,
        off: Int,
        len: Int,
    ): Int {
        if (remaining <= 0) return -1
        val bounded = minOf(len.toLong(), remaining).toInt()
        val result = delegate.read(b, off, bounded)
        if (result > 0) remaining -= result
        return result
    }

    fun drainRemaining() {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (remaining > 0) {
            if (read(buffer) <= 0) break
        }
    }
}
