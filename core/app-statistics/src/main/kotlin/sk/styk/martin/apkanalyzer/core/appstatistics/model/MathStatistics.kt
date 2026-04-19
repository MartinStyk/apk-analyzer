package sk.styk.martin.apkanalyzer.core.appstatistics.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.util.Arrays
import kotlin.math.sqrt

@Parcelize
data class MathStatistics(val arithmeticMean: BigDecimal, var median: BigDecimal, var max: BigDecimal, var min: BigDecimal, var variance: BigDecimal, var deviation: BigDecimal) : Parcelable

internal fun FloatArray.toMathStats(): MathStatistics {
    if (this.isEmpty()) {
        throw IllegalArgumentException()
    }

    Arrays.sort(this)

    fun computeMean(): Double = sum() / size.toDouble()

    fun computeMedian(): Float = if (size % 2 == 0) {
        (this[size / 2 - 1] + this[size / 2]) / 2.0f
    } else {
        this[size / 2]
    }

    fun computeVariance(mean: Float): Double {
        var temp = 0.0
        for (a in this) {
            temp += (a - mean) * (a - mean)
        }
        return temp / (size - 1)
    }

    val arithmeticMean = BigDecimal(computeMean())
    val variance = BigDecimal(computeVariance(arithmeticMean.toFloat()))
    val deviation = BigDecimal(sqrt(variance.toFloat().toDouble()))

    return MathStatistics(
        arithmeticMean = arithmeticMean,
        median = BigDecimal(computeMedian().toDouble()),
        max = BigDecimal(this[this.size - 1].toDouble()),
        min = BigDecimal(this[0].toDouble()),
        variance = variance,
        deviation = deviation,
    )
}
