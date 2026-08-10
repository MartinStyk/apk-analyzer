package sk.styk.martin.apkanalyzer.core.apps.manifest

import android.content.res.Resources
import android.content.res.XmlResourceParser
import javax.inject.Inject

internal class ManifestXmlRendererImpl @Inject constructor() : ManifestXmlRenderer {

    override fun parse(resources: Resources): ManifestXmlDocument {
        val nodes = mutableListOf<ManifestXmlNode>()
        resources.assets.openXmlResourceParser(MANIFEST_FILE_NAME).use { parser ->
            var eventType = parser.eventType
            while (eventType != XmlResourceParser.END_DOCUMENT) {
                when (eventType) {
                    XmlResourceParser.START_TAG -> nodes += parser.toStartTag(resources)

                    XmlResourceParser.END_TAG -> nodes += ManifestXmlNode.EndTag(
                        depth = parser.depth,
                        name = parser.name,
                    )

                    XmlResourceParser.TEXT ->
                        parser.text
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }
                            ?.let { nodes += ManifestXmlNode.Text(depth = parser.depth, value = it) }
                }
                eventType = parser.next()
            }
        }
        return ManifestXmlDocument(nodes)
    }

    override fun render(document: ManifestXmlDocument): String {
        val output = StringBuilder()
        document.nodes.forEach { node ->
            when (node) {
                is ManifestXmlNode.StartTag -> appendStartTag(output, node)
                is ManifestXmlNode.EndTag -> appendEndTag(output, node)
                is ManifestXmlNode.Text -> appendText(output, node)
            }
        }
        return output.toString().trim().takeIf { it.isNotEmpty() }
            ?: error("Manifest is empty")
    }

    private fun appendStartTag(output: StringBuilder, node: ManifestXmlNode.StartTag) {
        output.appendIndent(node.depth - 1)
            .append('<')
            .append(node.name)

        if (node.depth == 1) {
            output.append("\n")
                .appendIndent(node.depth)
                .append("xmlns:")
                .append(ANDROID_PREFIX)
                .append("=\"")
                .append(ANDROID_NAMESPACE)
                .append('"')
        }

        node.attributes.forEach { attribute ->
            output.append("\n")
                .appendIndent(node.depth)
                .appendQualifiedName(
                    prefix = attribute.prefix,
                    name = attribute.name,
                )
                .append("=\"")
                .append(attribute.value.escapeXml())
                .append('"')
        }
        output.append(">\n")
    }

    private fun appendEndTag(output: StringBuilder, node: ManifestXmlNode.EndTag) {
        output.appendIndent(node.depth - 1)
            .append("</")
            .append(node.name)
            .append(">\n")
    }

    private fun appendText(output: StringBuilder, node: ManifestXmlNode.Text) {
        output.appendIndent(node.depth)
            .append(node.value.escapeXml())
            .append('\n')
    }

    private fun XmlResourceParser.toStartTag(resources: Resources): ManifestXmlNode.StartTag = ManifestXmlNode.StartTag(
        depth = depth,
        name = name,
        attributes = List(attributeCount) { index ->
            ManifestXmlAttribute(
                prefix = ANDROID_PREFIX.takeIf { getAttributeNameResource(index) != 0 },
                name = getAttributeName(index),
                value = attributeValue(resources, index),
            )
        },
    )

    private fun XmlResourceParser.attributeValue(resources: Resources, index: Int): String {
        val resourceId = getAttributeResourceValue(index, 0)
        if (resourceId != 0) {
            return runCatching { "@${resources.getResourceName(resourceId)}" }
                .getOrDefault(getAttributeValue(index))
        }
        return getAttributeValue(index)
    }
}

private fun StringBuilder.appendIndent(depth: Int): StringBuilder = append(INDENT.repeat(depth.coerceAtLeast(0)))

private fun StringBuilder.appendQualifiedName(prefix: String?, name: String): StringBuilder {
    if (!prefix.isNullOrEmpty()) {
        append(prefix).append(':')
    }
    return append(name)
}

private fun String.escapeXml(): String = replace("&", "&amp;")
    .replace("\"", "&quot;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")

private const val MANIFEST_FILE_NAME = "AndroidManifest.xml"
private const val INDENT = "  "
private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
private const val ANDROID_PREFIX = "android"
