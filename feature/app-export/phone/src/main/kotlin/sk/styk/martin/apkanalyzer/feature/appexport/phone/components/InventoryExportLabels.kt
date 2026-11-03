package sk.styk.martin.apkanalyzer.feature.appexport.phone.components

import androidx.annotation.StringRes
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumn
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumnGroup
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryPreset
import sk.styk.martin.apkanalyzer.core.appexport.format.InventoryFormat
import sk.styk.martin.apkanalyzer.feature.appexport.phone.InventoryExportStep
import sk.styk.martin.apkanalyzer.feature.appexport.phone.R
import sk.styk.martin.apkanalyzer.core.apppermissions.R as AppPermissionsR
import sk.styk.martin.apkanalyzer.core.apps.R as AppsR

@Suppress("CyclomaticComplexMethod")
@StringRes
internal fun InventoryColumn.labelRes(): Int = when (this) {
    InventoryColumn.ApplicationName -> AppsR.string.general_info_application_name
    InventoryColumn.PackageName -> AppsR.string.general_info_package_name
    InventoryColumn.ProcessName -> AppsR.string.general_info_process_name
    InventoryColumn.Uid -> AppsR.string.general_info_uid
    InventoryColumn.SharedUserId -> AppsR.string.general_info_shared_user_id
    InventoryColumn.Category -> R.string.inventory_export_attribute_category
    InventoryColumn.VersionName -> AppsR.string.general_info_version_name
    InventoryColumn.VersionCode -> AppsR.string.general_info_version_code
    InventoryColumn.MinSdk -> AppsR.string.general_info_min_sdk
    InventoryColumn.TargetSdk -> AppsR.string.general_info_target_sdk
    InventoryColumn.AppType -> AppsR.string.general_info_app_type
    InventoryColumn.InstallSource -> AppsR.string.general_info_install_source
    InventoryColumn.InstallerPackage -> AppsR.string.general_info_installer_package
    InventoryColumn.InstallInitiator -> AppsR.string.general_info_install_initiator
    InventoryColumn.InstallOriginator -> AppsR.string.general_info_install_originator
    InventoryColumn.FirstInstalled -> AppsR.string.general_info_first_installed
    InventoryColumn.LastUpdated -> AppsR.string.general_info_last_updated
    InventoryColumn.LastUsed -> AppsR.string.general_info_last_used
    InventoryColumn.ApkSize -> AppsR.string.general_info_apk_size
    InventoryColumn.TotalSize -> AppsR.string.general_info_total_size
    InventoryColumn.SplitApks -> AppsR.string.general_info_split_apks
    InventoryColumn.InstallLocation -> AppsR.string.general_info_install_location
    InventoryColumn.ApkDirectory -> AppsR.string.general_info_apk_directory
    InventoryColumn.DataDirectory -> AppsR.string.general_info_data_directory
    InventoryColumn.Debuggable -> AppsR.string.general_info_debug_access
    InventoryColumn.AllowsBackup -> AppsR.string.general_info_app_data_backup
    InventoryColumn.CleartextTraffic -> AppsR.string.general_info_unencrypted_traffic
    InventoryColumn.SigningCertificateSha256 -> R.string.inventory_export_attribute_certificate_sha256
    InventoryColumn.SigningCertificateSubject -> R.string.inventory_export_attribute_certificate_subject
    InventoryColumn.RequestedPermissions -> R.string.inventory_export_attribute_requested_permissions
    InventoryColumn.GrantedPermissions -> R.string.inventory_export_attribute_granted_permissions
    InventoryColumn.ActivityCount -> R.string.inventory_export_attribute_activity_count
    InventoryColumn.ServiceCount -> R.string.inventory_export_attribute_service_count
    InventoryColumn.ReceiverCount -> R.string.inventory_export_attribute_receiver_count
    InventoryColumn.ProviderCount -> R.string.inventory_export_attribute_provider_count
    InventoryColumn.Description -> AppsR.string.general_info_description
    InventoryColumn.SplitApkDetails -> R.string.inventory_export_attribute_split_details
    InventoryColumn.NativeLibraryAbis -> AppsR.string.general_info_native_library_abis
    InventoryColumn.NativeLibraryFiles -> AppsR.string.general_info_native_library_files
    InventoryColumn.SigningCertificates -> AppsR.string.certificates_current_certificates
    InventoryColumn.SigningCertificateHistory -> AppsR.string.certificates_history
    InventoryColumn.HasMultipleSigners -> AppsR.string.certificates_multiple_signers
    InventoryColumn.SigningSchemes -> AppsR.string.certificates_scheme
    InventoryColumn.DefinedPermissions -> AppPermissionsR.string.permissions_scope_defined
    InventoryColumn.PermissionDetails -> R.string.inventory_export_attribute_permission_details
    InventoryColumn.Activities -> AppsR.string.components_type_activities
    InventoryColumn.Services -> AppsR.string.components_type_services
    InventoryColumn.Receivers -> AppsR.string.components_type_receivers
    InventoryColumn.Providers -> AppsR.string.components_type_providers
    InventoryColumn.IntentFilters -> R.string.inventory_export_attribute_intent_filters
    InventoryColumn.Features -> AppsR.string.requirements_title
    InventoryColumn.Manifest -> AppsR.string.manifest_title
    InventoryColumn.SigningCertificateMd5 -> R.string.inventory_export_attribute_certificate_md5
    InventoryColumn.SigningCertificateSha1 -> R.string.inventory_export_attribute_certificate_sha1
    InventoryColumn.SigningPublicKeyMd5 -> R.string.inventory_export_attribute_public_key_md5
    InventoryColumn.SigningPublicKeySha1 -> R.string.inventory_export_attribute_public_key_sha1
    InventoryColumn.SigningPublicKeySha256 -> R.string.inventory_export_attribute_public_key_sha256
}

@StringRes
internal fun InventoryColumnGroup.labelRes(): Int = when (this) {
    InventoryColumnGroup.Identification -> AppsR.string.general_info_section_identification
    InventoryColumnGroup.Versioning -> AppsR.string.general_info_section_versioning
    InventoryColumnGroup.ApiTargets -> AppsR.string.general_info_section_api_targets
    InventoryColumnGroup.Installation -> AppsR.string.general_info_section_installation
    InventoryColumnGroup.Storage -> AppsR.string.general_info_section_storage
    InventoryColumnGroup.Security -> AppsR.string.general_info_section_security
    InventoryColumnGroup.Signing -> AppsR.string.certificates_title
    InventoryColumnGroup.Permissions -> AppPermissionsR.string.permissions_title
    InventoryColumnGroup.Components -> AppsR.string.components_title
    InventoryColumnGroup.Requirements -> AppsR.string.requirements_title
    InventoryColumnGroup.Manifest -> AppsR.string.manifest_title
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

@StringRes
internal fun InventoryPreset.labelRes(): Int = when (this) {
    InventoryPreset.Basics -> R.string.inventory_export_preset_basics
    InventoryPreset.SecurityAudit -> R.string.inventory_export_preset_security_audit
    InventoryPreset.PermissionsAndComponents -> R.string.inventory_export_preset_permissions
    InventoryPreset.Everything -> R.string.inventory_export_preset_everything
}

@StringRes
internal fun InventoryExportStep.labelRes(): Int = when (this) {
    InventoryExportStep.Apps -> R.string.inventory_export_apps_page_title
    InventoryExportStep.Columns -> R.string.inventory_export_columns_page_title
    InventoryExportStep.Export -> R.string.inventory_export_step_export
}

@StringRes
internal fun InventoryExportStep.guidanceRes(): Int = when (this) {
    InventoryExportStep.Apps -> R.string.inventory_export_apps_guidance
    InventoryExportStep.Columns -> R.string.inventory_export_columns_guidance
    InventoryExportStep.Export -> R.string.inventory_export_format_guidance
}

@StringRes
internal fun InventoryPreset.descriptionRes(): Int = when (this) {
    InventoryPreset.Basics -> R.string.inventory_export_preset_basics_description
    InventoryPreset.SecurityAudit -> R.string.inventory_export_preset_security_audit_description
    InventoryPreset.PermissionsAndComponents -> R.string.inventory_export_preset_permissions_description
    InventoryPreset.Everything -> R.string.inventory_export_preset_everything_description
}
