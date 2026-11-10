package sk.styk.martin.apkanalyzer.core.appindex.browse

import sk.styk.martin.apkanalyzer.core.appindex.model.AppAttributeIndex

sealed interface BrowseOption {
    val key: String
    val label: String
    val count: Int

    data class Labeled(
        override val key: String,
        override val label: String,
        val rawIdentifier: String?,
        override val count: Int,
    ) : BrowseOption

    data class CertificateHash(
        override val key: String,
        override val label: String,
        val algorithm: BrowseSubAttribute,
        override val count: Int,
        val publisher: String?,
    ) : BrowseOption
}

fun AppAttributeIndex.browseOptions(
    dimension: BrowseDimension,
    subAttribute: BrowseSubAttribute?,
    labeler: BrowseDimensionLabeler,
): List<BrowseOption> = bucketsFor(dimension, subAttribute)
    .map { (key, packages) ->
        val label = labeler.label(dimension, key, subAttribute)
        if (subAttribute != null && subAttribute.isCertificateHash) {
            BrowseOption.CertificateHash(
                key = key,
                label = label,
                algorithm = subAttribute,
                count = packages.size,
                publisher = certificateByHash[key]?.subject?.displayName,
            )
        } else {
            BrowseOption.Labeled(
                key = key,
                label = label,
                rawIdentifier = labeler.rawIdentifier(dimension, key, subAttribute),
                count = packages.size,
            )
        }
    }
    .sortedWith(compareByDescending<BrowseOption> { it.count }.thenBy { it.label })

fun BrowseOption.matches(query: String): Boolean = query.isBlank() ||
    key.contains(query, ignoreCase = true) ||
    label.contains(query, ignoreCase = true) ||
    (this as? BrowseOption.CertificateHash)?.matchesHash(query) == true ||
    (this as? BrowseOption.Labeled)?.rawIdentifier?.contains(query, ignoreCase = true) == true

private fun BrowseOption.CertificateHash.matchesHash(query: String): Boolean {
    val hexQuery = query.replace(":", "")
    return publisher?.contains(query, ignoreCase = true) == true ||
        (hexQuery.isNotEmpty() && key.replace(":", "").contains(hexQuery, ignoreCase = true))
}

fun BrowseDimension.subAttributesOrDefault(): List<BrowseSubAttribute?> = subAttributes().takeIf { it.isNotEmpty() } ?: listOf(null)
