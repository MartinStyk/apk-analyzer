package sk.styk.martin.apkanalyzer.core.apps.search

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.apps.model.anApp

class AppSearchTest {

    private val whatsApp = anApp(name = "WhatsApp", packageName = "com.chat.messenger")
    private val notes = anApp(name = "Simple Notes", packageName = "org.quick.writer")

    @Test
    fun `Given a blank query When searching Then result is empty`() {
        val apps = listOf(whatsApp, notes)

        val result = apps.search(" ")

        result.shouldBeEmpty()
    }

    @Test
    fun `Given a query in a different case When searching Then app name matches`() {
        val apps = listOf(whatsApp, notes)

        val result = apps.search("WHATS")

        result shouldBe listOf(whatsApp)
    }

    @Test
    fun `Given a query in a different case When searching Then package name matches`() {
        val apps = listOf(whatsApp, notes)

        val result = apps.search("ORG.QUICK")

        result shouldBe listOf(notes)
    }

    @Test
    fun `Given a query with one wrong letter When searching Then app is still found`() {
        val apps = listOf(whatsApp, notes)

        val result = apps.search("whatzapp")

        result shouldBe listOf(whatsApp)
    }

    @Test
    fun `Given a query unlike any app When searching Then result is empty`() {
        val apps = listOf(whatsApp, notes)

        val result = apps.search("spotify")

        result.shouldBeEmpty()
    }

    @Test
    fun `Given exact and approximate matches When searching Then exact match is first`() {
        val approximate = anApp(name = "Mapz", packageName = "com.first.app")
        val exact = anApp(name = "Maps", packageName = "com.second.app")
        val apps = listOf(approximate, exact)

        val result = apps.search("maps")

        result shouldBe listOf(exact, approximate)
    }

    @Test
    fun `Given a match at a word start and one inside a word When searching Then word start is first`() {
        val insideWord = anApp(name = "Bitmap Editor", packageName = "com.first.app")
        val wordStart = anApp(name = "Maps", packageName = "com.second.app")
        val apps = listOf(insideWord, wordStart)

        val result = apps.search("map")

        result shouldBe listOf(wordStart, insideWord)
    }

    @Test
    fun `Given a query spanning two words When searching Then app is still found`() {
        val apps = listOf(whatsApp, notes)

        val result = apps.search("simple no")

        result shouldBe listOf(notes)
    }
}
