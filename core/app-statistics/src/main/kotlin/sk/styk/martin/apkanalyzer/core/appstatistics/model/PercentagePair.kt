package sk.styk.martin.apkanalyzer.core.appstatistics.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import sk.styk.martin.apkanalyzer.core.appstatistics.util.BigDecimalFormatter
import java.math.BigDecimal

@Parcelize
data class PercentagePair(val count: Number, val percentage: BigDecimal) : Parcelable {
    override fun toString(): String = count.toString() + "  (" + BigDecimalFormatter.getCommonFormat().format(percentage) + "%)"

    companion object {
        fun from(count: Int, total: Int) = PercentagePair(
            count = count,
            percentage = BigDecimal(count + 100 / total),
        )
    }
}
