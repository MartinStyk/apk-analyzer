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
private val material3Pattern = Regex("""\blibs\.androidx\.compose\.material3\b""")
private val namespacePattern = Regex("""\bnamespace\s*=\s*"([^"]+)"""")
private val baselineFilePattern = Regex(""".*baseline.*\.xml""", RegexOption.IGNORE_CASE)
private val ignoredDirectoryNames = setOf(".git", ".gradle", ".idea", "build", "out", "node_modules")

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

            if (moduleDirectory.isTvModule() && material3Pattern.containsMatchIn(buildScript)) {
                errors += "$displayPath: A5: TV modules never depend on phone Material3."
            }
            buildHygieneViolations(buildScript, moduleDirectory).forEach { violation ->
                errors += "$displayPath: $violation"
            }
            structureViolations(rootDirectory.resolve(moduleDirectory), moduleDirectory).forEach { violation ->
                errors += "$moduleDirectory: $violation"
            }
        }

        rootDirectory.walkTopDown()
            .onEnter { directory -> directory == rootDirectory || directory.name !in ignoredDirectoryNames }
            .filter { file -> file.isFile && baselineFilePattern.matches(file.name) }
            .forEach { file ->
                errors += "${file.relativeTo(rootDirectory).invariantSeparatorsPath}: B5: " +
                    "Lint and detekt baselines are not allowed. Fix the finding instead."
            }

        if (errors.isNotEmpty()) {
            throw GradleException(
                errors.joinToString(prefix = "Architecture validation failed:\n- ", separator = "\n- "),
            )
        }

        logger.lifecycle("Validated architecture of ${moduleDirectories.size} Gradle modules.")
    }
}

internal fun accessorOf(moduleDirectory: String): String = moduleDirectory
    .split('/')
    .joinToString(".") { segment ->
        segment.split('-').mapIndexed { index, part ->
            if (index == 0) part else part.replaceFirstChar(Char::uppercaseChar)
        }.joinToString("")
    }

@Suppress("CyclomaticComplexMethod")
internal fun moduleGraphViolation(source: String, target: String): String? {
    val sourceFeature = source.featureName()
    val targetFeature = target.featureName()
    val targetLayer = target.featureLayer()
    return when {
        source.featureLayer() == "api" ->
            "A1: feature api modules hold only NavKeys and depend on nothing, found $target."

        source.startsWith("core/") && targetFeature != null ->
            "A2: core modules never depend on feature modules, found $target."

        source.featureLayer() == "domain" && targetLayer in setOf("phone", "tv") ->
            "A4: feature domain modules never depend on UI modules, found $target."

        sourceFeature != null && targetFeature != null && targetFeature != sourceFeature && targetLayer != "api" ->
            "A3: features reach other features only through their api module, found $target."

        source.isTvModule() && (target == "core/design-system/phone" || targetLayer == "phone") ->
            "A5: TV modules never depend on phone modules, found $target."

        source.isPhoneModule() && (target.isTvModule() || target.featureLayer() == "tv") ->
            "A6: phone modules never depend on TV modules, found $target."

        targetLayer == "phone" && source != "app" ->
            "A7: only app assembles feature phone modules, found $target."

        targetLayer == "tv" && source != "tv" ->
            "A7: only tv assembles feature TV modules, found $target."

        else -> null
    }
}

internal fun buildHygieneViolations(buildScript: String, moduleDirectory: String): List<String> = buildList {
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
    namespacePattern.find(buildScript)?.groupValues?.get(1)?.let { namespace ->
        val expected = expectedNamespace(moduleDirectory)
        if (namespace != expected) {
            add("S4: namespace '$namespace' must be '$expected'.")
        }
    }
}

internal fun expectedNamespace(moduleDirectory: String): String {
    val segments = moduleDirectory.split('/').let { segments ->
        if (segments.first() == "app") segments.drop(1) else segments
    }
    return (listOf(ROOT_NAMESPACE) + segments.map { segment -> segment.replace("-", "") }).joinToString(".")
}

private fun structureViolations(moduleRoot: File, moduleDirectory: String): List<String> = buildList {
    val sourceSets = moduleRoot.resolve("src").listFiles().orEmpty().filter(File::isDirectory)
    sourceSets.forEach { sourceSet ->
        val layouts = sourceSet.resolve("res").listFiles().orEmpty().filter { file ->
            file.isDirectory && file.name.startsWith("layout")
        }
        if (layouts.isNotEmpty()) {
            add("S1: src/${sourceSet.name}/res/${layouts.first().name} is an XML layout. Use Compose.")
        }
        when (sourceSet.name) {
            "test", "testFixtures" -> if (!moduleDirectory.mayHaveUnitTests()) {
                add("S2: src/${sourceSet.name} is allowed only in core and feature domain modules.")
            }

            "androidTest" -> add("S3: src/androidTest is not allowed. Instrumentation tests need an explicit request.")
        }
    }
}

private fun String.featureName(): String? = split('/').takeIf { segments ->
    segments.size == 3 && segments[0] == "feature"
}?.get(1)

private fun String.featureLayer(): String? = split('/').takeIf { segments ->
    segments.size == 3 && segments[0] == "feature"
}?.get(2)

private fun String.isTvModule(): Boolean = this == "tv" || this == "core/design-system/tv" || featureLayer() == "tv"

private fun String.isPhoneModule(): Boolean = this == "app" || this == "core/design-system/phone" || featureLayer() == "phone"

private fun String.mayHaveUnitTests(): Boolean = startsWith("core/") || featureLayer() == "domain"
