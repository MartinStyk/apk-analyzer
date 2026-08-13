package sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.generation

import sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.AppAiContext
import javax.inject.Inject

internal class PromptBuilder @Inject constructor() {

    fun build(context: AppAiContext): String = buildString {
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
        appendLine("RULES")
        appendLine()
        appendLine("1. Do not invent functionality.")
        appendLine("2. Do not assume that a permission proves a feature exists.")
        appendLine("3. Do not mention permissions in the user-facing description.")
        appendLine("4. Do not mention package names or Android implementation details.")
        appendLine("5. Write at most three sentences describing the primary purpose of the application,")
        appendLine("   using the permissions only as supporting context, not as a separate topic.")
        appendLine("6. The user already sees the app's name and icon, so never start the description with")
        appendLine("   the app's name, \"This application\", \"This app\", or \"It is\". Start directly with")
        appendLine("   what the app does, for example beginning with a verb like \"Lets users...\",")
        appendLine("   \"Helps you...\", or \"Provides...\".")
        appendLine("7. Use conservative wording when the information is insufficient.")
        appendLine("8. Do not use marketing language.")
        appendLine("9. Do not claim specific features unless supported by the input.")
        appendLine("10. Do not mention the prompt or the AI model.")
        appendLine("11. Never repeat any permission name or package name from the input.")
        appendLine("12. Write plain sentences a non-technical person understands.")
        appendLine("13. Output ONLY the JSON object below and nothing else - no explanation, no markdown fences.")
        appendLine()
        appendLine("OUTPUT FORMAT")
        appendLine()
        appendLine("{")
        appendLine("  \"description\": \"Up to three sentences, maximum 70 words.\"")
        appendLine("}")
    }
}
