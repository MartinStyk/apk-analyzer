package sk.styk.martin.apkanalyzer.feature.appdetail.impl.appcomponents

import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilter

internal fun List<ComponentIntentFilter>.toItems() = mapIndexed { index, filter ->
    ComponentIntentFilterItem(
        index = index,
        actions = filter.actions,
        categories = filter.categories,
        dataRules = filter.dataRules.map { rule ->
            IntentFilterDataRuleItem(type = rule.type, value = rule.value)
        },
        uriRelativeGroups = filter.uriRelativeGroups.map { group ->
            IntentFilterUriRelativeGroupItem(
                isAllowed = group.isAllowed,
                dataRules = group.dataRules.map { rule ->
                    IntentFilterDataRuleItem(type = rule.type, value = rule.value)
                },
            )
        },
        priority = filter.priority,
        order = filter.order,
        isAutoVerify = filter.isAutoVerify,
    )
}

internal fun String.toDisplayRequestType(): String = removePrefix(FRAMEWORK_ACTION_PREFIX)

internal fun String.toDisplayCategory(): String = removePrefix(FRAMEWORK_CATEGORY_PREFIX)

private const val FRAMEWORK_ACTION_PREFIX = "android.intent.action."
private const val FRAMEWORK_CATEGORY_PREFIX = "android.intent.category."
