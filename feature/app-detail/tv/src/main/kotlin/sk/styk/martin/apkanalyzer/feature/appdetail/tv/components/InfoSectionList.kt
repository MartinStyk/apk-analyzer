package sk.styk.martin.apkanalyzer.feature.appdetail.tv.components

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.InfoListItem
import sk.styk.martin.apkanalyzer.core.designsystem.tv.focus.rememberRestorableFocus
import sk.styk.martin.apkanalyzer.core.designsystem.tv.focus.restorableFocus
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme

@Composable
internal fun InfoSectionList(
    title: String,
    appName: String,
    sections: List<InfoSection>,
    appReference: AppReference,
    modifier: Modifier = Modifier,
    onClickRow: (InfoRow) -> Unit = {},
    leadsSomewhere: (InfoRow) -> Boolean = { false },
) {
    val iconedSections = sections.map { section -> section.copy(rows = section.rows.map { it.copy(icon = it.icon ?: section.icon) }) }
    val rows = iconedSections.flatMap { it.rows }
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedRow = rows.firstOrNull { it.key == selectedKey } ?: rows.firstOrNull()
    val focus = rememberRestorableFocus(
        availableKeys = rows.mapTo(mutableSetOf()) { it.key },
        defaultKey = rows.firstOrNull()?.key.orEmpty(),
    )

    ListDetailScaffold(
        title = title,
        appName = appName,
        appReference = appReference,
        selected = selectedRow,
        modifier = modifier,
        detail = { row, paneModifier ->
            DetailPane(
                title = row.label,
                value = row.value,
                explanation = row.explanation,
                icon = row.icon,
                status = row.status,
                highlighted = row.highlighted,
                isCodeValue = row.isCode,
                modifier = paneModifier,
            )
        },
    ) {
        iconedSections.forEach { section ->
            item(key = "section:${section.title}") { ListSectionHeader(text = section.title) }
            items(section.rows, key = { it.key }) { row ->
                InfoListItem(
                    label = row.label,
                    value = row.value,
                    onClick = { onClickRow(row) },
                    trailingIcon = if (leadsSomewhere(row)) ApkAnalyzerIcons.ChevronRight else null,
                    highlighted = row.highlighted,
                    leadingIcon = row.icon,
                    modifier = Modifier.restorableFocus(focus, row.key).onFocusChanged {
                        if (it.isFocused) selectedKey = row.key
                    },
                )
            }
        }
    }
}

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun InfoSectionListPreview() {
    ApkAnalyzerTvTheme {
        InfoSectionList(
            title = "General information",
            appName = "YouTube",
            appReference = AppReference.InstalledPackage(PackageName("com.google.android.youtube.tv")),
            sections = listOf(
                InfoSection(
                    title = "Identification",
                    rows = listOf(
                        InfoRow(
                            key = "package",
                            label = "Package name",
                            value = "com.google.android.youtube.tv",
                            explanation = "The unique identifier Android and app stores use for this app.",
                        ),
                        InfoRow(key = "uid", label = "User ID", value = "10123", explanation = "The Linux user ID Android assigned to this app."),
                    ),
                ),
                InfoSection(
                    title = "Security settings",
                    rows = listOf(
                        InfoRow(
                            key = "debug",
                            label = "Debug access",
                            value = "Enabled",
                            explanation = "Other tools can attach a debugger to this app.",
                            highlighted = true,
                        ),
                    ),
                ),
            ),
        )
    }
}
