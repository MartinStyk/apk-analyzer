package sk.styk.martin.apkanalyzer.core.apphistory.capture.snapshot

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilter
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilterKey
import sk.styk.martin.apkanalyzer.core.apps.components.IntentFilterDataRule
import sk.styk.martin.apkanalyzer.core.apps.components.IntentFilterUriRelativeGroup

@Serializable
internal data class ComponentIntentFilterEntrySnapshot(val key: ComponentIntentFilterKeySnapshot, val filters: List<ComponentIntentFilterSnapshot>)

@Serializable
internal data class ComponentIntentFilterKeySnapshot(val name: String, val kind: String)

@Serializable
internal data class ComponentIntentFilterSnapshot(
    val actions: List<String>,
    val categories: List<String>,
    val dataRules: List<IntentFilterDataRuleSnapshot>,
    val uriRelativeGroups: List<IntentFilterUriRelativeGroupSnapshot>,
    val priority: Int,
    val order: Int,
    val isAutoVerify: Boolean,
)

@Serializable
internal data class IntentFilterUriRelativeGroupSnapshot(val isAllowed: Boolean, val dataRules: List<IntentFilterDataRuleSnapshot>)

@Serializable
internal data class IntentFilterDataRuleSnapshot(val type: String, val value: String)

internal fun ComponentIntentFilterKey.toSnapshot() = ComponentIntentFilterKeySnapshot(name = name, kind = kind.name)

internal fun ComponentIntentFilter.toSnapshot() = ComponentIntentFilterSnapshot(
    actions = actions.sorted(),
    categories = categories.sorted(),
    dataRules = dataRules.map { it.toSnapshot() }.sortedDataRules(),
    uriRelativeGroups = uriRelativeGroups.map { it.toSnapshot() }.sortedBy { Json.encodeToString(it) },
    priority = priority,
    order = order,
    isAutoVerify = isAutoVerify,
)

internal fun IntentFilterUriRelativeGroup.toSnapshot() = IntentFilterUriRelativeGroupSnapshot(
    isAllowed = isAllowed,
    dataRules = dataRules.map { it.toSnapshot() }.sortedDataRules(),
)

internal fun IntentFilterDataRule.toSnapshot() = IntentFilterDataRuleSnapshot(type = type.name, value = value)

private fun List<IntentFilterDataRuleSnapshot>.sortedDataRules() = sortedWith(compareBy({ it.type }, { it.value }))
