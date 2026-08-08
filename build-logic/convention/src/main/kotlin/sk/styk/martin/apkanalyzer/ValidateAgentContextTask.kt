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

@DisableCachingByDefault(because = "The task validates the live repository context without producing outputs.")
abstract class ValidateAgentContextTask : DefaultTask() {

    @get:Internal
    abstract val repositoryDirectory: DirectoryProperty

    @get:Input
    abstract val modules: MapProperty<String, String>

    init {
        group = "verification"
        description = "Validates repository AI context structure, metadata, links, and module coverage."
    }

    @TaskAction
    fun validate() {
        val rootDirectory = repositoryDirectory.asFile.get()
        val errors = mutableListOf<String>()
        val ignoredDirectoryNames = setOf(".git", ".gradle", ".idea", "build", "out")

        fun File.displayPath(): String = relativeTo(rootDirectory).invariantSeparatorsPath

        fun filesNamed(fileName: String): List<File> = rootDirectory
            .walkTopDown()
            .onEnter { directory ->
                directory == rootDirectory || directory.name !in ignoredDirectoryNames
            }.filter { file -> file.isFile && file.name == fileName }
            .toList()

        val agentFiles = filesNamed("AGENTS.md")
        val claudeFiles = filesNamed("CLAUDE.md")

        agentFiles.forEach { agentFile ->
            val claudeFile = agentFile.resolveSibling("CLAUDE.md")
            when {
                !claudeFile.isFile -> errors += "Missing ${claudeFile.displayPath()} for ${agentFile.displayPath()}."

                claudeFile.readText().trim() != "@AGENTS.md" ->
                    errors += "${claudeFile.displayPath()} must contain only @AGENTS.md."
            }
        }

        claudeFiles.forEach { claudeFile ->
            if (!claudeFile.resolveSibling("AGENTS.md").isFile) {
                errors += "${claudeFile.displayPath()} has no adjacent AGENTS.md."
            }
        }

        val normalizedRoot = rootDirectory.toPath().toAbsolutePath().normalize()
        modules.get().forEach { (modulePath, relativeModuleDirectory) ->
            var directory = rootDirectory.resolve(relativeModuleDirectory)
            var closestAgentFile: File? = null

            while (directory.toPath().toAbsolutePath().normalize().startsWith(normalizedRoot)) {
                val candidate = directory.resolve("AGENTS.md")
                if (candidate.isFile) {
                    closestAgentFile = candidate
                    break
                }
                if (directory == rootDirectory) break
                directory = directory.parentFile
            }

            if (closestAgentFile == null || closestAgentFile == rootDirectory.resolve("AGENTS.md")) {
                errors += "$modulePath has no module-scoped AGENTS.md."
            }
        }

        val skillsDirectory = rootDirectory.resolve(".claude/skills")
        val skillDirectories = skillsDirectory
            .listFiles()
            .orEmpty()
            .filter(File::isDirectory)
            .sortedBy(File::getName)
        val skillFiles = mutableListOf<File>()
        val skillNamePattern = Regex("[a-z0-9-]{1,64}")
        val frontmatterPattern = Regex(
            pattern = """\A---\r?\n(.*?)\r?\n---(?:\r?\n|$)""",
            option = RegexOption.DOT_MATCHES_ALL,
        )

        for (skillDirectory in skillDirectories) {
            val skillFile = skillDirectory.resolve("SKILL.md")
            if (!skillFile.isFile) {
                errors += "Missing ${skillFile.displayPath()}."
                continue
            }
            skillFiles += skillFile

            val frontmatter = frontmatterPattern.find(skillFile.readText())?.groupValues?.get(1)
            if (frontmatter == null) {
                errors += "${skillFile.displayPath()} has invalid YAML frontmatter."
                continue
            }

            fun metadataValue(key: String): String? = Regex("""(?m)^$key:\s*(.+?)\s*$""")
                .find(frontmatter)
                ?.groupValues
                ?.get(1)
                ?.trim()
                ?.trim('"', '\'')

            val name = metadataValue("name")
            val description = metadataValue("description")

            if (name != skillDirectory.name) {
                errors += "${skillFile.displayPath()} name must match directory ${skillDirectory.name}."
            }
            if (name == null || !skillNamePattern.matches(name)) {
                errors += "${skillFile.displayPath()} has an invalid skill name."
            }
            if (description.isNullOrBlank() || description.length > 1024) {
                errors += "${skillFile.displayPath()} needs a description of 1 to 1024 characters."
            }
        }

        val skillNames = skillDirectories.map(File::getName).toSet()
        listOf(".github/skills", ".agents/skills").forEach { duplicateLocation ->
            val duplicates = rootDirectory
                .resolve(duplicateLocation)
                .listFiles()
                .orEmpty()
                .filter(File::isDirectory)
                .map(File::getName)
                .filter(skillNames::contains)
            if (duplicates.isNotEmpty()) {
                errors += "$duplicateLocation duplicates shared skills: ${duplicates.sorted().joinToString()}."
            }
        }

        val duplicatePrompts = rootDirectory
            .resolve(".github/prompts")
            .listFiles()
            .orEmpty()
            .filter { file -> file.isFile && file.name.endsWith(".prompt.md") }
            .map { file -> file.name.removeSuffix(".prompt.md") }
            .filter(skillNames::contains)
        if (duplicatePrompts.isNotEmpty()) {
            errors += ".github/prompts duplicates shared skills: ${duplicatePrompts.sorted().joinToString()}."
        }

        val copilotInstructions = rootDirectory.resolve(".github/copilot-instructions.md")
        if (!copilotInstructions.isFile) {
            errors += "Missing .github/copilot-instructions.md."
        }

        val contextFiles = (
            agentFiles +
                claudeFiles +
                skillFiles +
                listOf(rootDirectory.resolve("README.md"), copilotInstructions)
            ).filter(File::isFile)
            .distinct()
        val markdownLinkPattern = Regex("""(?<!!)\[[^]]+]\(([^)]+)\)""")

        contextFiles.forEach { contextFile ->
            for (match in markdownLinkPattern.findAll(contextFile.readText())) {
                val target = match.groupValues[1]
                if (
                    target.startsWith("http://") ||
                    target.startsWith("https://") ||
                    target.startsWith("mailto:") ||
                    target.startsWith("#")
                ) {
                    continue
                }

                val path = target.substringBefore('#')
                if (path.isNotBlank() && !contextFile.parentFile.resolve(path).exists()) {
                    errors += "${contextFile.displayPath()} contains a broken local link: $target."
                }
            }
        }

        if (errors.isNotEmpty()) {
            throw GradleException(
                errors.joinToString(
                    prefix = "Agent context validation failed:\n- ",
                    separator = "\n- ",
                ),
            )
        }

        logger.lifecycle(
            "Validated ${agentFiles.size} AGENTS/CLAUDE pairs, " +
                "${modules.get().size} Gradle modules, and ${skillFiles.size} shared skills.",
        )
    }
}
