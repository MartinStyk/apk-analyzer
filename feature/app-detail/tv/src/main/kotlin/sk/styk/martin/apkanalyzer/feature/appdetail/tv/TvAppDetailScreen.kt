package sk.styk.martin.apkanalyzer.feature.appdetail.tv

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.apppermissions.detail.labelRes
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentKind
import sk.styk.martin.apkanalyzer.core.apps.components.sectionLabelRes
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import sk.styk.martin.apkanalyzer.core.common.util.openAppSystemPage
import sk.styk.martin.apkanalyzer.core.common.util.openGooglePlay
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.AppIcon
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.rememberAppAccentColor
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.AccentBackdrop
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.BarSegment
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.Button
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.CodeBox
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.Icon
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.InfoCard
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.InfoCardTone
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.SectionHeader
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.SegmentedBar
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.SegmentedBarLegend
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.Tag
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.tv.focus.MinimalFocusScroll
import sk.styk.martin.apkanalyzer.core.designsystem.tv.focus.rememberRestorableFocus
import sk.styk.martin.apkanalyzer.core.designsystem.tv.focus.restorableFocus
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.insight.AppDetailBadge
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.insight.AppDetailInsight
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.insight.SensitiveAccess
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.insight.label
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.insight.labelRes
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.appcomponents.icon
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.SectionError
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.SectionLoading
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.formatDate
import java.time.Instant
import sk.styk.martin.apkanalyzer.core.apppermissions.R as PermissionsR
import sk.styk.martin.apkanalyzer.core.apps.R as AppsR

@Composable
internal fun TvAppDetailScreen(
    appDetailInput: AppDetailInput,
    onNavigateToGeneralInfo: () -> Unit,
    onNavigateToPermissions: (permissionName: String?) -> Unit,
    onNavigateToComponents: () -> Unit,
    onNavigateToCertificates: () -> Unit,
    onNavigateToRequirements: () -> Unit,
    onNavigateToManifest: () -> Unit,
    viewModel: TvAppDetailViewModel = hiltViewModel { factory: TvAppDetailViewModel.Factory ->
        factory.create(appDetailInput)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TvAppDetailEvent.OpenAppInfo -> context.openAppSystemPage(event.packageName).onFailure {
                    Toast.makeText(context, AppsR.string.app_info_unavailable, Toast.LENGTH_LONG).show()
                }

                is TvAppDetailEvent.OpenPlayStore -> context.openGooglePlay(event.packageName)

                TvAppDetailEvent.NavigateToGeneralInfo -> onNavigateToGeneralInfo()

                is TvAppDetailEvent.NavigateToPermissions -> onNavigateToPermissions(event.permissionName)

                TvAppDetailEvent.NavigateToComponents -> onNavigateToComponents()

                TvAppDetailEvent.NavigateToCertificates -> onNavigateToCertificates()

                TvAppDetailEvent.NavigateToRequirements -> onNavigateToRequirements()

                TvAppDetailEvent.NavigateToManifest -> onNavigateToManifest()
            }
        }
    }

    TvAppDetailContent(state = state, onAction = viewModel::onAction)
}

@Composable
private fun TvAppDetailContent(state: TvAppDetailState, onAction: (TvAppDetailAction) -> Unit) {
    when (state) {
        TvAppDetailState.Loading -> SectionLoading()

        is TvAppDetailState.Error -> SectionError(
            message = stringResource(if (state.canRetry) R.string.tv_app_detail_error else R.string.tv_app_detail_error_unsupported_apk),
            onRetry = if (state.canRetry) {
                { onAction(TvAppDetailAction.Retry) }
            } else {
                null
            },
        )

        is TvAppDetailState.Loaded -> AccentBackdrop(accent = rememberAppAccentColor(state.appReference)) {
            MinimalFocusScroll { LoadedContent(state = state, onAction = onAction) }
        }
    }
}

@Composable
private fun LoadedContent(state: TvAppDetailState.Loaded, onAction: (TvAppDetailAction) -> Unit) {
    val canOpenAppInfo = state.analysisMode == AppDetail.AnalysisMode.InstalledPackage
    val focus = rememberRestorableFocus(
        availableKeys = buildSet {
            if (canOpenAppInfo) add(APP_INFO_KEY)
            add(PLAY_STORE_KEY)
            state.insights.indices.forEach { add(insightKey(it)) }
            addAll(SECTION_KEYS)
        },
        defaultKey = GENERAL_INFO_KEY,
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 48.dp, end = 36.dp, top = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Profile(
            state = state,
            modifier = Modifier
                .width(ProfileWidth)
                .fillMaxHeight()
                .padding(top = FocusRoom, bottom = 27.dp),
        ) {
            if (canOpenAppInfo) {
                Button(
                    onClick = { onAction(TvAppDetailAction.OpenAppInfo) },
                    label = stringResource(R.string.tv_app_detail_action_app_info),
                    leadingIcon = ApkAnalyzerIcons.Settings,
                    modifier = Modifier.restorableFocus(focus, APP_INFO_KEY).fillMaxWidth(),
                )
            }
            Button(
                onClick = { onAction(TvAppDetailAction.OpenPlayStore) },
                label = stringResource(R.string.tv_app_detail_action_play_store),
                leadingIcon = ApkAnalyzerIcons.PlayArrow,
                modifier = Modifier.restorableFocus(focus, PLAY_STORE_KEY).fillMaxWidth(),
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(SECTION_COLUMNS),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentPadding = PaddingValues(start = FocusRoom, end = FocusRoom, top = FocusRoom, bottom = 27.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = GENERAL_INFO_KEY) {
                GeneralInfoCard(state = state, onClick = {
                    onAction(TvAppDetailAction.OpenGeneralInfo)
                }, modifier = Modifier.restorableFocus(focus, GENERAL_INFO_KEY))
            }
            item(key = PERMISSIONS_KEY) {
                PermissionsCard(state = state, onClick = {
                    onAction(TvAppDetailAction.OpenPermissions)
                }, modifier = Modifier.restorableFocus(focus, PERMISSIONS_KEY))
            }
            item(key = COMPONENTS_KEY) {
                ComponentsCard(state = state, onClick = {
                    onAction(TvAppDetailAction.OpenComponents)
                }, modifier = Modifier.restorableFocus(focus, COMPONENTS_KEY))
            }
            item(key = CERTIFICATES_KEY) {
                CertificatesCard(state = state, onClick = {
                    onAction(TvAppDetailAction.OpenCertificates)
                }, modifier = Modifier.restorableFocus(focus, CERTIFICATES_KEY))
            }
            item(key = REQUIREMENTS_KEY) {
                RequirementsCard(state = state, onClick = {
                    onAction(TvAppDetailAction.OpenRequirements)
                }, modifier = Modifier.restorableFocus(focus, REQUIREMENTS_KEY))
            }
            item(key = MANIFEST_KEY) {
                SectionCard(
                    onClick = { onAction(TvAppDetailAction.OpenManifest) },
                    title = stringResource(AppsR.string.manifest_title),
                    icon = ApkAnalyzerIcons.File,
                    modifier = Modifier.restorableFocus(focus, MANIFEST_KEY),
                ) {
                    CardLine(text = stringResource(R.string.tv_app_detail_manifest_description))
                }
            }
            if (state.insights.isNotEmpty()) {
                item(key = "insights-title", span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader(text = stringResource(R.string.tv_app_detail_worth_knowing), modifier = Modifier.padding(top = 8.dp))
                }
                itemsIndexed(state.insights, key = { index, _ -> insightKey(index) }) { index, insight ->
                    InfoCard(
                        onClick = { onAction(TvAppDetailAction.OpenInsight(insight)) },
                        title = insight.label(LocalResources.current),
                        icon = ApkAnalyzerIcons.Warning,
                        tone = InfoCardTone.Warning,
                        modifier = Modifier.restorableFocus(focus, insightKey(index)).height(InsightCardHeight),
                    )
                }
            }
        }
    }
}

@Composable
private fun Profile(
    state: TvAppDetailState.Loaded,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppIcon(source = state.appReference, size = 96.dp)
        Text(
            text = state.appName,
            style = AppTheme.typography.headlineSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = state.packageName.value.replace(".", ".​"),
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurfaceVariant,
        )
        if (state.badges.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                state.badges.forEach { badge -> Tag(label = stringResource(badge.labelRes), icon = badge.icon) }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = actions)
    }
}

@Composable
private fun SectionCard(
    onClick: () -> Unit,
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tone: InfoCardTone = InfoCardTone.Default,
    content: @Composable ColumnScope.() -> Unit,
) {
    InfoCard(
        onClick = onClick,
        title = title,
        icon = icon,
        tone = tone,
        stackedHeader = true,
        modifier = modifier.height(CardHeight),
    ) {
        Column(modifier = Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun GeneralInfoCard(
    state: TvAppDetailState.Loaded,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(onClick = onClick, title = stringResource(AppsR.string.general_info_title), icon = ApkAnalyzerIcons.Info, modifier = modifier) {
        FactLine(label = stringResource(R.string.tv_app_detail_stat_version), value = state.versionName ?: state.versionCode.toString())
        FactLine(
            label = stringResource(R.string.tv_app_detail_stat_target),
            value = state.targetSdkVersion?.let { stringResource(R.string.tv_app_detail_api_level, it) } ?: "-",
            valueColor = if (state.isTargetSdkOutdated) AppTheme.colors.warning else AppTheme.colors.onSurface,
        )
        FactLine(
            label = stringResource(if (state.totalSize != null) R.string.tv_app_detail_stat_size else R.string.tv_app_detail_stat_apk),
            value = (state.totalSize ?: state.apkSize).formatted(),
        )
        state.lastUpdateTime?.let { FactLine(label = stringResource(R.string.tv_app_detail_stat_updated), value = formatDate(it)) }
    }
}

@Composable
private fun PermissionsCard(
    state: TvAppDetailState.Loaded,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val granted = state.grantedSensitivePermissionsCount
    val highlighted = (granted ?: state.sensitivePermissionsCount) > 0
    SectionCard(
        onClick = onClick,
        title = stringResource(PermissionsR.string.permissions_title),
        icon = ApkAnalyzerIcons.Permissions,
        modifier = modifier,
    ) {
        if (state.totalPermissionsCount == 0) {
            CardLine(text = stringResource(R.string.tv_app_detail_permissions_none))
            return@SectionCard
        }
        FactLine(
            label = stringResource(if (granted != null) R.string.tv_app_detail_stat_granted else R.string.tv_app_detail_stat_sensitive),
            value = if (granted != null) {
                stringResource(R.string.tv_app_detail_ratio, granted, state.sensitivePermissionsCount)
            } else {
                state.sensitivePermissionsCount.toString()
            },
            valueColor = if (highlighted) AppTheme.colors.warning else AppTheme.colors.onSurface,
        )
        FactLine(label = stringResource(R.string.tv_app_detail_stat_requested), value = state.totalPermissionsCount.toString())
        val segments = (ProtectionLevel.entries + null).map { level ->
            BarSegment(
                value = state.permissionLevelCounts[level] ?: 0,
                color = level.barColor(),
                label = stringResource(R.string.tv_label_count, stringResource(level.labelRes), state.permissionLevelCounts[level] ?: 0),
            )
        }
        SegmentedBar(segments = segments)
        SegmentedBarLegend(segments = segments)
    }
}

@Composable
private fun ComponentsCard(
    state: TvAppDetailState.Loaded,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(onClick = onClick, title = stringResource(AppsR.string.components_title), icon = ApkAnalyzerIcons.Widgets, modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ComponentKind.entries.forEach { kind ->
                val count = state.componentCounts[kind] ?: 0
                FactLine(label = stringResource(kind.sectionLabelRes), value = count.toString(), icon = kind.icon, dimmed = count == 0)
            }
        }
    }
}

@Composable
private fun FactLine(
    label: String,
    value: String,
    icon: ImageVector? = null,
    valueColor: Color = AppTheme.colors.onSurface,
    dimmed: Boolean = false,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (icon != null) {
            Icon(imageVector = icon, tint = if (dimmed) AppTheme.colors.onSurfaceVariant else AppTheme.colors.primary, modifier = Modifier.size(18.dp))
        }
        Text(
            text = label,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            text = value,
            style = AppTheme.typography.titleMedium,
            color = if (dimmed) AppTheme.colors.onSurfaceVariant else valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CertificatesCard(
    state: TvAppDetailState.Loaded,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(
        onClick = onClick,
        title = stringResource(AppsR.string.certificates_title),
        icon = if (state.isDebugSigned) ApkAnalyzerIcons.Warning else ApkAnalyzerIcons.VerifiedShield,
        tone = if (state.isDebugSigned) InfoCardTone.Warning else InfoCardTone.Default,
        modifier = modifier,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!state.isDebugSigned) {
                Icon(imageVector = ApkAnalyzerIcons.Verified, tint = AppTheme.colors.primary, modifier = Modifier.size(20.dp))
            }
            Text(
                text = when {
                    state.isDebugSigned -> stringResource(AppsR.string.certificates_debug_title)
                    state.signerName != null -> stringResource(R.string.tv_app_detail_signed_by, state.signerName)
                    else -> stringResource(AppsR.string.certificates_unknown_signer)
                },
                style = AppTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        state.sha256Fingerprint?.let { CodeBox(text = it, maxLines = 3, compact = true) }
    }
}

@Composable
private fun RequirementsCard(
    state: TvAppDetailState.Loaded,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasMissing = state.missingRequiredFeaturesCount > 0
    SectionCard(
        onClick = onClick,
        title = stringResource(AppsR.string.requirements_title),
        icon = if (hasMissing) ApkAnalyzerIcons.Warning else ApkAnalyzerIcons.Check,
        tone = if (hasMissing) InfoCardTone.Warning else InfoCardTone.Default,
        modifier = modifier,
    ) {
        if (state.requiredFeaturesCount + state.optionalFeaturesCount == 0) {
            CardLine(text = stringResource(R.string.tv_app_detail_requirements_none))
            return@SectionCard
        }
        Text(
            text = if (hasMissing) {
                pluralStringResource(R.plurals.tv_app_detail_requirements_missing, state.missingRequiredFeaturesCount, state.missingRequiredFeaturesCount)
            } else {
                stringResource(R.string.tv_app_detail_requirements_all_met)
            },
            style = AppTheme.typography.titleSmall,
            color = if (hasMissing) AppTheme.colors.warning else AppTheme.colors.onSurface,
            maxLines = 2,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Tag(label = pluralStringResource(R.plurals.tv_app_detail_required_count, state.requiredFeaturesCount, state.requiredFeaturesCount))
            Tag(label = pluralStringResource(R.plurals.tv_app_detail_optional_count, state.optionalFeaturesCount, state.optionalFeaturesCount))
        }
    }
}

@Composable
private fun CardLine(text: String) {
    Text(
        text = text,
        style = AppTheme.typography.bodyMedium,
        color = AppTheme.colors.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ProtectionLevel?.barColor(): Color = when (this) {
    ProtectionLevel.Dangerous -> AppTheme.colors.warning
    ProtectionLevel.Signature -> AppTheme.colors.primary
    ProtectionLevel.Internal -> AppTheme.colors.tertiary
    ProtectionLevel.Normal -> AppTheme.colors.secondary
    null -> AppTheme.colors.onSurfaceVariant
}

private val AppDetailBadge.icon: ImageVector
    get() = when (this) {
        AppDetailBadge.Large -> ApkAnalyzerIcons.DataUsage
        AppDetailBadge.System -> ApkAnalyzerIcons.Android
        AppDetailBadge.GooglePlay -> ApkAnalyzerIcons.PlayArrow
        AppDetailBadge.RecentlyInstalled -> ApkAnalyzerIcons.NewReleases
        AppDetailBadge.RecentlyUpdated -> ApkAnalyzerIcons.Sync
        AppDetailBadge.RecentlyUsed -> ApkAnalyzerIcons.History
    }

private fun insightKey(index: Int) = "insight:$index"

private const val APP_INFO_KEY = "action:app-info"
private const val PLAY_STORE_KEY = "action:play-store"
private const val GENERAL_INFO_KEY = "section:general-info"
private const val PERMISSIONS_KEY = "section:permissions"
private const val COMPONENTS_KEY = "section:components"
private const val CERTIFICATES_KEY = "section:certificates"
private const val REQUIREMENTS_KEY = "section:requirements"
private const val MANIFEST_KEY = "section:manifest"
private val SECTION_KEYS = listOf(GENERAL_INFO_KEY, PERMISSIONS_KEY, COMPONENTS_KEY, CERTIFICATES_KEY, REQUIREMENTS_KEY, MANIFEST_KEY)
private const val SECTION_COLUMNS = 3
private val ProfileWidth = 190.dp
private val FocusRoom = 12.dp
private val CardHeight = 200.dp
private val InsightCardHeight = 112.dp

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TvAppDetailContentPreview() {
    ApkAnalyzerTvTheme {
        TvAppDetailContent(
            state = TvAppDetailState.Loaded(
                appReference = AppReference.InstalledPackage(PackageName("com.google.android.youtube.tv")),
                analysisMode = AppDetail.AnalysisMode.InstalledPackage,
                appName = "YouTube",
                packageName = PackageName("com.google.android.youtube.tv"),
                versionName = "4.40.303",
                versionCode = 1_440_303,
                targetSdkVersion = 34,
                targetSdkLabel = "Android 14",
                apkSize = 92.megabytes,
                totalSize = 184.megabytes,
                lastUpdateTime = Instant.parse("2026-09-28T10:15:30Z"),
                badges = listOf(AppDetailBadge.GooglePlay, AppDetailBadge.RecentlyUpdated),
                insights = listOf(
                    AppDetailInsight.Sideloaded,
                    AppDetailInsight.SensitivePermission(SensitiveAccess.Contacts, "android.permission.READ_CONTACTS"),
                ),
                totalPermissionsCount = 24,
                sensitivePermissionsCount = 3,
                grantedSensitivePermissionsCount = 1,
                permissionLevelCounts = mapOf(ProtectionLevel.Dangerous to 3, ProtectionLevel.Normal to 18, ProtectionLevel.Signature to 3),
                componentCounts = mapOf(ComponentKind.Activity to 14, ComponentKind.Service to 6, ComponentKind.Receiver to 4, ComponentKind.Provider to 2),
                signerName = "Google LLC",
                isDebugSigned = false,
                sha256Fingerprint = "3D:7A:12:23:01:9A:A3:9D:9E:A0:E3:43:6A:B7:C0:89:6B:FB:4F:B6:79:F4:DE:5F:E7:C2:3F:32:6C:8F:99:4A",
                requiredFeaturesCount = 3,
                optionalFeaturesCount = 5,
                missingRequiredFeaturesCount = 1,
            ),
            onAction = {},
        )
    }
}

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TvAppDetailErrorPreview() {
    ApkAnalyzerTvTheme {
        TvAppDetailContent(state = TvAppDetailState.Error(canRetry = true), onAction = {})
    }
}
