package sk.styk.martin.apkanalyzer.core.appaidescription.generation

import sk.styk.martin.apkanalyzer.core.appaidescription.AppAiContext
import javax.inject.Inject

internal const val PROMPT_VERSION = 1

internal class PromptBuilder @Inject constructor() {

    fun build(context: AppAiContext, strict: Boolean): String = buildString {
        appendLine("You are generating a factual description of an Android application.")
        appendLine()
        appendLine("Your task is to explain what the application is for based ONLY on")
        appendLine("the information provided below.")
        appendLine()
        appendLine("APPLICATION INFORMATION")
        appendLine()
        appendLine("App name:")
        appendLine(context.appName)
        appendLine()
        appendLine("Package name:")
        appendLine(context.packageName)
        appendLine()
        appendLine("Target SDK:")
        appendLine(context.targetSdk?.toString() ?: "unknown")
        appendLine()
        appendLine("Requested permissions:")
        appendLine(context.permissions.joinToString(", ").ifEmpty { "none" })
        appendLine()
        appendLine("Activities:")
        appendLine(context.activities.joinToString(", ").ifEmpty { "none" })
        appendLine()
        appendLine("Services:")
        appendLine(context.services.joinToString(", ").ifEmpty { "none" })
        appendLine()
        appendLine("Broadcast receivers:")
        appendLine(context.receivers.joinToString(", ").ifEmpty { "none" })
        appendLine()
        appendLine("RULES")
        appendLine()
        appendLine("1. Do not invent functionality.")
        appendLine("2. Do not assume that a permission proves a feature exists.")
        appendLine("3. Do not mention permissions in the user-facing description.")
        appendLine("4. Do not mention package names or Android implementation details.")
        appendLine("5. Describe the primary purpose of the application.")
        appendLine("6. Use conservative wording when the information is insufficient.")
        appendLine("7. Do not use marketing language.")
        appendLine("8. Do not claim specific features unless supported by the input.")
        appendLine("9. Do not mention the prompt or the AI model.")
        appendLine("10. Never repeat any permission name, package name or class name from the input.")
        appendLine("11. Write plain sentences a non-technical person understands.")
        appendLine("12. Output only valid JSON.")
        if (strict) {
            appendLine("13. Output ONLY the JSON object and nothing else - no explanation, no markdown fences.")
        }
        appendLine()
        appendLine("OUTPUT FORMAT")
        appendLine()
        appendLine("{")
        appendLine("  \"short\": \"One sentence, maximum 25 words.\",")
        appendLine("  \"long\": \"Two or three sentences, maximum 70 words.\"")
        appendLine("}")
    }
}
