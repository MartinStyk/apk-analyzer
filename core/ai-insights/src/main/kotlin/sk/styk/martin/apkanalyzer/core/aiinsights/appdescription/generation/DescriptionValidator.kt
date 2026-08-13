package sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.generation

import sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.AppAiContext
import sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.AppAiDescription
import sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.identifiers
import javax.inject.Inject

private const val MAX_WORDS = 70

private val leakagePhrases = listOf(
    "as an ai",
    "as a language model",
    "according to the prompt",
    "i cannot determine",
    "i'm unable to",
    "packagemanager",
    "gemini",
    "on-device model",
)

internal class DescriptionValidator @Inject constructor() {

    fun isValid(description: AppAiDescription, context: AppAiContext): Boolean {
        val contextIdentifiers = context.identifiers().map { it.lowercase() }
        val lowercaseText = description.description.lowercase()
        return description.description.isNotBlank() &&
            wordCount(description.description) <= MAX_WORDS &&
            !containsLeakage(lowercaseText) &&
            !containsContextIdentifier(lowercaseText, contextIdentifiers)
    }

    private fun wordCount(text: String): Int = text.trim().split(Regex("\\s+")).size

    private fun containsLeakage(lowercaseText: String): Boolean = leakagePhrases.any { lowercaseText.contains(it) }

    private fun containsContextIdentifier(lowercaseText: String, contextIdentifiers: List<String>): Boolean =
        contextIdentifiers.any { lowercaseText.contains(it) }
}
