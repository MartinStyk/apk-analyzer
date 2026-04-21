package sk.styk.martin.apkanalyzer.core.common.model

import kotlin.math.abs

@JvmInline
value class AppSize(val bytes: Long) : Comparable<AppSize> {

    val kilobytes: Double get() = bytes / 1024.0
    val megabytes: Double get() = kilobytes / 1024.0
    val gigabytes: Double get() = megabytes / 1024.0

    fun formatted(): String = when {
        abs(bytes) < 1024L -> "$bytes B"
        abs(bytes) < 1024L * 1024L -> "%.0f KB".format(kilobytes)
        abs(bytes) < 1024L * 1024L * 1024L -> "%.1f MB".format(megabytes)
        else -> "%.2f GB".format(gigabytes)
    }

    override fun compareTo(other: AppSize): Int = bytes.compareTo(other.bytes)

    override fun toString(): String = formatted()
}

val Long.bytes: AppSize get() = AppSize(this)

val Long.kilobytes: AppSize get() = AppSize(this * 1024)

val Long.megabytes: AppSize get() = AppSize(this * 1024 * 1024)

val Long.gigabytes: AppSize get() = AppSize(this * 1024 * 1024 * 1024)

val Int.bytes: AppSize get() = this.toLong().bytes

val Int.kilobytes: AppSize get() = this.toLong().kilobytes

val Int.megabytes: AppSize get() = this.toLong().megabytes

val Int.gigabytes: AppSize get() = this.toLong().gigabytes

