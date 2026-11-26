package sk.styk.martin.apkanalyzer.core.appindex.api.browse

import sk.styk.martin.apkanalyzer.core.appindex.api.model.AppAttributeIndex

data class BrowseDimensionSummary(
    val dimension: BrowseDimension,
    val optionCount: Int,
    val topLabels: List<String>,
)

fun AppAttributeIndex.dimensionSummaries(labeler: BrowseDimensionLabeler, topLabelCount: Int): List<BrowseDimensionSummary> =
    BrowseDimension.entries.map { dimension ->
        val previewSubAttribute = if (dimension == BrowseDimension.SigningCertificate) BrowseSubAttribute.CertificateOrganization else null
        BrowseDimensionSummary(
            dimension = dimension,
            optionCount = bucketsFor(dimension).size,
            topLabels = bucketsFor(dimension, previewSubAttribute).entries
                .sortedByDescending { it.value.size }
                .take(topLabelCount)
                .map { labeler.label(dimension, it.key, previewSubAttribute) },
        )
    }
