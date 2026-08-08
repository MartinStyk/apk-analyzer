package sk.styk.martin.apkanalyzer.feature.appdetail.impl.appcomponents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import sk.styk.martin.apkanalyzer.core.apps.model.IntentFilterDataRuleType
import sk.styk.martin.apkanalyzer.core.uilibrary.components.BottomSheet
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Icon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.R
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.DetailField

@Suppress("LongMethod")
@Composable
internal fun ComponentDetailBottomSheet(
    item: ComponentItem,
    onCopy: (label: String, value: String) -> Unit,
    onOpenIntentFilters: () -> Unit,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = item.type.icon,
                    contentDescription = stringResource(item.type.sectionLabelRes),
                    tint = AppTheme.colors.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = item.simpleName,
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colors.onSurface,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            DetailField(
                label = stringResource(R.string.components_detail_full_name),
                value = item.name,
                onCopy = onCopy,
            )

            SheetSection(title = stringResource(R.string.components_detail_section_reachability)) {
                DetailField(
                    label = stringResource(R.string.components_detail_exported),
                    value = stringResource(if (item.isExported) R.string.components_exported_yes else R.string.components_exported_no),
                    explanation = stringResource(
                        when {
                            item.isLauncher -> R.string.components_exported_launcher_explanation
                            item.isUnprotected -> R.string.components_exported_unprotected_explanation
                            item.isExported -> R.string.components_exported_guarded_explanation
                            else -> R.string.components_exported_no_explanation
                        },
                    ),
                    onCopy = onCopy,
                )
                when (val details = item.details) {
                    is ComponentDetails.ActivityDetails -> details.permission?.let {
                        DetailField(label = stringResource(R.string.components_detail_permission), value = it, onCopy = onCopy)
                    }

                    is ComponentDetails.ServiceDetails -> details.permission?.let {
                        DetailField(label = stringResource(R.string.components_detail_permission), value = it, onCopy = onCopy)
                    }

                    is ComponentDetails.ReceiverDetails -> details.permission?.let {
                        DetailField(label = stringResource(R.string.components_detail_permission), value = it, onCopy = onCopy)
                    }

                    is ComponentDetails.ProviderDetails -> {
                        details.readPermission?.let {
                            DetailField(label = stringResource(R.string.components_detail_read_permission), value = it, onCopy = onCopy)
                        }
                        details.writePermission?.let {
                            DetailField(label = stringResource(R.string.components_detail_write_permission), value = it, onCopy = onCopy)
                        }
                    }
                }
            }

            if (item.intentFilters.isNotEmpty()) {
                SheetSection(title = stringResource(R.string.components_detail_section_intent_filters)) {
                    Text(
                        text = stringResource(R.string.components_detail_intent_filters_explanation),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(Shapes.CardShape)
                            .clickable(onClick = onOpenIntentFilters)
                            .padding(vertical = 10.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.components_detail_intent_filters_open),
                                style = AppTheme.typography.titleSmall,
                                color = AppTheme.colors.onSurface,
                            )
                            Text(
                                text = pluralStringResource(
                                    R.plurals.components_intent_filter_count,
                                    item.intentFilters.size,
                                    item.intentFilters.size,
                                ),
                                style = AppTheme.typography.bodySmall,
                                color = AppTheme.colors.onSurfaceVariant,
                            )
                        }
                        Icon(
                            imageVector = ApkAnalyzerIcons.ChevronRight,
                            contentDescription = null,
                            tint = AppTheme.colors.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            } else if (!item.areIntentFiltersAvailable) {
                SheetSection(title = stringResource(R.string.components_detail_section_intent_filters)) {
                    Text(
                        text = stringResource(R.string.components_detail_intent_filters_unavailable),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.onSurfaceVariant,
                    )
                }
            }

            val details = item.details
            val hasTypeDetails = item.flags.isNotEmpty() ||
                (
                    details is ComponentDetails.ActivityDetails &&
                        (item.isLauncher || details.label != null || details.targetActivity != null || details.parentName != null)
                    ) ||
                (details is ComponentDetails.ProviderDetails && details.authority != null)
            if (hasTypeDetails) {
                SheetSection(title = stringResource(R.string.components_detail_section_details)) {
                    when (details) {
                        is ComponentDetails.ActivityDetails -> {
                            if (item.isLauncher) {
                                DetailField(
                                    label = stringResource(R.string.components_detail_launcher),
                                    value = stringResource(R.string.components_flag_enabled),
                                    explanation = stringResource(R.string.components_detail_launcher_explanation),
                                    onCopy = onCopy,
                                )
                            }
                            details.label?.let {
                                DetailField(label = stringResource(R.string.components_detail_label), value = it, onCopy = onCopy)
                            }
                            details.targetActivity?.let {
                                DetailField(label = stringResource(R.string.components_detail_target_activity), value = it, onCopy = onCopy)
                            }
                            details.parentName?.let {
                                DetailField(label = stringResource(R.string.components_detail_parent), value = it, onCopy = onCopy)
                            }
                        }

                        is ComponentDetails.ProviderDetails -> details.authority?.let {
                            DetailField(label = stringResource(R.string.components_detail_authority), value = it, onCopy = onCopy)
                        }

                        else -> Unit
                    }
                    item.flags.forEach { flag ->
                        DetailField(
                            label = stringResource(flag.labelRes),
                            value = stringResource(R.string.components_flag_enabled),
                            explanation = stringResource(flag.explanationRes),
                            onCopy = onCopy,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.padding(top = 24.dp)) {
        Text(
            text = title,
            style = AppTheme.typography.titleSmall,
            color = AppTheme.colors.primary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        content()
    }
}

@Preview
@Composable
private fun ComponentDetailBottomSheetActivityPreview() {
    ApkAnalyzerTheme {
        ComponentDetailBottomSheet(
            item = ComponentItem(
                name = "com.spotify.music.features.home.HomeActivity",
                simpleName = "HomeActivity",
                packagePath = "com.spotify.music.features.home",
                type = ComponentType.Activity,
                isExported = true,
                isGuarded = false,
                isUnprotected = false,
                isLaunchable = true,
                flags = persistentListOf(),
                intentFilters = persistentListOf(
                    ComponentIntentFilterItem(
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
                ),
                details = ComponentDetails.ActivityDetails(
                    label = "Spotify",
                    targetActivity = null,
                    parentName = "com.spotify.music.MainActivity",
                    permission = null,
                    isLauncher = true,
                ),
            ),
            onCopy = { _, _ -> },
            onOpenIntentFilters = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun ComponentDetailBottomSheetServicePreview() {
    ApkAnalyzerTheme {
        ComponentDetailBottomSheet(
            item = ComponentItem(
                name = "com.spotify.music.playback.PlaybackService",
                simpleName = "PlaybackService",
                packagePath = "com.spotify.music.playback",
                type = ComponentType.Service,
                isExported = true,
                isGuarded = true,
                isUnprotected = false,
                isLaunchable = false,
                flags = persistentListOf(ComponentFlag.IsolatedProcess, ComponentFlag.StopWithTask),
                details = ComponentDetails.ServiceDetails(permission = "android.permission.BIND_JOB_SERVICE"),
            ),
            onCopy = { _, _ -> },
            onOpenIntentFilters = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun ComponentDetailBottomSheetProviderPreview() {
    ApkAnalyzerTheme {
        ComponentDetailBottomSheet(
            item = ComponentItem(
                name = "androidx.startup.InitializationProvider",
                simpleName = "InitializationProvider",
                packagePath = "androidx.startup",
                type = ComponentType.Provider,
                isExported = false,
                isGuarded = false,
                isUnprotected = false,
                isLaunchable = false,
                flags = persistentListOf(),
                details = ComponentDetails.ProviderDetails(
                    authority = "com.spotify.music.androidx-startup",
                    readPermission = null,
                    writePermission = null,
                ),
            ),
            onCopy = { _, _ -> },
            onOpenIntentFilters = {},
            onDismiss = {},
        )
    }
}
