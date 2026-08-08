package sk.styk.martin.apkanalyzer.feature.apps.impl.search.domain

import sk.styk.martin.apkanalyzer.feature.apps.impl.list.AppListItem
import javax.inject.Inject
import kotlin.math.min

class SearchAppsUseCase @Inject constructor() {

    operator fun invoke(query: String, apps: List<AppListItem>): List<AppListItem> {
        if (query.isBlank()) return emptyList()

        val normalizedQuery = query.lowercase()

        return apps
            .mapNotNull { app ->
                val score = bestScore(normalizedQuery, app)
                if (score <= MAX_SCORE_THRESHOLD) app to score else null
            }
            .sortedBy { it.second }
            .map { it.first }
    }

    private fun bestScore(query: String, app: AppListItem): Float {
        val nameScore = scoreField(query, app.applicationName.lowercase())
        val packageScore = scoreField(query, app.packageName.value.lowercase())
        return min(nameScore, packageScore)
    }

    private fun scoreField(query: String, field: String): Float = when {
        field.contains(query) -> SCORE_EXACT_CONTAINS

        field.split('.', ' ', '-', '_').any { it.startsWith(query) } -> SCORE_WORD_PREFIX

        else -> {
            val minWindowDistance = slidingWindowDistance(query, field)
            val normalized = minWindowDistance.toFloat() / query.length
            SCORE_FUZZY_BASE + normalized
        }
    }

    @Suppress("ReturnCount")
    private fun slidingWindowDistance(query: String, text: String): Int {
        if (query.length > text.length) return levenshtein(query, text)

        var best = query.length
        val windowSize = min(text.length, query.length + MAX_ALLOWED_EDITS)
        for (start in 0..(text.length - query.length)) {
            val end = min(start + windowSize, text.length)
            val window = text.substring(start, end)
            val distance = levenshtein(query, window)
            best = min(best, distance)
            if (best == 0) return 0
        }
        return best
    }

    private companion object {
        const val SCORE_EXACT_CONTAINS = 0.0f
        const val SCORE_WORD_PREFIX = 0.1f
        const val SCORE_FUZZY_BASE = 0.5f
        const val MAX_SCORE_THRESHOLD = 0.85f
        const val MAX_ALLOWED_EDITS = 3
    }
}

@Suppress("ReturnCount")
internal fun levenshtein(a: String, b: String): Int {
    val m = a.length
    val n = b.length

    if (m == 0) return n
    if (n == 0) return m

    var previousRow = IntArray(n + 1) { it }
    var currentRow = IntArray(n + 1)

    for (i in 1..m) {
        currentRow[0] = i
        for (j in 1..n) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            currentRow[j] = minOf(
                currentRow[j - 1] + 1,
                previousRow[j] + 1,
                previousRow[j - 1] + cost,
            )
        }
        val temp = previousRow
        previousRow = currentRow
        currentRow = temp
    }
    return previousRow[n]
}
