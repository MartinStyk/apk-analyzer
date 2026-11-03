package sk.styk.martin.apkanalyzer.core.appexport.column

enum class InventoryPreset(val columns: Set<InventoryColumn>) {
    Basics(
        setOf(
            InventoryColumn.ApplicationName,
            InventoryColumn.PackageName,
            InventoryColumn.VersionName,
            InventoryColumn.VersionCode,
            InventoryColumn.MinSdk,
            InventoryColumn.TargetSdk,
            InventoryColumn.InstallSource,
            InventoryColumn.LastUpdated,
        ),
    ),
    SecurityAudit(
        setOf(
            InventoryColumn.ApplicationName,
            InventoryColumn.PackageName,
            InventoryColumn.VersionName,
            InventoryColumn.TargetSdk,
            InventoryColumn.InstallSource,
            InventoryColumn.Debuggable,
            InventoryColumn.AllowsBackup,
            InventoryColumn.CleartextTraffic,
            InventoryColumn.SigningCertificateSha256,
            InventoryColumn.SigningCertificateSubject,
            InventoryColumn.HasMultipleSigners,
            InventoryColumn.SigningSchemes,
        ),
    ),
    PermissionsAndComponents(
        setOf(
            InventoryColumn.ApplicationName,
            InventoryColumn.PackageName,
            InventoryColumn.RequestedPermissions,
            InventoryColumn.GrantedPermissions,
            InventoryColumn.ActivityCount,
            InventoryColumn.ServiceCount,
            InventoryColumn.ReceiverCount,
            InventoryColumn.ProviderCount,
        ),
    ),
    Everything(InventoryColumn.entries.toSet()),
    ;

    companion object {
        val Default = Basics

        fun matching(columns: Set<InventoryColumn>): InventoryPreset? = entries.firstOrNull { it.columns == columns }
    }
}
