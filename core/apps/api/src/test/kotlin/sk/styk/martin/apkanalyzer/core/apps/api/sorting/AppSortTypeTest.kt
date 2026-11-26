package sk.styk.martin.apkanalyzer.core.apps.sorting

import io.kotest.matchers.shouldBe
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.apps.model.anApp
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import java.time.Instant
import java.util.Locale

class AppSortTypeTest {

    @Test
    fun `Given names with mixed case and accents When sorting by name Then they sort as letters`() {
        val apps = listOf(anApp(name = "Zebra"), anApp(name = "Éclair"), anApp(name = "apple"))

        val sorted = apps.sortedWith(AppSortType.Name.comparator(ascending = true, Locale.ENGLISH))

        sorted.map { it.applicationName } shouldBe listOf("apple", "Éclair", "Zebra")
    }

    @Test
    fun `Given an unknown total size When sorting by total size ascending Then it is first`() {
        val apps =
            listOf(anApp(name = "Large", totalSize = 20.megabytes), anApp(name = "Unknown", totalSize = null), anApp(name = "Small", totalSize = 10.megabytes))

        val sorted = apps.sortedWith(AppSortType.TotalSize.comparator(ascending = true))

        sorted.map { it.applicationName } shouldBe listOf("Unknown", "Small", "Large")
    }

    @Test
    fun `Given a never used app When sorting by last used descending Then it is last`() {
        val apps = listOf(
            anApp(name = "Never", lastUsedTime = null),
            anApp(name = "Older", lastUsedTime = Instant.parse("2026-01-01T00:00:00Z")),
            anApp(name = "Newer", lastUsedTime = Instant.parse("2026-02-01T00:00:00Z")),
        )

        val sorted = apps.sortedWith(AppSortType.LastUsed.comparator(ascending = false))

        sorted.map { it.applicationName } shouldBe listOf("Newer", "Older", "Never")
    }
}
