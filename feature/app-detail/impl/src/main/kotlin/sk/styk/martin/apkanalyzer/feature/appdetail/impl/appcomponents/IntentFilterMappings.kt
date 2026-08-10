package sk.styk.martin.apkanalyzer.feature.appdetail.impl.appcomponents

import kotlinx.collections.immutable.toImmutableList
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilter

internal fun List<ComponentIntentFilter>.toItems() = mapIndexed { index, filter ->
    ComponentIntentFilterItem(
        index = index,
        actions = filter.actions.toImmutableList(),
        categories = filter.categories.toImmutableList(),
        dataRules = filter.dataRules.map { rule ->
            IntentFilterDataRuleItem(type = rule.type, value = rule.value)
        }.toImmutableList(),
        uriRelativeGroups = filter.uriRelativeGroups.map { group ->
            IntentFilterUriRelativeGroupItem(
                isAllowed = group.isAllowed,
                dataRules = group.dataRules.map { rule ->
                    IntentFilterDataRuleItem(type = rule.type, value = rule.value)
                }.toImmutableList(),
            )
        }.toImmutableList(),
        priority = filter.priority,
        order = filter.order,
        isAutoVerify = filter.isAutoVerify,
    )
}.toImmutableList()

internal fun String.toDisplayRequestType(): String = removePrefix(FRAMEWORK_ACTION_PREFIX)

internal fun String.toDisplayCategory(): String = removePrefix(FRAMEWORK_CATEGORY_PREFIX)

private const val FRAMEWORK_ACTION_PREFIX = "android.intent.action."
private const val FRAMEWORK_CATEGORY_PREFIX = "android.intent.category."
