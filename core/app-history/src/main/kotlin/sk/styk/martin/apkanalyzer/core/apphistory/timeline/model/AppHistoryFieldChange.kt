package sk.styk.martin.apkanalyzer.core.apphistory.timeline.model

import sk.styk.martin.apkanalyzer.core.apps.components.Activity
import sk.styk.martin.apkanalyzer.core.apps.components.BroadcastReceiver
import sk.styk.martin.apkanalyzer.core.apps.components.ContentProvider
import sk.styk.martin.apkanalyzer.core.apps.components.Service
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraryFile
import sk.styk.martin.apkanalyzer.core.apps.permissions.Permission
import sk.styk.martin.apkanalyzer.core.common.model.AppSize

enum class AppHistoryItemField {
    Exported,
    Permission,
    ReadPermission,
    WritePermission,
    Authority,
    PathPermissions,
    TargetActivity,
    ParentActivity,
    Launcher,
    StopWithTask,
    SingleUser,
    IsolatedProcess,
    ExternalService,
    ProtectionLevel,
    PermissionGroup,
    DeclaringPackage,
    ExtraRules,
}

sealed interface AppHistoryFieldChange {
    data class Flag(
        val field: AppHistoryItemField,
        val before: Boolean,
        val after: Boolean,
    ) : AppHistoryFieldChange

    data class Value(
        val field: AppHistoryItemField,
        val before: String?,
        val after: String?,
    ) : AppHistoryFieldChange

    data class SizeValue(val abi: String, val change: ValueChange<AppSize>) : AppHistoryFieldChange

    data class Content(val field: AppHistoryItemField) : AppHistoryFieldChange
}

fun Activity.fieldChangesFrom(before: Activity): List<AppHistoryFieldChange> = listOfNotNull(
    flagChange(AppHistoryItemField.Exported, before.isExported, isExported),
    valueChange(AppHistoryItemField.Permission, before.permission, permission),
    valueChange(AppHistoryItemField.TargetActivity, before.targetActivity, targetActivity),
    valueChange(AppHistoryItemField.ParentActivity, before.parentName, parentName),
    launcherChange(before.isLauncher, isLauncher),
)

fun Service.fieldChangesFrom(before: Service): List<AppHistoryFieldChange> = listOfNotNull(
    flagChange(AppHistoryItemField.Exported, before.isExported, isExported),
    valueChange(AppHistoryItemField.Permission, before.permission, permission),
    flagChange(AppHistoryItemField.StopWithTask, before.isStopWithTask, isStopWithTask),
    flagChange(AppHistoryItemField.SingleUser, before.isSingleUser, isSingleUser),
    flagChange(AppHistoryItemField.IsolatedProcess, before.isIsolatedProcess, isIsolatedProcess),
    flagChange(AppHistoryItemField.ExternalService, before.isExternalService, isExternalService),
)

fun BroadcastReceiver.fieldChangesFrom(before: BroadcastReceiver): List<AppHistoryFieldChange> = listOfNotNull(
    flagChange(AppHistoryItemField.Exported, before.isExported, isExported),
    valueChange(AppHistoryItemField.Permission, before.permission, permission),
)

fun ContentProvider.fieldChangesFrom(before: ContentProvider): List<AppHistoryFieldChange> = listOfNotNull(
    flagChange(AppHistoryItemField.Exported, before.isExported, isExported),
    valueChange(AppHistoryItemField.Authority, before.authority, authority),
    valueChange(AppHistoryItemField.ReadPermission, before.readPermission, readPermission),
    valueChange(AppHistoryItemField.WritePermission, before.writePermission, writePermission),
    AppHistoryFieldChange.Content(AppHistoryItemField.PathPermissions).takeIf { before.pathPermissions != pathPermissions },
)

fun Permission.fieldChangesFrom(before: Permission): List<AppHistoryFieldChange> = listOfNotNull(
    valueChange(AppHistoryItemField.ProtectionLevel, before.details?.protectionLevel?.name, details?.protectionLevel?.name),
    valueChange(AppHistoryItemField.PermissionGroup, before.details?.groupName, details?.groupName),
    valueChange(AppHistoryItemField.DeclaringPackage, before.details?.declaringPackage?.value, details?.declaringPackage?.value),
    AppHistoryFieldChange.Content(AppHistoryItemField.ExtraRules).takeIf { before.details?.protectionFlags != details?.protectionFlags },
)

fun List<ValueChange<NativeLibraryFile>>.fieldChangesByLibraryName(): Map<String, List<AppHistoryFieldChange>> = groupBy { it.after.name }
    .mapValues { (_, files) ->
        files.sortedBy { it.after.abi }.map { AppHistoryFieldChange.SizeValue(it.after.abi, ValueChange(it.before.size, it.after.size)) }
    }

private fun flagChange(
    field: AppHistoryItemField,
    before: Boolean,
    after: Boolean,
): AppHistoryFieldChange? = AppHistoryFieldChange.Flag(field, before, after).takeIf { before != after }

private fun valueChange(
    field: AppHistoryItemField,
    before: String?,
    after: String?,
): AppHistoryFieldChange? = AppHistoryFieldChange.Value(field, before, after).takeIf { before != after }

private fun launcherChange(before: Boolean?, after: Boolean?): AppHistoryFieldChange? {
    if (before == null || after == null) return null
    return flagChange(AppHistoryItemField.Launcher, before, after)
}
