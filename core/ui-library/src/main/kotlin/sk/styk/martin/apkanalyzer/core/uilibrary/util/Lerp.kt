package sk.styk.martin.apkanalyzer.core.uilibrary.util

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.util.lerp

fun lerp(
    start: FontWeight,
    stop: FontWeight,
    fraction: Float,
): FontWeight = FontWeight(lerp(start.weight, stop.weight, fraction))
