package sk.styk.martin.apkanalyzer.core.common.digest

import sk.styk.martin.apkanalyzer.core.common.settings.FingerprintFormat
import java.util.Locale

fun String.toFingerprint(format: FingerprintFormat): String {
    val hex = replace(":", "").uppercase(Locale.ROOT)
    return when (format) {
        FingerprintFormat.ColonSeparated -> hex.chunked(2).joinToString(":")
        FingerprintFormat.Continuous -> hex
    }
}
