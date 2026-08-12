package sk.styk.martin.apkanalyzer.core.appaidescription.generation

import sk.styk.martin.apkanalyzer.core.appaidescription.AppAiContext
import sk.styk.martin.apkanalyzer.core.appaidescription.AppAiDescription
import sk.styk.martin.apkanalyzer.core.appaidescription.identifiers
import javax.inject.Inject

private const val MAX_SHORT_WORDS = 25
private const val MAX_LONG_WORDS = 70

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
        return isValidField(description.shortDescription, MAX_SHORT_WORDS, contextIdentifiers) &&
            isValidField(description.longDescription, MAX_LONG_WORDS, contextIdentifiers)
    }

    private fun isValidField(text: String, maxWords: Int, contextIdentifiers: List<String>): Boolean {
        val lowercaseText = text.lowercase()
        return text.isNotBlank() &&
            wordCount(text) <= maxWords &&
            !containsLeakage(lowercaseText) &&
            !containsContextIdentifier(lowercaseText, contextIdentifiers)
    }

    private fun wordCount(text: String): Int = text.trim().split(Regex("\\s+")).size

    private fun containsLeakage(lowercaseText: String): Boolean = leakagePhrases.any { lowercaseText.contains(it) }

    private fun containsContextIdentifier(lowercaseText: String, contextIdentifiers: List<String>): Boolean =
        contextIdentifiers.any { lowercaseText.contains(it) }
}
