package sk.styk.martin.apkanalyzer.core.apps.filtering

import io.kotest.matchers.shouldBe
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.common.model.megabytes

class AppSizeRangeTest {

    private val bounds = AppSizeRange(min = 10.megabytes, max = 20.megabytes)

    @Test
    fun `Given a range entirely above the bounds When coercing Then it collapses to the upper bound`() {
        val range = AppSizeRange(min = 30.megabytes, max = 40.megabytes)

        val coerced = range.coerceIn(bounds)

        coerced shouldBe AppSizeRange(min = 20.megabytes, max = 20.megabytes)
    }

    @Test
    fun `Given a range entirely below the bounds When coercing Then it collapses to the lower bound`() {
        val range = AppSizeRange(min = 1.megabytes, max = 2.megabytes)

        val coerced = range.coerceIn(bounds)

        coerced shouldBe AppSizeRange(min = 10.megabytes, max = 10.megabytes)
    }

    @Test
    fun `Given a range overlapping the bounds When coercing Then only the outside end moves`() {
        val range = AppSizeRange(min = 15.megabytes, max = 40.megabytes)

        val coerced = range.coerceIn(bounds)

        coerced shouldBe AppSizeRange(min = 15.megabytes, max = 20.megabytes)
    }
}
