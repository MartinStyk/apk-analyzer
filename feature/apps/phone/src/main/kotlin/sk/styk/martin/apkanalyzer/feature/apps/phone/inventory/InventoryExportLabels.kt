package sk.styk.martin.apkanalyzer.feature.apps.phone.inventory

import androidx.annotation.StringRes
import sk.styk.martin.apkanalyzer.core.appexport.InventoryAttribute
import sk.styk.martin.apkanalyzer.core.appexport.InventoryAttributeGroup
import sk.styk.martin.apkanalyzer.core.appexport.InventoryFormat
import sk.styk.martin.apkanalyzer.feature.apps.phone.R
import sk.styk.martin.apkanalyzer.core.apppermissions.R as AppPermissionsR
import sk.styk.martin.apkanalyzer.core.apps.R as AppsR

@Suppress("CyclomaticComplexMethod")
@StringRes
internal fun InventoryAttribute.labelRes(): Int = when (this) {
    InventoryAttribute.ApplicationName -> AppsR.string.general_info_application_name
    InventoryAttribute.PackageName -> AppsR.string.general_info_package_name
    InventoryAttribute.ProcessName -> AppsR.string.general_info_process_name
    InventoryAttribute.Uid -> AppsR.string.general_info_uid
    InventoryAttribute.SharedUserId -> AppsR.string.general_info_shared_user_id
    InventoryAttribute.Category -> R.string.inventory_export_attribute_category
    InventoryAttribute.VersionName -> AppsR.string.general_info_version_name
    InventoryAttribute.VersionCode -> AppsR.string.general_info_version_code
    InventoryAttribute.MinSdk -> AppsR.string.general_info_min_sdk
    InventoryAttribute.TargetSdk -> AppsR.string.general_info_target_sdk
    InventoryAttribute.AppType -> AppsR.string.general_info_app_type
    InventoryAttribute.InstallSource -> AppsR.string.general_info_install_source
    InventoryAttribute.InstallerPackage -> AppsR.string.general_info_installer_package
    InventoryAttribute.InstallInitiator -> AppsR.string.general_info_install_initiator
    InventoryAttribute.InstallOriginator -> AppsR.string.general_info_install_originator
    InventoryAttribute.FirstInstalled -> AppsR.string.general_info_first_installed
    InventoryAttribute.LastUpdated -> AppsR.string.general_info_last_updated
    InventoryAttribute.LastUsed -> AppsR.string.general_info_last_used
    InventoryAttribute.ApkSize -> AppsR.string.general_info_apk_size
    InventoryAttribute.TotalSize -> AppsR.string.general_info_total_size
    InventoryAttribute.SplitApks -> AppsR.string.general_info_split_apks
    InventoryAttribute.InstallLocation -> AppsR.string.general_info_install_location
    InventoryAttribute.ApkDirectory -> AppsR.string.general_info_apk_directory
    InventoryAttribute.DataDirectory -> AppsR.string.general_info_data_directory
    InventoryAttribute.Debuggable -> AppsR.string.general_info_debug_access
    InventoryAttribute.AllowsBackup -> AppsR.string.general_info_app_data_backup
    InventoryAttribute.CleartextTraffic -> AppsR.string.general_info_unencrypted_traffic
    InventoryAttribute.SigningCertificateSha256 -> R.string.inventory_export_attribute_certificate_sha256
    InventoryAttribute.SigningCertificateSubject -> R.string.inventory_export_attribute_certificate_subject
    InventoryAttribute.RequestedPermissions -> R.string.inventory_export_attribute_requested_permissions
    InventoryAttribute.GrantedPermissions -> R.string.inventory_export_attribute_granted_permissions
    InventoryAttribute.ActivityCount -> AppsR.string.components_type_activities
    InventoryAttribute.ServiceCount -> AppsR.string.components_type_services
    InventoryAttribute.ReceiverCount -> AppsR.string.components_type_receivers
    InventoryAttribute.ProviderCount -> AppsR.string.components_type_providers
}

@StringRes
internal fun InventoryAttributeGroup.labelRes(): Int = when (this) {
    InventoryAttributeGroup.Identification -> AppsR.string.general_info_section_identification
    InventoryAttributeGroup.Versioning -> AppsR.string.general_info_section_versioning
    InventoryAttributeGroup.ApiTargets -> AppsR.string.general_info_section_api_targets
    InventoryAttributeGroup.Installation -> AppsR.string.general_info_section_installation
    InventoryAttributeGroup.Storage -> AppsR.string.general_info_section_storage
    InventoryAttributeGroup.Security -> AppsR.string.general_info_section_security
    InventoryAttributeGroup.Signing -> AppsR.string.certificates_title
    InventoryAttributeGroup.Permissions -> AppPermissionsR.string.permissions_title
    InventoryAttributeGroup.Components -> AppsR.string.components_title
}

@StringRes
internal fun InventoryFormat.labelRes(): Int = when (this) {
    InventoryFormat.Csv -> R.string.inventory_export_format_csv
    InventoryFormat.Json -> R.string.inventory_export_format_json
}

@StringRes
internal fun InventoryFormat.descriptionRes(): Int = when (this) {
    InventoryFormat.Csv -> R.string.inventory_export_format_csv_description
    InventoryFormat.Json -> R.string.inventory_export_format_json_description
}
