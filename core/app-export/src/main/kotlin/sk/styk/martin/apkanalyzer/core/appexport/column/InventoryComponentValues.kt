package sk.styk.martin.apkanalyzer.core.appexport.column

import sk.styk.martin.apkanalyzer.core.apps.components.Activity
import sk.styk.martin.apkanalyzer.core.apps.components.BroadcastReceiver
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilter
import sk.styk.martin.apkanalyzer.core.apps.components.ContentProvider
import sk.styk.martin.apkanalyzer.core.apps.components.IntentFilterDataRule
import sk.styk.martin.apkanalyzer.core.apps.components.Service

internal fun Activity.toInventoryValue(): InventoryValue = record(
    "name" to text(name),
    "package_name" to text(packageName.value),
    "label" to text(label),
    "target_activity" to text(targetActivity),
    "permission" to text(permission),
    "parent_activity" to text(parentName),
    "exported" to flag(isExported),
    "launcher" to flag(isLauncher),
)

internal fun Service.toInventoryValue(): InventoryValue = record(
    "name" to text(name),
    "permission" to text(permission),
    "exported" to flag(isExported),
    "stop_with_task" to flag(isStopWithTask),
    "single_user" to flag(isSingleUser),
    "isolated_process" to flag(isIsolatedProcess),
    "external_service" to flag(isExternalService),
)

internal fun BroadcastReceiver.toInventoryValue(): InventoryValue = record(
    "name" to text(name),
    "permission" to text(permission),
    "exported" to flag(isExported),
)

internal fun ContentProvider.toInventoryValue(): InventoryValue = record(
    "name" to text(name),
    "authority" to text(authority),
    "read_permission" to text(readPermission),
    "write_permission" to text(writePermission),
    "exported" to flag(isExported),
    "path_permissions" to records(pathPermissions) {
        record(
            "path" to text(path),
            "match_type" to text(matchType.token),
            "read_permission" to text(readPermission),
            "write_permission" to text(writePermission),
        )
    },
)

internal fun ComponentIntentFilter.toInventoryValue(): InventoryValue = record(
    "actions" to InventoryValue.TextList(actions),
    "categories" to InventoryValue.TextList(categories),
    "data_rules" to records(dataRules) { toInventoryValue() },
    "uri_relative_groups" to records(uriRelativeGroups) {
        record(
            "allowed" to flag(isAllowed),
            "data_rules" to records(dataRules) { toInventoryValue() },
        )
    },
    "priority" to number(priority.toLong()),
    "order" to number(order.toLong()),
    "auto_verify" to flag(isAutoVerify),
)

private fun IntentFilterDataRule.toInventoryValue(): InventoryValue = record(
    "type" to text(type.manifestAttribute),
    "value" to text(value),
)
