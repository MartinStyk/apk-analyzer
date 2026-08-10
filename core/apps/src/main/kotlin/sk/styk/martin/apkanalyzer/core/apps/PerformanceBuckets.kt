package sk.styk.martin.apkanalyzer.core.apps

internal fun appCountBucket(appCount: Int): String = when (appCount) {
    in 0..49 -> "0_49"
    in 50..99 -> "50_99"
    in 100..199 -> "100_199"
    in 200..399 -> "200_399"
    else -> "400_plus"
}

internal fun splitCountBucket(splitCount: Int): String = when (splitCount) {
    0 -> "0"
    in 1..4 -> "1_4"
    in 5..9 -> "5_9"
    else -> "10_plus"
}
