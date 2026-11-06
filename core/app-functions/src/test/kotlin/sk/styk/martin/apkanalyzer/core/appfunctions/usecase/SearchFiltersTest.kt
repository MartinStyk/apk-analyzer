package sk.styk.martin.apkanalyzer.core.appfunctions.usecase

import io.kotest.matchers.shouldBe
import org.junit.Test

class SearchFiltersTest {

    @Test
    fun `Given a short permission name When parsing Then full permission name is used`() {
        val filters = parse(permission = "camera")

        filters.permission shouldBe "android.permission.CAMERA"
    }

    @Test
    fun `Given a full permission name When parsing Then it is kept unchanged`() {
        val filters = parse(permission = "com.example.permission.custom")

        filters.permission shouldBe "com.example.permission.custom"
    }

    @Test
    fun `Given only blank values When parsing Then filters are empty`() {
        val filters = parse(query = " ", permission = " ", installSource = " ")

        filters.isEmpty shouldBe true
    }
}

private fun parse(
    query: String? = null,
    permission: String? = null,
    installSource: String? = null,
) = SearchFilters.parse(query, permission, installSource, targetSdk = null, minTotalSizeMb = null, unusedForDays = null)
