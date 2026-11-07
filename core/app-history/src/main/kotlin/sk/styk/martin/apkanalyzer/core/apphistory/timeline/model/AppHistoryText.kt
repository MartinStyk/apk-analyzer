package sk.styk.martin.apkanalyzer.core.apphistory.timeline.model

import android.content.res.Resources
import androidx.annotation.StringRes
import sk.styk.martin.apkanalyzer.core.apphistory.R
import sk.styk.martin.apkanalyzer.core.apps.installsource.labelRes
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.bytes
import kotlin.math.abs
import sk.styk.martin.apkanalyzer.core.apppermissions.R as PermissionsR
import sk.styk.martin.apkanalyzer.core.apps.R as AppsR

private const val VISIBLE_LINES = 2

@get:StringRes
val AppHistoryEntry.Change.Kind.breakHeadlineRes: Int?
    get() = when (this) {
        AppHistoryEntry.Change.Kind.Updated -> null
        AppHistoryEntry.Change.Kind.Reinstalled -> R.string.app_history_reinstalled
        AppHistoryEntry.Change.Kind.NewDevice -> R.string.app_history_new_device
    }

@get:StringRes
val AppHistoryEntry.Change.Kind.certificateExplanationRes: Int
    get() = when (this) {
        AppHistoryEntry.Change.Kind.Updated -> R.string.app_history_certificate_updated_explanation
        AppHistoryEntry.Change.Kind.Reinstalled, AppHistoryEntry.Change.Kind.NewDevice -> R.string.app_history_certificate_new_copy_explanation
    }

fun AppHistoryVersion.label(resources: Resources): String = resources.getString(R.string.app_history_version, displayName)

fun ValueChange<AppHistoryVersion>.label(resources: Resources): String {
    val hasSameName = before.versionName != null && before.versionName == after.versionName
    val beforeText = if (hasSameName) before.nameWithCode(resources) else before.displayName
    val afterText = if (hasSameName) after.nameWithCode(resources) else after.displayName
    return resources.getString(R.string.app_history_value_change, beforeText, afterText)
}

private val AppHistoryVersion.displayName: String
    get() = versionName ?: versionCode.toString()

private fun AppHistoryVersion.nameWithCode(resources: Resources): String = resources.getString(R.string.app_history_version_with_code, versionName, versionCode)

fun ValueChange<AppHistoryInstallSource>.installSourceLabel(resources: Resources): String =
    resources.getString(R.string.app_history_value_change, before.text(this, resources), after.text(this, resources))

private fun AppHistoryInstallSource.text(change: ValueChange<AppHistoryInstallSource>, resources: Resources): String {
    val hasSameLabel = change.before.source == change.after.source
    return installingPackage?.value?.takeIf { hasSameLabel } ?: resources.getString(source.labelRes)
}

fun List<String>.collapsedToTwoLines(resources: Resources): List<String> {
    val hiddenCount = size - VISIBLE_LINES
    if (hiddenCount <= 0) return this
    val more = resources.getQuantityString(R.plurals.app_history_summary_more, hiddenCount, hiddenCount)
    return listOf(get(0), resources.getString(R.string.app_history_summary_join, get(1), more))
}

fun ValueChange<AppSize>.sizeLabel(resources: Resources): String {
    val delta = after.bytes - before.bytes
    return resources.getString(
        if (delta >= 0) R.string.app_history_size_change_up else R.string.app_history_size_change_down,
        before.formatted(),
        after.formatted(),
        abs(delta).bytes.formatted(),
    )
}

@Suppress("CyclomaticComplexMethod")
fun AppHistorySummaryPart.label(resources: Resources): String = when (this) {
    AppHistorySummaryPart.SigningMethodChanged -> resources.getString(R.string.app_history_signing_method_changed)

    is AppHistorySummaryPart.InstallSourceChanged -> resources.getString(R.string.app_history_summary_source, change.after.text(change, resources))

    is AppHistorySummaryPart.DebuggingChanged ->
        resources.getString(if (isEnabled) R.string.app_history_summary_debug_on else R.string.app_history_summary_debug_off)

    is AppHistorySummaryPart.BackupChanged ->
        resources.getString(if (isAllowed) R.string.app_history_summary_backup_on else R.string.app_history_summary_backup_off)

    is AppHistorySummaryPart.UnencryptedTrafficChanged ->
        resources.getString(if (isAllowed) R.string.app_history_summary_cleartext_on else R.string.app_history_summary_cleartext_off)

    is AppHistorySummaryPart.PermissionsAdded -> if (otherCount == 0) {
        resources.getString(R.string.app_history_summary_permission_added_single, firstLabel)
    } else {
        resources.getQuantityString(R.plurals.app_history_summary_permission_added_named, otherCount, firstLabel, otherCount)
    }

    is AppHistorySummaryPart.PermissionsRemoved -> resources.getQuantityString(R.plurals.app_history_summary_permissions_removed, count, count)

    is AppHistorySummaryPart.DefinedPermissionsChanged -> resources.getQuantityString(R.plurals.app_history_summary_defined_permissions, count, count)

    is AppHistorySummaryPart.TargetSdkChanged -> resources.getString(
        R.string.app_history_summary_target_sdk,
        before.sdkText(resources),
        after.sdkText(resources),
    )

    is AppHistorySummaryPart.MinSdkChanged -> resources.getString(R.string.app_history_summary_min_sdk, before.sdkText(resources), after.sdkText(resources))

    is AppHistorySummaryPart.ComponentsAdded -> resources.getQuantityString(R.plurals.app_history_summary_components_added, count, count)

    is AppHistorySummaryPart.ComponentsRemoved -> resources.getQuantityString(R.plurals.app_history_summary_components_removed, count, count)

    is AppHistorySummaryPart.ComponentsChanged -> resources.getQuantityString(R.plurals.app_history_summary_components_changed, count, count)

    is AppHistorySummaryPart.NativeLibrariesAdded -> resources.getQuantityString(R.plurals.app_history_summary_native_added, count, count)

    is AppHistorySummaryPart.NativeLibrariesRemoved -> resources.getQuantityString(R.plurals.app_history_summary_native_removed, count, count)

    is AppHistorySummaryPart.NativeLibrariesChanged -> resources.getQuantityString(R.plurals.app_history_summary_native_changed, count, count)

    is AppHistorySummaryPart.SizeChanged -> {
        val delta = after.bytes - before.bytes
        resources.getString(
            if (delta >= 0) R.string.app_history_summary_size_up else R.string.app_history_summary_size_down,
            abs(delta).bytes.formatted(),
        )
    }
}

private fun Int?.sdkText(resources: Resources): String = this?.toString() ?: resources.getString(AppsR.string.app_detail_unknown)

fun AppHistoryFieldChange.label(resources: Resources): String = when (this) {
    is AppHistoryFieldChange.Flag ->
        resources.getString(R.string.app_history_field_change, resources.getString(field.labelRes), before.yesNo(resources), after.yesNo(resources))

    is AppHistoryFieldChange.Value -> {
        val fieldLabel = resources.getString(field.labelRes)
        when {
            before == null -> resources.getString(R.string.app_history_field_added, fieldLabel)
            after == null -> resources.getString(R.string.app_history_field_removed, fieldLabel)
            else -> resources.getString(R.string.app_history_field_change, fieldLabel, before, after)
        }
    }

    is AppHistoryFieldChange.SizeValue -> resources.getString(R.string.app_history_field_value, abi, change.sizeLabel(resources))

    is AppHistoryFieldChange.Content -> resources.getString(R.string.app_history_field_changed, resources.getString(field.labelRes))
}

private fun Boolean.yesNo(resources: Resources): String =
    resources.getString(if (this) AppsR.string.components_exported_yes else AppsR.string.components_exported_no)

@get:StringRes
private val AppHistoryItemField.labelRes: Int
    get() = when (this) {
        AppHistoryItemField.Exported -> AppsR.string.components_detail_exported
        AppHistoryItemField.Permission -> AppsR.string.components_detail_permission
        AppHistoryItemField.ReadPermission -> AppsR.string.components_detail_read_permission
        AppHistoryItemField.WritePermission -> AppsR.string.components_detail_write_permission
        AppHistoryItemField.Authority -> AppsR.string.components_detail_authority
        AppHistoryItemField.PathPermissions -> AppsR.string.components_detail_section_path_permissions
        AppHistoryItemField.TargetActivity -> AppsR.string.components_detail_target_activity
        AppHistoryItemField.ParentActivity -> AppsR.string.components_detail_parent
        AppHistoryItemField.Launcher -> AppsR.string.components_detail_launcher
        AppHistoryItemField.StopWithTask -> AppsR.string.components_flag_stop_with_task
        AppHistoryItemField.SingleUser -> AppsR.string.components_flag_single_user
        AppHistoryItemField.IsolatedProcess -> AppsR.string.components_flag_isolated_process
        AppHistoryItemField.ExternalService -> AppsR.string.components_flag_external_service
        AppHistoryItemField.ProtectionLevel -> PermissionsR.string.permissions_detail_protection_level
        AppHistoryItemField.PermissionGroup -> PermissionsR.string.permissions_detail_group
        AppHistoryItemField.DeclaringPackage -> PermissionsR.string.permissions_detail_declared_by
        AppHistoryItemField.ExtraRules -> PermissionsR.string.permissions_detail_extra_rules
    }
