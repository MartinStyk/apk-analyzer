package sk.styk.martin.apkanalyzer.core.appexport

import com.networknt.schema.ExecutionContext
import com.networknt.schema.InputFormat
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumn
import sk.styk.martin.apkanalyzer.core.appexport.column.token
import sk.styk.martin.apkanalyzer.core.appexport.column.valueOf
import sk.styk.martin.apkanalyzer.core.appexport.format.INVENTORY_SCHEMA_URL
import sk.styk.martin.apkanalyzer.core.appexport.format.INVENTORY_SCHEMA_VERSION
import sk.styk.martin.apkanalyzer.core.appexport.format.JsonInventoryWriter
import sk.styk.martin.apkanalyzer.core.appexport.source.InventoryAppData
import sk.styk.martin.apkanalyzer.core.appexport.source.SourceResult
import sk.styk.martin.apkanalyzer.core.apps.components.Activity
import sk.styk.martin.apkanalyzer.core.apps.components.BroadcastReceiver
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilter
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilterKey
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentKind
import sk.styk.martin.apkanalyzer.core.apps.components.ContentProvider
import sk.styk.martin.apkanalyzer.core.apps.components.IntentFilterDataRule
import sk.styk.martin.apkanalyzer.core.apps.components.IntentFilterDataRuleType
import sk.styk.martin.apkanalyzer.core.apps.components.IntentFilterUriRelativeGroup
import sk.styk.martin.apkanalyzer.core.apps.components.ProviderPathMatchType
import sk.styk.martin.apkanalyzer.core.apps.components.ProviderPathPermission
import sk.styk.martin.apkanalyzer.core.apps.components.Service
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.Feature
import sk.styk.martin.apkanalyzer.core.apps.manifest.ParsedManifest
import sk.styk.martin.apkanalyzer.core.apps.model.AppCategory
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.model.Detail.WhatsApp
import sk.styk.martin.apkanalyzer.core.apps.model.InstallLocation
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.apps.model.anApp
import sk.styk.martin.apkanalyzer.core.apps.packaging.InstalledSplitApk
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraries
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraryFile
import sk.styk.martin.apkanalyzer.core.apps.packaging.SplitApkKind
import sk.styk.martin.apkanalyzer.core.apps.permissions.Permission
import sk.styk.martin.apkanalyzer.core.apps.permissions.PermissionDetails
import sk.styk.martin.apkanalyzer.core.apps.permissions.Permissions
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionFlag
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.apps.permissions.UsedPermission
import sk.styk.martin.apkanalyzer.core.apps.signing.ApkAnalyzerCertificate
import sk.styk.martin.apkanalyzer.core.apps.signing.AppSigning
import sk.styk.martin.apkanalyzer.core.apps.signing.CertificatePrincipal
import sk.styk.martin.apkanalyzer.core.apps.signing.CertificateTrustLevel
import sk.styk.martin.apkanalyzer.core.apps.signing.SigningSchemeVersion
import sk.styk.martin.apkanalyzer.core.apps.signing.WhatsAppCertificate
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import java.io.StringWriter
import java.time.Instant
import java.util.function.Consumer

@RunWith(RobolectricTestRunner::class)
class PublishedSchemaTest {

    private val schemaText = checkNotNull(javaClass.getResource("/schema.json")).readText()
    private val schema = Json.parseToJsonElement(schemaText)
    private val validator = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12).getSchema(schemaText)

    @Test
    fun `Given the column catalog When compared to the schema Then keys match in order`() {
        val keys = InventoryColumn.entries.map { it.key }

        keys shouldBe schema.at("\$defs", "app", "properties").jsonObject.keys.toList()
    }

    @Test
    fun `Given the schema version When compared to the schema Then version and address match`() {
        INVENTORY_SCHEMA_VERSION shouldBe schema.at("properties", "schema_version", "const").jsonPrimitive.int
        INVENTORY_SCHEMA_URL shouldBe schema.at("\$id").jsonPrimitive.content
    }

    @Test
    fun `Given enum tokens When compared to the schema Then they match`() {
        AppCategory.entries.mapNotNull { it.token }.toSet() shouldBe schema.enumAt("\$defs", "category")
        AppSource.entries.map { it.token }.toSet() shouldBe schema.enumAt("\$defs", "install_source")
        InstallLocation.entries.map { it.token }.toSet() shouldBe schema.enumAt("\$defs", "install_location")
        SigningSchemeVersion.entries.map { it.token }.toSet() shouldBe schema.enumAt("\$defs", "signing_scheme")
        SplitApkKind.entries.map { it.token }.toSet() shouldBe schema.enumAt("\$defs", "split_apk", "properties", "kind")
        CertificateTrustLevel.entries.map { it.token }.toSet() shouldBe schema.enumAt("\$defs", "certificate", "properties", "trust_level")
        ProtectionLevel.entries.map { it.token }.toSet() shouldBe
            schema.enumAt("\$defs", "permission", "properties", "details", "properties", "protection_level")
        ProtectionFlag.entries.map { it.token }.toSet() shouldBe
            schema.enumAt("\$defs", "permission", "properties", "details", "properties", "protection_flags", "items")
        ProviderPathMatchType.entries.map { it.token }.toSet() shouldBe
            schema.enumAt("\$defs", "provider", "properties", "path_permissions", "items", "properties", "match_type")
        ComponentKind.entries.map { it.token }.toSet() shouldBe
            schema.enumAt("\$defs", "component_intent_filters", "properties", "component_kind")
    }

    @Test
    fun `Given every source loaded When exporting Then the file matches the schema`() {
        val apps = everyInstalledApp.map { it.withEverySourceLoaded() } +
            InstallLocation.entries.map { location -> everyInstalledApp.first().withEverySourceLoaded(location) }

        val errors = validator.validate(exportedFile(apps), InputFormat.JSON, assertingFormats)

        errors.map { it.toString() }.shouldBeEmpty()
    }

    @Test
    fun `Given no source loaded When exporting Then the file matches the schema`() {
        val apps = everyInstalledApp.map { it.withNoSourceLoaded() }

        val errors = validator.validate(exportedFile(apps), InputFormat.JSON, assertingFormats)

        errors.map { it.toString() }.shouldBeEmpty()
    }
}

private val assertingFormats = Consumer<ExecutionContext> { context ->
    context.executionConfig { config -> config.formatAssertionsEnabled(true) }
}

private val everyInstalledApp: List<InstalledApp> =
    AppSource.entries.map { anApp(name = it.name, source = it) } +
        AppCategory.entries.map { anApp(name = it.name).copy(category = it) } +
        anApp(name = "System", totalSize = null, lastUsedTime = null).copy(isSystemApp = true, sharedUserId = "android.uid.system")

private fun anAppDetail(installLocation: InstallLocation) = AppDetail(
    analysisMode = AppDetail.AnalysisMode.InstalledPackage,
    info = WhatsApp.info.copy(
        installLocation = installLocation,
        installedSplits = SplitApkKind.entries.map { InstalledSplitApk("split_$it.apk", filePath = null, 1.megabytes, it, qualifier = "en") },
    ),
    signing = AppSigning(),
    activities = listOf(
        Activity(name = "com.example.Main", packageName = PackageName("com.example"), isLauncher = true),
        Activity(name = "com.example.Alias", packageName = PackageName("com.example"), isLauncher = null),
    ),
    services = listOf(
        Service(
            name = "com.example.Sync",
            permission = null,
            isExported = true,
            isStopWithTask = false,
            isSingleUser = false,
            isIsolatedProcess = false,
            isExternalService = false,
        ),
    ),
    contentProviders = listOf(
        ContentProvider(
            name = "com.example.Provider",
            pathPermissions = ProviderPathMatchType.entries.map { ProviderPathPermission("/$it", it, readPermission = "android.permission.CAMERA") },
        ),
    ),
    receivers = listOf(BroadcastReceiver(name = "com.example.Boot", permission = null, isExported = true)),
    permissions = Permissions(
        defined = ProtectionLevel.entries.map {
            Permission(
                name = "com.example.$it",
                details = PermissionDetails(
                    groupName = null,
                    protectionLevel = it,
                    protectionFlags = ProtectionFlag.entries.toSet(),
                    description = null,
                    declaringPackage = PackageName("com.example"),
                ),
            )
        },
        used = listOf(UsedPermission(Permission(name = "com.other.UNKNOWN", details = null), isGranted = false)),
    ),
    features = listOf(Feature.Hardware("android.hardware.camera", isRequired = true), Feature.OpenGlEs(reqGlEsVersion = 196608)),
)

private val fullSigning = AppSigning(
    currentCertificates = CertificateTrustLevel.entries.map { WhatsAppCertificate.copy(trustLevel = it, issuer = CertificatePrincipal()) },
    pastCertificates = listOf(ApkAnalyzerCertificate),
    hasMultipleSigners = true,
)

private val fullIntentFilters = ComponentKind.entries.associate { kind ->
    ComponentIntentFilterKey("com.example.$kind", kind) to listOf(
        ComponentIntentFilter(
            actions = listOf("android.intent.action.VIEW"),
            categories = listOf("android.intent.category.DEFAULT"),
            dataRules = IntentFilterDataRuleType.entries.map { IntentFilterDataRule(it, "value") },
            uriRelativeGroups = listOf(IntentFilterUriRelativeGroup(isAllowed = true, listOf(IntentFilterDataRule(IntentFilterDataRuleType.Query, "q")))),
            priority = 1,
            order = 2,
            isAutoVerify = true,
        ),
    )
}

private fun InstalledApp.withEverySourceLoaded(installLocation: InstallLocation = InstallLocation.Auto) = InventoryAppData(
    app = this,
    signing = SourceResult.Loaded(fullSigning),
    detail = SourceResult.Loaded(anAppDetail(installLocation)),
    nativeLibraries = SourceResult.Loaded(NativeLibraries(listOf(NativeLibraryFile("libchat.so", "arm64-v8a", 1.megabytes, "base.apk")))),
    signingSchemes = SourceResult.Loaded(SigningSchemeVersion.entries),
    intentFilters = SourceResult.Loaded(fullIntentFilters),
    manifest = SourceResult.Loaded(ParsedManifest(xml = "<manifest/>", additionalInstalledSplits = 1)),
)

private fun InstalledApp.withNoSourceLoaded() = InventoryAppData(
    app = this,
    signing = SourceResult.NotRequested,
    detail = SourceResult.Failed,
    nativeLibraries = SourceResult.NotRequested,
    signingSchemes = SourceResult.Failed,
    intentFilters = SourceResult.NotRequested,
    manifest = SourceResult.Failed,
)

private fun exportedFile(apps: List<InventoryAppData>): String {
    val output = StringWriter()
    JsonInventoryWriter(InventoryColumn.entries, output, Instant.parse("2026-11-03T14:49:12.345Z")).use { writer ->
        writer.begin()
        apps.forEach { app -> writer.writeApp(InventoryColumn.entries.map { it.valueOf(app) }) }
    }
    return output.toString()
}

private fun JsonElement.at(vararg path: String): JsonElement = path.fold(this) { element, key -> element.jsonObject.getValue(key) }

private fun JsonElement.enumAt(vararg path: String): Set<String> = at(*path).jsonObject.getValue("enum").jsonArray.map { it.jsonPrimitive.content }.toSet()
