package sk.styk.martin.apkanalyzer.feature.appdetail.phone.history

import android.text.format.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private const val SHORT_DATE_SKELETON = "dMMM"
private const val SHORT_DATE_TIME_SKELETON = "dMMMjmm"

internal fun Instant.historyDate(): String {
    val zone = ZoneId.systemDefault()
    if (atZone(zone).year != Instant.now().atZone(zone).year) return historyFullDate()
    val locale = Locale.getDefault()
    return DateTimeFormatter
        .ofPattern(DateFormat.getBestDateTimePattern(locale, SHORT_DATE_SKELETON), locale)
        .withZone(zone)
        .format(this)
}

internal fun Instant.historyDateTime(): String {
    val locale = Locale.getDefault()
    return DateTimeFormatter
        .ofPattern(DateFormat.getBestDateTimePattern(locale, SHORT_DATE_TIME_SKELETON), locale)
        .withZone(ZoneId.systemDefault())
        .format(this)
}

internal fun Instant.historyFullDate(): String = DateTimeFormatter
    .ofLocalizedDate(FormatStyle.MEDIUM)
    .withLocale(Locale.getDefault())
    .withZone(ZoneId.systemDefault())
    .format(this)

internal fun Instant.isSameDayAs(other: Instant): Boolean {
    val zone = ZoneId.systemDefault()
    return atZone(zone).toLocalDate() == other.atZone(zone).toLocalDate()
}
