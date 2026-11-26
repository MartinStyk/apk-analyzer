package sk.styk.martin.apkanalyzer.feature.appdetail.phone.appcomponents

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
import sk.styk.martin.apkanalyzer.core.apps.api.components.IntentFilterDataRuleType
import sk.styk.martin.apkanalyzer.core.apps.api.components.labelRes
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.BottomSheet
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.R
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.DetailField
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.R as DomainR

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
                    label = stringResource(DomainR.string.components_detail_intent_actions),
                    value = filter.actions.map { it.toDisplayRequestType() }.joinToString(separator = "\n"),
                    explanation = stringResource(R.string.components_detail_intent_actions_explanation),
                    onCopy = onCopy,
                )
            }
            if (filter.categories.isNotEmpty()) {
                DetailField(
                    label = stringResource(DomainR.string.components_detail_intent_categories),
                    value = filter.categories.map { it.toDisplayCategory() }.joinToString(separator = "\n"),
                    explanation = stringResource(R.string.components_detail_intent_categories_explanation),
                    onCopy = onCopy,
                )
            }
            if (filter.dataRules.isNotEmpty()) {
                DetailField(
                    label = stringResource(DomainR.string.components_detail_intent_data),
                    value = filter.dataRules.toDisplayText(),
                    explanation = stringResource(R.string.components_detail_intent_data_explanation),
                    onCopy = onCopy,
                )
            }
            filter.uriRelativeGroups.forEach { group ->
                DetailField(
                    label = stringResource(
                        if (group.isAllowed) {
                            DomainR.string.components_detail_intent_allowed_uri_parts
                        } else {
                            DomainR.string.components_detail_intent_blocked_uri_parts
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
                    label = stringResource(DomainR.string.components_detail_intent_auto_verify),
                    value = stringResource(DomainR.string.components_detail_intent_auto_verify_requested),
                    explanation = stringResource(DomainR.string.components_detail_intent_auto_verify_explanation),
                    onCopy = onCopy,
                )
            }
            if (filter.priority != 0) {
                val (labelRes, explanationRes) = when (componentType) {
                    ComponentType.Activity ->
                        DomainR.string.components_detail_intent_activity_priority to
                            R.string.components_detail_intent_activity_priority_explanation

                    ComponentType.Receiver ->
                        DomainR.string.components_detail_intent_receiver_priority to
                            R.string.components_detail_intent_receiver_priority_explanation

                    ComponentType.Service,
                    ComponentType.Provider,
                    -> DomainR.string.components_detail_intent_declared_priority to
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
                    label = stringResource(DomainR.string.components_detail_intent_order),
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
private fun List<IntentFilterDataRuleItem>.toDisplayText(): String = map { rule ->
    stringResource(
        DomainR.string.components_detail_intent_data_rule,
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
                actions = listOf("android.intent.action.VIEW"),
                categories = listOf(
                    "android.intent.category.DEFAULT",
                    "android.intent.category.BROWSABLE",
                ),
                dataRules = listOf(
                    IntentFilterDataRuleItem(IntentFilterDataRuleType.Scheme, "https"),
                    IntentFilterDataRuleItem(IntentFilterDataRuleType.Host, "open.spotify.com"),
                    IntentFilterDataRuleItem(IntentFilterDataRuleType.PathPrefix, "/track/"),
                ),
                uriRelativeGroups = listOf(),
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
