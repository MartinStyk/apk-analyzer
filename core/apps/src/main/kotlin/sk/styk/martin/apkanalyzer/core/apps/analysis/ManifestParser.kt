package sk.styk.martin.apkanalyzer.core.apps.analysis

import android.content.pm.PackageManager
import android.content.res.Resources
import android.content.res.XmlResourceParser
import androidx.core.text.htmlEncode
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import java.io.ByteArrayInputStream
import java.io.StringWriter
import javax.inject.Inject
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

class ManifestParser @Inject constructor(private val packageManager: PackageManager) {
    fun loadAndroidManifest(packageName: String, packagePath: String?): String {
        val manifest = readManifest(packageManager, packageName, packagePath)
        return formatManifest(manifest)
    }

    private fun readManifest(
        packageManager: PackageManager,
        packageName: String,
        packagePath: String?,
    ): String {
        val stringBuilder = StringBuilder()
        try {
            val apkResources =
                try {
                    packageManager.getResourcesForApplication(packageName)
                } catch (exception: PackageManager.NameNotFoundException) {
                    packagePath?.let {
                        packageManager.getPackageArchiveInfo(it, 0)?.applicationInfo?.let {
                            packageManager.getResourcesForApplication(it)
                        }
                    }
                }

            if (apkResources == null) {
                Logger.w(TAG, "Resources for package $packageName not found")
                return ""
            }

            val parser = apkResources.assets.openXmlResourceParser("AndroidManifest.xml")

            var eventType: Int = parser.next()

            while (eventType != XmlResourceParser.END_DOCUMENT) {
                if (eventType == XmlResourceParser.START_TAG) {
                    stringBuilder.append("<").append(parser.name)

                    for (attribute in 0 until parser.attributeCount) {
                        val attributeName = parser.getAttributeName(attribute)
                        val attributeValue = getAttributeValue(attributeName, parser.getAttributeValue(attribute), apkResources)

                        stringBuilder
                            .append(" ")
                            .append(attributeName)
                            .append("=\"")
                            .append(attributeValue)
                            .append("\"")
                    }

                    stringBuilder.append(">")

                    if (parser.text != null) {
                        stringBuilder.append(parser.text)
                    }
                } else if (eventType == XmlResourceParser.END_TAG) {
                    stringBuilder.append("</").append(parser.name).append(">")
                }

                eventType = parser.next()
            }
        } catch (e: Exception) {
            Logger.e(TAG, e, "Error parsing manifest for $packageName")
        }

        return stringBuilder.toString()
    }

    private fun formatManifest(manifest: String): String = try {
        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        val result = StreamResult(StringWriter())
        val source = StreamSource(ByteArrayInputStream(manifest.toByteArray(charset("UTF-8"))))
        transformer.transform(source, result)
        result.writer.toString()
    } catch (e: Exception) {
        Logger.e(TAG, e, "Error formatting manifest $manifest")
        ""
    }

    private fun getAttributeValue(
        attributeName: String,
        attributeValue: String,
        resources: Resources,
    ): String {
        if (attributeValue.startsWith("@")) {
            try {
                val id = Integer.valueOf(attributeValue.substring(1))

                val value: String = when (attributeName) {
                    "theme", "resource" -> resources.getResourceEntryName(id)
                    else -> resources.getString(id)
                }

                return value.htmlEncode()
            } catch (e: Exception) {
                Logger.w(TAG, e, "Error reading attribute value $attributeName, $attributeValue")
            }
        }
        return attributeValue
    }

    companion object {
        private const val TAG = "ManifestParser"
    }
}
