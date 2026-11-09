package sk.styk.martin.apkanalyzer.core.apphistory.timeline.model

import sk.styk.martin.apkanalyzer.core.apps.components.Activity
import sk.styk.martin.apkanalyzer.core.apps.components.BroadcastReceiver
import sk.styk.martin.apkanalyzer.core.apps.components.ContentProvider
import sk.styk.martin.apkanalyzer.core.apps.components.ProviderPathPermission
import sk.styk.martin.apkanalyzer.core.apps.components.Service
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.Feature
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraryFile
import sk.styk.martin.apkanalyzer.core.apps.permissions.Permission
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionFlag
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.bytes

enum class AppHistoryItemField {
    Exported,
    Permission,
    ReadPermission,
    WritePermission,
    Authority,
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
    Required,
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

    data class Abis(val direction: RuleDirection, val abis: List<String>) : AppHistoryFieldChange

    data class PathRule(val direction: RuleDirection, val rule: ProviderPathPermission) : AppHistoryFieldChange

    data class ExtraRule(val direction: RuleDirection, val flag: ProtectionFlag) : AppHistoryFieldChange
}

enum class RuleDirection {
    Added,
    Removed,
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
) + ruleChanges(before.pathPermissions, pathPermissions, AppHistoryFieldChange::PathRule)

fun Permission.fieldChangesFrom(before: Permission): List<AppHistoryFieldChange> = listOfNotNull(
    valueChange(AppHistoryItemField.ProtectionLevel, before.details?.protectionLevel?.name, details?.protectionLevel?.name),
    valueChange(AppHistoryItemField.PermissionGroup, before.details?.groupName, details?.groupName),
    valueChange(AppHistoryItemField.DeclaringPackage, before.details?.declaringPackage?.value, details?.declaringPackage?.value),
) + ruleChanges(before.details?.protectionFlags.orEmpty(), details?.protectionFlags.orEmpty(), AppHistoryFieldChange::ExtraRule)

fun Feature.fieldChangesFrom(before: Feature): List<AppHistoryFieldChange> =
    listOfNotNull(flagChange(AppHistoryItemField.Required, before.isRequired, isRequired))

fun NativeLibrary.fieldChangesFrom(before: NativeLibrary): List<AppHistoryFieldChange> {
    val sizesBefore = before.files.sizeByAbi()
    val sizesAfter = files.sizeByAbi()
    val addedAbis = (sizesAfter.keys - sizesBefore.keys).sorted()
    val removedAbis = (sizesBefore.keys - sizesAfter.keys).sorted()
    val resized = sizesAfter.keys.intersect(sizesBefore.keys).sorted().mapNotNull { abi ->
        val change = ValueChange(sizesBefore.getValue(abi), sizesAfter.getValue(abi))
        AppHistoryFieldChange.SizeValue(abi, change).takeIf { change.before != change.after }
    }
    return listOfNotNull(
        AppHistoryFieldChange.Abis(RuleDirection.Added, addedAbis).takeIf { addedAbis.isNotEmpty() },
        AppHistoryFieldChange.Abis(RuleDirection.Removed, removedAbis).takeIf { removedAbis.isNotEmpty() },
    ) + resized
}

private fun List<NativeLibraryFile>.sizeByAbi(): Map<String, AppSize> = groupBy { it.abi }
    .mapValues { (_, files) -> files.fold(0.bytes) { total, file -> total + file.size } }

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

private fun <T> ruleChanges(
    before: Collection<T>,
    after: Collection<T>,
    toChange: (RuleDirection, T) -> AppHistoryFieldChange,
): List<AppHistoryFieldChange> = (before - after.toSet()).map { toChange(RuleDirection.Removed, it) } +
    (after - before.toSet()).map { toChange(RuleDirection.Added, it) }

private fun launcherChange(before: Boolean?, after: Boolean?): AppHistoryFieldChange? {
    if (before == null || after == null) return null
    return flagChange(AppHistoryItemField.Launcher, before, after)
}
