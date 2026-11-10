package sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme

import androidx.compose.runtime.compositionLocalOf
import sk.styk.martin.apkanalyzer.core.common.settings.FingerprintFormat

val LocalFingerprintFormat = compositionLocalOf { FingerprintFormat.ColonSeparated }
