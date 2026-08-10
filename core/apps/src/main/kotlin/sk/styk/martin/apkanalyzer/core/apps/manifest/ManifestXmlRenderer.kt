package sk.styk.martin.apkanalyzer.core.apps.manifest

import android.content.res.Resources

internal interface ManifestXmlRenderer {
    fun parse(resources: Resources): ManifestXmlDocument

    fun render(document: ManifestXmlDocument): String
}

internal data class ManifestXmlDocument(val nodes: List<ManifestXmlNode>)

internal sealed interface ManifestXmlNode {
    data class StartTag(
        val depth: Int,
        val name: String,
        val attributes: List<ManifestXmlAttribute>,
    ) : ManifestXmlNode

    data class EndTag(val depth: Int, val name: String) : ManifestXmlNode

    data class Text(val depth: Int, val value: String) : ManifestXmlNode
}

internal data class ManifestXmlAttribute(
    val prefix: String?,
    val name: String,
    val value: String,
)
