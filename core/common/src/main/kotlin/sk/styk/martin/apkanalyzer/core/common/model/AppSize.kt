package sk.styk.martin.apkanalyzer.core.common.model

import java.util.Locale
import kotlin.math.abs

private const val BYTES_PER_KIBIBYTE = 1024L
private const val BYTES_PER_MEBIBYTE = BYTES_PER_KIBIBYTE * BYTES_PER_KIBIBYTE
private const val BYTES_PER_GIBIBYTE = BYTES_PER_MEBIBYTE * BYTES_PER_KIBIBYTE

@JvmInline
value class AppSize private constructor(val bytes: Long) : Comparable<AppSize> {

    val kilobytes: Double get() = bytes / BYTES_PER_KIBIBYTE.toDouble()
    val megabytes: Double get() = bytes / BYTES_PER_MEBIBYTE.toDouble()
    val gigabytes: Double get() = bytes / BYTES_PER_GIBIBYTE.toDouble()

    fun formatted(): String = when {
        abs(bytes) < BYTES_PER_KIBIBYTE -> "$bytes B"
        abs(bytes) < BYTES_PER_MEBIBYTE -> "%.0f KB".format(Locale.getDefault(), kilobytes)
        abs(bytes) < BYTES_PER_GIBIBYTE -> "%.1f MB".format(Locale.getDefault(), megabytes)
        else -> "%.2f GB".format(Locale.getDefault(), gigabytes)
    }

    operator fun plus(other: AppSize): AppSize = AppSize(bytes + other.bytes)

    operator fun minus(other: AppSize): AppSize = AppSize(bytes - other.bytes)

    override fun compareTo(other: AppSize): Int = bytes.compareTo(other.bytes)

    override fun toString(): String = formatted()

    internal companion object {
        fun ofBytes(bytes: Long): AppSize = AppSize(bytes)
    }
}

val Long.bytes: AppSize get() = AppSize.ofBytes(this)

val Long.kilobytes: AppSize get() = AppSize.ofBytes(this * BYTES_PER_KIBIBYTE)

val Long.megabytes: AppSize get() = AppSize.ofBytes(this * BYTES_PER_MEBIBYTE)

val Long.gigabytes: AppSize get() = AppSize.ofBytes(this * BYTES_PER_GIBIBYTE)

val Int.bytes: AppSize get() = this.toLong().bytes

val Int.kilobytes: AppSize get() = this.toLong().kilobytes

val Int.megabytes: AppSize get() = this.toLong().megabytes

val Int.gigabytes: AppSize get() = this.toLong().gigabytes

val Float.megabytes: AppSize get() = AppSize.ofBytes((this * BYTES_PER_MEBIBYTE).toLong())
