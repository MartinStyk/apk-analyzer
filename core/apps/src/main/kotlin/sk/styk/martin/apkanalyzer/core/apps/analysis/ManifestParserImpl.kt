package sk.styk.martin.apkanalyzer.core.apps.analysis

import android.content.pm.PackageManager
import android.content.res.Resources
import android.content.res.XmlResourceParser
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.apps.model.ComponentIntentFilter
import sk.styk.martin.apkanalyzer.core.apps.model.ComponentIntentFilterKey
import sk.styk.martin.apkanalyzer.core.apps.model.ComponentKind
import sk.styk.martin.apkanalyzer.core.apps.model.IntentFilterDataRule
import sk.styk.martin.apkanalyzer.core.apps.model.IntentFilterDataRuleType
import sk.styk.martin.apkanalyzer.core.apps.model.IntentFilterUriRelativeGroup
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import java.io.FileNotFoundException
import javax.inject.Inject

internal class ManifestParserImpl @Inject constructor(private val packageManager: PackageManager, private val dispatcherProvider: DispatcherProvider) : ManifestParser {

    override suspend fun manifest(reference: AppReference): Result<ParsedManifest> = when (reference) {
        is AppReference.InstalledPackage -> installedPackageManifest(reference.packageName)
        is AppReference.ApkFile -> apkFileManifest(reference.path)
    }

    override suspend fun componentIntentFilters(reference: AppReference): Result<Map<ComponentIntentFilterKey, List<ComponentIntentFilter>>> = withContext(dispatcherProvider.io()) {
        runCatchingCancellable {
            when (reference) {
                is AppReference.InstalledPackage -> installedComponentIntentFilters(reference.packageName)
                is AppReference.ApkFile -> resourcesForApk(reference.path).let(::readComponentManifest).filters
            }
                .groupBy(
                    keySelector = { it.first },
                    valueTransform = { it.second },
                )
                .mapValues { (_, filters) -> filters.distinct() }
        }.onFailure {
            Logger.e(TAG, it, "Can not read component intent filters from $reference")
        }
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

    private fun installedComponentIntentFilters(packageName: PackageName): List<Pair<ComponentIntentFilterKey, ComponentIntentFilter>> {
        val applicationInfo = packageManager.getApplicationInfo(packageName.value, 0)
        val resources = packageManager.getResourcesForApplication(applicationInfo)
        val expectedManifestCount = applicationInfo.splitSourceDirs.orEmpty().size + 1
        val manifests = mutableMapOf<String, ParsedComponentManifest>()

        for (cookie in 1..MAX_ASSET_COOKIE) {
            val parsedManifest = readInstalledComponentManifest(resources, cookie)
            if (parsedManifest?.packageName == packageName.value) {
                manifests.putIfAbsent(parsedManifest.splitName.orEmpty(), parsedManifest)
                if (manifests.size == expectedManifestCount) break
            }
        }

        check(manifests.size == expectedManifestCount) {
            "Read ${manifests.size} of $expectedManifestCount installed manifests for $packageName"
        }
        return manifests.values.flatMap { it.filters }
    }

    private fun readInstalledComponentManifest(resources: Resources, cookie: Int): ParsedComponentManifest? = try {
        resources.assets.openXmlResourceParser(cookie, MANIFEST_FILE_NAME).use { parser ->
            readComponentManifest(parser, resources)
        }
    } catch (_: FileNotFoundException) {
        null
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

    private fun readComponentManifest(resources: Resources): ParsedComponentManifest = resources.assets.openXmlResourceParser(MANIFEST_FILE_NAME).use { parser ->
        readComponentManifest(parser, resources)
    }

    @Suppress("CyclomaticComplexMethod")
    private fun readComponentManifest(parser: XmlResourceParser, resources: Resources): ParsedComponentManifest {
        val filters = mutableListOf<Pair<ComponentIntentFilterKey, ComponentIntentFilter>>()
        var packageName = ""
        var splitName: String? = null
        var componentKey: ComponentIntentFilterKey? = null
        var currentFilter: MutableComponentIntentFilter? = null
        var currentUriRelativeGroup: MutableIntentFilterUriRelativeGroup? = null

        var eventType = parser.eventType
        while (eventType != XmlResourceParser.END_DOCUMENT) {
            when (eventType) {
                XmlResourceParser.START_TAG -> when (parser.name) {
                    MANIFEST_TAG -> {
                        packageName = parser.getAttributeValue(null, PACKAGE_ATTRIBUTE).orEmpty()
                        splitName = parser.getAttributeValue(null, SPLIT_ATTRIBUTE)
                    }

                    in COMPONENT_TAGS -> componentKey = parser.resolvedAndroidString(resources, NAME_ATTRIBUTE)
                        ?.toQualifiedComponentName(packageName)
                        ?.let { name -> ComponentIntentFilterKey(name = name, kind = COMPONENT_KINDS.getValue(parser.name)) }

                    INTENT_FILTER_TAG -> if (componentKey != null) {
                        currentFilter = MutableComponentIntentFilter(
                            priority = parser.resolvedAndroidInt(resources, PRIORITY_ATTRIBUTE, 0),
                            order = parser.resolvedAndroidInt(resources, ORDER_ATTRIBUTE, 0),
                            isAutoVerify = parser.resolvedAndroidBoolean(resources, AUTO_VERIFY_ATTRIBUTE, false),
                        )
                    }

                    URI_RELATIVE_FILTER_GROUP_TAG -> if (currentFilter != null) {
                        currentUriRelativeGroup = MutableIntentFilterUriRelativeGroup(
                            isAllowed = parser.resolvedAndroidBoolean(resources, ALLOW_ATTRIBUTE, true),
                        )
                    }

                    ACTION_TAG -> parser.resolvedAndroidString(resources, NAME_ATTRIBUTE)?.let { currentFilter?.actions?.add(it) }

                    CATEGORY_TAG -> parser.resolvedAndroidString(resources, NAME_ATTRIBUTE)?.let { currentFilter?.categories?.add(it) }

                    DATA_TAG -> currentFilter?.let { filter ->
                        addIntentFilterDataRules(
                            parser = parser,
                            resources = resources,
                            rules = currentUriRelativeGroup?.dataRules ?: filter.dataRules,
                        )
                    }
                }

                XmlResourceParser.END_TAG -> when (parser.name) {
                    INTENT_FILTER_TAG -> {
                        val key = componentKey
                        val filter = currentFilter
                        if (key != null && filter != null) {
                            filters.add(key to filter.toComponentIntentFilter())
                        }
                        currentFilter = null
                    }

                    URI_RELATIVE_FILTER_GROUP_TAG -> {
                        currentUriRelativeGroup?.let { currentFilter?.uriRelativeGroups?.add(it) }
                        currentUriRelativeGroup = null
                    }

                    in COMPONENT_TAGS -> componentKey = null
                }
            }
            eventType = parser.next()
        }
        return ParsedComponentManifest(packageName = packageName, splitName = splitName, filters = filters)
    }

    private fun addIntentFilterDataRules(
        parser: XmlResourceParser,
        resources: Resources,
        rules: MutableList<IntentFilterDataRule>,
    ) {
        val host = parser.resolvedAndroidString(resources, IntentFilterDataRuleType.Host.manifestAttribute)
        val port = parser.resolvedAndroidString(resources, IntentFilterDataRuleType.Port.manifestAttribute)
        if (host != null) {
            val authority = if (port != null) "$host:$port" else host
            rules.add(IntentFilterDataRule(type = IntentFilterDataRuleType.Host, value = authority))
        }
        IntentFilterDataRuleType.entries
            .filterNot { it == IntentFilterDataRuleType.Host || it == IntentFilterDataRuleType.Port }
            .forEach { type ->
                parser.resolvedAndroidString(resources, type.manifestAttribute)?.let { value ->
                    rules.add(IntentFilterDataRule(type = type, value = value))
                }
            }
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
        private const val MANIFEST_TAG = "manifest"
        private const val INTENT_FILTER_TAG = "intent-filter"
        private const val ACTION_TAG = "action"
        private const val CATEGORY_TAG = "category"
        private const val DATA_TAG = "data"
        private const val URI_RELATIVE_FILTER_GROUP_TAG = "uri-relative-filter-group"
        private const val PACKAGE_ATTRIBUTE = "package"
        private const val SPLIT_ATTRIBUTE = "split"
        private const val NAME_ATTRIBUTE = "name"
        private const val PRIORITY_ATTRIBUTE = "priority"
        private const val ORDER_ATTRIBUTE = "order"
        private const val AUTO_VERIFY_ATTRIBUTE = "autoVerify"
        private const val ALLOW_ATTRIBUTE = "allow"
        private const val INDENT = "  "
        private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        private const val ANDROID_PREFIX = "android"
        private const val MAX_ASSET_COOKIE = 256
        private val COMPONENT_KINDS = mapOf(
            "activity" to ComponentKind.Activity,
            "activity-alias" to ComponentKind.Activity,
            "service" to ComponentKind.Service,
            "receiver" to ComponentKind.Receiver,
            "provider" to ComponentKind.Provider,
        )
        private val COMPONENT_TAGS = COMPONENT_KINDS.keys
    }

    private fun XmlResourceParser.resolvedAndroidString(resources: Resources, name: String): String? {
        val resourceId = getAttributeResourceValue(ANDROID_NAMESPACE, name, 0)
        return if (resourceId == 0) {
            getAttributeValue(ANDROID_NAMESPACE, name)?.takeIf { it.isNotBlank() }
        } else {
            runCatching { resources.getString(resourceId) }
                .onFailure { Logger.w(TAG, "Can not resolve string resource for attribute $name") }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
        }
    }

    private fun XmlResourceParser.resolvedAndroidInt(
        resources: Resources,
        name: String,
        defaultValue: Int,
    ): Int {
        val resourceId = getAttributeResourceValue(ANDROID_NAMESPACE, name, 0)
        return if (resourceId == 0) {
            getAttributeIntValue(ANDROID_NAMESPACE, name, defaultValue)
        } else {
            runCatching { resources.getInteger(resourceId) }
                .onFailure { Logger.w(TAG, "Can not resolve integer resource for attribute $name") }
                .getOrDefault(defaultValue)
        }
    }

    private fun XmlResourceParser.resolvedAndroidBoolean(
        resources: Resources,
        name: String,
        defaultValue: Boolean,
    ): Boolean {
        val resourceId = getAttributeResourceValue(ANDROID_NAMESPACE, name, 0)
        return if (resourceId == 0) {
            getAttributeBooleanValue(ANDROID_NAMESPACE, name, defaultValue)
        } else {
            runCatching { resources.getBoolean(resourceId) }
                .onFailure { Logger.w(TAG, "Can not resolve boolean resource for attribute $name") }
                .getOrDefault(defaultValue)
        }
    }

    private fun String.toQualifiedComponentName(packageName: String): String = when {
        startsWith('.') -> packageName + this
        '.' in this -> this
        else -> "$packageName.$this"
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

private data class ParsedComponentManifest(
    val packageName: String,
    val splitName: String?,
    val filters: List<Pair<ComponentIntentFilterKey, ComponentIntentFilter>>,
)

private data class MutableComponentIntentFilter(
    val actions: MutableList<String> = mutableListOf(),
    val categories: MutableList<String> = mutableListOf(),
    val dataRules: MutableList<IntentFilterDataRule> = mutableListOf(),
    val uriRelativeGroups: MutableList<MutableIntentFilterUriRelativeGroup> = mutableListOf(),
    val priority: Int,
    val order: Int,
    val isAutoVerify: Boolean,
) {
    fun toComponentIntentFilter() = ComponentIntentFilter(
        actions = actions.distinct(),
        categories = categories.distinct(),
        dataRules = dataRules.distinct(),
        uriRelativeGroups = uriRelativeGroups.map { it.toIntentFilterUriRelativeGroup() }.distinct(),
        priority = priority,
        order = order,
        isAutoVerify = isAutoVerify,
    )
}

private data class MutableIntentFilterUriRelativeGroup(val isAllowed: Boolean, val dataRules: MutableList<IntentFilterDataRule> = mutableListOf()) {
    fun toIntentFilterUriRelativeGroup() = IntentFilterUriRelativeGroup(
        isAllowed = isAllowed,
        dataRules = dataRules.distinct(),
    )
}
