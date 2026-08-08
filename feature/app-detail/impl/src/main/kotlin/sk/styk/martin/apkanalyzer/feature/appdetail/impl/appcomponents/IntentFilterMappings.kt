package sk.styk.martin.apkanalyzer.feature.appdetail.impl.appcomponents

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.toImmutableList
import sk.styk.martin.apkanalyzer.core.apps.model.ComponentIntentFilter
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.R

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

@Composable
internal fun String.toDisplayRequestType(): String = if (startsWith(FRAMEWORK_ACTION_PREFIX)) {
    removePrefix(FRAMEWORK_ACTION_PREFIX)
} else {
    stringResource(R.string.intent_filters_action_custom)
}

@Composable
internal fun String.toDisplayCategory(): String = if (startsWith(FRAMEWORK_CATEGORY_PREFIX)) {
    removePrefix(FRAMEWORK_CATEGORY_PREFIX)
} else {
    stringResource(R.string.intent_filters_category_custom)
}

private const val FRAMEWORK_ACTION_PREFIX = "android.intent.action."
private const val FRAMEWORK_CATEGORY_PREFIX = "android.intent.category."
