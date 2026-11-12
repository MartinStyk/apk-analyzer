package sk.styk.martin.apkanalyzer

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

private const val ROOT_NAMESPACE = "sk.styk.martin.apkanalyzer"

private val projectAccessorPattern = Regex("""\bprojects\.([A-Za-z0-9_.]+)""")
private val forbiddenBuildTokenPattern = Regex("""\b(minSdk|compileSdk|targetSdk|jvmToolchain|jvmTarget)\b|\bJavaVersion\.""")
private val stringProjectPattern = Regex("""\bproject\(\s*":""")
private val rawPluginPattern = Regex("""(?m)^\s*(id|kotlin)\(\s*"""")
private val literalCoordinatePattern = Regex(""""[\w.-]+:[\w.-]+:[\w.+-]+"""")
private val designSystemLibraries = mapOf(
    "libs.androidx.compose.material3" to setOf("app", "core/design-system/phone"),
    "libs.androidx.tv.material" to setOf("core/design-system/tv"),
    "libs.androidx.compose.material.icons.extended" to setOf("core/design-system/foundation"),
)
private val namespacePattern = Regex("""\bnamespace\s*=\s*"([^"]+)"""")

@DisableCachingByDefault(because = "The task validates the live repository structure without producing outputs.")
abstract class ValidateArchitectureTask : DefaultTask() {

    @get:Internal
    abstract val repositoryDirectory: DirectoryProperty

    @get:Input
    abstract val modules: MapProperty<String, String>

    init {
        group = "verification"
        description = "Validates the module graph, build file hygiene, and source set structure."
    }

    @TaskAction
    fun validate() {
        val rootDirectory = repositoryDirectory.asFile.get()
        val moduleDirectories = modules.get().values.sorted()
        val directoryByAccessor = moduleDirectories.associateBy(::accessorOf)
        val errors = mutableListOf<String>()

        moduleDirectories.forEach { moduleDirectory ->
            val buildFile = rootDirectory.resolve(moduleDirectory).resolve("build.gradle.kts")
            val buildScript = buildFile.readText()
            val displayPath = "$moduleDirectory/build.gradle.kts"

            projectAccessorPattern.findAll(buildScript)
                .map { match -> match.groupValues[1] }
                .mapNotNull(directoryByAccessor::get)
                .distinct()
                .forEach { dependency ->
                    moduleGraphViolation(moduleDirectory, dependency)?.let { violation ->
                        errors += "$displayPath: $violation"
                    }
                }

            buildHygieneViolations(buildScript, moduleDirectory).forEach { violation ->
                errors += "$displayPath: $violation"
            }
            structureViolations(rootDirectory.resolve(moduleDirectory), moduleDirectory).forEach { violation ->
                errors += "$moduleDirectory: $violation"
            }
        }

        if (errors.isNotEmpty()) {
            throw GradleException(
                errors.joinToString(prefix = "Architecture validation failed:\n- ", separator = "\n- "),
            )
        }

        logger.lifecycle("Validated architecture of ${moduleDirectories.size} Gradle modules.")
    }
}

private fun accessorOf(moduleDirectory: String): String = moduleDirectory
    .split('/')
    .joinToString(".") { segment ->
        segment.split('-').mapIndexed { index, part ->
            if (index == 0) part else part.replaceFirstChar(Char::uppercaseChar)
        }.joinToString("")
    }

private fun moduleGraphViolation(source: String, target: String): String? {
    val sourceFeature = source.featureName()
    val targetFeature = target.featureName()
    val targetLayer = target.featureLayer()
    return when {
        source.featureLayer() == "api" ->
            "A1: feature api modules hold only NavKeys and depend on nothing, found $target."

        source.startsWith("core/") && targetFeature != null ->
            "A2: core modules never depend on feature modules, found $target."

        source.featureLayer() == "domain" && (targetLayer in setOf("phone", "tv") || (targetFeature != null && targetFeature != sourceFeature)) ->
            "A4: feature domain modules depend only on their own api and core modules, found $target."

        sourceFeature != null && targetFeature != null && targetFeature != sourceFeature && targetLayer != "api" ->
            "A3: features reach other features only through their api module, found $target."

        source.isTvModule() && target.isPhoneModule() ->
            "A5: TV modules never depend on phone modules, found $target."

        source.isPhoneModule() && target.isTvModule() ->
            "A6: phone modules never depend on TV modules, found $target."

        else -> null
    }
}

private fun buildHygieneViolations(buildScript: String, moduleDirectory: String): List<String> = buildList {
    forbiddenBuildTokenPattern.find(buildScript)?.let { match ->
        add("B1: '${match.value}' comes from AndroidSdk.kt or Kotlin.kt in build-logic, never a module build file.")
    }
    if (stringProjectPattern.containsMatchIn(buildScript)) {
        add("B2: declare project dependencies with typesafe accessors such as projects.core.common.")
    }
    if (rawPluginPattern.containsMatchIn(buildScript)) {
        add("B3: apply plugins with alias(libs.plugins.*), never id(...) or kotlin(...).")
    }
    literalCoordinatePattern.find(buildScript)?.let { match ->
        add("B4: ${match.value} must be a gradle/libs.versions.toml entry.")
    }
    designSystemLibraries
        .filterValues { allowedModules -> moduleDirectory !in allowedModules }
        .keys
        .filter { library -> Regex("""\b${Regex.escape(library)}\b""").containsMatchIn(buildScript) }
        .forEach { library -> add("B5: only the design system depends on $library; use its wrappers.") }
    namespacePattern.find(buildScript)?.groupValues?.get(1)?.let { namespace ->
        val expected = expectedNamespace(moduleDirectory)
        if (namespace != expected) {
            add("S2: namespace '$namespace' must be '$expected'.")
        }
    }
}

private fun expectedNamespace(moduleDirectory: String): String {
    val segments = moduleDirectory.split('/').let { segments ->
        if (segments.first() == "app") segments.drop(1) else segments
    }
    return (listOf(ROOT_NAMESPACE) + segments.map { segment -> segment.replace("-", "") }).joinToString(".")
}

private fun structureViolations(moduleRoot: File, moduleDirectory: String): List<String> = listOf("test", "testFixtures")
    .filter { sourceSet -> moduleRoot.resolve("src/$sourceSet").isDirectory && !moduleDirectory.mayHaveUnitTests() }
    .map { sourceSet -> "S1: src/$sourceSet is allowed only in core and feature domain modules." }

private fun String.featureName(): String? = split('/').takeIf { segments ->
    segments.size == 3 && segments[0] == "feature"
}?.get(1)

private fun String.featureLayer(): String? = split('/').takeIf { segments ->
    segments.size == 3 && segments[0] == "feature"
}?.get(2)

private fun String.isTvModule(): Boolean = this == "tv" || this == "core/design-system/tv" || featureLayer() == "tv"

private fun String.isPhoneModule(): Boolean = this == "app" || this == "core/design-system/phone" || featureLayer() == "phone"

private fun String.mayHaveUnitTests(): Boolean = startsWith("core/") || featureLayer() == "domain"
