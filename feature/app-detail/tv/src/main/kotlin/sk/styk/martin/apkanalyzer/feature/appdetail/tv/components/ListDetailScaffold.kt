package sk.styk.martin.apkanalyzer.feature.appdetail.tv.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.AppIcon
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.rememberAppAccentColor
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.AccentBackdrop
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.CrossfadeScrollablePanel
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.InfoListItem
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.SectionHeader
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme

@Composable
internal fun <T> ListDetailScaffold(
    title: String,
    subtitle: String,
    appReference: AppReference,
    selected: T?,
    detail: @Composable (T, Modifier) -> Unit,
    modifier: Modifier = Modifier,
    controls: (@Composable () -> Unit)? = null,
    list: LazyListScope.() -> Unit,
) {
    AccentBackdrop(accent = rememberAppAccentColor(appReference), strength = BACKDROP_STRENGTH, modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 27.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Column(modifier = Modifier.weight(LIST_WEIGHT), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AppIcon(source = appReference, size = 56.dp)
                    Column {
                        Text(text = title, style = AppTheme.typography.headlineSmall)
                        Text(
                            text = subtitle,
                            style = AppTheme.typography.bodyMedium,
                            color = AppTheme.colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (controls != null) {
                    Box(modifier = Modifier.padding(top = 12.dp)) { controls() }
                }
                LazyColumn(
                    contentPadding = PaddingValues(top = 8.dp, bottom = 27.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    content = list,
                )
            }
            CrossfadeScrollablePanel(
                targetState = selected,
                modifier = Modifier.weight(DETAIL_WEIGHT).fillMaxHeight(),
            ) { item ->
                if (item != null) detail(item, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
internal fun ListSectionHeader(text: String, modifier: Modifier = Modifier) {
    SectionHeader(text = text, modifier = modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp))
}

private const val LIST_WEIGHT = 0.55f
private const val DETAIL_WEIGHT = 0.45f
private const val BACKDROP_STRENGTH = 0.2f

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ListDetailScaffoldPreview() {
    ApkAnalyzerTvTheme {
        ListDetailScaffold(
            title = "Permissions",
            subtitle = "YouTube",
            appReference = AppReference.InstalledPackage(PackageName("com.google.android.youtube.tv")),
            selected = "Camera",
            detail = { title, modifier ->
                DetailPane(title = title, value = "android.permission.CAMERA", modifier = modifier)
            },
        ) {
            item { ListSectionHeader(text = "Dangerous · 2") }
            item { InfoListItem(label = "Camera", value = "Granted", onClick = {}) }
            item { InfoListItem(label = "Microphone", value = "Not granted", onClick = {}) }
        }
    }
}
