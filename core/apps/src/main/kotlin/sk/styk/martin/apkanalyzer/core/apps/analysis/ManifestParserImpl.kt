package sk.styk.martin.apkanalyzer.core.apps.analysis

import android.content.pm.PackageManager
import android.content.res.Resources
import android.content.res.XmlResourceParser
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import javax.inject.Inject

internal class ManifestParserImpl @Inject constructor(private val packageManager: PackageManager, private val dispatcherProvider: DispatcherProvider) : ManifestParser {

    override suspend fun manifest(reference: AppReference): Result<ParsedManifest> = when (reference) {
        is AppReference.InstalledPackage -> installedPackageManifest(reference.packageName)
        is AppReference.ApkFile -> apkFileManifest(reference.path)
    }

    private suspend fun installedPackageManifest(packageName: PackageName): Result<ParsedManifest> = withContext(dispatcherProvider.io()) {
        runCatchingCancellable {
            val applicationInfo = packageManager.getApplicationInfo(packageName.value, 0)
            ParsedManifest(
                xml = readManifest(resourcesForApk(applicationInfo.sourceDir)),
                additionalInstalledSplits = applicationInfo.splitSourceDirs.orEmpty().size,
            )
        }.onFailure {
            Logger.e(TAG, it, "Can not read manifest for installed package $packageName")
        }
    }

    private suspend fun apkFileManifest(apkPath: String): Result<ParsedManifest> = withContext(dispatcherProvider.io()) {
        runCatchingCancellable {
            ParsedManifest(
                xml = readManifest(resourcesForApk(apkPath)),
                additionalInstalledSplits = 0,
            )
        }.onFailure {
            Logger.e(TAG, it, "Can not read manifest from $apkPath")
        }
    }

    private fun resourcesForApk(apkPath: String): Resources {
        val applicationInfo = packageManager.getPackageArchiveInfo(apkPath, 0)
            ?.applicationInfo
            ?: error("Can not read package information from $apkPath")
        applicationInfo.sourceDir = apkPath
        applicationInfo.publicSourceDir = apkPath
        applicationInfo.splitSourceDirs = null
        applicationInfo.splitPublicSourceDirs = null
        return packageManager.getResourcesForApplication(applicationInfo)
    }

    private fun readManifest(resources: Resources): String {
        val output = StringBuilder()
        resources.assets.openXmlResourceParser(MANIFEST_FILE_NAME).use { parser ->
            var eventType = parser.eventType
            while (eventType != XmlResourceParser.END_DOCUMENT) {
                when (eventType) {
                    XmlResourceParser.START_TAG -> appendStartTag(output, parser, resources)
                    XmlResourceParser.END_TAG -> appendEndTag(output, parser)
                    XmlResourceParser.TEXT -> appendText(output, parser)
                }
                eventType = parser.next()
            }
        }
        return output.toString().trim().takeIf { it.isNotEmpty() }
            ?: error("Manifest is empty")
    }

    private fun appendStartTag(
        output: StringBuilder,
        parser: XmlResourceParser,
        resources: Resources,
    ) {
        output.appendIndent(parser.depth - 1)
            .append('<')
            .append(parser.name)

        if (parser.depth == 1) {
            output.append("\n")
                .appendIndent(parser.depth)
                .append("xmlns:")
                .append(ANDROID_PREFIX)
                .append("=\"")
                .append(ANDROID_NAMESPACE)
                .append('"')
        }

        for (index in 0 until parser.attributeCount) {
            output.append("\n")
                .appendIndent(parser.depth)
                .appendQualifiedName(
                    prefix = ANDROID_PREFIX.takeIf { parser.getAttributeNameResource(index) != 0 },
                    name = parser.getAttributeName(index),
                )
                .append("=\"")
                .append(attributeValue(parser, resources, index).escapeXml())
                .append('"')
        }
        output.append(">\n")
    }

    private fun appendEndTag(output: StringBuilder, parser: XmlResourceParser) {
        output.appendIndent(parser.depth - 1)
            .append("</")
            .append(parser.name)
            .append(">\n")
    }

    private fun appendText(output: StringBuilder, parser: XmlResourceParser) {
        parser.text
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                output.appendIndent(parser.depth)
                    .append(it.escapeXml())
                    .append('\n')
            }
    }

    private fun attributeValue(
        parser: XmlResourceParser,
        resources: Resources,
        index: Int,
    ): String {
        val resourceId = parser.getAttributeResourceValue(index, 0)
        if (resourceId != 0) {
            return runCatching { "@${resources.getResourceName(resourceId)}" }
                .getOrDefault(parser.getAttributeValue(index))
        }
        return parser.getAttributeValue(index)
    }

    companion object {
        private const val TAG = "ManifestParser"
        private const val MANIFEST_FILE_NAME = "AndroidManifest.xml"
        private const val INDENT = "  "
        private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        private const val ANDROID_PREFIX = "android"
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
}
