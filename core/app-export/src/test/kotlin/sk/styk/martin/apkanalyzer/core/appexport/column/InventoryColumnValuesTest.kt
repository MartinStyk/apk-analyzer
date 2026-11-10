package sk.styk.martin.apkanalyzer.core.appexport.column

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryValue.Flag
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryValue.Items
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryValue.Missing
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryValue.Number
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryValue.Record
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryValue.Text
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryValue.TextList
import sk.styk.martin.apkanalyzer.core.appexport.source.InventoryAppData
import sk.styk.martin.apkanalyzer.core.appexport.source.InventoryDataSource
import sk.styk.martin.apkanalyzer.core.appexport.source.SourceResult
import sk.styk.martin.apkanalyzer.core.appexport.source.SourceResult.Loaded
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
import sk.styk.martin.apkanalyzer.core.apps.installsource.InstallSourceChain
import sk.styk.martin.apkanalyzer.core.apps.manifest.ParsedManifest
import sk.styk.martin.apkanalyzer.core.apps.model.AppCategory
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.model.Detail.WhatsApp
import sk.styk.martin.apkanalyzer.core.apps.model.InstallLocation
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
import sk.styk.martin.apkanalyzer.core.apps.signing.Certificate
import sk.styk.martin.apkanalyzer.core.apps.signing.CertificatePrincipal
import sk.styk.martin.apkanalyzer.core.apps.signing.CertificateTrustLevel
import sk.styk.martin.apkanalyzer.core.apps.signing.SigningSchemeVersion
import sk.styk.martin.apkanalyzer.core.apps.signing.WhatsAppCertificate
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.bytes
import sk.styk.martin.apkanalyzer.core.common.settings.FingerprintFormat
import java.time.Instant

class InventoryColumnValuesTest {

    @Test
    fun `Given an app with a distinct value in every field When reading scalar columns Then each column holds its own value`() {
        val data = anInventoryAppData()

        val values = InventoryColumn.entries.filter { it.shape == InventoryColumnShape.Scalar }.associate { it.key to it.valueOf(data) }

        values shouldBe mapOf(
            "application_name" to Text("Maps"),
            "package_name" to Text("com.example.maps"),
            "process_name" to Text("com.example.maps.process"),
            "uid" to Number(10001),
            "shared_user_id" to Text("com.example.shared"),
            "category" to Text("social"),
            "description" to Text("Shows maps."),
            "version_name" to Text("2.5"),
            "version_code" to Number(250),
            "min_sdk" to Number(26),
            "target_sdk" to Number(34),
            "app_type" to Text("system"),
            "install_source" to Text("f_droid"),
            "installer_package" to Text("com.installer"),
            "install_initiator" to Text("com.initiator"),
            "install_originator" to Text("com.originator"),
            "first_installed" to Text("2026-01-01T00:00:00Z"),
            "last_updated" to Text("2026-02-02T00:00:00Z"),
            "last_used" to Text("2026-03-03T00:00:00Z"),
            "apk_size_bytes" to Number(100),
            "total_size_bytes" to Number(200),
            "split_apks" to TextList(listOf("split_config.en.apk")),
            "install_location" to Text("prefer_external"),
            "apk_directory" to Text("/data/app/maps"),
            "data_directory" to Text("/data/user/0/maps"),
            "native_library_abis" to TextList(listOf("arm64-v8a", "x86_64")),
            "debuggable" to Flag(true),
            "allows_backup" to Flag(false),
            "cleartext_traffic" to Flag(true),
            "signing_certificate_sha256" to TextList(listOf("C2:56")),
            "signing_certificate_sha1" to TextList(listOf("C1")),
            "signing_certificate_md5" to TextList(listOf("C5")),
            "signing_public_key_sha256" to TextList(listOf("K2:56")),
            "signing_public_key_sha1" to TextList(listOf("K1")),
            "signing_public_key_md5" to TextList(listOf("K5")),
            "signing_certificate_subject" to TextList(listOf("Maps Inc")),
            "has_multiple_signers" to Flag(false),
            "signing_schemes" to TextList(listOf("v2", "v3.1")),
            "requested_permissions" to TextList(listOf("android.permission.CAMERA", "android.permission.INTERNET")),
            "granted_permissions" to TextList(listOf("android.permission.CAMERA")),
            "activity_count" to Number(1),
            "service_count" to Number(2),
            "receiver_count" to Number(3),
            "provider_count" to Number(4),
        )
    }

    @Test
    fun `Given the opposite debuggable and signer flags When reading flag columns Then each column follows its own flag`() {
        val data = anInventoryAppData(
            detail = Loaded(anAppDetail(isDebuggable = false)),
            signing = Loaded(AppSigning(hasMultipleSigners = true)),
        )

        val flags = listOf(
            InventoryColumn.Debuggable,
            InventoryColumn.AllowsBackup,
            InventoryColumn.CleartextTraffic,
            InventoryColumn.HasMultipleSigners,
        ).map { it.valueOf(data) }

        flags shouldBe listOf(Flag(false), Flag(false), Flag(true), Flag(true))
    }

    @Test
    fun `Given only the declared source of a column is loaded When reading it Then column has a value`() {
        val columns = InventoryColumn.entries

        val emptyColumns = columns.filter { it.valueOf(dataLoadedOnlyFrom(it.source)) == Missing }

        emptyColumns.map { it.key }.shouldBeEmpty()
    }

    @Test
    fun `Given no source is loaded When reading columns that need one Then every column is missing`() {
        val data = dataLoadedOnlyFrom(InventoryDataSource.AppList)

        val values = InventoryColumn.entries
            .filter { it.source != InventoryDataSource.AppList && it.source != InventoryDataSource.UsageStats }
            .map { it.valueOf(data) }

        values.toSet() shouldBe setOf(Missing)
    }

    @Test
    fun `Given loaded empty collections When reading columns Then they stay empty instead of missing`() {
        val data = anInventoryAppData(
            detail = Loaded(anAppDetail(installedSplits = emptyList(), activities = emptyList())),
            nativeLibraries = Loaded(NativeLibraries.Empty),
            signingSchemes = Loaded(emptyList()),
        )

        val values = listOf(
            InventoryColumn.SplitApks,
            InventoryColumn.SigningSchemes,
            InventoryColumn.NativeLibraryAbis,
            InventoryColumn.Activities,
            InventoryColumn.NativeLibraryFiles,
        ).map { it.valueOf(data) }

        values shouldBe listOf(
            TextList(emptyList()),
            TextList(emptyList()),
            TextList(emptyList()),
            Items(emptyList()),
            Items(emptyList()),
        )
    }

    @Test
    fun `Given a certificate When reading signing certificates Then record holds every field`() {
        val data = anInventoryAppData(
            signing = Loaded(
                AppSigning(
                    currentCertificates = listOf(
                        Certificate(
                            signAlgorithm = "SHA256withRSA",
                            certificateHashMd5 = "c5",
                            certificateHashSha1 = "c1",
                            certificateHashSha256 = "c256",
                            publicKeyMd5 = "k5",
                            publicKeySha1 = "k1",
                            publicKeySha256 = "k256",
                            validFrom = Instant.parse("2020-01-01T00:00:00Z"),
                            validUntil = Instant.parse("2030-01-01T00:00:00Z"),
                            serialNumber = "0A:0B",
                            issuer = CertificatePrincipal(name = "Issuer Name", organization = "Issuer Org", country = "US"),
                            subject = CertificatePrincipal(name = "Subject Name", organization = "Subject Org", country = "DE"),
                            trustLevel = CertificateTrustLevel.Debug,
                            isSelfSigned = true,
                        ),
                    ),
                ),
            ),
        )

        val value = InventoryColumn.SigningCertificates.valueOf(data)

        value shouldBe Items(
            listOf(
                Record(
                    mapOf(
                        "signature_algorithm" to Text("SHA256withRSA"),
                        "certificate_md5" to Text("C5"),
                        "certificate_sha1" to Text("C1"),
                        "certificate_sha256" to Text("C2:56"),
                        "public_key_md5" to Text("K5"),
                        "public_key_sha1" to Text("K1"),
                        "public_key_sha256" to Text("K2:56"),
                        "valid_from" to Text("2020-01-01T00:00:00Z"),
                        "valid_until" to Text("2030-01-01T00:00:00Z"),
                        "serial_number" to Text("0A:0B"),
                        "issuer" to Record(mapOf("name" to Text("Issuer Name"), "organization" to Text("Issuer Org"), "country" to Text("US"))),
                        "subject" to Record(mapOf("name" to Text("Subject Name"), "organization" to Text("Subject Org"), "country" to Text("DE"))),
                        "trust_level" to Text("debug"),
                        "self_signed" to Flag(true),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `Given current and past certificates When reading certificate columns Then they stay separate`() {
        val data = anInventoryAppData(
            signing = Loaded(
                AppSigning(
                    currentCertificates = listOf(WhatsAppCertificate.copy(certificateHashSha256 = "current")),
                    pastCertificates = listOf(WhatsAppCertificate.copy(certificateHashSha256 = "past")),
                ),
            ),
        )

        val current = InventoryColumn.SigningCertificates.valueOf(data, FingerprintFormat.Continuous).records.map { it["certificate_sha256"] }
        val past = InventoryColumn.SigningCertificateHistory.valueOf(data, FingerprintFormat.Continuous).records.map { it["certificate_sha256"] }

        current shouldBe listOf(Text("CURRENT"))
        past shouldBe listOf(Text("PAST"))
    }

    @Test
    fun `Given a split When reading split details Then record holds every field`() {
        val split = InstalledSplitApk("split_config.arm64.apk", filePath = "/data/app/split.apk", 300.bytes, SplitApkKind.Abi, qualifier = "arm64_v8a")
        val data = anInventoryAppData(detail = Loaded(anAppDetail(installedSplits = listOf(split))))

        val value = InventoryColumn.SplitApkDetails.valueOf(data)

        value shouldBe Items(
            listOf(
                Record(
                    mapOf(
                        "file_name" to Text("split_config.arm64.apk"),
                        "file_path" to Text("/data/app/split.apk"),
                        "size_bytes" to Number(300),
                        "kind" to Text("abi"),
                        "qualifier" to Text("arm64_v8a"),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `Given a native library When reading library files Then record holds every field`() {
        val data = anInventoryAppData(
            nativeLibraries = Loaded(NativeLibraries(listOf(NativeLibraryFile("libmaps.so", "x86_64", 400.bytes, "split.apk")))),
        )

        val value = InventoryColumn.NativeLibraryFiles.valueOf(data)

        value shouldBe Items(
            listOf(
                Record(
                    mapOf(
                        "name" to Text("libmaps.so"),
                        "abi" to Text("x86_64"),
                        "size_bytes" to Number(400),
                        "containing_apk" to Text("split.apk"),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `Given hardware and OpenGL features When reading features Then each has its own record`() {
        val features = listOf(Feature.Hardware("android.hardware.camera", version = 2, isRequired = true), Feature.OpenGlEs(reqGlEsVersion = 196608))
        val data = anInventoryAppData(detail = Loaded(anAppDetail(features = features)))

        val value = InventoryColumn.Features.valueOf(data)

        value shouldBe Items(
            listOf(
                Record(mapOf("kind" to Text("hardware"), "name" to Text("android.hardware.camera"), "version" to Number(2), "required" to Flag(true))),
                Record(mapOf("kind" to Text("opengl_es"), "version" to Number(196608), "version_name" to Text("3.0"), "required" to Flag(false))),
            ),
        )
    }

    @Test
    fun `Given known and unknown permissions When reading defined permissions Then only known ones have details`() {
        val known = Permission(
            name = "com.example.READ",
            details = PermissionDetails(
                groupName = "com.example.GROUP",
                protectionLevel = ProtectionLevel.Dangerous,
                protectionFlags = setOf(ProtectionFlag.Privileged, ProtectionFlag.AppOp),
                description = "Reads data.",
                declaringPackage = PackageName("com.example.declaring"),
            ),
        )
        val unknown = Permission(name = "com.example.UNKNOWN", details = null)
        val data = anInventoryAppData(detail = Loaded(anAppDetail(permissions = Permissions(defined = listOf(known, unknown), used = emptyList()))))

        val value = InventoryColumn.DefinedPermissions.valueOf(data)

        value shouldBe Items(
            listOf(
                Record(
                    mapOf(
                        "name" to Text("com.example.READ"),
                        "details" to Record(
                            mapOf(
                                "group_name" to Text("com.example.GROUP"),
                                "protection_level" to Text("dangerous"),
                                "protection_flags" to TextList(listOf("app_op", "privileged")),
                                "description" to Text("Reads data."),
                                "declaring_package" to Text("com.example.declaring"),
                            ),
                        ),
                    ),
                ),
                Record(mapOf("name" to Text("com.example.UNKNOWN"), "details" to Missing)),
            ),
        )
    }

    @Test
    fun `Given granted and denied permissions When reading permission details Then each keeps its grant state`() {
        val used = listOf(
            UsedPermission(Permission(name = "android.permission.CAMERA", details = null), isGranted = true),
            UsedPermission(Permission(name = "android.permission.INTERNET", details = null), isGranted = false),
        )
        val data = anInventoryAppData(detail = Loaded(anAppDetail(permissions = Permissions(defined = emptyList(), used = used))))

        val value = InventoryColumn.PermissionDetails.valueOf(data)

        value shouldBe Items(
            listOf(
                Record(mapOf("permission" to Record(mapOf("name" to Text("android.permission.CAMERA"), "details" to Missing)), "granted" to Flag(true))),
                Record(mapOf("permission" to Record(mapOf("name" to Text("android.permission.INTERNET"), "details" to Missing)), "granted" to Flag(false))),
            ),
        )
    }

    @Test
    fun `Given an activity When reading activities Then record holds every field`() {
        val activity = Activity(
            name = "com.example.Main",
            packageName = PackageName("com.example"),
            label = "Maps",
            targetActivity = "com.example.Target",
            permission = "com.example.OPEN",
            parentName = "com.example.Parent",
            isExported = true,
            isLauncher = false,
        )
        val data = anInventoryAppData(detail = Loaded(anAppDetail(activities = listOf(activity))))

        val value = InventoryColumn.Activities.valueOf(data)

        value shouldBe Items(
            listOf(
                Record(
                    mapOf(
                        "name" to Text("com.example.Main"),
                        "package_name" to Text("com.example"),
                        "label" to Text("Maps"),
                        "target_activity" to Text("com.example.Target"),
                        "permission" to Text("com.example.OPEN"),
                        "parent_activity" to Text("com.example.Parent"),
                        "exported" to Flag(true),
                        "launcher" to Flag(false),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `Given an activity with unknown launcher status When reading activities Then launcher is missing`() {
        val activity = Activity(name = "com.example.Main", packageName = PackageName("com.example"), isLauncher = null)
        val data = anInventoryAppData(detail = Loaded(anAppDetail(activities = listOf(activity))))

        val launcher = InventoryColumn.Activities.valueOf(data).records.map { it["launcher"] }

        launcher shouldBe listOf(Missing)
    }

    @Test
    fun `Given a service When reading services Then record holds its name and permission`() {
        val data = anInventoryAppData(detail = Loaded(anAppDetail(services = listOf(Service(name = "com.example.Sync", permission = "com.example.BIND")))))

        val value = InventoryColumn.Services.valueOf(data)

        value shouldBe Items(
            listOf(
                Record(
                    mapOf(
                        "name" to Text("com.example.Sync"),
                        "permission" to Text("com.example.BIND"),
                        "exported" to Flag(false),
                        "stop_with_task" to Flag(false),
                        "single_user" to Flag(false),
                        "isolated_process" to Flag(false),
                        "external_service" to Flag(false),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `Given services with one flag set each When reading services Then each field follows its own flag`() {
        val services = listOf(
            Service(name = "Exported", isExported = true),
            Service(name = "StopWithTask", isStopWithTask = true),
            Service(name = "SingleUser", isSingleUser = true),
            Service(name = "Isolated", isIsolatedProcess = true),
            Service(name = "External", isExternalService = true),
        )
        val data = anInventoryAppData(detail = Loaded(anAppDetail(services = services)))

        val enabledFields = InventoryColumn.Services.valueOf(data).records.map { fields -> fields.filterValues { it == Flag(true) }.keys }

        enabledFields shouldBe listOf(
            setOf("exported"),
            setOf("stop_with_task"),
            setOf("single_user"),
            setOf("isolated_process"),
            setOf("external_service"),
        )
    }

    @Test
    fun `Given a receiver When reading receivers Then record holds every field`() {
        val receiver = BroadcastReceiver(name = "com.example.Boot", permission = "com.example.SEND", isExported = true)
        val data = anInventoryAppData(detail = Loaded(anAppDetail(receivers = listOf(receiver))))

        val value = InventoryColumn.Receivers.valueOf(data)

        value shouldBe Items(
            listOf(Record(mapOf("name" to Text("com.example.Boot"), "permission" to Text("com.example.SEND"), "exported" to Flag(true)))),
        )
    }

    @Test
    fun `Given a provider with a path rule When reading providers Then record holds every field`() {
        val provider = ContentProvider(
            name = "com.example.Provider",
            authority = "com.example.authority",
            readPermission = "com.example.READ",
            writePermission = "com.example.WRITE",
            isExported = true,
            pathPermissions = listOf(
                ProviderPathPermission(
                    "/shared",
                    ProviderPathMatchType.Prefix,
                    readPermission = "com.example.PATH_READ",
                    writePermission = "com.example.PATH_WRITE",
                ),
            ),
        )
        val data = anInventoryAppData(detail = Loaded(anAppDetail(contentProviders = listOf(provider))))

        val value = InventoryColumn.Providers.valueOf(data)

        value shouldBe Items(
            listOf(
                Record(
                    mapOf(
                        "name" to Text("com.example.Provider"),
                        "authority" to Text("com.example.authority"),
                        "read_permission" to Text("com.example.READ"),
                        "write_permission" to Text("com.example.WRITE"),
                        "exported" to Flag(true),
                        "path_permissions" to Items(
                            listOf(
                                Record(
                                    mapOf(
                                        "path" to Text("/shared"),
                                        "match_type" to Text("prefix"),
                                        "read_permission" to Text("com.example.PATH_READ"),
                                        "write_permission" to Text("com.example.PATH_WRITE"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `Given an intent filter When reading intent filters Then record holds every field`() {
        val filter = ComponentIntentFilter(
            actions = listOf("android.intent.action.VIEW"),
            categories = listOf("android.intent.category.BROWSABLE"),
            dataRules = listOf(IntentFilterDataRule(IntentFilterDataRuleType.Scheme, "https")),
            uriRelativeGroups = listOf(
                IntentFilterUriRelativeGroup(isAllowed = false, listOf(IntentFilterDataRule(IntentFilterDataRuleType.PathPrefix, "/maps"))),
            ),
            priority = 5,
            order = 6,
            isAutoVerify = true,
        )
        val data = anInventoryAppData(intentFilters = Loaded(mapOf(ComponentIntentFilterKey("com.example.Main", ComponentKind.Receiver) to listOf(filter))))

        val value = InventoryColumn.IntentFilters.valueOf(data)

        value shouldBe Items(
            listOf(
                Record(
                    mapOf(
                        "component_name" to Text("com.example.Main"),
                        "component_kind" to Text("receiver"),
                        "filters" to Items(
                            listOf(
                                Record(
                                    mapOf(
                                        "actions" to TextList(listOf("android.intent.action.VIEW")),
                                        "categories" to TextList(listOf("android.intent.category.BROWSABLE")),
                                        "data_rules" to Items(listOf(Record(mapOf("type" to Text("scheme"), "value" to Text("https"))))),
                                        "uri_relative_groups" to Items(
                                            listOf(
                                                Record(
                                                    mapOf(
                                                        "allowed" to Flag(false),
                                                        "data_rules" to Items(listOf(Record(mapOf("type" to Text("pathPrefix"), "value" to Text("/maps"))))),
                                                    ),
                                                ),
                                            ),
                                        ),
                                        "priority" to Number(5),
                                        "order" to Number(6),
                                        "auto_verify" to Flag(true),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `Given intent filters of several components When reading intent filters Then they sort by kind then name`() {
        val keys = listOf(
            ComponentIntentFilterKey("com.example.A", ComponentKind.Service),
            ComponentIntentFilterKey("com.example.Z", ComponentKind.Activity),
            ComponentIntentFilterKey("com.example.B", ComponentKind.Activity),
        )
        val data = anInventoryAppData(intentFilters = Loaded(keys.associateWith { emptyList() }))

        val names = InventoryColumn.IntentFilters.valueOf(data).records.map { it["component_name"] }

        names shouldBe listOf(Text("com.example.B"), Text("com.example.Z"), Text("com.example.A"))
    }

    @Test
    fun `Given a parsed manifest When reading the manifest Then record holds every field`() {
        val data = anInventoryAppData(manifest = Loaded(ParsedManifest(xml = "<manifest/>", additionalInstalledSplits = 7)))

        val value = InventoryColumn.Manifest.valueOf(data)

        value shouldBe Record(mapOf("xml" to Text("<manifest/>"), "additional_installed_splits" to Number(7)))
    }
}

private val InventoryValue.records: List<Map<String, InventoryValue>>
    get() = (this as Items).values.map { (it as Record).fields }

private fun dataLoadedOnlyFrom(source: InventoryDataSource): InventoryAppData {
    val loaded = anInventoryAppData()
    return InventoryAppData(
        app = loaded.app,
        signing = if (source == InventoryDataSource.SigningIndex) loaded.signing else SourceResult.NotRequested,
        detail = if (source == InventoryDataSource.AppAnalysis) loaded.detail else SourceResult.Failed,
        nativeLibraries = if (source == InventoryDataSource.NativeLibraries) loaded.nativeLibraries else SourceResult.NotRequested,
        signingSchemes = if (source == InventoryDataSource.SigningSchemes) loaded.signingSchemes else SourceResult.Failed,
        intentFilters = if (source == InventoryDataSource.IntentFilters) loaded.intentFilters else SourceResult.NotRequested,
        manifest = if (source == InventoryDataSource.Manifest) loaded.manifest else SourceResult.Failed,
    )
}

private fun anInventoryAppData(
    signing: SourceResult<AppSigning> = Loaded(
        AppSigning(
            currentCertificates = listOf(
                WhatsAppCertificate.copy(
                    certificateHashSha256 = "c256",
                    certificateHashSha1 = "c1",
                    certificateHashMd5 = "c5",
                    publicKeySha256 = "k256",
                    publicKeySha1 = "k1",
                    publicKeyMd5 = "k5",
                    subject = CertificatePrincipal(name = "Maps Developer", organization = "Maps Inc"),
                ),
            ),
            pastCertificates = listOf(ApkAnalyzerCertificate),
            hasMultipleSigners = false,
        ),
    ),
    detail: SourceResult<AppDetail> = Loaded(anAppDetail()),
    nativeLibraries: SourceResult<NativeLibraries> = Loaded(
        NativeLibraries(
            listOf(
                NativeLibraryFile("libmaps.so", "x86_64", 1.bytes, "base.apk"),
                NativeLibraryFile("libmaps.so", "arm64-v8a", 1.bytes, "base.apk"),
            ),
        ),
    ),
    signingSchemes: SourceResult<List<SigningSchemeVersion>> = Loaded(listOf(SigningSchemeVersion.V2, SigningSchemeVersion.V3_1)),
    intentFilters: SourceResult<Map<ComponentIntentFilterKey, List<ComponentIntentFilter>>> = Loaded(emptyMap()),
    manifest: SourceResult<ParsedManifest> = Loaded(ParsedManifest(xml = "<manifest/>", additionalInstalledSplits = 0)),
) = InventoryAppData(
    app = anApp(
        name = "Maps",
        packageName = "com.example.maps",
        versionName = "2.5",
        source = AppSource.FDroid,
        targetSdk = 34,
        requestedPermissions = listOf("android.permission.CAMERA", "android.permission.INTERNET"),
        totalSize = 200.bytes,
        apkSize = 100.bytes,
        installTime = Instant.parse("2026-01-01T00:00:00Z"),
        lastUpdateTime = Instant.parse("2026-02-02T00:00:00Z"),
        lastUsedTime = Instant.parse("2026-03-03T00:00:00Z"),
    ).copy(
        isSystemApp = true,
        version = 250,
        minSdk = 26,
        sharedUserId = "com.example.shared",
        category = AppCategory.Social,
        installSourceChain = InstallSourceChain(
            installingPackage = PackageName("com.installer"),
            initiatingPackage = PackageName("com.initiator"),
            originatingPackage = PackageName("com.originator"),
        ),
    ),
    signing = signing,
    detail = detail,
    nativeLibraries = nativeLibraries,
    signingSchemes = signingSchemes,
    intentFilters = intentFilters,
    manifest = manifest,
)

private fun anAppDetail(
    isDebuggable: Boolean = true,
    installedSplits: List<InstalledSplitApk> =
        listOf(InstalledSplitApk("split_config.en.apk", filePath = null, 1.bytes, SplitApkKind.Language, qualifier = "en")),
    activities: List<Activity> = List(1) { Activity(name = "Activity$it", packageName = PackageName("com.example.maps")) },
    services: List<Service> = List(2) { Service(name = "Service$it") },
    receivers: List<BroadcastReceiver> = List(3) { BroadcastReceiver(name = "Receiver$it") },
    contentProviders: List<ContentProvider> = List(4) { ContentProvider(name = "Provider$it") },
    permissions: Permissions = Permissions(
        defined = emptyList(),
        used = listOf(
            UsedPermission(Permission(name = "android.permission.CAMERA", details = null), isGranted = true),
            UsedPermission(Permission(name = "android.permission.INTERNET", details = null), isGranted = false),
        ),
    ),
    features: List<Feature> = emptyList(),
) = AppDetail(
    analysisMode = AppDetail.AnalysisMode.InstalledPackage,
    info = WhatsApp.info.copy(
        processName = "com.example.maps.process",
        uid = 10001,
        description = "Shows maps.",
        installLocation = InstallLocation.External,
        apkDirectory = "/data/app/maps",
        dataDirectory = "/data/user/0/maps",
        isDebuggable = isDebuggable,
        allowsBackup = false,
        usesCleartextTraffic = true,
        installedSplits = installedSplits,
    ),
    signing = AppSigning(),
    activities = activities,
    services = services,
    contentProviders = contentProviders,
    receivers = receivers,
    permissions = permissions,
    features = features,
)
