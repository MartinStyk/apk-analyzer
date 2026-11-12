package sk.styk.martin.apkanalyzer

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

private val placeholderPattern = Regex("""(?<!%)%(\d+\$)?[sdf]""")

@DisableCachingByDefault(because = "The task validates the live string resources without producing outputs.")
abstract class ValidateTranslationsTask : DefaultTask() {

    @get:Internal
    abstract val repositoryDirectory: DirectoryProperty

    @get:Input
    abstract val modules: MapProperty<String, String>

    init {
        group = "verification"
        description = "Validates that every locale strings.xml has exactly the translatable base keys and placeholders."
    }

    @TaskAction
    fun validate() {
        val rootDirectory = repositoryDirectory.asFile.get()
        val errors = mutableListOf<String>()
        var localeFileCount = 0

        modules.get().values.sorted().forEach { moduleDirectory ->
            val resources = rootDirectory.resolve(moduleDirectory).resolve("src/main/res")
            val baseFile = resources.resolve("values/strings.xml")
            if (!baseFile.isFile) return@forEach

            val base = readStringResources(baseFile)
            val translatable = base.filterValues { resource -> resource.translatable }
            resources.listFiles().orEmpty()
                .filter { directory -> directory.name.startsWith("values-") }
                .map { directory -> directory.resolve("strings.xml") }
                .filter(File::isFile)
                .sortedBy { file -> file.parentFile.name }
                .forEach { localeFile ->
                    localeFileCount++
                    val displayPath = localeFile.relativeTo(rootDirectory).invariantSeparatorsPath
                    translationViolations(translatable, readStringResources(localeFile)).forEach { violation ->
                        errors += "$displayPath: $violation"
                    }
                }
        }

        if (errors.isNotEmpty()) {
            throw GradleException(
                errors.joinToString(prefix = "Translation validation failed:\n- ", separator = "\n- "),
            )
        }

        logger.lifecycle("Validated $localeFileCount locale strings.xml files.")
    }
}

internal data class StringResource(val translatable: Boolean, val placeholders: List<String>?)

internal fun translationViolations(base: Map<String, StringResource>, locale: Map<String, StringResource>): List<String> = buildList {
    (base.keys - locale.keys).sorted().forEach { key -> add("missing translation for '$key'.") }
    (locale.keys - base.keys).sorted().forEach { key ->
        add("'$key' is not a translatable key in values/strings.xml. Remove it from the locale.")
    }
    base.keys.intersect(locale.keys).sorted().forEach { key ->
        val expected = base.getValue(key).placeholders
        val actual = locale.getValue(key).placeholders
        if (expected != null && actual != null && expected != actual) {
            add("'$key' has placeholders $actual, but values/strings.xml has $expected.")
        }
    }
}

internal fun readStringResources(file: File): Map<String, StringResource> {
    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
    val root = document.documentElement
    return listOf("string", "plurals").flatMap { tag ->
        val nodes = root.getElementsByTagName(tag)
        (0 until nodes.length).map { index -> nodes.item(index) as Element }
    }.associate { element ->
        element.getAttribute("name") to StringResource(
            translatable = element.getAttribute("translatable") != "false",
            placeholders = if (element.tagName == "string") placeholdersOf(element.textContent) else null,
        )
    }
}

internal fun placeholdersOf(text: String): List<String> = placeholderPattern.findAll(text).map { match -> match.value }.sorted().toList()
