package sk.styk.martin.apkanalyzer.core.designsystem.phone.format

import android.text.format.DateFormat
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private const val SHORT_DATE_SKELETON = "dMMM"
private const val SHORT_DATE_TIME_SKELETON = "dMMMjmm"
private const val MONTH_SKELETON = "LLLL"
private const val MONTH_YEAR_SKELETON = "MMMMy"

fun YearMonth.historyMonth(locale: Locale): String {
    val skeleton = if (year == YearMonth.now().year) MONTH_SKELETON else MONTH_YEAR_SKELETON
    return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale).format(this)
}

fun Instant.historyDate(): String {
    val zone = ZoneId.systemDefault()
    if (atZone(zone).year != Instant.now().atZone(zone).year) return historyFullDate()
    val locale = Locale.getDefault()
    return DateTimeFormatter
        .ofPattern(DateFormat.getBestDateTimePattern(locale, SHORT_DATE_SKELETON), locale)
        .withZone(zone)
        .format(this)
}

fun Instant.historyDateTime(): String {
    val locale = Locale.getDefault()
    return DateTimeFormatter
        .ofPattern(DateFormat.getBestDateTimePattern(locale, SHORT_DATE_TIME_SKELETON), locale)
        .withZone(ZoneId.systemDefault())
        .format(this)
}

fun Instant.historyFullDate(): String = DateTimeFormatter
    .ofLocalizedDate(FormatStyle.MEDIUM)
    .withLocale(Locale.getDefault())
    .withZone(ZoneId.systemDefault())
    .format(this)

fun Instant.isSameDayAs(other: Instant): Boolean {
    val zone = ZoneId.systemDefault()
    return atZone(zone).toLocalDate() == other.atZone(zone).toLocalDate()
}
