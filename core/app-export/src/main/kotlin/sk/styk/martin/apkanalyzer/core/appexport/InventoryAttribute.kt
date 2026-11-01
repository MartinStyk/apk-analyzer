package sk.styk.martin.apkanalyzer.core.appexport

enum class InventoryAttributeGroup {
    Identification,
    Versioning,
    ApiTargets,
    Installation,
    Storage,
    Security,
    Signing,
    Permissions,
    Components,
}

internal enum class InventoryDataSource {
    AppList,
    SigningIndex,
    AppAnalysis,
}

enum class InventoryAttribute(
    val key: String,
    val group: InventoryAttributeGroup,
    internal val source: InventoryDataSource,
) {
    ApplicationName("application_name", InventoryAttributeGroup.Identification, InventoryDataSource.AppList),
    PackageName("package_name", InventoryAttributeGroup.Identification, InventoryDataSource.AppList),
    ProcessName("process_name", InventoryAttributeGroup.Identification, InventoryDataSource.AppAnalysis),
    Uid("uid", InventoryAttributeGroup.Identification, InventoryDataSource.AppAnalysis),
    SharedUserId("shared_user_id", InventoryAttributeGroup.Identification, InventoryDataSource.AppList),
    Category("category", InventoryAttributeGroup.Identification, InventoryDataSource.AppList),
    VersionName("version_name", InventoryAttributeGroup.Versioning, InventoryDataSource.AppList),
    VersionCode("version_code", InventoryAttributeGroup.Versioning, InventoryDataSource.AppList),
    MinSdk("min_sdk", InventoryAttributeGroup.ApiTargets, InventoryDataSource.AppList),
    TargetSdk("target_sdk", InventoryAttributeGroup.ApiTargets, InventoryDataSource.AppList),
    AppType("app_type", InventoryAttributeGroup.Installation, InventoryDataSource.AppList),
    InstallSource("install_source", InventoryAttributeGroup.Installation, InventoryDataSource.AppList),
    InstallerPackage("installer_package", InventoryAttributeGroup.Installation, InventoryDataSource.AppList),
    InstallInitiator("install_initiator", InventoryAttributeGroup.Installation, InventoryDataSource.AppList),
    InstallOriginator("install_originator", InventoryAttributeGroup.Installation, InventoryDataSource.AppList),
    FirstInstalled("first_installed", InventoryAttributeGroup.Installation, InventoryDataSource.AppList),
    LastUpdated("last_updated", InventoryAttributeGroup.Installation, InventoryDataSource.AppList),
    LastUsed("last_used", InventoryAttributeGroup.Installation, InventoryDataSource.AppList),
    ApkSize("apk_size_bytes", InventoryAttributeGroup.Storage, InventoryDataSource.AppList),
    TotalSize("total_size_bytes", InventoryAttributeGroup.Storage, InventoryDataSource.AppList),
    SplitApks("split_apks", InventoryAttributeGroup.Storage, InventoryDataSource.AppAnalysis),
    InstallLocation("install_location", InventoryAttributeGroup.Storage, InventoryDataSource.AppAnalysis),
    ApkDirectory("apk_directory", InventoryAttributeGroup.Storage, InventoryDataSource.AppAnalysis),
    DataDirectory("data_directory", InventoryAttributeGroup.Storage, InventoryDataSource.AppAnalysis),
    Debuggable("debuggable", InventoryAttributeGroup.Security, InventoryDataSource.AppAnalysis),
    AllowsBackup("allows_backup", InventoryAttributeGroup.Security, InventoryDataSource.AppAnalysis),
    CleartextTraffic("cleartext_traffic", InventoryAttributeGroup.Security, InventoryDataSource.AppAnalysis),
    SigningCertificateSha256("signing_certificate_sha256", InventoryAttributeGroup.Signing, InventoryDataSource.SigningIndex),
    SigningCertificateSubject("signing_certificate_subject", InventoryAttributeGroup.Signing, InventoryDataSource.SigningIndex),
    RequestedPermissions("requested_permissions", InventoryAttributeGroup.Permissions, InventoryDataSource.AppList),
    GrantedPermissions("granted_permissions", InventoryAttributeGroup.Permissions, InventoryDataSource.AppAnalysis),
    ActivityCount("activity_count", InventoryAttributeGroup.Components, InventoryDataSource.AppAnalysis),
    ServiceCount("service_count", InventoryAttributeGroup.Components, InventoryDataSource.AppAnalysis),
    ReceiverCount("receiver_count", InventoryAttributeGroup.Components, InventoryDataSource.AppAnalysis),
    ProviderCount("provider_count", InventoryAttributeGroup.Components, InventoryDataSource.AppAnalysis),
    ;

    val requiresAppAnalysis: Boolean
        get() = source == InventoryDataSource.AppAnalysis

    val requiresUsageAccess: Boolean
        get() = this == LastUsed || this == TotalSize

    companion object {
        val Defaults: Set<InventoryAttribute> = setOf(
            ApplicationName,
            PackageName,
            VersionName,
            VersionCode,
            MinSdk,
            TargetSdk,
            InstallSource,
            LastUpdated,
        )
    }
}
