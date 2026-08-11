package sk.styk.martin.apkanalyzer.core.appaidescription.generation

import sk.styk.martin.apkanalyzer.core.appaidescription.AppAiDescription
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

    fun isValid(description: AppAiDescription): Boolean =
        isValidField(description.shortDescription, MAX_SHORT_WORDS) && isValidField(description.longDescription, MAX_LONG_WORDS)

    private fun isValidField(text: String, maxWords: Int): Boolean = text.isNotBlank() && wordCount(text) <= maxWords && !containsLeakage(text)

    private fun wordCount(text: String): Int = text.trim().split(Regex("\\s+")).size

    private fun containsLeakage(text: String): Boolean {
        val lower = text.lowercase()
        return leakagePhrases.any { lower.contains(it) }
    }
}
