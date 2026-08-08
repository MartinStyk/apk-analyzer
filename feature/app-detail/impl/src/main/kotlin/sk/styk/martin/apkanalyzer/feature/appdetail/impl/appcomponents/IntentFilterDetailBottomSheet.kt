package sk.styk.martin.apkanalyzer.feature.appdetail.impl.appcomponents

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import sk.styk.martin.apkanalyzer.core.apps.model.IntentFilterDataRuleType
import sk.styk.martin.apkanalyzer.core.uilibrary.components.BottomSheet
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.R
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.DetailField

@Composable
internal fun IntentFilterDetailBottomSheet(
    filter: ComponentIntentFilterItem,
    componentType: ComponentType,
    onCopy: (label: String, value: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BottomSheet(onDismiss = onDismiss, modifier = modifier) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = filter.displayTitle(componentType),
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.onSurface,
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (filter.actions.isNotEmpty()) {
                DetailField(
                    label = stringResource(R.string.components_detail_intent_actions),
                    value = filter.actions.map { it.toDisplayRequestType() }.joinToString(separator = "\n"),
                    explanation = stringResource(R.string.components_detail_intent_actions_explanation),
                    onCopy = onCopy,
                )
            }
            if (filter.categories.isNotEmpty()) {
                DetailField(
                    label = stringResource(R.string.components_detail_intent_categories),
                    value = filter.categories.map { it.toDisplayCategory() }.joinToString(separator = "\n"),
                    explanation = stringResource(R.string.components_detail_intent_categories_explanation),
                    onCopy = onCopy,
                )
            }
            if (filter.dataRules.isNotEmpty()) {
                DetailField(
                    label = stringResource(R.string.components_detail_intent_data),
                    value = filter.dataRules.toDisplayText(),
                    explanation = stringResource(R.string.components_detail_intent_data_explanation),
                    onCopy = onCopy,
                )
            }
            filter.uriRelativeGroups.forEach { group ->
                DetailField(
                    label = stringResource(
                        if (group.isAllowed) {
                            R.string.components_detail_intent_allowed_uri_parts
                        } else {
                            R.string.components_detail_intent_blocked_uri_parts
                        },
                    ),
                    value = group.dataRules.toDisplayText(),
                    explanation = stringResource(
                        if (group.isAllowed) {
                            R.string.components_detail_intent_allowed_uri_parts_explanation
                        } else {
                            R.string.components_detail_intent_blocked_uri_parts_explanation
                        },
                    ),
                    onCopy = onCopy,
                )
            }
            if (filter.isAutoVerify) {
                DetailField(
                    label = stringResource(R.string.components_detail_intent_auto_verify),
                    value = stringResource(R.string.components_detail_intent_auto_verify_requested),
                    explanation = stringResource(R.string.components_detail_intent_auto_verify_explanation),
                    onCopy = onCopy,
                )
            }
            if (filter.priority != 0) {
                val (labelRes, explanationRes) = when (componentType) {
                    ComponentType.Activity ->
                        R.string.components_detail_intent_activity_priority to
                            R.string.components_detail_intent_activity_priority_explanation

                    ComponentType.Receiver ->
                        R.string.components_detail_intent_receiver_priority to
                            R.string.components_detail_intent_receiver_priority_explanation

                    ComponentType.Service,
                    ComponentType.Provider,
                    -> R.string.components_detail_intent_declared_priority to
                        R.string.components_detail_intent_declared_priority_explanation
                }
                DetailField(
                    label = stringResource(labelRes),
                    value = filter.priority.toString(),
                    explanation = stringResource(explanationRes),
                    onCopy = onCopy,
                )
            }
            if (filter.order != 0) {
                DetailField(
                    label = stringResource(R.string.components_detail_intent_order),
                    value = filter.order.toString(),
                    explanation = stringResource(R.string.components_detail_intent_order_explanation),
                    onCopy = onCopy,
                )
            }
        }
    }
}

@Composable
internal fun ComponentIntentFilterItem.displayTitle(componentType: ComponentType): String = actions.firstOrNull()
    ?.let { stringResource(purposeTitleRes(componentType)) }
    ?: stringResource(R.string.intent_filters_filter_fallback)

@Composable
private fun ImmutableList<IntentFilterDataRuleItem>.toDisplayText(): String = map { rule ->
    stringResource(
        R.string.components_detail_intent_data_rule,
        stringResource(rule.type.labelRes),
        rule.value,
    )
}.joinToString(separator = "\n")

@Preview
@Composable
private fun IntentFilterDetailBottomSheetPreview() {
    ApkAnalyzerTheme {
        IntentFilterDetailBottomSheet(
            filter = ComponentIntentFilterItem(
                index = 0,
                actions = persistentListOf("android.intent.action.VIEW"),
                categories = persistentListOf(
                    "android.intent.category.DEFAULT",
                    "android.intent.category.BROWSABLE",
                ),
                dataRules = persistentListOf(
                    IntentFilterDataRuleItem(IntentFilterDataRuleType.Scheme, "https"),
                    IntentFilterDataRuleItem(IntentFilterDataRuleType.Host, "open.spotify.com"),
                    IntentFilterDataRuleItem(IntentFilterDataRuleType.PathPrefix, "/track/"),
                ),
                uriRelativeGroups = persistentListOf(),
                priority = 0,
                order = 0,
                isAutoVerify = true,
            ),
            componentType = ComponentType.Activity,
            onCopy = { _, _ -> },
            onDismiss = {},
        )
    }
}
