package sk.styk.martin.apkanalyzer.core.appaidescription.generation

import sk.styk.martin.apkanalyzer.core.appaidescription.AppAiDescription
import javax.inject.Inject

private val jsonFieldPattern = { key: String -> Regex("\"$key\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"") }

internal class DescriptionParser @Inject constructor() {

    fun parse(rawOutput: String): AppAiDescription? {
        val jsonObject = extractJsonObject(rawOutput) ?: return null
        val short = extractField(jsonObject, "short")
        val long = extractField(jsonObject, "long")
        return if (short == null || long == null) null else AppAiDescription(shortDescription = short, longDescription = long)
    }

    private fun extractJsonObject(raw: String): String? {
        val fenceStripped = raw.replace("```json", "", ignoreCase = true).replace("```", "").trim()
        val start = fenceStripped.indexOf('{')
        val end = fenceStripped.lastIndexOf('}')
        if (start == -1 || end == -1 || end < start) return null
        return fenceStripped.substring(start, end + 1)
    }

    private fun extractField(json: String, key: String): String? {
        val match = jsonFieldPattern(key).find(json) ?: return null
        return unescape(match.groupValues[1]).trim().takeIf { it.isNotEmpty() }
    }

    private fun unescape(value: String): String = value
        .replace("\\n", " ")
        .replace("\\\"", "\"")
        .replace("\\\\", "\\")
}
